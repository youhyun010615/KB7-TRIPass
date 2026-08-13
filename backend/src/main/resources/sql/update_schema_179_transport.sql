-- #179 여행 예산 기준 단가·교통비 확장 변경 SQL
-- update_schema_179.sql 적용 여부와 관계없이 안전하게 실행할 수 있습니다.

CREATE TABLE IF NOT EXISTS country_budget_baselines
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '국가별 예산 기준값 ID',
    country_id         BIGINT         NOT NULL COMMENT '국가 ID',
    round_trip_airfare DECIMAL(18, 2) NOT NULL COMMENT '왕복 항공권 평균가(원화)',
    lodging_per_night  DECIMAL(18, 2) NOT NULL COMMENT '숙소 1박 평균가(원화)',
    food_per_day       DECIMAL(18, 2) NOT NULL COMMENT '1일 식비 평균(원화)',
    activity_per_day   DECIMAL(18, 2) NOT NULL COMMENT '1일 액티비티 평균(원화)',
    transport_per_day  DECIMAL(18, 2) NOT NULL COMMENT '1일 현지 교통비 평균(원화)',
    misc_per_day       DECIMAL(18, 2) NOT NULL COMMENT '1일 기타 평균(원화)',
    data_source        VARCHAR(500)   NULL COMMENT '조사 출처',
    reference_date     DATE           NOT NULL COMMENT '기준 조사일',
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_country_budget_baselines_country (country_id),
    CONSTRAINT fk_country_budget_baselines_country
        FOREIGN KEY (country_id) REFERENCES countries (id)
) COMMENT '국가별 여행 예산 기준값';

SET @has_recommended_transport := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'trip_budget_recommendations'
      AND COLUMN_NAME = 'recommended_transport_amount'
);
SET @sql := IF(@has_recommended_transport = 0,
    'ALTER TABLE trip_budget_recommendations ADD COLUMN recommended_transport_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''AI 추천 교통비 현지지출'' AFTER recommended_activity_amount',
    'SELECT 1');
PREPARE statement_to_execute FROM @sql;
EXECUTE statement_to_execute;
DEALLOCATE PREPARE statement_to_execute;

SET @has_confirmed_transport := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'trip_budget_recommendations'
      AND COLUMN_NAME = 'confirmed_transport_amount'
);
SET @sql := IF(@has_confirmed_transport = 0,
    'ALTER TABLE trip_budget_recommendations ADD COLUMN confirmed_transport_amount DECIMAL(18, 2) NULL COMMENT ''사용자 확정 교통비 현지지출'' AFTER confirmed_activity_amount',
    'SELECT 1');
PREPARE statement_to_execute FROM @sql;
EXECUTE statement_to_execute;
DEALLOCATE PREPARE statement_to_execute;
