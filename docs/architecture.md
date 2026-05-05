# ResolveHub Architecture

## Overview

ResolveHub is a full-stack customer support ticket platform. The system is designed around organization-scoped data, role-based permissions, auditable workflow changes, and AI-assisted classification.

The MVP should remain modular without becoming over-engineered. The backend owns the business rules. The frontend provides a clean operational interface. The AI layer suggests classifications but does not replace human approval.

## High-level system

```text
Browser
  |
  v
React Frontend
  |
  v
Spring Boot API
  |
  +--> PostgreSQL
  |
  +--> AI Provider Abstraction
          |
          +--> Ollama Provider
          +--> OpenAI Provider, future
          +--> Fake Provider, tests
```

## Backend responsibilities

The backend is responsible for:

- Authentication and JWT issuance
- Authorization and role checks
- Organization-based multi-tenancy
- Ticket workflow validation
- SLA deadline calculation
- Overdue ticket detection
- Audit log creation
- AI classification orchestration
- API documentation

## Frontend responsibilities

The frontend is responsible for:

- Login flow
- Ticket dashboard
- Ticket creation form
- Ticket detail view
- Commenting UI
- Role-aware actions
- Clear display of SLA status and priority

The frontend should not enforce business rules as the source of truth. It may hide unavailable actions for usability, but the backend must validate every important operation.

## Core domain model

### Organization

Represents a tenant. Users, tickets, SLA policies, and audit logs belong to an organization.

Important fields:

- id
- name
- status
- createdAt
- updatedAt

### User

Represents an authenticated system user.

Important fields:

- id
- organizationId
- name
- email
- passwordHash
- role
- status
- createdAt
- updatedAt

Roles:

```text
CUSTOMER
AGENT
MANAGER
ADMIN
```

### Ticket

Represents a support request.

Important fields:

- id
- organizationId
- createdByUserId
- assignedAgentId
- title
- description
- status
- priority
- category
- slaDeadlineAt
- resolvedAt
- closedAt
- createdAt
- updatedAt

Statuses:

```text
OPEN
IN_PROGRESS
WAITING_CUSTOMER
RESOLVED
CLOSED
```

Priorities:

```text
LOW
MEDIUM
HIGH
URGENT
```

Categories:

```text
BILLING
TECHNICAL
ACCOUNT
FEATURE_REQUEST
SECURITY
OTHER
```

### TicketComment

Represents a message in a ticket conversation.

Important fields:

- id
- organizationId
- ticketId
- authorUserId
- body
- visibility
- createdAt

Visibility:

```text
PUBLIC
INTERNAL
```

### SLA Policy

Defines deadlines based on ticket priority.

Important fields:

- id
- organizationId
- priority
- responseTimeMinutes
- resolutionTimeMinutes
- active

### AuditLog

Represents immutable records of important business actions.

Important fields:

- id
- organizationId
- actorUserId
- entityType
- entityId
- action
- beforeValue
- afterValue
- createdAt

Audited events should include:

- Ticket status changed
- Ticket assigned
- Ticket priority changed
- Ticket category changed
- Ticket reopened
- Ticket closed

### AI Classification

Represents an AI suggestion for a ticket.

Important fields:

- id
- organizationId
- ticketId
- provider
- model
- suggestedPriority
- suggestedCategory
- confidence
- explanation
- createdAt
- approvedByUserId
- approvedAt

## Package structure proposal

```text
com.resolvehub
  config
  common
    exception
    security
    web
  auth
    controller
    dto
    service
  organization
    domain
    repository
    service
  user
    domain
    repository
    service
  ticket
    domain
    repository
    service
    controller
    dto
  sla
    domain
    repository
    service
    scheduler
  audit
    domain
    repository
    service
  ai
    domain
    dto
    provider
    service
```

## Security model

All API requests except authentication endpoints should require a valid JWT.

Authorization is based on:

- authenticated user id
- organization id
- role
- resource ownership or assignment

Rules:

- Customers can access only their own tickets.
- Agents can access assigned or unassigned tickets in their organization.
- Managers can access all tickets in their organization.
- Admins can manage organization-level settings and users.
- Cross-organization access is forbidden.

## Multi-tenancy model

The MVP uses shared-database multi-tenancy with an `organization_id` column on tenant-owned tables.

Every tenant-owned query must include organization scoping.

No controller should accept organization id blindly as the source of truth. The backend should derive the current user's organization from the authenticated principal.

## Ticket workflow rules

Allowed status transitions:

```text
OPEN -> IN_PROGRESS
OPEN -> WAITING_CUSTOMER
OPEN -> RESOLVED

IN_PROGRESS -> WAITING_CUSTOMER
IN_PROGRESS -> RESOLVED

WAITING_CUSTOMER -> IN_PROGRESS
WAITING_CUSTOMER -> RESOLVED

RESOLVED -> CLOSED
RESOLVED -> IN_PROGRESS

CLOSED -> IN_PROGRESS, manager/admin only
```

Closed tickets cannot receive new comments unless reopened.

## SLA calculation

SLA deadlines are calculated when a ticket is created or when priority changes.

Initial default resolution targets:

| Priority | Resolution target |
|---|---:|
| LOW | 72 hours |
| MEDIUM | 48 hours |
| HIGH | 24 hours |
| URGENT | 4 hours |

The scheduler marks or reports overdue tickets without silently mutating critical fields unless the action is auditable.

## AI role in the system

AI assists with:

- Suggested category
- Suggested priority
- Short explanation of suggestion
- Optional response draft in later versions

AI does not:

- Automatically close tickets
- Override human decisions
- Bypass authorization
- Become required for ticket creation

If the AI provider fails, ticket creation must still succeed.

## API design principles

- Use RESTful endpoints.
- Use DTOs for request and response objects.
- Do not expose entities directly.
- Use pagination for list endpoints.
- Use validation annotations for request payloads.
- Return consistent error responses.
- Document endpoints with OpenAPI.

## Deployment direction

The MVP should run locally through Docker Compose and be deployable later to a generic VPS or container platform.

The application should keep secrets in environment variables, not in source control.
