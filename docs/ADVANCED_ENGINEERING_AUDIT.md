# BankCore360 – Advanced Engineering Audit Report

**Date:** November 2026  
**Auditor:** Senior FinTech Software Architect, Backend Engineer & DevOps Specialist  
**System Under Audit:** BankCore360 Modular Monolith (Java 21 + Spring Boot 3.4.3, PostgreSQL 16, Redis 7, Kafka KRaft, Next.js 16)  

---

## 1. Executive Summary

This comprehensive audit evaluates the BankCore360 platform across three enterprise capabilities:
1. **Bank Reconciliation Engine** (External statement matching, discrepancy resolution, audit logs)
2. **Advanced Security** (MFA, Refresh Token Rotation with reuse detection, Redis distributed rate limiting, Device/Session management)
3. **Monitoring & Observability** (Prometheus Actuator metrics, Grafana dashboards, OpenTelemetry distributed tracing, correlation IDs)

---

## 2. Feature-by-Feature Audit Matrix

| Feature | Status | Existing Files | Missing Work |
|---|---|---|---|
| **Bank Reconciliation** | **Missing** | • [`LedgerTransaction.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/entity/LedgerTransaction.java)<br>• [`LedgerEntry.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/entity/LedgerEntry.java)<br>• [`LedgerService.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/service/LedgerService.java) | • Bank statement import entity & schema (`BankStatement`, `BankStatementItem`).<br>• Reconciliation batch entity & discrepancies (`ReconciliationBatch`, `ReconciliationDiscrepancy`).<br>• CSV/Excel statement parser with column mapping & checksum validation.<br>• Automated 4-way matching engine (Reference, Amount, Date window, Account).<br>• Duplicate statement item detection.<br>• Unmatched & missing ledger transaction detectors.<br>• Discrepancy investigation workflow (Flag, Investigate, Resolve with audit notes).<br>• REST APIs: Statement upload, reconciliation trigger, discrepancy resolution, audit history, summary reports.<br>• Admin Reconciliation dashboard UI. |
| **MFA (Multi-Factor Auth)** | **Missing** | • [`AppUser.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/entity/AppUser.java)<br>• [`AuthService.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/service/AuthService.java)<br>• [`AuthController.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/controller/AuthController.java) | • MFA enrollment entity & fields on user (`mfaEnabled`, `mfaSecret`, `mfaType`).<br>• OTP/TOTP generator & validator with BCrypt/SHA-256 hashed storage.<br>• Recovery code generation, one-time hashed recovery verification.<br>• Login flow enhancement: Step 1 (Username/Password) returns `MFA_REQUIRED` ticket; Step 2 (`/api/auth/mfa/verify`) validates OTP and issues tokens.<br>• APIs: `/api/auth/mfa/enroll`, `/api/auth/mfa/verify`, `/api/auth/mfa/recovery`. |
| **Refresh Token Rotation** | **Missing** | • [`SecurityConfig.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/config/SecurityConfig.java) (Uses basic auth/stateless sessions)<br>• [`AuthService.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/service/AuthService.java) | • Secure Token Entity (`RefreshToken`) with token family ID, hashed token value, expiration, revocation status.<br>• JWT / Secure Token provider issuing short-lived Access Tokens (15 min) and Refresh Tokens (7 days).<br>• Token rotation upon `/api/auth/refresh`: old token invalidated, new pair generated in same family.<br>• Automatic token reuse detection: if an invalidated token is reused, all descendant tokens in the family are revoked and flagged as potential theft.<br>• Complete logout and session invalidation endpoint (`/api/auth/logout`). |
| **API Rate Limiting** | **Missing** | • [`RedisConfig.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/config/RedisConfig.java) (Lettuce connection configured)<br>• [`RedisVelocityService.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/service/RedisVelocityService.java) | • Redis sliding-window / token-bucket distributed rate limiting filter (`RateLimitingFilter`).<br>• Login endpoint protection: 5 attempts / min per IP & username.<br>• OTP endpoint protection: 3 attempts / 5 min.<br>• Financial Transaction API limits: 30 requests / min per authenticated user.<br>• Standard HTTP 429 `Too Many Requests` response with `Retry-After` headers. |
| **Device & Session Management** | **Missing** | • None (sessions currently untracked) | • User Session Entity (`UserSessionRecord`) storing device OS, browser, IP address, user-agent, session status, last activity timestamp.<br>• Geo/IP heuristic detector for suspicious logins (new device alert, IP anomaly).<br>• Endpoints: `GET /api/auth/sessions` (list active devices), `DELETE /api/auth/sessions/{id}` (revoke specific session), `POST /api/auth/sessions/revoke-all` (logout from all devices). |
| **Prometheus** | **Partial** | • `spring-boot-starter-actuator` in [`pom.xml`](file:///Users/ajeet/AjeetFOLIO/BankCore360/pom.xml)<br>• `management.endpoints.web.exposure: health,info` in [`application.yml`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/resources/application.yml) | • Add `micrometer-registry-prometheus` dependency to `pom.xml`.<br>• Expose `/actuator/prometheus` endpoint in `application.yml`.<br>• Custom FinTech Micrometer metrics: `bankcore360.ledger.transactions.total`, `bankcore360.ledger.amount.transferred`, `bankcore360.fraud.assessments`, `bankcore360.reconciliation.discrepancies`, `bankcore360.auth.failures`.<br>• Prometheus standalone scraper configuration (`prometheus.yml`). |
| **Grafana** | **Missing** | • None in [`docker-compose.yml`](file:///Users/ajeet/AjeetFOLIO/BankCore360/docker-compose.yml) | • Add Prometheus and Grafana container services to `docker-compose.yml`.<br>• Grafana automated datasource provisioning (`provisioning/datasources/prometheus.yml`).<br>• Pre-built production JSON dashboards: API Latency & Error Rates, JVM Memory & Threads, PostgreSQL Connection Pool (Hikari), Redis Health, Kafka Consumer Lag, FinTech Core Ledger & Fraud stats. |
| **OpenTelemetry & Tracing** | **Missing** | • None | • Add Micrometer Tracing & OpenTelemetry bridge (`micrometer-tracing-bridge-otel`).<br>• MDC correlation ID servlet filter (`CorrelationIdFilter`) populating `X-Correlation-ID` header and log format.<br>• Distributed trace propagation across HTTP calls and Kafka event headers.<br>• Logback structured JSON/Pattern logging with correlation ID tracing. |

---

## 3. Financial Invariant & Security Safeguards

1. **Reconciliation Invariants:**
   - Reconciliation is an **investigative and audit layer**. It MUST NEVER mutate posted ledger entries (`ledger_entries`).
   - Reconciliation MUST NEVER alter customer account balances directly; discrepancies require human officer review and ledger adjustment entries if necessary.
   - All statement uploads must compute a SHA-256 hash to prevent accidental duplicate file processing.
2. **Security Invariants:**
   - Passwords, OTPs, recovery codes, and refresh tokens must NEVER be stored in plaintext. Passwords and recovery codes use BCrypt; refresh tokens and OTPs use salted SHA-256.
   - Token reuse detection must trigger immediate invalidation of the entire token hierarchy for that session.
3. **Observability Invariants:**
   - Sensitive financial data (PAN, CVV, passwords, full account numbers) must NEVER be exposed in log messages, Prometheus metric tags, or trace attributes.

