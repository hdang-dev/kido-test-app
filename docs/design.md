# Design

This file holds the overall architecture and plan. It is the single source of truth for **what we're building and why**.
Update it in the same PR as any code change that affects it. Git history and PRs on
[hdang-dev/kido-test-app](https://github.com/hdang-dev/kido-test-app) record when things changed.

## 1. Tech stack
| Concern     | Choice |
|-------------|--------|
| Backend     | Java 17 · Spring Boot 3.5 · Spring Security 6.5 · Spring Data JPA · Bean Validation |
| Sessions    | Spring Session JDBC (sessions stored in Postgres) |
| Database    | PostgreSQL 16 (local via `docker-compose.yml`) · Flyway migrations |
| Frontend    | React 18 · TypeScript (strict) · Vite · React Router 6 · Tailwind CSS v4 |
| Tests       | JUnit 5 · MockMvc · `spring-security-test` · Testcontainers (Postgres) · Vitest · Testing Library |
| Repo / flow | Git monorepo (`backend/`, `frontend/`) on GitHub, one branch + PR per phase |

## 2. Architecture
```
Browser ──► Vite dev server :5173 ──/api proxy──► Spring Boot :8080 ──► PostgreSQL :5432
            (React SPA)                           (REST JSON, session auth)     ├─ app_user
                                                                                └─ spring_session*
```
- The SPA and API share a single origin (the Vite proxy in dev; in prod, one reverse proxy or the backend serving `dist/`), so no CORS config is needed.
- The backend is the only security boundary. Frontend route guards exist for UX only.

## 3. Key decisions
| # | Decision | Why | Rejected alternatives |
|---|----------|-----|-----------------------|
| D1 | **Server-side session + cookie** (Spring Security), not JWT | The `HttpOnly` cookie can't be read by JS (XSS). Sessions can be revoked instantly on logout, password change, or role change. | JWT in `localStorage`: readable by XSS and hard to revoke |
| D2 | **Spring Session JDBC** | Sessions survive restarts. `FindByIndexNameSessionRepository` can find and expire all sessions of a user (needed by D4 and the password change). Built-in remember-me. | In-memory `HttpSession`: lost on restart and can't list a user's sessions |
| D3 | **CSRF via `csrf.spa()`** | Cookie-based auth needs CSRF protection. The SPA reads the `XSRF-TOKEN` cookie and sends `X-XSRF-TOKEN`. | Disabling CSRF: unsafe with cookies |
| D4 | **3-tier roles with one seeded SUPER_ADMIN** (see §5) | Lets admins grant admin freely, but stops admins revoking each other. There is a single, non-transferable root. | 2 roles where any admin revokes any admin: admin lock-out wars. Multiple supers: the same problem one level up |
| D5 | **Email is the identity. Username is a non-unique display name** | The default username comes from the part of the email before the @, so collisions are expected (`john@a.com`, `john@b.com`). | Unique username: forces awkward suffixes at sign-up |
| D6 | **Flyway owns the schema** (`ddl-auto: validate`) | The schema is versioned in git and reviewable. Tests run the same migrations. | `ddl-auto: update`: invisible, unreviewable schema drift |
| D7 | **Testcontainers Postgres in tests** | Tests use the same DB engine and migrations as the real app. | H2: dialect differences hide bugs (requires Docker running for `mvn test`) |
| D8 | **UUID primary keys** | IDs exposed in URLs and API responses don't reveal the user count and can't be guessed (defence in depth; authorization is the real protection). Postgres generates them natively. | `bigserial`: sequential and enumerable. UUIDv7: time-ordered, but Postgres 16 can't generate it natively and it isn't needed at this scale |

## 4. Domain and schema
**`app_user`** (Flyway `V1`)
| Column          | Type | Notes |
|-----------------|------|-------|
| `id`            | `uuid` PK, default `gen_random_uuid()` | Random (v4). Entity: `@GeneratedValue(strategy = GenerationType.UUID)` |
| `email`         | `varchar(254)` not null | Stored lower-cased and trimmed. `UNIQUE` |
| `password_hash` | `varchar(100)` not null | BCrypt |
| `username`      | `varchar(50)` not null | Display name, **not unique**. Defaults to the part of the email before the @ |
| `bio`           | `varchar(500)` null | |
| `role`          | `varchar(20)` not null | `USER` · `ADMIN` · `SUPER_ADMIN`. `CHECK` constraint |
| `created_at` / `updated_at` | `timestamptz` not null | |

Plus a partial unique index ensuring **at most one** `SUPER_ADMIN`.
**`spring_session`, `spring_session_attributes`** (Flyway `V2`): the standard Spring Session JDBC schema.

## 5. Roles and permissions
Role hierarchy: `SUPER_ADMIN > ADMIN > USER`, so `hasRole('ADMIN')` also lets SUPER_ADMIN through.

| Action                         | USER | ADMIN | SUPER_ADMIN |
|--------------------------------|:----:|:-----:|:-----------:|
| View and edit own profile, change own password | ✅ | ✅ | ✅ |
| View user management page      |  ❌  |  ✅   |     ✅      |
| Grant ADMIN to a USER          |  ❌  |  ✅   |     ✅      |
| Revoke ADMIN (ADMIN → USER)    |  ❌  |  ❌   |     ✅      |
| Grant SUPER_ADMIN              |  ❌  |  ❌   |     ❌ (nobody) |

Rules enforced in `AdminUserService`:
- Nobody can change their own role.
- The SUPER_ADMIN can never be the target of a role change.
- `SUPER_ADMIN` is not an accepted value in role-change requests.
- A role change **expires all sessions of the target user**, so their new authorities take effect immediately.

**Bootstrap:** on startup, `SuperAdminSeeder` creates the SUPER_ADMIN from `APP_SUPER_ADMIN_EMAIL` / `APP_SUPER_ADMIN_PASSWORD` **only if none exists**. It never overwrites an existing account, and the credentials never appear in Flyway files.
Recovery after losing the super admin password requires manual DB access (an accepted trade-off at this scope).

## 6. Flows
**Sign up**: `/signup`
1. The user enters email, password, confirm password, and optionally username and bio.
2. The frontend validates the format, then calls `POST /api/auth/signup`.
3. The backend validates again, normalises the email, rejects a duplicate with `409` (a race is caught by the DB unique constraint), sets username to the email local part if blank, hashes the password, and saves with role `USER`.
4. The backend **logs the user in automatically** (new session) and returns `201 MeResponse`. The frontend navigates to `/`.

**Login**: `/login`
1. The user enters email, password, and an optional "Remember me".
2. `POST /api/auth/login` → `AuthenticationManager` → the session ID is rotated → `200 MeResponse`.
3. A failure always returns `401` with "Invalid email or password". It never reveals which part was wrong.
4. **Session lifetime:** the idle timeout is 30 min. With "Remember me", the session cookie persists for 30 days (Spring Session remember-me). Without it, the cookie ends when the browser closes.

**Profile**: `/` (the home page after login)
- Shows email (read-only), username, and bio. They can be edited inline → `PUT /api/me`.
- Change password: `PUT /api/me/password`. It requires the current password, and the new password must differ from it.
  On success, all **other** sessions of the user are expired and the current session ID is rotated.

**User management**: `/admin/users` (ADMIN and SUPER_ADMIN)
- A paginated list: email, username, role, created date.
- A grant or revoke button is shown per row according to §5. The backend enforces the same rules.

**Logout**: `POST /api/auth/logout` invalidates the session and clears the cookie. The frontend then goes to `/login`.

## 7. API
All endpoints sit under `/api` and use JSON. Errors are returned as `ErrorResponse {message, fieldErrors: {field: msg}}`.

| Method | Path | Access | Request → Response |
|--------|------|--------|--------------------|
| POST | `/api/auth/signup` | public | `{email, password, confirmPassword, username?, bio?}` → `201 MeResponse` · `400` · `409` |
| POST | `/api/auth/login` | public | `{email, password, rememberMe}` → `200 MeResponse` · `401` |
| POST | `/api/auth/logout` | authenticated | → `204` |
| GET  | `/api/me` | authenticated | → `200 MeResponse` · `401` |
| PUT  | `/api/me` | authenticated | `{username, bio}` → `200 MeResponse` · `400` |
| PUT  | `/api/me/password` | authenticated | `{currentPassword, newPassword, confirmPassword}` → `204` · `400` |
| GET  | `/api/admin/users?page=&size=` | ADMIN+ | → `200 Page<UserSummary>` |
| PUT  | `/api/admin/users/{id}/role` | ADMIN+ (per §5) | `{role: "ADMIN" \| "USER"}` → `200 UserSummary` · `400` · `403` · `404` |

`MeResponse` = `{id (uuid string), email, username, bio, role}` · `UserSummary` = `{id, email, username, role, createdAt}`. Neither ever includes the hash.

## 8. Validation rules
| Field | Rule |
|-------|------|
| email | Required, valid format, max 254 characters, unique (case-insensitive) |
| password | 8–72 characters (BCrypt ignores everything past 72 bytes), at least one letter and one digit. Must equal `confirmPassword` |
| username | Optional at sign-up (blank → email local part), max 50 characters, trimmed |
| bio | Optional, max 500 characters |

Validation runs on both sides: on the frontend for UX, and on the backend as the authority.

## 9. Frontend pages
| Route | Guard | Page |
|-------|-------|------|
| `/signup` | public (redirects to `/` if logged in) | `SignupPage` |
| `/login` | public (redirects to `/` if logged in) | `LoginPage` |
| `/` | ProtectedRoute | `ProfilePage`: view and edit profile, change password section |
| `/admin/users` | AdminRoute (ADMIN, SUPER_ADMIN) | `AdminUsersPage` |

## 10. Known trade-offs
- Sign-up's "email already used" lets someone check whether an email is registered. The feature asks for it explicitly, so it is accepted.
- There is no email verification and no "forgot password" flow (out of scope).
- There is no login rate limiting or lockout (out of scope; would be listed as next steps).

## 11. Delivery process
- **One phase = one branch (`phase/N-name`) = one PR to `main`.** The PR description lists what changed and how it was verified.
- Commits are small conventional commits: `feat(auth): …`, `test(user): …`, `docs: …`, `chore: …`.
- **Docs ship with the code.** Every PR updates this file (status, API, and decisions where relevant), plus `CLAUDE.md`, the README, or the skills if they are affected.
- Before merging, `mvn test`, `npm test`, and `npm run build` must all be green, and the `security-review` skill must have been run on the diff.

## 12. Phases
| # | Phase | Scope | Status | PR |
|---|-------|-------|--------|----|
| 0 | Foundation | git and GitHub, monorepo scaffold, docker-compose Postgres, Flyway, Tailwind, docs and skills | 🟡 in progress | |
| 1 | Security core | `V1` and `V2` migrations, `AppUser`, `UserDetailsService`, `SecurityConfig` (session, CSRF, 401/403 JSON, role hierarchy), Spring Session JDBC, `SuperAdminSeeder`, `GlobalExceptionHandler`, Testcontainers base test | ⬜ | |
| 2 | Sign up / Login / Logout | Signup, login (remember me), logout, and `/api/me` on BE and FE; `SignupPage`, `LoginPage`, auth context, guards | ⬜ | |
| 3 | Profile | Edit username and bio, change password (expires other sessions), BE and FE tests | ⬜ | |
| 4 | User management | Paginated list, grant and revoke admin per §5, session expiry on role change, RBAC tests | ⬜ | |
| 5 | Hardening and wrap-up | `security-review` skill pass and fixes, README, demo script | ⬜ | |
