# AI Provider Design

## Goal

ResolveHub uses AI to assist support teams with ticket classification. The AI layer should be useful, replaceable, testable, and safe.

The first provider target is local Ollama using an OpenAI-compatible API. The design should allow adding OpenAI or another hosted provider later without rewriting ticket business logic.

## Design principles

- The domain should not depend directly on a specific AI vendor.
- Ticket creation must not fail just because AI classification fails.
- AI suggestions are advisory by default.
- Tests should not require a real model.
- Provider prompts should be versioned or centralized.
- Sensitive data handling should be explicit.

## Provider abstraction

Suggested interface:

```java
public interface AiClassificationProvider {
    TicketClassificationSuggestion classify(TicketClassificationRequest request);
}
```

Suggested request object:

```java
public record TicketClassificationRequest(
    String title,
    String description,
    List<String> allowedCategories,
    List<String> allowedPriorities
) {}
```

Suggested response object:

```java
public record TicketClassificationSuggestion(
    TicketCategory category,
    TicketPriority priority,
    BigDecimal confidence,
    String explanation,
    String provider,
    String model
) {}
```

## Providers

### FakeAiClassificationProvider

Used for tests and local deterministic development.

Behavior:

- Returns predictable category and priority.
- Does not call external services.
- Enables integration tests without requiring Ollama or OpenAI.

### OllamaAiClassificationProvider

Used for local AI-assisted classification.

Default configuration:

```text
RESOLVEHUB_AI_PROVIDER=ollama
RESOLVEHUB_AI_BASE_URL=http://localhost:11434/v1
RESOLVEHUB_AI_MODEL=llama3.1:8b
```

In Docker Compose, the backend should use:

```text
RESOLVEHUB_AI_BASE_URL=http://ollama:11434/v1
```

### OpenAiClassificationProvider

Future provider.

Expected configuration:

```text
RESOLVEHUB_AI_PROVIDER=openai
RESOLVEHUB_AI_BASE_URL=https://api.openai.com/v1
RESOLVEHUB_AI_MODEL=<model-name>
OPENAI_API_KEY=<secret>
```

This provider should not be required for the MVP.

## Prompt design

The classification prompt should instruct the model to return strict JSON only.

Input:

- Ticket title
- Ticket description
- Allowed categories
- Allowed priorities

Expected JSON:

```json
{
  "category": "TECHNICAL",
  "priority": "HIGH",
  "confidence": 0.82,
  "explanation": "The user reports a production login failure affecting access."
}
```

The backend must validate the returned category and priority against enum values. Invalid AI output should be handled gracefully.

## Failure handling

AI classification may fail because of:

- Provider unavailable
- Timeout
- Invalid JSON
- Unsupported model
- Empty response
- Provider rate limit
- Unexpected response format

The service should:

1. Log the failure.
2. Save the ticket normally.
3. Optionally record an AI classification failure event.
4. Return the ticket without AI classification.

Ticket creation must not depend on successful AI output.

## Human approval model

For v1.0.0:

- AI suggests category and priority.
- Human users can accept or ignore suggestions.
- Accepted suggestions should be auditable.

Future versions may allow organization-level automation settings, but automatic classification should be explicit and configurable.

## Security and privacy

The AI provider receives ticket title and description. That may include sensitive customer information.

Rules:

- Do not send passwords, tokens, or secrets to AI providers.
- Consider redaction before provider calls in later versions.
- Document which provider is active.
- Keep API keys out of Git.
- Keep AI requests scoped to the current ticket only.

## Testing strategy for AI

Unit tests:

- Fake provider returns deterministic results.
- AI service handles provider exceptions.
- AI service validates enum output.
- Ticket creation succeeds when AI fails.

Integration tests:

- Ticket creation stores AI suggestion when fake provider succeeds.
- Ticket creation still works when fake provider simulates failure.

Manual tests:

- Run Ollama locally.
- Pull a supported model.
- Create a ticket.
- Verify classification suggestion is stored.

Example Ollama setup:

```bash
ollama pull llama3.1:8b
ollama serve
```

Then configure backend:

```bash
export RESOLVEHUB_AI_PROVIDER=ollama
export RESOLVEHUB_AI_BASE_URL=http://localhost:11434/v1
export RESOLVEHUB_AI_MODEL=llama3.1:8b
```
