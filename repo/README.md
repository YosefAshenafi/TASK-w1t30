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

## Default credentials

| Username | Password | Role |
|----------|----------|------|
| `admin` | `Admin@123!` | Administrator |
| `student1` | `Test@123!` | Student |
| `student2` | `Test@123!` | Student |
| `mentor1` | `Test@123!` | Corporate Mentor |
| `faculty1` | `Test@123!` | Faculty Mentor |

> Change the admin password after first login in a production environment.

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

## Encryption

Sensitive database columns (e.g. `audit_events.details`) are encrypted at rest using **AES-256-GCM** via `AesAttributeConverter`. Each value is encrypted with a fresh random 12-byte IV prepended to the ciphertext; the entire package is base64-encoded.

### Key configuration

The AES key is a base64-encoded 32-byte value set via `AES_KEY_BASE64`:

```bash
# Generate a new key
openssl rand -base64 32
```

Set it in `.env` (local) or your secrets manager (production). Never commit a real key to source control.

### Key rotation

1. Generate a new 32-byte key.
2. Write a one-time migration script that reads each encrypted row, decrypts with the old key, re-encrypts with the new key, and writes back.
3. Deploy the migration before updating `AES_KEY_BASE64` in the environment.
4. Update `AES_KEY_BASE64` in all environments and restart the server.

There is no automatic re-encryption on startup — rotation is intentionally manual to avoid data loss on misconfiguration.

## Running Tests

Run all suites at once via the convenience script at the repo root:

```bash
./run_tests.sh          # server unit + API integration + web unit tests
./run_tests.sh --e2e    # also run Playwright E2E (requires running services)
```

Or run individual suites:

| Suite | Command |
|-------|---------|
| Server unit tests | `cd server && ./mvnw test -Dtest="PasswordPolicyTest,LockoutPolicyTest,JwtServiceTest,IdempotencyServiceTest,SyncResolverTest"` |
| API integration tests | `cd server && ./mvnw test -Dtest="AuthApiTest,SyncApiTest"` |
| Web unit tests | `cd web && ./node_modules/.bin/ng test --watch=false --browsers=ChromeHeadlessCI` |
| E2E tests | `cd e2e_tests && npx playwright test` |

> E2E tests require the full stack running (`docker compose up --build -d`) before executing.

## Production build

The server `Dockerfile` has a slim `runtime` target (JRE only, no Maven):

```bash
docker build --target runtime -t meridian-server ./server
```
