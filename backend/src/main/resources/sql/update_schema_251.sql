USE tripass;

-- #251: 성공한 주간 절약 미션의 TRIP 월렛 보상 적립 상태를 저장한다.
ALTER TABLE weekly_saving_missions
    ADD COLUMN reward_amount INT NOT NULL DEFAULT 0
        COMMENT 'TRIP 월렛에 실제 적립한 미션 보상액(원)' AFTER actual_saving,
    ADD COLUMN wallet_ledger_id BIGINT NULL
        COMMENT '미션 보상 월렛 원장 ID' AFTER reward_amount,
    ADD COLUMN rewarded_at DATETIME NULL
        COMMENT 'TRIP 월렛 보상 적립 시각' AFTER evaluated_at,
    ADD UNIQUE KEY uk_weekly_saving_missions_wallet_ledger (wallet_ledger_id),
    ADD CONSTRAINT fk_weekly_saving_missions_wallet_ledger
        FOREIGN KEY (wallet_ledger_id) REFERENCES wallet_ledger (id);
