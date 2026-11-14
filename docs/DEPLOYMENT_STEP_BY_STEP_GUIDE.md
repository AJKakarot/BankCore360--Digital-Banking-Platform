# BankCore360 – Render & Vercel Step-by-Step Live Deployment Guide

Yeh document aapko **BankCore360** ko **Render (Backend + AI Service)**, **Vercel (Frontend)**, aur **Neon (PostgreSQL Database)** par live deploy karne ka complete step-by-step checklist deta hai.

---

## 🏗️ Architecture Summary

| Component | Platform | Tech Stack | Root Directory |
|---|---|---|---|
| **Backend Core** | Render (Web Service) | Java 21, Spring Boot 3.4.3 (Docker) | `.` (Root) |
| **AI Service** | Render (Web Service) | Python 3.12, FastAPI, LangGraph | `ai-service` |
| **Frontend UI** | Vercel | Next.js 16 (App Router), TypeScript | `frontend` |
| **Database** | Neon Cloud | PostgreSQL 16 (AWS us-east-2) | Cloud Managed |

---

## 🟢 STEP 1: Render par Backend (Spring Boot) Deploy Karna

1. **[dashboard.render.com](https://dashboard.render.com)** par login karein.
2. Top bar me **New +** button par click karke **Web Service** select karein.
3. Apna GitHub repository connect karein: `BankCore360--Digital-Banking-Platform`.
4. Basic settings fill karein:
   - **Name:** `bankcore360-api`
   - **Region:** `Ohio (US East)` *(Neon database ke kareeb)*
   - **Branch:** `main`
   - **Root Directory:** *(Khali chhod dein / default repository root)*
   - **Runtime:** `Docker` *(Repository ka Dockerfile use hoga)*
   - **Instance Type:** `Free` ya `Starter`
5. **Environment Variables** section me ye exact keys & values add karein:

```env
PORT=8080
DB_URL=jdbc:postgresql://ep-morning-lake-b4h65mvq-pooler.c-6.us-east-2.aws.neon.tech/neondb?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=npg_0IFZmYOw1iqM
FLYWAY_ENABLED=true
JPA_DDL_AUTO=validate
INTERNAL_API_SECRET=BankCore360SuperSecretInternalKey2026Secure
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://*.vercel.app
```
*(Note: `AI_SERVICE_URL` hum Step 3 me add karenge jab AI Service ka URL mil jayega).*

6. Click karein **Create Web Service**.
7. Logs me `Started BankCore360Application` aane tak 2-3 minute wait karein.
8. **Apna Backend URL note karein:** (e.g. `https://bankcore360-api.onrender.com`).
   - Browser me test karein: `https://bankcore360-api.onrender.com/actuator/health`
   - Response: `{"status":"UP"}`

---

## 🟣 STEP 2: Render par AI Service (FastAPI) Deploy Karna

1. Render dashboard me phir se **New +** $\rightarrow$ **Web Service** par click karein.
2. Wahi repository dobara select karein (`BankCore360--Digital-Banking-Platform`).
3. Settings fill karein:
   - **Name:** `bankcore360-ai`
   - **Region:** `Ohio (US East)`
   - **Branch:** `main`
   - **Root Directory:** `ai-service` *(⚠️ Bohat zaroori: ise `ai-service` set karein)*
   - **Runtime:** `Python 3`
   - **Build Command:** `pip install -r requirements.txt`
   - **Start Command:** `uvicorn app.main:app --host 0.0.0.0 --port $PORT`
   - **Instance Type:** `Free` ya `Starter`
4. **Environment Variables** add karein:

```env
PORT=8000
ENVIRONMENT=production
SPRING_BOOT_API=https://bankcore360-api.onrender.com
INTERNAL_API_SECRET=BankCore360SuperSecretInternalKey2026Secure
DATABASE_URL=postgresql://neondb_owner:npg_0IFZmYOw1iqM@ep-morning-lake-b4h65mvq-pooler.c-6.us-east-2.aws.neon.tech/neondb?sslmode=require
OPENAI_API_KEY=sk-... (Aapki OpenAI API Key)
OPENAI_MODEL=gpt-4o-mini
MOCK_LLM_RESPONSES=false
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://*.vercel.app
```
*(Yahan `SPRING_BOOT_API` me Step 1 ka exact Backend URL paste karein).*

5. Click karein **Create Web Service**.
6. **Apna AI Service URL note karein:** (e.g. `https://bankcore360-ai.onrender.com`).
   - Browser me test karein: `https://bankcore360-ai.onrender.com/`
   - Response: Status `online` dikhna chahiye.

---

## 🔄 STEP 3: Backend me AI Service URL Link Karna

1. Render dashboard me **Backend Service** (`bankcore360-api`) par jayein.
2. Left menu me **Environment** tab par click karein.
3. Ek naya variable add karein:

```env
AI_SERVICE_URL=https://bankcore360-ai.onrender.com
```
*(Step 2 me mila hua AI Service URL bina trailing slash ke).*

4. **Save Changes** par click karein. Render automatically redeploy karega.

---

## 🌐 STEP 4: Vercel par Frontend (Next.js) Deploy Karna

1. **[vercel.com](https://vercel.com)** par login karein.
2. **Add New...** $\rightarrow$ **Project** par click karein.
3. Apna GitHub repo `BankCore360--Digital-Banking-Platform` select karke **Import** karein.
4. Project Configuration me:
   - **Root Directory:** Edit par click karein aur `frontend` select karein.
   - **Framework Preset:** `Next.js` (auto-detected).
5. **Environment Variables** expand karein aur ye do variables dalein:

```env
NEXT_PUBLIC_API_URL=https://bankcore360-api.onrender.com
NEXT_PUBLIC_AI_URL=https://bankcore360-ai.onrender.com
```
*(Step 1 aur Step 2 ke exact URLs paste karein).*

6. Click karein **Deploy**.
7. 1-2 minute me build complete ho jayega aur aapko live link mil jayega (e.g. `https://bankcore360.vercel.app`).

---

## 🔒 STEP 5: Final CORS Domain Whitelisting

Ab aapke paas permanent Vercel domain aa gaya hai (e.g. `https://bankcore360.vercel.app`):
1. Render Backend (`bankcore360-api`) $\rightarrow$ **Environment** me update karein:
   ```env
   CORS_ALLOWED_ORIGINS=https://bankcore360.vercel.app,http://localhost:3000
   ```
2. Render AI Service (`bankcore360-ai`) $\rightarrow$ **Environment** me update karein:
   ```env
   CORS_ALLOWED_ORIGINS=https://bankcore360.vercel.app,http://localhost:3000
   ```
3. Save changes karein.

---

## 🧪 STEP 6: Live Production Verification Checklist

Apne Vercel link (`https://bankcore360.vercel.app`) ko open karke verify karein:

- [ ] **Login / Authentication:**
  - Login karein: Username: `admin` | Password: `Password@123`
  - Naya customer account register karke check karein.
- [ ] **Account & Ledger Balances:**
  - Customer dashboard open karein aur accounts & balance fetch hone ki pushti karein.
- [ ] **Fund Transfer:**
  - Ek account se dusre account me transfer karein.
  - Double-entry ledger entries check karein.
- [ ] **Multi-Agent AI Banking Assistant:**
  - `/customer/ai-assistant` par jayein.
  - RAG prompt test karein: *"What is the interest rate for a personal loan?"*
  - Insights prompt test karein: *"Show my spending breakdown."*
- [ ] **Admin Event Stream:**
  - `/admin/events` par outbox events stream aur telemetry verify karein.

---

## ⚡ STEP 7: Free 24/7 Keep-Alive (Render Cold Start Fix)

Render Free tier 15 minute inactivity ke baad sleep mode me chala jata hai. Use 24/7 warm rakhne ke liye:
1. **[cron-job.org](https://cron-job.org)** par free account banayein.
2. **Cron Job 1:**
   - **URL:** `https://bankcore360-api.onrender.com/actuator/health`
   - **Schedule:** Every 10 minutes
3. **Cron Job 2:**
   - **URL:** `https://bankcore360-ai.onrender.com/`
   - **Schedule:** Every 10 minutes
4. Ab aapke Render containers kabhi sleep nahi honge aur har user request instant response degi!
