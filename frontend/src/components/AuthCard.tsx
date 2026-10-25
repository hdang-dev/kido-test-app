import type { ReactNode } from 'react';

// Centered card shared by the login and sign-up pages.
export function AuthCard({ title, children, footer }: { title: string; children: ReactNode; footer: ReactNode }) {
  return (
    <div className="mx-auto mt-16 max-w-sm space-y-6 rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
      <h1 className="text-2xl font-semibold text-slate-900">{title}</h1>
      {children}
      <p className="text-center text-sm text-slate-600">{footer}</p>
    </div>
  );
}
