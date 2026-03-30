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
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)
- [Repository](#repository)

## Architecture and Technologies
- **Backend**: Spring Boot 4.0 / Java 25 (utilizing Virtual Threads)
- **Frontend**: Angular 19+ (Standalone Architecture)
- **Database**: PostgreSQL 17
- **Migrations**: Liquibase
- **Containerization**: Docker & Docker Compose
- **API Pattern**: RESTful with DTO/Entity separation
- **Testing**: JUnit 5, Testcontainers

## Project Structure
LogisticsCompany/
├── 📂 .github/                   # GitHub Actions (CI/CD workflows)
├── 📂 frontend/                  # Angular 19+ Application Root
│   ├── 📂 src/
│   │   ├── 📂 app/               # Components, Services, Models
│   │   └── 📂 environments/      # API URL configurations
│   ├── 📄 angular.json           # Angular build settings
│   ├── 📄 package.json           # Node.js dependencies
│   └── 📄 proxy.conf.json        # Bridges Frontend (4200) to Backend (8080)
│
├── 📂 src/                       # Spring Boot 4.0 Application Root
│   ├── 📂 main/
│   │   ├── 📂 java/bg/nbu/cscb532/
│   │   │   ├── 📜 LogisticsCompanyApplication.java
│   │   │   ├── 📂 web/           # API Layer
│   │   │   │   ├── 📂 controller/
│   │   │   │   └── 📂 dto/       # Request/Response Objects
│   │   │   ├── 📂 service/       # Business Logic Layer
│   │   │   └── 📂 data/          # Persistence Layer
│   │   │       ├── 📂 entity/    # DB Table Mappings
│   │   │       └── 📂 repository/# DB Access Interfaces
│   │   │
│   │   └── 📂 resources/
│   │       ├── 📂 db/changelog/  # Liquibase migration files
│   │       ├── 📂 static/        # Where Angular 'dist' will be built
│   │       └── 📄 application.yaml # Backend Configuration
│   │
│   └── 📂 test/                  # JUnit 5 & Testcontainers
│
├── 📂 logs/                      # Application log files (Ignored by Git)
├── 📄 .env                       # Local secrets (DB_PASSWORD, etc.)
├── 📄 .gitignore                 # Essential: Keeps your repo clean
├── 📄 build.gradle               # Backend build script
├── 📄 compose.yaml                # Docker Compose (Postgres setup)
└── 📄 README.md                  # Project instructions & documentation

## Installation and Setup

1. **Infrastructure**:
    ```bash
    docker-compose up -d
    ```

2. **Run Backend**:
    Press 'Run' in IntelliJ IDEA (LogisticsCompanyApplication).

3. **Run Frontend**:
    ```bash
    cd frontend && npm start
    ```

---

## Contributing

As this is a university course project, contributing is generally not required.

## License

The project is licensed under the MIT License.

## Acknowledgments
- Developed as part of the **CSCB532 Practicum in Programming and Internet Technologies** course at [New Bulgarian University](https://nbu.bg/).
- Special thanks to the course instructor for creating the project requirements.

## Repository
GitHub Repository: [https://github.com/StefanYankov/logistics-company](https://github.com/StefanYankov/logistics-company)