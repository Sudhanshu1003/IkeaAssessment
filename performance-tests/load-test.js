import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
let errorRate = new Rate('errors');

export let options = {
  stages: [
    { duration: '2m', target: 10 }, // Ramp up to 10 users
    { duration: '5m', target: 10 }, // Stay at 10 users
    { duration: '2m', target: 50 }, // Ramp up to 50 users
    { duration: '5m', target: 50 }, // Stay at 50 users
    { duration: '2m', target: 0 },  // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.1'],    // Error rate should be below 10%
    errors: ['rate<0.1'],             // Custom error rate below 10%
  },
};

const BASE_URL = 'http://localhost:8080';

export function setup() {
  // Create test data
  let payload = JSON.stringify({
    businessUnitCode: 'PERF-TEST-001',
    location: 'AMSTERDAM-001',
    capacity: 30,
    stock: 25
  });

  let params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  let response = http.post(`${BASE_URL}/warehouse`, payload, params);
  check(response, {
    'setup warehouse created': (r) => r.status === 200,
  });

  return { warehouseId: response.json('businessUnitCode') };
}

export default function(data) {
  let params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Test GET /warehouse (list all)
  let response = http.get(`${BASE_URL}/warehouse`, params);
  let success = check(response, {
    'list warehouses status is 200': (r) => r.status === 200,
    'list warehouses response time < 500ms': (r) => r.timings.duration < 500,
  });

  errorRate.add(!success);

  // Test GET /warehouse/{id} (get by ID)
  if (data && data.warehouseId) {
    response = http.get(`${BASE_URL}/warehouse/${data.warehouseId}`, params);
    success = check(response, {
      'get warehouse status is 200': (r) => r.status === 200,
      'get warehouse response time < 300ms': (r) => r.timings.duration < 300,
    });
    errorRate.add(!success);
  }

  // Test GET /warehouse/search (search functionality)
  response = http.get(`${BASE_URL}/warehouse/search?location=AMSTERDAM-001`, params);
  success = check(response, {
    'search warehouses status is 200': (r) => r.status === 200,
    'search warehouses response time < 400ms': (r) => r.timings.duration < 400,
  });
  errorRate.add(!success);

  // Test POST /warehouse (create warehouse)
  let payload = JSON.stringify({
    businessUnitCode: `PERF-TEST-${Date.now()}`,
    location: 'ZWOLLE-001',
    capacity: 25,
    stock: 20
  });

  response = http.post(`${BASE_URL}/warehouse`, payload, params);
  success = check(response, {
    'create warehouse status is 200': (r) => r.status === 200,
    'create warehouse response time < 600ms': (r) => r.timings.duration < 600,
  });
  errorRate.add(!success);

  sleep(1);
}

export function teardown(data) {
  // Clean up test data
  if (data && data.warehouseId) {
    let params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    http.del(`${BASE_URL}/warehouse/${data.warehouseId}`, params);
  }
}
