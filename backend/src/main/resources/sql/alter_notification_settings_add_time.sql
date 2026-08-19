-- 알림 수신 시간대 컬럼 추가
ALTER TABLE notification_settings
    ADD COLUMN quiet_start_time TIME DEFAULT '09:00:00' AFTER travel_report_enabled,
    ADD COLUMN quiet_end_time TIME DEFAULT '22:00:00' AFTER quiet_start_time;
