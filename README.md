> [!IMPORTANT]
> **Project Status: Active Development**
> This repository contains the final project for the CSCB532 Practicum in Programming and Internet Technologies course (Autumn Semester 2021/2022).

---
# Logistics Company System

## Overview

This project is the **final project** for the university course:
**[CSCB532 Practicum in Programming and Internet Technologies - Autumn Semester 2021/2022](https://ecatalog.nbu.bg/default.asp?V_Year=2021&YSem=5&Spec_ID=&Mod_ID=&PageShow=coursepresent&P_Menu=courses_part2&Fac_ID=3&M_PHD=&P_ID=832&TabIndex=&K_ID=50301&K_TypeID=77&l=1)**.

## Project Introduction

The **Logistics Company System** is a Java-based web application developed as a final project for the **CSCB532** course at the **New Bulgarian University**. It provides a centralized platform for managing the core processes of a logistics provider, including the acceptance, tracking, and delivery of shipments. The system supports various user roles (Client, Employee, and Admin) with distinct access levels, ensuring secure management of offices, couriers, and customer data.

## Table of Contents
- [Architecture & Technologies](#architecture-and-technologies)
- [Installation & Setup](#installation-and-setup)
- [Implemented Features](#implemented-features)
- [Project Structure](#project-structure)
- [Test Credentials](#test-credentials)
- [License](#license)
- [Acknowledgments](#acknowledgments)
- [Repository](#repository)

## Architecture and Technologies
- **Backend**: Spring Boot 4.0 / Java 25 (utilizing Virtual Threads)
- **Frontend**: Angular 19+ (Standalone Architecture)
- **Database**: PostgreSQL 17
- **Migrations**: Flyway (Seeds reference data on startup)
- **Containerization**: Docker & Docker Compose
- **Security**: Stateless JWT Authentication with Spring Security 7
- **API Pattern**: RESTful with DTO/Entity separation, OpenAPI (Swagger) for documentation and client generation
- **Testing**: JUnit 5, Testcontainers, Mockito, AssertJ (Backend); Vitest, Angular Testing Library (Frontend)

## Implemented Features

The project is being developed using a strict **Domain-Driven Design (DDD)** approach, organized into vertical slices:

*   **System Foundation**:
    *   Centralized RFC 9457 ProblemDetail exception handling (`GlobalExceptionHandler`).
    *   JSR-380 input validation.
    *   Automated Flyway database migrations (including Cities, Offices, and Service Catalog seed data).
*   **Company Domain**:
    *   CRUD operations for the core `Company` entity.
    *   Data integrity checks (Unique registration numbers).
*   **Office & City Domain**:
    *   Hierarchical office management (Cities -> Offices).
    *   Geospatial integration (Latitude/Longitude on addresses).
*   **Identity & Access Management (IAM)**:
    *   Full Spring Security integration.
    *   Stateless JWT (JSON Web Token) generation and validation with enriched claims (userId, role).
    *   Role-based access control (`CLIENT`, `COURIER`, `CLERK`, `ADMIN`).
    *   Secure email verification and single-use password recovery tokens.
*   **Client Domain**:
    *   Public self-registration endpoint for new customers.
    *   Secure password hashing (BCrypt).
    *   Proactive duplication checks (username, email).
*   **Employee Domain**:
    *   Polymorphic user types (`OfficeClerk` assigned to an office, mobile `Courier`).
    *   Admin-only management endpoints.
*   **Shipment Domain**:
    *   Full shipment registration workflow (accessible to staff and clients).
    *   **Service Addons**: Flexible addon system (e.g., "Fragile", "Express") with dynamic Fixed/Percentage pricing.
    *   Dynamic, versioned pricing engine for calculating shipping costs based on weight, distance, and addons.
    *   Shipment lifecycle management via a State Machine.
    *   Comprehensive audit trail via `ShipmentStatusHistory`.
    *   Public tracking number lookup.
    *   Client-specific access to their sent and received shipments.
*   **Reporting Domain**:
    *   Aggregate revenue reporting for administrators over custom date ranges.

## Frontend Application Structure & Features

The Angular frontend is built with a standalone component architecture and follows a modular, feature-driven structure based on user roles:

*   **Core Authentication:**
    *   `AuthService`: Manages JWT tokens, user login/logout, and token decoding.
    *   `AuthInterceptor`: Automatically attaches JWT to all outgoing API requests.
    *   `AuthGuard`: Protects authenticated routes.
*   **Public Zone (`features/public` & `features/auth`):**
    *   `PublicLayout`: Provides a shared `PublicHeader` (with Login/Register links) and footer for unauthenticated users.
    *   `Home`: Landing page with a public shipment tracking search bar.
    *   `Login`: User authentication form.
    *   `Register`: New client registration form.
    *   `Tracking`: Displays public shipment details by tracking number.
*   **Authenticated Zone (`layouts/authenticated-layout`):**
    *   `AuthenticatedLayout`: Provides a shared sidebar navigation and a smart `AuthenticatedHeader` (with conditional navigation and Logout) for authenticated users.
*   **Role-Based Feature Modules:**
    *   **Client (`features/client`):**
        *   `ClientDashboard`: Client-specific view displaying sent and received shipments.
    *   **Courier (`features/courier`):**
        *   `CourierDashboard`: Dedicated view showing assigned "My Deliveries" and "My Pickups".
    *   **Shipments & Operations (`features/shipments`):**
        *   `ClientRegistration` & `ClerkRegistration`: Role-specific forms for registering new shipments, including sender lookup and addon selection.
        *   `ShipmentList`: Master operational view for staff to see all shipments, apply addons, assign pickups to couriers, and update lifecycle statuses.

## Project Structure
The project follows a **Package-by-Feature** (Modular Monolith) structure to ensure high cohesion and prepare for potential future microservice extraction.

```
LogisticsCompany/
├── 📂 .github/                   # GitHub Actions (CI/CD workflows)
├── 📂 frontend/                  # Angular 19+ Application Root
│   ├── 📂 src/app/api/          # Auto-generated OpenAPI client services and models
│   ├── 📂 src/app/features/     # Role-based feature modules (client, courier, public, shipments)
│   ├── 📂 src/app/layouts/      # Shared layout components (public, authenticated)
│   ├── 📂 src/app/shared/       # Cross-cutting concerns (auth.service, auth.guard, ui components)
│   └── ...
├── 📂 src/                       # Spring Boot 4.0 Application Root
│   ├── 📂 main/
│   │   ├── 📂 java/bg/nbu/cscb532/
│   │   │   ├── 📜 LogisticsCompanyApplication.java
│   │   │   ├── 📂 client/        # Client registration & management
│   │   │   ├── 📂 company/       # Core logistics company details
│   │   │   ├── 📂 employee/      # Staff (Couriers, Clerks) management
│   │   │   ├── 📂 office/        # Physical locations and cities
│   │   │   ├── 📂 shared/        # Cross-cutting concerns (Security, Exceptions, Config)
│   │   │   ├── 📂 shipment/      # Core logistics process & Addon Pricing
│   │   │   └── 📂 user/          # IAM, Authentication, JWT logic
│   │   └── 📂 resources/
│   │       ├── 📂 db/migration/  # Flyway SQL scripts
│   │       └── 📄 application.yaml # Backend Configuration
│   │
│   └── 📂 test/                  # Comprehensive Test Suite
│       └── 📂 java/bg/nbu/cscb532/
│           └── ...               # Unit, Web Slice, and Data Slice tests per domain
│
├── 📄 build.gradle               # Backend build script
├── 📄 compose.yaml               # Docker Compose (Postgres setup)
└── 📄 README.md                  # Project instructions & documentation
```

## Installation and Setup

1. **Infrastructure**:
    ```bash
    docker-compose up -d
    ```

2. **Run Backend**:
    *   Open the project in IntelliJ IDEA.
    *   Run the `LogisticsCompanyApplication.java` file.
    *   Ensure the backend is running on `http://localhost:8080`.
    *   *Note: Flyway will automatically seed reference data on startup.*

3. **Generate Frontend API Client**:
    *   Navigate to the `frontend/` directory in your terminal.
    *   Run `npm install` to install dependencies.
    *   **CRITICAL:** Run `npm run generate-api` to create the TypeScript API client from the running backend. This must be done whenever the backend API contract changes.

4. **Run Frontend**:
    *   Navigate to the `frontend/` directory in your terminal.
    *   Run `npm start`.
    *   Open your browser to `http://localhost:4200`.

## Test Credentials

The `DataSeeder.java` class automatically creates the following staff accounts on startup with the password `password123`:
*   **Admin:** `admin`
*   **Clerk:** `clerk`
*   **Courier:** `courier`

---

## License

The project is licensed under the MIT License.

## Acknowledgments
- Developed as part of the **CSCB532 Practicum in Programming and Internet Technologies** course at [New Bulgarian University](https://nbu.bg/).
- Special thanks to the course instructor for creating the project requirements.

## Repository
GitHub Repository: [https://github.com/StefanYankov/logistics-company](https://github.com/StefanYankov/logistics-company)
