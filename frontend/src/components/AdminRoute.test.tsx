import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import type { Role } from '../api/types';
import { useAuth } from '../context/AuthContext';
import { AdminRoute } from './AdminRoute';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));

function renderAdminPage() {
  render(
    <MemoryRouter initialEntries={['/admin/users']} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route element={<AdminRoute />}>
          <Route path="/admin/users" element={<p>admin area</p>} />
        </Route>
        <Route path="/" element={<p>profile</p>} />
        <Route path="/login" element={<p>login</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

const base = { loading: false, login: vi.fn(), logout: vi.fn(), refresh: vi.fn(), setUser: vi.fn() };
const asRole = (role: Role) => ({
  ...base,
  user: { id: '00000000-0000-0000-0000-000000000001', email: 'u@x.io', username: 'u', bio: null, role },
});

describe('AdminRoute', () => {
  it.each<Role>(['ADMIN', 'SUPER_ADMIN'])('lets %s through', (role) => {
    vi.mocked(useAuth).mockReturnValue(asRole(role));
    renderAdminPage();
    expect(screen.getByText('admin area')).toBeInTheDocument();
  });

  it('sends a USER to the profile page', () => {
    vi.mocked(useAuth).mockReturnValue(asRole('USER'));
    renderAdminPage();
    expect(screen.getByText('profile')).toBeInTheDocument();
  });

  it('sends anonymous to /login', () => {
    vi.mocked(useAuth).mockReturnValue({ ...base, user: null });
    renderAdminPage();
    expect(screen.getByText('login')).toBeInTheDocument();
  });
});
