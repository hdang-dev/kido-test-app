import { vi } from 'vitest';
import { api, ApiError, setUnauthorizedHandler } from './client';

const json = (status: number, body: unknown = {}) =>
  new Response(status === 204 ? null : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

function clearCookies() {
  document.cookie.split('; ').filter(Boolean).forEach((c) => {
    document.cookie = `${c.split('=')[0]}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
  });
}

describe('api client', () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    clearCookies();
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    setUnauthorizedHandler(null);
  });

  it('echoes the XSRF-TOKEN cookie as a header on mutating requests', async () => {
    document.cookie = 'XSRF-TOKEN=abc%3D';
    fetchMock.mockResolvedValue(json(204));

    await api('/auth/logout', { method: 'POST' });

    const [, init] = fetchMock.mock.calls[0];
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('abc=');
    expect(init.credentials).toBe('include');
  });

  it('fetches a CSRF token first when the cookie is missing (e.g. right after logout)', async () => {
    fetchMock.mockImplementation(async (url: string) => {
      if (url === '/api/me') {
        document.cookie = 'XSRF-TOKEN=fresh'; // what the backend's Set-Cookie does
        return json(401);
      }
      return json(200, { id: '1' });
    });

    await api('/auth/login', { method: 'POST', body: '{}' });

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual(['/api/me', '/api/auth/login']);
    expect(new Headers(fetchMock.mock.calls[1][1].headers).get('X-XSRF-TOKEN')).toBe('fresh');
  });

  it('maps error bodies to ApiError with field errors', async () => {
    fetchMock.mockResolvedValue(json(409, { message: 'Email is already registered', fieldErrors: { email: 'taken' } }));

    await expect(api('/auth/signup', { method: 'POST', body: '{}' })).rejects.toMatchObject({
      status: 409,
      message: 'Email is already registered',
      fieldErrors: { email: 'taken' },
    });
  });

  it('reports session expiry on 401 from normal endpoints, not from /auth/*', async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    fetchMock.mockResolvedValue(json(401, { message: 'Authentication required' }));

    await expect(api('/me')).rejects.toBeInstanceOf(ApiError);
    expect(onUnauthorized).toHaveBeenCalledTimes(1);

    document.cookie = 'XSRF-TOKEN=t';
    await expect(api('/auth/login', { method: 'POST', body: '{}' })).rejects.toBeInstanceOf(ApiError);
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });
});
