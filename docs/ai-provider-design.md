# AI Provider Design

## Purpose

ResolveHub uses a provider-agnostic AI classification layer so AI capabilities can evolve without changing ticket business logic.

AI classification is on-demand through:

- `POST /api/tickets/{ticketId}/ai/classification`
- `PATCH /api/tickets/{ticketId}/classification` (manual apply by staff)

Ticket creation does not call AI.

## Current Contract

### Interface

`TicketAiClassifier` classifies a ticket and returns a `TicketClassificationSuggestion` with:

- `suggestedCategory`
- `suggestedPriority`
- `reasoning`

### Behavior Guarantees

- Suggestions are advisory only.
- Suggestions do not automatically update ticket category or priority.
- Staff may apply suggested category/priority explicitly through the classification PATCH endpoint.
- AI provider failures affect only the AI endpoint response.
- Core ticket flows (create/list/detail/comment/status/assignment) do not depend on AI availability.

## Implementations

### 1) `FakeTicketAiClassifier` (default)

- Deterministic rule-based implementation.
- No network calls.
- Used by default when `resolvehub.ai.provider` is missing or set to `fake`.
- Keeps local development and tests stable.
- Used intentionally in the hosted Render demo for deterministic, cost-safe behavior and resilience against external provider downtime.

### 2) `OpenAiCompatibleTicketAiClassifier`

- HTTP implementation for OpenAI-compatible APIs.
- Calls `POST {baseUrl}/chat/completions`.
- Sends ticket title/description with instructions to return strict JSON:
  - `category`
  - `priority`
  - `reasoning`
- Parses and validates response values against ResolveHub enums.
- Returns controlled provider errors for invalid responses or upstream failures.

## Configuration

```yaml
resolvehub:
  ai:
    provider: fake # fake | openai-compatible
    openai-compatible:
      base-url: http://127.0.0.1:11434/v1
      api-key: ollama
      model: llama3.1:8b
      timeout-seconds: 20
```

Environment variables:

- `RESOLVEHUB_AI_PROVIDER`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_API_KEY`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_TIMEOUT_SECONDS`

## Local Ollama Setup (OpenAI-compatible)

1. Start Ollama:

```bash
ollama serve
```

2. Pull model:

```bash
ollama pull llama3.1:8b
```

3. Run backend with OpenAI-compatible provider:

```bash
export RESOLVEHUB_AI_PROVIDER=openai-compatible
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL=http://127.0.0.1:11434/v1
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_API_KEY=ollama
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL=llama3.1:8b
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_TIMEOUT_SECONDS=20
```

If these variables are not set, backend defaults to `fake` provider.

For hosted environments such as Render, a local URL like `http://127.0.0.1:11434` points to that service container itself, not your development machine. Use a provider URL reachable from the hosted runtime.

## Why This Design

- Keeps domain logic isolated from provider details.
- Enables deterministic tests without real model calls.
- Allows future provider additions with minimal service/controller changes.

## Planned Enhancements

- Persist suggestion history/versioning.
- Extend prompt controls and provider observability.
