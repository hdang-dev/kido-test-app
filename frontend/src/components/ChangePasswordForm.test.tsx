import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { ApiError } from '../api/client';
import * as meApi from '../api/me';
import { ChangePasswordForm } from './ChangePasswordForm';

vi.mock('../api/me');

async function fill(current: string, next: string, confirm = next) {
  if (current) await userEvent.type(screen.getByLabelText('Current password'), current);
  if (next) await userEvent.type(screen.getByLabelText('New password'), next);
  if (confirm) await userEvent.type(screen.getByLabelText('Confirm new password'), confirm);
}
const submit = () => userEvent.click(screen.getByRole('button', { name: 'Change password' }));

describe('ChangePasswordForm', () => {
  beforeEach(() => {
    vi.mocked(meApi.changePassword).mockReset();
  });

  it('changes the password, confirms, and clears every field', async () => {
    vi.mocked(meApi.changePassword).mockResolvedValue(undefined);
    render(<ChangePasswordForm />);

    await fill('Passw0rd!', 'N3wPassword!');
    await submit();

    expect(meApi.changePassword).toHaveBeenCalledWith({
      currentPassword: 'Passw0rd!',
      newPassword: 'N3wPassword!',
      confirmPassword: 'N3wPassword!',
    });
    expect(await screen.findByRole('status')).toHaveTextContent('other devices have been signed out');
    for (const label of ['Current password', 'New password', 'Confirm new password']) {
      expect(screen.getByLabelText(label)).toHaveValue('');
    }
  });

  it('validates before calling the API', async () => {
    render(<ChangePasswordForm />);

    await fill('', 'short', 'different');
    await submit();

    expect(screen.getByText('Enter your current password')).toBeInTheDocument();
    expect(screen.getByText(/Password must be 8-72 characters/)).toBeInTheDocument();
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    expect(meApi.changePassword).not.toHaveBeenCalled();
  });

  it('rejects reusing the current password', async () => {
    render(<ChangePasswordForm />);

    await fill('Passw0rd!', 'Passw0rd!');
    await submit();

    expect(screen.getByText('New password must differ from the current one')).toBeInTheDocument();
    expect(meApi.changePassword).not.toHaveBeenCalled();
  });

  it('shows the server error for a wrong current password', async () => {
    vi.mocked(meApi.changePassword).mockRejectedValue(
      new ApiError(400, 'Current password is incorrect', { currentPassword: 'Current password is incorrect' }),
    );
    render(<ChangePasswordForm />);

    await fill('WrongPass1', 'N3wPassword!');
    await submit();

    expect(await screen.findByText('Current password is incorrect')).toBeInTheDocument();
    expect(screen.getByLabelText('Current password')).toHaveValue('');
  });
});
