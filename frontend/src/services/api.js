const BASE = '/api';

function authHeaders() {
  const token = localStorage.getItem('accessToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request(method, path, body) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
  };
  if (body) opts.body = JSON.stringify(body);

  const res = await fetch(`${BASE}${path}`, opts);

  if (res.status === 401) {
    // Try refresh
    const refreshed = await tryRefresh();
    if (refreshed) {
      opts.headers = { 'Content-Type': 'application/json', ...authHeaders() };
      const retry = await fetch(`${BASE}${path}`, opts);
      if (!retry.ok) throw await parseError(retry);
      return retry.status === 204 ? null : retry.json();
    }
    localStorage.clear();
    window.location.href = '/login';
    throw new Error('Session expired');
  }

  if (!res.ok) throw await parseError(res);
  return res.status === 204 ? null : res.json();
}

async function tryRefresh() {
  const rt = localStorage.getItem('refreshToken');
  if (!rt) return false;
  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rt }),
    });
    if (!res.ok) return false;
    const data = await res.json();
    localStorage.setItem('accessToken', data.accessToken);
    if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);
    return true;
  } catch {
    return false;
  }
}

async function parseError(res) {
  try {
    const data = await res.json();
    return new Error(data.message || data.error || 'Request failed');
  } catch {
    return new Error(`Request failed (${res.status})`);
  }
}

export const auth = {
  signup: (data) => request('POST', '/auth/signup', data),
  login: async (data) => {
    const res = await request('POST', '/auth/login', data);
    localStorage.setItem('accessToken', res.accessToken);
    if (res.refreshToken) localStorage.setItem('refreshToken', res.refreshToken);
    return res;
  },
  logout: () => {
    localStorage.clear();
    return Promise.resolve();
  },
};

export const users = {
  me: () => request('GET', '/users/me'),
  update: (data) => request('PUT', '/users/me', data),
};

export const transactions = {
  list: (params = '') => request('GET', `/transactions${params ? '?' + params : ''}`),
  get: (id) => request('GET', `/transactions/${id}`),
  create: (data) => request('POST', '/transactions', data),
  update: (id, data) => request('PUT', `/transactions/${id}`, data),
  delete: (id) => request('DELETE', `/transactions/${id}`),
  stats: (params = '') => request('GET', `/transactions/stats${params ? '?' + params : ''}`),
  statsByCategory: () => request('GET', '/transactions/stats/by-category'),
  summary: (days = 30) => request('GET', `/transactions/summary?days=${days}`),
  fraud: () => request('GET', '/transactions/fraud'),
};
