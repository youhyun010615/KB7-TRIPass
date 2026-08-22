-- users 테이블에 가상 날짜 컬럼 추가
ALTER TABLE users ADD COLUMN dev_override_date DATE NULL DEFAULT NULL AFTER current_view_mode;
