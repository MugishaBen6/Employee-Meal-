# System Architecture - Employee Meal Management System

## Architectural Overview

```
[ Frontend: React 18 + TS + Vite ]  <--- HTTP / REST JSON + JWT --->  [ Backend: Spring Boot 3 ]  <---> [ Database: PostgreSQL 16 ]
                                                                                │
                                                                                ├── Security Filter (JWT + RBAC)
                                                                                ├── Service Layer & DTOs
                                                                                └── Apache POI / OpenPDF Generators
```

### Key Security & Business Logic Highlights
1. **Stateless JWT Security**: Passwords stored via BCrypt. Requests authenticated via Bearer token in HTTP Headers.
2. **Strict Duplicate Meal Constraint**: Database level `UNIQUE(employee_id, meal_date)` constraint combined with service-level pre-checks.
3. **Soft Delete / Deactivation**: Employees are marked `INACTIVE` so historical meal transactions and audit reports remain 100% accurate.
4. **Audit Trail Logging**: Every data modification (user login, employee update, meal recording, setting update) is stored in `audit_logs`.
