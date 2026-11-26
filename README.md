# TheSystem — Backend

Java 17, Spring Boot 3.2.0, Maven, Spring Security/JWT, Spring Data JPA/Hibernate, PostgreSQL 16, Flyway, JUnit 5 and Mockito. Frontend uses React 19, TypeScript, Vite 8, Axios and Oxlint.

## MVP v0.1.0

TheSystem is an RPG-inspired web application for personal goals and quests. The MVP includes registration and login (JWT), Hunter Profile, player EXP and levels, stat XP progression, Daily and Hidden Quests, customizable daily targets (NOW or TOMORROW), avatar upload/removal and EN/RU localization. The Personal Quests REST API exists; a complete personal-quest UI is not part of the confirmed MVP.

## Local development (macOS)

Install Git, JDK 17, Maven, Node.js 22 (use a current compatible 22.x patch, at least 22.12), npm and a Docker engine with Docker Compose v2 (e.g. Docker Desktop compatible with your macOS version). Check `java -version`, `mvn -version`, `node -v`, `npm -v`, and `docker compose version`. Start Docker before running Compose. These instructions use two terminals and a POSIX-compatible shell such as zsh.

Clone the repositories into the same parent directory:

```sh
git clone --branch dev https://github.com/ytenen/TheSystemBackend.git
git clone --branch dev https://github.com/ytenen/TheSystemFrontend.git
```

### Terminal 1 — database and backend

```sh
cd TheSystemBackend
export POSTGRES_PASSWORD="$(openssl rand -hex 32)"
export DB_PASSWORD="$POSTGRES_PASSWORD"
export JWT_SECRET="$(openssl rand -hex 32)"
docker compose up -d
docker compose ps
```

Wait until PostgreSQL is healthy before starting Spring Boot:

```sh
mvn spring-boot:run
```

Backend: http://localhost:8080. PostgreSQL 16: 127.0.0.1:5433 (container port 5432), database and user both `thesystem`. Flyway creates/migrates the schema; Hibernate validates it. The named volume `postgres_data` persists data. Keep the same database password across restarts: changing POSTGRES_PASSWORD does not change the password of an already initialized database. The generated values above are for a fresh local instance only; store your own values securely outside Git and reuse them on subsequent launches. Changing JWT_SECRET invalidates existing tokens. Never commit passwords, secrets or real tokens.

| Variable | Purpose |
| --- | --- |
| POSTGRES_PASSWORD | PostgreSQL container initialization password |
| DB_PASSWORD | Spring datasource password; must match the actual database user password |
| JWT_SECRET | HS256 signing secret, at least 32 bytes |
| VITE_API_BASE_URL | Public frontend API origin (not a secret); release integration pending in the frontend stability work |

### Terminal 2 — frontend

From the common parent directory:

```sh
cd TheSystemFrontend
npm ci
VITE_API_BASE_URL=http://localhost:8080 npm run dev -- --host localhost --port 5173 --strictPort
```

Open http://localhost:5173. The audited frontend currently hardcodes this backend URL; the environment-variable support is being added separately and must be verified before release. Restart Vite after changing environment variables. Vite variables are public client-side configuration. The audited backend CORS configuration allows http://localhost:5173; verify the final security configuration before using another origin.

### Tests and builds

Backend (from its directory, with the database healthy and the same exported variables): `mvn -B clean verify`. Prefer a separate test database/instance for integration testing; CI uses `thesystem_test` via SPRING_DATASOURCE_URL and SPRING_DATASOURCE_USERNAME. Do not run integration tests against valuable data.

Frontend (from its directory):

```sh
npm ci
npm run build
npm run lint
npm test
```

## REST API overview

Base URL: http://localhost:8080. Use `Authorization: Bearer <token>` for authenticated requests; authentication endpoints are public. EN/RU locale selection uses Accept-Language. The final authorization rules must be checked after the security PR.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | /api/auth/register | Register |
| POST | /api/auth/login | Log in |
| GET | /api/player | Current player |
| GET | /api/player/{id} | Player by ID |
| POST | /api/player/exp?amount=... | EXP endpoint |
| POST | /api/player/stat?stat=...&amount=... | Stat endpoint |
| GET | /api/quests/daily | Daily quest |
| POST | /api/quests/daily/progress | Record progress |
| PUT | /api/quests/daily/settings | Configure daily targets |
| POST, GET | /api/quests/personal | Create/list personal quests |
| GET | /api/quests/personal/{id} | Get personal quest |
| POST | /api/quests/personal/{id}/progress | Record personal progress |
| PUT, DELETE | /api/player/avatar | Upload (multipart field file)/remove avatar |
| GET | /api/player/{id}/avatar | Retrieve avatar |

The EXP/stat mutation endpoints are present in the audited code; access and availability in the release are subject to the separate security work.

Daily settings example: `{"targets":{"pushups":20,"squats":30,"plank":60},"mode":"TOMORROW"}`. NOW applies immediately and clears current progress; TOMORROW defers the change. Hidden rewards are tied to the daily quest.

## Repository layout

- Backend: `src/main/java/org/smorodin/core` (models/services), `infrastructure` (auth, avatar, persistence, web, i18n), `config`; `src/main/resources/db/migration` (Flyway); `src/test`; `docker-compose.yml`; `pom.xml`.
- Frontend: `src/api`, `src/pages`, `src/components`, `src/types`, `src/i18n`, `src/utils`, `tests`, `vite.config.ts`.
- CI: `.github/workflows/backend-tests.yml` and `.github/workflows/frontend-ci.yml` in their respective repositories.

## Git and release workflow

Develop on feature branches from dev, open PRs into dev, review and require green CI. Release via dev → main PRs after owner approval; prefer merge commits to preserve history. Never force push or delete main. Frontend main must first be bootstrapped from a green commit containing the main-enabled CI workflow, before final release changes. Tag only verified main commits with v0.1.0 after explicit approval.

## Known limitations

Local development is the documented deployment target, not a hardened production deployment. Security and frontend stability work must be integrated and retested before release. Personal Quests API has no confirmed complete UI. Quest Engine 2.0, story chains and AI-generated quests are future work. No mobile client is included. Local commands must be validated on the final release candidate.

## Repositories

- [Backend](https://github.com/ytenen/TheSystemBackend)
- [Frontend](https://github.com/ytenen/TheSystemFrontend)
