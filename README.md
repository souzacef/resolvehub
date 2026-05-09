# ResolveHub

ResolveHub is a portfolio-grade customer support ticket platform focused on backend engineering quality, practical product workflows, and deployment-ready project discipline.

The repository currently contains a clean, reviewed foundation: architecture decisions, testing strategy, AI provider design, local infrastructure, and CI scaffolding.

## Mission

Build a professional full-stack support platform that demonstrates:

- organization-based multi-tenancy
- secure authentication and authorization
- ticket workflow and SLA handling
- auditability and operational reliability
- AI-assisted classification with safe fallbacks
- realistic tests, documentation, and delivery workflow

## Product scope

ResolveHub helps organizations manage customer support tickets from creation to closure.

Primary roles:

- `CUSTOMER`: creates and follows their own tickets
- `AGENT`: handles assigned or unassigned tickets in their organization
- `MANAGER`: manages assignment, escalations, and workflow operations
- `ADMIN`: manages organization-level users and settings

Ticket lifecycle:

```text
OPEN -> IN_PROGRESS -> WAITING_CUSTOMER -> RESOLVED -> CLOSED
```

Controlled reopen behavior:

```text
RESOLVED -> IN_PROGRESS
CLOSED -> IN_PROGRESS (MANAGER or ADMIN only)
```

## Tech stack

Backend:

- Java 21
- Spring Boot
- Spring Security + JWT
- PostgreSQL + JPA/Hibernate + Flyway
- Validation + OpenAPI/Swagger
- JUnit 5 + Testcontainers

Frontend:

- React
- TypeScript
- Vite

Infrastructure:

- Docker Compose
- GitHub Actions
- Local Ollama-compatible AI endpoint

## Repository layout

```text
resolvehub/
  backend/
  frontend/
  docs/
    architecture.md
    ai-provider-design.md
    testing-strategy.md
    deployment.md
  .github/
    pull_request_template.md
    workflows/ci.yml
  docker-compose.yml
  README.md
  .gitignore
```

## Local infrastructure

Start the foundational services:

```bash
docker compose up -d db ollama
```

Start application containers when backend/frontend Dockerfiles are available:

```bash
docker compose --profile app up --build
```

## Frontend local run

The frontend lives in `/frontend` and expects the backend at `http://localhost:8080` by default.

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

Build check:

```bash
npm run build
```

## Development standards

- Keep changes small and reviewable.
- Enforce tenant boundaries and authorization in backend logic.
- Add or update tests with behavior changes.
- Keep docs aligned with implementation changes.
- Never commit secrets.

Recommended branches:

```text
feature/<issue-number>-short-description
fix/<issue-number>-short-description
docs/<issue-number>-short-description
```

Recommended commit format:

```text
type(scope): short description
```

## Documentation index

- Architecture: [docs/architecture.md](docs/architecture.md)
- AI provider design: [docs/ai-provider-design.md](docs/ai-provider-design.md)
- Testing strategy: [docs/testing-strategy.md](docs/testing-strategy.md)
- Deployment notes: [docs/deployment.md](docs/deployment.md)
