---
name: add-feature
description: Add a new feature to kido-test-app end to end — Spring Boot endpoint (DTO, service, controller, security rule, MockMvc tests) and/or React page (api call, types, page, route, Vitest test). Use when the user asks to add or extend a feature such as a profile action or an admin operation.
---

# Add a feature to kido-test-app

Work in the order **contract → backend → frontend → verify**. The reference code for each step is in [examples.md](examples.md).

## 1. Contract first
- Add the endpoint to `docs/design.md` → **API** (method, path, access, request, response). If there is a page, add it to **Frontend pages** too.
- Decide where it belongs: `user/` (self-service), `admin/` (admin only), or a new feature package under `com.kido.testapp`.

## 2. Backend (`backend/`)
1. **Request/response DTOs**: `record`s with Bean Validation. The response must never contain the password hash.
2. **Service**: put the business rules here, mark writes `@Transactional`, and throw specific exceptions so `GlobalExceptionHandler` can map them to `ErrorResponse`.
   Admin operations also get `@PreAuthorize("hasRole('ADMIN')")`.
3. **Controller**: keep it thin. Use `@Valid @RequestBody`, take the identity from `@AuthenticationPrincipal`, and return the DTO or `ResponseEntity`.
4. **Security**: add or confirm a matching rule in `config/SecurityConfig`.
5. **Tests** (MockMvc + `spring-security-test`). Cover:
   - the allowed role → 2xx
   - the wrong role → 403
   - anonymous → 401
   - invalid body → 400 with `fieldErrors`
   - mutating requests without `.with(csrf())` → 403

## 3. Frontend (`frontend/`)
1. **Types**: add the request/response interfaces to `src/api/types.ts`.
2. **API call**: add a small function to a `src/api/<feature>.ts` file that uses `api<T>()`. Never call `fetch` directly.
3. **Page**: put it in `src/pages/`, with controlled form inputs and the `ApiError.fieldErrors` shown per field. Show a success message or navigate on success.
4. **Route**: add it to `App.tsx` under the right guard (`ProtectedRoute` or `AdminRoute`), and link to it from the nav if it is user-visible.
5. **Test**: write `*.test.tsx` next to the page. Mock the api module and cover a successful submit and a displayed error.

## 4. Verify
```bash
cd backend && mvn test
cd frontend && npm test && npm run build
```
All three must pass. After that, tick the phase in `docs/design.md`, and update the README if anything user-visible changed (for example demo accounts).
