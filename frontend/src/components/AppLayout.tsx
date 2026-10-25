import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { isAdmin } from '../context/roles';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-1.5 text-sm font-medium ${isActive ? 'bg-slate-900 text-white' : 'text-slate-700 hover:bg-slate-100'}`;

// Shell for signed-in pages: nav (admin link only for admins — UX only) + logout.
export function AppLayout() {
  const { user, logout } = useAuth();
  const [logoutError, setLogoutError] = useState('');

  const onLogout = async () => {
    setLogoutError('');
    try {
      await logout();
    } catch {
      setLogoutError('Logout failed. Please try again.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-3xl items-center gap-2 px-6 py-3">
          <span className="mr-4 font-semibold text-slate-900">Kido Auth</span>
          <NavLink to="/" end className={linkClass}>
            Profile
          </NavLink>
          {user && isAdmin(user.role) && (
            <NavLink to="/admin/users" className={linkClass}>
              Users
            </NavLink>
          )}
          <span className="ml-auto hidden text-sm text-slate-500 sm:inline">{user?.email}</span>
          <button type="button" onClick={() => void onLogout()}
            className="rounded-md px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100">
            Log out
          </button>
        </nav>
      </header>
      {logoutError && (
        <p role="alert" className="mx-auto mt-4 max-w-3xl rounded-md bg-red-50 px-6 py-2 text-sm text-red-700">
          {logoutError}
        </p>
      )}
      <main className="mx-auto max-w-3xl p-6">
        <Outlet />
      </main>
    </div>
  );
}
