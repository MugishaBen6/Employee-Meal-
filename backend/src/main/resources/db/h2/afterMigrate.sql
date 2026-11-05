-- H2 test profile only (see application-h2.yml): H2 stops at V6 because V7 has
-- PostgreSQL-only indexes, so the additive columns from V9 are mirrored here.
ALTER TABLE meal_records ADD COLUMN IF NOT EXISTS source VARCHAR(20);
ALTER TABLE meal_records ADD COLUMN IF NOT EXISTS imported_at TIMESTAMP;
