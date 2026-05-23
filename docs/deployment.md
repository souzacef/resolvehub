# Deployment Notes

## Scope

This document covers local developer deployment and portfolio evaluation setup for ResolveHub v1.0.0.
It also includes a production-minded deployment path for Render.

## Prerequisites

- Docker and Docker Compose
- Java 21
- Node.js 22+

## Local Run (Recommended)

### 1) Start PostgreSQL

```bash
docker compose up -d db
```

Optional AI runtime for OpenAI-compatible mode:

```bash
docker compose up -d ollama
```

### 2) Run backend

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Backend URL:

- `http://localhost:8080`

Swagger UI in dev profile:

- `http://localhost:8080/swagger-ui.html`

### 3) Run frontend

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

Frontend URL:

- `http://localhost:5173`

## Test and Build Commands

Backend:

```bash
cd backend
./mvnw clean verify
```

Frontend:

```bash
cd frontend
npm test -- --run
npm run build
```

GitHub Actions CI validates backend tests, frontend tests/build, and Docker image builds.

## Docker Compose App Profile

ResolveHub includes `backend` and `frontend` services under the `app` profile:

```bash
docker compose --profile app up --build
```

This is useful for containerized local demos. For AI behavior, keep backend provider configuration aligned with current application properties (`fake` or `openai-compatible`).
The current Compose backend service is configured for `openai-compatible` mode against the local `ollama` service.

## Render Deployment

Recommended topology:

- Render PostgreSQL service
- Render Web Service for backend (`/backend`)
- Render Static Site for frontend (`/frontend`)

You can also run frontend as a Render Web Service using `frontend/Dockerfile` if needed.

### 1) Create PostgreSQL database

1. In Render, create a new PostgreSQL database.
2. Copy the connection values for:
   - host
   - database name
   - username
   - password
3. Build JDBC URL in this format:
   - `jdbc:postgresql://<host>:5432/<database>`

### 2) Deploy backend service

Create a new Render Web Service from this repository:

- Root Directory: `backend`
- Runtime: Docker (or Java native build if preferred)
- Health Check Path: `/api/health` (or `/actuator/health`)

### 3) Set backend environment variables

Required:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<database>`
- `SPRING_DATASOURCE_USERNAME=<username>`
- `SPRING_DATASOURCE_PASSWORD=<password>`
- `JWT_SECRET=<strong-random-secret-at-least-32-characters>`
- `RESOLVEHUB_SECURITY_CORS_ALLOWED_ORIGINS=https://<your-frontend-domain>`
- `RESOLVEHUB_AI_PROVIDER=fake` (recommended for hosted demo if no reachable AI provider)

Optional (only when `RESOLVEHUB_AI_PROVIDER=openai-compatible`):

- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL=<reachable-provider-url>`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_API_KEY=<provider-key>`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL=<model-name>`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_TIMEOUT_SECONDS=20`

Notes:

- `RESOLVEHUB_SECURITY_CORS_ALLOWED_ORIGINS` is the project’s CORS origins variable (equivalent to a generic `CORS_ALLOWED_ORIGINS` setting).
- If using OpenAI-compatible mode, provider URL must be reachable from Render. Local-only URLs such as `http://127.0.0.1:11434` will not work.

### 4) Deploy frontend

Preferred: Render Static Site.

- Root Directory: `frontend`
- Build Command: `npm ci && npm run build`
- Publish Directory: `dist`

Alternative: Render Web Service using Docker with `frontend/Dockerfile`.

### 5) Set frontend environment variables

Required:

- `VITE_API_BASE_URL=https://<your-backend-domain>`

Important:

- Vite reads `VITE_API_BASE_URL` at build time, not runtime.
- For Docker frontend builds, pass it as a build argument (already supported by `frontend/Dockerfile` via `ARG VITE_API_BASE_URL`).

### 6) Verify health endpoint and login

1. Open backend health endpoint:
   - `https://<your-backend-domain>/api/health`
2. Open frontend URL and log in.
3. Verify ticket list loads and authenticated API calls succeed.
4. If using `fake` AI provider, confirm AI suggestion endpoint works without external provider dependencies.

## Environment Variables

### Backend

- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_SECONDS`
- `CORS_ALLOWED_ORIGINS`
- `RESOLVEHUB_SECURITY_CORS_ALLOWED_ORIGINS`
- `RESOLVEHUB_AI_PROVIDER`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_API_KEY`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_TIMEOUT_SECONDS`
- `RESOLVEHUB_SEED_DEMO_ENABLED`

### Frontend

- `VITE_API_BASE_URL`

Default local frontend target:

- `VITE_API_BASE_URL=http://localhost:8080`

## Local CORS

Backend allows local frontend origins by default:

- `http://localhost:5173`
- `http://127.0.0.1:5173`

Override example:

```bash
export RESOLVEHUB_SECURITY_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

## Demo Credentials

When backend runs with `dev` profile (default) or demo seed is enabled:

- `admin@resolvehub.dev` / `Password123!`
- `manager@resolvehub.dev` / `Password123!`
- `agent@resolvehub.dev` / `Password123!`
- `customer@resolvehub.dev` / `Password123!`

Seed behavior:

- idempotent
- single coherent demo organization
- sample tickets, comments, and assignment

If your existing local data was seeded before demo-data consistency fixes, reset local database volumes and reseed.

## Production Hardening Notes

- Replace default database credentials.
- Use strong, managed secrets for `JWT_SECRET` and provider keys.
- Restrict CORS to trusted production origins.
- Run with non-dev profile and disable demo seeding.
- Add TLS, monitoring, and backup strategy before production usage.
