-- Recycle Bin for deleted employees and meal records, plus who created each employee.
-- Additive only: no existing row is changed or removed.

-- Username that created the employee (NULL for employees created before V10).
-- Lets a USER correct the name/position of employees they added themselves.
ALTER TABLE employees ADD COLUMN IF NOT EXISTS created_by VARCHAR(50);

-- A deleted record is moved here as a complete snapshot (an employee together with all of its
-- meal records) and removed from the active tables, so it can never be counted in any list,
-- dashboard, report or export until it is restored with its original values.
CREATE TABLE IF NOT EXISTS trash_items (
    id BIGSERIAL PRIMARY KEY,
    item_type VARCHAR(20) NOT NULL,          -- EMPLOYEE | MEAL_RECORD
    original_id BIGINT NOT NULL,
    employee_id BIGINT,
    employee_code VARCHAR(50),
    employee_name VARCHAR(120),
    meal_date DATE,
    meal_status VARCHAR(20),
    meal_count INTEGER NOT NULL DEFAULT 0,   -- meal records included in the snapshot
    meal_amount NUMERIC(14, 2) NOT NULL DEFAULT 0, -- sum of ATE amounts in the snapshot
    payload TEXT NOT NULL,                   -- JSON snapshot used for an exact restore
    deleted_by VARCHAR(50) NOT NULL,
    deleted_at TIMESTAMP NOT NULL,
    batch_id VARCHAR(40)
);
CREATE INDEX IF NOT EXISTS idx_trash_items_deleted_at ON trash_items (deleted_at DESC);
CREATE INDEX IF NOT EXISTS idx_trash_items_type ON trash_items (item_type);
