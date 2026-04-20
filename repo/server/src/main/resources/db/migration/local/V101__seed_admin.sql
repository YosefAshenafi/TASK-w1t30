INSERT INTO users (
    id,
    username,
    display_name,
    password_bcrypt,
    role,
    status,
    organization_id
) VALUES (
    '00000000-0000-0000-0000-000000000100',
    'admin',
    'System Administrator',
    crypt('Admin@123!', gen_salt('bf', 12)),
    'ADMIN',
    'ACTIVE',
    '00000000-0000-0000-0000-000000000001'
);
