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

Outside the `dev` profile, the `SESSION` and `XSRF-TOKEN` cookies are `Secure`, so the app must be served over **https**.

## Demo accounts
| Email | Password | Role |
|-------|----------|------|
| superadmin@kido.local | SuperAdmin@123 | SUPER_ADMIN (seeded on first startup) |

Any other account can be created through the sign-up page.

## Demo walkthrough (about 3 minutes)
1. Open http://localhost:5173 → you're sent to **Log in**. Click **Sign up**.
2. Sign up with `neo@test.local` / `Passw0rd!` → you're logged in and land on the **profile** (username defaults to `neo`).
3. **Edit profile**: set a username and a bio → **Save**.
4. **Change password**: wrong current password → field error; correct one → "other devices have been signed out".
5. Try http://localhost:5173/admin/users → a USER is sent back to the profile (and the API answers 403).
6. **Log out**, then log in as `superadmin@kido.local` / `SuperAdmin@123` → the **Users** link appears.
7. **Users**: click **Make admin** on `neo@test.local` → confirm. Neo's sessions are signed out; when Neo logs in again, Neo sees **Users**.
8. As super admin, **Remove admin** on Neo. A regular admin wouldn't see that button, and the API would refuse it (403).

## Docs
- [Requirements](docs/requirements.md)
- [Design](docs/design.md): architecture, key decisions, roles, flows, API, and phase status
