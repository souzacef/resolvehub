# ResolveHub

ResolveHub is a portfolio-ready full-stack customer support ticket platform. It demonstrates backend architecture, security, product workflows, testing discipline, deployment, auditability, and human-in-the-loop AI assistance.

## Live Demo

- Frontend: https://resolvehub-frontend.onrender.com
- Service status: https://resolvehub-frontend.onrender.com/status
- API docs: Swagger/OpenAPI is available locally in the dev profile at `http://localhost:8080/swagger-ui.html`.
- Hosted demo data guide: [docs/demo-data.md](docs/demo-data.md)
- Deployment: Render frontend + backend, with persistent Neon PostgreSQL 16

Note: the hosted backend uses Render's free tier and may take a couple of minutes to wake after inactivity. The frontend service-status page polls the backend and reports when ResolveHub is ready.

## What This Project Demonstrates

- Organization-based multi-tenancy
- Role-based access control with JWT authentication
- Ticket workflow management (creation, status transitions, assignment, comments)
- SLA deadline calculation and overdue tracking
- Append-only audit logging for ticket lifecycle events
- AI-assisted ticket classification through a provider abstraction
- Human review before AI suggestions can change ticket data
- Persistent PostgreSQL deployment with Flyway migrations
- CI validation with backend/frontend tests and Docker image builds

## Implemented Features

- Organization-based multi-tenancy
- JWT authentication and role-based access control
- Frontend registration flow for new organizations and initial admin onboarding
- Organization user management (list + create users with role constraints)
- Ticket creation, listing, detail view, and status workflow
- Staff ticket creation on behalf of customers
- Human-readable ticket numbers (`RH-1001`, `RH-1002`, ...) with UUIDs retained for internal routing
- Searchable customer selector for staff requester selection
- Role-aware dashboard metrics for customer and staff personas
- Ticket comments (public/internal rules by role)
- Ticket assignment with role constraints
- Agent visibility restricted to unassigned tickets and tickets assigned to that agent
- SLA deadline calculation and overdue detection
- Expired-session handling (JWT `exp` checks + authenticated `401` recovery to login)
- Ticket audit logs
- AI-assisted ticket classification (advisory suggestions)
- Manual apply workflow for AI category/priority suggestions
- Audit logging when staff apply a classification change
- Ticket search and filtering (search, status, priority, category, overdue)
- Public service-status page for Render cold starts
- Render deployment with persistent Neon PostgreSQL
- Controlled demo data seeding
- GitHub Actions CI

## Demo Walkthrough

This walkthrough highlights portfolio-relevant product behavior, including role-aware UI, ticket workflow controls, SLA visibility, auditability, and AI-assisted classification.

1. Login screen
   ![ResolveHub login screen](docs/images/login.png)
   Role-based access starts at authentication and directs users into their permitted workflow.
2. Dashboard with ticket metrics
   ![ResolveHub dashboard](docs/images/dashboard.png)
   The dashboard summarizes operational state across tickets, including priority and SLA context.
3. Ticket detail with workflow, SLA, assignment, and metadata
   ![ResolveHub ticket detail](docs/images/ticket-detail.png)
   Ticket operations are role-aware, with status workflow and assignment bounded by backend authorization rules.
4. AI classification suggestion
   ![ResolveHub AI classification suggestion](docs/images/ai-classification.png)
   AI suggestions provide advisory category/priority guidance without auto-applying ticket changes. The hosted demo uses Gemini through ResolveHub's OpenAI-compatible provider.
5. Audit log and comments
   ![ResolveHub audit log and comments](docs/images/audit-log.png)
   Audit logs and comments provide a chronological trace of ticket communication and lifecycle actions, including applied classification changes.

## Role Behavior

- `CUSTOMER`: creates tickets and views/comments on own tickets
- `AGENT`: sees unassigned tickets plus tickets assigned to self; can self-assign eligible unassigned tickets and work assigned tickets
- `MANAGER`: manages organization tickets, assignments, workflows, and can create `CUSTOMER`/`AGENT` users
- `ADMIN`: full organization ticket management and can create all supported user roles

## Tech Stack

Backend:
- Java 21
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA + Hibernate
- PostgreSQL
- Flyway
- springdoc OpenAPI/Swagger
- JUnit 5 + Testcontainers

Frontend:
- React 18
- TypeScript
- Vite
- Vitest

Infrastructure:
- Docker / Docker Compose
- GitHub Actions
- Render
- Neon PostgreSQL 16

AI:
- Provider abstraction with deterministic fake provider for local/test use
- OpenAI-compatible HTTP provider
- Gemini 3.5 Flash in the hosted Render deployment

## Architecture Overview

- React frontend calls a secured Spring Boot API.
- JWT carries user id, organization id, and role claims.
- Backend enforces authorization and organization scoping.
- Neon PostgreSQL stores organizations, users, tickets, comments, and audit logs in the hosted environment.
- Flyway owns schema evolution and validates migrations at startup.
- AI classification is provider-agnostic: deterministic `fake` mode remains available, while production uses the OpenAI-compatible adapter with Gemini.
- AI suggestions are advisory until a staff user explicitly applies them; the resulting classification update is audit logged.

See [docs/architecture.md](docs/architecture.md) for details.

## Local Setup

### Prerequisites

- Java 21
- Node.js 22+ and npm
- Docker + Docker Compose

### 1) Start infrastructure

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

Backend runs on `http://localhost:8080`.

Swagger UI (dev profile only): `http://localhost:8080/swagger-ui.html`

### 3) Run frontend

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

Frontend runs on `http://localhost:5173`.

## Run Tests

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

## Docker Compose

Start the full local app stack:

```bash
docker compose --profile app up --build
```

The Compose backend can use the OpenAI-compatible adapter against local Ollama by setting `RESOLVEHUB_AI_PROVIDER=openai-compatible`.

## Demo Credentials

When the backend runs with the dev profile, demo data is seeded:

- `admin@resolvehub.dev` / `Password123!`
- `manager@resolvehub.dev` / `Password123!`
- `agent@resolvehub.dev` / `Password123!`
- `customer@resolvehub.dev` / `Password123!`

These credentials are for development/controlled demo environments only.

## AI Behavior

- AI suggestions are advisory only.
- Suggestions do not automatically update ticket category or priority.
- Staff explicitly review and apply a suggestion.
- Applying a suggestion uses the normal classification update path and writes an audit-log event.
- Core ticket operations remain available if the external AI provider is unavailable.
- The hosted Render deployment uses `gemini-3.5-flash` through Google's OpenAI-compatible Gemini endpoint.
- Local development/tests can continue using the deterministic `fake` provider or local Ollama.

Hosted provider configuration uses environment variables only; API keys are never committed:

```bash
RESOLVEHUB_AI_PROVIDER=openai-compatible
RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL=gemini-3.5-flash
```

See [docs/ai-provider-design.md](docs/ai-provider-design.md).

## Production Deployment

Current hosted topology:

```text
Render Static Site (React)
        |
        v
Render Web Service (Spring Boot)
        |
        +--> Neon PostgreSQL 16 (AWS us-west-2)
        |
        +--> Gemini 3.5 Flash (OpenAI-compatible API)
```

The backend and Neon database are colocated in AWS US West 2 / Oregon to reduce database latency. Flyway migrations rebuild an empty database and validate the current schema automatically.

See [docs/deployment.md](docs/deployment.md) for environment variables and recovery/seeding notes.

## CI/CD Status

GitHub Actions workflow: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

GitHub Actions currently provides CI validation for:
- repository structure
- backend build and tests (`./mvnw clean verify`)
- frontend build and tests (`npm run build`, `npm test -- --run`)
- backend Docker image build
- frontend Docker image build

Render handles hosted deployment from the configured branch/service settings.

## Documentation

- Demo data setup: [docs/demo-data.md](docs/demo-data.md)
- Architecture: [docs/architecture.md](docs/architecture.md)
- AI provider design: [docs/ai-provider-design.md](docs/ai-provider-design.md)
- Testing strategy: [docs/testing-strategy.md](docs/testing-strategy.md)
- Deployment notes: [docs/deployment.md](docs/deployment.md)
- Release readiness: [docs/v1.0.0-readiness-checklist.md](docs/v1.0.0-readiness-checklist.md)

## Roadmap

- Admin-assisted password reset / user lifecycle controls
- Server-side pagination and filtering for high-volume ticket lists
- End-to-end workflow tests across frontend and backend
- Refresh-token support and session lifecycle hardening
- Observability (centralized logs, traces, metrics, and alerting)
- Custom domain and production TLS hardening for hosted environments
- Secrets and security hardening for production operations
