-- =====================================================
-- #273 통합 페르소나 QA: 계좌·카드·월렛 연결 구조 보완
-- =====================================================

USE tripass;

ALTER TABLE cards
    ADD COLUMN payment_account_number VARCHAR(50) NULL COMMENT '카드 결제계좌 번호(CODEF 응답 기준)' AFTER organization_code,
    ADD COLUMN linked_account_id BIGINT NULL COMMENT '서비스에 연동된 카드 결제계좌 ID' AFTER payment_account_number,
    ADD INDEX idx_cards_linked_account (linked_account_id),
    ADD CONSTRAINT fk_cards_linked_account
        FOREIGN KEY (linked_account_id) REFERENCES accounts (id);

UPDATE cards c
JOIN accounts a
  ON a.user_id = c.user_id
 AND a.account_number = c.payment_account_number
 AND a.is_deleted = 0
SET c.linked_account_id = a.id
WHERE c.is_deleted = 0
  AND c.payment_account_number IS NOT NULL;

CREATE TABLE IF NOT EXISTS wallet_withdraw_recipient
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '최근 출금 계좌 ID',
    user_id             BIGINT       NOT NULL COMMENT '회원 ID',
    bank_code           VARCHAR(20)  NOT NULL COMMENT '금융기관 코드',
    bank_name           VARCHAR(100) NOT NULL COMMENT '금융기관명',
    account_number      VARCHAR(100) NOT NULL COMMENT '수취 계좌번호',
    account_holder_name VARCHAR(100) NULL COMMENT '예금주명',
    last_used_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최근 출금 시각',
    use_count           INT          NOT NULL DEFAULT 1 COMMENT '출금 사용 횟수',
    is_deleted          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at          DATETIME     NULL COMMENT '삭제일시',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_withdraw_recipient_user_account (user_id, bank_code, account_number),
    KEY idx_wallet_withdraw_recipient_recent (user_id, last_used_at),
    CONSTRAINT fk_wallet_withdraw_recipient_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '월렛 최근 출금 계좌';

-- 과거 Seed/회원가입 데이터에 월렛이 누락된 경우 보완한다.
INSERT INTO wallet (user_id, balance_amount, status, version, created_at, updated_at)
SELECT u.id, 0, 'ACTIVE', 0, NOW(), NOW()
FROM users u
LEFT JOIN wallet w ON w.user_id = u.id
WHERE u.is_deleted = 0
  AND w.id IS NULL;

-- 이미 연동된 트래블카드도 재연동 없이 월렛 후보에 보이도록 보완한다.
INSERT INTO user_travel_cards
    (user_id, travel_card_id, card_name, issuer_name, masked_card_number,
     brand_name, card_color, status, external_card_key, is_deleted, created_at, updated_at)
SELECT c.user_id, tc.id, tc.card_name, tc.card_company, c.masked_card_number,
       'CODEF', 'BLUE', 'ACTIVE',
       CONCAT('CODEF:', c.organization_code, ':', c.masked_card_number), FALSE, NOW(), NOW()
FROM cards c
JOIN travel_cards tc ON tc.card_name = c.card_name AND tc.is_active = TRUE
WHERE c.is_deleted = 0
ON DUPLICATE KEY UPDATE
    travel_card_id = VALUES(travel_card_id),
    status = 'ACTIVE',
    is_deleted = FALSE,
    deleted_at = NULL,
    updated_at = NOW();
