-- Clean up any test meal records created outside the imported August 2026 attendance sheet
DELETE FROM meal_records 
WHERE meal_date > '2026-08-31';

-- Ensure all remaining ATE records are set to 600.00 RWF
UPDATE meal_records
SET amount = 600.00
WHERE meal_status = 'ATE';

-- Ensure all DID_NOT_EAT records are 0.00 RWF
UPDATE meal_records
SET amount = 0.00
WHERE meal_status = 'DID_NOT_EAT';
