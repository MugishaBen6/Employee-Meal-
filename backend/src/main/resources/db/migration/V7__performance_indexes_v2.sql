-- V7__performance_indexes_v2.sql
-- High performance functional and composite indexes for fast logins, dashboard aggregations, and employee lookups

-- 1. Fast login lookups (case-insensitive username and email)
CREATE INDEX IF NOT EXISTS idx_users_lower_username ON users (LOWER(username));
CREATE INDEX IF NOT EXISTS idx_users_lower_email ON users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_role_status ON users (role, status);

-- 2. Fast dashboard and meal recording queries
CREATE INDEX IF NOT EXISTS idx_meal_records_date_status ON meal_records (meal_date, meal_status);
CREATE INDEX IF NOT EXISTS idx_meal_records_date_emp_status ON meal_records (meal_date, employee_id, meal_status);
CREATE INDEX IF NOT EXISTS idx_meal_records_emp_date ON meal_records (employee_id, meal_date DESC);

-- 3. Fast employee department and status filtering
CREATE INDEX IF NOT EXISTS idx_employees_status_dept ON employees (status, department);
CREATE INDEX IF NOT EXISTS idx_employees_dept ON employees (department);

-- 4. Fast recent activities and audit log queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp_desc ON audit_logs (timestamp DESC);
