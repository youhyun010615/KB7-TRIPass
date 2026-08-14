-- #193 카드 거래내역 Mock API를 위한 기존 DB 변경 SQL
-- schema.sql을 새로 실행하는 DB에는 이 파일을 추가로 실행하지 않습니다.

CREATE TABLE IF NOT EXISTS cards
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '카드 ID',
    user_id            BIGINT       NOT NULL COMMENT '회원 ID',
    card_name          VARCHAR(150) NOT NULL COMMENT '카드명',
    card_company       VARCHAR(100) NOT NULL COMMENT '카드사',
    masked_card_number VARCHAR(30)  NULL COMMENT '마스킹된 카드번호',
    external_card_key  VARCHAR(255) NOT NULL COMMENT '외부 카드 식별 키',
    connection_type    VARCHAR(20)  NOT NULL DEFAULT 'MOCK' COMMENT '연결 유형(MOCK/CODEF)',
    last_synced_at     TIMESTAMP    NULL COMMENT '최근 동기화 시각',
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '연동 활성 여부',
    is_deleted         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제 여부',
    deleted_at         DATETIME     NULL COMMENT '삭제일시',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cards_user_external_key (user_id, external_card_key),
    INDEX idx_cards_user_id (user_id),
    CONSTRAINT fk_cards_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '회원 보유 카드';

ALTER TABLE transactions
    MODIFY COLUMN account_id BIGINT NULL COMMENT '계좌 ID(계좌 거래일 때 사용)',
    ADD COLUMN card_id BIGINT NULL COMMENT '카드 ID(카드 거래일 때 사용)' AFTER account_id,
    ADD UNIQUE KEY uk_transactions_card_key (card_id, external_key),
    ADD INDEX idx_transactions_card_id (card_id),
    ADD CONSTRAINT fk_transactions_card FOREIGN KEY (card_id) REFERENCES cards (id),
    ADD CONSTRAINT chk_transactions_source CHECK (
        (account_id IS NOT NULL AND card_id IS NULL)
        OR (account_id IS NULL AND card_id IS NOT NULL)
    );
