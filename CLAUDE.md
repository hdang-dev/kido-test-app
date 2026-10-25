# kido-test-app

A monorepo for a pre-interview test. It is a small auth app with sign up, login (remember me), a profile page (edit username and bio, change password), and admin user management with a USER / ADMIN / SUPER_ADMIN role model.

- `backend/`: Spring Boot 3.5 REST API, package `com.kido.testapp`. Spring Security sessions are stored in Postgres (Spring Session JDBC), and Flyway owns the schema. See [backend/CLAUDE.md](backend/CLAUDE.md).
- `frontend/`: React 18 + TypeScript + Vite + Tailwind SPA. See [frontend/CLAUDE.md](frontend/CLAUDE.md).
- GitHub: https://github.com/hdang-dev/kido-test-app (**public**, so never commit secrets)

## Read first
- `docs/requirements.md`: the assignment (Part A) and the clarified requirements (Part B)
- `docs/design.md`: the architecture, **key decisions (§3)**, roles (§5), flows (§6), API (§7), and the **phase table (§12)**, which shows the current phase

## Commands
| What | Command |
|------|---------|
| Start Postgres | `docker compose up -d` (repo root) |
| Run BE (:8080) | `cd backend && mvn spring-boot:run` |
| Run FE (:5173, proxies `/api`) | `cd frontend && npm run dev` |
| Test BE (needs Docker running) | `cd backend && mvn test` |
| Test FE | `cd frontend && npm test` |
| Type-check + build FE | `cd frontend && npm run build` |

## Cross-cutting rules
- `docs/design.md` §7 **API** is the contract. When you add or change an endpoint, update that table, the backend DTO, and `frontend/src/api/types.ts` in the same change.
- Schema changes go in a **new** Flyway migration. Never edit a migration that has already been merged.
- The backend is the only security boundary. Frontend guards exist for UX only.
- Never log, return, or render a password or password hash.
- Keep the UI minimal and clean with Tailwind. The brief puts a polished UI out of scope.

## Workflow (design.md §11)
- One phase = one branch `phase/N-name` = one PR to `main`. Use small conventional commits.
- Docs ship with code. Update `design.md` (the phase status and anything else affected) in the same PR.
- Before opening a PR, `mvn test`, `npm test`, and `npm run build` must be green, and the `security-review` skill must have been run.

## Skills
- `.claude/skills/add-feature`: use it when adding an endpoint and/or page
- `.claude/skills/security-review`: run it before calling a phase done
