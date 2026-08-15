USE tripass;

-- #201: 카테고리에 코드 추가
ALTER TABLE spending_categories
    ADD COLUMN category_code VARCHAR(30) NULL
        COMMENT '카테고리 식별 코드' AFTER id;

-- 기존 카테고리 코드 설정
UPDATE spending_categories
SET category_code =
    CASE category_name
        WHEN '식비' THEN 'FOOD'
        WHEN '교통' THEN 'TRANSPORT'
        WHEN '교통비' THEN 'TRANSPORT'
        WHEN '숙박' THEN 'LODGING'
        WHEN '쇼핑' THEN 'SHOPPING'
        WHEN '관광' THEN 'SIGHTSEEING'
        WHEN '기타' THEN 'OTHER'
        WHEN '카페' THEN 'CAFE'
        WHEN '생활비' THEN 'LIVING'
        WHEN '취미여가' THEN 'LEISURE'
        ELSE category_code
    END
WHERE category_code IS NULL;

-- 위 CASE 목록에 없는 미확인 카테고리(팀/환경별 커스텀 데이터)는 category_code가 계속 NULL로 남아
-- 아래 NOT NULL 적용이 실패할 수 있으므로, id 기반의 임시 코드를 부여해 안전하게 넘어간다.
-- 이런 행이 실제로 존재한다면 운영팀이 실제 의미에 맞는 코드로 별도 정리해야 한다.
UPDATE spending_categories
SET category_code = CONCAT('CATEGORY_', id)
WHERE category_code IS NULL;

-- AI 저축 미션에 필요한 카페 카테고리 추가
INSERT INTO spending_categories
    (category_code, category_name, display_order)
SELECT
    'CAFE', '카페', 7
WHERE NOT EXISTS (
    SELECT 1
    FROM spending_categories
    WHERE category_code = 'CAFE'
       OR category_name = '카페'
);

-- AI 저축 미션에 필요한 생활비 카테고리 추가
INSERT INTO spending_categories
    (category_code, category_name, display_order)
SELECT
    'LIVING', '생활비', 8
WHERE NOT EXISTS (
    SELECT 1
    FROM spending_categories
    WHERE category_code = 'LIVING'
       OR category_name = '생활비'
);

-- AI 저축 미션에 필요한 취미여가 카테고리 추가
INSERT INTO spending_categories
    (category_code, category_name, display_order)
SELECT
    'LEISURE', '취미여가', 9
WHERE NOT EXISTS (
    SELECT 1
    FROM spending_categories
    WHERE category_code = 'LEISURE'
       OR category_name = '취미여가'
);

-- category_code가 없는 기존 사용자 정의 데이터가 있는지 확인한 후
-- 모든 데이터에 코드가 존재하는 환경에서만 NOT NULL로 변경한다.
-- (아래 ALTER 실행 전 반드시 SELECT id, category_name FROM spending_categories WHERE category_code IS NULL; 로 확인)
ALTER TABLE spending_categories
    MODIFY COLUMN category_code VARCHAR(30) NOT NULL
        COMMENT '카테고리 식별 코드';

ALTER TABLE spending_categories
    ADD UNIQUE KEY uk_spending_categories_code (category_code);

-- #201: 거래 자동분류 결과 메타데이터 추가
ALTER TABLE transactions
    ADD COLUMN category_source VARCHAR(20) NULL
        COMMENT '카테고리 분류 출처(USER/CODEF_TYPE/AI_MODEL/FALLBACK)'
        AFTER category_id,
    ADD COLUMN category_confidence DECIMAL(5, 4) NULL
        COMMENT '자동분류 신뢰도(0~1)'
        AFTER category_source,
    ADD COLUMN category_classified_at DATETIME NULL
        COMMENT '카테고리 자동분류 시각'
        AFTER category_confidence;
