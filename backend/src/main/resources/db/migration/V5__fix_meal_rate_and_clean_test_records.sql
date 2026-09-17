-- Normalize meal amounts to 600.00 RWF for all ATE records and 0.00 for DID_NOT_EAT
ALTER TABLE meal_records ALTER COLUMN amount SET DEFAULT 600.00;

UPDATE meal_records
SET amount = 600.00
WHERE meal_status = 'ATE';

UPDATE meal_records
SET amount = 0.00
WHERE meal_status = 'DID_NOT_EAT';

UPDATE settings
SET setting_value = '600.00'
WHERE setting_key = 'STANDARD_MEAL_PRICE';
