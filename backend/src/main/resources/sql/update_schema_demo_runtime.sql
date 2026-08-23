-- 데모 시나리오와 현재 백엔드 코드 사이의 누락 컬럼을 멱등하게 보완한다.
-- 각 문장은 컬럼이 이미 존재하면 아무 작업도 하지 않으므로 반복 실행해도 안전하다.
USE tripass;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'users'
        AND column_name = 'onboarding_shown_at') = 0,
    'ALTER TABLE users ADD COLUMN onboarding_shown_at DATETIME NULL COMMENT ''온보딩 최초 처리 시각'' AFTER current_view_mode',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'trips'
        AND column_name = 'initial_wallet_balance') = 0,
    'ALTER TABLE trips ADD COLUMN initial_wallet_balance DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT ''여행 시작 시 월렛 잔액'' AFTER total_target_amount',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'trips'
        AND column_name = 'emergency_amount') = 0,
    'ALTER TABLE trips ADD COLUMN emergency_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT ''목표 외 비상금'' AFTER initial_wallet_balance',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'trips'
        AND column_name = 'external_charge_amount') = 0,
    'ALTER TABLE trips ADD COLUMN external_charge_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT ''여행 중 추가 충전액'' AFTER emergency_amount',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'trips'
        AND column_name = 'start_report_viewed_at') = 0,
    'ALTER TABLE trips ADD COLUMN start_report_viewed_at DATETIME NULL COMMENT ''여행 시작 리포트 확인 시각'' AFTER total_target_amount',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'trips'
        AND column_name = 'savings_tracking_started_at') = 0,
    'ALTER TABLE trips ADD COLUMN savings_tracking_started_at DATETIME NULL COMMENT ''여행 저축 집계 시작 시각'' AFTER total_target_amount',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'trips'
        AND column_name = 'wallet_reflect_resolved') = 0,
    'ALTER TABLE trips ADD COLUMN wallet_reflect_resolved TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''기존 월렛 잔액 반영 선택 완료 여부'' AFTER savings_tracking_started_at',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'wallet_ledger'
        AND column_name = 'trip_id') = 0,
    'ALTER TABLE wallet_ledger ADD COLUMN trip_id BIGINT NULL COMMENT ''귀속 여행 ID'' AFTER wallet_id, ADD INDEX idx_wallet_ledger_trip (trip_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'monthly_saving_missions'
        AND column_name = 'trip_id') = 0,
    'ALTER TABLE monthly_saving_missions ADD COLUMN trip_id BIGINT NULL COMMENT ''귀속 여행 ID'' AFTER user_id, ADD INDEX idx_monthly_saving_missions_trip (trip_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'monthly_saving_missions'
        AND column_name = 'selected_at') = 0,
    'ALTER TABLE monthly_saving_missions ADD COLUMN selected_at DATE NULL COMMENT ''미션 선택일'' AFTER start_week',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'monthly_saving_missions'
        AND column_name = 'mission_start_date') = 0,
    'ALTER TABLE monthly_saving_missions ADD COLUMN mission_start_date DATE NULL COMMENT ''첫 주 미션 시작일'' AFTER selected_at',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'weekly_saving_missions'
        AND column_name = 'eligible_day_count') = 0,
    'ALTER TABLE weekly_saving_missions ADD COLUMN eligible_day_count TINYINT NOT NULL DEFAULT 7 COMMENT ''해당 주 참여 일수'' AFTER weekly_expected_saving',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
