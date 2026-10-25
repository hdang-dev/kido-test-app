// Thin fetch wrapper: always sends the session cookie and, for mutating requests,
// echoes the XSRF-TOKEN cookie back as the X-XSRF-TOKEN header (Spring Security SPA CSRF pattern).

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public fieldErrors: Record<string, string> = {},
  ) {
    super(message);
  }
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(`${name}=`))
    ?.split('=')[1];
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase();
  const headers = new Headers(options.headers);
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (method !== 'GET' && method !== 'HEAD') {
    const token = readCookie('XSRF-TOKEN');
    if (token) headers.set('X-XSRF-TOKEN', decodeURIComponent(token));
  }

  const res = await fetch(`/api${path}`, { ...options, method, headers, credentials: 'include' });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError(res.status, body.message ?? res.statusText, body.fieldErrors ?? {});
  }
  return res.status === 204 ? (undefined as T) : res.json();
}
