import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8081";
const QUEUE_ID = __ENV.QUEUE_ID || "product:100";
const START_USER_ID = Number(__ENV.START_USER_ID || 100000);
const SCENARIO = __ENV.SCENARIO || "smoke";

const STAGE_MAP = {
  smoke: [
    { duration: "30s", target: 5 },
    { duration: "60s", target: 5 },
    { duration: "15s", target: 0 },
  ],
  load: [
    { duration: "1m", target: 20 },
    { duration: "3m", target: 20 },
    { duration: "1m", target: 0 },
  ],
  stress: [
    { duration: "1m", target: 30 },
    { duration: "2m", target: 60 },
    { duration: "2m", target: 90 },
    { duration: "1m", target: 0 },
  ],
  spike: [
    { duration: "20s", target: 10 },
    { duration: "20s", target: 100 },
    { duration: "60s", target: 100 },
    { duration: "20s", target: 10 },
    { duration: "20s", target: 0 },
  ],
};

const stages = STAGE_MAP[SCENARIO] || STAGE_MAP.smoke;

export const options = {
  stages,
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500", "p(99)<1000"],
    checks: ["rate>0.99"],
  },
  tags: {
    app: "queue-service",
    test_type: SCENARIO,
  },
};

export default function () {
  const userId = START_USER_ID + (__VU * 100000) + __ITER;

  const payload = JSON.stringify({
    queueId: QUEUE_ID,
    userId,
  });

  const response = http.post(
    `${BASE_URL}/api/v1/queues/enter`,
    payload,
    {
      headers: {
        "Content-Type": "application/json",
      },
      tags: {
        endpoint: "POST /api/v1/queues/enter",
      },
    }
  );

  check(response, {
    "status is 200": (r) => r.status === 200,
    "has token": (r) => {
      try {
        const body = JSON.parse(r.body);
        return Boolean(body.token);
      } catch (_) {
        return false;
      }
    },
  });

  sleep(0.2);
}
