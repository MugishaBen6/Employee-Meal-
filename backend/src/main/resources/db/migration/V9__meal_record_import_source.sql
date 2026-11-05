-- Distinguishes how a meal record entered the system, without touching existing rows.
--   meal_date   : the actual day the employee ate (unchanged, used by every report)
--   created_at  : when the row was first written
--   imported_at : when the row was last written by an Excel import (NULL for manual records)
--   source      : 'MANUAL' or 'IMPORT' (NULL for rows created before this migration)
-- Additive and nullable only: no data is modified or deleted.
ALTER TABLE meal_records ADD COLUMN IF NOT EXISTS source VARCHAR(20);
ALTER TABLE meal_records ADD COLUMN IF NOT EXISTS imported_at TIMESTAMP;
