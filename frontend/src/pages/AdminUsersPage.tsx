import { useCallback, useEffect, useState } from 'react';
import { changeRole, listUsers } from '../api/admin';
import { ApiError } from '../api/client';
import type { Page, UserSummary } from '../api/types';
import { RoleBadge } from '../components/ProfileDetails';
import { useAuth } from '../context/AuthContext';
import { roleActionFor } from '../context/roles';

const PAGE_SIZE = 20;

const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' });

// User management (design.md §5, §9): list users, grant / revoke admin. The backend enforces every rule;
// the buttons only reflect them.
export function AdminUsersPage() {
  const { user: me } = useAuth();
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<UserSummary> | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async (p: number) => {
    setError('');
    try {
      setData(await listUsers(p, PAGE_SIZE));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not load users');
    }
  }, []);

  useEffect(() => {
    void load(page);
  }, [load, page]);

  if (!me) return null;

  const onChangeRole = async (target: UserSummary, role: 'ADMIN' | 'USER') => {
    const verb = role === 'ADMIN' ? 'Grant admin rights to' : 'Remove admin rights from';
    if (!window.confirm(`${verb} ${target.email}? They will be signed out of all devices.`)) return;

    setBusyId(target.id);
    setError('');
    setNotice('');
    try {
      const updated = await changeRole(target.id, role);
      setData((d) => d && { ...d, content: d.content.map((u) => (u.id === updated.id ? updated : u)) });
      setNotice(`${updated.email} is now ${updated.role === 'ADMIN' ? 'an admin' : 'a regular user'}.`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not change the role');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-baseline justify-between">
        <h1 className="text-2xl font-semibold">User management</h1>
        {data && <span className="text-sm text-slate-500">{data.totalElements} users</span>}
      </div>

      {error && <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      {notice && <p role="status" className="rounded-md bg-green-50 px-3 py-2 text-sm text-green-800">{notice}</p>}

      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full whitespace-nowrap text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-slate-600">
            <tr>
              <th className="px-4 py-2 font-medium">Email</th>
              <th className="px-4 py-2 font-medium">Username</th>
              <th className="px-4 py-2 font-medium">Role</th>
              <th className="px-4 py-2 font-medium">Joined</th>
              <th className="px-4 py-2 font-medium"><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            {data?.content.map((u) => {
              const action = roleActionFor(me, u);
              return (
                <tr key={u.id} className="border-b border-slate-100 last:border-0">
                  <td className="px-4 py-2">{u.email}</td>
                  <td className="px-4 py-2">{u.username}</td>
                  <td className="px-4 py-2"><RoleBadge role={u.role} /></td>
                  <td className="px-4 py-2 text-slate-500">{dateFormat.format(new Date(u.createdAt))}</td>
                  <td className="px-4 py-2 text-right">
                    {action.kind === 'grant' && (
                      <button type="button" disabled={busyId === u.id} onClick={() => void onChangeRole(u, 'ADMIN')}
                        aria-label={`Make ${u.email} an admin`}
                        className="rounded-md bg-blue-600 px-3 py-1 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50">
                        Make admin
                      </button>
                    )}
                    {action.kind === 'revoke' && (
                      <button type="button" disabled={busyId === u.id} onClick={() => void onChangeRole(u, 'USER')}
                        aria-label={`Remove admin rights from ${u.email}`}
                        className="rounded-md border border-red-300 px-3 py-1 text-xs font-medium text-red-700 hover:bg-red-50 disabled:opacity-50">
                        Remove admin
                      </button>
                    )}
                    {action.kind === 'none' && <span className="text-xs text-slate-400">{action.reason}</span>}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {!data && !error && <p className="p-4 text-sm text-slate-500">Loading…</p>}
      </div>

      {data && data.totalPages > 1 && (
        <nav className="flex items-center justify-between text-sm" aria-label="Pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}
            className="rounded-md border border-slate-300 px-3 py-1.5 hover:bg-slate-50 disabled:opacity-50">
            Previous
          </button>
          <span className="text-slate-600">Page {data.page + 1} of {data.totalPages}</span>
          <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}
            className="rounded-md border border-slate-300 px-3 py-1.5 hover:bg-slate-50 disabled:opacity-50">
            Next
          </button>
        </nav>
      )}
    </div>
  );
}
