-- ============================================================================
-- 여행 모드 QA 시드 데이터
--
-- 대상 사용자: tripasstravel (user_id = 22)
-- 금융 로그인: tripasstravel / Mock1234! (MockCodefClient)
-- 여행 경로: 프랑스 파리(8/16~19) → 독일 뮌헨(8/20~22) → 스위스 취리히(8/23~25)
--
-- 사전 조건:
--   1. tripasstravel 계정 가입 완료 (user_id = 22)
--   2. spending_categories_seed.sql 실행 완료
--   3. trip_goal_reference_seed.sql 실행 완료 (통화·국가)
--   4. country_budget_baseline_seed.sql 실행 완료 (독일·프랑스·스위스)
--
-- 실행 순서: 이 파일 실행 → 앱에서 tripasstravel 금융 연동 → 거래 동기화
-- ============================================================================

SET NAMES utf8mb4;
USE tripass;

SET @user_id = 22;

-- ============================================================================
-- 1. 여행 (TRAVELING 상태, 오늘 포함)
-- ============================================================================
INSERT INTO trips (user_id, trip_name, status, start_date, end_date, total_target_amount)
VALUES (@user_id, '유럽 3국 여행', 'TRAVELING', '2026-08-16', '2026-08-25', 5000000);

SET @trip_id = LAST_INSERT_ID();

-- ============================================================================
-- 2. 여행 국가 (프랑스 → 독일 → 스위스)
-- ============================================================================
INSERT INTO trip_countries (trip_id, country_id, arrival_date, departure_date, target_budget, display_order)
VALUES
    (@trip_id, (SELECT id FROM countries WHERE country_name = '프랑스'), '2026-08-16', '2026-08-19', 2000000, 1),
    (@trip_id, (SELECT id FROM countries WHERE country_name = '독일'),   '2026-08-20', '2026-08-22', 1500000, 2),
    (@trip_id, (SELECT id FROM countries WHERE country_name = '스위스'), '2026-08-23', '2026-08-25', 1500000, 3);

SET @tc_france  = (SELECT id FROM trip_countries WHERE trip_id = @trip_id AND display_order = 1);
SET @tc_germany = (SELECT id FROM trip_countries WHERE trip_id = @trip_id AND display_order = 2);
SET @tc_swiss   = (SELECT id FROM trip_countries WHERE trip_id = @trip_id AND display_order = 3);

-- ============================================================================
-- 3. 여행 일정
-- ============================================================================
SET @eur_id = (SELECT id FROM currencies WHERE currency_code = 'EUR');
SET @chf_id = (SELECT id FROM currencies WHERE currency_code = 'CHF');

INSERT INTO trip_schedules (trip_id, trip_country_id, currency_id, schedule_name, scheduled_at, amount, payment_status, schedule_status, place_name, memo)
VALUES
    -- 프랑스
    (@trip_id, @tc_france, @eur_id, '루브르 박물관',        '2026-08-16 10:00:00', 17.00,  'ONSITE',   'DONE',     'Musée du Louvre',          NULL),
    (@trip_id, @tc_france, @eur_id, '에펠탑 전망대',        '2026-08-16 15:00:00', 26.80,  'PREPAID',  'DONE',     'Tour Eiffel',              '온라인 사전 예약'),
    (@trip_id, @tc_france, @eur_id, '몽마르뜨 언덕 산책',   '2026-08-17 09:00:00', NULL,   'UNDECIDED','DONE',     'Montmartre',               NULL),
    (@trip_id, @tc_france, @eur_id, '오르세 미술관',        '2026-08-17 14:00:00', 16.00,  'ONSITE',   'DONE',     'Musée d''Orsay',           NULL),
    (@trip_id, @tc_france, @eur_id, '세느강 유람선',        '2026-08-18 18:00:00', 15.00,  'ONSITE',   'DONE',     'Bateaux Mouches',          NULL),
    (@trip_id, @tc_france, @eur_id, 'CDG 공항 → 뮌헨 이동','2026-08-19 14:00:00', NULL,   'PREPAID',  'DONE',     'CDG Airport',              '유로스타'),
    -- 독일
    (@trip_id, @tc_germany, @eur_id, '마리엔 광장 투어',    '2026-08-20 10:00:00', NULL,   'UNDECIDED','DONE',     'Marienplatz',              NULL),
    (@trip_id, @tc_germany, @eur_id, '님펜부르크 궁전',     '2026-08-21 09:00:00', 8.00,   'ONSITE',   'DONE',     'Schloss Nymphenburg',      NULL),
    (@trip_id, @tc_germany, @eur_id, 'BMW 박물관',          '2026-08-21 14:00:00', 10.00,  'ONSITE',   'UPCOMING', 'BMW Museum',               NULL),
    (@trip_id, @tc_germany, @eur_id, '뮌헨 → 취리히 이동',  '2026-08-22 12:00:00', NULL,   'PREPAID',  'UPCOMING', 'München Hbf',              'DB 기차'),
    -- 스위스
    (@trip_id, @tc_swiss, @chf_id, '취리히 호수 산책',      '2026-08-23 10:00:00', NULL,   'UNDECIDED','UPCOMING', 'Zürichsee',                NULL),
    (@trip_id, @tc_swiss, @chf_id, '융프라우 당일 투어',    '2026-08-24 07:00:00', 230.00, 'PREPAID',  'UPCOMING', 'Jungfraujoch',             '스위스패스 할인'),
    (@trip_id, @tc_swiss, @chf_id, '루체른 구시가 관광',    '2026-08-25 09:00:00', NULL,   'UNDECIDED','UPCOMING', 'Luzern Altstadt',          NULL);

-- ============================================================================
-- 4. 월렛 + 저축 이력 (4~8월 매월 저축)
-- ============================================================================
INSERT INTO wallet (user_id, balance_amount, status, version)
VALUES (@user_id, 1700000, 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE balance_amount = 1700000;

SET @wallet_id = (SELECT id FROM wallet WHERE user_id = @user_id);

INSERT INTO wallet_ledger (wallet_id, direction, transaction_type, transfer_method, amount, balance_before, balance_after, source_type, source_id, target_type, target_id, memo, created_at)
VALUES
    -- 4월 저축
    (@wallet_id, 'IN',  'CHARGE', 'MANUAL',     300000, 0,       300000,  'ACCOUNT', NULL, 'WALLET', @wallet_id, '4월 여행 저축', '2026-04-10 12:00:00'),
    -- 5월 저축
    (@wallet_id, 'IN',  'CHARGE', 'MANUAL',     300000, 300000,  600000,  'ACCOUNT', NULL, 'WALLET', @wallet_id, '5월 여행 저축', '2026-05-10 12:00:00'),
    -- 6월 저축
    (@wallet_id, 'IN',  'CHARGE', 'MANUAL',     350000, 600000,  950000,  'ACCOUNT', NULL, 'WALLET', @wallet_id, '6월 여행 저축', '2026-06-10 12:00:00'),
    -- 7월 저축
    (@wallet_id, 'IN',  'CHARGE', 'MANUAL',     350000, 950000,  1300000, 'ACCOUNT', NULL, 'WALLET', @wallet_id, '7월 여행 저축', '2026-07-10 12:00:00'),
    -- 8월 저축
    (@wallet_id, 'IN',  'CHARGE', 'MANUAL',     400000, 1300000, 1700000, 'ACCOUNT', NULL, 'WALLET', @wallet_id, '8월 여행 저축', '2026-08-10 12:00:00');

-- ============================================================================
-- 5. TRIP 월렛
-- ============================================================================
INSERT INTO trip_wallets (user_id, balance)
VALUES (@user_id, 1700000)
ON DUPLICATE KEY UPDATE balance = 1700000;

-- ============================================================================
-- 6. 저축 계획 (월 350,000원 목표)
-- ============================================================================
INSERT INTO saving_plans (trip_id, monthly_amount, goal_status)
VALUES (@trip_id, 350000, 'ACTIVE');

-- ============================================================================
-- 7. 영수증 (프랑스·독일 — 이미 방문한 나라)
-- ============================================================================
SET @cat_food     = (SELECT id FROM spending_categories WHERE category_code = 'FOOD');
SET @cat_transport= (SELECT id FROM spending_categories WHERE category_code = 'TRANSPORT');
SET @cat_shopping = (SELECT id FROM spending_categories WHERE category_code = 'SHOPPING');
SET @cat_sightsee = (SELECT id FROM spending_categories WHERE category_code = 'SIGHTSEEING');

SET @france_id = (SELECT id FROM countries WHERE country_name = '프랑스');
SET @germany_id= (SELECT id FROM countries WHERE country_name = '독일');

INSERT INTO receipts (user_id, trip_id, country_id, category_id, currency_id, payment_datetime,
                      merchant_original_name, merchant_translated_name, total_amount, status, split_count)
VALUES
    -- 프랑스 영수증
    (@user_id, @trip_id, @france_id, @cat_food,      @eur_id, '2026-08-16 19:00:00', 'LE PETIT BISTRO',     '르 쁘띠 비스트로',   24.50, 'COMPLETED', 1),
    (@user_id, @trip_id, @france_id, @cat_shopping,   @eur_id, '2026-08-16 21:00:00', 'GALERIES LAFAYETTE',  '갤러리 라파예트',    15.30, 'COMPLETED', 1),
    (@user_id, @trip_id, @france_id, @cat_sightsee,   @eur_id, '2026-08-18 09:00:00', 'MUSEE DU LOUVRE',     '루브르 박물관',      17.00, 'COMPLETED', 1),
    (@user_id, @trip_id, @france_id, @cat_food,      @eur_id, '2026-08-18 13:00:00', 'BOULANGERIE PAIN',    '불랑제리 빵집',      19.50, 'COMPLETED', 1),
    (@user_id, @trip_id, @france_id, @cat_transport,  @eur_id, '2026-08-17 11:00:00', 'METRO PARIS RATP',    '파리 메트로',        12.50, 'COMPLETED', 1),
    (@user_id, @trip_id, @france_id, @cat_food,      @eur_id, '2026-08-18 19:30:00', 'LE COMPTOIR PARIS',   '르 콩뚜아르',        31.20, 'COMPLETED', 1),
    -- 독일 영수증
    (@user_id, @trip_id, @germany_id, @cat_food,     @eur_id, '2026-08-20 18:00:00', 'HOFBRAUHAUS MUNCHEN', '호프브로이하우스',    17.50, 'COMPLETED', 1),
    (@user_id, @trip_id, @germany_id, @cat_shopping,  @eur_id, '2026-08-20 20:00:00', 'KAUFHOF MARIENPLATZ', '카우프호프',         22.30, 'COMPLETED', 1),
    (@user_id, @trip_id, @germany_id, @cat_sightsee,  @eur_id, '2026-08-21 09:00:00', 'SCHLOSS NYMPHENBURG', '님펜부르크 궁전',     8.00, 'COMPLETED', 1),
    (@user_id, @trip_id, @germany_id, @cat_food,     @eur_id, '2026-08-21 13:00:00', 'AUGUSTINER KELLER',   '아우구스티너 켈러',   12.80, 'COMPLETED', 1);

-- ============================================================================
-- 8. 영수증 품목 (각 영수증에 대표 품목 1개씩)
-- ============================================================================
INSERT INTO receipt_items (receipt_id, original_name, translated_name, quantity, amount, display_order)
SELECT r.id, item.original_name, item.translated_name, 1, r.total_amount, 1
FROM receipts r
JOIN (
    SELECT '르 쁘띠 비스트로'   AS merchant, 'Steak Dinner'       AS original_name, '스테이크 디너'       AS translated_name UNION ALL
    SELECT '갤러리 라파예트',              'Parfum',                        '향수'                           UNION ALL
    SELECT '루브르 박물관',                'Admission Ticket',              '입장권'                         UNION ALL
    SELECT '불랑제리 빵집',                'Croissant Set',                 '크루아상 세트'                    UNION ALL
    SELECT '파리 메트로',                  'Day Pass',                      '1일권'                          UNION ALL
    SELECT '르 콩뚜아르',                  'French Course Dinner',          '프렌치 코스 디너'                 UNION ALL
    SELECT '호프브로이하우스',              'Schweinshaxe + Bier',           '슈바인학세 + 맥주'                UNION ALL
    SELECT '카우프호프',                   'Souvenir',                      '기념품'                         UNION ALL
    SELECT '님펜부르크 궁전',              'Admission Ticket',              '입장권'                          UNION ALL
    SELECT '아우구스티너 켈러',             'Brezel + Bier',                 '브레첼 + 맥주'
) item ON r.merchant_translated_name = item.merchant
WHERE r.trip_id = @trip_id;
