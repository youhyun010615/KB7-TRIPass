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
-- 여행: 프랑스(4/4~4/8) → 스위스(4/9~4/13) → 포르투갈(4/14~4/18)
-- 총 여행 예산: 480만원
-- 저축 기간: 2026-08-23 ~ 2027-03-31
-- 비상금: wallet잔액 - 480만 (PLANNING→TRAVELING 전환 시 계산)
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

-- ============================================================================
-- 6. 월렛 연동 계좌
-- ============================================================================
INSERT INTO wallet_account (wallet_id, account_id, is_primary, status)
VALUES (@wallet_id, @account_id, TRUE, 'LINKED')
ON DUPLICATE KEY UPDATE is_primary = TRUE, status = 'LINKED';

-- ============================================================================
-- 7. 여행 (PLANNING 상태)
-- ============================================================================
INSERT INTO trips (user_id, trip_name, status, start_date, end_date,
                   total_target_amount, savings_tracking_started_at, wallet_reflect_resolved,
                   created_at, updated_at)
VALUES (@user_id, '유럽 3국 여행', 'PLANNING', '2027-04-04', '2027-04-18',
        4800000, '2026-08-23 10:00:00', 1,
        '2026-08-22 15:00:00', '2026-08-22 15:00:00');

SET @trip_id = LAST_INSERT_ID();

-- ============================================================================
-- 8. 여행 국가 (프랑스 → 스위스 → 포르투갈)
-- ============================================================================
INSERT INTO trip_countries (trip_id, country_id, arrival_date, departure_date, target_budget, display_order)
VALUES
    (@trip_id, (SELECT id FROM countries WHERE country_name = '프랑스'),   '2027-04-04', '2027-04-08', 2500000, 1),
    (@trip_id, (SELECT id FROM countries WHERE country_name = '스위스'),   '2027-04-09', '2027-04-13', 1700000, 2),
    (@trip_id, (SELECT id FROM countries WHERE country_name = '포르투갈'), '2027-04-14', '2027-04-18',  600000, 3);

SET @tc_france   = (SELECT id FROM trip_countries WHERE trip_id = @trip_id AND display_order = 1);
SET @tc_swiss    = (SELECT id FROM trip_countries WHERE trip_id = @trip_id AND display_order = 2);
SET @tc_portugal = (SELECT id FROM trip_countries WHERE trip_id = @trip_id AND display_order = 3);

-- ============================================================================
-- 9. 여행 예산 추천 (AI 추천 + 사용자 확정)
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
    -- 프랑스 (5일, 총 250만: 항공110+숙소60+관광31.5+식비40+기타8.5)
    (@tc_france, 1, 'MID_RANGE',
     1100000, 600000, 315000, 400000, 85000,
     1100000, 600000, 315000, 400000, 85000,
     '파리 5일 기준: 항공 110만(왕복 직항), 숙소 12만/박×5, 식비 8만/일×5, 관광·교통 6.3만/일×5, 기타 1.7만/일×5',
     'budget-ai-v1', 1),
    -- 스위스 (5일, 총 170만: 숙소85+관광45+식비30+기타10)
    (@tc_swiss, 1, 'MID_RANGE',
     0, 850000, 450000, 300000, 100000,
     0, 850000, 450000, 300000, 100000,
     '스위스 5일 기준(항공 프랑스 포함): 숙소 17만/박×5, 식비 6만/일×5, 관광·교통 9만/일×5, 기타 2만/일×5',
     'budget-ai-v1', 1),
    -- 포르투갈 (4일, 총 60만: 숙소28+관광14+식비18)
    (@tc_portugal, 1, 'MID_RANGE',
     0, 280000, 140000, 180000, 0,
     0, 280000, 140000, 180000, 0,
     '포르투갈 4일 기준(항공 프랑스 포함): 숙소 7만/박×4, 식비 4.5만/일×4, 관광·교통 3.5만/일×4',
     'budget-ai-v1', 1);

-- ============================================================================
-- 10. 저축 계획
-- ============================================================================
-- D-day 기반 계산: 480만 / (2026-08-23 ~ 2027-04-04) = 일평균 약 21,239원
-- 첫달(8/23~8/31, 9일): 191,150원 → 반올림 193,000원
-- 이후 월: (4,800,000 - 193,000) / 7개월 = 658,143원 → 반올림 658,000원
INSERT INTO saving_plans (trip_id, account_id, monthly_amount, goal_status, created_at)
VALUES (@trip_id, @account_id, 658000, 'ACTIVE', '2026-08-23 10:00:00');

-- ============================================================================
-- 11. TRIP 월렛
-- ============================================================================
INSERT INTO trip_wallets (user_id, balance, created_at)
VALUES (@user_id, 0, '2026-08-22 15:00:00')
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================================================
-- 12. 월렛 저축 이력 (8개월 저축 시나리오)
--
-- 8월:  193,000원 (D-day 첫달 프로레이션)
-- 9월:  658,000원 (정상)
-- 10월: 808,000원 (추가 저축 +150,000)
-- 11월: 658,000원 (정상)
-- 12월: 508,000원 (목표 미달 -150,000)
-- 1월:  858,000원 (보충 +200,000)
-- 2월:  808,000원 (추가 저축 +150,000)
-- 3월:  758,000원 (추가 저축 +100,000)
-- 총합: 5,249,000원  비상금: 449,000원
-- ============================================================================
INSERT INTO wallet_ledger
    (wallet_id, trip_id, direction, transaction_type, transfer_method,
     amount, balance_before, balance_after,
     source_type, source_id, target_type, target_id, idempotency_key, memo, created_at)
VALUES
    -- 8월 저축 (첫달 D-day 프로레이션)
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'MANUAL',
     193000, 0, 193000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-08', '8월 여행 저축 (D-day 첫달)', '2026-08-25 12:00:00'),

    -- 9월 저축
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     658000, 193000, 851000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-09', '9월 여행 저축', '2026-09-25 09:00:00'),

    -- 10월 저축 (추가 +150,000)
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     658000, 851000, 1509000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-10A', '10월 여행 저축', '2026-10-25 09:00:00'),
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'MANUAL',
     150000, 1509000, 1659000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-10B', '10월 추가 저축', '2026-10-28 20:00:00'),

    -- 11월 저축
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     658000, 1659000, 2317000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-11', '11월 여행 저축', '2026-11-25 09:00:00'),

    -- 12월 저축 (목표 미달 -150,000)
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     508000, 2317000, 2825000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-12', '12월 여행 저축 (목표 미달)', '2026-12-25 09:00:00'),

    -- 1월 저축 (보충 +200,000)
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     658000, 2825000, 3483000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-01A', '1월 여행 저축', '2027-01-25 09:00:00'),
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'MANUAL',
     200000, 3483000, 3683000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-01B', '1월 보충 저축 (12월 미달분)', '2027-01-28 20:00:00'),

    -- 2월 저축 (추가 +150,000)
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     658000, 3683000, 4341000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-02A', '2월 여행 저축', '2027-02-25 09:00:00'),
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'MANUAL',
     150000, 4341000, 4491000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-02B', '2월 추가 저축', '2027-02-27 20:00:00'),

    -- 3월 저축 (추가 +100,000)
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'AUTO_SAVING',
     658000, 4491000, 5149000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-03A', '3월 여행 저축', '2027-03-25 09:00:00'),
    (@wallet_id, @trip_id, 'IN', 'CHARGE', 'MANUAL',
     100000, 5149000, 5249000,
     'ACCOUNT', @account_id, 'WALLET', @wallet_id,
     'DEMO-SEED-03B', '3월 추가 저축', '2027-03-28 20:00:00');

-- 월렛 잔액을 최종 저축 완료 상태로 업데이트
UPDATE wallet SET balance_amount = 5249000, version = 12 WHERE id = @wallet_id;

-- TRIP 월렛 잔액도 동기화
UPDATE trip_wallets SET balance = 5249000 WHERE user_id = @user_id;

-- ============================================================================
-- 13. 여행 일정 (3월에 등록)
-- ============================================================================
SET @eur_id = (SELECT id FROM currencies WHERE currency_code = 'EUR');
SET @chf_id = (SELECT id FROM currencies WHERE currency_code = 'CHF');

INSERT INTO trip_schedules
    (trip_id, trip_country_id, currency_id, schedule_name, scheduled_at,
     amount, payment_status, schedule_status, place_name, memo)
VALUES
    -- 프랑스 (4/4~4/8)
    (@trip_id, @tc_france, @eur_id, '루브르 박물관',          '2027-04-04 10:00:00', 17.00,  'ONSITE',    'UPCOMING', 'Musée du Louvre',        NULL),
    (@trip_id, @tc_france, @eur_id, '에펠탑 전망대',          '2027-04-04 15:00:00', 26.80,  'PREPAID',   'UPCOMING', 'Tour Eiffel',            '온라인 사전 예약'),
    (@trip_id, @tc_france, @eur_id, '몽마르뜨 언덕 산책',     '2027-04-05 09:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Montmartre',             NULL),
    (@trip_id, @tc_france, @eur_id, '오르세 미술관',          '2027-04-05 14:00:00', 16.00,  'ONSITE',    'UPCOMING', 'Musée d''Orsay',         NULL),
    (@trip_id, @tc_france, @eur_id, '베르사유 궁전',          '2027-04-06 09:00:00', 21.00,  'PREPAID',   'UPCOMING', 'Château de Versailles',  '사전 예약 필수'),
    (@trip_id, @tc_france, @eur_id, '세느강 유람선',          '2027-04-07 18:00:00', 15.00,  'ONSITE',    'UPCOMING', 'Bateaux Mouches',        NULL),
    (@trip_id, @tc_france, @eur_id, '파리 → 취리히 이동',     '2027-04-08 14:00:00', NULL,   'PREPAID',   'UPCOMING', 'Gare de Lyon',           'TGV Lyria'),
    -- 스위스 (4/9~4/13)
    (@trip_id, @tc_swiss,  @chf_id, '취리히 호수 산책',       '2027-04-09 10:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Zürichsee',              NULL),
    (@trip_id, @tc_swiss,  @chf_id, '융프라우 당일 투어',     '2027-04-10 07:00:00', 230.00, 'PREPAID',   'UPCOMING', 'Jungfraujoch',           '스위스패스 할인'),
    (@trip_id, @tc_swiss,  @chf_id, '루체른 구시가 관광',     '2027-04-11 09:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Luzern Altstadt',        NULL),
    (@trip_id, @tc_swiss,  @chf_id, '인터라켄 패러글라이딩',   '2027-04-12 10:00:00', 180.00, 'ONSITE',    'UPCOMING', 'Interlaken',             NULL),
    (@trip_id, @tc_swiss,  @chf_id, '취리히 → 리스본 이동',   '2027-04-13 12:00:00', NULL,   'PREPAID',   'UPCOMING', 'Zürich Airport',         'TAP Air Portugal'),
    -- 포르투갈 (4/14~4/18)
    (@trip_id, @tc_portugal, @eur_id, '벨렝탑 & 제로니무스',   '2027-04-14 10:00:00', 10.00,  'ONSITE',    'UPCOMING', 'Torre de Belém',         NULL),
    (@trip_id, @tc_portugal, @eur_id, '리스본 트램 28번',      '2027-04-15 09:00:00', 3.00,   'ONSITE',    'UPCOMING', 'Tram 28',                NULL),
    (@trip_id, @tc_portugal, @eur_id, '신트라 궁전',           '2027-04-16 09:00:00', 14.00,  'ONSITE',    'UPCOMING', 'Palácio da Pena',        '리스본 근교'),
    (@trip_id, @tc_portugal, @eur_id, '포르투 당일치기',        '2027-04-17 07:00:00', NULL,   'UNDECIDED', 'UPCOMING', 'Porto',                  'AP 고속열차');

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
    (SELECT COUNT(*) FROM trip_schedules ts WHERE ts.trip_id = @trip_id) AS schedule_count
FROM users u
JOIN wallet w ON w.user_id = u.id
WHERE u.login_provider = 'LOCAL'
  AND u.login_id = 'yuhyun';
