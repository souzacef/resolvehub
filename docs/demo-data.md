# Demo Data Setup

Use this guide to prepare or restore the controlled ResolveHub portfolio demo.

## Current Hosted Demo Architecture

Local development and the hosted deployment use separate PostgreSQL databases:

- local development uses Docker PostgreSQL and the `dev` profile;
- hosted production uses persistent Neon PostgreSQL 16;
- Render hosts the frontend and backend services;
- data created locally does not appear in the hosted Neon database.

The production profile keeps demo seeding disabled by default.

## Canonical Demo Seeder

ResolveHub includes an idempotent `DevelopmentDemoDataSeeder`.

It runs automatically in the `dev` profile, or when this property is explicitly enabled:

```text
RESOLVEHUB_SEED_DEMO_ENABLED=true
```

The canonical seeded organization is:

- `ResolveHub Demo Org`

Canonical seeded users:

- `admin@resolvehub.dev` (`ADMIN`)
- `manager@resolvehub.dev` (`MANAGER`)
- `agent@resolvehub.dev` (`AGENT`)
- `customer@resolvehub.dev` (`CUSTOMER`)

Controlled demo password:

```text
Password123!
```

Use this password only for local development or a disposable public portfolio demo. Never reuse it for a real account.

## Canonical Seeded Tickets

The current seeder creates/maintains four representative tickets:

### Production login outage

- Status: `IN_PROGRESS`
- Priority: `URGENT`
- Category: `TECHNICAL`
- Requester: demo customer
- Assignee: demo agent

### Duplicate charge on monthly invoice

- Status: `WAITING_CUSTOMER`
- Priority: `HIGH`
- Category: `BILLING`
- Requester: demo customer
- Assignee: demo manager

### CSV export for ticket list

- Status: `RESOLVED`
- Priority: `LOW`
- Category: `FEATURE_REQUEST`
- Requester: demo customer
- Assignee: unassigned

### Unable to update profile email

- Status: `OPEN`
- Priority: `MEDIUM`
- Category: `ACCOUNT`
- Requester: demo customer
- Assignee: unassigned

The seeder also creates public/internal comments that make the ticket-detail and audit/workflow screens useful immediately.

## Restoring a Fresh Hosted Neon Database

Use this when the hosted database is empty or intentionally rebuilt.

1. Confirm Flyway has created/validated the schema.
2. In the Render backend environment, set:

   ```text
   RESOLVEHUB_SEED_DEMO_ENABLED=true
   ```

3. Deploy the backend.
4. Confirm the four seeded accounts can authenticate and the canonical tickets are visible.
5. Set the flag back to:

   ```text
   RESOLVEHUB_SEED_DEMO_ENABLED=false
   ```

6. Redeploy.
7. Confirm the already-created demo data remains present.

The seeder is idempotent, but production should normally keep the flag disabled after initialization so restarts do not continually restore canonical demo values.

## Why One-time Seeding Is Preferred in Production

Leaving the seeder off after initialization lets the hosted database retain changes made during demonstrations, such as:

- ticket assignment;
- status transitions;
- comments;
- AI-applied category/priority changes;
- audit-log history.

This also makes a backend redeploy a useful persistence smoke test: the data should remain in Neon when the application instance is replaced.

## Hosted AI Demo

The hosted deployment uses Gemini 3.5 Flash through ResolveHub's OpenAI-compatible provider.

AI classification remains advisory:

1. Staff open a ticket.
2. Staff request `Suggest classification with AI`.
3. ResolveHub displays suggested category, priority, and reasoning.
4. The ticket remains unchanged until staff choose `Apply suggestion`.
5. Applying the suggestion updates the ticket through the normal classification endpoint.
6. The category/priority change appears in the audit log.

## Recommended Real-AI Smoke Test

For a clear demonstration that the hosted model is reasoning from ticket content, create a new ticket with deliberately weak manual classification:

```text
Title: Strange activity in customer records
Category: OTHER
Priority: LOW

Description:
Three employee sessions originated from countries they have never visited, and several customer files were downloaded around 3 AM. Please investigate immediately and contain the situation.
```

Then request an AI classification.

In the verified hosted test, Gemini recommended:

- Category: `SECURITY`
- Priority: `URGENT`

with reasoning based on suspicious geographic access and likely data exfiltration.

This is a useful demo because the deterministic fake classifier does not receive the ticket's existing category/priority as instructions to echo them, and the human-selected `OTHER / LOW` values visibly differ from the semantic AI recommendation.

After reviewing the suggestion, apply it and open the audit log to demonstrate the full human-in-the-loop workflow.

## Suggested Portfolio Walkthrough

1. Open the public `Service status` page and wait for ResolveHub to become ready if Render is cold.
2. Sign in as `ADMIN` or `MANAGER`.
3. Show dashboard metrics and the seeded ticket queue.
4. Open an unassigned ticket and demonstrate assignment behavior.
5. Open a ticket detail page and show comments/SLA metadata.
6. Create the deliberate `OTHER / LOW` security test ticket.
7. Request the Gemini classification suggestion.
8. Review the recommendation without applying it immediately.
9. Apply the suggestion.
10. Show the resulting `TICKET_CLASSIFICATION_UPDATED` audit entry.

## Role Notes for the Demo

- `CUSTOMER`: own tickets only
- `AGENT`: unassigned tickets plus tickets assigned to self
- `MANAGER`: organization-wide ticket management and limited user creation
- `ADMIN`: organization-wide ticket/user management

This role split is useful to demonstrate that ResolveHub's permissions are enforced by backend rules, not only by hiding frontend controls.

## Related Docs

- [README.md](../README.md)
- [docs/deployment.md](deployment.md)
- [docs/architecture.md](architecture.md)
- [docs/ai-provider-design.md](ai-provider-design.md)
