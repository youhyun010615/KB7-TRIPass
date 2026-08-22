-- =====================================================
-- 온보딩(여행/계좌 등록 skip 가능) + 여행-월렛-미션 연동 구조 보완
-- =====================================================

USE tripass;

-- 1) 온보딩 최초 1회 노출 여부 (NULL = 아직 안 보여줌, 값 있음 = 이미 보여줌/처리됨)
ALTER TABLE users
    ADD COLUMN onboarding_shown_at DATETIME NULL COMMENT '여행/계좌 등록 온보딩 최초 노출 일시(NULL이면 아직 미노출)' AFTER current_view_mode;

-- 기존 가입자는 온보딩 대상에서 제외 (소급 노출 방지)
UPDATE users SET onboarding_shown_at = created_at WHERE onboarding_shown_at IS NULL;

-- 2) 여행: 여행+계좌 둘 다 충족된 시점(=여행 저축 집계 시작 시점), 월렛 잔액 반영 프롬프트 처리 여부
ALTER TABLE trips
    ADD COLUMN savings_tracking_started_at DATETIME NULL COMMENT '여행+계좌 둘 다 등록되어 여행 저축 집계가 시작된 시각' AFTER total_target_amount,
    ADD COLUMN wallet_reflect_resolved TINYINT(1) NOT NULL DEFAULT 0 COMMENT '집계 시작 시점 월렛 잔액 반영 여부 프롬프트 처리 완료 여부' AFTER savings_tracking_started_at;

-- 3) 월렛 원장: 여행 저축 집계 시작 이후 발생한 거래를 여행에 귀속시키기 위한 참조 컬럼
ALTER TABLE wallet_ledger
    ADD COLUMN trip_id BIGINT NULL COMMENT '이 거래가 귀속되는 여행 ID(집계 시작 전 거래는 NULL)' AFTER wallet_id,
    ADD INDEX idx_wallet_ledger_trip (trip_id),
    ADD CONSTRAINT fk_wallet_ledger_trip FOREIGN KEY (trip_id) REFERENCES trips (id);

-- 4) 월간 저축 미션: 생성 시점의 활성 여행에 귀속
ALTER TABLE monthly_saving_missions
    ADD COLUMN trip_id BIGINT NULL COMMENT '이 미션이 귀속되는 여행 ID' AFTER user_id,
    ADD INDEX idx_monthly_saving_missions_trip (trip_id),
    ADD CONSTRAINT fk_monthly_saving_missions_trip FOREIGN KEY (trip_id) REFERENCES trips (id);
