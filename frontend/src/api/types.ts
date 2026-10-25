// Mirrors backend DTOs — keep in sync with docs/design.md §7 "API".

export type Role = 'USER' | 'ADMIN' | 'SUPER_ADMIN';

export interface MeResponse {
  id: string; // UUID
  email: string;
  username: string;
  bio: string | null;
  role: Role;
}

export interface SignupRequest {
  email: string;
  password: string;
  confirmPassword: string;
  username?: string;
  bio?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe: boolean;
}

export interface UpdateProfileRequest {
  username: string;
  bio: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface UserSummary {
  id: string;
  email: string;
  username: string;
  role: Role;
  createdAt: string; // ISO-8601 instant
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
