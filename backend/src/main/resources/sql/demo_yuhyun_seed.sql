-- =====================================================
-- #300 데모 시연용 yuhyun 사용자 Seed
--
-- TRIPass 로그인
--   ID: yuhyun
--   PW: Mock1234!
--
-- Mock 금융기관 로그인(계좌/카드 공통)
--   ID: yuhyun
--   PW: Mock1234!
--
-- 여행: 프랑스(4/4~4/9) → 스위스(4/9~4/14) → 포르투갈(4/14~4/18)
-- 총 저축 목표: 2,843,000원
-- 저축 기간: 2026-08-26 ~ 2027-03-31
-- 월 자동 저축: 420,000원 (매월 5일)
--
-- 선행 기준 데이터:
--   1. schema.sql
--   2. tripass_asset_seed.sql (KB국민은행 0004)
--   3. update_schema_222.sql (KB카드 0301)
--   4. spending_categories_seed.sql
--   5. trip_goal_reference_seed.sql (통화·국가)
--   6. country_budget_baseline_seed.sql (프랑스·스위스·포르투갈)
-- =====================================================

SET NAMES utf8mb4;
USE tripass;
START TRANSACTION;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 0. 기존 yuhyun 데이터 클린업 (idempotent 재실행 보장)
-- ============================================================================
SET @cleanup_uid = (SELECT id FROM users WHERE login_provider = 'LOCAL' AND login_id = 'yuhyun');

-- 여행 관련
DELETE tbr FROM trip_budget_recommendations tbr
  JOIN trip_countries tc ON tbr.trip_country_id = tc.id
  JOIN trips t ON tc.trip_id = t.id
  WHERE @cleanup_uid IS NOT NULL AND t.user_id = @cleanup_uid;
DELETE ts FROM trip_schedules ts
  JOIN trips t ON ts.trip_id = t.id
  WHERE @cleanup_uid IS NOT NULL AND t.user_id = @cleanup_uid;
DELETE pe FROM pre_expenses pe
  JOIN trips t ON pe.trip_id = t.id
  WHERE @cleanup_uid IS NOT NULL AND t.user_id = @cleanup_uid;
DELETE tci FROM trip_checklist_items tci
  JOIN trips t ON tci.trip_id = t.id
  WHERE @cleanup_uid IS NOT NULL AND t.user_id = @cleanup_uid;
DELETE tr FROM trip_reports tr
  JOIN trips t ON tr.trip_id = t.id
  WHERE @cleanup_uid IS NOT NULL AND t.user_id = @cleanup_uid;
DELETE sp FROM saving_plans sp
  JOIN trips t ON sp.trip_id = t.id
  WHERE @cleanup_uid IS NOT NULL AND t.user_id = @cleanup_uid;
DELETE ri FROM receipt_items ri
  JOIN receipts r ON ri.receipt_id = r.id
  WHERE @cleanup_uid IS NOT NULL AND r.user_id = @cleanup_uid;
DELETE rp FROM receipt_participants rp
  JOIN receipts r ON rp.receipt_id = r.id
  WHERE @cleanup_uid IS NOT NULL AND r.user_id = @cleanup_uid;
DELETE FROM receipts WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE txn FROM transactions txn
  LEFT JOIN accounts a ON txn.account_id = a.id
  LEFT JOIN cards c ON txn.card_id = c.id
  WHERE @cleanup_uid IS NOT NULL AND (a.user_id = @cleanup_uid OR c.user_id = @cleanup_uid);
DELETE tc FROM trip_countries tc
  JOIN trips t ON tc.trip_id = t.id
  WHERE @cleanup_uid IS NOT NULL AND t.user_id = @cleanup_uid;
DELETE FROM trips WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE FROM trip_wallets WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;

-- 미션/분석
DELETE wsm FROM weekly_saving_missions wsm
  JOIN monthly_saving_missions msm ON wsm.monthly_saving_mission_id = msm.id
  WHERE @cleanup_uid IS NOT NULL AND msm.user_id = @cleanup_uid;
DELETE FROM monthly_saving_missions WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE FROM mission_category_selections
  WHERE @cleanup_uid IS NOT NULL AND monthly_spending_analysis_id IN (
    SELECT id FROM monthly_spending_analyses WHERE user_id = @cleanup_uid
  );
DELETE mca FROM monthly_category_analyses mca
  JOIN monthly_spending_analyses msa ON mca.monthly_spending_analysis_id = msa.id
  WHERE @cleanup_uid IS NOT NULL AND msa.user_id = @cleanup_uid;
DELETE FROM monthly_spending_analyses WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;

-- 월렛
DELETE wct FROM wallet_card_topup wct
  JOIN wallet w ON wct.wallet_id = w.id
  WHERE @cleanup_uid IS NOT NULL AND w.user_id = @cleanup_uid;
DELETE wet FROM wallet_exchange_transaction wet
  JOIN wallet w ON wet.wallet_id = w.id
  WHERE @cleanup_uid IS NOT NULL AND w.user_id = @cleanup_uid;
DELETE tcl FROM travel_card_ledger tcl
  JOIN wallet_travel_card wtc ON tcl.wallet_travel_card_id = wtc.id
  JOIN wallet w ON wtc.wallet_id = w.id
  WHERE @cleanup_uid IS NOT NULL AND w.user_id = @cleanup_uid;
DELETE tcb FROM travel_card_balance tcb
  JOIN wallet_travel_card wtc ON tcb.wallet_travel_card_id = wtc.id
  JOIN wallet w ON wtc.wallet_id = w.id
  WHERE @cleanup_uid IS NOT NULL AND w.user_id = @cleanup_uid;
DELETE wtc FROM wallet_travel_card wtc
  JOIN wallet w ON wtc.wallet_id = w.id
  WHERE @cleanup_uid IS NOT NULL AND w.user_id = @cleanup_uid;
DELETE wal FROM wallet_auto_saving_logs wal
  JOIN wallet w ON wal.wallet_id = w.id
  WHERE @cleanup_uid IS NOT NULL AND w.user_id = @cleanup_uid;
DELETE FROM wallet_auto_saving_rule
  WHERE @cleanup_uid IS NOT NULL AND wallet_id IN (SELECT id FROM wallet WHERE user_id = @cleanup_uid);
DELETE wl FROM wallet_ledger wl
  JOIN wallet w ON wl.wallet_id = w.id
  WHERE @cleanup_uid IS NOT NULL AND w.user_id = @cleanup_uid;
DELETE wa FROM wallet_account wa
  JOIN wallet w ON wa.wallet_id = w.id
  WHERE @cleanup_uid IS NOT NULL AND w.user_id = @cleanup_uid;
DELETE FROM wallet WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE FROM user_travel_cards WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;

-- 금융 프로필
DELETE FROM category_budgets WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE FROM financial_schedules WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE FROM fixed_expenses WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE FROM income_sources WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;

-- 계좌/카드/Codef
DELETE FROM cards WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE FROM accounts WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE cci FROM codef_connected_institutions cci
  JOIN codef_connections cc ON cci.codef_connection_id = cc.id
  WHERE @cleanup_uid IS NOT NULL AND cc.user_id = @cleanup_uid;
DELETE FROM codef_connections WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;

-- 기타
DELETE FROM exchange_rate_alerts WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE FROM notifications WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;
DELETE FROM wallet_withdraw_recipient WHERE @cleanup_uid IS NOT NULL AND user_id = @cleanup_uid;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 1. 사용자 계정
-- ============================================================================
INSERT INTO users
    (login_id, password, name, phone_number, login_provider, provider_key,
     current_view_mode, is_deleted, deleted_at, created_at, updated_at)
VALUES
    ('yuhyun',
     '$2a$10$pSTZxIVMYVUjMtt2XG5cKujdq3ySpWU2Za2b1raTv9rLuA/m8PQZy',
     '유현',
     '010-9174-0339',
     'LOCAL',
     NULL,
     'SAVING',
     FALSE,
     NULL,
     '2026-08-22 14:00:00',
     '2026-08-22 14:00:00')
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    name = VALUES(name),
    phone_number = VALUES(phone_number),
    current_view_mode = 'SAVING',
    is_deleted = FALSE,
    deleted_at = NULL,
    updated_at = NOW();

SET @user_id = (SELECT id FROM users WHERE login_provider = 'LOCAL' AND login_id = 'yuhyun');

-- ============================================================================
-- 2. 월렛 (가입 시 자동 생성)
-- ============================================================================
INSERT INTO wallet (user_id, balance_amount, status, version, created_at, updated_at)
VALUES (@user_id, 0, 'ACTIVE', 0, '2026-08-22 14:00:00', '2026-08-22 14:00:00')
ON DUPLICATE KEY UPDATE
    status = 'ACTIVE',
    updated_at = NOW();

SET @wallet_id = (SELECT id FROM wallet WHERE user_id = @user_id);

-- ============================================================================
-- 3. CODEF 연동 (금융기관 로그인 완료 상태)
-- ============================================================================
INSERT INTO codef_connections (user_id, connected_id, connection_status, last_synced_at, created_at)
VALUES (@user_id, 'MOCK-CONNECTED-YUHYUN', 'ACTIVE', '2026-08-22 14:30:00', '2026-08-22 14:30:00')
ON DUPLICATE KEY UPDATE connection_status = 'ACTIVE', last_synced_at = NOW();

SET @codef_conn_id = (SELECT id FROM codef_connections WHERE user_id = @user_id AND connected_id = 'MOCK-CONNECTED-YUHYUN');

-- 은행 연동 기관
INSERT INTO codef_connected_institutions (codef_connection_id, organization_code, organization_name, bank_codef_name, business_type, connection_status, created_at)
VALUES (@codef_conn_id, '0004', 'KB국민은행', 'KB국민은행', 'BK', 'CONNECTED', '2026-08-22 14:30:00')
ON DUPLICATE KEY UPDATE connection_status = 'CONNECTED';

-- 카드 연동 기관
INSERT INTO codef_connected_institutions (codef_connection_id, organization_code, organization_name, bank_codef_name, business_type, connection_status, created_at)
VALUES (@codef_conn_id, '0301', 'KB국민카드', 'KB국민카드', 'CD', 'CONNECTED', '2026-08-22 14:30:00')
ON DUPLICATE KEY UPDATE connection_status = 'CONNECTED';

-- ============================================================================
-- 4. 계좌 (KB 종합통장)
-- ============================================================================
INSERT INTO accounts (user_id, codef_connection_id, organization_code, account_name, account_number,
                      account_type, balance, withdrawable_amount, connection_type, is_travel_fund_included,
                      last_synced_at, created_at)
VALUES (@user_id, @codef_conn_id, '0004', 'KB국민은행 종합통장', '496501-01-110300',
        'CHECKING', 990924, 990924, 'CODEF', TRUE,
        '2026-08-22 14:30:00', '2026-08-22 14:30:00')
ON DUPLICATE KEY UPDATE balance = VALUES(balance), withdrawable_amount = VALUES(withdrawable_amount);

SET @account_id = (SELECT id FROM accounts WHERE user_id = @user_id AND account_number = '496501-01-110300');

-- ============================================================================
-- 5. 카드 (nori 체크카드 + 트래블러스 체크카드)
-- ============================================================================
INSERT INTO cards (user_id, codef_connection_id, card_name, masked_card_number, card_type,
                   organization_code, payment_account_number, linked_account_id, last_synced_at, created_at)
VALUES
    (@user_id, @codef_conn_id, 'KB nori 체크카드', '5412-****-****-9901', 'CHECK',
     '0301', '496501-01-110300', @account_id, '2026-08-22 14:30:00', '2026-08-22 14:30:00'),
    (@user_id, @codef_conn_id, 'KB 트래블러스 체크카드', '5412-****-****-9902', 'CHECK',
     '0301', '496501-01-110300', @account_id, '2026-08-22 14:30:00', '2026-08-22 14:30:00')
ON DUPLICATE KEY UPDATE card_name = VALUES(card_name);

-- 트래블러스 체크카드를 travel_cards 마스터와 매칭해 user_travel_cards에 등록
SET @travelcard_master_id = (SELECT id FROM travel_cards WHERE card_name = '트래블러스 체크카드' LIMIT 1);

INSERT INTO user_travel_cards
    (user_id, travel_card_id, card_name, issuer_name, masked_card_number,
     brand_name, card_color, status, external_card_key, is_deleted, created_at, updated_at)
VALUES (@user_id, @travelcard_master_id, '트래블러스 체크카드', 'KB국민카드', '5412-****-****-9902',
        'CODEF', 'BLUE', 'ACTIVE',
        CONCAT('CODEF:0301:5412-****-****-9902'), FALSE, '2026-08-22 14:30:00', '2026-08-22 14:30:00')
ON DUPLICATE KEY UPDATE
    travel_card_id = VALUES(travel_card_id),
    status = 'ACTIVE',
    is_deleted = FALSE,
    deleted_at = NULL,
    updated_at = NOW();

SET @utc_id = (SELECT id FROM user_travel_cards WHERE user_id = @user_id AND external_card_key = 'CODEF:0301:5412-****-****-9902' LIMIT 1);

-- ============================================================================
-- 6. 월렛 연동 계좌
-- ============================================================================
INSERT INTO wallet_account (wallet_id, account_id, is_primary, status)
VALUES (@wallet_id, @account_id, TRUE, 'LINKED')
ON DUPLICATE KEY UPDATE is_primary = TRUE, status = 'LINKED';

-- ============================================================================
-- 7. 월렛 트래블카드 연결 (EUR + CHF)
-- ============================================================================
INSERT INTO wallet_travel_card
    (wallet_id, user_travel_card_id, travel_card_id, card_name, issuer_name, masked_card_number, status)
VALUES
    (@wallet_id, @utc_id, @travelcard_master_id, '트래블러스 체크카드', 'KB국민카드', '5412-****-****-9902', 'LINKED');

SET @wtc_id = LAST_INSERT_ID();

-- 트래블카드 잔액 (EUR, CHF)
INSERT INTO travel_card_balance (wallet_travel_card_id, currency_code, balance_amount, krw_estimated_amount)
VALUES
    (@wtc_id, 'EUR', 0.00, 0),
    (@wtc_id, 'CHF', 0.00, 0);

-- ============================================================================
-- 8. 여행 (PLANNING 상태)
-- ============================================================================
INSERT INTO trips (user_id, trip_name, status, start_date, end_date,
                   total_target_amount,
                   created_at, updated_at)
VALUES (@user_id, '유럽 3국 여행', 'PLANNING', '2027-04-04', '2027-04-18',
        2843000,
        '2026-08-22 15:00:00', '2026-08-22 15:00:00');

SET @trip_id = LAST_INSERT_ID();

-- ============================================================================
-- 9. 여행 국가 (프랑스 → 스위스 → 포르투갈)
-- ============================================================================
INSERT INTO trip_countries (trip_id, country_id, arrival_date, departure_date, target_budget, display_order)
VALUES
    -- 항공·숙박을 제외한 현지 지출 목표만 저장한다.
    (@trip_id, (SELECT id FROM countries WHERE country_name = '프랑스'),   '2027-04-04', '2027-04-09', 1138000, 1),
    (@trip_id, (SELECT id FROM countries WHERE country_name = '스위스'),   '2027-04-09', '2027-04-14', 1170000, 2),
    (@trip_id, (SELECT id FROM countries WHERE country_name = '포르투갈'), '2027-04-14', '2027-04-18', 535000, 3);

SET @tc_france   = (SELECT id FROM trip_countries WHERE trip_id = @trip_id AND display_order = 1);
SET @tc_swiss    = (SELECT id FROM trip_countries WHERE trip_id = @trip_id AND display_order = 2);
SET @tc_portugal = (SELECT id FROM trip_countries WHERE trip_id = @trip_id AND display_order = 3);

-- ============================================================================
-- 10. 여행 예산 추천 (AI 추천 + 사용자 확정: 프랑스 food +100K)
-- ============================================================================
INSERT INTO trip_budget_recommendations
    (trip_country_id, traveler_count, travel_style,
     recommended_airfare_amount, recommended_lodging_amount,
     recommended_activity_amount,
     recommended_food_amount, recommended_other_amount,
     confirmed_airfare_amount, confirmed_lodging_amount,
     confirmed_activity_amount,
     confirmed_food_amount, confirmed_other_amount,
     ai_reason, ai_model, is_confirmed)
VALUES
    -- 프랑스 (6일5박): 항공120+숙소65+관광37.8+식비 추천51→확정61+기타15
    (@tc_france, 1, 'MID_RANGE',
     1200000, 650000, 378000, 510000, 150000,
     1200000, 650000, 378000, 610000, 150000,
     '파리 6일 기준: 항공 120만(왕복 직항), 숙소 13만/박x5, 식비 8.5만/일x6, 관광 6.3만/일x6, 기타 2.5만/일x6',
     'budget-ai-v1', 1),
    -- 스위스 (6일5박): 항공130+숙소100+관광57+식비42+기타18
    (@tc_swiss, 1, 'MID_RANGE',
     1300000, 1000000, 570000, 420000, 180000,
     1300000, 1000000, 570000, 420000, 180000,
     '스위스 6일 기준: 항공 130만(구간 이동 포함), 숙소 20만/박x5, 식비 7만/일x6, 관광 9.5만/일x6, 기타 3만/일x6',
     'budget-ai-v1', 1),
    -- 포르투갈 (5일4박): 항공90+숙소32+관광21+식비25+기타7.5
    (@tc_portugal, 1, 'MID_RANGE',
     900000, 320000, 210000, 250000, 75000,
     900000, 320000, 210000, 250000, 75000,
     '포르투갈 5일 기준: 항공 90만(구간 이동), 숙소 8만/박x4, 식비 5만/일x5, 관광 4.2만/일x5, 기타 1.5만/일x5',
     'budget-ai-v1', 1);

-- ============================================================================
-- 11. 저축 계획
-- ============================================================================
INSERT INTO saving_plans (trip_id, account_id, monthly_amount, goal_status, created_at)
VALUES (@trip_id, @account_id, 420000, 'ACTIVE', '2026-08-26 10:00:00');

-- ============================================================================
-- 12. TRIP 월렛
-- ============================================================================
INSERT INTO trip_wallets (user_id, balance, created_at)
VALUES (@user_id, 0, '2026-08-22 15:00:00')
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================================================
-- 13. 월렛 자동 저축 규칙
-- ============================================================================
INSERT INTO wallet_auto_saving_rule (wallet_id, source_account_id, amount, day_of_month, enabled, next_transfer_date)
VALUES (@wallet_id, @account_id, 420000, 5, TRUE, '2027-04-05');

-- ============================================================================
-- 14. 월렛 저축 이력 (IN: 저축, OUT: 환전)
-- ============================================================================
SET @eur_id = (SELECT id FROM currencies WHERE currency_code = 'EUR');
SET @chf_id = (SELECT id FROM currencies WHERE currency_code = 'CHF');
SET @krw_id = (SELECT id FROM currencies WHERE currency_code = 'KRW');

-- Mock 해외 승인내역을 원화로 환산할 수 있도록 데모 기준 환율을 보장한다.
INSERT INTO exchange_rates
    (base_currency_id, target_currency_id, currency_unit, deal_base_rate, prev_rate, rate_date, fetched_at)
VALUES
    (@krw_id, @eur_id, 1, 1600.00000000, 1610.00000000, '2027-04-12', '2027-04-12 09:00:00'),
    (@krw_id, @chf_id, 1, 1700.00000000, 1690.00000000, '2027-04-12', '2027-04-12 09:00:00')
ON DUPLICATE KEY UPDATE
    deal_base_rate = VALUES(deal_base_rate),
    prev_rate = VALUES(prev_rate),
    fetched_at = VALUES(fetched_at);

INSERT INTO wallet_ledger
    (wallet_id, direction, transaction_type, transfer_method,
     amount, balance_before, balance_after,
     source_type, source_id, target_type, target_id, idempotency_key, memo, created_at)
VALUES
    -- 1. 8/26 수동 저축
    (@wallet_id, 'IN', 'CHARGE', 'MANUAL',
     50000, 0, 50000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-08', '8월 첫 저축', '2026-08-26 12:00:00'),

    -- 2. 9/05 자동 저축
    (@wallet_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     420000, 50000, 470000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-09', '9월 자동 저축 (주)핀테크솔루션', '2026-09-05 09:00:00'),

    -- 3. 10/05 자동 저축
    (@wallet_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     420000, 470000, 890000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-10A', '10월 자동 저축 (주)핀테크솔루션', '2026-10-05 09:00:00'),

    -- 4. 10/08 수동 추가 저축
    (@wallet_id, 'IN', 'CHARGE', 'MANUAL',
     80000, 890000, 970000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-10B', '10월 추가 저축', '2026-10-08 20:00:00'),

    -- 5. 10/10 환전 OUT (EUR 300)
    (@wallet_id, 'OUT', 'CARD_TOPUP', NULL,
     480000, 970000, 490000,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-01', 'EUR 환전 300.00', '2026-10-10 14:00:00'),

    -- 6. 11/05 자동 저축
    (@wallet_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     420000, 490000, 910000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-11', '11월 자동 저축 (주)핀테크솔루션', '2026-11-05 09:00:00'),

    -- 7. 12/05 자동 저축
    (@wallet_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     320000, 910000, 1230000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-12', '12월 자동 저축 (주)핀테크솔루션', '2026-12-05 09:00:00'),

    -- 8. 12/20 환전 OUT (CHF 200)
    (@wallet_id, 'OUT', 'CARD_TOPUP', NULL,
     340000, 1230000, 890000,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-02', 'CHF 환전 200.00', '2026-12-20 14:00:00'),

    -- 9. 1/05 자동 저축
    (@wallet_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     420000, 890000, 1310000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-01A', '1월 자동 저축 (주)핀테크솔루션', '2027-01-05 09:00:00'),

    -- 10. 1/08 수동 추가 저축
    (@wallet_id, 'IN', 'CHARGE', 'MANUAL',
     100000, 1310000, 1410000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-01B', '1월 추가 저축', '2027-01-08 20:00:00'),

    -- 11. 1/20 환전 OUT (EUR 200)
    (@wallet_id, 'OUT', 'CARD_TOPUP', NULL,
     320000, 1410000, 1090000,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-03', 'EUR 환전 200.00', '2027-01-20 14:00:00'),

    -- 12. 2/05 자동 저축
    (@wallet_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     420000, 1090000, 1510000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-02', '2월 자동 저축 (주)핀테크솔루션', '2027-02-05 09:00:00'),

    -- 13. 2/20 환전 OUT (CHF 300)
    (@wallet_id, 'OUT', 'CARD_TOPUP', NULL,
     510000, 1510000, 1000000,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-04', 'CHF 환전 300.00', '2027-02-20 14:00:00'),

    -- 14. 3/05 자동 저축
    (@wallet_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     420000, 1000000, 1420000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-03', '3월 자동 저축 (주)핀테크솔루션', '2027-03-05 09:00:00'),

    -- 15. 3/20 환전 OUT (EUR 200)
    (@wallet_id, 'OUT', 'CARD_TOPUP', NULL,
     320000, 1420000, 1100000,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-05', 'EUR 환전 200.00', '2027-03-20 14:00:00'),

    -- 16. 3/20 환전 OUT (CHF 400)
    (@wallet_id, 'OUT', 'CARD_TOPUP', NULL,
     680000, 1100000, 420000,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-06', 'CHF 환전 400.00', '2027-03-20 14:30:00');

-- ledger ID 참조용
SET @wl_fx01 = (SELECT id FROM wallet_ledger WHERE wallet_id = @wallet_id AND idempotency_key = 'DEMO-SEED-FX-01');
SET @wl_fx02 = (SELECT id FROM wallet_ledger WHERE wallet_id = @wallet_id AND idempotency_key = 'DEMO-SEED-FX-02');
SET @wl_fx03 = (SELECT id FROM wallet_ledger WHERE wallet_id = @wallet_id AND idempotency_key = 'DEMO-SEED-FX-03');
SET @wl_fx04 = (SELECT id FROM wallet_ledger WHERE wallet_id = @wallet_id AND idempotency_key = 'DEMO-SEED-FX-04');
SET @wl_fx05 = (SELECT id FROM wallet_ledger WHERE wallet_id = @wallet_id AND idempotency_key = 'DEMO-SEED-FX-05');
SET @wl_fx06 = (SELECT id FROM wallet_ledger WHERE wallet_id = @wallet_id AND idempotency_key = 'DEMO-SEED-FX-06');

-- ============================================================================
-- 15. 트래블카드 원장 (IN: 환전으로 충전)
-- ============================================================================
INSERT INTO travel_card_ledger
    (wallet_travel_card_id, currency_code, direction, transaction_type,
     foreign_amount, balance_before, balance_after,
     source_type, source_id, target_type, target_id, idempotency_key, memo, created_at)
VALUES
    -- EUR 충전 1: 300
    (@wtc_id, 'EUR', 'IN', 'CARD_TOPUP',
     300.00, 0.00, 300.00,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-01', 'EUR 환전 충전', '2026-10-10 14:00:00'),
    -- CHF 충전 1: 200
    (@wtc_id, 'CHF', 'IN', 'CARD_TOPUP',
     200.00, 0.00, 200.00,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-02', 'CHF 환전 충전', '2026-12-20 14:00:00'),
    -- EUR 충전 2: 200
    (@wtc_id, 'EUR', 'IN', 'CARD_TOPUP',
     200.00, 300.00, 500.00,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-03', 'EUR 환전 충전', '2027-01-20 14:00:00'),
    -- CHF 충전 2: 300
    (@wtc_id, 'CHF', 'IN', 'CARD_TOPUP',
     300.00, 200.00, 500.00,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-04', 'CHF 환전 충전', '2027-02-20 14:00:00'),
    -- EUR 충전 3: 200
    (@wtc_id, 'EUR', 'IN', 'CARD_TOPUP',
     200.00, 500.00, 700.00,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-05', 'EUR 환전 충전', '2027-03-20 14:00:00'),
    -- CHF 충전 3: 400
    (@wtc_id, 'CHF', 'IN', 'CARD_TOPUP',
     400.00, 500.00, 900.00,
     'WALLET', @wallet_id, 'TRAVEL_CARD', @wtc_id,
     'DEMO-SEED-FX-06', 'CHF 환전 충전', '2027-03-20 14:30:00');

-- card ledger ID 참조
SET @cl_fx01 = (SELECT id FROM travel_card_ledger WHERE wallet_travel_card_id = @wtc_id AND idempotency_key = 'DEMO-SEED-FX-01');
SET @cl_fx02 = (SELECT id FROM travel_card_ledger WHERE wallet_travel_card_id = @wtc_id AND idempotency_key = 'DEMO-SEED-FX-02');
SET @cl_fx03 = (SELECT id FROM travel_card_ledger WHERE wallet_travel_card_id = @wtc_id AND idempotency_key = 'DEMO-SEED-FX-03');
SET @cl_fx04 = (SELECT id FROM travel_card_ledger WHERE wallet_travel_card_id = @wtc_id AND idempotency_key = 'DEMO-SEED-FX-04');
SET @cl_fx05 = (SELECT id FROM travel_card_ledger WHERE wallet_travel_card_id = @wtc_id AND idempotency_key = 'DEMO-SEED-FX-05');
SET @cl_fx06 = (SELECT id FROM travel_card_ledger WHERE wallet_travel_card_id = @wtc_id AND idempotency_key = 'DEMO-SEED-FX-06');

-- 트래블카드 잔액 업데이트
UPDATE travel_card_balance SET balance_amount = 700.00, krw_estimated_amount = 1120000
  WHERE wallet_travel_card_id = @wtc_id AND currency_code = 'EUR';
UPDATE travel_card_balance SET balance_amount = 900.00, krw_estimated_amount = 1530000
  WHERE wallet_travel_card_id = @wtc_id AND currency_code = 'CHF';

-- ============================================================================
-- 16. 환전 거래 내역
-- ============================================================================
INSERT INTO wallet_exchange_transaction
    (wallet_id, wallet_travel_card_id, currency_code, exchange_type,
     krw_amount, foreign_amount, base_exchange_rate, applied_exchange_rate,
     fee_rate, fee_amount, status, completed_at, wallet_ledger_id, card_ledger_id)
VALUES
    (@wallet_id, @wtc_id, 'EUR', 'BUY', 480000, 300.00, 1600.00, 1600.00, 0, 0, 'COMPLETED', '2026-10-10 14:00:00', @wl_fx01, @cl_fx01),
    (@wallet_id, @wtc_id, 'CHF', 'BUY', 340000, 200.00, 1700.00, 1700.00, 0, 0, 'COMPLETED', '2026-12-20 14:00:00', @wl_fx02, @cl_fx02),
    (@wallet_id, @wtc_id, 'EUR', 'BUY', 320000, 200.00, 1600.00, 1600.00, 0, 0, 'COMPLETED', '2027-01-20 14:00:00', @wl_fx03, @cl_fx03),
    (@wallet_id, @wtc_id, 'CHF', 'BUY', 510000, 300.00, 1700.00, 1700.00, 0, 0, 'COMPLETED', '2027-02-20 14:00:00', @wl_fx04, @cl_fx04),
    (@wallet_id, @wtc_id, 'EUR', 'BUY', 320000, 200.00, 1600.00, 1600.00, 0, 0, 'COMPLETED', '2027-03-20 14:00:00', @wl_fx05, @cl_fx05),
    (@wallet_id, @wtc_id, 'CHF', 'BUY', 680000, 400.00, 1700.00, 1700.00, 0, 0, 'COMPLETED', '2027-03-20 14:30:00', @wl_fx06, @cl_fx06);

-- ============================================================================
-- 17. 카드 충전 내역
-- ============================================================================
INSERT INTO wallet_card_topup
    (wallet_id, wallet_travel_card_id, currency_code, krw_amount, foreign_amount,
     status, idempotency_key, external_transaction_id, retry_count, last_tried_at, completed_at,
     wallet_ledger_id, card_ledger_id, refunded)
VALUES
    (@wallet_id, @wtc_id, 'EUR', 480000, 300.00, 'COMPLETED', 'DEMO-SEED-FX-01', 'EXT-FX-01', 1, '2026-10-10 14:00:00', '2026-10-10 14:00:00', @wl_fx01, @cl_fx01, 0),
    (@wallet_id, @wtc_id, 'CHF', 340000, 200.00, 'COMPLETED', 'DEMO-SEED-FX-02', 'EXT-FX-02', 1, '2026-12-20 14:00:00', '2026-12-20 14:00:00', @wl_fx02, @cl_fx02, 0),
    (@wallet_id, @wtc_id, 'EUR', 320000, 200.00, 'COMPLETED', 'DEMO-SEED-FX-03', 'EXT-FX-03', 1, '2027-01-20 14:00:00', '2027-01-20 14:00:00', @wl_fx03, @cl_fx03, 0),
    (@wallet_id, @wtc_id, 'CHF', 510000, 300.00, 'COMPLETED', 'DEMO-SEED-FX-04', 'EXT-FX-04', 1, '2027-02-20 14:00:00', '2027-02-20 14:00:00', @wl_fx04, @cl_fx04, 0),
    (@wallet_id, @wtc_id, 'EUR', 320000, 200.00, 'COMPLETED', 'DEMO-SEED-FX-05', 'EXT-FX-05', 1, '2027-03-20 14:00:00', '2027-03-20 14:00:00', @wl_fx05, @cl_fx05, 0),
    (@wallet_id, @wtc_id, 'CHF', 680000, 400.00, 'COMPLETED', 'DEMO-SEED-FX-06', 'EXT-FX-06', 1, '2027-03-20 14:30:00', '2027-03-20 14:30:00', @wl_fx06, @cl_fx06, 0);

-- ============================================================================
-- 18. 여행 일정 (~90개)
-- ============================================================================
INSERT INTO trip_schedules
    (trip_id, trip_country_id, currency_id, schedule_name, scheduled_at,
     amount, payment_status, schedule_status, place_name, memo)
VALUES
    -- === 프랑스 Day 1 (4/4) ===
    (@trip_id, @tc_france, @eur_id, '루브르 박물관',          '2027-04-04 10:00:00', 17.00,  'ONSITE',    'UPCOMING', 'Musee du Louvre',        NULL),
    (@trip_id, @tc_france, @eur_id, '에펠탑 전망대',          '2027-04-04 14:00:00', 26.80,  'PREPAID',   'UPCOMING', 'Tour Eiffel',            '온라인 사전 예약'),
    (@trip_id, @tc_france, @eur_id, '튈르리 정원 산책',       '2027-04-04 16:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Jardin des Tuileries',   NULL),
    (@trip_id, @tc_france, @eur_id, '샹젤리제 거리',          '2027-04-04 17:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Avenue des Champs-Elysees', NULL),
    (@trip_id, @tc_france, @eur_id, '개선문',                 '2027-04-04 18:30:00', 16.00,  'ONSITE',    'UPCOMING', 'Arc de Triomphe',        NULL),
    (@trip_id, @tc_france, @eur_id, '세느강 야경',            '2027-04-04 21:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Seine River',            NULL),
    -- === 프랑스 Day 2 (4/5) ===
    (@trip_id, @tc_france, @eur_id, '몽마르뜨 언덕',          '2027-04-05 09:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Montmartre',             NULL),
    (@trip_id, @tc_france, @eur_id, '사크레쾨르 대성당',       '2027-04-05 10:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Sacre-Coeur',            NULL),
    (@trip_id, @tc_france, @eur_id, '오르세 미술관',           '2027-04-05 13:00:00', 16.00,  'ONSITE',    'UPCOMING', 'Musee d''Orsay',         NULL),
    (@trip_id, @tc_france, @eur_id, '생제르맹 데프레',         '2027-04-05 16:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Saint-Germain-des-Pres', NULL),
    (@trip_id, @tc_france, @eur_id, '노트르담 외관',           '2027-04-05 17:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Notre-Dame de Paris',    '복원 중 외관만'),
    (@trip_id, @tc_france, @eur_id, '라틴지구 저녁',           '2027-04-05 19:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Quartier Latin',         NULL),
    -- === 프랑스 Day 3 (4/6) ===
    (@trip_id, @tc_france, @eur_id, '베르사유 궁전',           '2027-04-06 09:00:00', 21.00,  'PREPAID',   'UPCOMING', 'Chateau de Versailles',  '사전 예약 필수'),
    (@trip_id, @tc_france, @eur_id, '베르사유 정원',           '2027-04-06 12:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Jardins de Versailles',  NULL),
    (@trip_id, @tc_france, @eur_id, '마레 지구',              '2027-04-06 15:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Le Marais',              NULL),
    (@trip_id, @tc_france, @eur_id, '퐁피두 센터',            '2027-04-06 16:30:00', 15.00,  'ONSITE',    'UPCOMING', 'Centre Pompidou',        NULL),
    (@trip_id, @tc_france, @eur_id, '바스티유 광장',           '2027-04-06 18:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Place de la Bastille',   NULL),
    -- === 프랑스 Day 4 (4/7) ===
    (@trip_id, @tc_france, @eur_id, '오랑주리 미술관',         '2027-04-07 10:00:00', 12.50,  'ONSITE',    'UPCOMING', 'Musee de l''Orangerie',  NULL),
    (@trip_id, @tc_france, @eur_id, '콩코드 광장',            '2027-04-07 11:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Place de la Concorde',   NULL),
    (@trip_id, @tc_france, @eur_id, '마들렌 성당',            '2027-04-07 12:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Eglise de la Madeleine', NULL),
    (@trip_id, @tc_france, @eur_id, '갤러리 라파예트',         '2027-04-07 14:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Galeries Lafayette',     NULL),
    (@trip_id, @tc_france, @eur_id, '오페라 가르니에',         '2027-04-07 16:00:00', 14.00,  'ONSITE',    'UPCOMING', 'Palais Garnier',         NULL),
    (@trip_id, @tc_france, @eur_id, '세느강 유람선',           '2027-04-07 19:00:00', 15.00,  'ONSITE',    'UPCOMING', 'Bateaux Mouches',        NULL),
    -- === 프랑스 Day 5 (4/8) ===
    (@trip_id, @tc_france, @eur_id, '생 마르탱 운하',          '2027-04-08 09:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Canal Saint-Martin',     NULL),
    (@trip_id, @tc_france, @eur_id, '뷔트 쇼몽 공원',         '2027-04-08 10:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Parc des Buttes-Chaumont', NULL),
    (@trip_id, @tc_france, @eur_id, '벨빌 전망대',            '2027-04-08 12:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Parc de Belleville',     NULL),
    (@trip_id, @tc_france, @eur_id, '페르 라셰즈 묘지',        '2027-04-08 14:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Pere Lachaise',          NULL),
    (@trip_id, @tc_france, @eur_id, '바토 무슈',              '2027-04-08 18:00:00', 15.00,  'ONSITE',    'UPCOMING', 'Bateaux Mouches',        '석양 크루즈'),
    -- === 프랑스 Day 6 / 스위스 Day 1 (4/9) ===
    (@trip_id, @tc_france, @eur_id, '파리 → 취리히 이동',      '2027-04-09 08:00:00', NULL,   'PREPAID',   'UPCOMING', 'Gare de Lyon',           'TGV Lyria'),
    (@trip_id, @tc_swiss,  @chf_id, '반호프 거리',            '2027-04-09 14:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Bahnhofstrasse',         NULL),
    (@trip_id, @tc_swiss,  @chf_id, '취리히 호수 산책',        '2027-04-09 15:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Zurichsee',              NULL),
    (@trip_id, @tc_swiss,  @chf_id, '린덴호프 전망대',         '2027-04-09 17:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Lindenhof',              NULL),
    (@trip_id, @tc_swiss,  @chf_id, '그로스뮌스터 성당',       '2027-04-09 18:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Grossmunster',           NULL),
    -- === 스위스 Day 2 (4/10) ===
    (@trip_id, @tc_swiss,  @chf_id, '융프라우 당일 투어',      '2027-04-10 07:00:00', 230.00, 'PREPAID',   'UPCOMING', 'Jungfraujoch',           '스위스패스 할인'),
    (@trip_id, @tc_swiss,  @chf_id, '클라이네 샤이덱',         '2027-04-10 11:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Kleine Scheidegg',       NULL),
    (@trip_id, @tc_swiss,  @chf_id, '아이거 글레처',           '2027-04-10 13:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Eigergletscher',         NULL),
    (@trip_id, @tc_swiss,  @chf_id, '인터라켄 저녁',           '2027-04-10 18:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Interlaken',             NULL),
    -- === 스위스 Day 3 (4/11) ===
    (@trip_id, @tc_swiss,  @chf_id, '루체른 카펠교',           '2027-04-11 09:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Kapellbrucke',           NULL),
    (@trip_id, @tc_swiss,  @chf_id, '빈사의 사자상',           '2027-04-11 10:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Lowendenkmal',           NULL),
    (@trip_id, @tc_swiss,  @chf_id, '필라투스산 전망대',        '2027-04-11 12:00:00', 72.00,  'ONSITE',    'UPCOMING', 'Mount Pilatus',          NULL),
    (@trip_id, @tc_swiss,  @chf_id, '루체른 호수 유람',        '2027-04-11 15:00:00', 45.00,  'ONSITE',    'UPCOMING', 'Lake Lucerne',           NULL),
    (@trip_id, @tc_swiss,  @chf_id, '루체른 구시가 저녁',       '2027-04-11 18:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Luzern Altstadt',        NULL),
    -- === 스위스 Day 4 (4/12) ===
    (@trip_id, @tc_swiss,  @chf_id, '인터라켄 패러글라이딩',    '2027-04-12 10:00:00', 180.00, 'ONSITE',    'UPCOMING', 'Interlaken',             NULL),
    (@trip_id, @tc_swiss,  @chf_id, '하더 쿨름',              '2027-04-12 14:00:00', 32.00,  'ONSITE',    'UPCOMING', 'Harder Kulm',            NULL),
    (@trip_id, @tc_swiss,  @chf_id, '브리엔츠 호수',           '2027-04-12 16:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Brienzersee',            NULL),
    (@trip_id, @tc_swiss,  @chf_id, '인터라켄 마을 산책',       '2027-04-12 18:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Interlaken Ost',         NULL),
    -- === 스위스 Day 5 (4/13) ===
    (@trip_id, @tc_swiss,  @chf_id, '베른 구시가',             '2027-04-13 09:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Bern Altstadt',          NULL),
    (@trip_id, @tc_swiss,  @chf_id, '시계탑',                 '2027-04-13 10:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Zytglogge',              NULL),
    (@trip_id, @tc_swiss,  @chf_id, '곰 공원',               '2027-04-13 11:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Barenpark',              NULL),
    (@trip_id, @tc_swiss,  @chf_id, '아레강 전망',             '2027-04-13 12:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Aare River Viewpoint',   NULL),
    (@trip_id, @tc_swiss,  @chf_id, '장미 정원',              '2027-04-13 13:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Rosengarten',            NULL),
    -- === 스위스 Day 6 / 포르투갈 Day 1 (4/14) ===
    (@trip_id, @tc_swiss,  @chf_id, '취리히 → 리스본 이동',    '2027-04-14 08:00:00', NULL,   'PREPAID',   'UPCOMING', 'Zurich Airport',         'TAP Air Portugal'),
    (@trip_id, @tc_portugal, @eur_id, '벨렝탑',               '2027-04-14 14:00:00', 8.00,   'ONSITE',    'UPCOMING', 'Torre de Belem',         NULL),
    (@trip_id, @tc_portugal, @eur_id, '제로니무스 수도원',      '2027-04-14 15:30:00', 10.00,  'ONSITE',    'UPCOMING', 'Mosteiro dos Jeronimos', NULL),
    (@trip_id, @tc_portugal, @eur_id, '발견의 기념비',         '2027-04-14 17:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Padrao dos Descobrimentos', NULL),
    (@trip_id, @tc_portugal, @eur_id, '파스테이스 드 벨렝',     '2027-04-14 17:30:00', 5.00,   'ONSITE',    'UPCOMING', 'Pasteis de Belem',       NULL),
    (@trip_id, @tc_portugal, @eur_id, '코메르시우 광장',        '2027-04-14 19:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Praca do Comercio',      NULL),
    -- === 포르투갈 Day 2 (4/15) ===
    (@trip_id, @tc_portugal, @eur_id, '리스본 트램 28번',       '2027-04-15 09:00:00', 3.00,   'ONSITE',    'UPCOMING', 'Tram 28',                NULL),
    (@trip_id, @tc_portugal, @eur_id, '알파마 지구',            '2027-04-15 10:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Alfama',                 NULL),
    (@trip_id, @tc_portugal, @eur_id, '상 조르즈 성',           '2027-04-15 11:30:00', 10.00,  'ONSITE',    'UPCOMING', 'Castelo de Sao Jorge',   NULL),
    (@trip_id, @tc_portugal, @eur_id, '바이후 알투',            '2027-04-15 14:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Bairro Alto',            NULL),
    (@trip_id, @tc_portugal, @eur_id, '타임아웃 마켓',          '2027-04-15 18:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Time Out Market',        NULL),
    -- === 포르투갈 Day 3 (4/16) ===
    (@trip_id, @tc_portugal, @eur_id, '신트라 페나 궁전',       '2027-04-16 09:00:00', 14.00,  'ONSITE',    'UPCOMING', 'Palacio da Pena',        '리스본 근교'),
    (@trip_id, @tc_portugal, @eur_id, '무어 성',              '2027-04-16 11:00:00', 8.00,   'ONSITE',    'UPCOMING', 'Castelo dos Mouros',     NULL),
    (@trip_id, @tc_portugal, @eur_id, '헤갈레이라 별장',        '2027-04-16 13:00:00', 10.00,  'ONSITE',    'UPCOMING', 'Quinta da Regaleira',    NULL),
    (@trip_id, @tc_portugal, @eur_id, '로카 곶',              '2027-04-16 15:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Cabo da Roca',           '유럽 최서단'),
    (@trip_id, @tc_portugal, @eur_id, '카스카이스',            '2027-04-16 17:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Cascais',                NULL),
    -- === 포르투갈 Day 4 (4/17) ===
    (@trip_id, @tc_portugal, @eur_id, '상벤투 역',             '2027-04-17 09:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Estacao de Sao Bento',   '포르투 당일치기'),
    (@trip_id, @tc_portugal, @eur_id, '헤이스 서점',            '2027-04-17 10:00:00', 5.00,   'ONSITE',    'UPCOMING', 'Livraria Lello',         NULL),
    (@trip_id, @tc_portugal, @eur_id, '동 루이스 1세 다리',     '2027-04-17 11:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Ponte Dom Luis I',       NULL),
    (@trip_id, @tc_portugal, @eur_id, '히베이라 지구',          '2027-04-17 12:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Ribeira',                NULL),
    (@trip_id, @tc_portugal, @eur_id, '클레리구스 탑',          '2027-04-17 14:00:00', 6.00,   'ONSITE',    'UPCOMING', 'Torre dos Clerigos',     NULL),
    -- === 포르투갈 Day 5 (4/18) ===
    (@trip_id, @tc_portugal, @eur_id, 'LX Factory',           '2027-04-18 09:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'LX Factory',             NULL),
    (@trip_id, @tc_portugal, @eur_id, '크리스투 헤이 전망대',    '2027-04-18 11:00:00', 6.00,   'ONSITE',    'UPCOMING', 'Cristo Rei',             NULL),
    (@trip_id, @tc_portugal, @eur_id, '4월 25일 다리 뷰',      '2027-04-18 12:30:00', NULL,   'UNDECIDED', 'UPCOMING', 'Ponte 25 de Abril',      NULL),
    (@trip_id, @tc_portugal, @eur_id, '공항 이동',             '2027-04-18 16:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Aeroporto de Lisboa',    NULL);

-- ============================================================================
-- 19. 월간 소비 분석 (7개월: 2026-08 ~ 2027-02)
-- ============================================================================
-- analysis_year_month = 분석 대상 월(지난달), target_year_month = 미션 적용 월(이번달)
INSERT INTO monthly_spending_analyses
    (user_id, analysis_year_month, target_year_month, total_spending, report_status, created_at)
VALUES
    (@user_id, '2026-08', '2026-09', 1350000, 'CLOSED', '2026-09-01 08:00:00'),
    (@user_id, '2026-09', '2026-10', 1420000, 'CLOSED', '2026-10-01 08:00:00'),
    (@user_id, '2026-10', '2026-11', 1380000, 'CLOSED', '2026-11-01 08:00:00'),
    (@user_id, '2026-11', '2026-12', 1290000, 'CLOSED', '2026-12-01 08:00:00'),
    (@user_id, '2026-12', '2027-01', 1450000, 'CLOSED', '2027-01-01 08:00:00'),
    (@user_id, '2027-01', '2027-02', 1310000, 'CLOSED', '2027-02-01 08:00:00'),
    (@user_id, '2027-02', '2027-03', 1360000, 'CLOSED', '2027-03-01 08:00:00');

SET @msa_08 = (SELECT id FROM monthly_spending_analyses WHERE user_id = @user_id AND analysis_year_month = '2026-08');
SET @msa_09 = (SELECT id FROM monthly_spending_analyses WHERE user_id = @user_id AND analysis_year_month = '2026-09');
SET @msa_10 = (SELECT id FROM monthly_spending_analyses WHERE user_id = @user_id AND analysis_year_month = '2026-10');
SET @msa_11 = (SELECT id FROM monthly_spending_analyses WHERE user_id = @user_id AND analysis_year_month = '2026-11');
SET @msa_12 = (SELECT id FROM monthly_spending_analyses WHERE user_id = @user_id AND analysis_year_month = '2026-12');
SET @msa_01 = (SELECT id FROM monthly_spending_analyses WHERE user_id = @user_id AND analysis_year_month = '2027-01');
SET @msa_02 = (SELECT id FROM monthly_spending_analyses WHERE user_id = @user_id AND analysis_year_month = '2027-02');

-- ============================================================================
-- 20. 월간 카테고리별 분석 (각 분석당 7개 카테고리)
-- Categories: FOOD(1), TRANSPORT(2), LODGING(3), SHOPPING(4), SIGHTSEEING(5), OTHER(6), CAFE(7), LIVING(8), LEISURE(9)
-- ============================================================================
INSERT INTO monthly_category_analyses
    (monthly_spending_analysis_id, category_id, spending_amount, spending_ratio, transaction_count,
     mission_period_spending, mission_transaction_count)
VALUES
    -- Aug (total 1,350,000)
    (@msa_08, 1, 607000, 44.96, 45, 560000, 40),
    (@msa_08, 2, 108000, 8.00, 22, 100000, 20),
    (@msa_08, 4, 162000, 12.00, 8, 150000, 7),
    (@msa_08, 6, 67000, 4.96, 5, 62000, 4),
    (@msa_08, 7, 68000, 5.04, 15, 63000, 14),
    (@msa_08, 8, 270000, 20.00, 12, 250000, 11),
    (@msa_08, 9, 68000, 5.04, 4, 63000, 3),
    -- Sep (total 1,420,000)
    (@msa_09, 1, 639000, 45.00, 48, 590000, 43),
    (@msa_09, 2, 114000, 8.03, 24, 105000, 22),
    (@msa_09, 4, 170000, 11.97, 9, 157000, 8),
    (@msa_09, 6, 71000, 5.00, 6, 66000, 5),
    (@msa_09, 7, 71000, 5.00, 16, 66000, 14),
    (@msa_09, 8, 284000, 20.00, 13, 262000, 12),
    (@msa_09, 9, 71000, 5.00, 5, 66000, 4),
    -- Oct (total 1,380,000)
    (@msa_10, 1, 621000, 45.00, 46, 574000, 42),
    (@msa_10, 2, 110000, 7.97, 23, 102000, 21),
    (@msa_10, 4, 166000, 12.03, 8, 153000, 7),
    (@msa_10, 6, 69000, 5.00, 5, 64000, 4),
    (@msa_10, 7, 69000, 5.00, 15, 64000, 13),
    (@msa_10, 8, 276000, 20.00, 12, 255000, 11),
    (@msa_10, 9, 69000, 5.00, 4, 64000, 3),
    -- Nov (total 1,290,000)
    (@msa_11, 1, 581000, 45.04, 43, 536000, 39),
    (@msa_11, 2, 103000, 7.98, 21, 95000, 19),
    (@msa_11, 4, 155000, 12.02, 7, 143000, 6),
    (@msa_11, 6, 64000, 4.96, 5, 59000, 4),
    (@msa_11, 7, 64000, 4.96, 14, 59000, 12),
    (@msa_11, 8, 258000, 20.00, 11, 238000, 10),
    (@msa_11, 9, 65000, 5.04, 4, 60000, 3),
    -- Dec (total 1,450,000)
    (@msa_12, 1, 652000, 44.97, 50, 602000, 45),
    (@msa_12, 2, 116000, 8.00, 25, 107000, 23),
    (@msa_12, 4, 174000, 12.00, 10, 161000, 9),
    (@msa_12, 6, 73000, 5.03, 6, 67000, 5),
    (@msa_12, 7, 73000, 5.03, 17, 67000, 15),
    (@msa_12, 8, 290000, 20.00, 14, 268000, 13),
    (@msa_12, 9, 72000, 4.97, 5, 67000, 4),
    -- Jan (total 1,310,000)
    (@msa_01, 1, 590000, 45.04, 44, 545000, 40),
    (@msa_01, 2, 105000, 8.02, 22, 97000, 20),
    (@msa_01, 4, 157000, 11.98, 8, 145000, 7),
    (@msa_01, 6, 65000, 4.96, 5, 60000, 4),
    (@msa_01, 7, 65000, 4.96, 14, 60000, 12),
    (@msa_01, 8, 262000, 20.00, 12, 242000, 11),
    (@msa_01, 9, 66000, 5.04, 4, 61000, 3),
    -- Feb (total 1,360,000)
    (@msa_02, 1, 612000, 45.00, 45, 565000, 41),
    (@msa_02, 2, 109000, 8.01, 23, 101000, 21),
    (@msa_02, 4, 163000, 11.99, 9, 151000, 8),
    (@msa_02, 6, 68000, 5.00, 5, 63000, 4),
    (@msa_02, 7, 68000, 5.00, 15, 63000, 13),
    (@msa_02, 8, 272000, 20.00, 12, 251000, 11),
    (@msa_02, 9, 68000, 5.00, 4, 63000, 3);

-- ============================================================================
-- 21. 미션 카테고리 선택 (매월 FOOD=30% 절감)
-- ============================================================================
INSERT INTO mission_category_selections
    (monthly_spending_analysis_id, category_id, reduction_rate,
     baseline_spending_amount, monthly_reduction_target, monthly_usage_target)
VALUES
    (@msa_08, 1, 30, 560000, 168000, 392000),
    (@msa_09, 1, 30, 590000, 177000, 413000),
    (@msa_10, 1, 30, 574000, 172200, 401800),
    (@msa_11, 1, 30, 536000, 160800, 375200),
    (@msa_12, 1, 30, 602000, 180600, 421400),
    (@msa_01, 1, 30, 545000, 163500, 381500),
    (@msa_02, 1, 30, 565000, 169500, 395500);

SET @mcs_08 = (SELECT id FROM mission_category_selections WHERE monthly_spending_analysis_id = @msa_08 AND category_id = 1);
SET @mcs_09 = (SELECT id FROM mission_category_selections WHERE monthly_spending_analysis_id = @msa_09 AND category_id = 1);
SET @mcs_10 = (SELECT id FROM mission_category_selections WHERE monthly_spending_analysis_id = @msa_10 AND category_id = 1);
SET @mcs_11 = (SELECT id FROM mission_category_selections WHERE monthly_spending_analysis_id = @msa_11 AND category_id = 1);
SET @mcs_12 = (SELECT id FROM mission_category_selections WHERE monthly_spending_analysis_id = @msa_12 AND category_id = 1);
SET @mcs_01 = (SELECT id FROM mission_category_selections WHERE monthly_spending_analysis_id = @msa_01 AND category_id = 1);
SET @mcs_02 = (SELECT id FROM mission_category_selections WHERE monthly_spending_analysis_id = @msa_02 AND category_id = 1);

-- ============================================================================
-- 22. 월간 절감 미션 (7개월)
-- ============================================================================
INSERT INTO monthly_saving_missions
    (user_id, monthly_spending_analysis_id, mission_category_selection_id, category_id,
     target_year_month, reduction_rate, baseline_spending_amount,
     monthly_reduction_target, monthly_usage_target, planned_saving_amount, start_week, status, created_at)
VALUES
    (@user_id, @msa_08, @mcs_08, 1, '2026-09', 30, 560000, 168000, 392000, 168000, 1, 'COMPLETED', '2026-09-01 08:00:00'),
    (@user_id, @msa_09, @mcs_09, 1, '2026-10', 30, 590000, 177000, 413000, 177000, 1, 'COMPLETED', '2026-10-01 08:00:00'),
    (@user_id, @msa_10, @mcs_10, 1, '2026-11', 30, 574000, 172200, 401800, 172200, 1, 'COMPLETED', '2026-11-01 08:00:00'),
    (@user_id, @msa_11, @mcs_11, 1, '2026-12', 30, 536000, 160800, 375200, 160800, 1, 'COMPLETED', '2026-12-01 08:00:00'),
    (@user_id, @msa_12, @mcs_12, 1, '2027-01', 30, 602000, 180600, 421400, 180600, 1, 'COMPLETED', '2027-01-01 08:00:00'),
    (@user_id, @msa_01, @mcs_01, 1, '2027-02', 30, 545000, 163500, 381500, 163500, 1, 'COMPLETED', '2027-02-01 08:00:00'),
    (@user_id, @msa_02, @mcs_02, 1, '2027-03', 30, 565000, 169500, 395500, 169500, 1, 'IN_PROGRESS', '2027-03-01 08:00:00');

SET @msm_09 = (SELECT id FROM monthly_saving_missions WHERE user_id = @user_id AND target_year_month = '2026-09');
SET @msm_10 = (SELECT id FROM monthly_saving_missions WHERE user_id = @user_id AND target_year_month = '2026-10');
SET @msm_11 = (SELECT id FROM monthly_saving_missions WHERE user_id = @user_id AND target_year_month = '2026-11');
SET @msm_12 = (SELECT id FROM monthly_saving_missions WHERE user_id = @user_id AND target_year_month = '2026-12');
SET @msm_01 = (SELECT id FROM monthly_saving_missions WHERE user_id = @user_id AND target_year_month = '2027-01');
SET @msm_02 = (SELECT id FROM monthly_saving_missions WHERE user_id = @user_id AND target_year_month = '2027-02');
SET @msm_03 = (SELECT id FROM monthly_saving_missions WHERE user_id = @user_id AND target_year_month = '2027-03');

-- ============================================================================
-- 23. 주간 절감 미션 (4주 x 7개월 = 28)
-- weekly_usage_limit = baseline * 0.70 / 4
-- SUCCESS: actual_spending ~ limit * 0.85, actual_saving = (baseline/4) - actual_spending
-- FAILED: actual_spending ~ limit * 1.15, actual_saving = 0
-- ============================================================================
INSERT INTO weekly_saving_missions
    (monthly_saving_mission_id, week_number, period_start_date, period_end_date,
     weekly_usage_limit, weekly_expected_saving, actual_spending, actual_saving, status, evaluated_at)
VALUES
    -- Sep (baseline 560000, limit=98000/wk): S,S,F,S
    (@msm_09, 1, '2026-09-01', '2026-09-07', 98000, 42000, 82000, 58000, 'SUCCESS', '2026-09-08 00:00:00'),
    (@msm_09, 2, '2026-09-08', '2026-09-14', 98000, 42000, 88000, 52000, 'SUCCESS', '2026-09-15 00:00:00'),
    (@msm_09, 3, '2026-09-15', '2026-09-21', 98000, 42000, 115000, 0, 'FAILED', '2026-09-22 00:00:00'),
    (@msm_09, 4, '2026-09-22', '2026-09-28', 98000, 42000, 85000, 55000, 'SUCCESS', '2026-09-29 00:00:00'),
    -- Oct (baseline 590000, limit=103250/wk): S,S,S,F
    (@msm_10, 1, '2026-10-01', '2026-10-07', 103250, 44250, 88000, 59500, 'SUCCESS', '2026-10-08 00:00:00'),
    (@msm_10, 2, '2026-10-08', '2026-10-14', 103250, 44250, 92000, 55500, 'SUCCESS', '2026-10-15 00:00:00'),
    (@msm_10, 3, '2026-10-15', '2026-10-21', 103250, 44250, 85000, 62500, 'SUCCESS', '2026-10-22 00:00:00'),
    (@msm_10, 4, '2026-10-22', '2026-10-28', 103250, 44250, 120000, 0, 'FAILED', '2026-10-29 00:00:00'),
    -- Nov (baseline 574000, limit=100450/wk): S,F,S,S
    (@msm_11, 1, '2026-11-01', '2026-11-07', 100450, 43050, 86000, 57500, 'SUCCESS', '2026-11-08 00:00:00'),
    (@msm_11, 2, '2026-11-08', '2026-11-14', 100450, 43050, 118000, 0, 'FAILED', '2026-11-15 00:00:00'),
    (@msm_11, 3, '2026-11-15', '2026-11-21', 100450, 43050, 82000, 61500, 'SUCCESS', '2026-11-22 00:00:00'),
    (@msm_11, 4, '2026-11-22', '2026-11-28', 100450, 43050, 90000, 53500, 'SUCCESS', '2026-11-29 00:00:00'),
    -- Dec (baseline 536000, limit=93800/wk): F,S,S,S
    (@msm_12, 1, '2026-12-01', '2026-12-07', 93800, 40200, 112000, 0, 'FAILED', '2026-12-08 00:00:00'),
    (@msm_12, 2, '2026-12-08', '2026-12-14', 93800, 40200, 78000, 56000, 'SUCCESS', '2026-12-15 00:00:00'),
    (@msm_12, 3, '2026-12-15', '2026-12-21', 93800, 40200, 82000, 52000, 'SUCCESS', '2026-12-22 00:00:00'),
    (@msm_12, 4, '2026-12-22', '2026-12-28', 93800, 40200, 75000, 59000, 'SUCCESS', '2026-12-29 00:00:00'),
    -- Jan (baseline 602000, limit=105350/wk): S,S,S,S
    (@msm_01, 1, '2027-01-01', '2027-01-07', 105350, 45150, 89000, 61500, 'SUCCESS', '2027-01-08 00:00:00'),
    (@msm_01, 2, '2027-01-08', '2027-01-14', 105350, 45150, 92000, 58500, 'SUCCESS', '2027-01-15 00:00:00'),
    (@msm_01, 3, '2027-01-15', '2027-01-21', 105350, 45150, 85000, 65500, 'SUCCESS', '2027-01-22 00:00:00'),
    (@msm_01, 4, '2027-01-22', '2027-01-28', 105350, 45150, 95000, 55500, 'SUCCESS', '2027-01-29 00:00:00'),
    -- Feb (baseline 545000, limit=95375/wk): S,S,F,S
    (@msm_02, 1, '2027-02-01', '2027-02-07', 95375, 40875, 80000, 56250, 'SUCCESS', '2027-02-08 00:00:00'),
    (@msm_02, 2, '2027-02-08', '2027-02-14', 95375, 40875, 83000, 53250, 'SUCCESS', '2027-02-15 00:00:00'),
    (@msm_02, 3, '2027-02-15', '2027-02-21', 95375, 40875, 112000, 0, 'FAILED', '2027-02-22 00:00:00'),
    (@msm_02, 4, '2027-02-22', '2027-02-28', 95375, 40875, 78000, 58250, 'SUCCESS', '2027-03-01 00:00:00'),
    -- Mar (baseline 565000, limit=98875/wk): S,S (only weeks 1-2 completed before trip)
    (@msm_03, 1, '2027-03-01', '2027-03-07', 98875, 42375, 82000, 59250, 'SUCCESS', '2027-03-08 00:00:00'),
    (@msm_03, 2, '2027-03-08', '2027-03-14', 98875, 42375, 86000, 55250, 'SUCCESS', '2027-03-15 00:00:00'),
    (@msm_03, 3, '2027-03-15', '2027-03-21', 98875, 42375, NULL, NULL, 'PENDING', NULL),
    (@msm_03, 4, '2027-03-22', '2027-03-28', 98875, 42375, NULL, NULL, 'PENDING', NULL);

-- ============================================================================
-- 24. 환율 알림
-- ============================================================================
SET @alert_country_france = (SELECT id FROM countries WHERE country_name = '프랑스' LIMIT 1);
SET @insert_exchange_alert_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'exchange_rate_alerts'
        AND column_name = 'country_id') > 0,
    'INSERT INTO exchange_rate_alerts (user_id, country_id, target_rate, is_deleted) VALUES (@user_id, @alert_country_france, 1580.00, 0)',
    'INSERT INTO exchange_rate_alerts (user_id, currency_id, target_rate, is_deleted) VALUES (@user_id, @eur_id, 1580.00, 0)'
);
PREPARE insert_exchange_alert_stmt FROM @insert_exchange_alert_sql;
EXECUTE insert_exchange_alert_stmt;
DEALLOCATE PREPARE insert_exchange_alert_stmt;

-- ============================================================================
-- 25. 알림
-- ============================================================================
INSERT INTO notifications (user_id, notification_type, title, message, is_read, created_at)
VALUES
    (@user_id, 'EXCHANGE_RATE', '환율 알림', 'EUR/KRW 환율이 목표 환율 1,580원에 근접했습니다. 현재 1,585원', FALSE, '2026-10-09 10:00:00'),
    (@user_id, 'AUTO_SAVING', '자동 저축 완료', '9월 자동 저축 420,000원이 여행 월렛으로 이체되었습니다.', TRUE, '2026-09-05 09:01:00'),
    (@user_id, 'AUTO_SAVING', '자동 저축 완료', '10월 자동 저축 420,000원이 여행 월렛으로 이체되었습니다.', TRUE, '2026-10-05 09:01:00'),
    (@user_id, 'MISSION', '주간 미션 성공!', '9월 1주차 식비 절감 미션을 달성했습니다. 58,000원 절약!', TRUE, '2026-09-08 08:00:00');

-- ============================================================================
-- 26. 영수증 (여행 중 사용 - 시드에서는 미리 생성)
-- ============================================================================
-- 프랑스 영수증 (4/4~4/8): ~25개
SET @country_france = (SELECT id FROM countries WHERE country_name = '프랑스');
SET @country_swiss  = (SELECT id FROM countries WHERE country_name = '스위스');
SET @country_portugal = (SELECT id FROM countries WHERE country_name = '포르투갈');

INSERT INTO receipts
    (user_id, trip_id, country_id, category_id, currency_id, payment_datetime, file_name, file_url, file_type, status, merchant_original_name, merchant_translated_name, total_amount, ocr_raw_text)
VALUES
    -- France Day 1
    (@user_id, @trip_id, @country_france, 7, @eur_id, '2027-04-04 09:17:00', 'receipt_001.jpg', 'https://storage.tripass.com/demo/receipt_001.jpg', 'IMAGE', 'COMPLETED', 'Cafe de Flore', '카페 드 플로르', 18.50, 'Cafe de Flore\nCafe Creme x2\nCroissant x2\nTotal: 18.50 EUR'),
    (@user_id, @trip_id, @country_france, 5, @eur_id, '2027-04-04 10:34:00', 'receipt_002.jpg', 'https://storage.tripass.com/demo/receipt_002.jpg', 'IMAGE', 'COMPLETED', 'Musee du Louvre', '루브르 박물관', 17.00, 'Musee du Louvre\nBillet Adult x1\nTotal: 17.00 EUR'),
    (@user_id, @trip_id, @country_france, 1, @eur_id, '2027-04-04 11:51:00', 'receipt_003.jpg', 'https://storage.tripass.com/demo/receipt_003.jpg', 'IMAGE', 'COMPLETED', 'Le Petit Cler', '르 프티 클레르', 32.00, 'Le Petit Cler\nSteak Frites\nVin Rouge\nDessert\nTotal: 32.00 EUR'),
    (@user_id, @trip_id, @country_france, 5, @eur_id, '2027-04-04 12:08:00', 'receipt_004.jpg', 'https://storage.tripass.com/demo/receipt_004.jpg', 'IMAGE', 'COMPLETED', 'Tour Eiffel', '에펠탑', 26.80, 'Tour Eiffel\nSommet Adult x1\nTotal: 26.80 EUR'),
    (@user_id, @trip_id, @country_france, 2, @eur_id, '2027-04-04 13:25:00', 'receipt_005.jpg', 'https://storage.tripass.com/demo/receipt_005.jpg', 'IMAGE', 'COMPLETED', 'Metro RATP', '파리 메트로', 16.90, 'RATP\nCarnet 10 Tickets\nTotal: 16.90 EUR'),
    -- France Day 2
    (@user_id, @trip_id, @country_france, 1, @eur_id, '2027-04-05 14:42:00', 'receipt_006.jpg', 'https://storage.tripass.com/demo/receipt_006.jpg', 'IMAGE', 'COMPLETED', 'Boulangerie Montmartre', '몽마르뜨 빵집', 8.50, 'Boulangerie\nPain au Chocolat x2\nCafe x1\nTotal: 8.50 EUR'),
    (@user_id, @trip_id, @country_france, 5, @eur_id, '2027-04-05 15:59:00', 'receipt_007.jpg', 'https://storage.tripass.com/demo/receipt_007.jpg', 'IMAGE', 'COMPLETED', 'Musee d''Orsay', '오르세 미술관', 16.00, 'Musee d''Orsay\nBillet Adult x1\nTotal: 16.00 EUR'),
    (@user_id, @trip_id, @country_france, 1, @eur_id, '2027-04-05 16:16:00', 'receipt_008.jpg', 'https://storage.tripass.com/demo/receipt_008.jpg', 'IMAGE', 'COMPLETED', 'Les Deux Magots', '레 되 마고', 28.00, 'Les Deux Magots\nSoupe a l''oignon\nConfit de Canard\nTotal: 28.00 EUR'),
    (@user_id, @trip_id, @country_france, 4, @eur_id, '2027-04-05 17:33:00', 'receipt_009.jpg', 'https://storage.tripass.com/demo/receipt_009.jpg', 'IMAGE', 'COMPLETED', 'Shakespeare and Company', '셰익스피어 앤 컴퍼니', 22.00, 'Shakespeare and Company\nBooks x2\nTotal: 22.00 EUR'),
    -- France Day 3
    (@user_id, @trip_id, @country_france, 5, @eur_id, '2027-04-06 18:50:00', 'receipt_010.jpg', 'https://storage.tripass.com/demo/receipt_010.jpg', 'IMAGE', 'COMPLETED', 'Chateau de Versailles', '베르사유 궁전', 21.00, 'Chateau de Versailles\nBillet Adult x1\nTotal: 21.00 EUR'),
    (@user_id, @trip_id, @country_france, 7, @eur_id, '2027-04-06 19:07:00', 'receipt_011.jpg', 'https://storage.tripass.com/demo/receipt_011.jpg', 'IMAGE', 'COMPLETED', 'Angelina Versailles', '앙젤리나 베르사유', 15.50, 'Angelina\nChocolat Chaud\nMont Blanc\nTotal: 15.50 EUR'),
    (@user_id, @trip_id, @country_france, 5, @eur_id, '2027-04-06 08:24:00', 'receipt_012.jpg', 'https://storage.tripass.com/demo/receipt_012.jpg', 'IMAGE', 'COMPLETED', 'Centre Pompidou', '퐁피두 센터', 15.00, 'Centre Pompidou\nBillet Adult x1\nTotal: 15.00 EUR'),
    (@user_id, @trip_id, @country_france, 1, @eur_id, '2027-04-06 09:41:00', 'receipt_013.jpg', 'https://storage.tripass.com/demo/receipt_013.jpg', 'IMAGE', 'COMPLETED', 'Le Marais Falafel', '마레 팔라펠', 9.50, 'L''As du Fallafel\nFalafel Special\nTotal: 9.50 EUR'),
    -- France Day 4
    (@user_id, @trip_id, @country_france, 5, @eur_id, '2027-04-07 10:58:00', 'receipt_014.jpg', 'https://storage.tripass.com/demo/receipt_014.jpg', 'IMAGE', 'COMPLETED', 'Musee de l''Orangerie', '오랑주리 미술관', 12.50, 'Musee de l''Orangerie\nBillet Adult x1\nTotal: 12.50 EUR'),
    (@user_id, @trip_id, @country_france, 4, @eur_id, '2027-04-07 11:15:00', 'receipt_015.jpg', 'https://storage.tripass.com/demo/receipt_015.jpg', 'IMAGE', 'COMPLETED', 'Galeries Lafayette', '갤러리 라파예트', 85.00, 'Galeries Lafayette\nParfum\nTotal: 85.00 EUR'),
    (@user_id, @trip_id, @country_france, 5, @eur_id, '2027-04-07 12:32:00', 'receipt_016.jpg', 'https://storage.tripass.com/demo/receipt_016.jpg', 'IMAGE', 'COMPLETED', 'Palais Garnier', '오페라 가르니에', 14.00, 'Palais Garnier\nVisite libre\nTotal: 14.00 EUR'),
    (@user_id, @trip_id, @country_france, 5, @eur_id, '2027-04-07 13:49:00', 'receipt_017.jpg', 'https://storage.tripass.com/demo/receipt_017.jpg', 'IMAGE', 'COMPLETED', 'Bateaux Mouches', '바토 무슈', 15.00, 'Bateaux Mouches\nCroisiere 1h\nTotal: 15.00 EUR'),
    (@user_id, @trip_id, @country_france, 1, @eur_id, '2027-04-07 14:06:00', 'receipt_018.jpg', 'https://storage.tripass.com/demo/receipt_018.jpg', 'IMAGE', 'COMPLETED', 'Le Bouillon Chartier', '르 부이용 샤르티에', 24.00, 'Le Bouillon Chartier\nEscargots\nBoeuf Bourguignon\nTotal: 24.00 EUR'),
    -- France Day 5
    (@user_id, @trip_id, @country_france, 7, @eur_id, '2027-04-08 15:23:00', 'receipt_019.jpg', 'https://storage.tripass.com/demo/receipt_019.jpg', 'IMAGE', 'COMPLETED', 'Canal Saint-Martin Cafe', '생마르탱 카페', 12.00, 'Cafe Craft\nFlat White x2\nTotal: 12.00 EUR'),
    (@user_id, @trip_id, @country_france, 5, @eur_id, '2027-04-08 16:40:00', 'receipt_020.jpg', 'https://storage.tripass.com/demo/receipt_020.jpg', 'IMAGE', 'COMPLETED', 'Bateaux Mouches Sunset', '바토 무슈 석양', 15.00, 'Bateaux Mouches\nCroisiere Sunset\nTotal: 15.00 EUR'),
    (@user_id, @trip_id, @country_france, 1, @eur_id, '2027-04-08 17:57:00', 'receipt_021.jpg', 'https://storage.tripass.com/demo/receipt_021.jpg', 'IMAGE', 'COMPLETED', 'Chez Janou', '셰 자누', 38.00, 'Chez Janou\nMousse au Chocolat\nPlat du Jour\nVin\nTotal: 38.00 EUR'),
    -- France Day 6 (travel day)
    (@user_id, @trip_id, @country_france, 1, @eur_id, '2027-04-09 18:14:00', 'receipt_022.jpg', 'https://storage.tripass.com/demo/receipt_022.jpg', 'IMAGE', 'COMPLETED', 'Gare de Lyon Relay', '리옹역 릴레이', 9.80, 'Relay\nSandwich\nEau minerale\nTotal: 9.80 EUR'),
    -- Switzerland Day 1 (4/9)
    (@user_id, @trip_id, @country_swiss, 1, @chf_id, '2027-04-09 19:31:00', 'receipt_023.jpg', 'https://storage.tripass.com/demo/receipt_023.jpg', 'IMAGE', 'COMPLETED', 'Zeughauskeller', '초이크하우스켈러', 42.00, 'Zeughauskeller\nZurcher Geschnetzeltes\nBier\nTotal: CHF 42.00'),
    (@user_id, @trip_id, @country_swiss, 4, @chf_id, '2027-04-09 08:48:00', 'receipt_024.jpg', 'https://storage.tripass.com/demo/receipt_024.jpg', 'IMAGE', 'COMPLETED', 'Sprungli', '슈프륑리', 18.50, 'Confiserie Sprungli\nLuxemburgerli Box\nTotal: CHF 18.50'),
    -- Switzerland Day 2 (4/10)
    (@user_id, @trip_id, @country_swiss, 5, @chf_id, '2027-04-10 09:05:00', 'receipt_025.jpg', 'https://storage.tripass.com/demo/receipt_025.jpg', 'IMAGE', 'COMPLETED', 'Jungfrau Railways', '융프라우 철도', 230.00, 'Jungfrau Railways\nTop of Europe Ticket\nTotal: CHF 230.00'),
    (@user_id, @trip_id, @country_swiss, 1, @chf_id, '2027-04-10 10:22:00', 'receipt_026.jpg', 'https://storage.tripass.com/demo/receipt_026.jpg', 'IMAGE', 'COMPLETED', 'Restaurant Eigerblick', '아이거블릭 레스토랑', 35.00, 'Restaurant Eigerblick\nRosti mit Spiegelei\nKaffee\nTotal: CHF 35.00'),
    (@user_id, @trip_id, @country_swiss, 4, @chf_id, '2027-04-10 11:39:00', 'receipt_027.jpg', 'https://storage.tripass.com/demo/receipt_027.jpg', 'IMAGE', 'COMPLETED', 'Coop Interlaken', '쿱 인터라켄', 15.80, 'Coop\nWasser\nSchokolade\nSnacks\nTotal: CHF 15.80'),
    -- Switzerland Day 3 (4/11)
    (@user_id, @trip_id, @country_swiss, 5, @chf_id, '2027-04-11 12:56:00', 'receipt_028.jpg', 'https://storage.tripass.com/demo/receipt_028.jpg', 'IMAGE', 'COMPLETED', 'Pilatus Bahnen', '필라투스 철도', 72.00, 'Pilatus Bahnen AG\nGolden Round Trip\nTotal: CHF 72.00'),
    (@user_id, @trip_id, @country_swiss, 5, @chf_id, '2027-04-11 13:13:00', 'receipt_029.jpg', 'https://storage.tripass.com/demo/receipt_029.jpg', 'IMAGE', 'COMPLETED', 'SGV Luzern', '루체른 유람선', 45.00, 'SGV\nSchifffahrt Luzern\nTotal: CHF 45.00'),
    (@user_id, @trip_id, @country_swiss, 1, @chf_id, '2027-04-11 14:30:00', 'receipt_030.jpg', 'https://storage.tripass.com/demo/receipt_030.jpg', 'IMAGE', 'COMPLETED', 'Wirtshaus Galliker', '갈리커 레스토랑', 38.00, 'Wirtshaus Galliker\nKalbsgeschnetzeltes\nSalat\nTotal: CHF 38.00'),
    (@user_id, @trip_id, @country_swiss, 4, @chf_id, '2027-04-11 15:47:00', 'receipt_031.jpg', 'https://storage.tripass.com/demo/receipt_031.jpg', 'IMAGE', 'COMPLETED', 'Bachmann Confiserie', '바흐만 과자점', 12.50, 'Bachmann\nLuzerner Lebkuchen\nTotal: CHF 12.50'),
    -- Switzerland Day 4 (4/12)
    (@user_id, @trip_id, @country_swiss, 5, @chf_id, '2027-04-12 16:04:00', 'receipt_032.jpg', 'https://storage.tripass.com/demo/receipt_032.jpg', 'IMAGE', 'COMPLETED', 'Skywings Paragliding', '스카이윙스 패러글라이딩', 180.00, 'Skywings Interlaken\nTandem Paragliding\nTotal: CHF 180.00'),
    (@user_id, @trip_id, @country_swiss, 5, @chf_id, '2027-04-12 17:21:00', 'receipt_033.jpg', 'https://storage.tripass.com/demo/receipt_033.jpg', 'IMAGE', 'COMPLETED', 'Harder Kulm Bahn', '하더 쿨름 케이블카', 32.00, 'Harder Kulm\nFunicular Return\nTotal: CHF 32.00'),
    (@user_id, @trip_id, @country_swiss, 1, @chf_id, '2027-04-12 18:38:00', 'receipt_034.jpg', 'https://storage.tripass.com/demo/receipt_034.jpg', 'IMAGE', 'COMPLETED', 'Restaurant Baren', '베렌 레스토랑', 45.00, 'Restaurant Baren\nFondue Moitie-Moitie\nBier\nTotal: CHF 45.00'),
    -- Portugal Day 1 (4/14)
    (@user_id, @trip_id, @country_portugal, 1, @eur_id, '2027-04-14 19:55:00', 'receipt_035.jpg', 'https://storage.tripass.com/demo/receipt_035.jpg', 'IMAGE', 'COMPLETED', 'Pasteis de Belem', '파스테이스 드 벨렘', 8.50, 'Pasteis de Belem\nPastel de Nata x4\nCafe x2\nTotal: 8.50 EUR'),
    (@user_id, @trip_id, @country_portugal, 5, @eur_id, '2027-04-14 08:12:00', 'receipt_036.jpg', 'https://storage.tripass.com/demo/receipt_036.jpg', 'IMAGE', 'COMPLETED', 'Torre de Belem', '벨렘탑', 8.00, 'Torre de Belem\nBilhete Adulto\nTotal: 8.00 EUR'),
    (@user_id, @trip_id, @country_portugal, 1, @eur_id, '2027-04-14 09:29:00', 'receipt_037.jpg', 'https://storage.tripass.com/demo/receipt_037.jpg', 'IMAGE', 'COMPLETED', 'Cervejaria Ramiro', '세르베자리아 하미루', 35.00, 'Cervejaria Ramiro\nGambas al Ajillo\nPrego\nVinho Verde\nTotal: 35.00 EUR'),
    -- Portugal Day 2 (4/15)
    (@user_id, @trip_id, @country_portugal, 5, @eur_id, '2027-04-15 10:46:00', 'receipt_038.jpg', 'https://storage.tripass.com/demo/receipt_038.jpg', 'IMAGE', 'COMPLETED', 'Castelo de Sao Jorge', '상 조르제 성', 10.00, 'Castelo de Sao Jorge\nBilhete\nTotal: 10.00 EUR'),
    (@user_id, @trip_id, @country_portugal, 1, @eur_id, '2027-04-15 11:03:00', 'receipt_039.jpg', 'https://storage.tripass.com/demo/receipt_039.jpg', 'IMAGE', 'COMPLETED', 'Time Out Market', '타임아웃 마켓', 22.00, 'Time Out Market\nBacalhau a Bras\nGinjinha\nTotal: 22.00 EUR'),
    (@user_id, @trip_id, @country_portugal, 2, @eur_id, '2027-04-15 12:20:00', 'receipt_040.jpg', 'https://storage.tripass.com/demo/receipt_040.jpg', 'IMAGE', 'COMPLETED', 'Carris Lisboa', '리스본 교통', 7.00, 'Carris\nViva Viagem 24h\nTotal: 7.00 EUR'),
    -- Portugal Day 3 (4/16)
    (@user_id, @trip_id, @country_portugal, 5, @eur_id, '2027-04-16 13:37:00', 'receipt_041.jpg', 'https://storage.tripass.com/demo/receipt_041.jpg', 'IMAGE', 'COMPLETED', 'Palacio da Pena', '페나 궁전', 14.00, 'Palacio Nacional da Pena\nBilhete\nTotal: 14.00 EUR'),
    (@user_id, @trip_id, @country_portugal, 5, @eur_id, '2027-04-16 14:54:00', 'receipt_042.jpg', 'https://storage.tripass.com/demo/receipt_042.jpg', 'IMAGE', 'COMPLETED', 'Quinta da Regaleira', '헤갈레이라 별장', 10.00, 'Quinta da Regaleira\nEntrada\nTotal: 10.00 EUR'),
    (@user_id, @trip_id, @country_portugal, 1, @eur_id, '2027-04-16 15:11:00', 'receipt_043.jpg', 'https://storage.tripass.com/demo/receipt_043.jpg', 'IMAGE', 'COMPLETED', 'Restaurante Regional', '지역 레스토랑', 18.00, 'Restaurante Regional\nFrango Piri-Piri\nArroz\nTotal: 18.00 EUR'),
    -- Portugal Day 4 (4/17)
    (@user_id, @trip_id, @country_portugal, 2, @eur_id, '2027-04-17 16:28:00', 'receipt_044.jpg', 'https://storage.tripass.com/demo/receipt_044.jpg', 'IMAGE', 'COMPLETED', 'CP Comboios', '포르투갈 기차', 25.00, 'CP\nLisboa-Porto AP\nTotal: 25.00 EUR'),
    (@user_id, @trip_id, @country_portugal, 5, @eur_id, '2027-04-17 17:45:00', 'receipt_045.jpg', 'https://storage.tripass.com/demo/receipt_045.jpg', 'IMAGE', 'COMPLETED', 'Livraria Lello', '렐루 서점', 5.00, 'Livraria Lello\nVoucher Entrada\nTotal: 5.00 EUR'),
    (@user_id, @trip_id, @country_portugal, 1, @eur_id, '2027-04-17 18:02:00', 'receipt_046.jpg', 'https://storage.tripass.com/demo/receipt_046.jpg', 'IMAGE', 'COMPLETED', 'Cafe Majestic', '카페 마제스틱', 16.00, 'Cafe Majestic\nFrancesinha\nCerveja\nTotal: 16.00 EUR'),
    -- Portugal Day 5 (4/18)
    (@user_id, @trip_id, @country_portugal, 4, @eur_id, '2027-04-18 19:19:00', 'receipt_047.jpg', 'https://storage.tripass.com/demo/receipt_047.jpg', 'IMAGE', 'COMPLETED', 'LX Factory Market', 'LX 팩토리', 28.00, 'LX Factory\nAzulejo Art\nSouvenir\nTotal: 28.00 EUR'),
    (@user_id, @trip_id, @country_portugal, 5, @eur_id, '2027-04-18 08:36:00', 'receipt_048.jpg', 'https://storage.tripass.com/demo/receipt_048.jpg', 'IMAGE', 'COMPLETED', 'Cristo Rei', '크리스투 레이', 6.00, 'Santuario Cristo Rei\nElevador\nTotal: 6.00 EUR');

-- ============================================================================
-- 27. 영수증 품목 (주요 영수증에 2~4개씩)
-- ============================================================================
-- receipt_001 (Cafe de Flore)
SET @r001 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_001.jpg');
SET @r003 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_003.jpg');
SET @r008 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_008.jpg');
SET @r015 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_015.jpg');
SET @r018 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_018.jpg');
SET @r021 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_021.jpg');
SET @r026 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_026.jpg');
SET @r030 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_030.jpg');
SET @r034 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_034.jpg');
SET @r037 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_037.jpg');
SET @r039 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_039.jpg');
SET @r046 = (SELECT id FROM receipts WHERE user_id = @user_id AND file_name = 'receipt_046.jpg');

INSERT INTO receipt_items (receipt_id, original_name, translated_name, quantity, amount, display_order)
VALUES
    -- Cafe de Flore
    (@r001, 'Cafe Creme', '카페 크렘', 2, 7.00, 1),
    (@r001, 'Croissant', '크루아상', 2, 4.50, 2),
    -- Le Petit Cler (dinner)
    (@r003, 'Steak Frites', '스테이크 프리트', 1, 18.00, 1),
    (@r003, 'Vin Rouge (verre)', '레드 와인 한잔', 1, 8.00, 2),
    (@r003, 'Creme Brulee', '크렘 브륄레', 1, 6.00, 3),
    -- Les Deux Magots
    (@r008, 'Soupe a l''oignon', '양파 수프', 1, 9.00, 1),
    (@r008, 'Confit de Canard', '오리 콩피', 1, 19.00, 2),
    -- Galeries Lafayette
    (@r015, 'Parfum Chanel N5', '샤넬 N5 향수', 1, 85.00, 1),
    -- Le Bouillon Chartier
    (@r018, 'Escargots (6pcs)', '달팽이 요리', 1, 7.00, 1),
    (@r018, 'Boeuf Bourguignon', '부르기뇽', 1, 14.00, 2),
    (@r018, 'Vin de la maison', '하우스 와인', 1, 3.00, 3),
    -- Chez Janou
    (@r021, 'Plat du Jour', '오늘의 메뉴', 1, 16.00, 1),
    (@r021, 'Mousse au Chocolat', '초콜릿 무스', 1, 8.00, 2),
    (@r021, 'Vin Rouge', '레드 와인', 1, 14.00, 3),
    -- Restaurant Eigerblick
    (@r026, 'Rosti mit Spiegelei', '뢰스티+계란프라이', 1, 28.00, 1),
    (@r026, 'Kaffee', '커피', 1, 7.00, 2),
    -- Wirtshaus Galliker
    (@r030, 'Kalbsgeschnetzeltes', '송아지 스튜', 1, 32.00, 1),
    (@r030, 'Gruner Salat', '그린 샐러드', 1, 6.00, 2),
    -- Restaurant Baren (Fondue)
    (@r034, 'Fondue Moitie-Moitie', '치즈 퐁듀', 1, 35.00, 1),
    (@r034, 'Bier (500ml)', '맥주', 1, 10.00, 2),
    -- Cervejaria Ramiro
    (@r037, 'Gambas al Ajillo', '마늘 새우', 1, 15.00, 1),
    (@r037, 'Prego no Pao', '프레고 샌드위치', 1, 12.00, 2),
    (@r037, 'Vinho Verde', '비뉴 베르드', 1, 8.00, 3),
    -- Time Out Market
    (@r039, 'Bacalhau a Bras', '바칼랴우 아 브라스', 1, 14.00, 1),
    (@r039, 'Ginjinha', '진지냐(체리 리큐어)', 1, 8.00, 2),
    -- Cafe Majestic
    (@r046, 'Francesinha', '프란세지냐', 1, 12.00, 1),
    (@r046, 'Cerveja Super Bock', '수퍼 복 맥주', 1, 4.00, 2);

-- ============================================================================
-- 28. 영수증 참가자 (스위스 공유 비용)
-- ============================================================================
SET @r026_id = @r026;
SET @r030_id = @r030;
SET @r034_id = @r034;

INSERT INTO receipt_participants (receipt_id, participant_name, display_order)
VALUES
    (@r026_id, '유현', 1), (@r026_id, '아영', 2), (@r026_id, '유진', 3),
    (@r030_id, '유현', 1), (@r030_id, '아영', 2), (@r030_id, '유진', 3),
    (@r034_id, '유현', 1), (@r034_id, '아영', 2), (@r034_id, '유진', 3);

-- 최신 여행별 집계 컬럼을 데모 데이터에도 연결한다.
UPDATE wallet_ledger
SET trip_id = @trip_id
WHERE wallet_id = @wallet_id;

UPDATE monthly_saving_missions
SET trip_id = @trip_id,
    selected_at = STR_TO_DATE(CONCAT(target_year_month, '-01'), '%Y-%m-%d'),
    mission_start_date = STR_TO_DATE(CONCAT(target_year_month, '-01'), '%Y-%m-%d')
WHERE user_id = @user_id;

-- ============================================================================
-- 확인 쿼리
-- ============================================================================
SELECT
    u.id AS user_id,
    u.login_id,
    u.name,
    w.balance_amount AS wallet_balance,
    (SELECT COUNT(*) FROM accounts a WHERE a.user_id = u.id AND a.is_deleted = 0) AS account_count,
    (SELECT COUNT(*) FROM cards c WHERE c.user_id = u.id AND c.is_deleted = 0) AS card_count,
    (SELECT COUNT(*) FROM trips t WHERE t.user_id = u.id AND t.is_deleted = 0) AS trip_count,
    (SELECT COUNT(*) FROM wallet_ledger wl WHERE wl.wallet_id = w.id) AS ledger_count,
    (SELECT COUNT(*) FROM trip_schedules ts WHERE ts.trip_id = @trip_id) AS schedule_count,
    (SELECT COUNT(*) FROM wallet_exchange_transaction wet WHERE wet.wallet_id = w.id) AS exchange_count,
    (SELECT COUNT(*) FROM travel_card_ledger tcl WHERE tcl.wallet_travel_card_id = @wtc_id) AS card_ledger_count,
    (SELECT COUNT(*) FROM receipts r WHERE r.user_id = u.id) AS receipt_count,
    (SELECT COUNT(*) FROM monthly_saving_missions msm WHERE msm.user_id = u.id) AS mission_count,
    (SELECT COUNT(*) FROM weekly_saving_missions wsm
       JOIN monthly_saving_missions msm ON wsm.monthly_saving_mission_id = msm.id
       WHERE msm.user_id = u.id) AS weekly_mission_count
FROM users u
JOIN wallet w ON w.user_id = u.id
WHERE u.login_provider = 'LOCAL'
  AND u.login_id = 'yuhyun';

COMMIT;
