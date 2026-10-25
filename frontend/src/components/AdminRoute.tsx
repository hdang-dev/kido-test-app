import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { isAdmin } from '../context/roles';

// UX guard only — /api/admin/** is enforced server-side.
export function AdminRoute() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <p>Loading…</p>;
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />;
  if (!isAdmin(user.role)) return <Navigate to="/" replace />;
  return <Outlet />;
}
