import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { AdminRoute } from './AdminRoute';
import { useAuth } from '../context/AuthContext';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));

function renderAt(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<AdminRoute />}>
          <Route path="/admin/users" element={<p>admin area</p>} />
        </Route>
        <Route path="/profile" element={<p>profile</p>} />
        <Route path="/login" element={<p>login</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

const base = { loading: false, login: vi.fn(), logout: vi.fn(), refresh: vi.fn() };
const user = { id: '00000000-0000-0000-0000-000000000001', username: 'u', fullName: 'U', email: 'u@x.io' };

describe('AdminRoute', () => {
  it('lets an ADMIN through', () => {
    vi.mocked(useAuth).mockReturnValue({ ...base, user: { ...user, role: 'ADMIN' } });
    renderAt('/admin/users');
    expect(screen.getByText('admin area')).toBeInTheDocument();
  });

  it('sends a USER to /profile', () => {
    vi.mocked(useAuth).mockReturnValue({ ...base, user: { ...user, role: 'USER' } });
    renderAt('/admin/users');
    expect(screen.getByText('profile')).toBeInTheDocument();
  });

  it('sends anonymous to /login', () => {
    vi.mocked(useAuth).mockReturnValue({ ...base, user: null });
    renderAt('/admin/users');
    expect(screen.getByText('login')).toBeInTheDocument();
  });
});
