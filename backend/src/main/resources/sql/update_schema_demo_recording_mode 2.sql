-- =====================================================
-- 데모 녹화 모드 (가상 날짜 전환 시 자동 리셋을 건너뛰는 스위치)
-- =====================================================

USE tripass;

ALTER TABLE users
    ADD COLUMN demo_recording_mode TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '켜져 있으면 가상 날짜 전환 시 자동 리셋을 건너뛴다(시연 녹화용)';
