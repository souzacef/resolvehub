# Deployment Notes

## Goal

The MVP should be simple to run locally and straightforward to deploy later.

Initial deployment focus:

- Docker Compose
- Environment variables
- PostgreSQL container
- Backend container
- Frontend container

## Local services

Start only the database and Ollama:

```bash
docker compose up -d db ollama
```

Start the full app once backend and frontend Dockerfiles exist:

```bash
docker compose --profile app up --build
```

## Environment variables

Backend variables:

```text
SPRING_PROFILES_ACTIVE
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
RESOLVEHUB_AI_PROVIDER
RESOLVEHUB_AI_OPENAI_COMPATIBLE_BASE_URL
RESOLVEHUB_AI_OPENAI_COMPATIBLE_API_KEY
RESOLVEHUB_AI_OPENAI_COMPATIBLE_MODEL
RESOLVEHUB_AI_OPENAI_COMPATIBLE_TIMEOUT_SECONDS
RESOLVEHUB_SEED_DEMO_ENABLED
RESOLVEHUB_SECURITY_CORS_ALLOWED_ORIGINS
JWT_SECRET
```

Frontend variables:

```text
VITE_API_BASE_URL
```

Local CORS defaults allow:

- `http://localhost:5173`
- `http://127.0.0.1:5173`

Override if needed:

```text
RESOLVEHUB_SECURITY_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

## Production notes

Do not use the default local database password in production.

Do not commit secrets.

Use managed PostgreSQL or a properly backed-up database volume for real deployment.

Run migrations automatically only if the deployment process is designed for it.

## Dev Demo Accounts

When running with `SPRING_PROFILES_ACTIVE=dev` (or `RESOLVEHUB_SEED_DEMO_ENABLED=true`), ResolveHub seeds idempotent demo data:

- `admin@resolvehub.dev` / `Password123!`
- `manager@resolvehub.dev` / `Password123!`
- `agent@resolvehub.dev` / `Password123!`
- `customer@resolvehub.dev` / `Password123!`

The seed creates one demo organization, sample tickets across statuses/priorities, comments, and assigned work so Swagger can be explored immediately.

If your local database was created before the latest demo-seed consistency fixes and demo users/tickets look split across organizations, reset your local dev database volume and restart the backend so a fresh coherent demo dataset is created.
