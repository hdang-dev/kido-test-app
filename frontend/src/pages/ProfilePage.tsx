import { ChangePasswordForm } from '../components/ChangePasswordForm';
import { ProfileDetails } from '../components/ProfileDetails';
import { useAuth } from '../context/AuthContext';

// Home page after login (design.md §9): account details + change password.
export function ProfilePage() {
  const { user } = useAuth();
  if (!user) return null; // ProtectedRoute guarantees a user; keeps the types honest

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-semibold">Hi, {user.username}</h1>
      <ProfileDetails user={user} />
      <ChangePasswordForm />
    </div>
  );
}
