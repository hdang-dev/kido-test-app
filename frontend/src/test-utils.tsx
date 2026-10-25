import { render } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import type { MeResponse, Role } from './api/types';

export const ROUTER_FUTURE = { v7_startTransition: true, v7_relativeSplatPath: true } as const;

export function renderWithRouter(ui: ReactNode, path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]} future={ROUTER_FUTURE}>
      {ui}
    </MemoryRouter>,
  );
}

export const aUser = (role: Role = 'USER', overrides: Partial<MeResponse> = {}): MeResponse => ({
  id: '00000000-0000-0000-0000-000000000001',
  email: 'neo@test.local',
  username: 'neo',
  bio: null,
  role,
  ...overrides,
});

// Shape of useAuth() for vi.mocked(useAuth).mockReturnValue(...)
export const authState = (user: MeResponse | null = null) => ({
  user,
  loading: false,
  login: vi.fn(),
  logout: vi.fn(),
  refresh: vi.fn(),
  setUser: vi.fn(),
});
