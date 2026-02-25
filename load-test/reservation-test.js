import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend } from 'k6/metrics';
// 실행 명령어 -> k6 run --out influxdb=http://devhns3.labs-smart.com:8086/k6 -e TARGET_URL=https://devhns3.labs-smart.com:58080 .\reservation-test.js


// 🔥 커스텀 지표(Trend) 생성
const availabilityDuration = new Trend('req_duration_availability');
const createDuration = new Trend('req_duration_create');

// 1. 부하 테스트 시나리오 (Options)
export const options = {
  stages: [
    { duration: '30s', target: 100 },  // 30초 동안 가상 사용자(VU)를 1명에서 50명까지 서서히 증가 (Ramp-up)
    { duration: '1m', target: 80 },   // 1분 동안 50명의 VU 유지 (서버에 지속적인 부하 발생)
    { duration: '30s', target: 0 },   // 30초 동안 0명으로 서서히 감소 (Ramp-down)
  ],
  // 에러 비율이 10% 이상이거나, 95%의 요청이 2초 이상 걸리면 테스트 실패로 간주 (임계값 설정 예시)
  thresholds: {
    http_req_failed: ['rate<0.1'],
    http_req_duration: ['p(95)<1000', 'p(99)<1500'],
  },
  // 💡 핵심: 결과 요약표에 표시할 지표를 명시적으로 설정 (p(95), p(99) 포함)
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

// 환경 변수에서 URL을 가져오도록 설정 (값이 없으면 로컬값 사용)
const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

// 테스트에 사용할 샘플 데이터 (현장에 맞춰 수정 필요)
const TEST_USER = {
    loginId: '1111',
    password: '1111'
};

const TEST_CONFIG = {
    spaceId: 1, // 스터디룸A 공간
    capacity: 4, // 사용 인원 수
    ownerName: '부하테스트',
    ownerPhone: '010-1234-5678',
    paymentMethod: 'MANAGEMENT_FEE',
    targetDate: '2026-03-01' // 조회할 날짜
};

// 초기 설정 (각 VU가 실행되기 전 1번만 실행되어 토큰을 가져옴)
export function setup() {
    const loginUrl = `${BASE_URL}/api/member/login`;
    const payload = JSON.stringify({
        loginId: TEST_USER.loginId,
        password: TEST_USER.password,
    });
    const params = { headers: { 'Content-Type': 'application/json' } };

    const res = http.post(loginUrl, payload, params);
    const token = res.json('accessToken');

    if(!token) {
        console.error('Login Failed! Check TEST_USER credentials.');
    }
    return { token: token };
}

// 메인 시나리오 (예약 생성)
export default function (data) {
    if(!data.token) return;

    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'Content-Type': 'application/json',
        },
        responseCallback: http.expectedStatuses(200, 201, 400, 409),
    };

    // Step 1: 사용자가 특정 날짜의 예약 현황(불가능 시간대)을 조회
    group('1. Check Availability', function () {
        const availabilityUrl = `${BASE_URL}/api/reservation/availability?spaceId=${TEST_CONFIG.spaceId}&date=${TEST_CONFIG.targetDate}`;
        const availabilityRes = http.get(availabilityUrl, params);

        // 🔥 조회 요청에 걸린 시간을 커스텀 지표에 기록
        availabilityDuration.add(availabilityRes.timings.duration);

        check(availabilityRes, {
            'availability status is 200 or 201': (r) => r.status === 200 || r.status === 201,
        });
    });

    // 사용자가 화면을 보고 예약 시간을 선택하는 시간 (Think Time 시뮬레이션: 1~3초)
    sleep(Math.random() * 2 + 1);

    // Step 2: 예약 생성 요청
    group('2. Create Reservation', function () {
        const createUrl = `${BASE_URL}/api/reservation`;

        // 동시성 테스트를 위한 고정 시간 (실제 동시성 문제가 터지는지 확인하기 위함)
        const startTime = `${TEST_CONFIG.targetDate}T14:00:00`;
        const endTime = `${TEST_CONFIG.targetDate}T15:00:00`;

        const payload = JSON.stringify({
            spaceId: TEST_CONFIG.spaceId,
            startTime: startTime,
            endTime: endTime,
            capacity: TEST_CONFIG.capacity,
            ownerName: TEST_CONFIG.ownerName,
            ownerPhone: TEST_CONFIG.ownerPhone,
            paymentMethod: TEST_CONFIG.paymentMethod,
        });

        const createRes = http.post(createUrl, payload, params);

        // 🔥 생성 요청에 걸린 시간을 커스텀 지표에 기록
        createDuration.add(createRes.timings.duration);

        // GlobalException 적용 후 409 에러가 정상적으로 떨어지는지 확인
        check(createRes, {
            'create status is 200 or 201': (r) => r.status === 200 || r.status === 201,
            'create status is 400': (r) => r.status === 400, //잘못된 인원수, 시간 설정
            'create status is 409 (Conflict/Overlap)': (r) => r.status === 409, // 중복 예약 시 발생 예상
        });
    });
    // 실제 사용자처럼 행동하도록 각 요청 사이에 0.5초 ~ 1.5초 사이의 랜덤한 대기 시간 추가
    sleep(Math.random() + 0.5);
}