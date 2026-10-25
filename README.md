# kido-test-app

A simple authentication and user management app. It has login, a profile page, password change, and ADMIN/USER roles.

| Part | Tech | Port |
|------|------|------|
| `backend/`  | Java 17 · Spring Boot 3.5 · Spring Security · Spring Session JDBC · Postgres · Flyway | 8080 |
| `frontend/` | React 18 · TypeScript · Vite · Tailwind | 5173 |

## Requirements
- Java 17+ and Maven 3.9+
- Node 20+ and npm

## Run
In two terminals:
```bash
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev
```
Then open http://localhost:5173. Vite forwards `/api` to the backend.

## Test
```bash
cd backend && mvn test
cd frontend && npm test
```

## Demo accounts
| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | Admin@123 | ADMIN |

The admin account is created on startup. You can override it with `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`.
Data is in-memory, so it resets on every restart.

## Docs
- [Requirements](docs/requirements.md)
- [Design](docs/design.md): includes the auth decision and the API list
