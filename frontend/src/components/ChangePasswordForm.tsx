import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { changePassword } from '../api/me';
import { passwordError } from '../validation';
import { FormField } from './FormField';

const EMPTY = { currentPassword: '', newPassword: '', confirmPassword: '' };

// Self-service password change (design.md §6). Fields are cleared after every submit attempt that
// reaches the server, so passwords don't linger in the DOM.
export function ChangePasswordForm() {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const set = (field: keyof typeof EMPTY) => (value: string) => setForm((f) => ({ ...f, [field]: value }));

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setMessage('');
    const clientErrors: Record<string, string> = {};
    if (!form.currentPassword) clientErrors.currentPassword = 'Enter your current password';
    const policy = passwordError(form.newPassword);
    if (policy) clientErrors.newPassword = policy;
    else if (form.newPassword === form.currentPassword) clientErrors.newPassword = 'New password must differ from the current one';
    if (form.confirmPassword !== form.newPassword) clientErrors.confirmPassword = 'Passwords do not match';
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) return;

    setSubmitting(true);
    try {
      await changePassword(form);
      setMessage('Password changed. Your other devices have been signed out.');
    } catch (err) {
      setErrors(err instanceof ApiError && Object.keys(err.fieldErrors).length > 0
        ? err.fieldErrors
        : { _: err instanceof ApiError ? err.message : 'Could not change the password, please try again' });
    } finally {
      setForm(EMPTY);
      setSubmitting(false);
    }
  };

  return (
    <section className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold">Change password</h2>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <FormField label="Current password" type="password" autoComplete="current-password"
          value={form.currentPassword} error={errors.currentPassword}
          onChange={(e) => set('currentPassword')(e.target.value)} />
        <FormField label="New password" type="password" autoComplete="new-password"
          value={form.newPassword} error={errors.newPassword} hint="8+ characters, with a letter and a digit"
          onChange={(e) => set('newPassword')(e.target.value)} />
        <FormField label="Confirm new password" type="password" autoComplete="new-password"
          value={form.confirmPassword} error={errors.confirmPassword}
          onChange={(e) => set('confirmPassword')(e.target.value)} />
        {errors._ && <p role="alert" className="text-sm text-red-600">{errors._}</p>}
        {message && <p role="status" className="text-sm text-green-700">{message}</p>}
        <button type="submit" disabled={submitting}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
          {submitting ? 'Changing…' : 'Change password'}
        </button>
      </form>
    </section>
  );
}
