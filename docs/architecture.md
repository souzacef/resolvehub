# ResolveHub Architecture

## Overview

ResolveHub is a full-stack customer support ticket platform designed around organization-scoped data, role-based access control, auditable ticket operations, persistent relational storage, and optional AI-assisted classification.

Core principles:

- Backend is the source of truth for authorization and business rules.
- Multi-tenancy is enforced by organization scoping on data access.
- Agent visibility is narrower than manager/admin visibility.
- AI suggestions are advisory and isolated from core ticket workflows.
- Persisted business changes remain explicit human actions and are audit logged.
- Database schema evolution is owned by Flyway rather than Hibernate auto-generation.

## High-level System

```text
React (Vite, TypeScript)
  |
  |  JWT Bearer API calls
  v
Spring Boot API (Render)
  |
  +--> PostgreSQL
  |      +--> local Docker PostgreSQL in development
  |      +--> Neon PostgreSQL 16 in hosted production
  |
  +--> Ticket AI Classifier abstraction
         +--> Fake provider (local/test/default fallback)
         +--> OpenAI-compatible provider
                +--> local Ollama
                +--> Gemini 3.5 Flash in hosted production
```

The hosted Render backend and Neon database are both located in the Oregon / AWS US West 2 region to reduce database latency.

## Backend Modules

- `auth`: register/login endpoints, password hashing, JWT issuance
- `common.security`: JWT filter, principal mapping, security configuration
- `organization`: tenant model
- `user`: user model and role model
- `ticket`: ticket creation/list/detail, classification, status workflow, assignment, SLA due date, overdue filtering
- `ticketcomment`: ticket comment creation/listing with internal/public visibility rules
- `audit`: append-only ticket audit logs and visibility rules
- `ai`: classifier contract + fake provider + OpenAI-compatible provider
- `seed`: controlled demo-data seeding

## Core Domain Model

### Organization

- `id`
- `name` (unique)
- `status`
- `createdAt`
- `updatedAt`

### User

- `id`
- `organization`
- `name`
- `email` (unique)
- `passwordHash` (BCrypt)
- `role` (`CUSTOMER`, `AGENT`, `MANAGER`, `ADMIN`)
- `status`
- `createdAt`
- `updatedAt`

### Ticket

- `id` (UUID internal key)
- `ticketNumber` (human-readable reference, e.g. `RH-1001`)
- `organization`
- `requester`
- `assignee` (nullable)
- `title`
- `description`
- `status` (`OPEN`, `IN_PROGRESS`, `WAITING_CUSTOMER`, `RESOLVED`, `CLOSED`)
- `priority` (`LOW`, `MEDIUM`, `HIGH`, `URGENT`)
- `category` (`BILLING`, `TECHNICAL`, `ACCOUNT`, `FEATURE_REQUEST`, `SECURITY`, `OTHER`)
- `slaDueAt`
- `createdAt`
- `updatedAt`

### TicketComment

- `id`
- `ticket`
- `author`
- `body`
- `internal`
- `createdAt`

### AuditLog

- `id`
- `organization`
- `actor` (nullable)
- `ticket` (nullable)
- `action` (`TICKET_CREATED`, `TICKET_STATUS_CHANGED`, `TICKET_ASSIGNED`, `TICKET_UNASSIGNED`, `COMMENT_ADDED`, `TICKET_CLASSIFICATION_UPDATED`)
- `details`
- `createdAt`

## Persistence and Migrations

ResolveHub uses PostgreSQL through Spring Data JPA/Hibernate.

Hibernate is configured with:

```text
ddl-auto: validate
```

Flyway is responsible for schema creation/evolution. The current migration chain is `V1` through `V8` and includes the foundation schema, users, tickets, comments, assignment, SLA fields, audit logs, and ticket numbers.

This means a new empty PostgreSQL database can be reconstructed from versioned migrations while Hibernate validates that the mapped entities match the resulting schema.

Hosted production uses persistent Neon PostgreSQL 16 rather than a database tied to Render's free database lifecycle.

## Security and Multi-tenancy

- JWT is required for protected endpoints.
- Public backend endpoints include:
  - `/api/auth/**`
  - `/api/health`
  - `/actuator/health`
  - Swagger/OpenAPI only in the `dev` profile
- CORS is restricted by configured allowed origins.
- Tenant isolation is enforced in service/repository access using the authenticated organization id.
- AI provider credentials remain backend-only environment secrets and are never exposed to the React client.

## Role Behavior

### `CUSTOMER`

- creates tickets for self
- lists/views only own tickets
- comments only on own tickets
- cannot create internal comments
- cannot assign tickets
- cannot view audit logs
- cannot request/apply AI classification

### `AGENT`

- sees unassigned tickets plus tickets assigned to self inside the organization
- can create tickets on behalf of organization customers
- can self-assign an eligible unassigned ticket
- cannot assign tickets to another staff user or unassign them
- can work allowed status transitions on tickets assigned to self
- can create internal/public comments where permitted
- can request and apply AI classification

### `MANAGER`

- sees/manages organization tickets
- can create tickets on behalf of organization customers
- can create `CUSTOMER` and `AGENT` users
- can assign/unassign eligible staff users
- can create internal/public comments
- can request and apply AI classification
- can view ticket audit logs

### `ADMIN`

- sees/manages organization tickets
- can create tickets on behalf of organization customers
- can create `CUSTOMER`, `AGENT`, `MANAGER`, and `ADMIN` users
- can assign/unassign eligible staff users
- can request and apply AI classification
- can view ticket audit logs

## Ticket Workflow

Allowed backend transitions:

- `OPEN -> IN_PROGRESS`
- `OPEN -> CLOSED`
- `IN_PROGRESS -> WAITING_CUSTOMER`
- `IN_PROGRESS -> RESOLVED`
- `IN_PROGRESS -> CLOSED`
- `WAITING_CUSTOMER -> IN_PROGRESS`
- `WAITING_CUSTOMER -> RESOLVED`
- `RESOLVED -> CLOSED`
- `RESOLVED -> IN_PROGRESS`
- `CLOSED ->` no transitions

Customer-specific status permissions:

- `OPEN -> CLOSED`
- `RESOLVED -> IN_PROGRESS`

## SLA and Overdue Rules

SLA due date is set on ticket creation:

- `URGENT`: `createdAt + 4h`
- `HIGH`: `createdAt + 8h`
- `MEDIUM`: `createdAt + 24h`
- `LOW`: `createdAt + 72h`

Overdue is computed dynamically when:

- `slaDueAt < now`, and
- status is not `RESOLVED`, and
- status is not `CLOSED`

No scheduler persists overdue state.

## AI Classification

AI classification is explicit and endpoint-driven:

- `POST /api/tickets/{ticketId}/ai/classification`
- `PATCH /api/tickets/{ticketId}/classification`

Suggestion response contains:

- suggested category
- suggested priority
- short reasoning

The suggestion endpoint is read-only with respect to ticket classification. It calls the configured `TicketAiClassifier`, returns the suggestion, and leaves existing category/priority untouched.

When staff choose to apply a suggestion, the frontend calls the normal classification PATCH endpoint. The backend records old/new category and priority in an audit-log event.

Hosted production uses `OpenAiCompatibleTicketAiClassifier` with Gemini 3.5 Flash. Local/test environments can remain deterministic by using `FakeTicketAiClassifier`.

## Failure Isolation

AI failure does not prevent ticket creation, listing, detail views, comments, assignment, or status changes. Provider errors are surfaced through the AI endpoint as controlled gateway failures.

Database availability remains required for the core application because tickets, identity, and authorization state are persistent relational data.

## Frontend Service Status

The public `/status` route is intentionally separate from authentication. It polls the backend health endpoint and presents a friendly cold-start state while the Render free service wakes.

This keeps users on the ResolveHub frontend instead of exposing the raw Render backend health page as the primary status experience.

## API Documentation and CI

- OpenAPI docs are available only in the `dev` profile through Swagger UI.
- CI validates repository structure, backend tests/build, frontend tests/build, and Docker image builds.
