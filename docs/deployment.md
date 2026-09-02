# Deployment Notes

## Scope

This document covers local development and the current hosted portfolio deployment for ResolveHub.

Current hosted topology:

```text
Render Static Site (React frontend)
        |
        v
Render Web Service (Spring Boot backend, Oregon)
        |
        +--> Neon PostgreSQL 16 (AWS us-west-2 / Oregon)
        |
        +--> Gemini 3.5 Flash via Google's OpenAI-compatible endpoint
```

The database is external to Render so it is not tied to the lifecycle of Render's free database offering.

## Prerequisites

- Docker and Docker Compose
- Java 21
- Node.js 22+

## Local Run

### 1) Start PostgreSQL

```bash
docker compose up -d db
```

Optional local AI runtime:

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

Swagger UI in dev profile only:

- `http://localhost:8080/swagger-ui.html`

The production profile disables Swagger/OpenAPI endpoints.

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

GitHub Actions validates backend tests, frontend tests/build, and Docker image builds.

## Docker Compose App Profile

Start the containerized local stack with:

```bash
docker compose --profile app up --build
```

The Compose backend can use `openai-compatible` mode against the local Ollama service. The hosted production deployment uses the same provider abstraction with Gemini instead.

## Hosted Render + Neon Deployment

### 1) Create the Neon database

The current portfolio deployment uses:

- PostgreSQL 16
- Neon Auth disabled
- AWS US West 2 (Oregon), matching the Render backend region
- direct database connection

For a Spring/JDBC connection, convert Neon's connection string into JDBC form.

Neon-style URL:

```text
postgresql://<username>:<password>@<host>/<database>?sslmode=require
```

ResolveHub datasource URL:

```text
jdbc:postgresql://<host>/<database>?sslmode=require
```

Keep username and password in separate Render environment variables. Never commit the password or full credential-bearing Neon connection string.

### 2) Configure the Render backend

Current backend service shape:

- Root Directory: `backend`
- Runtime: Docker
- Region: Oregon
- Production profile: `prod`

Recommended health endpoint:

- `/actuator/health`

The frontend also exposes a public `/status` page that repeatedly checks the backend while a free Render instance wakes.

### 3) Set datasource and security environment variables

Required production values:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://<neon-host>/<database>?sslmode=require
SPRING_DATASOURCE_USERNAME=<neon-role>
SPRING_DATASOURCE_PASSWORD=<neon-password>
JWT_SECRET=<strong-random-secret-at-least-32-characters>
CORS_ALLOWED_ORIGINS=https://<your-frontend-domain>
```

`RESOLVEHUB_SECURITY_CORS_ALLOWED_ORIGINS` is also supported as a fallback configuration name, but `CORS_ALLOWED_ORIGINS` is the primary environment variable read by the current application configuration.

### 4) Configure real Gemini classification

The hosted deployment uses ResolveHub's existing OpenAI-compatible adapter against Google's Gemini API.

```text
RESOLVEHUB_AI_PROVIDER=openai-compatible
RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
RESOLVEHUB_AI_OPENAI_COMPATIBLE_API_KEY=<gemini-api-key>
RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL=gemini-3.5-flash
RESOLVEHUB_AI_OPENAI_COMPATIBLE_TIMEOUT_SECONDS=30
```

The API key must exist only in Render's secret environment configuration.

Core ticket operations do not call Gemini. AI is invoked only when an eligible staff user requests a classification suggestion.

### 5) Deploy the frontend

Preferred Render Static Site settings:

- Root Directory: `frontend`
- Build Command: `npm ci && npm run build`
- Publish Directory: `dist`

Required frontend environment variable:

```text
VITE_API_BASE_URL=https://<your-backend-domain>
```

Vite reads `VITE_API_BASE_URL` at build time.

### 6) Flyway database initialization

ResolveHub uses Flyway with Hibernate `ddl-auto: validate`.

On a new empty Neon database, application startup should:

1. establish the PostgreSQL connection;
2. create Flyway's schema history table;
3. apply all versioned migrations in order;
4. validate the resulting schema;
5. start the Spring Boot application.

The current schema is represented by migrations `V1` through `V8`.

Do not create production tables manually when Flyway can build the schema from an empty database.

### 7) Seed a controlled hosted demo once

Production defaults to demo seeding disabled.

For a brand-new portfolio database, the existing idempotent demo seeder can initialize the controlled demo dataset:

```text
RESOLVEHUB_SEED_DEMO_ENABLED=true
```

Deploy once, verify the demo organization/users/tickets exist, then change it back to:

```text
RESOLVEHUB_SEED_DEMO_ENABLED=false
```

Redeploy and confirm the data remains present. Disabling the seeder does not delete already-persisted data.

Leaving the flag disabled after initialization prevents future application restarts from reapplying the canonical demo state.

### 8) Production verification

After deployment, verify this sequence:

1. Open `https://<frontend-domain>/status` and wait for the backend-ready state.
2. Log in and confirm seeded/persisted tickets load.
3. Create or modify a record and confirm it survives a backend redeploy.
4. Request an AI classification as `AGENT`, `MANAGER`, or `ADMIN`.
5. Review the suggestion before applying it.
6. Apply the suggestion and confirm category/priority change.
7. Confirm the classification change appears in the ticket audit log.

A useful semantic-AI smoke test is to create a deliberately misclassified ticket whose description clearly implies a security incident, request AI classification, and verify that Gemini can recommend a more appropriate `SECURITY` classification based on the text rather than the ticket's existing category/priority.

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
- `SERVER_PORT`

### Frontend

- `VITE_API_BASE_URL`

Default local frontend target:

```text
VITE_API_BASE_URL=http://localhost:8080
```

## Local CORS

Backend allows local frontend origins by default:

- `http://localhost:5173`
- `http://127.0.0.1:5173`

Override example:

```bash
export CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

## Demo Credentials

When the backend runs with the dev profile, or when the controlled demo seeder is explicitly enabled, the canonical seeded accounts are:

- `admin@resolvehub.dev` / `Password123!`
- `manager@resolvehub.dev` / `Password123!`
- `agent@resolvehub.dev` / `Password123!`
- `customer@resolvehub.dev` / `Password123!`

Use these only for local development or a disposable portfolio demo environment.

## Operational Notes

- Render free web services can cold-start slowly. The `/status` frontend route is designed to make that wait understandable to users.
- Neon compute may also scale to zero when inactive, so the first database connection can take longer than a warm connection.
- A transient Render-to-Neon connection stall can prevent a new instance from binding its HTTP port before Render's deployment timeout. A retry may succeed, but repeated stalls should be addressed with explicit JDBC connection/startup timeouts rather than endless manual retries.
- Keep provider and database secrets out of logs, documentation examples, commits, and screenshots.

## Production Hardening Notes

- Use strong managed secrets for JWT and provider keys.
- Restrict CORS to trusted production origins.
- Keep `RESOLVEHUB_SEED_DEMO_ENABLED=false` after controlled demo initialization.
- Add database backups/restore procedures for non-demo production use.
- Add centralized monitoring/alerting for backend, database, and AI-provider failures.
- Consider explicit JDBC connection timeouts and connection-pool tuning for cold-start resilience.
