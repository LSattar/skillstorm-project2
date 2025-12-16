DROP TABLE IF EXISTS payment_transactions;
DROP TABLE IF EXISTS reservation_holds;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS room_type_amenities;
DROP TABLE IF EXISTS room_types;
DROP TABLE IF EXISTS amenities;
DROP TABLE IF EXISTS oauth_identities;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS hotels;

-- =========================
-- Hotels
-- =========================
CREATE TABLE hotels (
  hotel_id     UUID PRIMARY KEY,
  name         VARCHAR(200) NOT NULL,
  phone        VARCHAR(30)  NOT NULL,
  address1     VARCHAR(200) NOT NULL,
  address2     VARCHAR(200),
  city         VARCHAR(120) NOT NULL,
  state        VARCHAR(60)  NOT NULL,
  zip          VARCHAR(20)  NOT NULL,
  timezone     VARCHAR(80)  DEFAULT 'America/Chicago',
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- Users + Roles (RBAC)
-- =========================
CREATE TABLE users (
  user_id     UUID PRIMARY KEY,
  first_name  VARCHAR(120) NOT NULL,
  last_name   VARCHAR(120) NOT NULL,
  email       VARCHAR(255) NOT NULL UNIQUE,
  phone       VARCHAR(30),
  address1    VARCHAR(200),
  address2    VARCHAR(200),
  city        VARCHAR(120),
  state       VARCHAR(60),
  zip         VARCHAR(20),
  status      VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
  role_id UUID PRIMARY KEY,
  name    VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
  user_id UUID NOT NULL,
  role_id UUID NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

CREATE TABLE oauth_identities (
  oauth_identity_id UUID PRIMARY KEY,
  user_id           UUID NOT NULL,
  provider          VARCHAR(50) NOT NULL,
  provider_user_id  VARCHAR(255) NOT NULL,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT uq_provider_user UNIQUE (provider, provider_user_id)
);

-- =========================
-- Amenities
-- =========================
CREATE TABLE amenities (
  amenity_id UUID PRIMARY KEY,
  name       VARCHAR(120) NOT NULL UNIQUE,
  category   VARCHAR(40)  NOT NULL DEFAULT 'ROOM',
  is_active  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- =========================
-- Room Types
-- =========================
CREATE TABLE room_types (
  room_type_id UUID PRIMARY KEY,
  hotel_id     UUID NOT NULL,
  name         VARCHAR(120) NOT NULL,
  description  VARCHAR(2000),
  base_price   DECIMAL(10,2) NOT NULL CHECK (base_price >= 0),
  max_guests   INT NOT NULL CHECK (max_guests > 0),
  bed_count    INT NOT NULL DEFAULT 1 CHECK (bed_count > 0),
  bed_type     VARCHAR(60) NOT NULL DEFAULT 'queen',
  is_active    BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_room_types_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE,
  CONSTRAINT uq_room_type_name UNIQUE (hotel_id, name)
);

CREATE TABLE room_type_amenities (
  room_type_id UUID NOT NULL,
  amenity_id   UUID NOT NULL,
  PRIMARY KEY (room_type_id, amenity_id),
  CONSTRAINT fk_rta_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE CASCADE,
  CONSTRAINT fk_rta_amenity   FOREIGN KEY (amenity_id) REFERENCES amenities(amenity_id) ON DELETE RESTRICT
);

-- =========================
-- Rooms
-- =========================
CREATE TABLE rooms (
  room_id      UUID PRIMARY KEY,
  hotel_id     UUID NOT NULL,
  room_type_id UUID NOT NULL,
  room_number  VARCHAR(20) NOT NULL,
  floor        VARCHAR(20),
  status       VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
  notes        VARCHAR(2000),
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rooms_hotel     FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE,
  CONSTRAINT fk_rooms_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE RESTRICT,
  CONSTRAINT uq_room_number UNIQUE (hotel_id, room_number)
);

-- =========================
-- Reservations
-- =========================
CREATE TABLE reservations (
  reservation_id UUID PRIMARY KEY,
  hotel_id       UUID NOT NULL,
  user_id        UUID NOT NULL,
  room_id        UUID NOT NULL,
  room_type_id   UUID NOT NULL,
  start_date     DATE NOT NULL,
  end_date       DATE NOT NULL,
  guest_count    INT  NOT NULL CHECK (guest_count > 0),
  status         VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  total_amount   DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
  currency       CHAR(3) NOT NULL DEFAULT 'USD',
  special_requests VARCHAR(2000),
  cancellation_reason VARCHAR(2000),
  cancelled_at   TIMESTAMP,
  cancelled_by_user_id UUID,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_res_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id),
  CONSTRAINT fk_res_user  FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_res_room  FOREIGN KEY (room_id) REFERENCES rooms(room_id),
  CONSTRAINT fk_res_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id),
  CONSTRAINT fk_res_cancelled_by FOREIGN KEY (cancelled_by_user_id) REFERENCES users(user_id),
  CONSTRAINT ck_res_dates CHECK (end_date > start_date)
);

CREATE INDEX idx_reservations_room_dates ON reservations (room_id, start_date, end_date);
CREATE INDEX idx_reservations_user_start ON reservations (user_id, start_date);
CREATE INDEX idx_reservations_hotel_status_start ON reservations (hotel_id, status, start_date);

-- =========================
-- Reservation Holds (optional)
-- =========================
CREATE TABLE reservation_holds (
  hold_id     UUID PRIMARY KEY,
  hotel_id    UUID NOT NULL,
  room_id     UUID NOT NULL,
  user_id     UUID NOT NULL,
  start_date  DATE NOT NULL,
  end_date    DATE NOT NULL,
  status      VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  expires_at  TIMESTAMP NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_hold_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE,
  CONSTRAINT fk_hold_room  FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
  CONSTRAINT fk_hold_user  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT ck_hold_dates CHECK (end_date > start_date)
);

CREATE INDEX idx_holds_room_dates ON reservation_holds (room_id, start_date, end_date);
CREATE INDEX idx_holds_expires_at ON reservation_holds (expires_at);

-- =========================
-- Payments / Transactions
-- =========================
CREATE TABLE payment_transactions (
  payment_id     UUID PRIMARY KEY,
  reservation_id UUID NOT NULL,
  user_id        UUID NOT NULL,
  provider       VARCHAR(30) NOT NULL DEFAULT 'STRIPE',
  amount         DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
  currency       CHAR(3) NOT NULL DEFAULT 'USD',
  status         VARCHAR(40) NOT NULL DEFAULT 'PROCESSING',
  transaction_id VARCHAR(120),
  stripe_payment_intent_id VARCHAR(255),
  stripe_charge_id         VARCHAR(255),
  receipt_url    VARCHAR(2000),
  failure_reason VARCHAR(2000),
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pay_res FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE,
  CONSTRAINT fk_pay_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT uq_transaction_id UNIQUE (transaction_id)
);

CREATE INDEX idx_payments_reservation ON payment_transactions (reservation_id);
CREATE INDEX idx_payments_user ON payment_transactions (user_id);
CREATE INDEX idx_payments_status ON payment_transactions (status);
