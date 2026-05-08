# AI Provider Design

## Goal

ResolveHub uses provider-agnostic AI classification so ticket workflows stay stable while AI providers can be swapped by configuration.

AI classification is explicit and on-demand through:

- `POST /api/tickets/{ticketId}/ai/classification`

Ticket creation does not call AI and must continue working even if an AI provider is unavailable.

## Current Abstraction

### `TicketAiClassifier`

Provider interface used by the ticket service.

Input:

- ticket title
- ticket description

Output:

- `TicketClassificationSuggestion`
  - `suggestedCategory`
  - `suggestedPriority`
  - `reasoning`

### Implementations

1. `FakeTicketAiClassifier`
- deterministic keyword rules
- no network calls
- default in dev/test unless provider is overridden

2. `OpenAiCompatibleTicketAiClassifier`
- HTTP-based implementation for OpenAI-compatible APIs
- calls `POST {baseUrl}/chat/completions`
- prompts for strict JSON with `category`, `priority`, `reasoning`
- validates category/priority against ResolveHub enums
- returns controlled `502 Bad Gateway` style error when provider response is invalid or request fails

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

Environment variable equivalents used by `application.yml`:

- `RESOLVEHUB_AI_PROVIDER`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_API_KEY`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL`
- `RESOLVEHUB_AI_OPENAI_COMPATIBLE_TIMEOUT_SECONDS`

## Local Ollama Example

1. Start Ollama:

```bash
ollama serve
```

2. Pull the model:

```bash
ollama pull llama3.1:8b
```

3. Run backend using OpenAI-compatible provider:

```bash
export RESOLVEHUB_AI_PROVIDER=openai-compatible
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL=http://127.0.0.1:11434/v1
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_API_KEY=ollama
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL=llama3.1:8b
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_TIMEOUT_SECONDS=20
```

## Behavior Guarantees

- AI suggestions do not automatically update ticket category/priority in v1.0.0.
- AI endpoint failures are isolated to the AI endpoint response.
- Core ticket flows remain unaffected.

## Future Direction

- Add richer prompt/versioning strategy.
- Add response confidence metadata if needed.
- Add additional OpenAI-compatible and hosted providers later without changing ticket business logic.
