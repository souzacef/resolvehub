# ResolveHub

ResolveHub is a portfolio-ready full-stack customer support ticket platform. It demonstrates backend architecture, security, product workflows, testing discipline, and deployment readiness.

## Live Demo

- Frontend: https://resolvehub-frontend.onrender.com
- Backend health: https://resolvehub-0ssp.onrender.com/actuator/health
- API docs: https://resolvehub-0ssp.onrender.com/swagger-ui/index.html
- Hosted demo data guide: [docs/demo-data.md](docs/demo-data.md)
- Deployment: Hosted on Render (frontend static site + backend web service + managed PostgreSQL)

Note: the hosted backend may take a moment to wake up on the first request.

## What This Project Demonstrates

- Organization-based multi-tenancy
- Role-based access control with JWT authentication
- Ticket workflow management (creation, status transitions, assignment, comments)
- SLA deadline calculation and overdue tracking
- Append-only audit logging for ticket lifecycle events
- AI-assisted ticket classification through a provider abstraction
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
- SLA deadline calculation and overdue detection
- Expired-session handling (JWT `exp` checks + authenticated `401` recovery to login)
- Ticket audit logs
- AI-assisted ticket classification (advisory suggestions)
- Manual apply workflow for AI category/priority suggestions
- Ticket search and filtering (search, status, priority, category, overdue)
- Render deployment (frontend + backend + managed PostgreSQL)
- Dev demo data seeding
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
   AI suggestions provide advisory category/priority guidance without auto-applying ticket changes.
5. Audit log and comments
   ![ResolveHub audit log and comments](docs/images/audit-log.png)
   Audit logs and comments provide a clear, chronological trace of ticket communication and lifecycle actions.

## Role Behavior

- `CUSTOMER`: creates tickets and views/comments on own tickets
- `AGENT`: manages tickets in own organization and can create tickets on behalf of customers
- `MANAGER`: manages tickets, can create tickets on behalf of customers, and can create organization users (`CUSTOMER`, `AGENT`)
- `ADMIN`: full organization ticket management, can create tickets on behalf of customers, and can create organization users (`CUSTOMER`, `AGENT`, `MANAGER`, `ADMIN`)

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
- Docker
- Docker Compose
- GitHub Actions
- Render
- Managed PostgreSQL

## Architecture Overview

- React frontend calls a secured Spring Boot API.
- JWT carries user id, organization id, and role claims.
- Backend enforces authorization and organization scoping.
- PostgreSQL stores organizations, users, tickets, comments, and audit logs.
- AI classification is provider-agnostic (`fake` by default, OpenAI-compatible optional).

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

Optional (for local AI provider testing):

```bash
docker compose up -d ollama
```

### 2) Run backend

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`.

Swagger UI (dev profile): `http://localhost:8080/swagger-ui.html`

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

Start full app stack (DB + Ollama + backend + frontend):

```bash
docker compose --profile app up --build
```

Notes:
- App services in Compose run with Docker profile configuration.
- If you want backend AI classification to use Ollama, set `RESOLVEHUB_AI_PROVIDER=openai-compatible` for the backend environment.

## Demo Credentials

When running backend with default `dev` profile, demo data is seeded:

- `admin@resolvehub.dev` / `Password123!`
- `manager@resolvehub.dev` / `Password123!`
- `agent@resolvehub.dev` / `Password123!`
- `customer@resolvehub.dev` / `Password123!`

## AI Behavior

- AI suggestions are advisory only.
- Suggestions do not automatically update ticket category or priority.
- `fake` provider is default.
- OpenAI-compatible provider can be enabled for Ollama (`http://127.0.0.1:11434/v1`).

See [docs/ai-provider-design.md](docs/ai-provider-design.md).

## CI/CD Status

GitHub Actions workflow: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

GitHub Actions currently provides CI (validation, test, and build checks):
- repository validation
- backend build and tests (`./mvnw clean verify`)
- frontend build and tests (`npm run build`, `npm test -- --run`)
- backend Docker image build validation
- frontend Docker image build validation

Deployment is hosted on Render (frontend, backend, and managed PostgreSQL).
Auto-deploy behavior depends on your Render service settings and branch configuration.

## Documentation

- Demo data setup: [docs/demo-data.md](docs/demo-data.md)
- Architecture: [docs/architecture.md](docs/architecture.md)
- AI provider design: [docs/ai-provider-design.md](docs/ai-provider-design.md)
- Testing strategy: [docs/testing-strategy.md](docs/testing-strategy.md)
- Deployment notes: [docs/deployment.md](docs/deployment.md)
- Release readiness: [docs/v1.0.0-readiness-checklist.md](docs/v1.0.0-readiness-checklist.md)

## Roadmap

- Server-side pagination and filtering for high-volume ticket lists
- End-to-end workflow tests across frontend and backend
- Refresh-token support and session lifecycle hardening
- Observability (centralized logs, traces, metrics, and alerting)
- Custom domain and production TLS hardening for hosted environments
- Secrets and security hardening for production operations
