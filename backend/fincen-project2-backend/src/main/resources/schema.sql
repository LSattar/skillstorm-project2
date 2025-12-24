-- Highlights:
-- - UUIDs generated in DB (pgcrypto: gen_random_uuid())
-- - Case-insensitive email (citext)
-- - Auto-updating updated_at triggers (all tables with updated_at)
-- - Immutable users.email enforced in DB
-- - Exclusion constraints prevent overlapping reservations/holds per room
--   using half-open date ranges: [start_date, end_date)
--     - Back-to-back stays allowed (end == next start)

-- Extensions (REQUIRED)
-- Need elevated DB privileges to run
-- Run extensions once during initial DB setup, then disable or remove
-- to avoid application startup errors.
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;     -- case-insensitive text
CREATE EXTENSION IF NOT EXISTS btree_gist; -- needed for UUID equality in GiST exclusion constraints

-- Drop order (children first)
DROP TABLE IF EXISTS payment_transactions CASCADE;
DROP TABLE IF EXISTS reservation_holds CASCADE;
DROP TABLE IF EXISTS reservations CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;
DROP TABLE IF EXISTS room_type_amenities CASCADE;
DROP TABLE IF EXISTS room_types CASCADE;
DROP TABLE IF EXISTS amenities CASCADE;
DROP TABLE IF EXISTS oauth_identities CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS hotels CASCADE;

-- =========================
-- Hotels
-- =========================
CREATE TABLE hotels (
  hotel_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name       VARCHAR(200) NOT NULL,
  phone      VARCHAR(30)  NOT NULL,
  address1   VARCHAR(200) NOT NULL,
  address2   VARCHAR(200),
  city       VARCHAR(60)  NOT NULL,
  state      VARCHAR(2)   NOT NULL,
  zip        VARCHAR(10)  NOT NULL,
  timezone   VARCHAR(80)  NOT NULL DEFAULT 'America/Chicago',
  created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

  CONSTRAINT ck_hotels_state_len CHECK (LENGTH(state) = 2),
  CONSTRAINT ck_hotels_zip_len   CHECK (LENGTH(zip) BETWEEN 5 AND 10)
);

-- Users + Roles (RBAC)
CREATE TABLE users (
  user_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  first_name    VARCHAR(120) NOT NULL,
  last_name     VARCHAR(120) NOT NULL,
  email         CITEXT NOT NULL,
  password_hash VARCHAR(255),
  phone         VARCHAR(30),
  address1      VARCHAR(200),
  address2      VARCHAR(200),
  city          VARCHAR(60),
  state         VARCHAR(2),
  zip           VARCHAR(10),
  status        VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_users_email UNIQUE (email),

  CONSTRAINT ck_users_email_trimmed CHECK (email = btrim(email)),

  CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
  CONSTRAINT ck_users_state_len CHECK (state IS NULL OR LENGTH(state) = 2),
  CONSTRAINT ck_users_zip_len CHECK (zip IS NULL OR LENGTH(zip) BETWEEN 5 AND 10)
);

CREATE TABLE roles (
  role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name    VARCHAR(40) NOT NULL,
  CONSTRAINT uq_roles_name UNIQUE (name),
  CONSTRAINT ck_roles_name_upper CHECK (name = upper(name))
);

CREATE TABLE user_roles (
  user_id UUID NOT NULL,
  role_id UUID NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

CREATE TABLE oauth_identities (
  oauth_identity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID NOT NULL,
  provider          VARCHAR(50)  NOT NULL,
  provider_user_id  VARCHAR(255) NOT NULL,
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT uq_provider_user UNIQUE (provider, provider_user_id),
  CONSTRAINT uq_user_provider UNIQUE (user_id, provider),
  CONSTRAINT ck_oauth_provider CHECK (provider IN ('GOOGLE', 'GITHUB', 'MICROSOFT', 'APPLE', 'OKTA'))
);

-- Amenities
CREATE TABLE amenities (
  amenity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name       VARCHAR(120) NOT NULL,
  category   VARCHAR(40)  NOT NULL DEFAULT 'ROOM',
  is_active  BOOLEAN      NOT NULL DEFAULT TRUE,

  CONSTRAINT uq_amenities_name UNIQUE (name),
  CONSTRAINT ck_amenities_category CHECK (category IN ('ROOM', 'PROPERTY'))
);

-- Room Types
CREATE TABLE room_types (
  room_type_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  hotel_id     UUID NOT NULL,
  name         VARCHAR(120) NOT NULL,
  description  VARCHAR(2000),
  base_price   NUMERIC(10,2) NOT NULL,
  max_guests   INT NOT NULL,
  bed_count    INT NOT NULL DEFAULT 1,
  bed_type     VARCHAR(60) NOT NULL DEFAULT 'queen',
  is_active    BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_room_types_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE,
  CONSTRAINT uq_room_type_name UNIQUE (hotel_id, name),
  CONSTRAINT ck_room_types_base_price CHECK (base_price >= 0),
  CONSTRAINT ck_room_types_max_guests CHECK (max_guests > 0),
  CONSTRAINT ck_room_types_bed_count CHECK (bed_count > 0),
  CONSTRAINT ck_room_types_bed_type CHECK (LOWER(bed_type) IN ('twin','full','queen','king','sofa'))
);

CREATE TABLE room_type_amenities (
  room_type_id UUID NOT NULL,
  amenity_id   UUID NOT NULL,
  PRIMARY KEY (room_type_id, amenity_id),
  CONSTRAINT fk_rta_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE CASCADE,
  CONSTRAINT fk_rta_amenity   FOREIGN KEY (amenity_id) REFERENCES amenities(amenity_id) ON DELETE RESTRICT
);

-- Rooms
CREATE TABLE rooms (
  room_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  hotel_id     UUID NOT NULL,
  room_type_id UUID NOT NULL,
  room_number  VARCHAR(20) NOT NULL,
  floor        VARCHAR(20),
  status       VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
  notes        VARCHAR(2000),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_rooms_hotel     FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE,
  CONSTRAINT fk_rooms_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE RESTRICT,
  CONSTRAINT uq_room_number UNIQUE (hotel_id, room_number),
  CONSTRAINT ck_rooms_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'OUT_OF_SERVICE'))
);

-- Reservations
-- Note: start/end are treated as a half-open range [start_date, end_date)
CREATE TABLE reservations (
  reservation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  hotel_id       UUID NOT NULL,
  user_id        UUID NOT NULL,
  room_id        UUID NOT NULL,
  room_type_id   UUID NOT NULL,
  start_date     DATE NOT NULL,
  end_date       DATE NOT NULL,
  guest_count    INT  NOT NULL CHECK (guest_count > 0),
  status         VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  total_amount   NUMERIC(10,2) NOT NULL DEFAULT 0,
  currency       CHAR(3) NOT NULL DEFAULT 'USD',
  special_requests     VARCHAR(2000),
  cancellation_reason  VARCHAR(2000),
  cancelled_at         TIMESTAMPTZ,
  cancelled_by_user_id UUID,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_res_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE RESTRICT,
  CONSTRAINT fk_res_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE RESTRICT,
  CONSTRAINT fk_res_room  FOREIGN KEY (room_id)  REFERENCES rooms(room_id)  ON DELETE RESTRICT,
  CONSTRAINT fk_res_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE RESTRICT,
  CONSTRAINT fk_res_cancelled_by FOREIGN KEY (cancelled_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL,

  CONSTRAINT ck_res_dates CHECK (end_date > start_date),
  CONSTRAINT ck_res_guest_count CHECK (guest_count > 0),
  CONSTRAINT ck_res_total_amount CHECK (total_amount >= 0),
  CONSTRAINT ck_res_currency CHECK (currency ~ '^[A-Z]{3}$'),
  CONSTRAINT ck_res_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'CHECKED_IN', 'CHECKED_OUT'))
);

-- Reservation Holds
CREATE TABLE reservation_holds (
  hold_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  hotel_id    UUID NOT NULL,
  room_id     UUID NOT NULL,
  user_id     UUID NOT NULL,
  start_date  DATE NOT NULL,
  end_date    DATE NOT NULL,
  status      VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  expires_at  TIMESTAMPTZ NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_hold_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE,
  CONSTRAINT fk_hold_room  FOREIGN KEY (room_id)  REFERENCES rooms(room_id)  ON DELETE CASCADE,
  CONSTRAINT fk_hold_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE CASCADE,

  CONSTRAINT ck_hold_dates CHECK (end_date > start_date),
  CONSTRAINT ck_hold_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED'))
);

-- Payments / Transactions
CREATE TABLE payment_transactions (
  payment_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reservation_id UUID NOT NULL,
  user_id        UUID NOT NULL,
  provider       VARCHAR(30) NOT NULL DEFAULT 'STRIPE',
  amount         NUMERIC(10,2) NOT NULL,
  currency       CHAR(3) NOT NULL DEFAULT 'USD',
  status         VARCHAR(40) NOT NULL DEFAULT 'PROCESSING',
  transaction_id VARCHAR(120),
  stripe_payment_intent_id VARCHAR(255),
  stripe_charge_id         VARCHAR(255),
  receipt_url    VARCHAR(2000),
  failure_reason VARCHAR(2000),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_pay_res FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE,
  CONSTRAINT fk_pay_user FOREIGN KEY (user_id)        REFERENCES users(user_id)        ON DELETE RESTRICT,

  CONSTRAINT uq_transaction_id UNIQUE (transaction_id),
  CONSTRAINT ck_pay_amount CHECK (amount >= 0),
  CONSTRAINT ck_pay_currency CHECK (currency ~ '^[A-Z]{3}$'),
  CONSTRAINT ck_pay_provider CHECK (provider IN ('STRIPE', 'PAYPAL', 'ADYEN')),
  CONSTRAINT ck_pay_status CHECK (status IN ('PROCESSING', 'SUCCEEDED', 'FAILED', 'REFUNDED', 'CANCELLED'))
);


-- Exclusion constraints (NO overlapping bookings/holds per room)
-- Half-open daterange: [start_date, end_date)

-- Activeish reservations cannot overlap for the same room
ALTER TABLE reservations
ADD CONSTRAINT ex_reservations_room_no_overlap
EXCLUDE USING gist (
  room_id WITH =,
  daterange(start_date, end_date, '[)') WITH &&
)
WHERE (status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN'));

-- Active holds cannot overlap for the same room
ALTER TABLE reservation_holds
ADD CONSTRAINT ex_holds_room_no_overlap
EXCLUDE USING gist (
  room_id WITH =,
  daterange(start_date, end_date, '[)') WITH &&
)
WHERE (status = 'ACTIVE');


-- Indexes (Performance)
-- Junction/FK indexes
CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE INDEX idx_oauth_user_id ON oauth_identities (user_id);
CREATE INDEX idx_oauth_provider_user ON oauth_identities (provider, provider_user_id);

CREATE INDEX idx_room_types_hotel_id ON room_types (hotel_id);

CREATE INDEX idx_rooms_hotel_id ON rooms (hotel_id);
CREATE INDEX idx_rooms_room_type_id ON rooms (room_type_id);

CREATE INDEX idx_rta_amenity_id ON room_type_amenities (amenity_id);

CREATE INDEX idx_reservations_hotel_id ON reservations (hotel_id);
CREATE INDEX idx_reservations_user_id ON reservations (user_id);
CREATE INDEX idx_reservations_room_id ON reservations (room_id); -- added
CREATE INDEX idx_reservations_room_type_id ON reservations (room_type_id);
CREATE INDEX idx_reservations_room_dates ON reservations (room_id, start_date, end_date);
CREATE INDEX idx_reservations_user_start ON reservations (user_id, start_date);
CREATE INDEX idx_reservations_hotel_status_start ON reservations (hotel_id, status, start_date);

CREATE INDEX idx_holds_room_id ON reservation_holds (room_id); -- added
CREATE INDEX idx_holds_room_dates ON reservation_holds (room_id, start_date, end_date);
CREATE INDEX idx_holds_expires_at ON reservation_holds (expires_at);
CREATE INDEX idx_holds_user_id ON reservation_holds (user_id);

CREATE INDEX idx_payments_reservation_id ON payment_transactions (reservation_id);
CREATE INDEX idx_payments_user_id ON payment_transactions (user_id);
CREATE INDEX idx_payments_status ON payment_transactions (status);

-- Triggers
-- Auto-maintain updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_hotels_updated_at ON hotels;
CREATE TRIGGER trg_hotels_updated_at
BEFORE UPDATE ON hotels
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_oauth_updated_at ON oauth_identities;
CREATE TRIGGER trg_oauth_updated_at
BEFORE UPDATE ON oauth_identities
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_room_types_updated_at ON room_types;
CREATE TRIGGER trg_room_types_updated_at
BEFORE UPDATE ON room_types
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_rooms_updated_at ON rooms;
CREATE TRIGGER trg_rooms_updated_at
BEFORE UPDATE ON rooms
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_reservations_updated_at ON reservations;
CREATE TRIGGER trg_reservations_updated_at
BEFORE UPDATE ON reservations
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_holds_updated_at ON reservation_holds;
CREATE TRIGGER trg_holds_updated_at
BEFORE UPDATE ON reservation_holds
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_payments_updated_at ON payment_transactions;
CREATE TRIGGER trg_payments_updated_at
BEFORE UPDATE ON payment_transactions
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
 
-- Enforce users.email immutability
CREATE OR REPLACE FUNCTION prevent_users_email_update()
RETURNS trigger AS $$
BEGIN
  IF NEW.email IS DISTINCT FROM OLD.email THEN
    RAISE EXCEPTION 'Email is immutable and cannot be updated';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_users_email_update ON users;
CREATE TRIGGER trg_prevent_users_email_update
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION prevent_users_email_update();
