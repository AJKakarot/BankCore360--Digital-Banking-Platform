# BankCore 360 — Educational Digital Banking Core

BankCore 360 is a modern, modular digital banking core platform built with **Java 21** and **Spring Boot 3**. Designed for educational and enterprise-readiness exploration, it emphasizes clean layered architecture, robust domain modeling, resilience patterns, and secure financial data handling.

---

## 🏛 Layered Architecture

BankCore 360 follows a strict separation of concerns with standard Spring enterprise layering under `com.bankcore360`:

```
com.bankcore360/
├── config/             # Spring configuration (CORS, Security, WebMvc)
├── controller/         # REST API endpoints & Web controllers
├── dto/                # Request / Response Data Transfer Objects (Records)
├── entity/             # JPA Entities representing core domain models
├── exception/          # Centralized exception handling & error models
├── repository/         # Spring Data JPA persistence interfaces
├── service/            # Core business logic and transaction boundaries
└── BankCore360Application.java  # Main Spring Boot application entry point
```

---

## 🚀 Tech Stack

- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.4.3
- **Build Tool**: Maven (with Maven Wrapper `mvnw` included)
- **Web**: Spring Web (RESTful API)
- **Persistence**: Spring Data JPA & Hibernate
- **Database**: PostgreSQL
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **Developer Experience**: Spring Boot DevTools
- **Testing**: JUnit 5, MockMvc, AssertJ

---

## 📋 Prerequisites

Before running the application, ensure you have:
- **Java 21** or later installed (`java -version`)
- **PostgreSQL 14+** running (optional for initial health check, required for database entities)
- **Git**

---

## ⚙️ Configuration & Environment Variables

BankCore 360 adheres to the **Twelve-Factor App** methodology: **no database credentials or secrets are hardcoded**.

All configuration is externalized via `src/main/resources/application.yml` and can be overridden via environment variables or a `.env` file:

| Variable | Default Value | Description |
|---|---|---|
| `PORT` | `8080` | HTTP port for the web server |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `bankcore360` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `DB_URL` | *(auto-derived from host/port/name)* | Custom JDBC connection URL override |
| `JPA_DDL_AUTO` | `none` | Hibernate DDL strategy (`none`, `validate`, `update`) |
| `SHOW_SQL` | `false` | Enable SQL query logging |

An environment template is provided in `.env.example`.

---

## 🛠️ Building & Running

### 1. Compile the Project
Using the included Maven wrapper:
```bash
./mvnw clean compile
```

Or with your local Maven installation:
```bash
mvn clean compile
```

### 2. Run Tests
```bash
./mvnw test
```

### 3. Run the Application Locally
You can run the application directly with Spring Boot:
```bash
./mvnw spring-boot:run
```

With custom database environment variables:
```bash
DB_HOST=localhost DB_PORT=5432 DB_NAME=bankcore360 DB_USERNAME=myuser DB_PASSWORD=secret ./mvnw spring-boot:run
```

### 4. Build a Production JAR
```bash
./mvnw clean package -DskipTests
java -jar target/bankcore360-0.0.1-SNAPSHOT.jar
```

---

## 🩺 API Endpoints (Initial Bootstrap)

### Health Check
Returns the operational status of the service.

- **URL**: `/api/health`
- **Method**: `GET`
- **Headers**: `Accept: application/json`
- **Response Status**: `200 OK`
- **Response Body**:
```json
{
  "status": "UP",
  "application": "BankCore360"
}
```

#### Test with cURL:
```bash
curl -i http://localhost:8080/api/health
```

---

## 🗺️ Roadmap

- [ ] **Phase 2**: Customer & Account domain entities (Checking, Savings)
- [ ] **Phase 3**: Double-entry ledger and money movement engine
- [ ] **Phase 4**: Spring Security, JWT authentication & RBAC
- [x] **Phase 5**: Transactional Outbox Event Architecture with PostgreSQL Audit Trails
- [ ] **Phase 6**: AI Financial Assistant & Transaction Enrichment
