# AI Provider Design

## Goal

ResolveHub exposes AI-assisted ticket classification through a provider-agnostic abstraction so ticket workflows remain independent from any specific model vendor.

For v1.0.0, the backend uses a deterministic fake provider. Real Ollama/OpenAI-compatible providers will be added later without changing ticket business logic.

## Current Abstraction

### `TicketAiClassifier`

`TicketAiClassifier` is the provider interface used by the ticket layer:

- Input: a ticket (title and description context)
- Output: `TicketClassificationSuggestion`

### `TicketClassificationSuggestion`

The suggestion object returns:

- `suggestedCategory` (`TicketCategory`)
- `suggestedPriority` (`TicketPriority`)
- `reasoning` (short explanation)

### `FakeTicketAiClassifier`

`FakeTicketAiClassifier` is the default implementation in development and tests.

Behavior:

- deterministic keyword-based classification
- no network calls
- stable outputs for repeatable tests

Default provider selection:

- `resolvehub.ai.provider=fake`
- configurable via `RESOLVEHUB_AI_PROVIDER`

## API Contract

Endpoint:

- `POST /api/tickets/{ticketId}/ai/classification`

Behavior:

- Returns a suggestion payload only.
- Does not modify stored ticket category or priority.
- Keeps AI logic separate from ticket creation and lifecycle updates.

## Authorization Rules

- CUSTOMER cannot request AI classification.
- AGENT, MANAGER, and ADMIN can request classification for tickets in their organization.
- Cross-organization access is denied.

## Failure Isolation

- Ticket creation does not depend on AI classification.
- AI classification is explicitly requested by endpoint call.
- If a future real provider fails, it should not impact existing ticket creation or core ticket CRUD behavior.

## Future Providers

Planned next implementations:

- Ollama provider (local deployment)
- OpenAI-compatible provider (hosted API)

Both will implement `TicketAiClassifier` and be selected by configuration.
