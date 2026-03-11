import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20  },
    { duration: '1m',  target: 100 },
    { duration: '30s', target: 0   },
  ],
  thresholds: {
    http_req_failed:   ['rate<0.10'],
    http_req_duration: ['p(95)<5000'],
  },
};

const BASE = 'https://payflow.intellenz.com:8443';

export function setup() {
  const res = http.post(`${BASE}/api/auth/login`, JSON.stringify({
    email: 'admin@payflow.com',
    password: 'Admin2025!'
  }), { headers: { 'Content-Type': 'application/json' } });
  return { token: res.json('accessToken') };
}

export default function (data) {
  const headers = {
    'Authorization': `Bearer ${data.token}`,
  };

  const r = http.get(`${BASE}/api/wallets/me`, { headers });
  check(r, { 'status 200': (r) => r.status === 200 });
  sleep(0.5);
}
