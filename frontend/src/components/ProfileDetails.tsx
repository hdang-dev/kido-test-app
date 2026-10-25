import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { updateProfile } from '../api/me';
import type { MeResponse, Role } from '../api/types';
import { useAuth } from '../context/AuthContext';
import { FormField } from './FormField';
import { TextAreaField } from './TextAreaField';

const ROLE_LABEL: Record<Role, string> = { USER: 'User', ADMIN: 'Admin', SUPER_ADMIN: 'Super admin' };

export function RoleBadge({ role }: { role: Role }) {
  const colour = role === 'USER' ? 'bg-slate-100 text-slate-700' : 'bg-amber-100 text-amber-800';
  return <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${colour}`}>{ROLE_LABEL[role]}</span>;
}

// Email (read-only), username and bio, with an inline edit mode (design.md §6 "Profile").
export function ProfileDetails({ user }: { user: MeResponse }) {
  const { setUser } = useAuth();
  const [editing, setEditing] = useState(false);
  const [username, setUsername] = useState(user.username);
  const [bio, setBio] = useState(user.bio ?? '');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);

  const startEditing = () => {
    setUsername(user.username);
    setBio(user.bio ?? '');
    setErrors({});
    setMessage('');
    setEditing(true);
  };

  const onSave = async (e: FormEvent) => {
    e.preventDefault();
    const clientErrors: Record<string, string> = {};
    if (username.trim().length > 50) clientErrors.username = 'Username must be at most 50 characters';
    if (bio.trim().length > 500) clientErrors.bio = 'Bio must be at most 500 characters';
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) return;

    setSaving(true);
    try {
      setUser(await updateProfile({ username: username.trim(), bio: bio.trim() }));
      setEditing(false);
      setMessage('Profile updated');
    } catch (err) {
      setErrors(err instanceof ApiError && Object.keys(err.fieldErrors).length > 0
        ? err.fieldErrors
        : { _: err instanceof ApiError ? err.message : 'Could not save, please try again' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Account</h2>
        {!editing && (
          <button type="button" onClick={startEditing}
            className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium hover:bg-slate-50">
            Edit profile
          </button>
        )}
      </div>

      {message && <p role="status" className="text-sm text-green-700">{message}</p>}

      {!editing ? (
        <dl className="grid grid-cols-[8rem_1fr] gap-y-3 text-sm">
          <dt className="text-slate-500">Email</dt>
          <dd>{user.email}</dd>
          <dt className="text-slate-500">Username</dt>
          <dd>{user.username}</dd>
          <dt className="text-slate-500">Bio</dt>
          <dd className="whitespace-pre-line">{user.bio || <span className="text-slate-400">No bio yet</span>}</dd>
          <dt className="text-slate-500">Role</dt>
          <dd><RoleBadge role={user.role} /></dd>
        </dl>
      ) : (
        <form onSubmit={onSave} className="space-y-4" noValidate>
          <FormField label="Email" value={user.email} disabled readOnly hint="Email cannot be changed" />
          <FormField label="Username" value={username} error={errors.username} maxLength={50}
            hint="Leave empty to use the part of your email before @"
            onChange={(e) => setUsername(e.target.value)} />
          <TextAreaField label="Bio" value={bio} error={errors.bio} hint={`${bio.trim().length}/500`}
            onChange={(e) => setBio(e.target.value)} />
          {errors._ && <p role="alert" className="text-sm text-red-600">{errors._}</p>}
          <div className="flex gap-2">
            <button type="submit" disabled={saving}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
              {saving ? 'Saving…' : 'Save'}
            </button>
            <button type="button" onClick={() => setEditing(false)}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium hover:bg-slate-50">
              Cancel
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
