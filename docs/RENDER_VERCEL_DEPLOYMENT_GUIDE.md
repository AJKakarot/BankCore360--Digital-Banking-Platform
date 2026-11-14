# BankCore360 – Render & Vercel Cloud Deployment Guide

This guide details the complete configuration required to run BankCore360 across **Vercel (Frontend)**, **Render (Spring Boot Backend)**, **Render (FastAPI AI Service)**, and **Neon (PostgreSQL Database)**.

---

## 🏗️ Production Architecture Overview

```
┌─────────────────────────────────┐
│     Vercel (Next.js App)        │
│   https://bankcore360.vercel.app │
└──────────────┬──────────────────┘
               │ (HTTPS with Bearer JWT & CORS)
               ▼
┌─────────────────────────────────┐           ┌─────────────────────────────────┐
│   Render Web Service (Java 21)  │ ◄───────► │ Render Web Service (FastAPI AI) │
│ https://bankcore360.onrender.com│ (Internal │https://bankcore360-ai.onrender. │
└──────────────┬──────────────────┘   Secret) └──────────────┬──────────────────┘
               │                                             │
               │ (SSL / TLS Pooler Connection)               │
               ▼                                             ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                    Neon PostgreSQL Cloud Database (AWS us-east-2)             │
│                      ep-morning-lake-b4h65mvq-pooler.c-6.us-east-2...          │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## 1. Vercel Frontend Deployment (Next.js)

### Steps to Deploy:
1. Connect your GitHub repository to [Vercel](https://vercel.com).
2. Select **Root Directory:** `frontend`.
3. Framework Preset: **Next.js**.
4. Configure Environment Variables in Vercel Project Settings:

| Variable Name | Value | Purpose |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | `https://your-backend.onrender.com` | Base URL of Spring Boot Backend on Render |
| `NEXT_PUBLIC_AI_URL` | `https://your-ai-service.onrender.com` | Base URL of FastAPI AI Service on Render |

5. Click **Deploy**.

---

## 2. Render Backend Deployment (Spring Boot Web Service)

### Service Settings:
- **Type:** Web Service
- **Environment:** `Docker` (or Java 21)
- **Root Directory:** `.` (repository root)
- **Dockerfile Path:** `./Dockerfile`
- **Instance Type:** Starter or Free

### Environment Variables on Render:

| Variable Name | Value | Notes |
|---|---|---|
| `PORT` | `8080` | Spring Boot HTTP port |
| `DB_URL` | `jdbc:postgresql://ep-morning-lake-b4h65mvq-pooler.c-6.us-east-2.aws.neon.tech/neondb?sslmode=require` | Neon PostgreSQL pooled URL |
| `DB_USERNAME` | `neondb_owner` | Neon database user |
| `DB_PASSWORD` | `npg_0IFZmYOw1iqM` | Neon database password |
| `AI_SERVICE_URL` | `https://your-ai-service.onrender.com` | Public HTTPS URL of Render AI service |
| `INTERNAL_API_SECRET` | `<32_or_64_char_random_secret>` | Must match the secret in AI service |
| `CORS_ALLOWED_ORIGINS` | `https://your-app.vercel.app,http://localhost:3000` | Whitelisted frontend origins |
| `FLYWAY_ENABLED` | `true` | Runs baseline and immutability migrations |
| `JPA_DDL_AUTO` | `validate` | Validates entity schemas without mutating |
| `SHOW_SQL` | `false` | Disables query logging in production |

---

## 3. Render AI Service Deployment (Python FastAPI)

### Service Settings:
- **Type:** Web Service
- **Environment:** `Python 3`
- **Root Directory:** `ai-service`
- **Build Command:** `pip install -r requirements.txt`
- **Start Command:** `uvicorn app.main:app --host 0.0.0.0 --port $PORT`
- **Instance Type:** Starter or Free

### Environment Variables on Render:

| Variable Name | Value | Notes |
|---|---|---|
| `PORT` | `8000` | FastAPI HTTP port |
| `ENVIRONMENT` | `production` | Production environment flag |
| `SPRING_BOOT_API` | `https://your-backend.onrender.com` | Spring Boot backend URL |
| `INTERNAL_API_SECRET` | `<same_secret_as_backend>` | Used for secure inter-service API auth |
| `CORS_ALLOWED_ORIGINS` | `https://your-app.vercel.app,http://localhost:3000` | Allowed Vercel origins |
| `DATABASE_URL` | `postgresql://neondb_owner:npg_0IFZmYOw1iqM@ep-morning-lake-b4h65mvq-pooler.c-6.us-east-2.aws.neon.tech/neondb?sslmode=require` | Neon PostgreSQL database connection |
| `OPENAI_API_KEY` | `sk-...` | OpenAI API Key for multi-agent reasoning |
| `OPENAI_MODEL` | `gpt-4o-mini` | Default cost-efficient model |
| `MOCK_LLM_RESPONSES` | `false` | Set to false in production |

---

## 4. Handling Render Cold Starts (Free/Starter Tier Sleep Mode)

Render Free/Starter tier web services automatically spin down after 15 minutes of inactivity. When a new request arrives, it takes ~30–50 seconds to wake up.

### Built-in Protections in Code:
1. **Spring Boot Fail-Fast Timeout:** `AiAssistantService.java` is configured with a 5s connect timeout and 12s read timeout so that if the AI service is sleeping, Spring Boot does not hang and cleanly serves built-in RAG fallback answers.
2. **Frontend Auto-Retry:** `frontend/src/lib/api.ts` intercepts network calls and strips trailing slashes to prevent malformed URLs.

### Optional: Free Keep-Alive Ping (Zero Cold Starts)
To keep your Render services 100% warm 24/7 without paying for upgraded tiers:
1. Create a free account at [cron-job.org](https://cron-job.org) or [UptimeKuma].
2. Set up a ping every **10 minutes** to:
   - `https://your-backend.onrender.com/actuator/health`
   - `https://your-ai-service.onrender.com/health`
3. This prevents Render from spinning down the containers.
