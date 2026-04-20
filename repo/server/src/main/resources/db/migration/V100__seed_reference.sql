INSERT INTO notification_templates (key, title_tmpl, body_tmpl) VALUES
('export.ready',        'Export Ready',                    'Your export {{filename}} is ready for download.'),
('export.failed',       'Export Failed',                   'Your export request failed: {{reason}}.'),
('anomaly.newDevice',   'New Device Login',                'A login from a new device was detected on your account.'),
('anomaly.ipOutOfRange','Login from Unexpected IP',        'A login from IP {{ip}} is outside your allowed IP ranges.'),
('anomaly.exportBurst', 'Unusual Export Activity',         'An unusually high number of exports was detected on your account.'),
('approval.requested',  'Approval Request',                'A new {{type}} approval request has been submitted by {{requester}}.'),
('approval.decided',    'Approval Request Decided',        'Your approval request for {{type}} has been {{decision}}.'),
('cert.expiring30',     'Certification Expiring Soon',     'Your certification for {{course}} expires in 30 days.'),
('cert.expiring60',     'Certification Expiring in 60 Days','Your certification for {{course}} expires in 60 days.'),
('cert.expiring90',     'Certification Expiring in 90 Days','Your certification for {{course}} expires in 90 days.');

INSERT INTO organizations (id, name, code) VALUES
('00000000-0000-0000-0000-000000000001', 'Meridian Internal', 'MERIDIAN');

INSERT INTO locations (id, name, address) VALUES
('00000000-0000-0000-0000-000000000010', 'Main Training Center', '1 Training Way, Anytown, USA');

INSERT INTO instructors (id, name, email) VALUES
('00000000-0000-0000-0000-000000000020', 'Default Instructor', 'instructor@meridian.local');
