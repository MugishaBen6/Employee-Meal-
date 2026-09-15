-- V4: Performance Optimization Indexes

-- Index on phone for fast duplicate checks and search
CREATE INDEX IF NOT EXISTS idx_employees_phone ON employees(phone);

-- Composite index on employee status and department for fast filtered pagination
CREATE INDEX IF NOT EXISTS idx_employees_status_dept ON employees(status, department);

-- Composite index on meal records for high-speed date and status aggregations
CREATE INDEX IF NOT EXISTS idx_meal_date_status ON meal_records(meal_date, meal_status);

-- Composite index on meal records for quick check and attendance lookups
CREATE INDEX IF NOT EXISTS idx_meal_emp_date ON meal_records(employee_id, meal_date);
