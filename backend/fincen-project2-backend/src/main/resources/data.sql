-- data.sql (PostgreSQL) aligned to schema + constraints
-- Notes:
-- - provider values match CHECK constraint
-- - amenities.category values match CHECK constraint
-- - statuses match CHECK constraints
-- - uses ON CONFLICT DO NOTHING for portability
-- - uses future expires_at for ACTIVE hold

-- Hotels
INSERT INTO hotels (hotel_id, name, phone, address1, address2, city, state, zip, timezone)
VALUES
('11111111-1111-1111-1111-111111111111', 'Lone Star Suites', '254-555-0111', '100 Main St', NULL, 'Copperas Cove', 'TX', '76522', 'America/Chicago'),
('22222222-2222-2222-2222-222222222222', 'Austin City Hotel', '512-555-0222', '200 Congress Ave', 'Suite 10', 'Austin', 'TX', '78701', 'America/Chicago')
ON CONFLICT DO NOTHING;

-- Roles
INSERT INTO roles (role_id, name) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'GUEST'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ADMIN'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'MANAGER')
ON CONFLICT DO NOTHING;

-- Users (email is CITEXT + UNIQUE; status must be ACTIVE/INACTIVE/SUSPENDED)
INSERT INTO users (user_id, first_name, last_name, email, phone, address1, address2, city, state, zip, status)
VALUES
('33333333-3333-3333-3333-333333333333', 'Joshua', 'Thompson', 'joshua@example.com', '912-555-0101', '813 Stockdale Rd', NULL, 'Copperas Cove', 'TX', '76522', 'ACTIVE'),
('44444444-4444-4444-4444-444444444444', 'Hannah', 'Thorpe', 'hannah@example.com', '512-555-0102', '1 Admin Way', NULL, 'Austin', 'TX', '78701', 'ACTIVE'),
('55555555-5555-5555-5555-555555555555', 'Murali', 'Guthula', 'murali@example.com', '512-555-0103', '2 Manager Dr', NULL, 'Austin', 'TX', '78701', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- User roles (junction)
INSERT INTO user_roles (user_id, role_id) VALUES
('33333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'), -- guest
('44444444-4444-4444-4444-444444444444', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'), -- admin
('55555555-5555-5555-5555-555555555555', 'cccccccc-cccc-cccc-cccc-cccccccccccc')  -- manager
ON CONFLICT DO NOTHING;

-- OAuth identities
-- provider must be one of: GOOGLE/GITHUB/MICROSOFT/APPLE/OKTA
INSERT INTO oauth_identities (oauth_identity_id, user_id, provider, provider_user_id)
VALUES
('66666666-6666-6666-6666-666666666666', '33333333-3333-3333-3333-333333333333', 'GOOGLE', 'google-sub-123456'),
('77777777-7777-7777-7777-777777777777', '44444444-4444-4444-4444-444444444444', 'GOOGLE', 'google-sub-999999')
ON CONFLICT DO NOTHING;

-- Amenities
-- category must be ROOM or PROPERTY
INSERT INTO amenities (amenity_id, name, category, is_active) VALUES
('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 'Free WiFi',            'ROOM',     TRUE),
('a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2', 'Breakfast Included',   'PROPERTY', TRUE),
('a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a3', 'Pool Access',          'PROPERTY', TRUE),
('a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4', 'Kitchenette',          'ROOM',     TRUE),
('a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a5a5', 'Accessible Shower',    'ROOM',     TRUE)
ON CONFLICT DO NOTHING;

-- Room Types
-- bed_type must be one of: twin/full/queen/king/sofa (case-insensitive)

-- Room Types (Hotel 1)
INSERT INTO room_types (room_type_id, hotel_id, name, description, base_price, max_guests, bed_count, bed_type, is_active)
VALUES
('10101010-1010-1010-1010-101010101010', '11111111-1111-1111-1111-111111111111', 'Standard King', 'King bed, work desk, basic amenities.', 129.00, 2, 1, 'king', TRUE),
('20202020-2020-2020-2020-202020202020', '11111111-1111-1111-1111-111111111111', 'Double Queen',  'Two queen beds, ideal for families.',     149.00, 4, 2, 'queen', TRUE),
('30303030-3030-3030-3030-303030303030', '11111111-1111-1111-1111-111111111111', 'King Suite',    'More space with kitchenette and seating area.', 199.00, 3, 1, 'king', TRUE)
ON CONFLICT DO NOTHING;

-- Room Types (Hotel 2)
INSERT INTO room_types (room_type_id, hotel_id, name, description, base_price, max_guests, bed_count, bed_type, is_active)
VALUES
('40404040-4040-4040-4040-404040404040', '22222222-2222-2222-2222-222222222222', 'City Standard', 'Modern room in downtown Austin.', 179.00, 2, 1, 'king', TRUE)
ON CONFLICT DO NOTHING;

-- Room Type Amenities (junction)
INSERT INTO room_type_amenities (room_type_id, amenity_id) VALUES
('10101010-1010-1010-1010-101010101010', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1'), -- wifi
('10101010-1010-1010-1010-101010101010', 'a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2'), -- breakfast
('20202020-2020-2020-2020-202020202020', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1'),
('20202020-2020-2020-2020-202020202020', 'a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a3'), -- pool
('30303030-3030-3030-3030-303030303030', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1'),
('30303030-3030-3030-3030-303030303030', 'a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4'), -- kitchenette
('30303030-3030-3030-3030-303030303030', 'a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a5a5'), -- accessible
('40404040-4040-4040-4040-404040404040', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1')
ON CONFLICT DO NOTHING;

-- Rooms
-- status must be: AVAILABLE/OCCUPIED/MAINTENANCE/OUT_OF_SERVICE

-- Rooms (Hotel 1)
INSERT INTO rooms (room_id, hotel_id, room_type_id, room_number, floor, status, notes)
VALUES
('11110000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '10101010-1010-1010-1010-101010101010', '101', '1', 'AVAILABLE', NULL),
('11110000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '10101010-1010-1010-1010-101010101010', '102', '1', 'AVAILABLE', NULL),
('11110000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', '20202020-2020-2020-2020-202020202020', '201', '2', 'AVAILABLE', NULL),
('11110000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', '20202020-2020-2020-2020-202020202020', '202', '2', 'MAINTENANCE', 'AC repair in progress'),
('11110000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', '30303030-3030-3030-3030-303030303030', '301', '3', 'AVAILABLE', 'Corner suite')
ON CONFLICT DO NOTHING;

-- Rooms (Hotel 2)
INSERT INTO rooms (room_id, hotel_id, room_type_id, room_number, floor, status, notes)
VALUES
('22220000-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', '40404040-4040-4040-4040-404040404040', '1204', '12', 'AVAILABLE', NULL)
ON CONFLICT DO NOTHING;

-- Reservations
INSERT INTO reservations (
  reservation_id, hotel_id, user_id, room_id, room_type_id,
  start_date, end_date, guest_count, status, total_amount, currency, special_requests
) VALUES
('90000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333',
 '11110000-0000-0000-0000-000000000001', '10101010-1010-1010-1010-101010101010',
 DATE '2025-12-20', DATE '2025-12-22', 2, 'CONFIRMED', 258.00, 'USD', 'Late check-in if possible'),

('90000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333',
 '11110000-0000-0000-0000-000000000003', '20202020-2020-2020-2020-202020202020',
 DATE '2026-01-05', DATE '2026-01-07', 4, 'PENDING', 298.00, 'USD', NULL)
ON CONFLICT DO NOTHING;

-- Cancelled reservation example
INSERT INTO reservations (
  reservation_id, hotel_id, user_id, room_id, room_type_id,
  start_date, end_date, guest_count, status, total_amount, currency,
  cancellation_reason, cancelled_at, cancelled_by_user_id
) VALUES
('90000000-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333',
 '22220000-0000-0000-0000-000000000001', '40404040-4040-4040-4040-404040404040',
 DATE '2025-12-26', DATE '2025-12-27', 2, 'CANCELLED', 179.00, 'USD',
 'Plans changed', '2025-12-16 10:15:00+00'::timestamptz, '33333333-3333-3333-3333-333333333333')
ON CONFLICT DO NOTHING;

-- Reservation Holds
INSERT INTO reservation_holds (
  hold_id, hotel_id, room_id, user_id, start_date, end_date, status, expires_at
) VALUES
('91000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111',
 '11110000-0000-0000-0000-000000000002', '33333333-3333-3333-3333-333333333333',
 DATE '2025-12-21', DATE '2025-12-22', 'ACTIVE', NOW() + INTERVAL '7 days')
ON CONFLICT DO NOTHING;

-- Payments / Transactions
INSERT INTO payment_transactions (
  payment_id, reservation_id, user_id, provider, amount, currency, status,
  transaction_id, stripe_payment_intent_id, receipt_url
) VALUES
('92000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333',
 'STRIPE', 258.00, 'USD', 'SUCCEEDED',
 'txn_10001', 'pi_mock_10001', 'https://example.com/receipt/txn_10001'),

('92000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000002', '33333333-3333-3333-3333-333333333333',
 'STRIPE', 298.00, 'USD', 'PROCESSING',
 'txn_10002', 'pi_mock_10002', NULL),

('92000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000003', '33333333-3333-3333-3333-333333333333',
 'STRIPE', 179.00, 'USD', 'REFUNDED',
 'txn_10003', 'pi_mock_10003', 'https://example.com/receipt/txn_10003')
ON CONFLICT DO NOTHING;
