import type { Role } from '../api/types';

// Role hierarchy SUPER_ADMIN > ADMIN > USER (design.md §5). UX only — the backend enforces access.
export const isAdmin = (role: Role) => role === 'ADMIN' || role === 'SUPER_ADMIN';

export type RoleAction =
  | { kind: 'grant' }
  | { kind: 'revoke' }
  | { kind: 'none'; reason: string };

/** Which button the user-management page shows for a row — mirrors AdminUserService.changeRole. */
export function roleActionFor(actor: { id: string; role: Role }, target: { id: string; role: Role }): RoleAction {
  if (target.id === actor.id) return { kind: 'none', reason: 'You' };
  if (target.role === 'SUPER_ADMIN') return { kind: 'none', reason: 'Protected' };
  if (target.role === 'USER') return { kind: 'grant' };
  return actor.role === 'SUPER_ADMIN' ? { kind: 'revoke' } : { kind: 'none', reason: 'Only the super admin can revoke' };
}
