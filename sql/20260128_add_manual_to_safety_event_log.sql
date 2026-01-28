-- SafetyEventLog: manual 컬럼 추가 + 기존 request_from="MANUAL" 데이터 정리
-- 대상 DB: MySQL
--
-- 변경 요약
-- 1) safety_event_log.manual (TINYINT(1) NOT NULL DEFAULT 0) 추가
-- 2) 기존 request_from 값이 'MANUAL' 이면 manual=1 로 백필
-- 3) request_from 에 플래그 문자열 저장 금지 원칙에 따라 'MANUAL' 값을 'unknown' 으로 치환

-- 1) manual 컬럼이 없으면 추가
SET @manual_col_exists :=
    (SELECT COUNT(*)
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'safety_event_log'
       AND COLUMN_NAME = 'manual');

SET @add_manual_sql :=
    IF(@manual_col_exists = 0,
       'ALTER TABLE safety_event_log ADD COLUMN manual TINYINT(1) NOT NULL DEFAULT 0',
       'SELECT 1');

PREPARE stmt FROM @add_manual_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 기존 MANUAL 플래그 데이터 백필
UPDATE safety_event_log
SET manual = 1
WHERE UPPER(request_from) = 'MANUAL';

-- 3) request_from 역할 변경: 플래그 문자열 제거
UPDATE safety_event_log
SET request_from = 'unknown'
WHERE UPPER(request_from) = 'MANUAL';

