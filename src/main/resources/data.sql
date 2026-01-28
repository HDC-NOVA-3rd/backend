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

INSERT INTO ho (dong_id, ho_no, floor) VALUES (@DONG_101, '101호', 1);
SET @HO_101_101 := LAST_INSERT_ID();

INSERT INTO ho (dong_id, ho_no, floor) VALUES (@DONG_101, '102호', 1);

INSERT INTO ho (dong_id, ho_no, floor) VALUES (@DONG_102, '101호', 1);
SET @HO_102_101 := LAST_INSERT_ID();

INSERT INTO ho (dong_id, ho_no, floor) VALUES (@DONG_102, '103호', 1);

-- =========================
-- HomeEnvironment 더미
-- =========================

INSERT INTO room (ho_id, name) VALUES (@HO_101_101, '거실');
SET @ROOM1 := LAST_INSERT_ID();

INSERT INTO room (ho_id, name) VALUES (@HO_101_101, '침실');
SET @ROOM2 := LAST_INSERT_ID();

INSERT INTO room_env_log (room_id, sensor_type, sensor_value, unit, recorded_at, created_at)
VALUES
    (@ROOM1, 'TEMP', 235, 'C', NOW(), NOW()),
    (@ROOM1, 'HUMIDITY', 45, '%', NOW(), NOW()),
    (@ROOM2, 'TEMP', 221, 'C', NOW(), NOW()),
    (@ROOM2, 'HUMIDITY', 40, '%', NOW(), NOW());
