-- USERS
-- All test users have password: "password123"
-- BCrypt hash: $2a$10$rZqLLt8VvZ9vZ9vZ9vZ9vOeX6m8yFz8yFz8yFz8yFz8yFz8yFz
INSERT INTO users (
  user_id,
  first_name,
  last_name,
  email,
  password_hash,
  phone,
  address1,
  city,
  state,
  zip,
  status
) VALUES
(
  '33333333-3333-3333-3333-333333333333',
  'Joshua',
  'Thompson',
  'joshua@example.com',
  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
  '912-555-0101',
  '813 Stockdale Rd',
  'Copperas Cove',
  'TX',
  '76522',
  'ACTIVE'
),
(
  '44444444-4444-4444-4444-444444444444',
  'Hannah',
  'Thompson',
  'hannah@example.com',
  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
  '512-555-0102',
  '1 Admin Way',
  'Austin',
  'TX',
  '78701',
  'ACTIVE'
),
(
  '55555555-5555-5555-5555-555555555555',
  'Karen',
  'Thompson',
  'kthompson@example.com',
  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
  '512-555-0103',
  '2 Manager Dr',
  'Austin',
  'TX',
  '78701',
  'ACTIVE'
);

-- ROLES (RBAC)
INSERT INTO roles (role_id, name) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'GUEST'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ADMIN'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'MANAGER'),
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'EMPLOYEE'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'BUSINESS_OWNER')
ON CONFLICT (name) DO NOTHING;

-- USER ↔ ROLE JUNCTION
INSERT INTO user_roles (user_id, role_id) VALUES
-- Joshua: Guest
('33333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),

-- Hannah: Admin + Business Owner
('44444444-4444-4444-4444-444444444444', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('44444444-4444-4444-4444-444444444444', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),

-- Karen: Manager + Employee
('55555555-5555-5555-5555-555555555555', 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
('55555555-5555-5555-5555-555555555555', 'dddddddd-dddd-dddd-dddd-dddddddddddd');

-- OAUTH IDENTITIES
INSERT INTO oauth_identities (
  oauth_identity_id,
  user_id,
  provider,
  provider_user_id
) VALUES
(
  '66666666-6666-6666-6666-666666666666',
  '33333333-3333-3333-3333-333333333333',
  'GOOGLE',
  'google-sub-123456'
),
(
  '77777777-7777-7777-7777-777777777777',
  '44444444-4444-4444-4444-444444444444',
  'OKTA',
  'okta-sub-abcdef'
);
