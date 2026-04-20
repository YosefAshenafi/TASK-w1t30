-- Fixtures for ClassificationApiTest.
-- Seeds a PUBLIC course and a CONFIDENTIAL course with child resources so
-- classification checks on child endpoints can be verified.

INSERT INTO users (id, username, display_name, password_bcrypt, role, status, organization_id)
VALUES ('00000000-0000-0000-0000-000000000050', 'cls_test_mentor', 'Classification Test Mentor',
        '$2a$12$placeholder', 'CORPORATE_MENTOR', 'ACTIVE',
        '00000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO users (id, username, display_name, password_bcrypt, role, status, organization_id)
VALUES ('00000000-0000-0000-0000-000000000060', 'cls_test_faculty', 'Classification Test Faculty',
        '$2a$12$placeholder', 'FACULTY_MENTOR', 'ACTIVE', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO users (id, username, display_name, password_bcrypt, role, status, organization_id)
VALUES ('00000000-0000-0000-0000-000000000070', 'cls_test_admin', 'Classification Test Admin',
        '$2a$12$placeholder', 'ADMIN', 'ACTIVE',
        '00000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO courses (id, code, title, version, location_id, instructor_id, classification)
VALUES ('00000000-0000-0000-0000-00000000cc10', 'CLS-PUBLIC', 'Classification Public Course',
        '1.0', '00000000-0000-0000-0000-000000000010',
        '00000000-0000-0000-0000-000000000020', 'PUBLIC')
ON CONFLICT DO NOTHING;

INSERT INTO courses (id, code, title, version, location_id, instructor_id, classification)
VALUES ('00000000-0000-0000-0000-00000000cc11', 'CLS-CONF', 'Classification Confidential Course',
        '1.0', '00000000-0000-0000-0000-000000000010',
        '00000000-0000-0000-0000-000000000020', 'CONFIDENTIAL')
ON CONFLICT DO NOTHING;

INSERT INTO cohorts (id, course_id, name, total_seats, starts_at, ends_at) VALUES
('00000000-0000-0000-0000-00000000cc20', '00000000-0000-0000-0000-00000000cc11',
 'CLS-CONF Cohort A', 10, '2026-01-01T09:00:00Z', '2026-01-31T17:00:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO activities (id, course_id, name, description, sort_order) VALUES
('00000000-0000-0000-0000-00000000cc30', '00000000-0000-0000-0000-00000000cc11',
 'CLS-CONF Activity', 'Confidential activity', 1)
ON CONFLICT DO NOTHING;

INSERT INTO knowledge_points (id, course_id, name, description) VALUES
('00000000-0000-0000-0000-00000000cc40', '00000000-0000-0000-0000-00000000cc11',
 'CLS-CONF KP', 'Confidential knowledge point')
ON CONFLICT DO NOTHING;

INSERT INTO assessment_items (id, course_id, knowledge_point_id, type, stem, choices) VALUES
('00000000-0000-0000-0000-00000000cc50', '00000000-0000-0000-0000-00000000cc11',
 '00000000-0000-0000-0000-00000000cc40', 'SINGLE',
 'Confidential item stem',
 '["A","B","C","D"]')
ON CONFLICT DO NOTHING;
