# BankCore360 — Enterprise Digital Banking Platform

BankCore360 is an enterprise-grade digital banking core and financial engineering platform built with **Java 21**, **Spring Boot 3.4**, **PostgreSQL**, **Redis**, **Apache Kafka**, and **React / Next.js**. Engineered as a high-performance **Modular Monolith**, it incorporates advanced financial invariants, immutable accounting, and real-time distributed fraud prevention.

---

## 🏛 Core Financial Engineering Modules

### Module 1: Double-Entry Accounting Ledger System
- **Strict Invariant:** Every financial transaction produces balanced debit and credit entries where $\sum \text{Debit} == \sum \text{Credit}$.
- **Normal Balance Formula:** 
  - `ASSET` & `EXPENSE`: Debit Normal (Debits increase, Credits decrease)
  - `LIABILITY`, `EQUITY` & `REVENUE`: Credit Normal (Credits increase, Debits decrease)
- **Immutable Entries:** Posted records are permanent (`updatable = false`). Corrections use audit-linked compensating `REVERSAL` transactions.
- **Deadlock-Free Concurrency:** Pessimistic write locking (`SELECT ... FOR UPDATE`) enforces deterministic ascending account ID lock ordering (`min(idA, idB)` followed by `max(idA, idB)`).
- **Zero Floating-Point Drift:** Strict `BigDecimal` (precision 19, scale 4) throughout.

### Module 2: Real-Time Fraud Detection Engine & Transactional Outbox
- **Pre-Settlement Interceptor:** Evaluates transactions prior to ledger lock acquisition.
- **Dynamic Rule Engine:** Database-driven rules (`fraud_rules`) for large amount spikes, hourly volume thresholds, and burst attempts.
- **Redis Sliding-Window Velocity:** High-throughput sliding-window transaction counters and velocity tracking via Redis Sorted Sets (`ZSET`).
- **Risk Classification:**
  - `0 – 30`: Low Risk (Auto-approved)
  - `31 – 60`: Medium Risk (Auto-approved with telemetry logging)
  - `61 – 80`: High Risk (Held in `PENDING_REVIEW`; alert generated for compliance)
  - `81 – 100`: Critical Risk (Hard blocked with HTTP 403; account flagged)
- **Transactional Outbox Pattern:** Guarantees reliable, at-least-once event delivery to **Apache Kafka** (`bankcore360.transactions.*`, `bankcore360.fraud.alerts`) without dual-write inconsistency.
- **Compliance Center:** Full frontend investigation portal (`/admin/fraud`) with live evaluation simulator and 1-click status transitions.

---

## 🚀 Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Core** | Java 21 (LTS), Spring Boot 3.4.3, Spring Security, Spring Data JPA |
| **In-Memory & Cache** | Redis 7 Alpine, Spring Data Redis, Lettuce Client |
| **Event Streaming** | Apache Kafka (KRaft Mode), Spring Kafka, Transactional Outbox |
| **Relational Database** | PostgreSQL 16 (Neon Cloud / Local Docker) |
| **Frontend UI** | Next.js 16 (App Router), React 19, Tailwind CSS, Lucide Icons |
| **AI Service** | Python 3.11, FastAPI, LangChain, Grok / OpenAI Client |
| **Containerization** | Docker, Docker Compose (Postgres, Redis, Kafka, App) |
| **Testing** | JUnit 5, Mockito, AssertJ, Spring MockMvc (**203/203 Tests Passing**) |

---

## 🗺️ Engineering Phase Roadmap

- [x] **Phase 1**: Analyze architecture and design transaction pipeline
- [x] **Phase 2**: Design database schema, entities, and financial invariants
- [x] **Phase 3**: Implement production Double-Entry Ledger System
- [x] **Phase 4**: Write comprehensive double-entry ledger unit & controller tests
- [x] **Phase 5**: Implement Real-Time Fraud Detection & Scoring Engine
- [x] **Phase 6**: Integrate Redis sliding-window velocity and Apache Kafka
- [x] **Phase 7**: Implement Transactional Outbox and reliable event publisher worker
- [x] **Phase 8**: Integrate Frontend Compliance & Fraud Dashboard (`/admin/fraud`)
- [x] **Phase 9**: Dockerize infrastructure (`docker-compose.yml` for Postgres, Redis, Kafka, App)
- [x] **Phase 10**: Prepare production-readiness checklist and FinTech interview dossier (`docs/FINTECH_SYSTEM_DESIGN_AND_INTERVIEW_GUIDE.md`)

---

## 📋 Quickstart with Docker Compose

Run the entire distributed banking infrastructure with a single command:

```bash
docker compose up -d
```

This starts:
1. **PostgreSQL 16** on `localhost:5432`
2. **Redis 7** on `localhost:6379`
3. **Apache Kafka (KRaft)** on `localhost:9092`
4. **BankCore360 Monolith** on `localhost:8080`

### Running Backend Locally
```bash
# Execute complete test suite (203 tests)
./mvnw clean test

# Run Spring Boot application
./mvnw spring-boot:run
```

---

## 🔌 Core API Reference

### Double-Entry Ledger APIs (`/api/ledger`)
- `POST /api/ledger/deposit` — Balanced deposit (Debit Vault Asset, Credit Account Liability)
- `POST /api/ledger/withdraw` — Balanced withdrawal (Debit Account Liability, Credit Vault Asset)
- `POST /api/ledger/transfer` — Deadlock-free P2P transfer (Debit Sender, Credit Recipient)
- `GET /api/ledger/accounts/{id}/entries` — Paginated immutable ledger statement
- `GET /api/ledger/transactions/{id}` — Full transaction breakdown with entry splits
- `POST /api/ledger/transactions/{id}/reverse` — Immutable compensating reversal

### Fraud Detection & Compliance APIs (`/api/fraud`)
- `POST /api/fraud/evaluate` — Pre-settlement transaction risk assessment (0–100 score)
- `GET /api/fraud/alerts` — Paginated suspicious transaction alerts
- `GET /api/fraud/alerts/{id}` — Alert investigation history & audit trail
- `PATCH /api/fraud/alerts/{id}/status` — Compliance reviewer status updates & notes
- `GET /api/fraud/accounts/{id}/risk-profile` — Aggregated account risk profile

---

## 📚 Technical Documentation
For in-depth architectural diagrams, financial invariants, and FinTech system design interview questions, see:  
👉 [docs/FINTECH_SYSTEM_DESIGN_AND_INTERVIEW_GUIDE.md](docs/FINTECH_SYSTEM_DESIGN_AND_INTERVIEW_GUIDE.md)
