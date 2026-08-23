-- 데모 가상 시계 기능에 필요한 users 컬럼을 멱등하게 추가한다.
-- 이미 수동으로 컬럼을 추가한 개발 DB에서도 안전하게 재실행할 수 있다.
USE tripass;

SET @add_dev_override_date = IF(
    (SELECT COUNT(*)
       FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'users'
        AND column_name = 'dev_override_date') = 0,
    'ALTER TABLE users ADD COLUMN dev_override_date DATE NULL DEFAULT NULL COMMENT ''개발용 가상 기준일'' AFTER current_view_mode',
    'SELECT 1'
);
PREPARE add_dev_override_date_stmt FROM @add_dev_override_date;
EXECUTE add_dev_override_date_stmt;
DEALLOCATE PREPARE add_dev_override_date_stmt;

SET @add_demo_recording_mode = IF(
    (SELECT COUNT(*)
       FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'users'
        AND column_name = 'demo_recording_mode') = 0,
    'ALTER TABLE users ADD COLUMN demo_recording_mode TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''가상 날짜 전환 시 자동 리셋 생략 여부'' AFTER dev_override_date',
    'SELECT 1'
);
PREPARE add_demo_recording_mode_stmt FROM @add_demo_recording_mode;
EXECUTE add_demo_recording_mode_stmt;
DEALLOCATE PREPARE add_demo_recording_mode_stmt;
