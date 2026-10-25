import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { isAdmin } from '../context/roles';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-1.5 text-sm font-medium ${isActive ? 'bg-slate-900 text-white' : 'text-slate-700 hover:bg-slate-100'}`;

// Shell for signed-in pages: nav (admin link only for admins — UX only) + logout.
export function AppLayout() {
  const { user, logout } = useAuth();

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
          <button type="button" onClick={() => void logout()}
            className="rounded-md px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100">
            Log out
          </button>
        </nav>
      </header>
      <main className="mx-auto max-w-3xl p-6">
        <Outlet />
      </main>
    </div>
  );
}
