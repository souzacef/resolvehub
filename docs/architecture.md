# ResolveHub Architecture

## Overview

ResolveHub is a full-stack customer support ticket platform designed around organization-scoped data, role-based access control, auditable ticket operations, and optional AI-assisted classification.

Core principles in the current implementation:

- Backend is the source of truth for authorization and business rules.
- Multi-tenancy is enforced by organization scoping on data access.
- AI suggestions are advisory and isolated from core ticket creation/update flows.

## High-level System

```text
React (Vite, TypeScript)
  |
  |  JWT Bearer API calls
  v
Spring Boot API
  |
  +--> PostgreSQL (organizations, users, tickets, comments, audit logs)
  |
  +--> Ticket AI Classifier abstraction
         +--> Fake provider (default)
         +--> OpenAI-compatible provider (optional, e.g., Ollama)
```

## Backend Modules

- `auth`: register/login endpoints, password hashing, JWT issuance
- `common.security`: JWT filter, principal mapping, security configuration
- `organization`: tenant model
- `user`: user model and role model
- `ticket`: ticket CRUD/listing/detail, status workflow, assignment, SLA due date, overdue filtering
- `ticketcomment`: ticket comment creation/listing with internal/public visibility rules
- `audit`: append-only ticket audit logs and visibility rules
- `ai`: provider abstraction + fake provider + OpenAI-compatible provider
- `seed`: dev-only demo data seeding

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
- `ticketNumber` (human-readable reference, e.g., `RH-1001`)
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

## Security and Multi-tenancy

- JWT is required for all protected endpoints.
- Public endpoints:
  - `/api/auth/**`
  - `/api/health`
  - `/actuator/health`
  - Swagger/OpenAPI endpoints in `dev` profile
- CORS is restricted by configured allowed origins.
- Tenant isolation is enforced in service/repository access using authenticated organization id.

## Role Behavior

- `CUSTOMER`
  - can create tickets for self
  - can list/view only own tickets
  - can comment only on own tickets
  - cannot create internal comments
  - cannot assign tickets
  - cannot view audit logs
- `AGENT`
  - can list/view tickets in own organization
  - can create tickets on behalf of customers in own organization
  - can create internal/public comments in own organization
  - can request AI classification
- `MANAGER`
  - can list/view tickets in own organization
  - can create tickets on behalf of customers in own organization
  - can create organization users with roles `CUSTOMER` or `AGENT`
  - can create internal/public comments in own organization
  - can request AI classification
- `ADMIN`
  - can list/view tickets in own organization
  - can create tickets on behalf of customers in own organization
  - can create organization users with roles `CUSTOMER`, `AGENT`, `MANAGER`, or `ADMIN`
  - can create internal/public comments in own organization
  - can request AI classification
- Assignment-specific behavior:
  - `AGENT` can assign unassigned tickets only to self
  - `MANAGER` and `ADMIN` can assign/unassign eligible staff users in own organization

## Ticket Workflow

Allowed transitions implemented in backend:

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

SLA due date is set on ticket creation only:

- `URGENT`: `createdAt + 4h`
- `HIGH`: `createdAt + 8h`
- `MEDIUM`: `createdAt + 24h`
- `LOW`: `createdAt + 72h`

Overdue is computed dynamically in responses when:

- `slaDueAt < now`, and
- status is not `RESOLVED`, and
- status is not `CLOSED`

No scheduler persists overdue state in v1.0.0.

## AI Classification

AI classification is explicit and endpoint-driven:

- `POST /api/tickets/{ticketId}/ai/classification`
- `PATCH /api/tickets/{ticketId}/classification`

Suggestion response returns:

- suggested category
- suggested priority
- short reasoning

Suggestions do not automatically update the ticket. Staff users explicitly apply classification updates through the classification PATCH endpoint.

## API Documentation and CI

- OpenAPI docs are available in `dev` profile through Swagger UI.
- CI pipeline validates repository structure and runs backend + frontend build/test jobs.
