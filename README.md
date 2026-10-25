# kido-test-app

A simple authentication and user management app with:
- sign up and login (with "remember me")
- a profile page where you can edit your username and bio and change your password
- an admin page for managing users, with USER, ADMIN and SUPER_ADMIN roles

| Part | Tech | Port |
|------|------|------|
| `backend/`  | Java 17 · Spring Boot 3.5 · Spring Security · Spring Session JDBC · Flyway | 8080 |
| `frontend/` | React 18 · TypeScript · Vite · Tailwind CSS v4 | 5173 |
| Database    | PostgreSQL 16 (Docker) | 5432 |

## Requirements
- Java 17+ and Maven 3.9+
- Node 20+ and npm
- Docker Desktop. It must be running, both for the database and for the backend tests (Testcontainers).

## Run
```bash
docker compose up -d --wait                # 1. start Postgres, wait until healthy
cd backend && mvn spring-boot:run          # 2. API on :8080, dev profile (Flyway migrates the database on startup)
cd frontend && npm install && npm run dev  # 3. UI on :5173 (forwards /api to the backend)
```
Run each of steps 2 and 3 in its own terminal.
Then open http://localhost:5173.

To stop: press Ctrl+C in each terminal, then run `docker compose down`. To also wipe the database, use `docker compose down -v`.

## Test
```bash
cd backend && mvn test                      # starts a throwaway Postgres container, so Docker must be running
cd frontend && npm test && npm run build
```

## Configuration
| Env var | Default (`dev` profile only) | Purpose |
|---------|--------------------|---------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/testapp` / `testapp` / `testapp` | Database connection (**required** outside the `dev` profile) |
| `APP_SUPER_ADMIN_EMAIL` / `APP_SUPER_ADMIN_PASSWORD` | `superadmin@kido.local` / `SuperAdmin@123` | The super admin created on first startup (**required** outside `dev`) |

## Demo accounts
| Email | Password | Role |
|-------|----------|------|
| superadmin@kido.local | SuperAdmin@123 | SUPER_ADMIN (seeded on first startup, from Phase 1) |

Any other account can be created through the sign-up page.

## Docs
- [Requirements](docs/requirements.md)
- [Design](docs/design.md): architecture, key decisions, roles, flows, API, and phase status
