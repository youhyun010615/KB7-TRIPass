-- =====================================================
-- 테스트 계정 상태 초기화 스크립트
-- 온보딩(여행/계좌 등록 skip 플로우), 여행 lifecycle, 미션 등을
-- 실제 날짜/1회성 플래그 제약 없이 반복 테스트하기 위한 도구입니다.
--
-- 사용법:
--   1) 아래 @target_login_id를 초기화할 계정으로 변경
--   2) @also_reset_accounts 를 1로 두면 계좌(Codef) 연동까지 지움
--      (①②③④ 4가지 상태를 전부 재현하려면 1로 두고 재사용)
--      0으로 두면 계좌 연동은 유지한 채 여행/월렛/미션만 리셋
--   3) 전체 실행
--
-- 주의: 특정 "한 계정"만 대상으로 합니다. WHERE 절 없는 전체 삭제 없음.
-- =====================================================

USE tripass;
SET FOREIGN_KEY_CHECKS = 0;

SET @target_login_id = 'cadry2';
SET @also_reset_accounts = 1;

SELECT id INTO @uid FROM users WHERE login_id = @target_login_id AND is_deleted = 0;

-- 0) 온보딩을 다시 보게 함 (가입 직후 상태로 복귀)
UPDATE users SET onboarding_shown_at = NULL WHERE id = @uid;

-- 1) 여행 관련 데이터 전부 삭제
DELETE tbr FROM trip_budget_recommendations tbr
  JOIN trip_countries tc ON tbr.trip_country_id = tc.id
  JOIN trips t ON tc.trip_id = t.id
  WHERE t.user_id = @uid;
DELETE ts FROM trip_schedules ts
  JOIN trips t ON ts.trip_id = t.id
  WHERE t.user_id = @uid;
DELETE pe FROM pre_expenses pe
  JOIN trips t ON pe.trip_id = t.id
  WHERE t.user_id = @uid;
DELETE tci FROM trip_checklist_items tci
  JOIN trips t ON tci.trip_id = t.id
  WHERE t.user_id = @uid;
DELETE tr FROM trip_reports tr
  JOIN trips t ON tr.trip_id = t.id
  WHERE t.user_id = @uid;
DELETE sp FROM saving_plans sp
  JOIN trips t ON sp.trip_id = t.id
  WHERE t.user_id = @uid;
DELETE ri FROM receipt_items ri
  JOIN receipts r ON ri.receipt_id = r.id
  WHERE r.user_id = @uid;
DELETE rp FROM receipt_participants rp
  JOIN receipts r ON rp.receipt_id = r.id
  WHERE r.user_id = @uid;
DELETE FROM receipts WHERE user_id = @uid;
DELETE txn FROM transactions txn
  LEFT JOIN accounts a ON txn.account_id = a.id
  LEFT JOIN cards c ON txn.card_id = c.id
  WHERE a.user_id = @uid OR c.user_id = @uid;
DELETE tc FROM trip_countries tc
  JOIN trips t ON tc.trip_id = t.id
  WHERE t.user_id = @uid;
DELETE FROM trips WHERE user_id = @uid;
DELETE FROM trip_wallets WHERE user_id = @uid;

-- 2) 미션 / 분석 리포트 초기화
DELETE wsm FROM weekly_saving_missions wsm
  JOIN monthly_saving_missions msm ON wsm.monthly_saving_mission_id = msm.id
  WHERE msm.user_id = @uid;
DELETE FROM monthly_saving_missions WHERE user_id = @uid;
DELETE FROM mission_category_selections
  WHERE monthly_spending_analysis_id IN (
    SELECT id FROM monthly_spending_analyses WHERE user_id = @uid
  );
DELETE FROM monthly_spending_analyses WHERE user_id = @uid;

-- 3) 월렛 초기화 (잔액 0, 거래내역/트래블카드 데이터 삭제)
DELETE wct FROM wallet_card_topup wct
  JOIN wallet_travel_card wtc ON wct.wallet_travel_card_id = wtc.id
  JOIN wallet w ON wtc.wallet_id = w.id
  WHERE w.user_id = @uid;
DELETE wet FROM wallet_exchange_transaction wet
  JOIN wallet_travel_card wtc ON wet.wallet_travel_card_id = wtc.id
  JOIN wallet w ON wtc.wallet_id = w.id
  WHERE w.user_id = @uid;
DELETE tcl FROM travel_card_ledger tcl
  JOIN wallet_travel_card wtc ON tcl.wallet_travel_card_id = wtc.id
  JOIN wallet w ON wtc.wallet_id = w.id
  WHERE w.user_id = @uid;
DELETE tcb FROM travel_card_balance tcb
  JOIN wallet_travel_card wtc ON tcb.wallet_travel_card_id = wtc.id
  JOIN wallet w ON wtc.wallet_id = w.id
  WHERE w.user_id = @uid;
DELETE wtc FROM wallet_travel_card wtc
  JOIN wallet w ON wtc.wallet_id = w.id
  WHERE w.user_id = @uid;
DELETE wal FROM wallet_auto_saving_logs wal
  JOIN wallet w ON wal.wallet_id = w.id
  WHERE w.user_id = @uid;
DELETE FROM wallet_auto_saving_rule
  WHERE wallet_id IN (SELECT id FROM wallet WHERE user_id = @uid);
DELETE wl FROM wallet_ledger wl
  JOIN wallet w ON wl.wallet_id = w.id
  WHERE w.user_id = @uid;
DELETE wa FROM wallet_account wa
  JOIN wallet w ON wa.wallet_id = w.id
  WHERE w.user_id = @uid;
UPDATE wallet SET balance_amount = 0, version = 0 WHERE user_id = @uid;
DELETE FROM user_travel_cards WHERE user_id = @uid;

-- 4) (옵션) 계좌/카드/Codef 연동까지 초기화 — 4가지 상태(①②③④)를 전부 재현하고 싶을 때
DELETE FROM cards WHERE @also_reset_accounts = 1 AND user_id = @uid;
DELETE FROM accounts WHERE @also_reset_accounts = 1 AND user_id = @uid;
DELETE cci FROM codef_connected_institutions cci
  JOIN codef_connections cc ON cci.codef_connection_id = cc.id
  WHERE @also_reset_accounts = 1 AND cc.user_id = @uid;
DELETE FROM codef_connections WHERE @also_reset_accounts = 1 AND user_id = @uid;

SET FOREIGN_KEY_CHECKS = 1;

-- 확인
SELECT @uid AS user_id, @target_login_id AS login_id, @also_reset_accounts AS accounts_also_reset;
SELECT id, login_id, onboarding_shown_at FROM users WHERE id = @uid;
