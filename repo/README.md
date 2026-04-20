# Meridian Training Analytics Management System

On-premise training platform: Angular SPA + Spring Boot REST API + PostgreSQL.

## Quickstart

```bash
# 1. Clone and enter repo
cd repo

# 2. Copy env template (edit values as needed)
cp .env.example .env

# 3. Generate TLS cert (requires openssl)
chmod +x scripts/gen-local-cert.sh && ./scripts/gen-local-cert.sh

# 4. Build and start all services
docker compose up --build -d
```

After `docker compose up` completes (2–5 min for first build):

| URL | Service |
|-----|---------|
| https://localhost/ | Angular SPA |
| https://localhost/api/v1/health | Health check |

## Common commands

| Command | Description |
|---------|-------------|
| `docker compose up --build -d` | Build images and start containers |
| `docker compose down` | Stop containers |
| `docker compose logs -f` | Tail all logs |
| `docker compose exec postgres psql -U meridian meridian` | Open psql on the meridian DB |
| `docker compose exec server ./mvnw flyway:migrate` | Run Flyway migrations |

## Architecture

```
nginx (HTTPS :443)
 ├── /api/*  → server (Spring Boot :8080)
 └── /*      → web (Angular via nginx :80)
```

All services run inside Docker; no external dependencies required at runtime.

## Environment variables

See `.env.example` for the full list. Key variables:

- `JWT_SIGNING_KEY` — ≥32-char HMAC-SHA256 signing key
- `AES_KEY_BASE64` — base64-encoded 32-byte AES key (`openssl rand -base64 32`)
- `BACKUP_PATH` / `EXPORT_PATH` — local filesystem paths inside the server container

## Production build

The server `Dockerfile` has a slim `runtime` target (JRE only, no Maven):

```bash
docker build --target runtime -t meridian-server ./server
```
