# Database Design - Employee Meal Management System

## Overview
PostgreSQL database designed with 3NF normalization, foreign key integrity constraints, and Flyway migration scripts.

## Entity Relationship Diagram (ERD)

```
[ users ]
- id (PK)
- username (UNIQUE)
- email (UNIQUE)
- password (BCrypt)
- role (ADMIN, MANAGING_DIRECTOR, ACCOUNTANT, HR)
- status (ACTIVE, INACTIVE)

[ employees ]
- id (PK)
- employee_code (UNIQUE)
- first_name
- last_name
- department
- position
- phone
- email
- status (ACTIVE, INACTIVE)

[ meal_records ]
- id (PK)
- employee_id (FK -> employees.id)
- meal_date (DATE)
- meal_status (ATE, DID_NOT_EAT)
- amount (NUMERIC(12, 2))
- recorded_by
* UNIQUE CONSTRAINT: (employee_id, meal_date)

[ audit_logs ]
- id (PK)
- user_id
- username
- user_role
- action
- entity_type
- entity_id
- description
- timestamp

[ settings ]
- id (PK)
- setting_key (UNIQUE)
- setting_value
- description
```
