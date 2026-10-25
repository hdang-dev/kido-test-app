import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { useAuth } from '../context/AuthContext';
import { GuestRoute } from './GuestRoute';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));

function renderLoginAt(entry: string | { pathname: string; state: unknown }) {
  render(
    <MemoryRouter initialEntries={[entry]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route element={<GuestRoute />}>
          <Route path="/login" element={<p>login form</p>} />
        </Route>
        <Route path="/" element={<p>profile</p>} />
        <Route path="/admin/users" element={<p>admin area</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

const base = { loading: false, login: vi.fn(), logout: vi.fn(), refresh: vi.fn(), setUser: vi.fn() };
const loggedIn = {
  ...base,
  user: { id: '00000000-0000-0000-0000-000000000001', email: 'u@x.io', username: 'u', bio: null, role: 'ADMIN' as const },
};

describe('GuestRoute', () => {
  it('shows the page to anonymous users', () => {
    vi.mocked(useAuth).mockReturnValue({ ...base, user: null });
    renderLoginAt('/login');
    expect(screen.getByText('login form')).toBeInTheDocument();
  });

  it('sends a logged-in user to the profile by default', () => {
    vi.mocked(useAuth).mockReturnValue(loggedIn);
    renderLoginAt('/login');
    expect(screen.getByText('profile')).toBeInTheDocument();
  });

  it('returns a logged-in user to the page that redirected them', () => {
    vi.mocked(useAuth).mockReturnValue(loggedIn);
    renderLoginAt({ pathname: '/login', state: { from: { pathname: '/admin/users', search: '' } } });
    expect(screen.getByText('admin area')).toBeInTheDocument();
  });
});
