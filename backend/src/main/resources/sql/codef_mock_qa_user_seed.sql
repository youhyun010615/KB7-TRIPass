-- =====================================================
-- #271 CODEF 금융 연동 QA 신규 사용자 Seed
--
-- TRIPass 로그인
--   ID: qafinance
--   PW: Test1234!
--
-- Mock 금융기관 로그인(계좌/카드 공통)
--   ID: tripassqa
--   PW: Mock1234!
--
-- 주의
--   이 스크립트는 서비스 사용자만 생성한다.
--   계좌, 카드, 거래내역, 사용자 보유 트래블카드는 미리 적재하지 않는다.
--   codef.mode=mock 상태에서 사용자가 기존 연동 화면을 완료할 때 순서대로 적재된다.
--
-- 선행 기준 데이터
--   1. schema.sql
--   2. tripass_asset_seed.sql              (KB국민은행 0004)
--   3. update_schema_222.sql               (KB카드 0301)
--   4. spending_categories_seed.sql
--   5. travel_card_seed.sql                (트래블러스 체크카드)
-- =====================================================

USE tripass;

INSERT INTO users
    (login_id, password, name, phone_number, login_provider, provider_key,
     current_view_mode, is_deleted, deleted_at, created_at, updated_at)
VALUES
    ('qafinance',
     '$2a$10$mHASKxszz8B6ioIRz49Y..Y5KrS368Oc.j2AS48HY9xKHYYklFyoW',
     '금융QA',
     '010-9271-0001',
     'LOCAL',
     NULL,
     'SAVING',
     FALSE,
     NULL,
     NOW(),
     NOW())
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    name = VALUES(name),
    phone_number = VALUES(phone_number),
    current_view_mode = 'SAVING',
    is_deleted = FALSE,
    deleted_at = NULL,
    updated_at = NOW();

-- 직접 Seed로 생성한 QA 사용자도 회원가입 사용자와 동일하게 기본 월렛을 가진다.
INSERT INTO wallet (user_id, balance_amount, status, version, created_at, updated_at)
SELECT u.id, 0, 'ACTIVE', 0, NOW(), NOW()
FROM users u
WHERE u.login_provider = 'LOCAL'
  AND u.login_id = 'qafinance'
ON DUPLICATE KEY UPDATE
    status = 'ACTIVE',
    updated_at = NOW();

-- 신규 상태 확인: 아래 네 값이 모두 0이어야 QA를 처음부터 시작할 수 있다.
SELECT
    u.id,
    u.login_id,
    u.name,
    (SELECT COUNT(*) FROM accounts a WHERE a.user_id = u.id AND a.is_deleted = 0) AS account_count,
    (SELECT COUNT(*) FROM cards c WHERE c.user_id = u.id AND c.is_deleted = 0) AS card_count,
    (SELECT COUNT(*) FROM trips t WHERE t.user_id = u.id AND t.is_deleted = 0) AS trip_count,
    (SELECT COUNT(*) FROM user_travel_cards utc WHERE utc.user_id = u.id AND utc.is_deleted = 0)
        AS user_travel_card_count
FROM users u
WHERE u.login_provider = 'LOCAL'
  AND u.login_id = 'qafinance';
