-- V8__add_user_role_index.sql
-- Adds an index on meal_records(recorded_by) to support efficient USER-scoped
-- meal record queries (search, count, sum) introduced with the USER role.
--
-- No schema columns or constraints are added or changed.
-- The role column is VARCHAR(30) with no CHECK constraint, so adding USER
-- to the Java Role enum is sufficient — no DDL change is required for the role itself.
-- Existing data and all other tables are untouched.

CREATE INDEX IF NOT EXISTS idx_meal_records_recorded_by
    ON meal_records (recorded_by);
