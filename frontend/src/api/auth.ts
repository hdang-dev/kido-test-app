import { api } from './client';
import type { LoginRequest, MeResponse, SignupRequest } from './types';

export const fetchMe = () => api<MeResponse>('/me');

export const signup = (body: SignupRequest) =>
  api<MeResponse>('/auth/signup', { method: 'POST', body: JSON.stringify(body) });

export const login = (body: LoginRequest) =>
  api<MeResponse>('/auth/login', { method: 'POST', body: JSON.stringify(body) });

export const logout = () => api<void>('/auth/logout', { method: 'POST' });
