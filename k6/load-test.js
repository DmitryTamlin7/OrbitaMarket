import http from 'k6/http';
import { check, sleep } from 'k6';
import { jUnit, textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

export const options = {
    stages: [
        { duration: '10s', target: 10 },
        { duration: '30s', target: 10 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<200'],
    },
};

export default function () {
    const url = 'http://localhost:8080/api/v1/orders';

    const payload = JSON.stringify({
        product_type: "TASKING",
        price: 10,
        payload: {
            aoi: "polygon_coordinates",
            sensor_type: "SAR",
            time_window: {
                start: "2026-06-29T19:30:00",
                end: "2026-06-29T21:30:00"
            }
        }
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-User-Id': `user-local-${__VU}`,
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
        'response has id': (r) => {
            try {
                return JSON.parse(r.body).id !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    sleep(1);
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        '../allure-results/k6-load-results.xml': jUnit(data),
    };
}