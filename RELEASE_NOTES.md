# TheSystem v0.1.0 — First Stable Local MVP Release

TheSystem is an RPG-inspired web application for personal goals, daily quests and character progression. Version 0.1.0 targets running the application on a single computer with a local PostgreSQL database, Java backend and browser frontend. It is not a public production deployment.

## Features

- Authentication: registration, login, JWT authentication and BCrypt password hashing.
- Hunter Profile: rank, level, player EXP and six attributes.
- Player EXP and Level: server-controlled quest rewards and level progression.
- Stat XP Progression: independent attribute XP bars and quest reward indicators.
- Daily and Hidden Quests: progress tracking, completion rewards and duplicate-reward prevention.
- Personal Daily Settings: NOW applies new targets immediately and clears current progress; TOMORROW schedules targets for the next day.
- Avatar System: upload, cropping, persistent storage and removal.
- EN/RU Localization: interface and system/API messages.

## Security and stability

Direct client EXP and stat mutation handlers have been removed and their paths are explicitly denied. JWT signature, expiration, subject and signing-key configuration are validated. Public access is limited to registration/login and avatar retrieval; other application endpoints require authentication. Explicit CORS origins are configurable; verbose security/web logging and default technical error details are disabled by default. The frontend does not send a saved JWT with authentication requests and only clears the matching stored token after a protected 401. Tokens remain in browser localStorage.

## Requirements and compatibility

Use matching Backend and Frontend versions 0.1.0. Install Git, Java 17, Maven, Node.js 22 (22.12 or newer compatible 22.x), npm and Docker with Compose v2. PostgreSQL 16 runs in Docker; Flyway applies migrations V1–V4. Back up existing databases and retain their password and volume.

## Run locally

After the release tags exist:

```sh
git clone --branch v0.1.0 https://github.com/ytenen/TheSystemBackend.git
git clone --branch v0.1.0 https://github.com/ytenen/TheSystemFrontend.git
```

Before tagging, use the matching reviewed release candidates instead.

Backend terminal (fresh local database only; otherwise reuse existing credentials):

```sh
cd TheSystemBackend
export POSTGRES_PASSWORD="$(openssl rand -hex 32)"
export DB_PASSWORD="$POSTGRES_PASSWORD"
export JWT_SECRET="$(openssl rand -hex 32)"
export SERVER_ADDRESS=127.0.0.1
docker compose up -d
docker compose ps
```

After PostgreSQL is healthy, run `mvn spring-boot:run`. The API is at http://localhost:8080 and PostgreSQL at 127.0.0.1:5433. Compose starts only PostgreSQL.

Frontend terminal, from the common parent directory:

```sh
cd TheSystemFrontend
npm ci
VITE_API_BASE_URL=http://localhost:8080 npm run dev -- --host localhost --port 5173 --strictPort
```

Open http://localhost:5173. The backend CORS default permits this origin. The frontend has a development-only localhost fallback; always configure the URL explicitly for a compiled build. For optional local build/preview, use `VITE_API_BASE_URL=http://localhost:8080 npm run build` and `npm run preview -- --host localhost --port 4173 --strictPort`, allowing http://localhost:4173 in backend CORS and restarting the backend first. The current client checks the URL during initialization, not at build time. Vite variables are public; never put secrets in them.

Backend configuration: DB_PASSWORD, POSTGRES_PASSWORD, JWT_SECRET (at least 32 UTF-8 bytes), CORS_ALLOWED_ORIGINS (default http://localhost:5173; explicit comma-separated origins), SECURITY_LOG_LEVEL and WEB_LOG_LEVEL (both default WARN), and optional Spring Boot SERVER_ADDRESS for loopback binding.

Stop applications with Ctrl+C and use `docker compose stop` to stop PostgreSQL without deleting data. Reuse credentials on restart. See both README files for tests, migrations and local acceptance checks.

## Known limitations

- Personal Quests REST API exists without a complete frontend management UI.
- Quest Engine 2.0, story chains, AI-generated quests and a separate native mobile application are not part of this MVP.
- Quest progress is user-reported; avatars are publicly readable and player-by-ID profiles are readable by authenticated users.
- Public hosting, cloud infrastructure, reverse proxy deployment and operational hardening are outside this release.

## Repositories

- [Backend](https://github.com/ytenen/TheSystemBackend)
- [Frontend](https://github.com/ytenen/TheSystemFrontend)
