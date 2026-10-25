import { api } from './client';
import type { ChangePasswordRequest, MeResponse, UpdateProfileRequest } from './types';

export const updateProfile = (body: UpdateProfileRequest) =>
  api<MeResponse>('/me', { method: 'PUT', body: JSON.stringify(body) });

// 204; the backend also logs out the user's other sessions and rotates this one's ID
export const changePassword = (body: ChangePasswordRequest) =>
  api<void>('/me/password', { method: 'PUT', body: JSON.stringify(body) });
