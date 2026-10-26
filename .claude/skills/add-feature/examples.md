# Reference implementation: "change password", end to end

These are real, tested files. Open them and copy their shape. Excerpts below show the parts that matter.

| Layer | File |
|---|---|
| Request DTO | `backend/src/main/java/com/kido/testapp/user/ChangePasswordRequest.java` |
| Service rule | `backend/src/main/java/com/kido/testapp/user/UserService.java` → `changePassword` |
| Controller | `backend/src/main/java/com/kido/testapp/auth/PasswordController.java` |
| Backend test | `backend/src/test/java/com/kido/testapp/user/MeControllerTest.java` |
| Test helper | `backend/src/test/java/com/kido/testapp/AuthTestClient.java` |
| API call | `frontend/src/api/me.ts` |
| Component | `frontend/src/components/ChangePasswordForm.tsx` |
| Component test | `frontend/src/components/ChangePasswordForm.test.tsx` |

## Backend: DTO (validation only, with `toString` that never shows secrets)
```java
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @ValidPassword String newPassword,   // never @Size/@Pattern for passwords (72-byte rule)
        @NotBlank String confirmPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[***]";
    }
}
```

## Backend: service (cross-field and business rules → `ApiException.field`)
```java
@Transactional
public void changePassword(String email, String currentPassword, String newPassword, String confirmPassword) {
    AppUser user = repository.findByEmailForUpdate(Emails.normalise(email)).orElseThrow(...); // row locked
    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
        throw ApiException.field(HttpStatus.BAD_REQUEST, "currentPassword", "Current password is incorrect");
    }
    // ... confirmation, must differ ...
    user.changePasswordHash(passwordEncoder.encode(newPassword)); // also bumps the credential version (D10)
}
```

## Backend: controller (identity from the session, thin orchestration)
```java
@PutMapping("/api/me/password")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void changePassword(@Valid @RequestBody ChangePasswordRequest body, Authentication authentication,
                           HttpServletRequest request, HttpServletResponse response) {
    // identity from the session, never from the request body or path
    Instant newCredentialVersion = userService.changePassword(authentication.getName(), body.currentPassword(),
            body.newPassword(), body.confirmPassword(), request.getSession().getId());
    sessionLogin.afterPasswordChange(authentication, newCredentialVersion, request, response); // session work: SessionLogin
}
```

## Backend: test (`@IntegrationTest` + `AuthTestClient`: real session cookie + real CSRF)
```java
@IntegrationTest
class MeControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    AuthTestClient client;

    @BeforeEach
    void setUp() { client = new AuthTestClient(mvc, json); }

    @Test
    void expiresOtherSessionsAndRotatesTheCurrentOne() throws Exception {
        String email = uniqueEmail();                 // unique per test: no shared-data coupling
        Cookie laptop = client.signup(email);
        Cookie phone = client.login(email, PASSWORD);

        Cookie rotated = client.put("/api/me/password", passwordChange(PASSWORD, NEW_PASSWORD, NEW_PASSWORD), laptop)
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("SESSION");

        client.getAs(phone, "/api/me").andExpect(status().isUnauthorized());
        client.getAs(rotated, "/api/me").andExpect(status().isOk());
    }
}
```
Always cover: allowed → 2xx, anonymous → 401, missing CSRF (`client.putWithoutCsrf`) → 403, invalid body → 400 with `fieldErrors`, and the wrong role → 403 for role-restricted endpoints.

## Frontend: API function (`src/api/<feature>.ts`, always through `api<T>()`)
```ts
export const changePassword = (body: ChangePasswordRequest) =>
  api<void>('/me/password', { method: 'PUT', body: JSON.stringify(body) });
```

## Frontend: component pattern
- Run client-side checks first, using `src/validation.ts` (it mirrors the backend rules).
- Call the API. On `ApiError`, show `fieldErrors` per field, or else `err.message`.
- Clear password inputs after any attempt that reached the server.
- Use `FormField` / `TextAreaField` for accessible labels and errors.

## Frontend: test (mock the `src/api/*` module, assert what the user sees)
```tsx
vi.mock('../api/me');

beforeEach(() => {
  vi.mocked(meApi.changePassword).mockReset();   // braces: see frontend/CLAUDE.md
});

it('shows the server error for a wrong current password', async () => {
  vi.mocked(meApi.changePassword).mockRejectedValue(
    new ApiError(400, 'Current password is incorrect', { currentPassword: 'Current password is incorrect' }),
  );
  render(<ChangePasswordForm />);
  // ...type and submit...
  expect(await screen.findByText('Current password is incorrect')).toBeInTheDocument();
});
```
