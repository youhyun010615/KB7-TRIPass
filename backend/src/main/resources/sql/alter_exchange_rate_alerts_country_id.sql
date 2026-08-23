-- exchange_rate_alerts: currency_id → country_id 전환
-- countries 테이블의 currency_id FK를 통해 통화 정보를 조회하는 구조로 변경

USE tripass;

-- 1. country_id 컬럼 추가
ALTER TABLE exchange_rate_alerts
    ADD COLUMN country_id BIGINT NULL COMMENT '국가 ID' AFTER user_id;

-- 2. 기존 currency_id 데이터를 country_id로 마이그레이션
UPDATE exchange_rate_alerts era
INNER JOIN countries co ON co.currency_id = era.currency_id
SET era.country_id = co.id
WHERE era.country_id IS NULL;

-- 3. country_id NOT NULL 제약 추가
ALTER TABLE exchange_rate_alerts
    MODIFY COLUMN country_id BIGINT NOT NULL COMMENT '국가 ID';

-- 4. 기존 currency_id FK 제거
ALTER TABLE exchange_rate_alerts
    DROP FOREIGN KEY fk_exchange_rate_alerts_currency;

-- 5. currency_id 컬럼 제거
ALTER TABLE exchange_rate_alerts
    DROP COLUMN currency_id;

-- 6. country_id FK 추가
ALTER TABLE exchange_rate_alerts
    ADD CONSTRAINT fk_exchange_rate_alerts_country FOREIGN KEY (country_id) REFERENCES countries (id);
