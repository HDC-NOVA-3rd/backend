-- 아파트 관련 더미 데이터 생성 SQL로, yaml파일에서 실행 시 자동 추가 (always) --

-- 1. 아파트 등록 (ID를 명시하지 않음 -> 자동으로 AI 카운트 증가)
INSERT INTO apartment (name, address, latitude, longitude)
VALUES ('자이 아파트', '서울시 강남구 역삼동', 37.5172, 127.0473),
       ('북극 아파트', '북극동', 90, 0);

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
    (123, 1, NULL, 1, 'FACILITY_SMOKE_123', 'MQTT', 'SMOKE', NOW(6)),
    (124, 1, 1, NULL, 'DONG_HEAT_124', 'MQTT', 'HEAT', NOW(6));

INSERT INTO sensor_log (sensor_id, value, unit, recorded_at)
VALUES
    (123, 120.0, 'ppm', NOW(6)),
    (124, 45.0, 'C', NOW(6));

INSERT INTO safety_status (apartment_id, dong_id, facility_id, updated_at, reason, safety_status)
VALUES
    (1, 1, NULL, NOW(6), 'HEAT', 'SAFE'),
    (1, NULL, 2, NOW(6), 'FIRE_SMOKE', 'SAFE');

INSERT INTO safety_event_log (apartment_id, dong_id, facility_id, manual, request_from, sensor_id, sensor_type, value, unit, status_to, event_at)
VALUES
    (1, 1, NULL, 0, 'seed', 124, 'HEAT', 75.0, 'C', 'DANGER', NOW(6)),
    (1, NULL, 2, 0, 'seed', 123, 'SMOKE', 650.0, 'ppm', 'DANGER', NOW(6));

-- admin 테이블 더미 데이터
-- Admin 엔티티 기반 삽입, 비밀번호는 BCrypt 해시, apartment_id 참조
INSERT INTO admin
(id, birth_date, created_at, email, failed_login_count, locked_until, last_login_at, login_id, name, password, phone_number, profile_img, role, status, updated_at, apartment_id, 기타컬럼)
VALUES
    (1, '1980-01-01', '2026-02-04 15:42:36.657856', 'ahncsk0709@gmail.com', 0, NULL, NULL, 'superadmin', '슈퍼 관리자', '$2a$10$ToC2gp6a8i7NR0BvJ.JhjudZ2vGdWsyfPeQv/1eB40MwM2qQ5XOGa', '01000000000', NULL, 'SUPER_ADMIN', 'ACTIVE', '2026-02-04 15:42:36.657856', 1, ''),
    (2, '1988-01-10', '2026-02-04 15:42:36.664893', 'ahncsk00@naver.com', 0, '2026-02-06 14:23:48.310425', NULL, 'admin01', '자이아파트 관리자', '$2a$10$U3Bfce5whxhtwNUYc5ure.cwY6LAX261h3s6CV2e2mkM6p497yT32', '01099998888', NULL, 'ADMIN', 'ACTIVE', '2026-02-06 14:23:50.584493', 1, '');
-- notice: 공지/대상동/발송로그 더미 데이터
INSERT INTO admin (login_id, password_hash, name, email, status, role, apartment_id, failed_login_count, created_at, updated_at)
VALUES ('seed-admin', 'seed-password-hash', '시드 관리자', 'seed-admin@nova.local', 'ACTIVE', 'ADMIN', 1, 0, NOW(6), NOW(6));

-- 전체 공지
INSERT INTO notice (admin_id, title, content, target_scope, created_at, updated_at)
VALUES (
    (SELECT id FROM admin WHERE login_id = 'seed-admin'),
    '단지 전체 안내',
    '이번 주 금요일 오전 10시부터 정전 점검이 진행됩니다.',
    'ALL',
    NOW(6),
    NOW(6)
);

-- 101동 대상 공지
INSERT INTO notice (admin_id, title, content, target_scope, created_at, updated_at)
VALUES (
    (SELECT id FROM admin WHERE login_id = 'seed-admin'),
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
