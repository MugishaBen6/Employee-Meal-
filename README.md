# Employee Meal Management System

A production-ready full-stack enterprise **Employee Meal Management System** built with **Spring Boot 3**, **PostgreSQL**, **Flyway**, **Apache POI**, **OpenPDF**, **React 18**, **TypeScript**, **Vite**, and **Tailwind CSS**.

---

## Features

- **Role-Based Access Control (RBAC)**: Supports `ADMIN`, `MANAGING_DIRECTOR`, `ACCOUNTANT`, and `HR` roles with secure JWT authentication and BCrypt password hashing.
- **Employee Management**: Full CRUD operations with employee code validation, department tracking, and soft-delete/deactivation.
- **Fast Meal Recording Interface**: High-efficiency lunchtime queue workflow featuring instant employee lookup, default meal pricing (1,500 RWF), 1-click confirmation, toast notifications, and automatic search resetting.
- **Duplicate Meal Prevention**: Enforces database-level `UNIQUE(employee_id, meal_date)` constraints to prevent double meal recording.
- **Financial & Attendance Dashboards**: Real-time business metrics from PostgreSQL, including active employees count, ate today, did not eat today, daily/weekly/monthly cost totals, and Recharts interactive graphs.
- **Excel & PDF Report Engine**: Daily, weekly, and monthly report generation powered by Apache POI (`.xlsx`) and OpenPDF (`.pdf`).
- **Comprehensive Audit Trail**: Automatically logs all user logins, employee edits, meal entries, and setting modifications to `audit_logs`.
- **System Configuration**: Dynamic configuration for standard meal prices (default 1,500 RWF), company name, local currency (`RWF`), and timezone (`Africa/Kigali`).

---

## Technology Stack

### Backend
- **Framework**: Java 17+ / Spring Boot 3.2
- **Security**: Spring Security + Stateless JWT
- **ORM / Database**: Spring Data JPA + PostgreSQL 16
- **Migrations**: Flyway Migration Scripts (`V1__initial_schema.sql`, `V2__seed_initial_data.sql`)
- **Reporting**: Apache POI (Excel) & OpenPDF (PDF)
- **Build Tool**: Apache Maven

### Frontend
- **Framework**: React 18 + TypeScript + Vite
- **Styling**: Tailwind CSS + Lucide Icons
- **Data Visualization**: Recharts
- **HTTP Client**: Axios with JWT interceptors
- **State & Router**: React Context + React Router v6

---

## System Architecture

```
employee-meal-management-system/
├── backend/                  # Spring Boot 3 Java Application
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/emeal/
│       │   ├── config/       # JwtUtils, JwtAuthFilter, SecurityConfig, CorsConfig
│       │   ├── controller/   # Auth, Employee, MealRecord, Dashboard, Report, Expense, Audit, Users, Settings
│       │   ├── dto/          # Request & Response DTOs
│       │   ├── entity/       # JPA Entities (User, Employee, MealRecord, AuditLog, Settings)
│       │   ├── repository/   # Spring Data JPA Repositories
│       │   └── service/      # Business logic & Apache POI/OpenPDF report generators
│       └── resources/        # application.yml & Flyway migration scripts
├── frontend/                 # React 18 + TypeScript + Vite + Tailwind CSS Application
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/              # Axios API modules
│       ├── components/       # Layouts, Sidebar, Navbar, Modal, Toast, Skeleton, Badge, Card
│       ├── context/          # AuthContext with JWT & RBAC management
│       ├── pages/            # Login, Dashboard, Employees, MealRecording, Reports, Expenses, AuditLogs, Users, Settings
│       └── types/            # TypeScript interfaces
├── database/
│   ├── schema/schema.sql     # Reference DDL Schema
│   └── seed/seed.sql         # Reference Initial Data Seed
└── docs/                     # API documentation, Database design, System architecture
```

---

## Prerequisites

- **Java Development Kit (JDK)** 17 or higher
- **Node.js** v18+ & **npm** v9+
- **PostgreSQL** 14+ running on port `5432`
- **Apache Maven** 3.8+

---

## Database Setup

1. Create a PostgreSQL database named `employee_meal_db`:
   ```bash
   createdb -U postgres employee_meal_db
   ```
2. Flyway will automatically run `V1__initial_schema.sql` and `V2__seed_initial_data.sql` when the Spring Boot backend starts up.

---

## How to Run

### 1. Start Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```
The API server will launch on `http://localhost:8080/api`.

### 2. Start Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```
The application will launch on `http://localhost:5173`.

---

## Pre-loaded Development Login Credentials

| Role | Username | Password | Default Redirect |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin` | `Password123!` | `/admin/dashboard` |
| **Managing Director** | `director` | `Password123!` | `/director/dashboard` |
| **Senior Accountant** | `accountant1` | `Password123!` | `/accountant/dashboard` |
| **Accountant 2** | `accountant2` | `Password123!` | `/accountant/dashboard` |
| **Accountant 3** | `accountant3` | `Password123!` | `/accountant/dashboard` |
| **HR Manager** | `hr` | `Password123!` | `/hr/dashboard` |

---

## API Overview

- `POST /api/auth/login` - Authenticate user & receive JWT token
- `GET /api/employees` - Search employees with department & status filter
- `POST /api/employees` - Create new employee
- `GET /api/meals/quick-check` - Fast lookup for meal recording
- `POST /api/meals` - Record daily meal (with duplicate date prevention)
- `GET /api/reports/daily` - Get daily summary statistics
- `GET /api/reports/daily/excel` - Download Daily Excel (`.xlsx`) Report
- `GET /api/reports/daily/pdf` - Download Daily PDF (`.pdf`) Report
- `GET /api/dashboard/statistics` - Real-time statistics from PostgreSQL
- `GET /api/audit-logs` - Query audit activity trail
