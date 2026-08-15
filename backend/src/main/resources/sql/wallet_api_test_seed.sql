USE tripass;
SET SQL_SAFE_UPDATES = 0;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET FOREIGN_KEY_CHECKS = 0;

SET @wallet_test_user_id := (
    SELECT id
    FROM users
    WHERE login_provider COLLATE utf8mb4_unicode_ci = 'LOCAL'
      AND login_id COLLATE utf8mb4_unicode_ci = 'walletapi'
    LIMIT 1
);

DELETE wex
FROM wallet_exchange_transaction wex
         INNER JOIN wallet w ON w.id = wex.wallet_id
WHERE w.user_id = @wallet_test_user_id;

DELETE wct
FROM wallet_card_topup wct
         INNER JOIN wallet w ON w.id = wct.wallet_id
WHERE w.user_id = @wallet_test_user_id;

DELETE tcl
FROM travel_card_ledger tcl
         INNER JOIN wallet_travel_card wtc ON wtc.id = tcl.wallet_travel_card_id
         INNER JOIN wallet w ON w.id = wtc.wallet_id
WHERE w.user_id = @wallet_test_user_id;

DELETE tcb
FROM travel_card_balance tcb
         INNER JOIN wallet_travel_card wtc ON wtc.id = tcb.wallet_travel_card_id
         INNER JOIN wallet w ON w.id = wtc.wallet_id
WHERE w.user_id = @wallet_test_user_id;

DELETE wtc
FROM wallet_travel_card wtc
         INNER JOIN wallet w ON w.id = wtc.wallet_id
WHERE w.user_id = @wallet_test_user_id;

DELETE wasr
FROM wallet_auto_saving_rule wasr
         INNER JOIN wallet w ON w.id = wasr.wallet_id
WHERE w.user_id = @wallet_test_user_id;

DELETE wl
FROM wallet_ledger wl
         INNER JOIN wallet w ON w.id = wl.wallet_id
WHERE w.user_id = @wallet_test_user_id;

DELETE wa
FROM wallet_account wa
         INNER JOIN wallet w ON w.id = wa.wallet_id
WHERE w.user_id = @wallet_test_user_id;

DELETE ts
FROM trip_schedules ts
         INNER JOIN trips t ON t.id = ts.trip_id
WHERE t.user_id = @wallet_test_user_id;

DELETE tc
FROM trip_countries tc
         INNER JOIN trips t ON t.id = tc.trip_id
WHERE t.user_id = @wallet_test_user_id;

DELETE FROM wallet WHERE user_id = @wallet_test_user_id;
DELETE FROM user_travel_cards WHERE user_id = @wallet_test_user_id;
DELETE FROM accounts WHERE user_id = @wallet_test_user_id;
DELETE FROM trips WHERE user_id = @wallet_test_user_id;
DELETE FROM users WHERE id = @wallet_test_user_id;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO currencies (currency_code, currency_name, symbol, unit)
VALUES ('KRW', '대한민국 원', '₩', 1),
       ('EUR', '유로', '€', 1),
       ('JPY', '일본 엔', '¥', 100),
       ('CHF', '스위스 프랑', 'CHF', 1),
       ('USD', '미국 달러', '$', 1)
    ON DUPLICATE KEY UPDATE
                         currency_name = VALUES(currency_name),
                         symbol = VALUES(symbol),
                         unit = VALUES(unit);

INSERT INTO exchange_rates (
    base_currency_id,
    target_currency_id,
    currency_unit,
    deal_base_rate,
    prev_rate,
    rate_date,
    fetched_at
)
SELECT krw.id, target_currency.id, target_currency.unit, target_currency.rate, target_currency.prev_rate, CURRENT_DATE, NOW()
FROM currencies krw
         INNER JOIN (
    SELECT id, unit, 1486.20000000 AS rate, 1489.30000000 AS prev_rate
    FROM currencies
    WHERE currency_code = 'EUR'
    UNION ALL
    SELECT id, unit, 942.00000000 AS rate, 944.20000000 AS prev_rate
    FROM currencies
    WHERE currency_code = 'JPY'
    UNION ALL
    SELECT id, unit, 1606.00000000 AS rate, 1602.50000000 AS prev_rate
    FROM currencies
    WHERE currency_code = 'CHF'
    UNION ALL
    SELECT id, unit, 1375.00000000 AS rate, 1378.00000000 AS prev_rate
    FROM currencies
    WHERE currency_code = 'USD'
) target_currency
WHERE krw.currency_code = 'KRW'
    ON DUPLICATE KEY UPDATE
                         currency_unit = VALUES(currency_unit),
                         deal_base_rate = VALUES(deal_base_rate),
                         prev_rate = VALUES(prev_rate),
                         fetched_at = VALUES(fetched_at);

-- 여행 일정 테스트에 쓸 국가 기준 데이터(country 테이블은 별도 시드가 없어 여기서 함께 채운다).
INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '프랑스', c.id, 'Europe/Paris'
FROM currencies c
WHERE c.currency_code = 'EUR'
  AND NOT EXISTS (SELECT 1 FROM countries WHERE country_name COLLATE utf8mb4_unicode_ci = '프랑스');

INSERT INTO countries (country_name, currency_id, time_zone)
SELECT '스위스', c.id, 'Europe/Zurich'
FROM currencies c
WHERE c.currency_code = 'CHF'
  AND NOT EXISTS (SELECT 1 FROM countries WHERE country_name COLLATE utf8mb4_unicode_ci = '스위스');

SET @wallet_country_france_id := (
    SELECT id FROM countries WHERE country_name COLLATE utf8mb4_unicode_ci = '프랑스' LIMIT 1
);

SET @wallet_country_switzerland_id := (
    SELECT id FROM countries WHERE country_name COLLATE utf8mb4_unicode_ci = '스위스' LIMIT 1
);

INSERT INTO travel_cards (
    card_name,
    card_company,
    bank_name,
    required_account,
    instant_use,
    applied_rate_info,
    settlement_type,
    exchange_fee,
    re_exchange_fee,
    payment_fee,
    withdrawal_fee,
    auto_charge_supported,
    is_transit_card,
    is_active
)
SELECT 'KB 트래블러스 체크카드',
       'KB국민카드',
       'KB국민은행',
       NULL,
       TRUE,
       '실시간 고시환율',
       'DIRECT',
       '0%',
       '1%',
       '0%',
       'ATM 무료',
       TRUE,
       TRUE,
       TRUE
    WHERE NOT EXISTS (
    SELECT 1
    FROM travel_cards
    WHERE card_name COLLATE utf8mb4_unicode_ci IN ('KB 트래블러스 체크카드', '트래블러스 체크카드')
      AND card_company COLLATE utf8mb4_unicode_ci = 'KB국민카드'
);

INSERT INTO travel_cards (
    card_name,
    card_company,
    bank_name,
    required_account,
    instant_use,
    applied_rate_info,
    settlement_type,
    exchange_fee,
    re_exchange_fee,
    payment_fee,
    withdrawal_fee,
    auto_charge_supported,
    is_transit_card,
    is_active
)
SELECT '하나 트래블로그 카드',
       '하나카드',
       '하나은행',
       NULL,
       TRUE,
       '실시간 고시환율',
       'DIRECT',
       '0%',
       '1%',
       '0%',
       'ATM 무료',
       TRUE,
       TRUE,
       TRUE
    WHERE NOT EXISTS (
    SELECT 1
    FROM travel_cards
    WHERE card_name COLLATE utf8mb4_unicode_ci IN ('하나 트래블로그 카드', '트래블로그 체크카드')
      AND card_company COLLATE utf8mb4_unicode_ci = '하나카드'
);

SET @wallet_kb_card_id := (
    SELECT id
    FROM travel_cards
    WHERE card_name COLLATE utf8mb4_unicode_ci IN ('KB 트래블러스 체크카드', '트래블러스 체크카드')
      AND card_company COLLATE utf8mb4_unicode_ci = 'KB국민카드'
    ORDER BY id
    LIMIT 1
);

SET @wallet_hana_card_id := (
    SELECT id
    FROM travel_cards
    WHERE card_name COLLATE utf8mb4_unicode_ci IN ('하나 트래블로그 카드', '트래블로그 체크카드')
      AND card_company COLLATE utf8mb4_unicode_ci = '하나카드'
    ORDER BY id
    LIMIT 1
);

INSERT INTO travel_card_currencies (card_id, currency_code)
SELECT @wallet_kb_card_id, c.currency_code
FROM currencies c
WHERE c.currency_code IN ('EUR', 'JPY', 'CHF', 'USD')
    ON DUPLICATE KEY UPDATE is_deleted = 0, deleted_at = NULL;

INSERT INTO travel_card_currencies (card_id, currency_code)
SELECT @wallet_hana_card_id, c.currency_code
FROM currencies c
WHERE c.currency_code IN ('EUR', 'JPY', 'CHF', 'USD')
    ON DUPLICATE KEY UPDATE is_deleted = 0, deleted_at = NULL;

INSERT INTO users (
    login_id,
    password,
    name,
    phone_number,
    login_provider,
    current_view_mode
)
VALUES (
           'walletapi',
           '$2a$10$mHASKxszz8B6ioIRz49Y..Y5KrS368Oc.j2AS48HY9xKHYYklFyoW',
           '월렛테스트',
           '010-9000-0001',
           'LOCAL',
           'SAVING'
       );

SET @wallet_test_user_id := LAST_INSERT_ID();

INSERT INTO trips (
    user_id,
    trip_name,
    status,
    start_date,
    end_date,
    total_target_amount,
    created_at
)
VALUES (
           @wallet_test_user_id,
           '유럽 2개국 배낭여행',
           'PLANNING',
           '2026-08-28',
           '2026-09-27',
           5000000.00,
           '2026-05-15 09:00:00'
       );

SET @wallet_trip_id := LAST_INSERT_ID();

INSERT INTO trip_countries (
    trip_id,
    country_id,
    arrival_date,
    departure_date,
    target_budget,
    display_order
)
VALUES
    (@wallet_trip_id, @wallet_country_france_id, '2026-08-28', '2026-09-12', 2500000.00, 1),
    (@wallet_trip_id, @wallet_country_switzerland_id, '2026-09-12', '2026-09-27', 2500000.00, 2);

SET @wallet_trip_country_france_id := (
    SELECT id FROM trip_countries
    WHERE trip_id = @wallet_trip_id AND country_id = @wallet_country_france_id
    LIMIT 1
);

SET @wallet_trip_country_switzerland_id := (
    SELECT id FROM trip_countries
    WHERE trip_id = @wallet_trip_id AND country_id = @wallet_country_switzerland_id
    LIMIT 1
);

SET @wallet_currency_eur_id := (SELECT id FROM currencies WHERE currency_code = 'EUR' LIMIT 1);
SET @wallet_currency_chf_id := (SELECT id FROM currencies WHERE currency_code = 'CHF' LIMIT 1);

-- 여행 일정(trip_schedules) 시드: 프랑스 구간 3건 + 스위스 구간 3건, 결제 상태를 다양하게 섞어 둔다.
INSERT INTO trip_schedules (
    trip_id,
    trip_country_id,
    currency_id,
    schedule_name,
    scheduled_at,
    amount,
    payment_status,
    schedule_status,
    place_name,
    place_address,
    memo
)
VALUES
    (@wallet_trip_id, @wallet_trip_country_france_id, @wallet_currency_eur_id,
     '샤를드골 공항 도착', '2026-08-28 12:30:00', NULL, 'UNDECIDED', 'UPCOMING',
     '샤를 드골 국제공항', '95700 Roissy-en-France, France', '입국 심사 후 시내로 이동'),
    (@wallet_trip_id, @wallet_trip_country_france_id, @wallet_currency_eur_id,
     '에펠탑 전망대', '2026-08-29 08:00:00', 29.00, 'PREPAID', 'UPCOMING',
     '에펠탑', 'Champ de Mars, 5 Avenue Anatole France, 75007 Paris', '예약 시간 20분 전까지 도착'),
    (@wallet_trip_id, @wallet_trip_country_france_id, @wallet_currency_eur_id,
     '루브르 박물관 자유 관람', '2026-08-30 08:30:00', 22.00, 'PREPAID', 'UPCOMING',
     '루브르 박물관', 'Rue de Rivoli, 75001 Paris', NULL),
    (@wallet_trip_id, @wallet_trip_country_switzerland_id, @wallet_currency_chf_id,
     '취리히행 열차 이동', '2026-09-12 07:15:00', NULL, 'ONSITE', 'UPCOMING',
     '파리 리옹역', '20 Boulevard Diderot, 75012 Paris', '좌석 지정권 현장 발권'),
    (@wallet_trip_id, @wallet_trip_country_switzerland_id, @wallet_currency_chf_id,
     '융프라우요흐 전망대', '2026-09-15 06:00:00', 220.00, 'PREPAID', 'UPCOMING',
     '융프라우요흐', '3801 Jungfraujoch, Switzerland', '고산지대 방한복 필수'),
    (@wallet_trip_id, @wallet_trip_country_switzerland_id, @wallet_currency_chf_id,
     '취리히 시내 자유 관광', '2026-09-20 09:00:00', NULL, 'UNDECIDED', 'UPCOMING',
     '취리히 반호프 거리', 'Bahnhofstrasse, 8001 Zürich', '현지에서 일정 확정 예정');

INSERT INTO accounts (
    user_id,
    account_name,
    account_number,
    account_type,
    balance,
    withdrawable_amount,
    recognized_amount,
    is_travel_fund_included,
    connection_type,
    external_account_key
)
VALUES (@wallet_test_user_id, 'KB국민은행 여행통장', '12345678901', 'CHECKING', 5200000.00, 5200000.00, 5200000.00, TRUE, 'DEMO', 'walletapi-kb-account'),
       (@wallet_test_user_id, '신한은행 통장', '110456789', 'CHECKING', 300000.00, 300000.00, 300000.00, TRUE, 'DEMO', 'walletapi-sh-account'),
       (@wallet_test_user_id, '카카오뱅크 입출금통장', '10003344', 'CHECKING', 4250000.00, 4250000.00, 4250000.00, TRUE, 'DEMO', 'walletapi-kakao-account');

INSERT INTO wallet (
    user_id,
    balance_amount,
    status,
    version
)
VALUES (
           @wallet_test_user_id,
           0.00,
           'ACTIVE',
           0
       );

SET @wallet_id := LAST_INSERT_ID();

SET @wallet_primary_account_id := (
    SELECT id
    FROM accounts
    WHERE user_id = @wallet_test_user_id
      AND external_account_key = 'walletapi-kb-account'
    LIMIT 1
);

SET @wallet_second_account_id := (
    SELECT id
    FROM accounts
    WHERE user_id = @wallet_test_user_id
      AND external_account_key = 'walletapi-sh-account'
    LIMIT 1
);

SET @wallet_third_account_id := (
    SELECT id
    FROM accounts
    WHERE user_id = @wallet_test_user_id
      AND external_account_key = 'walletapi-kakao-account'
    LIMIT 1
);

INSERT INTO wallet_account (wallet_id, account_id, is_primary, status)
VALUES (@wallet_id, @wallet_primary_account_id, TRUE, 'LINKED'),
       (@wallet_id, @wallet_second_account_id, FALSE, 'LINKED'),
       (@wallet_id, @wallet_third_account_id, FALSE, 'LINKED');

INSERT INTO wallet_auto_saving_rule (
    wallet_id,
    source_account_id,
    amount,
    day_of_month,
    enabled,
    next_transfer_date
)
VALUES (
           @wallet_id,
           @wallet_primary_account_id,
           700000.00,
           25,
           TRUE,
           DATE_FORMAT(DATE_ADD(CURRENT_DATE, INTERVAL 1 MONTH), '%Y-%m-25')
       );

INSERT INTO user_travel_cards (
    user_id,
    travel_card_id,
    card_name,
    issuer_name,
    masked_card_number,
    brand_name,
    card_color,
    status,
    external_card_key
)
VALUES (@wallet_test_user_id, @wallet_kb_card_id, 'KB 트래블러스 체크카드', 'KB국민카드', '1234 · 5678 · **** · 3456', 'TRAVELUS', '#153783', 'ACTIVE', 'walletapi-card-kb'),
       (@wallet_test_user_id, @wallet_hana_card_id, '하나 트래블로그 카드', '하나카드', '4567 · 1234 · **** · 9081', 'TRAVLOG', '#12a997', 'ACTIVE', 'walletapi-card-hana');

SET @wallet_user_card_id := (
    SELECT id
    FROM user_travel_cards
    WHERE user_id = @wallet_test_user_id
      AND external_card_key = 'walletapi-card-kb'
    LIMIT 1
);

INSERT INTO wallet_travel_card (
    wallet_id,
    user_travel_card_id,
    travel_card_id,
    card_name,
    issuer_name,
    masked_card_number,
    status
)
VALUES (
           @wallet_id,
           @wallet_user_card_id,
           @wallet_kb_card_id,
           'KB 트래블러스 체크카드',
           'KB국민카드',
           '1234 · 5678 · **** · 3456',
           'LINKED'
       );

SET @wallet_travel_card_id := LAST_INSERT_ID();

-- 7월 월렛 내역(채우기/자동 채우기/빼기) 시드
INSERT INTO wallet_ledger (
    wallet_id,
    direction,
    transaction_type,
    transfer_method,
    amount,
    balance_before,
    balance_after,
    source_type,
    source_id,
    target_type,
    target_id,
    idempotency_key,
    memo,
    created_at
)
VALUES
    (@wallet_id, 'IN', 'CHARGE', 'MANUAL', 500000.00, 0.00, 500000.00,
     'ACCOUNT', @wallet_primary_account_id, 'WALLET', @wallet_id,
     'walletapi-seed-ledger-1', '월렛 수동 충전', '2026-07-05 09:10:00'),
    (@wallet_id, 'IN', 'CHARGE', 'AUTO_SAVING', 200000.00, 500000.00, 700000.00,
     'ACCOUNT', @wallet_primary_account_id, 'WALLET', @wallet_id,
     'walletapi-seed-ledger-2', '월 목표 자동 송금', '2026-07-12 08:00:00'),
    (@wallet_id, 'OUT', 'WITHDRAW', NULL, 150000.00, 700000.00, 550000.00,
     'WALLET', @wallet_id, 'ACCOUNT', @wallet_second_account_id,
     'walletapi-seed-ledger-3', '월렛 출금', '2026-07-20 19:45:00'),
    (@wallet_id, 'IN', 'CHARGE', 'MANUAL', 300000.00, 550000.00, 850000.00,
     'ACCOUNT', @wallet_third_account_id, 'WALLET', @wallet_id,
     'walletapi-seed-ledger-4', '월렛 수동 충전', '2026-07-28 21:30:00');

UPDATE wallet SET balance_amount = 850000.00 WHERE id = @wallet_id;

SET SQL_SAFE_UPDATES = 1;

SELECT 'walletapi / Test1234!' AS wallet_test_login;