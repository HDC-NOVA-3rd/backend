import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 부하 테스트 시나리오 (Options)
export const options = {
  stages: [
    { duration: '30s', target: 50 },  // 30초 동안 가상 사용자(VU)를 1명에서 50명까지 서서히 증가 (Ramp-up)
    { duration: '1m', target: 50 },   // 1분 동안 50명의 VU 유지 (서버에 지속적인 부하 발생)
    { duration: '30s', target: 0 },   // 30초 동안 0명으로 서서히 감소 (Ramp-down)
  ],
};

// 환경 변수에서 URL을 가져오도록 설정 (값이 없으면 로컬값 사용)
const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

// 2. 가상 사용자가 반복해서 실행할 행동
export default function () {
  // 테스트할 실제 API 엔드포인트로 변경
  const url = `${BASE_URL}/api/health`; // 테스트 할 API
  // 헤더에 JWT 토큰 추가
    const params = {
      headers: {
        'Authorization': 'Bearer 여기에_실제_발급받은_액세스_토큰_문자열_입력',
        'Content-Type': 'application/json',
      },
    };

    const res = http.get(url);
    // const res = http.get(url, params);

  // 3. 검증 로직 (Assertions)
  // 응답 코드가 200인지, 응답 시간이 500ms 이하인지 확인
  check(res, {
    'is status 200': (r) => r.status === 200,
    'transaction time < 500ms': (r) => r.timings.duration < 500,
  });

  console.log(`Response Status: ${res.status}`);
  // 실제 사용자처럼 행동하도록 각 요청 사이에 0.5초 ~ 1.5초 사이의 랜덤한 대기 시간 추가
  sleep(Math.random() + 0.5);
}