import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  duration: '1m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000'],
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
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  // Health check
  let r = http.get(`${BASE}/actuator/health`);
  check(r, { 'health OK': (r) => r.status === 200 });

  // Wallet
  r = http.get(`${BASE}/api/wallets/me`, { headers });
  check(r, { 'wallet OK': (r) => r.status === 200 });

  sleep(1);
}
