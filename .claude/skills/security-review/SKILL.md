---
name: security-review
description: Review kido-test-app (Spring Boot API + React SPA) for authentication and authorization flaws — RBAC gaps, password handling, CSRF, IDOR, session/cookie config, info leaks, token storage. Use before finishing a phase, after adding an endpoint, or when the user asks for a security check.
---

# Security review for kido-test-app

Review the changed code, or all of `backend/src/main` and `frontend/src` if the user asks for a full review, against this checklist.
For each item, report **PASS**, **FAIL**, or **N/A**, with `file:line` evidence. Then list the fixes, ordered by severity.

## Backend: authorization
- [ ] Every controller route has a matching rule in `SecurityConfig`. Only `/api/auth/login` and `/api/auth/signup` are `permitAll`, and the catch-all is `anyRequest().authenticated()`.
- [ ] `/api/admin/**` requires `ROLE_ADMIN` (the role hierarchy `SUPER_ADMIN > ADMIN > USER` is configured), and admin service methods also carry `@PreAuthorize`.
- [ ] Role rules from `design.md` §5 hold: only SUPER_ADMIN can revoke ADMIN, nobody changes their own role, SUPER_ADMIN is never a target, and `SUPER_ADMIN` is rejected as a requested role.
- [ ] A role change expires all of the target user's sessions, so stale authorities don't survive.
- [ ] Self-service endpoints take the identity from the security context, never from a path, query, or body parameter (no IDOR).
- [ ] Unauthenticated API calls get a `401` JSON response, not a `302` to a login page.
- [ ] Tests exist for 401 (anonymous), 403 (wrong role), and 403 (missing CSRF token).

## Backend: passwords
- [ ] Passwords are hashed with BCrypt through `PasswordEncoder`. There is no plaintext or fast-hash comparison.
- [ ] A change requires the current password. Validation enforces the length and complexity rules.
- [ ] Passwords and hashes never appear in logs, `toString()`, exception messages, or response DTOs.
- [ ] Seeded admin credentials come from config or env vars and are documented as demo-only.

## Backend: session, CSRF, and config
- [ ] CSRF is enabled for the SPA (`csrf.spa()` or an equivalent cookie repository). It is not disabled globally.
- [ ] The session cookie is `HttpOnly` and `SameSite=Lax` or stricter. The session ID is rotated on login (session fixation protection).
- [ ] Logout invalidates the session and clears the cookie.
- [ ] Login failures return a generic message that does not reveal whether an email is registered.
- [ ] CORS is not opened with `*` together with credentials (dev uses the Vite proxy, so CORS isn't needed).
- [ ] Entities are never bound from request bodies (no mass assignment of `role` or `enabled`).
- [ ] Queries use derived methods or parameters. There is no string-concatenated JPQL or SQL.
- [ ] Schema changes are new Flyway migrations (no edits to merged ones), and `ddl-auto` is `validate`. `server.error.include-stacktrace` is `never`.
- [ ] Password change expires the user's other sessions and rotates the current session ID.
- [ ] No secrets or real credentials are committed (the repo is public). The super admin comes from env vars.

## Frontend
- [ ] No token, password, or user object is stored in `localStorage` or `sessionStorage`.
- [ ] All requests go through `api/client.ts`, which uses `credentials: 'include'` and sends `X-XSRF-TOKEN` on mutating requests.
- [ ] No `dangerouslySetInnerHTML` with server or user data.
- [ ] Route guards are not the only protection. Every guarded page's API calls are also protected on the backend.
- [ ] `401` responses send the user to `/login`. `403` responses show a "not allowed" message rather than a blank page.
- [ ] Password inputs use `type="password"` and are cleared after submit.

## Output
End the review with a one-line verdict, **Ready** or **Needs fixes (N)**. Do not change code unless the user asks.
