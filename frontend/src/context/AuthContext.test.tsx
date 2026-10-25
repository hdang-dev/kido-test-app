import { act, render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import * as authApi from '../api/auth';
import { ApiError } from '../api/client';
import { aUser } from '../test-utils';
import { AuthProvider, useAuth } from './AuthContext';

vi.mock('../api/auth');

let auth: ReturnType<typeof useAuth>;
function Probe() {
  auth = useAuth();
  return <p>{auth.loading ? 'loading' : (auth.user?.email ?? 'anonymous')}</p>;
}

const renderProvider = () =>
  render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  );

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.mocked(authApi.fetchMe).mockReset();
    vi.mocked(authApi.logout).mockReset();
  });

  it('loads the current user on start', async () => {
    vi.mocked(authApi.fetchMe).mockResolvedValue(aUser());
    renderProvider();
    expect(await screen.findByText('neo@test.local')).toBeInTheDocument();
  });

  it('treats a 401 on start as anonymous', async () => {
    vi.mocked(authApi.fetchMe).mockRejectedValue(new ApiError(401, 'Authentication required'));
    renderProvider();
    expect(await screen.findByText('anonymous')).toBeInTheDocument();
  });

  it('survives a server error on start without an unhandled rejection', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    vi.mocked(authApi.fetchMe).mockRejectedValue(new ApiError(502, 'Bad Gateway'));
    renderProvider();
    expect(await screen.findByText('anonymous')).toBeInTheDocument();
  });

  it('keeps the user when logout fails and the session is still alive', async () => {
    vi.mocked(authApi.fetchMe).mockResolvedValue(aUser());
    vi.mocked(authApi.logout).mockRejectedValue(new ApiError(503, 'Service Unavailable'));
    renderProvider();
    await screen.findByText('neo@test.local');

    await act(async () => {
      await expect(auth.logout()).rejects.toBeInstanceOf(ApiError);
    });

    expect(screen.getByText('neo@test.local')).toBeInTheDocument();
  });

  it('clears the user after a successful logout', async () => {
    vi.mocked(authApi.fetchMe).mockResolvedValue(aUser());
    vi.mocked(authApi.logout).mockResolvedValue(undefined);
    renderProvider();
    await screen.findByText('neo@test.local');

    await act(() => auth.logout());

    await waitFor(() => expect(screen.getByText('anonymous')).toBeInTheDocument());
  });
});
