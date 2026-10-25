// Thin fetch wrapper: always sends the session cookie and, for mutating requests,
// echoes the XSRF-TOKEN cookie back as the X-XSRF-TOKEN header (design.md D3).

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public fieldErrors: Record<string, string> = {},
  ) {
    super(message);
  }
}

// Called when an authenticated call gets 401 (session expired / revoked). AuthContext registers it.
let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(`${name}=`))
    ?.split('=')[1];
}

// The backend issues XSRF-TOKEN on any response, but logout deletes it and nothing else may have
// been requested since — so fetch one first instead of sending a doomed, token-less POST.
async function csrfToken(): Promise<string | undefined> {
  if (!readCookie('XSRF-TOKEN')) {
    await fetch('/api/me', { credentials: 'include' }).catch(() => undefined);
  }
  const token = readCookie('XSRF-TOKEN');
  return token ? decodeURIComponent(token) : undefined;
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase();
  const headers = new Headers(options.headers);
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (method !== 'GET' && method !== 'HEAD') {
    const token = await csrfToken();
    if (token) headers.set('X-XSRF-TOKEN', token);
  }

  const res = await fetch(`/api${path}`, { ...options, method, headers, credentials: 'include' });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    // /auth/* 401s mean "wrong credentials", not "session expired"
    if (res.status === 401 && !path.startsWith('/auth/')) onUnauthorized?.();
    throw new ApiError(res.status, body.message ?? res.statusText, body.fieldErrors ?? {});
  }
  return res.status === 204 ? (undefined as T) : res.json();
}
