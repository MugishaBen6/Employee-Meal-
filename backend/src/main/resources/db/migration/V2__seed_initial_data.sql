-- System Settings
INSERT INTO settings (setting_key, setting_value, description)
VALUES
('STANDARD_MEAL_PRICE', '1500.00', 'Standard meal price per employee in local currency'),
('COMPANY_NAME', 'Kigali Manufacturing Ltd', 'Company or Factory Name'),
('CURRENCY', 'RWF', 'System currency code'),
('TIMEZONE', 'Africa/Kigali', 'System timezone');

-- Initial Audit Logs
INSERT INTO audit_logs (user_id, username, user_role, action, entity_type, entity_id, description, ip_address)
VALUES
(NULL, 'SYSTEM', 'SYSTEM', 'SYSTEM_INITIALIZATION', 'SYSTEM', '1', 'System settings initialized successfully. Ready for initial administrator setup.', '127.0.0.1');
