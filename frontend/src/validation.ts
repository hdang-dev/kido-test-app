// Client-side mirrors of backend rules (design.md §8) — for fast feedback only; the backend is the authority.

export const PASSWORD_MESSAGE = 'Password must be 8-72 characters with at least one letter and one digit';
export const USERNAME_MAX = 50;
export const BIO_MAX = 500;

// Same shape the backend accepts: something@something.tld
export function emailError(email: string): string | undefined {
  const value = email.trim();
  if (!value) return 'Email is required';
  if (value.length > 254) return 'Email must be at most 254 characters';
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'Enter a valid email address';
  return undefined;
}

// Mirrors PasswordPolicy: >= 8 code points, <= 72 UTF-8 bytes (BCrypt), a letter and a digit.
export function passwordError(password: string): string | undefined {
  const codePoints = [...password].length;
  const bytes = new TextEncoder().encode(password).length;
  if (codePoints < 8 || bytes > 72 || !/\p{L}/u.test(password) || !/\d/.test(password)) {
    return PASSWORD_MESSAGE;
  }
  return undefined;
}

// Mirrors @MaxLength: the trimmed value, counted in code points (an emoji is 1, like Postgres varchar).
export const textLength = (value: string) => [...value.trim()].length;

export const usernameError = (username: string) =>
  textLength(username) > USERNAME_MAX ? `Username must be at most ${USERNAME_MAX} characters` : undefined;

export const bioError = (bio: string) =>
  textLength(bio) > BIO_MAX ? `Bio must be at most ${BIO_MAX} characters` : undefined;
