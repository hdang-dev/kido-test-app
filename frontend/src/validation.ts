// Client-side mirrors of backend rules (design.md §8) — for fast feedback only; the backend is the authority.

export const PASSWORD_MESSAGE = 'Password must be 8-72 characters with at least one letter and one digit';

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
