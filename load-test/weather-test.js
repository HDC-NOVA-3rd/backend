import http from 'k6/http';
import { check, sleep, group } from 'k6';

// 1. 부하 테스트 시나리오 (Options)
export const options = {
  stages: [
    { duration: '30s', target: 50 },  // 30초 동안 가상 사용자(VU) 50명으로 증가
    { duration: '1m', target: 50 },   // 1분 동안 50명의 VU 유지 (지속적 부하)
    { duration: '30s', target: 0 },   // 30초 동안 0명으로 감소
  ],
  // (선택) 임계값 설정: 95%의 요청이 특정 시간 내에 들어와야 테스트 통과로 간주
  thresholds: {
    'http_req_duration': ['p(95)<200'], // 전체 요청의 95%가 200ms 미만이어야 함
  },
};

// 타겟 서버 URL (터미널에서 환경변수로 주입 가능)
const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

// 테스트에 사용할 실제 Access Token (회원 정보 조회 API용)
// 터미널 실행 시: k6 run -e ACCESS_TOKEN="실제토큰" load_test.js 로 주입하는 것을 권장합니다.
//const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '여기에_실제_발급받은_액세스_토큰_문자열_입력';

// 2. 가상 사용자가 반복해서 실행할 행동
export default function () {

  // 인증이 필요한 API를 위한 헤더
//  const params = {
//    headers: {
//      'Authorization': `Bearer ${ACCESS_TOKEN}`,
//      'Content-Type': 'application/json',
//    },
//  };

  // -----------------------------------------------------------
  // [TEST 1] 날씨 API (Redis Caching + 비동기 WebClient 최적화 검증)
  // -----------------------------------------------------------
  group('1. Apartment Weather API (Redis Optimized)', function () {
    const apartmentId = 1; // 테스트할 단지 ID
    const weatherUrl = `${BASE_URL}/api/apartment/${apartmentId}/weather`;

    // 날씨 API는 보통 토큰이 필요 없으므로 파라미터 없이 호출
    const weatherRes = http.get(weatherUrl);

    // 검증 로직 (캐시 히트 시 매우 빠르므로 50ms 이하 기준 추가)
    check(weatherRes, {
      'status is 200': (r) => r.status === 200,
      'weather duration < 500ms': (r) => r.timings.duration < 500, // 최초 요청 보장
      'weather cache hit (< 50ms)': (r) => r.timings.duration < 50, // 캐시 적중 시
    });
  });

  // -----------------------------------------------------------
  // [TEST 2] 내 아파트 정보 API (Fetch Join 최적화 검증)
  // -----------------------------------------------------------
  group('2. Member Apartment Info API (Fetch Join Optimized)', function () {
    const apartmentUrl = `${BASE_URL}/api/member/apartment`;

    // 회원 정보 API는 토큰이 필요하므로 params 전달
    const apartmentRes = http.get(apartmentUrl, params);

    // 검증 로직 (1+1 쿼리 제거로 I/O가 줄었는지 확인)
    check(apartmentRes, {
      'status is 200': (r) => r.status === 200,
      'apartment duration < 200ms': (r) => r.timings.duration < 200,
    });
  });

  // 실제 사용자처럼 행동하도록 0.5초 ~ 1.5초 사이의 랜덤 대기
  sleep(Math.random() + 0.5);
}