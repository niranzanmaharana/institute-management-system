-- Institute public / contact profile (platform directory)
ALTER TABLE institutes
  ADD COLUMN mobile VARCHAR(32) NULL AFTER timezone,
  ADD COLUMN admin_email VARCHAR(255) NULL AFTER mobile,
  ADD COLUMN website VARCHAR(512) NULL AFTER admin_email,
  ADD COLUMN address_line1 VARCHAR(255) NULL AFTER website,
  ADD COLUMN address_line2 VARCHAR(255) NULL AFTER address_line1,
  ADD COLUMN city VARCHAR(128) NULL AFTER address_line2,
  ADD COLUMN state VARCHAR(128) NULL AFTER city,
  ADD COLUMN postal_code VARCHAR(32) NULL AFTER state,
  ADD COLUMN country VARCHAR(128) NULL AFTER postal_code,
  ADD COLUMN icon_url VARCHAR(1024) NULL AFTER country;

UPDATE institutes
SET
  mobile = '9000000001',
  admin_email = 'admin.a@demo.local',
  website = 'https://demo-a.example.local',
  address_line1 = '12 Demo Street',
  address_line2 = 'Near Central Park',
  city = 'Pune',
  state = 'MH',
  postal_code = '411001',
  country = 'India',
  icon_url = 'https://placehold.co/96x96/png?text=A'
WHERE code = 'DEMO_A';

UPDATE institutes
SET
  mobile = '9000000002',
  admin_email = 'admin.b@demo.local',
  website = 'https://demo-b.example.local',
  address_line1 = '45 Learning Avenue',
  city = 'Bengaluru',
  state = 'KA',
  postal_code = '560001',
  country = 'India',
  icon_url = 'https://placehold.co/96x96/png?text=B'
WHERE code = 'DEMO_B';
