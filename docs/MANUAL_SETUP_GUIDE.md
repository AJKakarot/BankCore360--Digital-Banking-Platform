# BankCore360 – Production Operations & Manual Setup Guide

This guide details all necessary manual provisioning, credential configuration, and infrastructure operations required by system administrators and DevOps engineers when deploying BankCore360 to staging or production environments.

---

## 1. Asymmetric RSA-256 JWT Keypair Provisioning

In development, BankCore360 automatically generates an in-memory 2048-bit RSA keypair on startup. In staging and production, operators should generate and mount versioned cryptographic keys.

### Keypair Generation Commands
```bash
# 1. Generate 2048-bit PKCS#8 RSA Private Key
openssl genpkey -algorithm RSA -out bankcore360-jwt-private.pem -pkeyopt rsa_keygen_bits:2048

# 2. Extract Public Key
openssl rsa -pubout -in bankcore360-jwt-private.pem -out bankcore360-jwt-public.pem

# 3. Secure file permissions
chmod 600 bankcore360-jwt-private.pem
chmod 644 bankcore360-jwt-public.pem
```

### Environment Variable Configuration
Inject the generated PEM strings into container environment variables or Kubernetes secrets:
```bash
JWT_PRIVATE_KEY_PATH=/etc/secrets/jwt/bankcore360-jwt-private.pem
JWT_PUBLIC_KEY_PATH=/etc/secrets/jwt/bankcore360-jwt-public.pem
SECURITY_JWT_ACCESS_TOKEN_VALIDITY_SECONDS=900 # 15 minutes
```

---

## 2. PostgreSQL & Flyway Database Setup

BankCore360 utilizes Flyway version-controlled migrations for all schema evolution.

### Cloud Provider (Neon PostgreSQL / AWS RDS / Supabase)
1. Provision a PostgreSQL 16+ instance.
2. Grant user permissions:
```sql
GRANT ALL PRIVILEGES ON DATABASE neondb TO neondb_owner;
GRANT CREATE ON SCHEMA public TO neondb_owner;
```
3. Configure application environment variables:
```bash
DB_URL=jdbc:postgresql://<HOST>:<PORT>/<DATABASE>?sslmode=require
DB_USERNAME=<USERNAME>
DB_PASSWORD=<PASSWORD>
FLYWAY_ENABLED=true
JPA_DDL_AUTO=validate
```
> **Regulatory Notice:** Flyway script `V2__ledger_immutability_trigger.sql` installs PostgreSQL triggers that strictly forbid `UPDATE` and `DELETE` queries on `ledger_entries`. Do NOT bypass this trigger in production.

---

## 3. Distributed Redis Cache & Velocity Limiter

BankCore360 uses Redis for sliding-window rate limiting, customer transaction velocity checks, and session telemetry.

```bash
SPRING_DATA_REDIS_HOST=redis.internal.bankcore360.com
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=<STRONG_REDIS_PASSWORD>
```

---

## 4. Apache Kafka Cluster & Topics

BankCore360 uses Kafka for event streaming and dead letter queue routing.

### Topic Provisioning
```bash
# Core Topics
kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 3 --partitions 3 --topic bankcore360.transactions.initiated
kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 3 --partitions 3 --topic bankcore360.transactions.approved
kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 3 --partitions 3 --topic bankcore360.transactions.flagged
kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 3 --partitions 3 --topic bankcore360.fraud.alerts

# Dead Letter Topic (DLT)
kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 3 --partitions 3 --topic bankcore360.fraud.alerts.DLT
```

---

## 5. Cloudinary KYC Document Storage

1. Create a Cloudinary enterprise account at https://cloudinary.com.
2. Retrieve your Account Credentials from Dashboard.
3. Configure in backend environment:
```bash
CLOUDINARY_URL=cloudinary://<API_KEY>:<API_SECRET>@<CLOUD_NAME>
```

---

## 6. Multi-Agent AI Service Configuration

The FastAPI multi-agent AI assistant runs on port 8000.

### Environment Configuration (`ai-service/.env`)
```bash
ENVIRONMENT=production
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
LANGSMITH_TRACING=true
LANGSMITH_API_KEY=lsv2_pt_...
LANGSMITH_PROJECT=bankcore360-ai-assistant
INTERNAL_API_SECRET=<STRONG_64_CHAR_HEX_SECRET>
```

---

## 7. GitHub Actions CI/CD Secrets

Configure the following secrets in GitHub Repository Settings (`Settings -> Secrets and variables -> Actions`):

| Secret Name | Description | Example / Target |
|---|---|---|
| `DB_URL` | Staging PostgreSQL connection string | `jdbc:postgresql://...` |
| `DB_USERNAME` | Staging database user | `bankcore_user` |
| `DB_PASSWORD` | Staging database password | `secret` |
| `OPENAI_API_KEY` | Model testing key | `sk-...` |
| `LANGSMITH_API_KEY` | Multi-agent tracing token | `lsv2_...` |
| `INTERNAL_API_SECRET` | AI gateway authentication secret | `hex-string` |
