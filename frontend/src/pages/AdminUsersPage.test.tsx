import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import * as adminApi from '../api/admin';
import { ApiError } from '../api/client';
import type { Page, UserSummary } from '../api/types';
import { useAuth } from '../context/AuthContext';
import { aUser, authState } from '../test-utils';
import { AdminUsersPage } from './AdminUsersPage';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));
vi.mock('../api/admin');

const row = (id: string, email: string, role: UserSummary['role']): UserSummary => ({
  id, email, username: email.split('@')[0], role, createdAt: '2026-10-25T08:00:00Z',
});

const pageOf = (content: UserSummary[], totalPages = 1, page = 0): Page<UserSummary> => ({
  content, page, size: 20, totalElements: content.length, totalPages,
});

const ME = aUser('ADMIN', { id: 'me', email: 'me@test.local' });
const USERS = [
  row('me', 'me@test.local', 'ADMIN'),
  row('root', 'root@test.local', 'SUPER_ADMIN'),
  row('u1', 'neo@test.local', 'USER'),
  row('a1', 'trinity@test.local', 'ADMIN'),
];

const rowFor = (email: string) => screen.getByText(email).closest('tr')!;

describe('AdminUsersPage', () => {
  beforeEach(() => {
    vi.mocked(adminApi.listUsers).mockReset();
    vi.mocked(adminApi.changeRole).mockReset();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });

  it('lists users with the actions an ADMIN is allowed', async () => {
    vi.mocked(useAuth).mockReturnValue(authState(ME));
    vi.mocked(adminApi.listUsers).mockResolvedValue(pageOf(USERS));
    render(<AdminUsersPage />);

    await screen.findByText('neo@test.local');
    expect(within(rowFor('me@test.local')).getByText('You')).toBeInTheDocument();
    expect(within(rowFor('root@test.local')).getByText('Protected')).toBeInTheDocument();
    expect(within(rowFor('neo@test.local')).getByRole('button', { name: /make .* an admin/i })).toBeInTheDocument();
    expect(within(rowFor('trinity@test.local')).queryByRole('button')).not.toBeInTheDocument();
  });

  it('shows "Remove admin" only to the SUPER_ADMIN', async () => {
    vi.mocked(useAuth).mockReturnValue(authState(aUser('SUPER_ADMIN', { id: 'root', email: 'root@test.local' })));
    vi.mocked(adminApi.listUsers).mockResolvedValue(pageOf(USERS));
    render(<AdminUsersPage />);

    await screen.findByText('trinity@test.local');
    expect(within(rowFor('trinity@test.local')).getByRole('button', { name: /remove admin/i })).toBeInTheDocument();
  });

  it('grants admin after confirmation and updates the row', async () => {
    vi.mocked(useAuth).mockReturnValue(authState(ME));
    vi.mocked(adminApi.listUsers).mockResolvedValue(pageOf(USERS));
    vi.mocked(adminApi.changeRole).mockResolvedValue(row('u1', 'neo@test.local', 'ADMIN'));
    render(<AdminUsersPage />);

    await userEvent.click(await screen.findByRole('button', { name: /make neo@test.local an admin/i }));

    expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining('signed out of all devices'));
    expect(adminApi.changeRole).toHaveBeenCalledWith('u1', 'ADMIN');
    expect(await screen.findByRole('status')).toHaveTextContent('neo@test.local is now an admin');
    expect(within(rowFor('neo@test.local')).getByText('Admin')).toBeInTheDocument();
  });

  it('does nothing when the confirmation is cancelled', async () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    vi.mocked(useAuth).mockReturnValue(authState(ME));
    vi.mocked(adminApi.listUsers).mockResolvedValue(pageOf(USERS));
    render(<AdminUsersPage />);

    await userEvent.click(await screen.findByRole('button', { name: /make neo@test.local an admin/i }));

    expect(adminApi.changeRole).not.toHaveBeenCalled();
  });

  it('shows the server message when a change is refused', async () => {
    vi.mocked(useAuth).mockReturnValue(authState(ME));
    vi.mocked(adminApi.listUsers).mockResolvedValue(pageOf(USERS));
    vi.mocked(adminApi.changeRole).mockRejectedValue(new ApiError(403, 'Only the super admin can revoke admin rights'));
    render(<AdminUsersPage />);

    await userEvent.click(await screen.findByRole('button', { name: /make neo@test.local an admin/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Only the super admin can revoke admin rights');
  });

  it('pages through the list', async () => {
    vi.mocked(useAuth).mockReturnValue(authState(ME));
    vi.mocked(adminApi.listUsers)
      .mockResolvedValueOnce(pageOf([USERS[2]], 2, 0))
      .mockResolvedValueOnce(pageOf([USERS[3]], 2, 1));
    render(<AdminUsersPage />);

    await screen.findByText('Page 1 of 2');
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    expect(await screen.findByText('trinity@test.local')).toBeInTheDocument();
    expect(adminApi.listUsers).toHaveBeenLastCalledWith(1, 20);
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('keeps label and buttons on the shown page when loading the next page fails', async () => {
    vi.mocked(useAuth).mockReturnValue(authState(ME));
    vi.mocked(adminApi.listUsers)
      .mockResolvedValueOnce(pageOf([USERS[2]], 3, 0))
      .mockRejectedValueOnce(new ApiError(503, 'Service Unavailable'))
      .mockResolvedValueOnce(pageOf([USERS[3]], 3, 1));
    render(<AdminUsersPage />);

    await screen.findByText('Page 1 of 3');
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Service Unavailable');
    expect(screen.getByText('Page 1 of 3')).toBeInTheDocument(); // still describes what is shown

    await userEvent.click(screen.getByRole('button', { name: 'Next' })); // retries page 2, doesn't skip it
    expect(await screen.findByText('trinity@test.local')).toBeInTheDocument();
    expect(adminApi.listUsers).toHaveBeenLastCalledWith(1, 20);
  });

  it('ignores a slow response for a page the user already left', async () => {
    vi.mocked(useAuth).mockReturnValue(authState(ME));
    let resolveSlow!: (p: Page<UserSummary>) => void;
    vi.mocked(adminApi.listUsers)
      .mockResolvedValueOnce(pageOf([USERS[2]], 3, 0))
      .mockImplementationOnce(() => new Promise((resolve) => (resolveSlow = resolve)))
      .mockResolvedValueOnce(pageOf([row('z', 'zion@test.local', 'USER')], 3, 0));
    render(<AdminUsersPage />);

    await screen.findByText('Page 1 of 3');
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));   // slow request for page 2
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));   // newer request, answered first
    expect(await screen.findByText('zion@test.local')).toBeInTheDocument();

    resolveSlow(pageOf([row('old', 'stale@test.local', 'USER')], 3, 1));
    await new Promise((r) => setTimeout(r, 0));
    expect(screen.queryByText('stale@test.local')).not.toBeInTheDocument();
  });
});
