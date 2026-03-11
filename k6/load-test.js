import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const transferDuration = new Trend('transfer_duration');

export const options = {
  stages: [
    { duration: '1m', target: 10 },   // ramp up
    { duration: '3m', target: 50 },   // steady load
    { duration: '1m', target: 0  },   // ramp down
  ],
  thresholds: {
    http_req_failed:   ['rate<0.05'],
    http_req_duration: ['p(95)<3000'],
    errors:            ['rate<0.05'],
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

  // Health
  let r = http.get(`${BASE}/actuator/health`);
  errorRate.add(r.status !== 200);
  check(r, { 'health 200': (r) => r.status === 200 });

  // Wallet balance
  r = http.get(`${BASE}/api/wallets/me`, { headers });
  errorRate.add(r.status !== 200);
  check(r, { 'wallet 200': (r) => r.status === 200 });

  sleep(1);
}
