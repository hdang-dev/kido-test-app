// Mirrors backend DTOs — keep in sync with docs/design.md "API".

export type Role = 'ADMIN' | 'USER';

export interface CurrentUser {
  id: string; // UUID
  username: string;
  fullName: string;
  email: string;
  role: Role;
}
