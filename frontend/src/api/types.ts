// Mirrors backend DTOs — keep in sync with docs/design.md §7 "API".

export type Role = 'USER' | 'ADMIN' | 'SUPER_ADMIN';

export interface MeResponse {
  id: string; // UUID
  email: string;
  username: string;
  bio: string | null;
  role: Role;
}

// Role hierarchy SUPER_ADMIN > ADMIN > USER (design.md §5)
export const isAdmin = (role: Role) => role === 'ADMIN' || role === 'SUPER_ADMIN';
