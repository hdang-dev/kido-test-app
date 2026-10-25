import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// UX guard only — /api/admin/** is enforced server-side.
export function AdminRoute() {
  const { user, loading } = useAuth();

  if (loading) return <p>Loading…</p>;
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== 'ADMIN') return <Navigate to="/profile" replace />;
  return <Outlet />;
}
