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
RESOLVEHUB_AI_BASE_URL
RESOLVEHUB_AI_MODEL
JWT_SECRET
```

Frontend variables:

```text
VITE_API_BASE_URL
```

## Production notes

Do not use the default local database password in production.

Do not commit secrets.

Use managed PostgreSQL or a properly backed-up database volume for real deployment.

Run migrations automatically only if the deployment process is designed for it.
