USE tripass;

-- #228: 월간·주간 절감 미션 생성
CREATE TABLE monthly_saving_missions
(
    id                            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '월간 미션 ID',
    user_id                       BIGINT      NOT NULL COMMENT '회원 ID',
    monthly_spending_analysis_id  BIGINT      NOT NULL COMMENT '월간 분석 ID',
    mission_category_selection_id BIGINT      NOT NULL COMMENT '카테고리 절감률 선택 ID',
    category_id                   BIGINT      NOT NULL COMMENT '소비 카테고리 ID',
    target_year_month             CHAR(7)     NOT NULL COMMENT '미션 적용 연월(YYYY-MM)',
    reduction_rate                TINYINT     NOT NULL COMMENT '절감률(10/30/50, %)',
    baseline_spending_amount      INT         NOT NULL COMMENT '선택 시점 기준 지출액(원)',
    monthly_reduction_target      INT         NOT NULL COMMENT '월 절감 목표 금액(원)',
    monthly_usage_target          INT         NOT NULL COMMENT '월 사용 목표 금액(원)',
    planned_saving_amount         INT         NOT NULL COMMENT '실제 생성된 주차의 예상 절약액 합계(원)',
    start_week                    TINYINT     NOT NULL COMMENT '미션 시작 주차(1~4)',
    status                        VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '상태(IN_PROGRESS/COMPLETED/CANCELLED)',
    created_at                    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at                    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_saving_missions_user_month_category (user_id, target_year_month, category_id),
    UNIQUE KEY uk_monthly_saving_missions_selection (mission_category_selection_id),
    CONSTRAINT fk_monthly_saving_missions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_monthly_saving_missions_analysis FOREIGN KEY (monthly_spending_analysis_id) REFERENCES monthly_spending_analyses (id),
    CONSTRAINT fk_monthly_saving_missions_selection FOREIGN KEY (mission_category_selection_id) REFERENCES mission_category_selections (id),
    CONSTRAINT fk_monthly_saving_missions_category FOREIGN KEY (category_id) REFERENCES spending_categories (id),
    CONSTRAINT chk_monthly_saving_missions_reduction_rate CHECK (reduction_rate IN (10, 30, 50)),
    CONSTRAINT chk_monthly_saving_missions_start_week CHECK (start_week BETWEEN 1 AND 4)
) COMMENT '월간 카테고리 절감 미션';

CREATE TABLE weekly_saving_missions
(
    id                        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '주간 미션 ID',
    monthly_saving_mission_id BIGINT      NOT NULL COMMENT '월간 미션 ID',
    week_number               TINYINT     NOT NULL COMMENT '주차(1~4)',
    period_start_date         DATE        NOT NULL COMMENT '주차 시작일',
    period_end_date           DATE        NOT NULL COMMENT '주차 종료일',
    weekly_usage_limit        INT         NOT NULL COMMENT '주간 사용 한도(원)',
    weekly_expected_saving    INT         NOT NULL COMMENT '주간 예상 절약 금액(원)',
    actual_spending           INT         NULL COMMENT '주간 실제 지출액(원)',
    actual_saving             INT         NULL COMMENT '주간 실제 절약액(원)',
    status                    VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '상태(PENDING/SUCCESS/FAILED)',
    evaluated_at              DATETIME    NULL COMMENT '판정 시각',
    created_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_weekly_saving_missions_monthly_week (monthly_saving_mission_id, week_number),
    CONSTRAINT fk_weekly_saving_missions_monthly FOREIGN KEY (monthly_saving_mission_id) REFERENCES monthly_saving_missions (id),
    CONSTRAINT chk_weekly_saving_missions_week CHECK (week_number BETWEEN 1 AND 4)
) COMMENT '주간 카테고리 절감 미션';

CREATE INDEX idx_monthly_saving_missions_user_month ON monthly_saving_missions (user_id, target_year_month);
CREATE INDEX idx_weekly_saving_missions_period ON weekly_saving_missions (period_start_date, period_end_date);
