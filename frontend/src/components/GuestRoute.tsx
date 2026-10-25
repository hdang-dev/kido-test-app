import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// /login and /signup: already-authenticated users go straight to the profile.
export function GuestRoute() {
  const { user, loading } = useAuth();

  if (loading) return <p>Loading…</p>;
  if (user) return <Navigate to="/" replace />;
  return <Outlet />;
}
