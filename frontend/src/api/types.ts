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
