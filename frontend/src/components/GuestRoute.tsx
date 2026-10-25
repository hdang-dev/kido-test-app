import { Navigate, Outlet, useLocation, type Location } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// /login and /signup: once authenticated, go back to the page that sent us here (set by
// ProtectedRoute/AdminRoute), else the profile. Pages don't navigate themselves after login.
export function GuestRoute() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <p>Loading…</p>;
  if (user) {
    const from = (location.state as { from?: Location } | null)?.from;
    return <Navigate to={from ? `${from.pathname}${from.search}` : '/'} replace />;
  }
  return <Outlet />;
}
