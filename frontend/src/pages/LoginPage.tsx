import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../api/client';
import { AuthCard } from '../components/AuthCard';
import { FormField } from '../components/FormField';
import { useAuth } from '../context/AuthContext';

// After a successful login, GuestRoute redirects to state.from (or /) — no navigate() here.
export function LoginPage() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await login(email, password, rememberMe);
    } catch (err) {
      setPassword('');
      setError(err instanceof ApiError && err.status === 401 ? err.message : 'Something went wrong, please try again');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard
      title="Log in"
      footer={
        <>
          No account?{' '}
          <Link to="/signup" className="font-medium text-blue-600 hover:underline">
            Sign up
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <FormField label="Email" type="email" autoComplete="email" required value={email}
          onChange={(e) => setEmail(e.target.value)} />
        <FormField label="Password" type="password" autoComplete="current-password" required value={password}
          onChange={(e) => setPassword(e.target.value)} />
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input type="checkbox" checked={rememberMe} onChange={(e) => setRememberMe(e.target.checked)} />
          Remember me for 30 days
        </label>
        {error && (
          <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}
        <button type="submit" disabled={submitting}
          className="w-full rounded-md bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:opacity-50">
          {submitting ? 'Logging in…' : 'Log in'}
        </button>
      </form>
    </AuthCard>
  );
}
