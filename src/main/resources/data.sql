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
