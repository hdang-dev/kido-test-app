import { Navigate, Route, Routes } from 'react-router-dom';
import { AdminRoute } from './components/AdminRoute';
import { GuestRoute } from './components/GuestRoute';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AdminUsersPage } from './pages/AdminUsersPage';
import { LoginPage } from './pages/LoginPage';
import { ProfilePage } from './pages/ProfilePage';
import { SignupPage } from './pages/SignupPage';

// Routes per docs/design.md §9
export function App() {
  return (
    <main className="mx-auto max-w-3xl p-6">
      <Routes>
        <Route element={<GuestRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
        </Route>
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<ProfilePage />} />
        </Route>
        <Route element={<AdminRoute />}>
          <Route path="/admin/users" element={<AdminUsersPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </main>
  );
}
