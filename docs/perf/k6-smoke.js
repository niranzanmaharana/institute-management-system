/**
 * Step 7.3 performance smoke — login, outstanding list, student search.
 *
 * Prefer BASE_URL=http://host.docker.internal:8080 (API) for baseline numbers.
 * Gateway (:8088) login rate-limit (20/min/IP) will fail a concurrent login smoke.
 *
 *   .\scripts\perf\run-k6-baseline.ps1
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const username = __ENV.USERNAME || 'admin-a';
const password = __ENV.PASSWORD || 'Password@123';
const institute = __ENV.INSTITUTE || 'DEMO_A';

const loginTrend = new Trend('ims_login_ms', true);
const outstandingTrend = new Trend('ims_outstanding_ms', true);
const studentSearchTrend = new Trend('ims_student_search_ms', true);
const errorRate = new Rate('ims_errors');

export const options = {
  scenarios: {
    login: {
      executor: 'constant-arrival-rate',
      rate: 2,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 2,
      maxVUs: 4,
      exec: 'loginOnly',
      startTime: '0s',
      tags: { endpoint: 'login' },
    },
    reads: {
      executor: 'constant-vus',
      vus: 8,
      duration: '45s',
      exec: 'authenticatedReads',
      startTime: '2s',
      tags: { endpoint: 'reads' },
    },
  },
  thresholds: {
    ims_login_ms: ['p(95)<800'],
    ims_outstanding_ms: ['p(95)<500'],
    ims_student_search_ms: ['p(95)<500'],
    ims_errors: ['rate<0.05'],
    http_req_failed: ['rate<0.05'],
  },
};

function doLogin() {
  const res = http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({
      username,
      password,
      instituteCode: institute,
    }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'POST /auth/login' } },
  );
  loginTrend.add(res.timings.duration);
  const ok = check(res, {
    'login status 200': (r) => r.status === 200,
    'login has token': (r) => {
      try {
        return !!r.json('accessToken');
      } catch (e) {
        return false;
      }
    },
  });
  errorRate.add(!ok);
  if (!ok) {
    return null;
  }
  return res.json('accessToken');
}

export function setup() {
  const token = doLogin();
  if (!token) {
    throw new Error(`setup login failed against ${baseUrl}`);
  }
  return { token };
}

export function loginOnly() {
  doLogin();
}

export function authenticatedReads(data) {
  const headers = {
    Authorization: `Bearer ${data.token}`,
    'Content-Type': 'application/json',
  };

  const outstanding = http.get(`${baseUrl}/api/v1/outstanding`, {
    headers,
    tags: { name: 'GET /outstanding' },
  });
  outstandingTrend.add(outstanding.timings.duration);
  errorRate.add(
    !check(outstanding, {
      'outstanding 200': (r) => r.status === 200,
    }),
  );

  const search = http.get(`${baseUrl}/api/v1/students?q=a&status=ACTIVE&page=0&size=20`, {
    headers,
    tags: { name: 'GET /students?q=' },
  });
  studentSearchTrend.add(search.timings.duration);
  errorRate.add(
    !check(search, {
      'student search 200': (r) => r.status === 200,
    }),
  );

  sleep(0.15);
}
