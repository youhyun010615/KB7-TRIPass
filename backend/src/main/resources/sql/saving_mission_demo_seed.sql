-- ============================================================================
-- #258 AI 저축 미션 화면 검증용 카드 거래 Seed
--
-- 대상 사용자: cadry111
-- 분석월: 2026-07
-- 목표월: 2026-08
--
-- 목적
--   실제 CODEF 거래만으로 추천 조건을 충족하지 못하는 경우에도
--   월간 소비 분석 -> 추천 TOP 3 -> 절감률 선택 -> 미션 생성 화면을 검증한다.
--
-- 주의
--   1. 실제 카드·통장 연동 API를 대체하지 않는다.
--   2. cadry111이 보유한 활성 카드 중 일반 결제 카드로 보이는 카드를 우선 사용한다.
--   3. external_key의 DEMO:SAV:2026-07 접두사로 실제 거래와 구분한다.
--   4. INSERT IGNORE를 사용하므로 여러 번 실행해도 중복 적재되지 않는다.
--   5. Seed 실행 후 반드시 POST /api/v1/saving/analyses/2026-07을 호출해
--      기존 월간 리포트를 다시 계산해야 한다.
-- ============================================================================

USE tripass;

-- 추천 후보 조건
--   - 카테고리별 1~28일 거래 5건 이상
--   - 카테고리별 1~28일 지출 50,000원 이상
--   - 단일 거래 금액이 해당 카테고리 지출의 50% 미만
--
-- 식비 6건 / 87,000원
-- 카페 6건 / 57,000원
-- 쇼핑 6건 / 123,000원
INSERT IGNORE INTO transactions
(
    account_id,
    card_id,
    category_id,
    category_source,
    category_confidence,
    category_classified_at,
    transaction_date,
    transaction_time,
    transaction_type,
    transaction_region,
    amount,
    merchant_name,
    merchant_type,
    payment_method,
    memo,
    external_key,
    is_deleted
)
SELECT NULL,
       demo_card.card_id,
       category.id,
       'AI_MODEL',
       0.9900,
       NOW(),
       demo_transaction.transaction_date,
       demo_transaction.transaction_time,
       'WITHDRAWAL',
       'DOMESTIC',
       demo_transaction.amount,
       demo_transaction.merchant_name,
       demo_transaction.merchant_type,
       'CARD',
       '#258 AI 저축 미션 화면 검증용 거래',
       demo_transaction.external_key,
       0
FROM
(
    SELECT c.id AS card_id
    FROM cards c
    INNER JOIN users u ON u.id = c.user_id
    WHERE u.login_id = 'cadry111'
      AND c.is_active = TRUE
      AND c.is_deleted = FALSE
    ORDER BY CASE WHEN c.card_name LIKE '%노리2%' THEN 0 ELSE 1 END,
             c.id
    LIMIT 1
) demo_card
INNER JOIN
(
    SELECT 'FOOD' AS category_code, DATE('2026-07-02') AS transaction_date, TIME('12:10:00') AS transaction_time,
           18000 AS amount, '한상차림 강남점' AS merchant_name, '일반음식점' AS merchant_type,
           'DEMO:SAV:2026-07:FOOD:001' AS external_key
    UNION ALL SELECT 'FOOD', '2026-07-05', '18:40:00', 16000, '오늘의식탁', '일반음식점', 'DEMO:SAV:2026-07:FOOD:002'
    UNION ALL SELECT 'FOOD', '2026-07-09', '12:25:00', 15000, '도시락공방', '일반음식점', 'DEMO:SAV:2026-07:FOOD:003'
    UNION ALL SELECT 'FOOD', '2026-07-13', '19:15:00', 14000, '행복한그릇', '일반음식점', 'DEMO:SAV:2026-07:FOOD:004'
    UNION ALL SELECT 'FOOD', '2026-07-18', '13:05:00', 13000, '키친테이블', '일반음식점', 'DEMO:SAV:2026-07:FOOD:005'
    UNION ALL SELECT 'FOOD', '2026-07-24', '18:20:00', 11000, '샐러드데이', '일반음식점', 'DEMO:SAV:2026-07:FOOD:006'

    UNION ALL SELECT 'CAFE', '2026-07-03', '09:10:00', 8500, '카페모먼트', '커피전문점', 'DEMO:SAV:2026-07:CAFE:001'
    UNION ALL SELECT 'CAFE', '2026-07-07', '14:30:00', 9500, '브루잉하우스', '커피전문점', 'DEMO:SAV:2026-07:CAFE:002'
    UNION ALL SELECT 'CAFE', '2026-07-11', '10:20:00', 10000, '카페모먼트', '커피전문점', 'DEMO:SAV:2026-07:CAFE:003'
    UNION ALL SELECT 'CAFE', '2026-07-15', '15:45:00', 9000, '데일리커피', '커피전문점', 'DEMO:SAV:2026-07:CAFE:004'
    UNION ALL SELECT 'CAFE', '2026-07-20', '11:10:00', 10500, '브루잉하우스', '커피전문점', 'DEMO:SAV:2026-07:CAFE:005'
    UNION ALL SELECT 'CAFE', '2026-07-25', '16:05:00', 9500, '데일리커피', '커피전문점', 'DEMO:SAV:2026-07:CAFE:006'

    UNION ALL SELECT 'SHOPPING', '2026-07-04', '17:20:00', 24000, '라이프마켓', '온라인쇼핑', 'DEMO:SAV:2026-07:SHOPPING:001'
    UNION ALL SELECT 'SHOPPING', '2026-07-08', '20:10:00', 22000, '데일리몰', '온라인쇼핑', 'DEMO:SAV:2026-07:SHOPPING:002'
    UNION ALL SELECT 'SHOPPING', '2026-07-12', '13:40:00', 21000, '라이프마켓', '온라인쇼핑', 'DEMO:SAV:2026-07:SHOPPING:003'
    UNION ALL SELECT 'SHOPPING', '2026-07-16', '19:30:00', 20000, '스타일샵', '의류잡화', 'DEMO:SAV:2026-07:SHOPPING:004'
    UNION ALL SELECT 'SHOPPING', '2026-07-21', '12:50:00', 19000, '데일리몰', '온라인쇼핑', 'DEMO:SAV:2026-07:SHOPPING:005'
    UNION ALL SELECT 'SHOPPING', '2026-07-26', '15:15:00', 17000, '스타일샵', '의류잡화', 'DEMO:SAV:2026-07:SHOPPING:006'
) demo_transaction
INNER JOIN spending_categories category
        ON category.category_code = demo_transaction.category_code;

-- 적재 결과 확인
SELECT u.login_id,
       sc.category_code,
       sc.category_name,
       COUNT(*) AS transaction_count,
       SUM(t.amount) AS total_amount,
       MAX(t.amount) AS max_single_amount
FROM transactions t
INNER JOIN cards c ON c.id = t.card_id
INNER JOIN users u ON u.id = c.user_id
INNER JOIN spending_categories sc ON sc.id = t.category_id
WHERE u.login_id = 'cadry111'
  AND t.external_key LIKE 'DEMO:SAV:2026-07:%'
  AND t.is_deleted = FALSE
GROUP BY u.login_id, sc.category_code, sc.category_name
ORDER BY total_amount DESC;

-- 필요할 때만 사용하는 정리 SQL
-- DELETE FROM transactions
-- WHERE external_key LIKE 'DEMO:SAV:2026-07:%';

