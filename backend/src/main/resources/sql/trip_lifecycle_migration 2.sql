-- Existing environments only. Fresh installations use schema.sql.
ALTER TABLE trips
    ADD COLUMN start_report_viewed_at DATETIME NULL
        COMMENT '여행 시작 저축 리포트 팝업 확인 시각'
        AFTER total_target_amount;

