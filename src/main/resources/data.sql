-- =========================
-- Apartment / Dong / Ho 더미 (ID 가정 X)
-- =========================

INSERT INTO apartment (name, address, zipcode)
VALUES ('자이 아파트', '서울시 강남구 역삼동', '12345');
SET @APT_ID := LAST_INSERT_ID();

INSERT INTO dong (apartment_id, dong_no) VALUES (@APT_ID, '101동');
SET @DONG_101 := LAST_INSERT_ID();

INSERT INTO dong (apartment_id, dong_no) VALUES (@APT_ID, '102동');
SET @DONG_102 := LAST_INSERT_ID();

-- 102동 관련 호수들
INSERT INTO ho (dong_id, ho_no, floor) VALUES (2, '101호', 1);
INSERT INTO ho (dong_id, ho_no, floor) VALUES (2, '103호', 1);

-- resident: 101동 101호, 102호에 사람 넣기
INSERT INTO resident (ho_id, name, phone)
VALUES
    (1, '홍길동', '010-1111-2222'),
    (2, '김영희', '010-3333-4444');

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

