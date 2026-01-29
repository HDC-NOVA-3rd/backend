-- 아파트 관련 더미 데이터 생성 SQL로, yaml파일에서 실행 시 자동 추가 (always) --

-- 1. 아파트 등록 (ID를 명시하지 않음 -> 자동으로 AI 카운트 증가)
INSERT INTO apartment (name, address, latitude, longitude)
VALUES ('자이 아파트', '서울시 강남구 역삼동', 37.5172, 127.0473);


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