# Reference code: change password (end to end)

> TODO (phase 3): once the change-password flow is built, replace these sketches with that real,
> tested code so the examples always match the codebase.

## Backend: DTO (`user/ChangePasswordRequest.java`)
```java
package com.kido.testapp.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain a letter and a digit")
        String newPassword,
        @NotBlank String confirmPassword) {
}
```

## Backend: Service (`user/UserService.java`)
```java
@Service
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        AppUser user = repository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("currentPassword", "Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new InvalidPasswordException("confirmPassword", "Passwords do not match");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }
}
```

## Backend: Controller (`user/MeController.java`)
```java
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getUsername(), request);
        return ResponseEntity.noContent().build();
    }
}
```

## Backend: Test (`user/MeControllerTest.java`)
```java
@SpringBootTest
@AutoConfigureMockMvc
class MeControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void anonymousGets401() throws Exception {
        mvc.perform(put("/api/me/password").with(csrf())
                        .contentType(APPLICATION_JSON).content("{}"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void missingCsrfGets403() throws Exception {
        mvc.perform(put("/api/me/password").contentType(APPLICATION_JSON).content("{}"))
           .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotListUsers() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
    }
}
```

## Frontend: API (`src/api/me.ts`)
```ts
import { api } from './client';
import type { ChangePasswordRequest } from './types';

export const changePassword = (body: ChangePasswordRequest) =>
  api<void>('/me/password', { method: 'PUT', body: JSON.stringify(body) });
```

## Frontend: Component (`src/components/ChangePasswordForm.tsx`, rendered inside `ProfilePage`)
```tsx
import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { changePassword } from '../api/me';

export function ChangePasswordForm() {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState('');

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrors({});
    try {
      await changePassword(form);
      setMessage('Password changed');
      setForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      if (err instanceof ApiError) setErrors({ ...err.fieldErrors, _: err.message });
      else throw err;
    }
  };

  const field = (name: keyof typeof form, label: string) => (
    <label className="block space-y-1">
      <span className="text-sm font-medium">{label}</span>
      <input
        type="password"
        className="w-full rounded border px-3 py-2"
        value={form[name]}
        onChange={(e) => setForm({ ...form, [name]: e.target.value })}
      />
      {errors[name] && <span role="alert" className="text-sm text-red-600">{errors[name]}</span>}
    </label>
  );

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <h2 className="text-lg font-semibold">Change password</h2>
      {field('currentPassword', 'Current password')}
      {field('newPassword', 'New password')}
      {field('confirmPassword', 'Confirm new password')}
      {errors._ && <p role="alert" className="text-red-600">{errors._}</p>}
      {message && <p role="status" className="text-green-700">{message}</p>}
      <button type="submit" className="rounded bg-blue-600 px-4 py-2 text-white">Save</button>
    </form>
  );
}
```

## Frontend: Test (`src/components/ChangePasswordForm.test.tsx`)
```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { ApiError } from '../api/client';
import * as meApi from '../api/me';
import { ChangePasswordForm } from './ChangePasswordForm';

vi.mock('../api/me');

it('shows the field error returned by the API', async () => {
  vi.mocked(meApi.changePassword).mockRejectedValue(
    new ApiError(400, 'Validation failed', { currentPassword: 'Current password is incorrect' }),
  );
  render(<ChangePasswordForm />);
  await userEvent.click(screen.getByRole('button', { name: 'Save' }));
  expect(await screen.findByText('Current password is incorrect')).toBeInTheDocument();
});
```
