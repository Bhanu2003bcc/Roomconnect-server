INSERT INTO users (phone, role, phone_verified, status)
VALUES ('+917507030770', 'admin', TRUE, 'active')
ON CONFLICT (phone) DO UPDATE SET role = 'admin';
