USE tripass;

-- #222: 계좌별 CODEF 거래 동기화에 사용할 기관코드를 계좌 자체에 보존한다.
ALTER TABLE accounts
    ADD COLUMN organization_code VARCHAR(20) NULL COMMENT 'CODEF 기관코드' AFTER codef_connection_id;

ALTER TABLE cards
    ADD COLUMN last_synced_at TIMESTAMP NULL COMMENT '마지막 거래내역 동기화 시각' AFTER organization_code;

-- 기존 데이터는 연결된 은행 기관이 정확히 하나인 경우에만 안전하게 보정한다.
UPDATE accounts a
JOIN (
    SELECT codef_connection_id, MIN(organization_code) AS organization_code
    FROM codef_connected_institutions
    WHERE business_type = 'BK'
      AND is_active = TRUE
    GROUP BY codef_connection_id
    HAVING COUNT(*) = 1
) i ON i.codef_connection_id = a.codef_connection_id
SET a.organization_code = i.organization_code
WHERE a.organization_code IS NULL;

-- CODEF 개인 카드 주요 8개 기관.
INSERT INTO supported_institutions
    (organization_code, institution_name, business_type, display_order, is_active)
VALUES
    ('0301', 'KB카드', 'CD', 101, TRUE),
    ('0302', '현대카드', 'CD', 102, TRUE),
    ('0303', '삼성카드', 'CD', 103, TRUE),
    ('0304', 'NH카드', 'CD', 104, TRUE),
    ('0305', 'BC카드', 'CD', 105, TRUE),
    ('0306', '신한카드', 'CD', 106, TRUE),
    ('0311', '롯데카드', 'CD', 107, TRUE),
    ('0313', '하나카드', 'CD', 108, TRUE)
ON DUPLICATE KEY UPDATE
    institution_name = VALUES(institution_name),
    business_type = VALUES(business_type),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active),
    updated_at = NOW();
