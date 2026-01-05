-- USERS
INSERT INTO users (
  user_id,
  first_name,
  last_name,
  email,
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
  'joshuathompson0526@gmail.com',
  '912-572-1579',
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
  'hannahthompson@example.com',
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
  '512-555-0103',
  '2 Manager Dr',
  'Austin',
  'TX',
  '78701',
  'ACTIVE'
),
-- Added: Guest
(
  '77777777-7777-7777-7777-777777777777',
  'Gary',
  'Guest',
  'gary.guest@example.com',
  '210-555-0199',
  '99 Traveler Ln',
  'San Antonio',
  'TX',
  '78205',
  'ACTIVE'
),
-- Added: Business Owner
(
  '88888888-8888-8888-8888-888888888888',
  'Olivia',
  'Owner',
  'olivia.owner@example.com',
  '214-555-0111',
  '500 Commerce St',
  'Dallas',
  'TX',
  '75201',
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

-- USER ↔ ROLE JUNCTION (use role name lookup so IDs don't have to match)
INSERT INTO user_roles (user_id, role_id)
SELECT '33333333-3333-3333-3333-333333333333'::uuid, r.role_id FROM roles r WHERE r.name = 'ADMIN'
UNION ALL
SELECT '44444444-4444-4444-4444-444444444444'::uuid, r.role_id FROM roles r WHERE r.name = 'EMPLOYEE'
UNION ALL
SELECT '55555555-5555-5555-5555-555555555555'::uuid, r.role_id FROM roles r WHERE r.name = 'MANAGER'
UNION ALL
SELECT '77777777-7777-7777-7777-777777777777'::uuid, r.role_id FROM roles r WHERE r.name = 'GUEST'
UNION ALL
SELECT '88888888-8888-8888-8888-888888888888'::uuid, r.role_id FROM roles r WHERE r.name = 'BUSINESS_OWNER'
ON CONFLICT DO NOTHING;

-- OAUTH IDENTITIES
-- NOTE: provider_user_id must match Google's real "sub" to match on login.
-- If you rely on "link by verified email", you can omit these rows entirely.
INSERT INTO oauth_identities (
  oauth_identity_id,
  user_id,
  provider,
  provider_user_id
) VALUES
(
  '66666666-6666-6666-6666-666666666666',
  '33333333-3333-3333-3333-333333333333',
  'google',
  'google-sub-123456'
),
(
  '99999999-9999-9999-9999-999999999999',
  '44444444-4444-4444-4444-444444444444',
  'google',
  'google-sub-hannah'
),
(
  'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
  '55555555-5555-5555-5555-555555555555',
  'google',
  'google-sub-karen'
),
(
  'bbbbbbbb-cccc-dddd-eeee-ffffffffffff',
  '77777777-7777-7777-7777-777777777777',
  'google',
  'google-sub-gary'
),
(
  'cccccccc-dddd-eeee-ffff-000000000000',
  '88888888-8888-8888-8888-888888888888',
  'google',
  'google-sub-olivia'
)
ON CONFLICT DO NOTHING;