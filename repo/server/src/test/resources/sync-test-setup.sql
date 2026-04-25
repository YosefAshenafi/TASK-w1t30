-- Organizations for OrgIsolationContentApiTest
INSERT INTO organizations (id, name, code) VALUES
('00000000-0000-0000-0000-000000000010', 'Org Alpha', 'ALPHA'),
('00000000-0000-0000-0000-000000000020', 'Org Beta',  'BETA')
ON CONFLICT DO NOTHING;

-- Corporate mentor and student fixtures for OrgIsolationContentApiTest
INSERT INTO users (id, username, display_name, password_bcrypt, role, status, organization_id) VALUES
('00000000-0000-0000-0000-000000000011', 'corp_mentor_a',  'Corp Mentor A',  '$2a$12$placeholder', 'CORPORATE_MENTOR', 'ACTIVE', '00000000-0000-0000-0000-000000000010'),
('00000000-0000-0000-0000-000000000021', 'corp_mentor_b',  'Corp Mentor B',  '$2a$12$placeholder', 'CORPORATE_MENTOR', 'ACTIVE', '00000000-0000-0000-0000-000000000020'),
('00000000-0000-0000-0000-000000000012', 'student_org_a',  'Student Org A',  '$2a$12$placeholder', 'STUDENT',          'ACTIVE', '00000000-0000-0000-0000-000000000010'),
('00000000-0000-0000-0000-000000000022', 'student_org_b',  'Student Org B',  '$2a$12$placeholder', 'STUDENT',          'ACTIVE', '00000000-0000-0000-0000-000000000020')
ON CONFLICT DO NOTHING;

INSERT INTO users (id, username, display_name, password_bcrypt, role, status, organization_id)
VALUES ('00000000-0000-0000-0000-000000000002', 'sync_test_student', 'Sync Test Student',
        '$2a$12$placeholder', 'STUDENT', 'ACTIVE', '00000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO users (id, username, display_name, password_bcrypt, role, status, organization_id)
VALUES ('00000000-0000-0000-0000-000000000003', 'sync_test_other', 'Sync Test Other',
        '$2a$12$placeholder', 'STUDENT', 'ACTIVE', '00000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO users (id, username, display_name, password_bcrypt, role, status, organization_id)
VALUES ('00000000-0000-0000-0000-000000000099', 'sync_test_other_org', 'Other Org Student',
        '$2a$12$placeholder', 'STUDENT', 'ACTIVE', '00000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO courses (id, code, title, version, location_id, instructor_id, classification)
VALUES ('00000000-0000-0000-0000-000000000c10', 'SYNC-TEST-001', 'Sync Test Course', '1.0',
        '00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000020', 'INTERNAL')
ON CONFLICT DO NOTHING;

INSERT INTO activities (id, course_id, name, description, sort_order)
VALUES ('00000000-0000-0000-0000-000000000a01', '00000000-0000-0000-0000-000000000c10',
        'Test Activity', 'Test activity description', 1)
ON CONFLICT DO NOTHING;
