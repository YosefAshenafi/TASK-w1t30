-- Dev users
INSERT INTO users (id, username, display_name, password_bcrypt, role, status, organization_id) VALUES
('00000000-0000-0000-0000-000000000101', 'student1',  'Alice Student',   crypt('Test@123!', gen_salt('bf', 12)), 'STUDENT',          'ACTIVE', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000102', 'mentor1',   'Bob Mentor',      crypt('Test@123!', gen_salt('bf', 12)), 'CORPORATE_MENTOR', 'ACTIVE', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000103', 'faculty1',  'Carol Faculty',   crypt('Test@123!', gen_salt('bf', 12)), 'FACULTY_MENTOR',   'ACTIVE', NULL),
('00000000-0000-0000-0000-000000000104', 'student2',  'Dave Student',    crypt('Test@123!', gen_salt('bf', 12)), 'STUDENT',          'ACTIVE', '00000000-0000-0000-0000-000000000001');

-- Courses
INSERT INTO courses (id, code, title, version, location_id, instructor_id, classification) VALUES
('00000000-0000-0000-0000-000000000200', 'CPR-101',  'Basic CPR & First Aid',           '2024.1', '00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000020', 'PUBLIC'),
('00000000-0000-0000-0000-000000000201', 'FIRE-201', 'Fire Safety & Suppression',       '2024.1', '00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000020', 'INTERNAL'),
('00000000-0000-0000-0000-000000000202', 'HAZMAT-301','Hazardous Materials Handling',   '2024.1', '00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000020', 'CONFIDENTIAL');

-- Cohorts
INSERT INTO cohorts (id, course_id, name, total_seats, starts_at, ends_at) VALUES
('00000000-0000-0000-0000-000000000300', '00000000-0000-0000-0000-000000000200', 'CPR-101 Spring 2025',   20, '2025-03-01 09:00:00+00', '2025-03-02 17:00:00+00'),
('00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000200', 'CPR-101 Summer 2025',  20, '2025-06-01 09:00:00+00', '2025-06-02 17:00:00+00'),
('00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000201', 'FIRE-201 Spring 2025', 15, '2025-04-10 09:00:00+00', '2025-04-11 17:00:00+00');

-- Knowledge points
INSERT INTO knowledge_points (id, course_id, name, description) VALUES
('00000000-0000-0000-0000-000000000400', '00000000-0000-0000-0000-000000000200', 'Chest Compressions',     'Proper technique for chest compressions'),
('00000000-0000-0000-0000-000000000401', '00000000-0000-0000-0000-000000000200', 'Rescue Breathing',       'Airway management and rescue breathing'),
('00000000-0000-0000-0000-000000000402', '00000000-0000-0000-0000-000000000200', 'AED Operation',          'Automated external defibrillator usage'),
('00000000-0000-0000-0000-000000000403', '00000000-0000-0000-0000-000000000201', 'Fire Classification',    'Types of fires and appropriate extinguishers'),
('00000000-0000-0000-0000-000000000404', '00000000-0000-0000-0000-000000000201', 'Evacuation Procedures',  'Building evacuation routes and assembly points');

-- Activities
INSERT INTO activities (id, course_id, name, description, sort_order) VALUES
('00000000-0000-0000-0000-000000000500', '00000000-0000-0000-0000-000000000200', 'CPR Mannequin Practice',   'Hands-on CPR on training mannequin',      1),
('00000000-0000-0000-0000-000000000501', '00000000-0000-0000-0000-000000000200', 'AED Trainer Simulation',   'Practice with AED trainer device',        2),
('00000000-0000-0000-0000-000000000502', '00000000-0000-0000-0000-000000000200', 'Scenario Drill',           'Full scenario combining CPR and AED',     3),
('00000000-0000-0000-0000-000000000503', '00000000-0000-0000-0000-000000000201', 'Extinguisher Operation',   'Hands-on fire extinguisher practice',     1),
('00000000-0000-0000-0000-000000000504', '00000000-0000-0000-0000-000000000201', 'Evacuation Walk-through',  'Guided evacuation route familiarization', 2);

-- Assessment items
INSERT INTO assessment_items (id, course_id, knowledge_point_id, type, stem, choices) VALUES
('00000000-0000-0000-0000-000000000600', '00000000-0000-0000-0000-000000000200', '00000000-0000-0000-0000-000000000400', 'SINGLE',
 'What is the correct compression rate per minute for adult CPR?',
 '["60-80","80-100","100-120","120-140"]'),
('00000000-0000-0000-0000-000000000601', '00000000-0000-0000-0000-000000000200', '00000000-0000-0000-0000-000000000400', 'SINGLE',
 'How deep should chest compressions be for an adult?',
 '["1 inch","1.5 inches","2-2.4 inches","3 inches"]'),
('00000000-0000-0000-0000-000000000602', '00000000-0000-0000-0000-000000000200', '00000000-0000-0000-0000-000000000402', 'SINGLE',
 'When should you NOT use an AED?',
 '["When the patient is wet","When the patient has a pacemaker","When the patient is unconscious","When no pulse is detected"]'),
('00000000-0000-0000-0000-000000000603', '00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000403', 'SINGLE',
 'Which fire extinguisher class is used for electrical fires?',
 '["Class A","Class B","Class C","Class D"]');

-- Enrollments
INSERT INTO enrollments (id, student_id, cohort_id, organization_id) VALUES
('00000000-0000-0000-0000-000000000700', '00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000300', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000701', '00000000-0000-0000-0000-000000000104', '00000000-0000-0000-0000-000000000300', '00000000-0000-0000-0000-000000000001');
