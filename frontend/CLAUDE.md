# Frontend conventions

React 18 · TypeScript (strict) · Vite · React Router 6 · Vitest + Testing Library

- All HTTP goes through `src/api/client.ts` (`api<T>()`). It sends cookies and the CSRF header, and it throws `ApiError {status, message, fieldErrors}`. Never call `fetch` directly from a component.
- `src/api/types.ts` mirrors the backend DTOs. Keep it in sync with `docs/design.md` → API.
- The auth state comes from `useAuth()` (`src/context/AuthContext.tsx`). Never store a token or user data in `localStorage`.
- Route guards (`ProtectedRoute`, `AdminRoute`) are for UX only. Still handle `401` and `403` from the API.
- Each page is one file in `src/pages/`, and reusable pieces go in `src/components/`. Use named exports.
- Style with Tailwind utility classes only. There is no component library and no custom CSS beyond `src/index.css`. Keep it clean and minimal.
- Forms use controlled inputs and show `fieldErrors` next to each field.
- Tests sit next to the component as `*.test.tsx`. Mock `useAuth` (with `authState()` from `src/test-utils.tsx`) or the `src/api/*` module rather than the network, and render with `renderWithRouter`.
- Vitest gotcha: write `beforeEach(() => { mock.mockReset(); })` **with braces**. A function returned from a hook runs as teardown, and `mockReset()` returns the mock, which then gets called.
- Client-side validation (`src/validation.ts`) mirrors backend rules for fast feedback only. Keep it in sync with `PasswordPolicy` and the DTO limits.
