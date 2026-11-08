-- H2 test profile only (see application-h2.yml): H2 stops at V6 because V7 has
-- PostgreSQL-only indexes, so the additive columns from V9 are mirrored here.
ALTER TABLE meal_records ADD COLUMN IF NOT EXISTS source VARCHAR(20);
ALTER TABLE meal_records ADD COLUMN IF NOT EXISTS imported_at TIMESTAMP;
-- Mirrors V10 for the H2 test profile.
ALTER TABLE employees ADD COLUMN IF NOT EXISTS created_by VARCHAR(50);
CREATE TABLE IF NOT EXISTS trash_items (
    id BIGSERIAL PRIMARY KEY,
    item_type VARCHAR(20) NOT NULL,
    original_id BIGINT NOT NULL,
    employee_id BIGINT,
    employee_code VARCHAR(50),
    employee_name VARCHAR(120),
    meal_date DATE,
    meal_status VARCHAR(20),
    meal_count INTEGER NOT NULL DEFAULT 0,
    meal_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    payload TEXT NOT NULL,
    deleted_by VARCHAR(50) NOT NULL,
    deleted_at TIMESTAMP NOT NULL,
    batch_id VARCHAR(40)
);
