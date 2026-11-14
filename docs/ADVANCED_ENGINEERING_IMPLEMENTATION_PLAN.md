# BankCore360 – Advanced Engineering Implementation Plan

**Date:** November 2026  
**Document:** Implementation Roadmap & Technical Architecture  
**Dependencies Ordered By Execution Phase**  

---

## 1. Dependency-Ordered Execution Phases

```
┌────────────────────────────────────────────────────────┐
│ Phase A: Observability & Core Infrastructure           │
│ - Micrometer Prometheus & Actuator                     │
│ - Correlation ID & OpenTelemetry MDC Filter            │
│ - Prometheus Scraper & Grafana in Docker Compose       │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│ Phase B: Advanced Security & Session Governance        │
│ - Refresh Token Rotation & Reuse Detection             │
│ - OTP / TOTP MFA with Recovery Codes                   │
│ - Redis Distributed Sliding-Window Rate Limiting       │
│ - Device & Session Management with Revocation          │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│ Phase C: Bank Reconciliation Engine                    │
│ - Statement Upload, SHA-256 Idempotency, CSV/Excel     │
│ - Automated 4-Way Matching Engine                      │
│ - Discrepancy Investigation Workflow & Audit Records   │
│ - Reconciliation Reports & Admin Dashboard             │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│ Phase D: Verification, End-to-End Tests & Changelog    │
│ - Unit, Integration & Concurrency Tests                │
│ - Security & Rate Limiting Verification                │
│ - Changelog Documentation in ADVANCED_ENGINEERING_     │
│   CHANGELOG.md                                         │
└────────────────────────────────────────────────────────┘
```

---

## 2. Detailed Technical Design by Phase

### Phase A: Observability & Core Infrastructure
1. **Dependencies (`pom.xml`):**
   - `io.micrometer:micrometer-registry-prometheus`
   - `io.micrometer:micrometer-tracing-bridge-otel`
   - `io.opentelemetry:opentelemetry-api`
2. **Correlation ID & Tracing:**
   - `CorrelationIdFilter`: Generates or extracts `X-Correlation-ID`, attaches to SLF4J MDC (`correlationId`), injects into HTTP response headers, and propagates into Kafka headers.
3. **Application Configuration (`application.yml`):**
   - Enable Prometheus actuator endpoint `/actuator/prometheus`.
   - Configure Micrometer meters and metrics distribution.
4. **DevOps Provisioning (`docker-compose.yml`, `prometheus.yml`, `provisioning/`):**
   - Add Prometheus 2.50+ container scraping `bankcore360-app:8080/actuator/prometheus`.
   - Add Grafana 10.3+ container pre-configured with Prometheus datasource.
   - Provision JSON dashboards: `bankcore360-overview.json` (Latency, TPS, Errors, Ledger throughput, Fraud checks, JVM & Hikari pool).

---

### Phase B: Advanced Security & Session Governance
1. **Database Entities:**
   - `RefreshToken`: `id`, `user_id`, `token_hash`, `token_family`, `is_revoked`, `expires_at`, `created_at`.
   - `UserSessionRecord`: `id`, `user_id`, `session_token_hash`, `ip_address`, `user_agent`, `device_os`, `browser`, `is_active`, `last_activity_at`, `login_at`.
   - Enhance `AppUser`: `mfa_enabled`, `mfa_secret_hash`, `mfa_type`, `mfa_recovery_codes_hash`.
2. **MFA Engine (`MfaService.java`):**
   - Generate secure 6-digit cryptographic OTPs and time-limited tokens.
   - Hash OTPs with SHA-256 before storing temporarily in Redis / DB.
   - Generate 8 one-time cryptographic recovery codes (hashed with BCrypt).
   - Multi-step login flow: Step 1 validates password; if MFA enabled, returns `mfaRequired: true` and temporary session ticket; Step 2 validates OTP / recovery code and issues full access + refresh token.
3. **Refresh Token Rotation (`TokenRotationService.java`):**
   - Issue cryptographically secure random token string.
   - On `/api/auth/refresh`: Check token existence and revocation.
   - **Reuse Detection:** If an already-revoked refresh token is presented, immediately revoke **all** tokens sharing that `token_family` (indicating stolen credentials), log security alert, and force re-login.
   - Issue new access token + rotated refresh token in the same family.
4. **Redis Distributed Rate Limiting (`RateLimitingFilter.java`):**
   - Uses Redis sliding-window counter (`ZADD` / `ZREMRANGEBYSCORE` or atomic script).
   - Rules:
     - `/api/auth/login`: Max 5 requests / min per IP + username.
     - `/api/auth/mfa/**`: Max 3 requests / 5 min.
     - `/api/ledger/**`: Max 30 requests / min per authenticated user.
   - Responds with HTTP 429 `Too Many Requests` and headers `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After`.
5. **Session & Device Tracking (`SessionManagementService.java`):**
   - Inspects `User-Agent` to parse OS and Browser.
   - Tracks IP and alerts on anomalous new device/IP combinations.
   - Endpoints to list sessions, revoke a specific session ID, or logout all active sessions.

---

### Phase C: Bank Reconciliation Engine
1. **Database Entities:**
   - `BankStatement`: `id`, `statementReference`, `bankName`, `accountNumber`, `filename`, `fileHashSha256`, `statementStartDate`, `statementEndDate`, `openingBalance`, `closingBalance`, `totalRecords`, `status`, `uploadedBy`, `uploadedAt`.
   - `BankStatementItem`: `id`, `statement_id`, `itemReference`, `transactionDate`, `valueDate`, `amount`, `direction` (DEBIT/CREDIT), `description`, `externalReference`, `reconciliationStatus` (UNMATCHED, MATCHED, DISCREPANCY, DUPLICATE), `matched_ledger_entry_id`, `matched_at`.
   - `ReconciliationBatch`: `id`, `batchReference`, `statement_id`, `reconciledAt`, `reconciledBy`, `matchedCount`, `unmatchedStatementCount`, `unmatchedLedgerCount`, `discrepancyCount`, `status`.
   - `ReconciliationDiscrepancy`: `id`, `batch_id`, `statement_item_id`, `ledger_entry_id`, `discrepancyType` (AMOUNT_MISMATCH, DATE_OUT_OF_BOUNDS, MISSING_IN_LEDGER, MISSING_IN_STATEMENT, DUPLICATE_ENTRY), `description`, `status` (OPEN, UNDER_INVESTIGATION, RESOLVED, IGNORED), `investigatorNotes`, `resolvedBy`, `resolvedAt`.
2. **Statement Parser (`StatementParserService.java`):**
   - Parses CSV bank statements and Excel (.xlsx) formats.
   - Validates mandatory columns: `Date`, `Reference`, `Description`, `Debit`, `Credit`, `Balance`.
   - Computes SHA-256 file checksum to reject duplicate statement uploads idempotently.
3. **Automated 4-Way Matching Engine (`ReconciliationService.java`):**
   - Queries `ledger_entries` within the statement date window for the target `LedgerAccount`.
   - **Match Rule 1 (Exact Match):** Same reference (or reference in narration) + exact direction + exact amount $\implies$ `MATCHED`.
   - **Match Rule 2 (Fuzzy Date Match):** Same account + exact direction + exact amount within $\pm 2$ banking days $\implies$ `MATCHED`.
   - **Duplicate Detection:** Multiple statement items with identical reference/amount or ledger entries already reconciled $\implies$ `DUPLICATE`.
   - **Discrepancy Identification:**
     - Reference matches but amount differs $\implies$ `AMOUNT_MISMATCH`.
     - Entry exists in statement but absent in internal ledger $\implies$ `MISSING_IN_LEDGER`.
     - Entry exists in ledger but absent in bank statement $\implies$ `MISSING_IN_STATEMENT`.
4. **Discrepancy Investigation Workflow:**
   - Manual override APIs for auditors and financial controllers (`PATCH /api/reconciliation/discrepancies/{id}/status`).
   - Strict FinTech invariant: Does NOT mutate historical ledger entries; creates immutable audit trail.
5. **Reconciliation Controller & Reports:**
   - `POST /api/reconciliation/upload`: Upload CSV/Excel statement.
   - `POST /api/reconciliation/batches/execute`: Run automated matching against statement.
   - `GET /api/reconciliation/batches/{id}/report`: Full reconciliation variance report.
   - `GET /api/reconciliation/discrepancies`: Filter discrepancies by status and type.

---

### Phase D: Verification & Testing
1. Comprehensive JUnit 5 / Spring Boot test suite:
   - `StatementParserServiceTest`: CSV parsing, malformed format rejection, SHA-256 duplicate detection.
   - `ReconciliationEngineTest`: Exact matches, amount mismatches, missing transactions, duplicate statement items.
   - `MfaAuthenticationTest`: MFA enrollment, OTP verification, invalid OTP rejection, recovery code usage.
   - `TokenRotationTest`: Access token renewal, refresh token rotation, token reuse detection and full family revocation.
   - `RateLimitingTest`: Rapid fire requests to `/api/auth/login` and `/api/ledger/**` verifying HTTP 429 and Redis expiry.
   - `ObservabilityEndpointTest`: Verification of `/actuator/prometheus`, `X-Correlation-ID` header injection.
2. Full test run using `./mvnw test`.
3. Creation of `docs/ADVANCED_ENGINEERING_CHANGELOG.md`.

