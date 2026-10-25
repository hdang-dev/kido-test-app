import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { signup } from '../api/auth';
import { ApiError } from '../api/client';
import { AuthCard } from '../components/AuthCard';
import { FormField } from '../components/FormField';
import { useAuth } from '../context/AuthContext';
import { emailError, passwordError } from '../validation';

const EMPTY = { email: '', password: '', confirmPassword: '', username: '', bio: '' };
type Form = typeof EMPTY;

function validate(form: Form): Record<string, string> {
  const errors: Record<string, string> = {};
  const email = emailError(form.email);
  if (email) errors.email = email;
  const password = passwordError(form.password);
  if (password) errors.password = password;
  if (form.confirmPassword !== form.password) errors.confirmPassword = 'Passwords do not match';
  if (form.username.length > 50) errors.username = 'Username must be at most 50 characters';
  if (form.bio.length > 500) errors.bio = 'Bio must be at most 500 characters';
  return errors;
}

// Sign-up logs the user in (design.md §6): setUser → GuestRoute redirects to the profile.
export function SignupPage() {
  const { setUser } = useAuth();
  const [form, setForm] = useState<Form>(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const set = (field: keyof Form) => (value: string) => setForm((f) => ({ ...f, [field]: value }));
  const localPart = form.email.includes('@') ? form.email.slice(0, form.email.lastIndexOf('@')) : '';

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setFormError('');
    const clientErrors = validate(form);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) return;

    setSubmitting(true);
    try {
      setUser(
        await signup({
          email: form.email.trim(),
          password: form.password,
          confirmPassword: form.confirmPassword,
          username: form.username.trim() || undefined,
          bio: form.bio.trim() || undefined,
        }),
      );
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length > 0) setErrors(err.fieldErrors);
      else setFormError(err instanceof ApiError ? err.message : 'Something went wrong, please try again');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard
      title="Create an account"
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-blue-600 hover:underline">
            Log in
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <FormField label="Email" type="email" autoComplete="email" required value={form.email}
          error={errors.email} onChange={(e) => set('email')(e.target.value)} />
        <FormField label="Password" type="password" autoComplete="new-password" required value={form.password}
          error={errors.password} hint="8+ characters, with a letter and a digit"
          onChange={(e) => set('password')(e.target.value)} />
        <FormField label="Confirm password" type="password" autoComplete="new-password" required
          value={form.confirmPassword} error={errors.confirmPassword}
          onChange={(e) => set('confirmPassword')(e.target.value)} />
        <FormField label="Username (optional)" autoComplete="nickname" value={form.username}
          error={errors.username} placeholder={localPart || 'defaults to the part of your email before @'}
          onChange={(e) => set('username')(e.target.value)} />
        <FormField label="Bio (optional)" value={form.bio} error={errors.bio}
          onChange={(e) => set('bio')(e.target.value)} />
        {formError && (
          <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {formError}
          </p>
        )}
        <button type="submit" disabled={submitting}
          className="w-full rounded-md bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:opacity-50">
          {submitting ? 'Creating account…' : 'Sign up'}
        </button>
      </form>
    </AuthCard>
  );
}
