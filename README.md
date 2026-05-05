# ResolveHub

ResolveHub is a portfolio-grade customer support ticket platform focused on realistic backend engineering, clean product workflows, and AI-assisted support operations.

The project explores agent-assisted software development while keeping the important decisions human-reviewed: architecture, business rules, tests, documentation, and deployment flow.

## What ResolveHub demonstrates

- Customer support ticket workflows
- Organization-based multi-tenancy
- Role-based access control
- JWT authentication
- SLA calculation and overdue detection
- Audit logging for business-critical actions
- AI-assisted ticket classification
- Backend testing with unit and integration tests
- Docker-based local development
- GitHub-centered workflow with CI and pull requests

## Planned stack

### Backend

- Java 21
- Spring Boot
- Spring Security
- JWT
- PostgreSQL
- JPA/Hibernate
- Flyway
- JUnit 5
- Testcontainers
- Swagger/OpenAPI

### Frontend

- React
- TypeScript
- Vite

### Infrastructure

- Docker Compose
- GitHub Actions
- PostgreSQL container
- Local Ollama-compatible AI provider support

## Product concept

ResolveHub helps organizations manage customer support tickets from creation to resolution. It supports customers, support agents, managers, and admins.

The system will help classify tickets using AI, calculate SLA deadlines, track overdue work, and preserve a clear audit trail for status and assignment changes.

## User roles

| Role | Purpose |
|---|---|
| CUSTOMER | Creates tickets and comments on their own tickets |
| AGENT | Handles assigned or unassigned tickets within the organization |
| MANAGER | Manages support workflow, escalations, assignments, and metrics |
| ADMIN | Manages organization settings and users |

## Ticket lifecycle

```text
OPEN -> IN_PROGRESS -> WAITING_CUSTOMER -> RESOLVED -> CLOSED
```

Possible controlled transitions:

```text
RESOLVED -> IN_PROGRESS
CLOSED -> IN_PROGRESS, only by manager/admin
```

## Ticket priorities

```text
LOW
MEDIUM
HIGH
URGENT
```

## Ticket categories

```text
BILLING
TECHNICAL
ACCOUNT
FEATURE_REQUEST
SECURITY
OTHER
```

## MVP scope

Version 1.0.0 should include:

- User registration and login
- JWT-based authentication
- Organization-scoped users and tickets
- Role-based authorization
- Ticket creation
- Ticket listing and filtering
- Ticket assignment
- Ticket comments
- Ticket status workflow validation
- SLA policy calculation
- Overdue ticket detection
- Audit logging
- AI provider abstraction
- Local Ollama-based AI classification
- Fake AI provider for tests
- Docker Compose setup
- GitHub Actions CI
- API documentation
- Professional README and documentation

## Out of scope for v1.0.0

- Real email delivery
- Real payment/billing integration
- Complex analytics dashboards
- Mobile app
- Multi-language UI
- Full production Kubernetes deployment
- Automatic AI decision-making without human approval

## Local development target

The intended local development flow:

```bash
docker compose up -d db ollama
cd backend
./mvnw spring-boot:run
cd ../frontend
npm install
npm run dev
```

Backend and frontend implementation will be added incrementally.

## Repository structure

```text
resolvehub/
  backend/
    src/
    pom.xml
    Dockerfile
  frontend/
    src/
    package.json
    Dockerfile
  docs/
    architecture.md
    ai-provider-design.md
    testing-strategy.md
    deployment.md
  .github/
    workflows/
      ci.yml
    pull_request_template.md
  docker-compose.yml
  README.md
  .gitignore
```

## Development workflow

Recommended branch pattern:

```text
main
feature/<issue-number>-short-description
fix/<issue-number>-short-description
docs/<issue-number>-short-description
```

Recommended commit format:

```text
type(scope): short description
```

Examples:

```text
chore(repo): add initial project skeleton
docs(architecture): document backend module boundaries
feat(auth): add JWT login endpoint
test(tickets): cover invalid status transitions
```

## Project status

Current status: repository skeleton and planning phase.

Next implementation step: create the backend Spring Boot foundation with health checks, database connectivity, Flyway setup, and authentication scaffolding.
