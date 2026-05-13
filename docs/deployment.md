# Deployment Notes

## Scope

This document covers local developer deployment and portfolio evaluation setup for ResolveHub v1.0.0.

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
./mvnw spring-boot:run
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

CI validates application tests, frontend build, and Docker image builds.

## Docker Compose App Profile

ResolveHub includes `backend` and `frontend` services under the `app` profile:

```bash
docker compose --profile app up --build
```

This is useful for containerized local demos. For AI behavior, keep backend provider configuration aligned with current application properties (`fake` or `openai-compatible`).
The current Compose backend service is configured for `openai-compatible` mode against the local `ollama` service.

## Environment Variables

### Backend

- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_SECONDS`
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
