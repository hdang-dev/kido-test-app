import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import * as authApi from '../api/auth';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { aUser, authState, renderWithRouter } from '../test-utils';
import { SignupPage } from './SignupPage';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));
vi.mock('../api/auth');

async function fill(email: string, password: string, confirm = password) {
  await userEvent.type(screen.getByLabelText('Email'), email);
  await userEvent.type(screen.getByLabelText('Password'), password);
  await userEvent.type(screen.getByLabelText('Confirm password'), confirm);
}

const submit = () => userEvent.click(screen.getByRole('button', { name: 'Sign up' }));

describe('SignupPage', () => {
  // Braces matter: a function returned from beforeEach is run by Vitest as teardown (mockReset returns the mock).
  beforeEach(() => {
    vi.mocked(authApi.signup).mockReset();
  });

  it('signs up and stores the returned user (GuestRoute then redirects to /)', async () => {
    const state = authState();
    vi.mocked(useAuth).mockReturnValue(state);
    const me = aUser();
    vi.mocked(authApi.signup).mockResolvedValue(me);
    renderWithRouter(<SignupPage />);

    await fill('neo@test.local', 'Passw0rd!');
    await submit();

    expect(authApi.signup).toHaveBeenCalledWith({
      email: 'neo@test.local',
      password: 'Passw0rd!',
      confirmPassword: 'Passw0rd!',
      username: undefined,
      bio: undefined,
    });
    expect(state.setUser).toHaveBeenCalledWith(me);
  });

  it('suggests the email local part as the default username', async () => {
    vi.mocked(useAuth).mockReturnValue(authState());
    renderWithRouter(<SignupPage />);

    await userEvent.type(screen.getByLabelText('Email'), ' Trinity@Test.local'); // previewed as the backend normalises

    expect(screen.getByLabelText('Username (optional)')).toHaveAttribute('placeholder', 'trinity');
  });

  it('validates on the client before calling the API', async () => {
    vi.mocked(useAuth).mockReturnValue(authState());
    renderWithRouter(<SignupPage />);

    await fill('not-an-email', 'short', 'different');
    await submit();

    expect(screen.getByText('Enter a valid email address')).toBeInTheDocument();
    expect(screen.getByText(/Password must be 8-72 characters/)).toBeInTheDocument();
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    expect(authApi.signup).not.toHaveBeenCalled();
  });

  it('shows the server field error when the email is taken', async () => {
    vi.mocked(useAuth).mockReturnValue(authState());
    vi.mocked(authApi.signup).mockRejectedValue(
      new ApiError(409, 'Email is already registered', { email: 'Email is already registered' }),
    );
    renderWithRouter(<SignupPage />);

    await fill('neo@test.local', 'Passw0rd!');
    await submit();

    expect(await screen.findByText('Email is already registered')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true');
  });
});
