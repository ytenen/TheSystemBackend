# TheSystem — Backend

Java 17, Spring Boot 3.2.0, Maven, Spring Security/JWT, Spring Data JPA/Hibernate, PostgreSQL 16, Flyway, JUnit 5 and Mockito. Frontend uses React 19, TypeScript, Vite 8, Axios and Oxlint.

## MVP v0.1.0 — first local release

TheSystem is an RPG-inspired web application for personal goals and quests. The MVP includes registration and login (JWT), Hunter Profile, player EXP and levels, stat XP progression, Daily and Hidden Quests, customizable daily targets (NOW or TOMORROW), avatar upload/cropping/removal and EN/RU localization. The Personal Quests REST API exists; a complete personal-quest UI is not part of the MVP.

This release targets a single computer: PostgreSQL in Docker, a locally running Java backend and a browser-based frontend. No public hosting, cloud deployment or production infrastructure is required or certified. The release acceptance path is the local development server; a configured static build and local preview are additional checks, not a hosted deployment.

## Run locally (macOS)

Install Git, JDK 17, Maven, Node.js 22 (use a current compatible 22.x patch, at least 22.12), npm and a Docker engine with Docker Compose v2 (e.g. Docker Desktop compatible with your macOS version). Check `java -version`, `mvn -version`, `node -v`, `npm -v`, and `docker compose version`. Start Docker before running Compose. These instructions use two terminals and a POSIX-compatible shell such as zsh.

After release tags have been published, clone both matching versions into the same parent directory:

```sh
git clone --branch v0.1.0 https://github.com/ytenen/TheSystemBackend.git
git clone --branch v0.1.0 https://github.com/ytenen/TheSystemFrontend.git
```

Before release, or for ongoing development, use `dev` instead (choose one set of clone commands):

```sh
git clone --branch dev https://github.com/ytenen/TheSystemBackend.git
git clone --branch dev https://github.com/ytenen/TheSystemFrontend.git
```

### Terminal 1 — database and backend

The following credential-generation commands are only for a fresh database. For an existing database, restore the original password and JWT secret instead of generating new ones.

```sh
cd TheSystemBackend
export POSTGRES_PASSWORD="$(openssl rand -hex 32)"
export DB_PASSWORD="$POSTGRES_PASSWORD"
export JWT_SECRET="$(openssl rand -hex 32)"
# Keep the backend accessible only on this computer.
export SERVER_ADDRESS=127.0.0.1
docker compose up -d
docker compose ps
```

Docker Compose starts PostgreSQL only, not either application. Wait until PostgreSQL is healthy before starting Spring Boot:

```sh
mvn spring-boot:run
```

Backend: http://localhost:8080. PostgreSQL 16: 127.0.0.1:5433 (container port 5432), database and user both `thesystem`. Flyway applies V1 (initial schema), V2 (daily settings), V3 (avatars) and V4 (stat XP); Hibernate validates the schema. The default and postgres profile use the same local datasource settings. Back up existing databases before migrations; do not delete the persistent volume. The named volume `postgres_data` persists data. Keep the same database password across restarts: changing POSTGRES_PASSWORD does not change the password of an already initialized database. The generated values above are for a fresh local instance only; store your own values securely outside Git and reuse them on subsequent launches. Changing JWT_SECRET invalidates existing tokens. Never commit passwords, secrets or real tokens. Spring Boot's `SERVER_ADDRESS=127.0.0.1` setting explicitly binds the backend to IPv4 loopback; this is not the default in the checked-in configuration. Do not expose the backend or Vite server to your LAN or the public internet for this release.

| Variable | Purpose |
| --- | --- |
| POSTGRES_PASSWORD | PostgreSQL container initialization password |
| DB_PASSWORD | Spring datasource password; must match the actual database user password |
| JWT_SECRET | Required HS256 signing secret, at least 32 UTF-8 bytes; startup rejects a shorter key; token lifetime is 24 hours |
| CORS_ALLOWED_ORIGINS | Comma-separated explicit browser origins; default `http://localhost:5173`; `*` is rejected |
| SECURITY_LOG_LEVEL | Spring Security log level, default `WARN` |
| WEB_LOG_LEVEL | Spring Web log level, default `WARN` |
| SERVER_ADDRESS | Optional Spring Boot bind address; use `127.0.0.1` for local-only operation |
| VITE_API_BASE_URL | Public frontend API base URL; set explicitly for a non-development build |

### Terminal 2 — frontend

From the common parent directory:

```sh
cd TheSystemFrontend
npm ci
VITE_API_BASE_URL=http://localhost:8080 npm run dev -- --host localhost --port 5173 --strictPort
```

Open http://localhost:5173. In development only, an absent or blank `VITE_API_BASE_URL` falls back to `http://localhost:8080`. To configure it persistently, copy `.env.example` to `.env.local` in the frontend root and edit the value; restart Vite after changes. Shell environment variables can also be used as above.

For a different browser origin, set `CORS_ALLOWED_ORIGINS` before starting Backend, for example `export CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:4173`. Use exact scheme, hostname and port, without a path or trailing slash. The origin is the browser frontend address, not the backend API address. Wildcard origins are rejected; CORS is not an authentication mechanism.

### Optional local build and preview

The normal local release uses `npm run dev`. To verify the compiled frontend as an additional local check, first allow the preview origin in the backend terminal and restart the backend:

```sh
export CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:4173
```

In the frontend directory:

```sh
npm ci
VITE_API_BASE_URL=http://localhost:8080 npm run build
npm run preview -- --host localhost --port 4173 --strictPort
```

Open http://localhost:4173. Vite's build mode disables the development-only localhost fallback; the URL must be explicit for a working bundle. The current application validates the URL when its API client initializes, not in `vite.config.ts`. A successful unconfigured build is therefore not proof that the app can start. The URL is embedded in `dist/`; rebuild after changes. `VITE_` values are public client configuration, never secrets. Non-loopback absolute URLs require HTTPS outside development; same-origin proxy paths are supported by the resolver, but reverse proxy setup and external hosting are outside this release. Preview is for local verification, not a public hosting server.

### Tests and builds

Backend: with PostgreSQL healthy and the same exported variables, create a separate local test database once (if it does not already exist):

```sh
docker compose exec postgres createdb -U thesystem thesystem_test
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/thesystem_test SPRING_DATASOURCE_USERNAME=thesystem mvn -B clean verify
```

CI uses a separate `thesystem_test` database. Never run integration tests against valuable data. `mvn -B clean verify` also packages the Backend; the executable JAR is `target/system-backend-0.1.0.jar`. Run it with `java -jar target/system-backend-0.1.0.jar` using the application database configuration, not the test database.

Frontend (from its directory):

```sh
npm ci
VITE_API_BASE_URL=http://localhost:8080 npm run build
npm run lint
npm test
```

### Stop and restart safely

Stop the frontend and backend with Ctrl+C in their respective terminals. From the backend directory, `docker compose stop` stops PostgreSQL without removing its data. On the next run, restore your existing environment variables (including the original database password and JWT key), run `docker compose up -d`, wait for a healthy database and restart both applications. Do not generate replacement credentials for an existing database or remove its named volume.

### Local release acceptance

Before tagging, check both matching release candidates on the same machine: clean setup, Flyway migrations, registration/login, Hunter Profile, Daily and Hidden rewards including duplicate-reward prevention, NOW/TOMORROW settings, stat XP, avatar upload/removal, EN/RU switching, logout/re-login and persistence after restart. Check the explicitly configured build and preview separately. CI verifies builds/tests, not the complete browser workflow.

## REST API overview

Base URL: http://localhost:8080. Use `Authorization: Bearer <token>` for authenticated requests. Only POST registration/login and GET avatar retrieval are public application endpoints; other listed endpoints require authentication. EN/RU locale selection uses `Accept-Language`.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | /api/auth/register | Register |
| POST | /api/auth/login | Log in |
| GET | /api/player | Current player |
| GET | /api/player/{id} | Player by ID |
| GET | /api/quests/daily | Daily quest |
| POST | /api/quests/daily/progress | Record progress |
| PUT | /api/quests/daily/settings | Configure daily targets |
| POST, GET | /api/quests/personal | Create/list personal quests |
| GET | /api/quests/personal/{id} | Get personal quest |
| POST | /api/quests/personal/{id}/progress | Record personal progress |
| PUT, DELETE | /api/player/avatar | Upload (multipart field file)/remove avatar |
| GET | /api/player/{id}/avatar | Retrieve public avatar |

EXP and individual stat XP are awarded by server-side quest/game logic, not by direct client mutations. The former direct mutation handlers were removed and are explicitly denied by Spring Security.

## Security and configuration

Backend uses stateless JWT authentication and BCrypt password hashing. JWT validation requires a valid signature, expiration and positive numeric user ID; invalid or expired tokens do not authenticate. Registration/login and public avatar retrieval are explicitly allowed; other application requests require authentication. Authenticated player-by-ID reads are available; personal quest operations and avatar writes use the authenticated user ID. Default framework error responses omit exception details, stack traces, binding errors and messages; security and web logging default to WARN. Keep secrets outside Git and bind local services to loopback. A public deployment requires a separate security and operational review.

Frontend excludes saved JWTs from login/registration, sends Bearer tokens on protected API calls and clears only the matching stored token after a protected 401 (not a newer token or an auth-endpoint failure). Tokens are still stored in browser localStorage; this does not eliminate XSS risk.

Daily settings example: `{"targets":{"pushups":20,"squats":30,"plank":60},"mode":"TOMORROW"}`. NOW applies immediately and clears current progress; TOMORROW defers the change. Hidden rewards are tied to the daily quest.

## Repository layout

- Backend: `src/main/java/org/smorodin/core` (models/services), `infrastructure` (auth, avatar, persistence, web, i18n), `config`; `src/main/resources/db/migration` (Flyway); `src/test`; `docker-compose.yml`; `pom.xml`.
- Frontend: `src/api`, `src/pages`, `src/components`, `src/types`, `src/i18n`, `src/utils`, `tests`, `vite.config.ts`.
- CI: `.github/workflows/backend-tests.yml` and `.github/workflows/frontend-ci.yml` in their respective repositories.

## Git and release workflow

Develop on feature branches from dev, open PRs into dev, review and require green CI. Release via dev → main PRs after owner approval; prefer merge commits to preserve history. Never force push or delete main. Both repositories now have a main branch. Do not recreate or reset either branch. Once final documentation and any release fixes are merged into dev, compare main and dev and open release PRs where differences exist. Verify CI on the exact main commits and complete the local acceptance checks before tagging both repositories with v0.1.0 after explicit owner approval. Continue feature development in dev.

## Known limitations

This is a local-only MVP, not a public production release. Cloud hosting, public HTTPS deployment, reverse proxy infrastructure and operational hardening are outside v0.1.0. Personal Quests REST API is available, but a full frontend management UI is not included. Quest Engine 2.0, story chains and AI-generated quests are future work. No separate native mobile app is included. Progress is user-reported; server-side reward logic does not verify real-world activity. Avatars are publicly readable and authenticated player-by-ID reads are not restricted to the current player in the current implementation. End-to-end local smoke tests must be checked on the final release candidate; public production deployment is not a release requirement.

## Release notes

[TheSystem v0.1.0 — First Stable Local MVP Release](RELEASE_NOTES.md)

## Repositories

- [Backend](https://github.com/ytenen/TheSystemBackend)
- [Frontend](https://github.com/ytenen/TheSystemFrontend)
