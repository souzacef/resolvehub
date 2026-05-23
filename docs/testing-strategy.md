# Testing Strategy

## Goal

ResolveHub should demonstrate that business rules are not just described in documentation. They should be protected by automated tests.

The test suite should prioritize correctness in authentication, authorization, multi-tenancy, ticket workflows, SLA rules, audit logging, and AI failure handling.

## Test pyramid

```text
Many unit tests
Some integration tests
A few end-to-end/manual workflow checks
```

## Backend unit tests

Unit tests should cover isolated business logic.

Target areas:

- Ticket status transition validation
- SLA deadline calculation
- Role permission decisions
- Audit log event creation
- AI classification fallback behavior
- Request validation helpers
- DTO mapping where logic exists

Examples:

```text
Ticket cannot move from CLOSED to RESOLVED.
Customer can only use customer-approved status transitions.
URGENT ticket receives shorter SLA deadline than HIGH ticket.
Ticket creation succeeds when AI provider throws exception.
```

## Backend integration tests

Integration tests should use Spring Boot test support and Testcontainers for PostgreSQL.

Target areas:

- Authentication endpoints
- Protected endpoint access
- Organization scoping
- Ticket persistence
- Comment persistence
- Audit log persistence
- Flyway migrations
- Repository queries

Examples:

```text
A customer from organization A cannot read tickets from organization B.
An agent can list assigned tickets in their organization.
Status change creates an audit log entry.
Creating a ticket stores an AI suggestion when fake AI provider succeeds.
```

## Frontend tests

Frontend tests should start small and practical.

Target areas:

- Login form validation
- Ticket list rendering
- Ticket detail rendering
- Role-aware action visibility
- API error display

Suggested tools:

- Vitest
- React Testing Library

## API tests

API tests can be documented with HTTP examples and later automated with Postman, Thunder Client, or REST Assured.

Important API scenarios:

- Register user
- Login
- Create ticket
- List tickets
- Assign ticket
- Add comment
- Change status
- Approve AI suggestion
- View audit log

## Security tests

Security tests should verify:

- Missing token returns 401.
- Invalid token returns 401.
- Valid token with wrong role returns 403.
- Cross-organization access returns 403 or 404.
- Users cannot set their own organization by request body.
- Customers cannot access other customers' tickets.

## Multi-tenancy tests

Every tenant-owned operation should be tested with at least two organizations.

Required pattern:

```text
Given organization A and organization B
And users in both organizations
When user A requests resource from organization B
Then access is denied
```

## SLA tests

SLA tests should verify:

- Default SLA deadlines by priority
- Recalculation when priority changes
- Overdue detection
- Resolved or closed tickets are not treated as active overdue work

## Audit tests

Audit tests should verify that important events are recorded:

- Status changed
- Ticket assigned
- Priority changed
- Ticket reopened
- AI suggestion approved

Audit records should be append-only in normal application flows.

## AI tests

The AI layer should have deterministic tests.

Required cases:

- Fake provider returns a valid suggestion.
- Provider throws an exception.
- Provider returns invalid category.
- Provider returns invalid priority.
- Provider returns malformed JSON.
- Ticket creation succeeds despite AI failure.

## Definition of done for backend features

A backend feature is done when:

- Business rules are implemented.
- Authorization is enforced.
- Tenant scoping is enforced.
- Unit tests cover core logic.
- Integration tests cover persistence or API behavior where relevant.
- Swagger/OpenAPI reflects the endpoint.
- Errors are handled consistently.
- Documentation is updated if behavior changed.

## Definition of done for frontend features

A frontend feature is done when:

- It works with the backend API.
- Loading and error states are handled.
- Role-specific actions are visible only when appropriate.
- Basic tests exist for important rendering or validation behavior.
- The UI remains simple and readable.

## CI expectations

GitHub Actions CI should run:

- Backend build and tests
- Frontend install/build/tests
- Backend Docker image build validation
- Frontend Docker image build validation

CI should remain strict enough to catch regressions in authorization, multi-tenancy, and workflow behavior.
