import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { authState, renderWithRouter } from '../test-utils';
import { LoginPage } from './LoginPage';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));

describe('LoginPage', () => {
  it('logs in with email, password and the remember-me choice', async () => {
    const state = authState();
    vi.mocked(useAuth).mockReturnValue(state);
    renderWithRouter(<LoginPage />);

    await userEvent.type(screen.getByLabelText('Email'), 'neo@test.local');
    await userEvent.type(screen.getByLabelText('Password'), 'Passw0rd!');
    await userEvent.click(screen.getByLabelText(/remember me/i));
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(state.login).toHaveBeenCalledWith('neo@test.local', 'Passw0rd!', true);
  });

  it('shows the generic error and clears the password on 401', async () => {
    const state = authState();
    state.login.mockRejectedValue(new ApiError(401, 'Invalid email or password'));
    vi.mocked(useAuth).mockReturnValue(state);
    renderWithRouter(<LoginPage />);

    await userEvent.type(screen.getByLabelText('Email'), 'neo@test.local');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
    expect(screen.getByLabelText('Password')).toHaveValue('');
  });

  it('links to sign-up', () => {
    vi.mocked(useAuth).mockReturnValue(authState());
    renderWithRouter(<LoginPage />);
    expect(screen.getByRole('link', { name: 'Sign up' })).toHaveAttribute('href', '/signup');
  });
});
