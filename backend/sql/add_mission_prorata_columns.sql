ALTER TABLE monthly_saving_missions
    ADD COLUMN selected_at DATE NULL DEFAULT NULL AFTER start_week,
    ADD COLUMN mission_start_date DATE NULL DEFAULT NULL AFTER selected_at;

ALTER TABLE weekly_saving_missions
    ADD COLUMN eligible_day_count INT NULL DEFAULT 7 AFTER weekly_expected_saving;
