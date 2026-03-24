# 📊 BricoliRent - Comprehensive Project Report

## 1. 🎯 What The Project Does (Project Overview)
**BricoliRent** is a Jakarta EE web application designed for renting professional tools. It streamlines the rental process by connecting three types of users:
- **Clients**: Can browse the catalog, reserve tools (one at a time, specifying quantity and period), and track their reservations.
- **Agents**: Responsible for processing the physical check-out of tools and managing returns, including calculating potential late penalty fees.
- **Administrators (Admins)**: Manage the system catalog, user accounts, and overall parameters.

The system automates the validation of reservations based on a "client score" and provides traceability for payments and rental operations.

---

## 2. ✅ What is Already Done (Implemented Features)
Based on an analysis of the codebase, the following features and layers are currently implemented:
- **Domain Layer**: Entities and Enums representing the core business logic (`User`, `Client`, `Tool`, `Reservation`, `Payment`).
- **Data Persistence**: A fully integrated Hibernate 6.4 ORM mapped to a PostgreSQL database, utilizing a Repository pattern for data access.
- **Business Service Layer**: Interfaces and their implementations managing the core rules (e.g., reservation validation, return processing).
- **Web Layer (JSF/CDI)**: Managed Beans are in place (`LoginBean`, `CategoryBean`, `ToolBean`, `ReservationBean`, `PaymentBean`, `ReturnBean`) to bridge the UI with the backend services.
- **Frontend MVC**: Basic `.xhtml` facelets are implemented, structural templates exist in `src/main/webapp/templates`, and application views reside in `src/main/webapp/app`.

---

## 3. 🚧 What is NOT Already Done (Pending/Missing Features)
The project is functional for its core requirements, but several modules are explicitly pending:
- **Security Module**: The `security` package (`com.bricolirent.security`) is currently perfectly empty (`.gitkeep`). Proper authentication, session management, and Role-Based Access Control (RBAC) filtering need to be implemented.
- **Application Configuration**: The `config` package is also empty. This means global application configurations (like email services, global constants, or scheduled tasks) are not yet centralized.
- **Robust Error Handling**: While the happy paths are implemented, global JSF exception handlers and comprehensive logging frameworks (e.g., SLF4J/Logback) are not explicitly set up in the provided structure.
- **Unit & Integration Tests**: The `src/test/java` directory is not heavily utilized or present, meaning comprehensive test coverage is missing for CI/CD pipelines.

---

## 4. 📁 Project Structure Analysis
The architecture follows a strict, layered MVC design using Jakarta EE standards:

* `com.bricolirent.domain`: Core entities and enumerations.
* `com.bricolirent.repository`: Direct Hibernate session management and CRUD operations.
* `com.bricolirent.service`: Business logic interfaces protecting the domain.
* `com.bricolirent.web`: CDI Managed Beans and standard JSF Converters.
* `src/main/webapp`: Contains standard deployment descriptors (`web.xml`, `beans.xml`), layouts (`templates/`), and active pages (`app/`).

**Tech Stack**: Java 17 | Maven | WildFly 30+ | JSF/Facelets | Jakarta CDI 4.1 | Hibernate 6.4 | PostgreSQL.

---

## 5. 🤝 How to Collaborate & Develop on This Project
To effectively contribute to BricoliRent, developers should adhere to the following workflow:

### A. Environment Setup
1. Ensure **Java 17**, **Maven 3.9+**, and **WildFly 30+** are properly installed.
2. Initialize the PostgreSQL database by running `CREATE DATABASE bricolirent;`.
3. Verify local configurations in `src/main/resources/hibernate.cfg.xml`.

### B. Development Workflow (Layered Approach)
When adding a new feature (e.g., "Invoices"), follow the strict architectural flow:
1. **Domain**: Create the `Invoice` Entity in `domain/entity`.
2. **Repository**: Create `InvoiceRepository` for database interactions.
3. **Service**: Create `InvoiceService` and `InvoiceServiceImpl` for business rules.
4. **Web/Controllers**: Create `InvoiceBean` (annotated `@Named` and `@ViewScoped`) injected with `InvoiceService`.
5. **UI**: Create the Facelet page `invoices.xhtml` in `webapp/app/` binding to `InvoiceBean`.

### C. Version Control & Contribution
- **Branching Strategy**: Use feature branches (`feature/add-invoices`, `fix/login-bug`) and merge them via Pull Requests to the `main` branch.
- **IDE Integration**: Rely on IntelliJ IDEA by opening the `pom.xml`. Configure a local *JBoss/WildFly* Run Configuration targeting the `bricolirent:war exploded` artifact to enable hot-deployments for `.xhtml` changes without full recompilation. 

---
_Generated locally by AI._
