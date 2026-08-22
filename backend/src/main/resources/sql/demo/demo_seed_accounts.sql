-- ============================================================================
-- TriPass 시연 영상용 데모 계정 3종 시드 스크립트
-- ============================================================================
-- 생성: Claude (2026-08-20) / 요청: 시연 영상 촬영용 계정 3개 (여행 전/중/후)
--
-- [무엇을 만드는가]
--   demoprep   (여행 전/준비중) : 도쿄 여행 D-8, 목표저축 92.6% 달성
--   demotravel (여행 중)        : 유럽 3개국 - 프랑스(종료)→독일(진행중,3일차)→스위스(예정)
--   demodone   (여행 후)        : 방콕 2주 여행 종료, 여행후 리포트/귀국체크리스트 완료
--   각 계정은 users/trips/schedules/receipts/transactions 등 15개+ 테이블에 걸쳐
--   여행일정·영수증·거래내역까지 화면에 바로 뜨는 수준으로 데이터가 채워져 있습니다.
--
-- [로그인 정보]
--   TriPass 서비스 로그인 (id/pw): demoprep / demotravel / demodone, 공통 비밀번호 Demo1234!
--   금융기관(CODEF Mock) 로그인   : 위와 동일한 id, 공통 비밀번호 Mock1234!
--     (금융기관 Mock 로그인이 통하려면 MockCodefClient.java의 데모 페르소나 코드가
--      해당 서버에 배포되어 있어야 합니다 — develop 브랜치 push 시 자동 배포됨)
--
-- [실행 방법]
--   로컬:  mysql -h localhost -P 3306 -u root -p<DB_PW> tripass < demo_seed_accounts.sql
--   EC2:   scp로 이 파일을 서버에 올린 뒤
--          docker exec -i tripass-db mysql -u root -p'<DB_PASSWORD>' tripass < demo_seed_accounts.sql
--
-- [주의사항]
--   - 전체가 하나의 트랜잭션(START TRANSACTION ~ COMMIT)이라 중간에 에러 나면 자동 롤백됩니다.
--   - 이미 demoprep/demotravel/demodone 로그인 아이디가 존재하는 DB에 두 번 실행하면
--     UNIQUE 제약(login_id) 위반으로 실패합니다 — 재실행 전 기존 계정 삭제가 필요합니다.
--   - countries.id(프랑스=1/스위스=2/독일=3/일본=4/태국=24), spending_categories.id,
--     checklist_templates.id, travel_cards.id(1,2) 등 참조 ID는 이 스크립트를 만들 당시
--     로컬 개발 DB 기준입니다. 배포 DB의 시드 데이터(countries 등 기준정보)가 다르게
--     세팅되어 있다면 해당 ID를 먼저 확인하고 스크립트를 조정해야 합니다.
-- ============================================================================

START TRANSACTION;
SET NAMES utf8mb4;

-- ===================== USER: demoprep =====================
INSERT INTO users (login_id, password, name, phone_number, login_provider, current_view_mode)
VALUES ('demoprep', '$2y$10$asMTswO1AjoOP2O.x51Rie407EG4Nvux9oXjXGHwzIN6dkOUU0xfi', '여행준비생', '010-9100-0001', 'LOCAL', 'SAVING');
SET @prep_uid = LAST_INSERT_ID();

INSERT INTO notification_settings (user_id, all_enabled, travel_schedule_enabled, exchange_rate_enabled, checklist_enabled, travel_report_enabled)
VALUES (@prep_uid, true, true, true, true, true);

INSERT INTO wallet (user_id, balance_amount, status, version) VALUES (@prep_uid, 0.00, 'ACTIVE', 0);
SET @prep_wid = LAST_INSERT_ID();

INSERT INTO trip_wallets (user_id, balance) VALUES (@prep_uid, 0.00);

INSERT INTO codef_connections (user_id, connected_id, connection_status)
VALUES (@prep_uid, 'MOCK-CONNECTED-DEMO-PREP', 'ACTIVE');
SET @prep_connid = LAST_INSERT_ID();

INSERT INTO codef_connected_institutions (codef_connection_id, organization_code, organization_name, business_type, connection_status)
VALUES
  (@prep_connid, '0004', 'KB국민은행', 'BK', 'CONNECTED'),
  (@prep_connid, '0301', 'KB카드', 'CD', 'CONNECTED');

INSERT INTO accounts (user_id, codef_connection_id, organization_code, account_name, account_number, account_type, balance, withdrawable_amount, recognized_amount, is_travel_fund_included, connection_type, is_active)
VALUES (@prep_uid, @prep_connid, '0004', 'KB국민은행 급여통장', '110000000001', 'CHECKING', 2145000, 2145000, 0, 0, 'CODEF', 1);
SET @prep_acc1 = LAST_INSERT_ID();

INSERT INTO accounts (user_id, codef_connection_id, organization_code, account_name, account_number, account_type, balance, withdrawable_amount, recognized_amount, is_travel_fund_included, connection_type, is_active)
VALUES (@prep_uid, @prep_connid, '0004', 'KB국민은행 여행저축통장', '110000000002', 'SAVINGS', 780000, 780000, 780000, 1, 'CODEF', 1);
SET @prep_acc2 = LAST_INSERT_ID();

INSERT INTO cards (user_id, codef_connection_id, card_name, masked_card_number, card_type, organization_code)
VALUES (@prep_uid, @prep_connid, 'KB Star 체크카드', '5412-****-****-3301', 'CHECK', '0301');
SET @prep_card1 = LAST_INSERT_ID();

-- 여행 계획: 일본, PLANNING, 출국 D-8
INSERT INTO trips (user_id, trip_name, status, start_date, end_date, total_target_amount)
VALUES (@prep_uid, '도쿄 첫 자유여행', 'PLANNING', '2026-08-28', '2026-09-04', 842000);
SET @prep_trip = LAST_INSERT_ID();

INSERT INTO trip_countries (trip_id, country_id, arrival_date, departure_date, target_budget, display_order)
VALUES (@prep_trip, 4, '2026-08-28', '2026-09-04', 842000, 1);
SET @prep_tc_jp = LAST_INSERT_ID();

INSERT INTO trip_budget_recommendations (trip_country_id, traveler_count, travel_style,
  recommended_airfare_amount, recommended_lodging_amount, recommended_activity_amount, recommended_transport_amount, recommended_food_amount, recommended_other_amount,
  confirmed_airfare_amount, confirmed_lodging_amount, confirmed_activity_amount, confirmed_transport_amount, confirmed_food_amount, confirmed_other_amount,
  ai_reason, ai_model, is_confirmed)
VALUES (@prep_tc_jp, 1, 'MID_RANGE',
  400000, 630000, 280000, 72000, 360000, 120000,
  400000, 630000, 300000, 72000, 350000, 120000,
  'KAYAK 항공권가, 도쿄메트로 요금 기준 7박8일 추천 예산입니다.', 'gpt-demo', 1);

INSERT INTO saving_plans (trip_id, account_id, monthly_amount, goal_status)
VALUES (@prep_trip, @prep_acc2, 260000, 'ACTIVE');

INSERT INTO wallet_auto_saving_rule (wallet_id, source_account_id, amount, day_of_month, enabled, next_transfer_date)
VALUES (@prep_wid, @prep_acc1, 260000, 10, 1, '2026-09-10');

INSERT INTO wallet_auto_saving_logs (wallet_id, status, amount, reason, executed_at) VALUES
(@prep_wid, 'SUCCESS', 260000, NULL, '2026-05-10 12:00:00'),
(@prep_wid, 'SUCCESS', 260000, NULL, '2026-06-10 12:00:00'),
(@prep_wid, 'SUCCESS', 260000, NULL, '2026-07-10 12:00:00'),
(@prep_wid, 'SUCCESS', 260000, NULL, '2026-08-10 12:00:00');

INSERT INTO wallet_ledger (wallet_id, direction, transaction_type, transfer_method, amount, balance_before, balance_after, source_type, source_id, target_type, target_id, idempotency_key, memo) VALUES
(@prep_wid, 'IN', 'CHARGE', 'AUTO', 260000, 0, 260000, 'ACCOUNT', @prep_acc1, 'WALLET', @prep_wid, 'PREP-AUTO-202605', '자동저축'),
(@prep_wid, 'IN', 'CHARGE', 'AUTO', 260000, 260000, 520000, 'ACCOUNT', @prep_acc1, 'WALLET', @prep_wid, 'PREP-AUTO-202606', '자동저축'),
(@prep_wid, 'IN', 'CHARGE', 'AUTO', 260000, 520000, 780000, 'ACCOUNT', @prep_acc1, 'WALLET', @prep_wid, 'PREP-AUTO-202607', '자동저축');
UPDATE wallet SET balance_amount = 780000 WHERE id = @prep_wid;

-- D-30 체크리스트 (8개 중 6개 완료)
INSERT INTO trip_checklist_items (trip_id, template_id, checklist_type, dday_stage, item_name, is_completed) VALUES
(@prep_trip, 1, 'PRE_TRAVEL', 'D30', '항공권 예약 확인', 1),
(@prep_trip, 2, 'PRE_TRAVEL', 'D30', '숙소 예약하기', 1),
(@prep_trip, 3, 'PRE_TRAVEL', 'D30', '여권 유효기간 확인', 1),
(@prep_trip, 4, 'PRE_TRAVEL', 'D30', '국제운전면허증 발급', 0),
(@prep_trip, 5, 'PRE_TRAVEL', 'D30', '여행자보험 가입', 1),
(@prep_trip, 6, 'PRE_TRAVEL', 'D30', '여행지 맛집·카페 저장해두기', 1),
(@prep_trip, 7, 'PRE_TRAVEL', 'D30', '가고 싶은 관광지·명소 코스 짜기', 1),
(@prep_trip, 8, 'PRE_TRAVEL', 'D30', '트래블카드·월렛 준비하기', 0);
-- D-7 체크리스트 (7개 중 2개 완료 - 임박)
INSERT INTO trip_checklist_items (trip_id, template_id, checklist_type, dday_stage, item_name, is_completed) VALUES
(@prep_trip, 9, 'PRE_TRAVEL', 'D7', '환전 완료하기', 1),
(@prep_trip, 10, 'PRE_TRAVEL', 'D7', '여행 필수템 구매하기', 1),
(@prep_trip, 11, 'PRE_TRAVEL', 'D7', '여행에 유용한 앱 설치하기', 0),
(@prep_trip, 12, 'PRE_TRAVEL', 'D7', '해외 결제 카드 확인', 0),
(@prep_trip, 13, 'PRE_TRAVEL', 'D7', '포켓 와이파이 / 유심·eSIM 신청', 0),
(@prep_trip, 14, 'PRE_TRAVEL', 'D7', '여행 의류 및 착장 정하기', 0),
(@prep_trip, 15, 'PRE_TRAVEL', 'D7', '상비약 챙기기', 0);
-- D-1 체크리스트 (전부 미완료)
INSERT INTO trip_checklist_items (trip_id, template_id, checklist_type, dday_stage, item_name, is_completed) VALUES
(@prep_trip, 16, 'PRE_TRAVEL', 'D1', '여권 챙기기', 0),
(@prep_trip, 17, 'PRE_TRAVEL', 'D1', '항공권·탑승 정보 확인', 0),
(@prep_trip, 18, 'PRE_TRAVEL', 'D1', '수하물 규정 무게 확인', 0),
(@prep_trip, 19, 'PRE_TRAVEL', 'D1', '전자기기 충전기 및 어댑터 챙기기', 0),
(@prep_trip, 20, 'PRE_TRAVEL', 'D1', '보조배터리 챙기기', 0);
-- demoprep 계좌 거래내역 (4~8월)
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-04-01', 'DEPOSIT', 'DOMESTIC', 2800000, 4945000, '4월 급여', 0, 'PREP-BK-20260401-SAL4');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-04-10', 'WITHDRAWAL', 'DOMESTIC', 260000, 4685000, '여행 저축 이체', 0, 'PREP-BK-20260410-SAV4');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-04-05', 'WITHDRAWAL', 'DOMESTIC', 280000, 4405000, 'KB카드 결제', 0, 'PREP-BK-20260405-CARD4');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-05-01', 'DEPOSIT', 'DOMESTIC', 2800000, 7205000, '5월 급여', 0, 'PREP-BK-20260501-SAL5');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-05-10', 'WITHDRAWAL', 'DOMESTIC', 260000, 6945000, '여행 저축 이체', 0, 'PREP-BK-20260510-SAV5');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-05-05', 'WITHDRAWAL', 'DOMESTIC', 295000, 6650000, 'KB카드 결제', 0, 'PREP-BK-20260505-CARD5');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-06-01', 'DEPOSIT', 'DOMESTIC', 2800000, 9450000, '6월 급여', 0, 'PREP-BK-20260601-SAL6');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-06-10', 'WITHDRAWAL', 'DOMESTIC', 260000, 9190000, '여행 저축 이체', 0, 'PREP-BK-20260610-SAV6');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-06-05', 'WITHDRAWAL', 'DOMESTIC', 310000, 8880000, 'KB카드 결제', 0, 'PREP-BK-20260605-CARD6');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-07-01', 'DEPOSIT', 'DOMESTIC', 2800000, 11680000, '7월 급여', 0, 'PREP-BK-20260701-SAL7');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-07-10', 'WITHDRAWAL', 'DOMESTIC', 260000, 11420000, '여행 저축 이체', 0, 'PREP-BK-20260710-SAV7');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-07-05', 'WITHDRAWAL', 'DOMESTIC', 325000, 11095000, 'KB카드 결제', 0, 'PREP-BK-20260705-CARD7');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-08-01', 'DEPOSIT', 'DOMESTIC', 2800000, 13895000, '8월 급여', 0, 'PREP-BK-20260801-SAL8');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-08-10', 'WITHDRAWAL', 'DOMESTIC', 260000, 13635000, '여행 저축 이체', 0, 'PREP-BK-20260810-SAV8');
INSERT INTO transactions (account_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_type, transaction_region, amount, balance_after, merchant_name, is_pre_expense, external_key)
VALUES (@prep_acc1, NULL, NULL, NULL, NULL, NULL, '2026-08-05', 'WITHDRAWAL', 'DOMESTIC', 340000, 13295000, 'KB카드 결제', 0, 'PREP-BK-20260805-CARD8');
UPDATE accounts SET balance = 13295000, withdrawable_amount = 13295000 WHERE id = @prep_acc1;
-- demoprep 카드 거래내역 (국내 소비, 카테고리 다양)
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 1, 'RULE_BASED', NULL, NULL, NULL, '2026-04-03', '09:00:00', 'WITHDRAWAL', 'DOMESTIC', 6000, '한상차림', '일반음식점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260403-090000-4-3');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 7, 'RULE_BASED', NULL, NULL, NULL, '2026-04-07', '10:13:00', 'WITHDRAWAL', 'DOMESTIC', 7500, '카페모먼트', '커피전문점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260407-101300-4-7');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 4, 'RULE_BASED', NULL, NULL, NULL, '2026-04-12', '11:26:00', 'WITHDRAWAL', 'DOMESTIC', 9000, '스타일샵', '일반의류', NULL, NULL, '체크카드', 0, 'PREP-CD-20260412-112600-4-12');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 8, 'RULE_BASED', NULL, NULL, NULL, '2026-04-18', '12:39:00', 'WITHDRAWAL', 'DOMESTIC', 10500, 'CU편의점', '편의점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260418-123900-4-18');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 2, 'RULE_BASED', NULL, NULL, NULL, '2026-04-24', '13:52:00', 'WITHDRAWAL', 'DOMESTIC', 12000, '카카오택시', '택시', NULL, NULL, '체크카드', 0, 'PREP-CD-20260424-135200-4-24');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 1, 'RULE_BASED', NULL, NULL, NULL, '2026-05-03', '09:00:00', 'WITHDRAWAL', 'DOMESTIC', 6400, '한상차림', '일반음식점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260503-090000-5-3');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 7, 'RULE_BASED', NULL, NULL, NULL, '2026-05-07', '10:13:00', 'WITHDRAWAL', 'DOMESTIC', 7900, '카페모먼트', '커피전문점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260507-101300-5-7');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 4, 'RULE_BASED', NULL, NULL, NULL, '2026-05-12', '11:26:00', 'WITHDRAWAL', 'DOMESTIC', 9400, '스타일샵', '일반의류', NULL, NULL, '체크카드', 0, 'PREP-CD-20260512-112600-5-12');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 8, 'RULE_BASED', NULL, NULL, NULL, '2026-05-18', '12:39:00', 'WITHDRAWAL', 'DOMESTIC', 10900, 'CU편의점', '편의점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260518-123900-5-18');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 2, 'RULE_BASED', NULL, NULL, NULL, '2026-05-24', '13:52:00', 'WITHDRAWAL', 'DOMESTIC', 12400, '카카오택시', '택시', NULL, NULL, '체크카드', 0, 'PREP-CD-20260524-135200-5-24');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 1, 'RULE_BASED', NULL, NULL, NULL, '2026-06-03', '09:00:00', 'WITHDRAWAL', 'DOMESTIC', 6800, '한상차림', '일반음식점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260603-090000-6-3');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 7, 'RULE_BASED', NULL, NULL, NULL, '2026-06-07', '10:13:00', 'WITHDRAWAL', 'DOMESTIC', 8300, '카페모먼트', '커피전문점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260607-101300-6-7');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 4, 'RULE_BASED', NULL, NULL, NULL, '2026-06-12', '11:26:00', 'WITHDRAWAL', 'DOMESTIC', 9800, '스타일샵', '일반의류', NULL, NULL, '체크카드', 0, 'PREP-CD-20260612-112600-6-12');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 8, 'RULE_BASED', NULL, NULL, NULL, '2026-06-18', '12:39:00', 'WITHDRAWAL', 'DOMESTIC', 11300, 'CU편의점', '편의점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260618-123900-6-18');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 2, 'RULE_BASED', NULL, NULL, NULL, '2026-06-24', '13:52:00', 'WITHDRAWAL', 'DOMESTIC', 12800, '카카오택시', '택시', NULL, NULL, '체크카드', 0, 'PREP-CD-20260624-135200-6-24');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 1, 'RULE_BASED', NULL, NULL, NULL, '2026-07-03', '09:00:00', 'WITHDRAWAL', 'DOMESTIC', 7200, '한상차림', '일반음식점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260703-090000-7-3');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 7, 'RULE_BASED', NULL, NULL, NULL, '2026-07-07', '10:13:00', 'WITHDRAWAL', 'DOMESTIC', 8700, '카페모먼트', '커피전문점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260707-101300-7-7');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 4, 'RULE_BASED', NULL, NULL, NULL, '2026-07-12', '11:26:00', 'WITHDRAWAL', 'DOMESTIC', 10200, '스타일샵', '일반의류', NULL, NULL, '체크카드', 0, 'PREP-CD-20260712-112600-7-12');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 8, 'RULE_BASED', NULL, NULL, NULL, '2026-07-18', '12:39:00', 'WITHDRAWAL', 'DOMESTIC', 11700, 'CU편의점', '편의점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260718-123900-7-18');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 2, 'RULE_BASED', NULL, NULL, NULL, '2026-07-24', '13:52:00', 'WITHDRAWAL', 'DOMESTIC', 13200, '카카오택시', '택시', NULL, NULL, '체크카드', 0, 'PREP-CD-20260724-135200-7-24');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 1, 'RULE_BASED', NULL, NULL, NULL, '2026-08-03', '09:00:00', 'WITHDRAWAL', 'DOMESTIC', 7600, '한상차림', '일반음식점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260803-090000-8-3');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 7, 'RULE_BASED', NULL, NULL, NULL, '2026-08-07', '10:13:00', 'WITHDRAWAL', 'DOMESTIC', 9100, '카페모먼트', '커피전문점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260807-101300-8-7');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 4, 'RULE_BASED', NULL, NULL, NULL, '2026-08-12', '11:26:00', 'WITHDRAWAL', 'DOMESTIC', 10600, '스타일샵', '일반의류', NULL, NULL, '체크카드', 0, 'PREP-CD-20260812-112600-8-12');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@prep_card1, 8, 'RULE_BASED', NULL, NULL, NULL, '2026-08-18', '12:39:00', 'WITHDRAWAL', 'DOMESTIC', 12100, 'CU편의점', '편의점', NULL, NULL, '체크카드', 0, 'PREP-CD-20260818-123900-8-18');

-- ===================== USER: demotravel =====================
INSERT INTO users (login_id, password, name, phone_number, login_provider, current_view_mode)
VALUES ('demotravel', '$2y$10$asMTswO1AjoOP2O.x51Rie407EG4Nvux9oXjXGHwzIN6dkOUU0xfi', '여행중테스터', '010-9100-0002', 'LOCAL', 'SAVING');
SET @trv_uid = LAST_INSERT_ID();

INSERT INTO notification_settings (user_id, all_enabled, travel_schedule_enabled, exchange_rate_enabled, checklist_enabled, travel_report_enabled)
VALUES (@trv_uid, true, true, true, true, true);

INSERT INTO wallet (user_id, balance_amount, status, version) VALUES (@trv_uid, 0.00, 'ACTIVE', 0);
SET @trv_wid = LAST_INSERT_ID();

INSERT INTO trip_wallets (user_id, balance) VALUES (@trv_uid, 0.00);

INSERT INTO codef_connections (user_id, connected_id, connection_status)
VALUES (@trv_uid, 'MOCK-CONNECTED-DEMO-TRAVEL', 'ACTIVE');
SET @trv_connid = LAST_INSERT_ID();

INSERT INTO codef_connected_institutions (codef_connection_id, organization_code, organization_name, business_type, connection_status)
VALUES
  (@trv_connid, '0004', 'KB국민은행', 'BK', 'CONNECTED'),
  (@trv_connid, '0301', 'KB카드', 'CD', 'CONNECTED');

INSERT INTO accounts (user_id, codef_connection_id, organization_code, account_name, account_number, account_type, balance, withdrawable_amount, recognized_amount, is_travel_fund_included, connection_type, is_active)
VALUES (@trv_uid, @trv_connid, '0004', 'KB국민은행 여행통장', '110000000001', 'SAVINGS', 320000, 320000, 320000, 1, 'CODEF', 1);
SET @trv_acc1 = LAST_INSERT_ID();

INSERT INTO accounts (user_id, codef_connection_id, organization_code, account_name, account_number, account_type, balance, withdrawable_amount, recognized_amount, is_travel_fund_included, connection_type, is_active)
VALUES (@trv_uid, @trv_connid, '0004', 'KB국민은행 생활비통장', '110000000002', 'CHECKING', 1580000, 1580000, 0, 0, 'CODEF', 1);
SET @trv_acc2 = LAST_INSERT_ID();

INSERT INTO cards (user_id, codef_connection_id, card_name, masked_card_number, card_type, organization_code)
VALUES (@trv_uid, @trv_connid, 'KB 트래블러스 체크카드', '5412-****-****-8801', 'CHECK', '0301');
SET @trv_card1 = LAST_INSERT_ID();

INSERT INTO trips (user_id, trip_name, status, start_date, end_date, total_target_amount)
VALUES (@trv_uid, '유럽 3개국 배낭여행', 'TRAVELING', '2026-08-12', '2026-08-29', 3028000);
SET @trv_trip = LAST_INSERT_ID();

INSERT INTO trip_countries (trip_id, country_id, arrival_date, departure_date, target_budget, display_order) VALUES
(@trv_trip, 1, '2026-08-12', '2026-08-17', 1038000, 1),
(@trv_trip, 3, '2026-08-18', '2026-08-24', 1015000, 2),
(@trv_trip, 2, '2026-08-25', '2026-08-29', 975000, 3);
SET @trv_tc_fr = (SELECT id FROM trip_countries WHERE trip_id = @trv_trip AND country_id = 1);
SET @trv_tc_de = (SELECT id FROM trip_countries WHERE trip_id = @trv_trip AND country_id = 3);
SET @trv_tc_ch = (SELECT id FROM trip_countries WHERE trip_id = @trv_trip AND country_id = 2);

INSERT INTO trip_budget_recommendations (trip_country_id, traveler_count, travel_style,
  recommended_airfare_amount, recommended_lodging_amount, recommended_activity_amount, recommended_transport_amount, recommended_food_amount, recommended_other_amount,
  confirmed_airfare_amount, confirmed_lodging_amount, confirmed_activity_amount, confirmed_transport_amount, confirmed_food_amount, confirmed_other_amount,
  ai_model, is_confirmed) VALUES
(@trv_tc_fr, 1, 'MID_RANGE', 1200000, 650000, 270000, 108000, 510000, 150000, 1200000, 650000, 270000, 108000, 510000, 150000, 'gpt-demo', 1),
(@trv_tc_de, 1, 'MID_RANGE', 0, 660000, 280000, 175000, 420000, 140000, 0, 660000, 280000, 175000, 420000, 140000, 'gpt-demo', 1),
(@trv_tc_ch, 1, 'MID_RANGE', 0, 800000, 275000, 200000, 350000, 150000, 0, 800000, 275000, 200000, 350000, 150000, 'gpt-demo', 1);

INSERT INTO saving_plans (trip_id, account_id, monthly_amount, goal_status)
VALUES (@trv_trip, @trv_acc1, 350000, 'ACTIVE');

-- 여행 전 자동저축 이력 (4~8월, 이미 완료된 과거)
INSERT INTO wallet_auto_saving_logs (wallet_id, status, amount, reason, executed_at) VALUES
(@trv_wid, 'SUCCESS', 350000, NULL, '2026-04-10 12:00:00'),
(@trv_wid, 'SUCCESS', 350000, NULL, '2026-05-10 12:00:00'),
(@trv_wid, 'SUCCESS', 350000, NULL, '2026-06-10 12:00:00'),
(@trv_wid, 'SUCCESS', 400000, NULL, '2026-07-10 12:00:00');
UPDATE wallet SET balance_amount = 0 WHERE id = @trv_wid;

-- PRE_TRAVEL 체크리스트: 이미 출발했으므로 전부 완료
INSERT INTO trip_checklist_items (trip_id, template_id, checklist_type, dday_stage, item_name, is_completed)
SELECT @trv_trip, id, checklist_type, dday_stage, item_name, 1 FROM checklist_templates WHERE checklist_type = 'PRE_TRAVEL';
-- RETURN 체크리스트: 아직 여행 안 끝났으므로 전부 미완료
INSERT INTO trip_checklist_items (trip_id, template_id, checklist_type, dday_stage, item_name, is_completed)
SELECT @trv_trip, id, checklist_type, dday_stage, item_name, 0 FROM checklist_templates WHERE checklist_type = 'RETURN';

INSERT INTO trip_schedules (trip_id, trip_country_id, currency_id, schedule_name, scheduled_at, amount, payment_status, schedule_status, place_name, place_address) VALUES
(@trv_trip, @trv_tc_fr, 2, '루브르 박물관 관람', '2026-08-13 10:00:00', 25000, 'PREPAID', 'DONE', '루브르 박물관', '파리, 프랑스'),
(@trv_trip, @trv_tc_fr, 2, '에펠탑 전망대 투어', '2026-08-14 18:00:00', 32000, 'PREPAID', 'DONE', '에펠탑', '파리, 프랑스'),
(@trv_trip, @trv_tc_fr, 2, '센강 디너 크루즈', '2026-08-15 19:30:00', 89000, 'PREPAID', 'DONE', '센강', '파리, 프랑스'),
(@trv_trip, @trv_tc_fr, 2, '몽마르뜨 언덕 산책', '2026-08-16 15:00:00', 0, 'UNDECIDED', 'DONE', '몽마르뜨', '파리, 프랑스');
-- 독일 (진행중) - 지난 일정 + 오늘(8/20) 일정 + 향후
INSERT INTO trip_schedules (trip_id, trip_country_id, currency_id, schedule_name, scheduled_at, amount, payment_status, schedule_status, place_name, place_address) VALUES
(@trv_trip, @trv_tc_de, 2, '노이슈반슈타인 성 투어', '2026-08-18 09:00:00', 45000, 'PREPAID', 'DONE', '노이슈반슈타인 성', '퓌센, 독일'),
(@trv_trip, @trv_tc_de, 2, '뮌헨 시청사 광장', '2026-08-19 14:00:00', 0, 'UNDECIDED', 'DONE', '마리엔 광장', '뮌헨, 독일'),
(@trv_trip, @trv_tc_de, 2, '호프브로이하우스 저녁식사', '2026-08-20 19:00:00', 38000, 'ONSITE', 'UPCOMING', '호프브로이하우스', '뮌헨, 독일'),
(@trv_trip, @trv_tc_de, 2, 'BMW 박물관 관람', '2026-08-21 11:00:00', 28000, 'PREPAID', 'UPCOMING', 'BMW 벨트', '뮌헨, 독일'),
(@trv_trip, @trv_tc_de, 2, '님펜부르크 궁전', '2026-08-22 10:00:00', 20000, 'PREPAID', 'UPCOMING', '님펜부르크 궁전', '뮌헨, 독일');
-- 스위스 (예정)
INSERT INTO trip_schedules (trip_id, trip_country_id, currency_id, schedule_name, scheduled_at, amount, payment_status, schedule_status, place_name, place_address) VALUES
(@trv_trip, @trv_tc_ch, 4, '융프라우요흐 등반열차', '2026-08-26 08:00:00', 250000, 'PREPAID', 'UPCOMING', '융프라우요흐역', '인터라켄, 스위스'),
(@trv_trip, @trv_tc_ch, 4, '루체른 카펠교 산책', '2026-08-27 16:00:00', 0, 'UNDECIDED', 'UPCOMING', '카펠교', '루체른, 스위스'),
(@trv_trip, @trv_tc_ch, 4, '취리히 호수 유람선', '2026-08-28 13:00:00', 45000, 'ONSITE', 'UPCOMING', '취리히 호수', '취리히, 스위스');

INSERT INTO user_travel_cards (user_id, travel_card_id, card_name, issuer_name, masked_card_number, brand_name, card_color, status)
VALUES (@trv_uid, 1, 'KB 트래블러스 체크카드', 'KB국민카드', '5412-****-****-9021', 'Mastercard', '#5B7FDB', 'ACTIVE');
SET @trv_utc = LAST_INSERT_ID();

INSERT INTO wallet_travel_card (wallet_id, user_travel_card_id, travel_card_id, card_name, issuer_name, masked_card_number, status)
VALUES (@trv_wid, @trv_utc, 1, 'KB 트래블러스 체크카드', 'KB국민카드', '5412-****-****-9021', 'LINKED');
SET @trv_wtc = LAST_INSERT_ID();

INSERT INTO travel_card_balance (wallet_travel_card_id, currency_code, balance_amount, krw_estimated_amount) VALUES
(@trv_wtc, 'EUR', 185.40, 267000),
(@trv_wtc, 'CHF', 420.00, 630000);

INSERT INTO travel_card_ledger (wallet_travel_card_id, currency_code, direction, transaction_type, foreign_amount, balance_before, balance_after, source_type, source_id, target_type, target_id, idempotency_key, memo) VALUES
(@trv_wtc, 'EUR', 'IN', 'CHARGE', 500.00, 0, 500.00, 'WALLET', @trv_wid, 'TRAVEL_CARD', @trv_wtc, 'TRV-CHG-EUR-01', '출국 전 환전 충전'),
(@trv_wtc, 'CHF', 'IN', 'CHARGE', 600.00, 0, 600.00, 'WALLET', @trv_wid, 'TRAVEL_CARD', @trv_wtc, 'TRV-CHG-CHF-01', '출국 전 환전 충전'),
(@trv_wtc, 'CHF', 'OUT', 'CARD_TOPUP', 180.00, 600.00, 420.00, 'TRAVEL_CARD', @trv_wtc, 'TRAVEL_CARD', @trv_wtc, 'TRV-USE-CHF-01', '스위스 숙박 사전 결제');
-- demotravel 카드 거래: 프랑스 구간 (사전지출+여행중지출)
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key) VALUES
(@trv_card1, 2, 'RULE_BASED', @trv_trip, @trv_tc_fr, 1, '2026-07-15', '14:00:00', 'WITHDRAWAL', 'DOMESTIC', 1200000, '대한항공', '항공사', NULL, NULL, '체크카드', 1, 'TRV-PRE-AIR-01'),
(@trv_card1, 3, 'RULE_BASED', @trv_trip, @trv_tc_fr, 1, '2026-07-20', '10:00:00', 'WITHDRAWAL', 'DOMESTIC', 650000, '호텔스닷컴 파리', '숙박', NULL, NULL, '체크카드', 1, 'TRV-PRE-LDG-FR');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 2, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-12', '14:30:00', 'WITHDRAWAL', 'OVERSEAS', 48000, 'TAXI PARISIEN', '해외교통', 30.97, 1550, '체크카드', 0, 'TRV-CD-20260812-143000-2026-08-12-14:30');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 1, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-12', '19:00:00', 'WITHDRAWAL', 'OVERSEAS', 35000, 'LE PETIT BISTRO', '해외음식점', 22.58, 1550, '체크카드', 0, 'TRV-CD-20260812-190000-2026-08-12-19:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 4, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-12', '21:00:00', 'WITHDRAWAL', 'OVERSEAS', 22000, 'GALERIES LAFAYETTE', '해외쇼핑', 14.19, 1550, '체크카드', 0, 'TRV-CD-20260812-210000-2026-08-12-21:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 7, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-13', '08:30:00', 'WITHDRAWAL', 'OVERSEAS', 12000, 'CAFE DE FLORE', '해외카페', 7.74, 1550, '체크카드', 0, 'TRV-CD-20260813-083000-2026-08-13-08:30');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 2, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-13', '11:00:00', 'WITHDRAWAL', 'OVERSEAS', 18000, 'METRO PARIS RATP', '해외교통', 11.61, 1550, '체크카드', 0, 'TRV-CD-20260813-110000-2026-08-13-11:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 1, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-13', '20:00:00', 'WITHDRAWAL', 'OVERSEAS', 42000, 'BRASSERIE LIPP', '해외음식점', 27.1, 1550, '체크카드', 0, 'TRV-CD-20260813-200000-2026-08-13-20:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 5, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-14', '09:00:00', 'WITHDRAWAL', 'OVERSEAS', 25000, 'MUSEE DU LOUVRE', '해외관광', 16.13, 1550, '체크카드', 0, 'TRV-CD-20260814-090000-2026-08-14-09:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 1, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-14', '13:00:00', 'WITHDRAWAL', 'OVERSEAS', 28000, 'BOULANGERIE PAIN', '해외음식점', 18.06, 1550, '체크카드', 0, 'TRV-CD-20260814-130000-2026-08-14-13:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 1, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-14', '19:30:00', 'WITHDRAWAL', 'OVERSEAS', 45000, 'LE COMPTOIR PARIS', '해외음식점', 29.03, 1550, '체크카드', 0, 'TRV-CD-20260814-193000-2026-08-14-19:30');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 2, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-15', '08:00:00', 'WITHDRAWAL', 'OVERSEAS', 15000, 'UBER PARIS', '해외교통', 9.68, 1550, '체크카드', 0, 'TRV-CD-20260815-080000-2026-08-15-08:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 5, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-15', '19:30:00', 'WITHDRAWAL', 'OVERSEAS', 89000, 'BATEAUX PARISIENS', '해외관광', 57.42, 1550, '체크카드', 0, 'TRV-CD-20260815-193000-2026-08-15-19:30');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 4, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-16', '10:00:00', 'WITHDRAWAL', 'OVERSEAS', 38000, 'PRINTEMPS PARIS', '해외쇼핑', 24.52, 1550, '체크카드', 0, 'TRV-CD-20260816-100000-2026-08-16-10:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 7, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-16', '20:00:00', 'WITHDRAWAL', 'OVERSEAS', 30000, 'CAFE DE FLORE', '해외카페', 19.35, 1550, '체크카드', 0, 'TRV-CD-20260816-200000-2026-08-16-20:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 4, 'RULE_BASED', @trv_trip, @trv_tc_fr, 2, '2026-08-17', '10:00:00', 'WITHDRAWAL', 'OVERSEAS', 30000, 'DUTY FREE CDG', '해외쇼핑', 19.35, 1550, '체크카드', 0, 'TRV-CD-20260817-100000-2026-08-17-10:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key) VALUES
(@trv_card1, 3, 'RULE_BASED', @trv_trip, @trv_tc_de, 1, '2026-07-22', '10:00:00', 'WITHDRAWAL', 'DOMESTIC', 660000, '호텔스닷컴 뮌헨', '숙박', NULL, NULL, '체크카드', 1, 'TRV-PRE-LDG-DE');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 2, 'RULE_BASED', @trv_trip, @trv_tc_de, 2, '2026-08-18', '15:00:00', 'WITHDRAWAL', 'OVERSEAS', 12000, 'DB BAHN TICKET', '해외교통', 7.74, 1550, '체크카드', 0, 'TRV-CD-20260818-150000-2026-08-18-15:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 1, 'RULE_BASED', @trv_trip, @trv_tc_de, 2, '2026-08-18', '18:00:00', 'WITHDRAWAL', 'OVERSEAS', 25000, 'HOFBRAUHAUS MUNCHEN', '해외음식점', 16.13, 1550, '체크카드', 0, 'TRV-CD-20260818-180000-2026-08-18-18:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 4, 'RULE_BASED', @trv_trip, @trv_tc_de, 2, '2026-08-18', '20:00:00', 'WITHDRAWAL', 'OVERSEAS', 32000, 'KAUFHOF MARIENPLATZ', '해외쇼핑', 20.65, 1550, '체크카드', 0, 'TRV-CD-20260818-200000-2026-08-18-20:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 5, 'RULE_BASED', @trv_trip, @trv_tc_de, 2, '2026-08-19', '09:00:00', 'WITHDRAWAL', 'OVERSEAS', 20000, 'SCHLOSS NYMPHENBURG', '해외관광', 12.9, 1550, '체크카드', 0, 'TRV-CD-20260819-090000-2026-08-19-09:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 1, 'RULE_BASED', @trv_trip, @trv_tc_de, 2, '2026-08-19', '13:00:00', 'WITHDRAWAL', 'OVERSEAS', 18000, 'AUGUSTINER KELLER', '해외음식점', 11.61, 1550, '체크카드', 0, 'TRV-CD-20260819-130000-2026-08-19-13:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 4, 'RULE_BASED', @trv_trip, @trv_tc_de, 2, '2026-08-19', '17:00:00', 'WITHDRAWAL', 'OVERSEAS', 42000, 'MAXIMILIANSTRASSE', '해외쇼핑', 27.1, 1550, '체크카드', 0, 'TRV-CD-20260819-170000-2026-08-19-17:00');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 7, 'RULE_BASED', @trv_trip, @trv_tc_de, 2, '2026-08-20', '08:30:00', 'WITHDRAWAL', 'OVERSEAS', 9500, 'CAFE LUITPOLD', '해외카페', 6.13, 1550, '체크카드', 0, 'TRV-CD-20260820-083000-2026-08-20-08:30');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@trv_card1, 1, 'RULE_BASED', @trv_trip, @trv_tc_de, 2, '2026-08-20', '12:30:00', 'WITHDRAWAL', 'OVERSEAS', 21000, 'VIKTUALIENMARKT', '해외음식점', 13.55, 1550, '체크카드', 0, 'TRV-CD-20260820-123000-2026-08-20-12:30');

INSERT INTO receipts (user_id, trip_id, country_id, category_id, currency_id, payment_datetime, file_name, file_url, file_type, status, merchant_original_name, merchant_translated_name, total_amount, split_count, processed_at) VALUES
(@trv_uid, @trv_trip, 1, 1, 2, '2026-08-12 19:00:00', 'receipt_le_petit_bistro.jpg', 'https://storage.tripass.demo/receipts/receipt_le_petit_bistro.jpg', 'JPG', 'COMPLETED', 'LE PETIT BISTRO', '르 쁘띠 비스트로', 35000, 1, '2026-08-12 19:05:00'),
(@trv_uid, @trv_trip, 1, 4, 2, '2026-08-13 20:00:00', 'receipt_galeries.jpg', 'https://storage.tripass.demo/receipts/receipt_galeries.jpg', 'JPG', 'COMPLETED', 'GALERIES LAFAYETTE', '갤러리 라파예트', 22000, 1, '2026-08-13 20:10:00'),
(@trv_uid, @trv_trip, 1, 5, 2, '2026-08-15 19:30:00', 'receipt_bateaux.jpg', 'https://storage.tripass.demo/receipts/receipt_bateaux.jpg', 'JPG', 'COMPLETED', 'BATEAUX PARISIENS', '바토 파리지앵', 89000, 1, '2026-08-15 19:40:00'),
(@trv_uid, @trv_trip, 3, 1, 2, '2026-08-18 18:00:00', 'receipt_hofbrauhaus.jpg', 'https://storage.tripass.demo/receipts/receipt_hofbrauhaus.jpg', 'JPG', 'COMPLETED', 'HOFBRAUHAUS MUNCHEN', '호프브로이하우스 뮌헨', 25000, 2, '2026-08-18 18:10:00'),
(@trv_uid, @trv_trip, 3, 1, 2, '2026-08-20 12:30:00', 'receipt_viktualienmarkt.jpg', 'https://storage.tripass.demo/receipts/receipt_viktualienmarkt.jpg', 'JPG', 'COMPLETED', 'VIKTUALIENMARKT', '빅투알리엔 시장', 21000, 1, '2026-08-20 12:35:00');
SET @trv_r1 = (SELECT id FROM receipts WHERE user_id=@trv_uid AND merchant_original_name='HOFBRAUHAUS MUNCHEN');

INSERT INTO receipt_items (receipt_id, original_name, translated_name, quantity, amount, display_order) VALUES
(@trv_r1, 'Schweinshaxe', '족발구이', 1, 16000, 1),
(@trv_r1, 'Weissbier', '바이스비어', 2, 9000, 2);

-- ===================== USER: demodone =====================
INSERT INTO users (login_id, password, name, phone_number, login_provider, current_view_mode)
VALUES ('demodone', '$2y$10$asMTswO1AjoOP2O.x51Rie407EG4Nvux9oXjXGHwzIN6dkOUU0xfi', '여행완료유저', '010-9100-0003', 'LOCAL', 'SAVING');
SET @done_uid = LAST_INSERT_ID();

INSERT INTO notification_settings (user_id, all_enabled, travel_schedule_enabled, exchange_rate_enabled, checklist_enabled, travel_report_enabled)
VALUES (@done_uid, true, true, true, true, true);

INSERT INTO wallet (user_id, balance_amount, status, version) VALUES (@done_uid, 0.00, 'ACTIVE', 0);
SET @done_wid = LAST_INSERT_ID();

INSERT INTO trip_wallets (user_id, balance) VALUES (@done_uid, 0.00);

INSERT INTO codef_connections (user_id, connected_id, connection_status)
VALUES (@done_uid, 'MOCK-CONNECTED-DEMO-DONE', 'ACTIVE');
SET @done_connid = LAST_INSERT_ID();

INSERT INTO codef_connected_institutions (codef_connection_id, organization_code, organization_name, business_type, connection_status)
VALUES
  (@done_connid, '0004', 'KB국민은행', 'BK', 'CONNECTED'),
  (@done_connid, '0301', 'KB카드', 'CD', 'CONNECTED');

INSERT INTO accounts (user_id, codef_connection_id, organization_code, account_name, account_number, account_type, balance, withdrawable_amount, recognized_amount, is_travel_fund_included, connection_type, is_active)
VALUES (@done_uid, @done_connid, '0004', 'KB국민은행 여행통장', '110000000001', 'SAVINGS', 1150000, 1150000, 1150000, 1, 'CODEF', 1);
SET @done_acc1 = LAST_INSERT_ID();

INSERT INTO accounts (user_id, codef_connection_id, organization_code, account_name, account_number, account_type, balance, withdrawable_amount, recognized_amount, is_travel_fund_included, connection_type, is_active)
VALUES (@done_uid, @done_connid, '0004', 'KB국민은행 급여통장', '110000000002', 'CHECKING', 3020000, 3020000, 0, 0, 'CODEF', 1);
SET @done_acc2 = LAST_INSERT_ID();

INSERT INTO cards (user_id, codef_connection_id, card_name, masked_card_number, card_type, organization_code)
VALUES (@done_uid, @done_connid, 'KB 트래블러스 체크카드', '5412-****-****-7701', 'CHECK', '0301');
SET @done_card1 = LAST_INSERT_ID();

INSERT INTO trips (user_id, trip_name, status, start_date, end_date, total_target_amount)
VALUES (@done_uid, '방콕 완전정복 2주', 'ENDED', '2026-07-20', '2026-08-03', 1550000);
SET @done_trip = LAST_INSERT_ID();

INSERT INTO trip_countries (trip_id, country_id, arrival_date, departure_date, target_budget, display_order)
VALUES (@done_trip, 24, '2026-07-20', '2026-08-03', 1550000, 1);
SET @done_tc_th = LAST_INSERT_ID();

INSERT INTO trip_budget_recommendations (trip_country_id, traveler_count, travel_style,
  recommended_airfare_amount, recommended_lodging_amount, recommended_activity_amount, recommended_transport_amount, recommended_food_amount, recommended_other_amount,
  confirmed_airfare_amount, confirmed_lodging_amount, confirmed_activity_amount, confirmed_transport_amount, confirmed_food_amount, confirmed_other_amount,
  ai_model, is_confirmed)
VALUES (@done_tc_th, 1, 'MID_RANGE', 550000, 900000, 420000, 150000, 630000, 250000, 550000, 900000, 400000, 150000, 620000, 240000, 'gpt-demo', 1);

INSERT INTO saving_plans (trip_id, account_id, monthly_amount, goal_status)
VALUES (@done_trip, @done_acc1, 300000, 'COMPLETED');

INSERT INTO wallet_auto_saving_logs (wallet_id, status, amount, reason, executed_at) VALUES
(@done_wid, 'SUCCESS', 300000, NULL, '2026-05-10 12:00:00'),
(@done_wid, 'SUCCESS', 300000, NULL, '2026-06-10 12:00:00'),
(@done_wid, 'SUCCESS', 300000, NULL, '2026-07-10 12:00:00');

-- PRE_TRAVEL + RETURN 체크리스트 전부 완료 (여행 종료됨)
INSERT INTO trip_checklist_items (trip_id, template_id, checklist_type, dday_stage, item_name, is_completed)
SELECT @done_trip, id, checklist_type, dday_stage, item_name, 1 FROM checklist_templates;

-- 여행 전 항공/숙박 사전지출
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, payment_method, is_pre_expense, external_key) VALUES
(@done_card1, 2, 'RULE_BASED', @done_trip, @done_tc_th, 1, '2026-06-25', '10:00:00', 'WITHDRAWAL', 'DOMESTIC', 550000, '타이항공', '항공사', '체크카드', 1, 'DONE-PRE-AIR-01'),
(@done_card1, 3, 'RULE_BASED', @done_trip, @done_tc_th, 1, '2026-06-28', '11:00:00', 'WITHDRAWAL', 'DOMESTIC', 900000, '아고다 방콕호텔', '숙박', '체크카드', 1, 'DONE-PRE-LDG-01');

-- 귀국 후 정산 완료 상태(트래블카드 잔액 0으로 매도 완료)
INSERT INTO user_travel_cards (user_id, travel_card_id, card_name, issuer_name, masked_card_number, brand_name, card_color, status)
VALUES (@done_uid, 2, '하나 트래블로그 카드', '하나카드', '5312-****-****-4402', 'Visa', '#2FBF8F', 'ACTIVE');
SET @done_utc = LAST_INSERT_ID();
INSERT INTO wallet_travel_card (wallet_id, user_travel_card_id, travel_card_id, card_name, issuer_name, masked_card_number, status)
VALUES (@done_wid, @done_utc, 2, '하나 트래블로그 카드', '하나카드', '5312-****-****-4402', 'LINKED');
SET @done_wtc = LAST_INSERT_ID();
INSERT INTO travel_card_balance (wallet_travel_card_id, currency_code, balance_amount, krw_estimated_amount) VALUES (@done_wtc, 'THB', 0, 0);
-- demodone 태국 여행중 지출 (7/20~8/3)
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 1, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-20', '11:14:00', 'WITHDRAWAL', 'OVERSEAS', 21024, 'S&P Restaurant', '해외음식점', 546.08, 38.5, '체크카드', 0, 'DONE-CD-20260720-111400-20-11:14:00-S&P');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 7, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-20', '09:37:00', 'WITHDRAWAL', 'OVERSEAS', 38741, '애프터유 디저트', '해외카페', 1006.26, 38.5, '체크카드', 0, 'DONE-CD-20260720-093700-20-09:37:00-애프터');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 4, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-20', '09:13:00', 'WITHDRAWAL', 'OVERSEAS', 4952, '시암 파라곤', '해외쇼핑', 128.62, 38.5, '체크카드', 0, 'DONE-CD-20260720-091300-20-09:13:00-시암 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 7, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-20', '11:45:00', 'WITHDRAWAL', 'OVERSEAS', 39781, '애프터유 디저트', '해외카페', 1033.27, 38.5, '체크카드', 0, 'DONE-CD-20260720-114500-20-11:45:00-애프터');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-21', '15:37:00', 'WITHDRAWAL', 'OVERSEAS', 17446, '왕궁 투어', '해외관광', 453.14, 38.5, '체크카드', 0, 'DONE-CD-20260721-153700-21-15:37:00-왕궁 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 2, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-21', '19:27:00', 'WITHDRAWAL', 'OVERSEAS', 13463, '그랩택시', '해외교통', 349.69, 38.5, '체크카드', 0, 'DONE-CD-20260721-192700-21-19:27:00-그랩택');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 2, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-21', '11:48:00', 'WITHDRAWAL', 'OVERSEAS', 13189, 'BTS 스카이트레인', '해외교통', 342.57, 38.5, '체크카드', 0, 'DONE-CD-20260721-114800-21-11:48:00-BTS');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 2, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-21', '14:06:00', 'WITHDRAWAL', 'OVERSEAS', 9078, '그랩택시', '해외교통', 235.79, 38.5, '체크카드', 0, 'DONE-CD-20260721-140600-21-14:06:00-그랩택');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 2, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-22', '20:02:00', 'WITHDRAWAL', 'OVERSEAS', 20335, '뚝뚝', '해외교통', 528.18, 38.5, '체크카드', 0, 'DONE-CD-20260722-200200-22-20:02:00-뚝뚝');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 4, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-22', '14:05:00', 'WITHDRAWAL', 'OVERSEAS', 11180, '터미널21', '해외쇼핑', 290.39, 38.5, '체크카드', 0, 'DONE-CD-20260722-140500-22-14:05:00-터미널');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-22', '17:56:00', 'WITHDRAWAL', 'OVERSEAS', 44198, '왕궁 투어', '해외관광', 1148.0, 38.5, '체크카드', 0, 'DONE-CD-20260722-175600-22-17:56:00-왕궁 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-23', '08:42:00', 'WITHDRAWAL', 'OVERSEAS', 7558, '왓포 사원', '해외관광', 196.31, 38.5, '체크카드', 0, 'DONE-CD-20260723-084200-23-08:42:00-왓포 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 7, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-23', '21:14:00', 'WITHDRAWAL', 'OVERSEAS', 8229, '와와 카페', '해외카페', 213.74, 38.5, '체크카드', 0, 'DONE-CD-20260723-211400-23-21:14:00-와와 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 1, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-23', '15:40:00', 'WITHDRAWAL', 'OVERSEAS', 21217, '코코넛 아이스크림', '해외음식점', 551.09, 38.5, '체크카드', 0, 'DONE-CD-20260723-154000-23-15:40:00-코코넛');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 7, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-24', '11:42:00', 'WITHDRAWAL', 'OVERSEAS', 26283, '와와 카페', '해외카페', 682.68, 38.5, '체크카드', 0, 'DONE-CD-20260724-114200-24-11:42:00-와와 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 2, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-24', '17:40:00', 'WITHDRAWAL', 'OVERSEAS', 7679, '뚝뚝', '해외교통', 199.45, 38.5, '체크카드', 0, 'DONE-CD-20260724-174000-24-17:40:00-뚝뚝');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 7, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-24', '15:24:00', 'WITHDRAWAL', 'OVERSEAS', 13708, '애프터유 디저트', '해외카페', 356.05, 38.5, '체크카드', 0, 'DONE-CD-20260724-152400-24-15:24:00-애프터');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-25', '21:49:00', 'WITHDRAWAL', 'OVERSEAS', 24252, '왓포 사원', '해외관광', 629.92, 38.5, '체크카드', 0, 'DONE-CD-20260725-214900-25-21:49:00-왓포 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 1, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-25', '20:20:00', 'WITHDRAWAL', 'OVERSEAS', 5103, '솜땀 거리노점', '해외음식점', 132.55, 38.5, '체크카드', 0, 'DONE-CD-20260725-202000-25-20:20:00-솜땀 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 4, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-25', '11:58:00', 'WITHDRAWAL', 'OVERSEAS', 7337, '짜뚜짝 시장', '해외쇼핑', 190.57, 38.5, '체크카드', 0, 'DONE-CD-20260725-115800-25-11:58:00-짜뚜짝');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 2, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-26', '14:56:00', 'WITHDRAWAL', 'OVERSEAS', 35717, '그랩택시', '해외교통', 927.71, 38.5, '체크카드', 0, 'DONE-CD-20260726-145600-26-14:56:00-그랩택');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 4, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-26', '10:15:00', 'WITHDRAWAL', 'OVERSEAS', 20359, '시암 파라곤', '해외쇼핑', 528.81, 38.5, '체크카드', 0, 'DONE-CD-20260726-101500-26-10:15:00-시암 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-26', '19:37:00', 'WITHDRAWAL', 'OVERSEAS', 20219, '마사지샵', '해외관광', 525.17, 38.5, '체크카드', 0, 'DONE-CD-20260726-193700-26-19:37:00-마사지');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 4, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-26', '13:14:00', 'WITHDRAWAL', 'OVERSEAS', 29175, '터미널21', '해외쇼핑', 757.79, 38.5, '체크카드', 0, 'DONE-CD-20260726-131400-26-13:14:00-터미널');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-27', '20:03:00', 'WITHDRAWAL', 'OVERSEAS', 8957, '왕궁 투어', '해외관광', 232.65, 38.5, '체크카드', 0, 'DONE-CD-20260727-200300-27-20:03:00-왕궁 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 1, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-27', '10:50:00', 'WITHDRAWAL', 'OVERSEAS', 44120, '솜땀 거리노점', '해외음식점', 1145.97, 38.5, '체크카드', 0, 'DONE-CD-20260727-105000-27-10:50:00-솜땀 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 4, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-28', '14:24:00', 'WITHDRAWAL', 'OVERSEAS', 7163, '터미널21', '해외쇼핑', 186.05, 38.5, '체크카드', 0, 'DONE-CD-20260728-142400-28-14:24:00-터미널');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-28', '12:35:00', 'WITHDRAWAL', 'OVERSEAS', 37676, '왕궁 투어', '해외관광', 978.6, 38.5, '체크카드', 0, 'DONE-CD-20260728-123500-28-12:35:00-왕궁 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 1, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-28', '20:17:00', 'WITHDRAWAL', 'OVERSEAS', 38190, 'S&P Restaurant', '해외음식점', 991.95, 38.5, '체크카드', 0, 'DONE-CD-20260728-201700-28-20:17:00-S&P');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 2, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-28', '14:10:00', 'WITHDRAWAL', 'OVERSEAS', 22234, '그랩택시', '해외교통', 577.51, 38.5, '체크카드', 0, 'DONE-CD-20260728-141000-28-14:10:00-그랩택');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 1, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-29', '20:11:00', 'WITHDRAWAL', 'OVERSEAS', 35806, 'MK Restaurant', '해외음식점', 930.03, 38.5, '체크카드', 0, 'DONE-CD-20260729-201100-29-20:11:00-MK ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-29', '12:53:00', 'WITHDRAWAL', 'OVERSEAS', 43979, '왓포 사원', '해외관광', 1142.31, 38.5, '체크카드', 0, 'DONE-CD-20260729-125300-29-12:53:00-왓포 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-29', '10:23:00', 'WITHDRAWAL', 'OVERSEAS', 16035, '마사지샵', '해외관광', 416.49, 38.5, '체크카드', 0, 'DONE-CD-20260729-102300-29-10:23:00-마사지');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-30', '17:20:00', 'WITHDRAWAL', 'OVERSEAS', 3037, '마사지샵', '해외관광', 78.88, 38.5, '체크카드', 0, 'DONE-CD-20260730-172000-30-17:20:00-마사지');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 4, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-30', '13:56:00', 'WITHDRAWAL', 'OVERSEAS', 10331, '시암 파라곤', '해외쇼핑', 268.34, 38.5, '체크카드', 0, 'DONE-CD-20260730-135600-30-13:56:00-시암 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 7, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-31', '17:05:00', 'WITHDRAWAL', 'OVERSEAS', 18785, '애프터유 디저트', '해외카페', 487.92, 38.5, '체크카드', 0, 'DONE-CD-20260731-170500-31-17:05:00-애프터');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 1, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-31', '20:34:00', 'WITHDRAWAL', 'OVERSEAS', 7535, '코코넛 아이스크림', '해외음식점', 195.71, 38.5, '체크카드', 0, 'DONE-CD-20260731-203400-31-20:34:00-코코넛');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 7, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-07-31', '16:10:00', 'WITHDRAWAL', 'OVERSEAS', 34148, '애프터유 디저트', '해외카페', 886.96, 38.5, '체크카드', 0, 'DONE-CD-20260731-161000-31-16:10:00-애프터');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-08-01', '11:59:00', 'WITHDRAWAL', 'OVERSEAS', 30730, '마사지샵', '해외관광', 798.18, 38.5, '체크카드', 0, 'DONE-CD-20260801-115900-1-11:59:00-마사지');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-08-01', '19:19:00', 'WITHDRAWAL', 'OVERSEAS', 16182, '마사지샵', '해외관광', 420.31, 38.5, '체크카드', 0, 'DONE-CD-20260801-191900-1-19:19:00-마사지');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 4, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-08-01', '15:57:00', 'WITHDRAWAL', 'OVERSEAS', 27472, '터미널21', '해외쇼핑', 713.56, 38.5, '체크카드', 0, 'DONE-CD-20260801-155700-1-15:57:00-터미널');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 4, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-08-02', '11:04:00', 'WITHDRAWAL', 'OVERSEAS', 19246, '시암 파라곤', '해외쇼핑', 499.9, 38.5, '체크카드', 0, 'DONE-CD-20260802-110400-2-11:04:00-시암 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 2, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-08-02', '16:14:00', 'WITHDRAWAL', 'OVERSEAS', 41555, '그랩택시', '해외교통', 1079.35, 38.5, '체크카드', 0, 'DONE-CD-20260802-161400-2-16:14:00-그랩택');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 5, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-08-02', '09:45:00', 'WITHDRAWAL', 'OVERSEAS', 3471, '왓포 사원', '해외관광', 90.16, 38.5, '체크카드', 0, 'DONE-CD-20260802-094500-2-09:45:00-왓포 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 1, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-08-02', '08:55:00', 'WITHDRAWAL', 'OVERSEAS', 7417, '솜땀 거리노점', '해외음식점', 192.65, 38.5, '체크카드', 0, 'DONE-CD-20260802-085500-2-08:55:00-솜땀 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 1, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-08-03', '18:31:00', 'WITHDRAWAL', 'OVERSEAS', 21250, '솜땀 거리노점', '해외음식점', 551.95, 38.5, '체크카드', 0, 'DONE-CD-20260803-183100-3-18:31:00-솜땀 ');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 7, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-08-03', '17:30:00', 'WITHDRAWAL', 'OVERSEAS', 40423, '애프터유 디저트', '해외카페', 1049.95, 38.5, '체크카드', 0, 'DONE-CD-20260803-173000-3-17:30:00-애프터');
INSERT INTO transactions (card_id, category_id, category_source, trip_id, trip_country_id, currency_id, transaction_date, transaction_time, transaction_type, transaction_region, amount, merchant_name, merchant_type, original_amount, applied_exchange_rate, payment_method, is_pre_expense, external_key)
VALUES (@done_card1, 7, 'RULE_BASED', @done_trip, @done_tc_th, 23, '2026-08-03', '11:06:00', 'WITHDRAWAL', 'OVERSEAS', 29677, '와와 카페', '해외카페', 770.83, 38.5, '체크카드', 0, 'DONE-CD-20260803-110600-3-11:06:00-와와 ');

INSERT INTO trip_schedules (trip_id, trip_country_id, currency_id, schedule_name, scheduled_at, amount, payment_status, schedule_status, place_name, place_address) VALUES
(@done_trip, @done_tc_th, 23, '왕궁 & 왓프라깨우 투어', '2026-07-21 09:00:00', 500, 'PREPAID', 'DONE', '왕궁', '방콕, 태국'),
(@done_trip, @done_tc_th, 23, '수상시장 투어', '2026-07-23 07:00:00', 800, 'PREPAID', 'DONE', '담넌사두억 수상시장', '방콕, 태국'),
(@done_trip, @done_tc_th, 23, '아유타야 당일치기', '2026-07-26 08:00:00', 1200, 'PREPAID', 'DONE', '아유타야', '아유타야, 태국'),
(@done_trip, @done_tc_th, 23, '카오산로드 야시장', '2026-07-29 19:00:00', 0, 'UNDECIDED', 'DONE', '카오산로드', '방콕, 태국'),
(@done_trip, @done_tc_th, 23, '무에타이 관람', '2026-08-01 20:00:00', 1500, 'ONSITE', 'DONE', '랏차담넌 스타디움', '방콕, 태국');
INSERT INTO receipts (user_id, trip_id, country_id, category_id, currency_id, payment_datetime, file_name, file_url, file_type, status, merchant_original_name, merchant_translated_name, total_amount, split_count, processed_at) VALUES
(@done_uid, @done_trip, 24, 1, 23, '2026-07-21 13:00:00', 'receipt_20260721.jpg', 'https://storage.tripass.demo/receipts/receipt_20260721.jpg', 'JPG', 'COMPLETED', 'S&P Restaurant', 'S&P 레스토랑', 420, 1, '2026-07-21 13:05:00'),
(@done_uid, @done_trip, 24, 4, 23, '2026-07-22 13:00:00', 'receipt_20260722.jpg', 'https://storage.tripass.demo/receipts/receipt_20260722.jpg', 'JPG', 'COMPLETED', 'Siam Paragon', '시암 파라곤', 1800, 1, '2026-07-22 13:05:00'),
(@done_uid, @done_trip, 24, 5, 23, '2026-07-23 13:00:00', 'receipt_20260723.jpg', 'https://storage.tripass.demo/receipts/receipt_20260723.jpg', 'JPG', 'COMPLETED', 'Damnoen Saduak Market', '담넌사두억 시장', 800, 1, '2026-07-23 13:05:00'),
(@done_uid, @done_trip, 24, 1, 23, '2026-07-24 13:00:00', 'receipt_20260724.jpg', 'https://storage.tripass.demo/receipts/receipt_20260724.jpg', 'JPG', 'COMPLETED', 'MK Restaurant', 'MK 레스토랑', 650, 1, '2026-07-24 13:05:00'),
(@done_uid, @done_trip, 24, 7, 23, '2026-07-25 13:00:00', 'receipt_20260725.jpg', 'https://storage.tripass.demo/receipts/receipt_20260725.jpg', 'JPG', 'COMPLETED', 'After You Dessert', '애프터유 디저트', 380, 1, '2026-07-25 13:05:00'),
(@done_uid, @done_trip, 24, 5, 23, '2026-07-26 13:00:00', 'receipt_20260726.jpg', 'https://storage.tripass.demo/receipts/receipt_20260726.jpg', 'JPG', 'COMPLETED', 'Ayutthaya Temple Tour', '아유타야 사원 투어', 1200, 1, '2026-07-26 13:05:00'),
(@done_uid, @done_trip, 24, 4, 23, '2026-07-27 13:00:00', 'receipt_20260727.jpg', 'https://storage.tripass.demo/receipts/receipt_20260727.jpg', 'JPG', 'COMPLETED', 'Chatuchak Market', '짜뚜짝 시장', 950, 1, '2026-07-27 13:05:00'),
(@done_uid, @done_trip, 24, 1, 23, '2026-07-28 13:00:00', 'receipt_20260728.jpg', 'https://storage.tripass.demo/receipts/receipt_20260728.jpg', 'JPG', 'COMPLETED', 'Somtam Street Stall', '솜땀 거리노점', 180, 1, '2026-07-28 13:05:00'),
(@done_uid, @done_trip, 24, 2, 23, '2026-07-30 13:00:00', 'receipt_20260730.jpg', 'https://storage.tripass.demo/receipts/receipt_20260730.jpg', 'JPG', 'COMPLETED', 'Grab Taxi', '그랩택시', 320, 1, '2026-07-30 13:05:00'),
(@done_uid, @done_trip, 24, 5, 23, '2026-08-01 13:00:00', 'receipt_20260801.jpg', 'https://storage.tripass.demo/receipts/receipt_20260801.jpg', 'JPG', 'COMPLETED', 'Rajadamnern Stadium', '랏차담넌 스타디움', 1500, 1, '2026-08-01 13:05:00'),
(@done_uid, @done_trip, 24, 1, 23, '2026-08-02 13:00:00', 'receipt_20260802.jpg', 'https://storage.tripass.demo/receipts/receipt_20260802.jpg', 'JPG', 'COMPLETED', 'Coconut Ice Cream', '코코넛 아이스크림', 150, 1, '2026-08-02 13:05:00'),
(@done_uid, @done_trip, 24, 4, 23, '2026-08-03 13:00:00', 'receipt_20260803.jpg', 'https://storage.tripass.demo/receipts/receipt_20260803.jpg', 'JPG', 'COMPLETED', 'Terminal21', '터미널21', 2200, 1, '2026-08-03 13:05:00');
COMMIT;
