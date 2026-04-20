-- Notification templates for the quarterly recovery drill and registration
-- SLA escalation features. Added in a separate migration so existing
-- environments that already ran V100 pick them up via a forward migration.

INSERT INTO notification_templates (key, title_tmpl, body_tmpl) VALUES
('backup.drillOverdue',   'Quarterly Recovery Drill Overdue',
 '{{message}}'),
('registration.slaOverdue', 'Registration Approval Overdue',
 'Registration for user {{username}} has been pending for {{hours}} hours and exceeds the SLA.')
ON CONFLICT (key) DO NOTHING;
