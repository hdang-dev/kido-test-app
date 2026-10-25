import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import type { Role } from '../api/types';
import { useAuth } from '../context/AuthContext';
import { aUser, authState, renderWithRouter } from '../test-utils';
import { AppLayout } from './AppLayout';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));

describe('AppLayout', () => {
  it('hides the Users link from regular users', () => {
    vi.mocked(useAuth).mockReturnValue(authState(aUser('USER')));
    renderWithRouter(<AppLayout />);
    expect(screen.queryByRole('link', { name: 'Users' })).not.toBeInTheDocument();
  });

  it.each<Role>(['ADMIN', 'SUPER_ADMIN'])('shows the Users link to %s', (role) => {
    vi.mocked(useAuth).mockReturnValue(authState(aUser(role)));
    renderWithRouter(<AppLayout />);
    expect(screen.getByRole('link', { name: 'Users' })).toHaveAttribute('href', '/admin/users');
  });

  it('logs out', async () => {
    const state = authState(aUser());
    vi.mocked(useAuth).mockReturnValue(state);
    renderWithRouter(<AppLayout />);

    await userEvent.click(screen.getByRole('button', { name: 'Log out' }));

    expect(state.logout).toHaveBeenCalled();
  });

  it('tells the user when logout failed', async () => {
    const state = authState(aUser());
    state.logout.mockRejectedValue(new Error('network'));
    vi.mocked(useAuth).mockReturnValue(state);
    renderWithRouter(<AppLayout />);

    await userEvent.click(screen.getByRole('button', { name: 'Log out' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Logout failed');
  });
});
