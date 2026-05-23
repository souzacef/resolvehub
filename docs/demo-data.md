# Demo Data Setup

Use this guide to prepare a useful hosted demo environment in Render.

## Why This Is Needed

ResolveHub local development and the hosted Render deployment use separate PostgreSQL databases.

- Local development uses the dev profile and seeded demo data.
- Render uses its own PostgreSQL instance.
- Data created locally does not appear in Render.
- If you want a polished hosted demo, you need to create demo users and tickets in the Render app.

## Initial Render State

When you register a new organization in the hosted Render app, that organization starts with one `ADMIN` user: the account used during registration.

That first `ADMIN` user can then create additional users inside the same organization.

## User Management Rules

Within an organization:

- `ADMIN` can create `CUSTOMER`, `AGENT`, `MANAGER`, and `ADMIN` users.
- `MANAGER` can create `CUSTOMER` and `AGENT` users.
- `CUSTOMER` cannot create users.
- `AGENT` cannot create users.

## Recommended Demo Organization

Create this organization in Render for a clean, portfolio-ready demo:

- `Acme Support Demo`

## Recommended Demo Users

Use these accounts for a consistent hosted demo setup:

- `admin@resolvehub.demo` / `ADMIN`
- `manager@resolvehub.demo` / `MANAGER`
- `agent@resolvehub.demo` / `AGENT`
- `customer@resolvehub.demo` / `CUSTOMER`
- `customer2@resolvehub.demo` / `CUSTOMER`

Use `Password123!` only for local development and controlled demo environments. Do not use it for real production accounts.

## Suggested Setup Order

1. Register `Acme Support Demo` in the hosted Render app using `admin@resolvehub.demo`.
2. Sign in as that initial `ADMIN` user.
3. Open `Organization Users`.
4. Create `manager@resolvehub.demo`, `agent@resolvehub.demo`, `customer@resolvehub.demo`, and `customer2@resolvehub.demo`.
5. Sign out and sign back in with the customer accounts to create sample tickets.
6. Sign back in as staff to assign, classify, comment on, and progress tickets.

## Suggested Demo Tickets

Create these tickets with the customer accounts so the hosted demo has a realistic mix of operational work.

### 1. Production login outage

Suggested values:

- Requester: `customer@resolvehub.demo`
- Priority: `URGENT`
- Category: `TECHNICAL`
- Status: `IN_PROGRESS`
- Assignee: `agent@resolvehub.demo`

Suggested description:

```text
Several employees cannot log in to the production portal after this morning's deployment. Users report invalid session and timeout messages even after resetting their passwords.
```

Suggested comments:

- Customer comment: `This is blocking our support team from handling live customer requests.`
- Staff public comment: `We have reproduced the issue and are investigating authentication failures now.`
- Staff internal comment: `Possible regression after auth gateway rollout. Check token validation and session cache.`

### 2. Duplicate charge on monthly invoice

Suggested values:

- Requester: `customer2@resolvehub.demo`
- Priority: `HIGH`
- Category: `BILLING`
- Status: `WAITING_CUSTOMER`
- Assignee: `manager@resolvehub.demo`

Suggested description:

```text
Our finance team noticed two identical charges for this month's subscription renewal. Please confirm which charge is valid and how the duplicate will be handled.
```

Suggested comments:

- Customer comment: `I can provide the invoice PDF and bank statement if needed.`
- Staff public comment: `We found the duplicate transaction and need the invoice number to complete the refund review.`
- Staff internal comment: `Refund likely required. Waiting on customer to confirm invoice reference and payment timestamp.`

### 3. Unable to update profile email

Suggested values:

- Requester: `customer@resolvehub.demo`
- Priority: `MEDIUM`
- Category: `ACCOUNT`
- Status: `OPEN`
- Assignee: Unassigned initially, then assign to `agent@resolvehub.demo` during the demo

Suggested description:

```text
I can update my profile name and phone number, but changing the account email address fails with a generic error message after saving.
```

Suggested comments:

- Customer comment: `This happens in Chrome and Firefox.`
- Staff public comment: `Thanks, we have the report and will test the profile update flow.`
- Staff internal comment: `Good candidate to demonstrate assignment from unassigned queue.`

### 4. CSV export for ticket list

Suggested values:

- Requester: `customer2@resolvehub.demo`
- Priority: `LOW`
- Category: `FEATURE_REQUEST`
- Status: `RESOLVED`
- Assignee: `manager@resolvehub.demo`

Suggested description:

```text
It would help our operations team if the ticket list could be exported as CSV for weekly reporting and trend analysis.
```

Suggested comments:

- Customer comment: `A CSV export would save us manual copy/paste work every Friday.`
- Staff public comment: `We logged this feature request and marked it resolved for demo purposes after documenting the requirement.`
- Staff internal comment: `Useful example of non-incident work in the queue.`

### 5. Suspicious account activity alert

Suggested values:

- Requester: `customer@resolvehub.demo`
- Priority: `HIGH`
- Category: `SECURITY`
- Status: `OPEN`
- Assignee: `manager@resolvehub.demo`

Suggested description:

```text
We received an alert about suspicious sign-in attempts from an unfamiliar location. Please confirm whether additional account protection steps are required.
```

Suggested comments:

- Customer comment: `The alert referenced logins from a region where our team does not operate.`
- Staff public comment: `We are reviewing the activity and recommend rotating credentials as a precaution.`
- Staff internal comment: `Strong ticket to showcase security categorization and audit visibility.`

## Demo Walkthrough

Use this short flow when presenting the hosted app:

1. Register organization
   Create `Acme Support Demo` in the hosted Render app. This creates the first `ADMIN` account for that organization.
2. Create users
   Sign in as the initial admin and create the recommended `MANAGER`, `AGENT`, and `CUSTOMER` users from `Organization Users`.
3. Create tickets as customer
   Sign in as `customer@resolvehub.demo` or `customer2@resolvehub.demo` and create the suggested tickets.
4. Manage tickets as staff
   Sign in as `ADMIN`, `MANAGER`, or `AGENT` to assign tickets, add comments, move statuses, and review dashboard metrics.
5. Use AI classification
   Open a ticket detail page and request an AI category/priority suggestion.
6. Apply AI suggestion
   Review the suggestion and apply it manually to demonstrate that AI guidance is advisory, not automatic.
7. View audit log
   Open the audit log on the ticket detail page to show the append-only record of ticket lifecycle changes.

## Recommended Demo Outcome

After setup, the hosted Render app should clearly demonstrate:

- Multi-user organization onboarding
- Role-aware navigation and permissions
- Customer ticket submission
- Staff assignment and workflow handling
- AI-assisted classification
- Auditability through ticket history and logs

## Related Docs

- [README.md](../README.md)
- [docs/deployment.md](deployment.md)
- [docs/architecture.md](architecture.md)
