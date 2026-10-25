import type { Role } from '../api/types';

// Role hierarchy SUPER_ADMIN > ADMIN > USER (design.md §5). UX only — the backend enforces access.
export const isAdmin = (role: Role) => role === 'ADMIN' || role === 'SUPER_ADMIN';
