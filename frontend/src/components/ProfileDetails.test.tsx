import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { ApiError } from '../api/client';
import * as meApi from '../api/me';
import { useAuth } from '../context/AuthContext';
import { aUser, authState } from '../test-utils';
import { ProfileDetails } from './ProfileDetails';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));
vi.mock('../api/me');

describe('ProfileDetails', () => {
  beforeEach(() => {
    vi.mocked(meApi.updateProfile).mockReset();
  });

  it('shows email, username, bio and role', () => {
    vi.mocked(useAuth).mockReturnValue(authState());
    render(<ProfileDetails user={aUser('ADMIN', { bio: 'Hello there' })} />);

    expect(screen.getByText('neo@test.local')).toBeInTheDocument();
    expect(screen.getByText('neo')).toBeInTheDocument();
    expect(screen.getByText('Hello there')).toBeInTheDocument();
    expect(screen.getByText('Admin')).toBeInTheDocument();
  });

  it('edits username and bio, then stores the returned user', async () => {
    const state = authState();
    vi.mocked(useAuth).mockReturnValue(state);
    const updated = aUser('USER', { username: 'Morpheus', bio: 'Captain' });
    vi.mocked(meApi.updateProfile).mockResolvedValue(updated);
    render(<ProfileDetails user={aUser()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Edit profile' }));
    expect(screen.getByLabelText('Email')).toBeDisabled();
    await userEvent.clear(screen.getByLabelText('Username'));
    await userEvent.type(screen.getByLabelText('Username'), ' Morpheus ');
    await userEvent.type(screen.getByLabelText('Bio'), 'Captain');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(meApi.updateProfile).toHaveBeenCalledWith({ username: 'Morpheus', bio: 'Captain' });
    expect(state.setUser).toHaveBeenCalledWith(updated);
    expect(await screen.findByRole('status')).toHaveTextContent('Profile updated');
  });

  it('cancel leaves the profile untouched', async () => {
    vi.mocked(useAuth).mockReturnValue(authState());
    render(<ProfileDetails user={aUser()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Edit profile' }));
    await userEvent.type(screen.getByLabelText('Bio'), 'draft');
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(meApi.updateProfile).not.toHaveBeenCalled();
    expect(screen.getByText('No bio yet')).toBeInTheDocument();
  });

  it('shows server field errors', async () => {
    vi.mocked(useAuth).mockReturnValue(authState());
    vi.mocked(meApi.updateProfile).mockRejectedValue(
      new ApiError(400, 'Validation failed', { bio: 'size must be between 0 and 500' }),
    );
    render(<ProfileDetails user={aUser()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Edit profile' }));
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('size must be between 0 and 500')).toBeInTheDocument();
  });
});
