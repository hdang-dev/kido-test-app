import { api } from './client';
import type { Page, UserSummary } from './types';

export const listUsers = (page: number, size = 20) =>
  api<Page<UserSummary>>(`/admin/users?page=${page}&size=${size}`);

// Only ADMIN | USER: SUPER_ADMIN can never be granted (design.md §5)
export const changeRole = (id: string, role: 'ADMIN' | 'USER') =>
  api<UserSummary>(`/admin/users/${encodeURIComponent(id)}/role`, {
    method: 'PUT',
    body: JSON.stringify({ role }),
  });
