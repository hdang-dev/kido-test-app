import { emailError, passwordError } from './validation';

describe('emailError', () => {
  it.each(['a@b.co', '  John@Example.com '])('accepts %s', (email) => {
    expect(emailError(email)).toBeUndefined();
  });

  it.each(['', 'plain', 'user@nodot', 'a b@c.de'])('rejects %j', (email) => {
    expect(emailError(email)).toBeDefined();
  });
});

describe('passwordError (mirrors backend PasswordPolicy)', () => {
  it.each(['abcdefg1', 'Passw0rd!', 'mật khẩu 2024'])('accepts %s', (pw) => {
    expect(passwordError(pw)).toBeUndefined();
  });

  it.each(['abc1234', 'abcdefgh', '12345678'])('rejects %s', (pw) => {
    expect(passwordError(pw)).toBeDefined();
  });

  it('enforces 72 UTF-8 bytes, not 72 characters', () => {
    expect(passwordError('a1' + 'x'.repeat(70))).toBeUndefined(); // 72 bytes
    expect(passwordError('a1' + 'ậ'.repeat(30))).toBeDefined(); // 32 chars, 92 bytes
  });
});
