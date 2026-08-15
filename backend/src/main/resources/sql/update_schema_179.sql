-- #179 여행 목표 등록 및 AI 여행 예산 추천 스키마 변경
-- 기존 DB에 적용할 때 사용합니다. schema.sql은 전체 초기화용입니다.

CREATE TABLE IF NOT EXISTS trip_wallets
(
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'TRIP 월렛 ID',
    user_id       BIGINT         NOT NULL COMMENT '회원 ID',
    balance       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '현재 월렛 잔액',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trip_wallets_user (user_id),
    CONSTRAINT fk_trip_wallets_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT 'TRIP 월렛';

CREATE TABLE IF NOT EXISTS trip_budget_recommendations
(
    id                         BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'AI 예산 추천 ID',
    trip_country_id            BIGINT         NOT NULL COMMENT '여행 국가 ID',
    traveler_count             INT            NOT NULL DEFAULT 1 COMMENT '여행 인원(현재 성인 1인 고정)',
    travel_style               VARCHAR(20)    NOT NULL DEFAULT 'MID_RANGE' COMMENT '여행 스타일',
    recommended_airfare_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    recommended_lodging_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    recommended_activity_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    recommended_food_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0,
    recommended_other_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0,
    confirmed_airfare_amount   DECIMAL(18, 2) NULL,
    confirmed_lodging_amount   DECIMAL(18, 2) NULL,
    confirmed_activity_amount  DECIMAL(18, 2) NULL,
    confirmed_food_amount      DECIMAL(18, 2) NULL,
    confirmed_other_amount     DECIMAL(18, 2) NULL,
    ai_reason                  VARCHAR(1000) NULL,
    ai_model                   VARCHAR(100)  NULL,
    is_confirmed               TINYINT(1)    NOT NULL DEFAULT 0,
    created_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trip_budget_recommendations_country (trip_country_id),
    CONSTRAINT fk_trip_budget_recommendations_country
        FOREIGN KEY (trip_country_id) REFERENCES trip_countries (id)
) COMMENT '여행 국가별 AI 예산 추천';

CREATE INDEX idx_trip_budget_recommendations_country
    ON trip_budget_recommendations (trip_country_id);
