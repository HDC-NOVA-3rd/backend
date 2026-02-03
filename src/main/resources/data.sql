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