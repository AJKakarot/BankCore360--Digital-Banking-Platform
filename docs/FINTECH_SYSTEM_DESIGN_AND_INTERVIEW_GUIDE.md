# BankCore360: Advanced FinTech Engineering & System Design Dossier

> **Authoritative Technical Documentation & Senior FinTech Architect Interview Reference**  
> Covering **Module 1 (Double-Entry Ledger Engine)** and **Module 2 (Real-Time Fraud Detection & Transactional Outbox)**.

---

## 🏛 1. High-Level System Architecture

BankCore360 is engineered as an enterprise-grade **Modular Monolith** in Java 21 & Spring Boot 3.4, designed to eliminate the network serialization, distributed transaction overhead, and partial failure modes of microservices while enforcing clean domain boundaries.

```
                              [Client Application / Web Frontend]
                                               │
                                               ▼
                              [Spring Security Filter Chain]
                                (Stateless HTTP Basic / RBAC)
                                               │
                                               ▼
                                 [Idempotency Interceptor]
                                (Deterministic Key Validation)
                                               │
                                               ▼
                             ┌───────────────────────────────────┐
                             │    MODULE 2: FRAUD DETECTION      │
                             ├───────────────────────────────────┤
                             │ • Redis Sliding-Window Velocity   │
                             │ • Configurable Fraud Rules Engine │
                             │ • Dynamic Risk Score (0 - 100)    │
                             └─────────────────┬─────────────────┘
                                               │
                      ┌────────────────────────┴────────────────────────┐
                      ▼                                                 ▼
               Score 0–60 (Low/Med)                             Score 61–100 (High/Crit)
                  [APPROVED]                                    [MANUAL REVIEW / BLOCK]
                      │                                                 │
                      │                                      ┌──────────┴──────────┐
                      │                                      ▼                     ▼
                      │                               [Create FraudAlert]   [Abort & Block Txn]
                      │                               (Queue Compliance)     (Throw 403 / 422)
                      ▼                                      │                     │
        ┌───────────────────────────┐                        └──────────┬──────────┘
        │  MODULE 1: LEDGER ENGINE  │                                   │
        ├───────────────────────────┤                                   │
        │ • Debit Vault / Sender    │                                   │
        │ • Credit Recipient / Acc  │                                   │
        │ • Invariant: ΣDr == ΣCr   │                                   │
        │ • Pessimistic Write Lock  │                                   │
        └─────────────┬─────────────┘                                   │
                      │                                                 │
                      └────────────────────────┬────────────────────────┘
                                               │
                                               ▼
                                 [Atomic Database Transaction]
                            (PostgreSQL: ACID Isolation Level RC)
                            ├── ledger_transactions
                            ├── ledger_entries (Immutable)
                            ├── account_balances
                            ├── fraud_assessments
                            └── outbox_events (Transactional Outbox)
                                               │
                                 [Local Commit: All or Nothing]
                                               │
                                               ▼
                              [Asynchronous Outbox Poller Worker]
                                 (@Scheduled with Backoff Retry)
                                               │
                                               ▼
                                    [Apache Kafka Cluster]
                      ├── bankcore360.transactions.initiated
                      ├── bankcore360.transactions.approved
                      ├── bankcore360.transactions.flagged
                      └── bankcore360.fraud.alerts
```

---

## ⚖️ 2. Module 1: Double-Entry Accounting Ledger Engine

### 2.1 The Fundamental Accounting Equation
In enterprise financial engineering, money is never "updated" or "created out of thin air." Every balance modification is a derived consequence of balanced debit and credit entries:

$$\sum \text{Debit} = \sum \text{Credit}$$

#### Normal Balance Classifications:
1. **ASSET & EXPENSE Accounts:** Normal Balance = `DEBIT`.
   - Debit entry: **Increases** balance ($Bal_{\text{new}} = Bal_{\text{current}} + \text{Amount}$)
   - Credit entry: **Decreases** balance ($Bal_{\text{new}} = Bal_{\text{current}} - \text{Amount}$)
2. **LIABILITY, EQUITY & REVENUE Accounts:** Normal Balance = `CREDIT`.
   - Credit entry: **Increases** balance ($Bal_{\text{new}} = Bal_{\text{current}} + \text{Amount}$)
   - Debit entry: **Decreases** balance ($Bal_{\text{new}} = Bal_{\text{current}} - \text{Amount}$)

### 2.2 Balance Mapping by Transaction Type

| Transaction Type | Debit Leg | Credit Leg | Accounting Effect |
| :--- | :--- | :--- | :--- |
| **Deposit** | Bank Central Vault (`ASSET`) | Customer Account (`LIABILITY`) | Bank cash increases; bank obligation to customer increases. |
| **Withdrawal** | Customer Account (`LIABILITY`) | Bank Central Vault (`ASSET`) | Customer claim decreases; bank vault cash decreases. |
| **Fund Transfer** | Sender Account (`LIABILITY`) | Destination Account (`LIABILITY`) | Sender liability decreases; recipient liability increases. |
| **Reversal** | Inverse of original Debit Leg | Inverse of original Credit Leg | Exactly offsets original balance impacts without mutating historical rows. |

### 2.3 Concurrency & Deadlock Prevention
When transferring funds between Account $A$ and Account $B$, two concurrent bidirectional transfers ($A \to B$ and $B \to A$) can cause database-level deadlocks if locks are acquired arbitrarily.

**Solution:** Deterministic Lock Ordering:
```java
// Always acquire locks in ascending order of Account ID:
Long firstLockId  = Math.min(accountA.getId(), accountB.getId());
Long secondLockId = Math.max(accountA.getId(), accountB.getId());

AccountBalance balance1 = accountBalanceRepository.findByAccountIdWithLock(firstLockId);
AccountBalance balance2 = accountBalanceRepository.findByAccountIdWithLock(secondLockId);
```

### 2.4 Immutability & Reversals
- Core columns in `ledger_entries` (`direction`, `amount`, `balance_after`, `created_at`) have `updatable = false`.
- If an erroneous transaction is posted, **it is never deleted or updated**. A compensating `REVERSAL` transaction is created, generating opposite entries linked by `transaction_reversals`.

---

## 🛡 3. Module 2: Real-Time Fraud Detection Engine

### 3.1 Pre-Settlement Risk Scoring
Before any funds move or locks are acquired, the transaction is evaluated against active rules in `fraud_rules`:

$$\text{Final Risk Score} = \min\left(100, \; \sum_{i=1}^{k} \text{Rule Weight}_i\right)$$

| Score Range | Risk Level | System Action | Real-World Context |
| :---: | :---: | :---: | :--- |
| **0 – 30** | `LOW` | **ALLOW** | Routine payments, regular amounts. |
| **31 – 60** | `MEDIUM` | **ALLOW + LOG** | Moderately elevated amounts; telemetry recorded. |
| **61 – 80** | `HIGH` | **MANUAL REVIEW** | Velocity spurt or large transfer; held in `PENDING_REVIEW`. |
| **81 – 100** | `CRITICAL` | **HARD REJECT** | Critical threshold breach; immediate 403 Forbidden; account flagged. |

### 3.2 Redis Sliding-Window Velocity Checking
Rather than executing expensive, disk-bound SQL aggregation queries (`COUNT(*) WHERE created_at > NOW() - INTERVAL '5 minutes'`), we utilize **Redis Sorted Sets (`ZSET`)**:
- **Key:** `fraud:velocity:ts:<accountNumber>`
- **Member:** `<transactionRef>:<amount>:<timestampEpochMs>`
- **Score:** `<timestampEpochMs>`

**Sliding Window Algorithm:**
1. `ZREMRANGEBYSCORE key -inf (now - windowMs)` (Purge expired entries in $O(\log N + M)$)
2. `ZCARD key` (Retrieve instantaneous transaction count in $O(1)$)
3. `ZADD key now member` (Append new transaction in $O(\log N)$)
4. `EXPIRE key 3600` (Automatic TTL retention)

---

## ⚡ 4. Distributed Systems: Transactional Outbox Pattern

### 4.1 The Dual-Write Problem
Attempting to write to a relational database and publish to Apache Kafka within the same method without an Outbox produces the **Dual-Write Anti-Pattern**:

```
// ANTI-PATTERN:
@Transactional
public void processPayment() {
    ledgerRepository.save(entry);      // 1. PostgreSQL DB Write
    kafkaTemplate.send("payments", ...); // 2. Network Call to Kafka
}
```
- **Scenario A:** DB commits, but Kafka broker disconnects $\to$ Event lost forever; downstream services out of sync.
- **Scenario B:** Kafka succeeds, but DB transaction rolls back due to constraint violation $\to$ Downstream services process a ghost payment.

### 4.2 The Solution: Transactional Outbox
1. In the same ACID transaction that updates the ledger, an event record is saved to the local table `outbox_events`.
2. A background worker polls `outbox_events WHERE status = 'PENDING'`.
3. The worker dispatches to Kafka with synchronous acknowledgment (`acks=all`).
4. On success: `status = PUBLISHED`.
5. On failure: `retry_count++` with exponential backoff (dead-letter after 5 retries).

---

## 📋 5. Production-Readiness Checklist

### Database Layer (PostgreSQL)
- [x] High-cardinality indices on `account_number`, `transaction_reference`, `idempotency_key`, `status`.
- [x] Connection pool configured via HikariCP (`maximum-pool-size: 5`, `minimum-idle: 1`, `timeout: 30s`).
- [x] Optimistic locking (`@Version`) on `account_balances` alongside pessimistic write lock queries.
- [x] Explicit Hibernate dialect specified (`PostgreSQLDialect`) to prevent metadata cold-start issues.

### In-Memory Layer (Redis)
- [x] Automatic key expiration (1 hour TTL) on velocity tracking keys.
- [x] Resilient in-memory fallback queue (`localFallbackStore`) in case Redis is rebooted.
- [x] Lettuce client timeout bounded at 2000ms to eliminate thread starvation.

### Message Streaming (Apache Kafka)
- [x] Producer durability guarantee configured with `acks=all`.
- [x] Idempotent publisher retries (`retries=3`, `max.block.ms=2000`).
- [x] Pre-created topics with 3 partitions for horizontal consumer scalability.

---

## 🎯 6. Senior FinTech Engineering Interview Q&A

### Q1: Why double-entry instead of just storing `balance` on the account?
**Answer:** In single-entry systems, balances are updated destructively via `UPDATE accounts SET balance = balance + amount`. If a crash, concurrent race condition, or corruption occurs, it is mathematically impossible to reconstruct how that balance was reached or detect internal fraud. In double-entry accounting, balances are auditably backed by an unbroken chain of debit and credit entries. The sum of all assets must always equal liabilities plus equity.

### Q2: How do you prevent floating-point rounding errors in financial transactions?
**Answer:** Floating-point numbers (`float`, `double`) use binary IEEE 754 representations that cannot accurately represent base-10 fractions (e.g. `0.1 + 0.2 != 0.3`). In BankCore360, all monetary amounts and balance calculations strictly use `BigDecimal` with a fixed precision of 19 digits and a scale of 4 decimal places, utilizing explicit rounding modes (`RoundingMode.HALF_EVEN` / Banker's Rounding).

### Q3: What is the difference between Pessimistic and Optimistic Locking, and where do you use each?
**Answer:**
- **Optimistic Locking (`@Version`):** Best for low-contention scenarios (e.g. updating customer KYC details or loan profile). It verifies that no other transaction modified the row before committing.
- **Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`):** Essential for high-concurrency money movement. It acquires a PostgreSQL row-level lock (`SELECT ... FOR UPDATE`) immediately, forcing concurrent withdrawals or transfers to serialize and preventing overdraft race conditions.

### Q4: How does BankCore360 guarantee idempotent payment requests?
**Answer:** Each mutation API accepts an `idempotencyKey` provided by the client. Before executing business logic, `LedgerTransactionRepository.findByIdempotencyKey(key)` checks if a transaction with that key already exists. If found, the existing transaction response is returned immediately without executing debit/credit operations again.
