-- 아파트 관련 더미 데이터 생성 SQL로, yaml파일에서 실행 시 자동 추가 (always) --

-- 1. 아파트 등록 (ID를 명시하지 않음 -> 자동으로 AI 카운트 증가)
INSERT INTO apartment (name, address, latitude, longitude)
VALUES ('자이 아파트', '서울시 강남구 역삼동', 37.5172, 127.0473);
INSERT INTO apartment (name, address, latitude, longitude)
VALUES ('북극 아파트', '북극동', 90, 0);

-- 2. 동 등록 (아파트 ID 참조)
INSERT INTO dong (apartment_id, dong_no) VALUES (1, '101동');
INSERT INTO dong (apartment_id, dong_no) VALUES (1, '102동');

-- 3. 호수 등록 (동 ID 참조)
-- 101동 관련 호수들
INSERT INTO ho (dong_id, ho_no, floor) VALUES (1, '101호', 1);
INSERT INTO ho (dong_id, ho_no, floor) VALUES (1, '102호', 1);

-- 102동 관련 호수들
INSERT INTO ho (dong_id, ho_no, floor) VALUES (2, '101호', 1);
INSERT INTO ho (dong_id, ho_no, floor) VALUES (2, '103호', 1);

-- resident: 101동 101호, 102호에 사람 넣기
INSERT INTO resident (ho_id, name, phone)
VALUES
    (1, '홍길동', '01011112222'),
    (2, '김영희', '01033334444');

-- room: ho_id=1 (101동 101호)에 거실/침실 생성
INSERT INTO room (ho_id, name)
VALUES
    (1, '거실'),
    (1, '침실');

-- 거실(room_id=1) 환경 로그
INSERT INTO room_env_log (room_id, sensor_type, sensor_value, unit, recorded_at, created_at)
VALUES
    (1, 'TEMP', 24, 'C', NOW(6), NOW(6)),
    (1, 'HUMIDITY', 45, '%', NOW(6), NOW(6)),
    (1, 'LIGHT', 320, 'lux', NOW(6), NOW(6));
INSERT INTO device (room_id, device_code, name, type, power, brightness, target_temp, updated_at)
VALUES
    (1, 'light-1', '거실 전등', 'LED', false, 80, null, NOW()),
    (1, 'fan-1', '거실 팬', 'FAN', false, null, null, NOW());

-- 시설(facility) 등록
INSERT INTO facility (apartment_id, name, description, start_hour, end_hour, reservation_available)
VALUES
    (1, '헬스장', '지하 1층', '06:00:00', '22:00:00', 1),
    (1, '스터디룸', '1층', '09:00:00', '21:00:00', 1);

-- space (스터디룸에 속한 공간)
INSERT INTO space (facility_id, name, max_capacity, min_capacity, price)
VALUES
    (2, '스터디룸 A', 6, 1, 0),
    (2, '스터디룸 B', 8, 1, 0);

-- safety: MQTT 테스트용 센서/로그/상태 더미 데이터
-- deviceId=123 으로 MQTT 수신 테스트 시 매칭되는 센서
INSERT INTO sensor (id, apartment_id, ho_id, space_id, name, type, sensor_type, created_at)
VALUES
    (1, 1, 1, NULL, 'HO_101_GAS', 'MQTT', 'GAS', NOW(6)),
    (2, 1, 1, NULL, 'HO_101_HEAT', 'MQTT', 'HEAT', NOW(6)),
    (9, 1, NULL, 1, 'SPACE_1_GAS', 'MQTT', 'GAS', NOW(6)),
    (10, 1, NULL, 1, 'SPACE_1_HEAT', 'MQTT', 'HEAT', NOW(6));

INSERT INTO sensor_log (sensor_id, value, unit, recorded_at)
VALUES
    (1, 320.0, 'raw', NOW(6)),
    (2, 25.0, 'C', NOW(6)),
    (9, 410.0, 'raw', NOW(6)),
    (10, 26.0, 'C', NOW(6));

INSERT INTO safety_status (apartment_id, dong_id, facility_id, updated_at, reason, safety_status)
VALUES
    (1, 1, NULL, NOW(6), 'HEAT', 'SAFE'),
    (1, NULL, 2, NOW(6), 'GAS', 'SAFE');

INSERT INTO safety_event_log (apartment_id, dong_id, facility_id, manual, request_from, sensor_id, sensor_type, value, unit, status_to, event_at)
VALUES
    (1, 1, NULL, 0, 'seed', 2, 'HEAT', 75.0, 'C', 'DANGER', NOW(6)),
    (1, NULL, 2, 0, 'seed', 9, 'GAS', 650.0, 'raw', 'DANGER', NOW(6));

-- admin 테이블 더미 데이터
-- Admin 엔티티 기반 삽입, 비밀번호는 BCrypt 해시, apartment_id 참조
-- admin 테이블 더미 데이터
-- 비밀번호는 BCrypt 해시, apartment_id = 1 기준

INSERT INTO admin (
    id,
    birth_date,
    created_at,
    updated_at,
    email,
    failed_login_count,
    locked_until,
    last_login_at,
    login_id,
    name,
    password,
    phone_number,
    profile_img,
    role,
    status,
    apartment_id
)
VALUES
(
    1,
    '1980-01-01',
    '2026-02-04 15:42:36.657856',
    '2026-02-04 15:42:36.657856',
    'ahncsk0709@gmail.com',
    0,
    NULL,
    NULL,
    'superadmin',
    '슈퍼 관리자',
    '$2a$10$ToC2gp6a8i7NR0BvJ.JhjudZ2vGdWsyfPeQv/1eB40MwM2qQ5XOGa',
    '01000000000',
    NULL,
    'SUPER_ADMIN',
    'ACTIVE',
    1
),
(
    2,
    '1988-01-10',
    '2026-02-04 15:42:36.664893',
    '2026-02-06 14:23:50.584493',
    'ahncsk00@naver.com',
    0,
    '2026-02-06 14:23:48.310425',
    NULL,
    'admin01',
    '자이아파트 관리자',
    '$2a$10$U3Bfce5whxhtwNUYc5ure.cwY6LAX261h3s6CV2e2mkM6p497yT32',
    '01099998888',
    NULL,
    'ADMIN',
    'ACTIVE',
    1
);

-- 전체 공지
INSERT INTO notice (admin_id, title, content, target_scope, created_at, updated_at)
VALUES (
    (SELECT id FROM admin WHERE login_id = 'admin01'),
    '단지 전체 안내',
    '이번 주 금요일 오전 10시부터 정전 점검이 진행됩니다.',
    'ALL',
    NOW(6),
    NOW(6)
);

-- 101동 대상 공지
INSERT INTO notice (admin_id, title, content, target_scope, created_at, updated_at)
VALUES (
    (SELECT id FROM admin WHERE login_id = 'admin01'),
    '101동 소독 일정 안내',
    '101동은 화요일 오후 2시에 공동 구역 방역을 진행합니다.',
    'DONG',
    NOW(6),
    NOW(6)
);

-- 동 대상 매핑 (101동 공지 -> 101동)
INSERT INTO notice_target_dong (notice_id, dong_id, created_at)
VALUES (
    (SELECT id FROM notice WHERE title = '101동 소독 일정 안내'),
    (SELECT id FROM dong WHERE apartment_id = 1 AND dong_no = '101동'),
    NOW(6)
);

-- 발송 로그 샘플
INSERT INTO notice_send_log (notice_id, recipient_id, title, content, sent_at, is_read)
VALUES
(
    (SELECT id FROM notice WHERE title = '단지 전체 안내'),
    (SELECT id FROM resident WHERE phone = '01011112222'),
    '단지 전체 안내',
    '이번 주 금요일 오전 10시부터 정전 점검이 진행됩니다.',
    NOW(6),
    0
),
(
    (SELECT id FROM notice WHERE title = '101동 소독 일정 안내'),
    (SELECT id FROM resident WHERE phone = '01033334444'),
    '101동 소독 일정 안내',
    '101동은 화요일 오후 2시에 공동 구역 방역을 진행합니다.',
    NOW(6),
    1
);

-- ---------------------------------------------------------
-- 추가 시설(Facility) 등록 (ID 3~6 자동 생성 가정)
-- ---------------------------------------------------------

-- 3. 실내 골프연습장 (06~23시, 예약 가능)
INSERT INTO facility (apartment_id, name, description, start_hour, end_hour, reservation_available)
VALUES (1, '실내 골프연습장', '지하 2층 스포츠 센터', '06:00:00', '23:00:00', 1);

-- 4. 게스트하우스 (13~22시, 예약 가능, 가족 단위 숙박)
INSERT INTO facility (apartment_id, name, description, start_hour, end_hour, reservation_available)
VALUES (1, '게스트하우스', '105동 1층', '13:00:00', '22:00:00', 1);

-- 5. 프리미엄 독서실 (24시간, 예약 가능)
INSERT INTO facility (apartment_id, name, description, start_hour, end_hour, reservation_available)
VALUES (1, '프리미엄 독서실', '커뮤니티 센터 2층', '00:00:00', '23:59:59', 1);

-- 6. 주민 카페 (10~19시, 예약 불가능 - Walk-in 전용 테스트)
INSERT INTO facility (apartment_id, name, description, start_hour, end_hour, reservation_available)
VALUES (1, '주민 카페', '커뮤니티 센터 로비', '10:00:00', '19:00:00', 0);


-- ---------------------------------------------------------
-- 추가 공간(Space) 등록
-- *기존 데이터가 2개 있으므로 Facility ID는 3번부터 시작한다고 가정*
-- ---------------------------------------------------------

-- [Facility ID: 3] 골프연습장 공간들
-- 무료 타석과 유료 게임룸이 섞여 있는 경우 테스트
INSERT INTO space (facility_id, name, max_capacity, min_capacity, price)
VALUES
    (3, '일반 타석 1', 1, 1, 0),      -- 1인 전용, 무료
    (3, '일반 타석 2', 1, 1, 0),
    (3, '스크린 골프룸 A', 4, 1, 20000); -- 최대 4인, 유료

-- [Facility ID: 4] 게스트하우스 공간들
-- 수용 인원 범위(min~max) 필터링 로직 테스트
INSERT INTO space (facility_id, name, max_capacity, min_capacity, price)
VALUES
    (4, 'Standard Room (20평)', 4, 1, 50000), -- 1~4명 수용
    (4, 'Royal Suite (40평)', 8, 4, 120000);  -- 4~8명 수용 (최소 인원 제한 있음)

-- [Facility ID: 5] 독서실 공간들
-- 저렴한 유료 좌석 테스트
INSERT INTO space (facility_id, name, max_capacity, min_capacity, price)
VALUES
    (5, '1인 집중석 A', 1, 1, 2000),
    (5, '1인 집중석 B', 1, 1, 2000),
    (5, '오픈 데스크', 1, 1, 0);

-- [Facility ID: 6] 주민 카페
-- 공간(Space) 데이터가 없는 경우(Empty List 반환)를 테스트하기 위해 insert 생략

-- [complaint] 데이터
INSERT INTO complaint
(content, created_at, deleted, resolved_at, status, title, type, updated_at, admin_id, apartment_id, member_id)
VALUES
('엘리베이터가 3일째 작동하지 않습니다.', NOW(6), b'0', NOW(6), 'RECEIVED', '엘리베이터 고장 신고', 'MAINTENANCE', NOW(6), 1, 1, 1),

('윗층에서 밤마다 소음이 심합니다.', NOW(6), b'0', NOW(6), 'IN_PROGRESS', '층간소음 민원', 'NOISE', NOW(6), 2, 2, 1),

('지하주차장에 불법주차 차량이 있습니다.', NOW(6), b'0', NOW(6), 'ASSIGNED', '불법주차 신고', 'PARKING', NOW(6), 1, 1, 2),

('관리비 청구 금액이 잘못된 것 같습니다.', NOW(6), b'0', NOW(6), 'COMPLETED', '관리비 오류 문의', 'ADMIN', NOW(6), 3, 2, 2),

('공용 복도 조명이 깜빡거립니다.', NOW(6), b'0', NOW(6), 'RECEIVED', '복도 조명 수리 요청', 'LIVING', NOW(6), 2, 1, 2);
-- 'ASSIGNED','CANCELLED','COMPLETED','IN_PROGRESS','RECEIVED'
-- 'ADMIN','LIVING','MAINTENANCE','NOISE','OTHER','PARKING'

-- [reservation] 데이터
-- 1. 과거 데이터: 이미 이용 완료된 예약 (COMPLETED)
INSERT INTO reservation (member_id, space_id, start_time, end_time, capacity, total_price, owner_name, owner_phone, payment_method, qr_token, status)
VALUES (1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 2 HOUR, 4, 20000, '홍길동', '010-1234-5678', 'ONLINE_PAYMENT', 'qr_past_001', 'COMPLETED');

-- 2. 현재 데이터: 지금 이용 중인 예약 (INUSE)
-- 현재 시간 기준 앞뒤로 걸쳐 있어 QR 이용 가능한 상태
INSERT INTO reservation (member_id, space_id, start_time, end_time, capacity, total_price, owner_name, owner_phone, payment_method, qr_token, status)
VALUES (1, 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 1 HOUR), 2, 10000, '홍길동', '010-1234-5678', 'MANAGEMENT_FEE', 'qr_now_002', 'INUSE');

-- 3. 미래 데이터: 예약 확정 상태 (CONFIRMED)
-- 내일 예약 건
INSERT INTO reservation (member_id, space_id, start_time, end_time, capacity, total_price, owner_name, owner_phone, payment_method, qr_token, status)
VALUES (1, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 3 HOUR, 6, 30000, '홍길동', '010-1234-5678', 'ONLINE_PAYMENT', 'qr_future_003', 'CONFIRMED');

-- 4. 취소 데이터: 사용자가 취소한 건 (CANCELLED)
INSERT INTO reservation (member_id, space_id, start_time, end_time, capacity, total_price, owner_name, owner_phone, payment_method, qr_token, status)
VALUES (1, 1, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY) + INTERVAL 1 HOUR, 1, 5000, '홍길동', '010-1234-5678', 'MANAGEMENT_FEE', 'qr_cancel_004', 'CANCELLED');

-- 5. 입장 임박 데이터: 곧 INUSE로 변경되어야 할 예약 (CONFIRMED)
-- 시작 5분 전 데이터 (배치 작업 테스트용)
INSERT INTO reservation (member_id, space_id, start_time, end_time, capacity, total_price, owner_name, owner_phone, payment_method, qr_token, status)
VALUES (1, 1, DATE_ADD(NOW(), INTERVAL 5 MINUTE), DATE_ADD(NOW(), INTERVAL 65 MINUTE), 3, 15000, '김철수', '010-9999-8888', 'ONLINE_PAYMENT', 'qr_near_005', 'CONFIRMED');