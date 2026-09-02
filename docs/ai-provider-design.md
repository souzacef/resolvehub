# AI Provider Design

## Purpose

ResolveHub uses a provider-agnostic AI classification layer so AI capabilities can evolve without changing ticket business logic.

AI classification is on-demand through:

- `POST /api/tickets/{ticketId}/ai/classification`
- `PATCH /api/tickets/{ticketId}/classification` (manual apply by staff)

Ticket creation does not call AI.

## Current Contract

`TicketAiClassifier` classifies a ticket and returns a `TicketClassificationSuggestion` with:

- `suggestedCategory`
- `suggestedPriority`
- `reasoning`

### Behavior Guarantees

- Suggestions are advisory only.
- Suggestions do not automatically update ticket category or priority.
- Only eligible staff users can request or apply AI classification.
- Applying a suggestion uses the normal classification update path.
- Applied category/priority changes are written to the ticket audit log.
- AI provider failures affect only the AI classification request.
- Core ticket flows (create/list/detail/comment/status/assignment) do not depend on external AI availability.

This keeps the workflow human-in-the-loop: the model recommends, a staff user decides.

## Implementations

### 1) `FakeTicketAiClassifier`

- Deterministic rule-based implementation.
- No network calls.
- Used when `resolvehub.ai.provider` is missing or set to `fake`.
- Useful for local development, automated tests, and deterministic demos.
- Uses ordered keyword rules and a general fallback when no rule matches.

The fake provider is no longer the AI provider used by the hosted Render portfolio deployment.

### 2) `OpenAiCompatibleTicketAiClassifier`

- HTTP implementation for OpenAI-compatible chat-completions APIs.
- Calls `POST {baseUrl}/chat/completions`.
- Uses Bearer authentication when a provider API key is configured.
- Sends ticket title/description plus instructions to return strict JSON:
  - `category`
  - `priority`
  - `reasoning`
- Parses the assistant response and validates category/priority against ResolveHub enums.
- Converts invalid/upstream provider responses into controlled gateway errors.

This adapter is used both for local OpenAI-compatible providers such as Ollama and for Gemini's OpenAI-compatible endpoint in the hosted environment.

## Hosted Production Provider

The current Render deployment uses:

```text
Provider mode: openai-compatible
Base URL: https://generativelanguage.googleapis.com/v1beta/openai
Model: gemini-3.5-flash
Timeout: 30 seconds
```

The Gemini API key is stored only as a Render environment secret.

Production environment variables:

```text
RESOLVEHUB_AI_PROVIDER=openai-compatible
RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
RESOLVEHUB_AI_OPENAI_COMPATIBLE_API_KEY=<secret>
RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL=gemini-3.5-flash
RESOLVEHUB_AI_OPENAI_COMPATIBLE_TIMEOUT_SECONDS=30
```

No provider secret is required in frontend code because all AI requests flow through the authenticated ResolveHub backend.

## Verified Human-in-the-loop Flow

The hosted production flow has been smoke-tested end to end:

1. A ticket is created with a human-selected category and priority.
2. Staff request an AI suggestion from the ticket detail page.
3. Gemini receives the ticket text through the OpenAI-compatible adapter.
4. ResolveHub displays suggested category, priority, and reasoning without changing the ticket.
5. Staff explicitly choose `Apply suggestion`.
6. The backend updates category/priority through the normal classification endpoint.
7. The classification change is written to the append-only audit log.

A production smoke test deliberately created a ticket as `OTHER / LOW` while its description strongly implied suspicious access and data exfiltration. Gemini independently returned `SECURITY / URGENT` with security-specific reasoning, demonstrating that the hosted provider was reasoning from ticket content rather than echoing the existing classification.

## Base Configuration

Application properties remain provider-neutral:

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

If no provider is configured, ResolveHub defaults to `fake` mode.

## Local Ollama Setup

1. Start Ollama:

```bash
ollama serve
```

2. Pull a model:

```bash
ollama pull llama3.1:8b
```

3. Run the backend with the OpenAI-compatible provider:

```bash
export RESOLVEHUB_AI_PROVIDER=openai-compatible
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL=http://127.0.0.1:11434/v1
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_API_KEY=ollama
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL=llama3.1:8b
export RESOLVEHUB_AI_OPENAI_COMPATIBLE_TIMEOUT_SECONDS=20
```

A local URL such as `http://127.0.0.1:11434` is not valid for a Render-hosted provider because it points back to the Render container itself.

## Why This Design

- Keeps domain/business logic isolated from provider details.
- Enables deterministic automated tests without real model calls.
- Keeps external AI failure isolated from core support workflows.
- Allows local and hosted providers to share the same adapter.
- Makes provider replacement an environment/configuration decision instead of a ticket-domain rewrite.
- Preserves human control over model-suggested business changes.

## Planned Enhancements

- Persist AI suggestion history/versioning separately from applied classification changes.
- Add provider latency/error metrics and structured observability.
- Add richer prompt/version metadata for production troubleshooting.
