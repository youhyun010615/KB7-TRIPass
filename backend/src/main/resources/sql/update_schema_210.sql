USE tripass;

-- #210: 월간 AI 소비 분석 리포트
CREATE TABLE monthly_spending_analyses
(
    id                       BIGINT         NOT NULL AUTO_INCREMENT COMMENT '월간 분석 ID',
    user_id                  BIGINT         NOT NULL COMMENT '회원 ID',
    analysis_year_month      CHAR(7)        NOT NULL COMMENT '분석 대상 연월(YYYY-MM, 지난달)',
    target_year_month        CHAR(7)        NOT NULL COMMENT '미션 적용 연월(YYYY-MM, 이번달)',
    total_spending           DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '지난달 총지출(1일~말일)',
    saving_target_amount     DECIMAL(18, 2) NULL COMMENT '저축 목표 금액',
    actual_saving_amount     DECIMAL(18, 2) NULL COMMENT '실제 저축 금액',
    saving_difference_amount DECIMAL(18, 2) NULL COMMENT '초과·부족 금액(실제-목표)',
    saving_result_message    VARCHAR(200)   NULL COMMENT '저축 결과 문구',
    report_status            VARCHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT '리포트 상태(PENDING/VIEWED/CLOSED)',
    report_viewed_at         DATETIME       NULL COMMENT '리포트 확인 시각',
    report_closed_at         DATETIME       NULL COMMENT '리포트 종료 시각',
    created_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_spending_analyses_user_month (user_id, analysis_year_month),
    CONSTRAINT fk_monthly_spending_analyses_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT '월간 AI 소비 분석 리포트';

-- #210: 월간 카테고리별 소비·절감 추천 분석
CREATE TABLE monthly_category_analyses
(
    id                         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '카테고리별 분석 ID',
    monthly_analysis_id        BIGINT         NOT NULL COMMENT '월간 분석 ID',
    category_id                BIGINT         NOT NULL COMMENT '카테고리 ID',
    spending_amount            DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '지난달 지출액(1일~말일, 소비순위 표시용)',
    spending_ratio             DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '전체 지출 대비 비율(%)',
    transaction_count          INT            NOT NULL DEFAULT 0 COMMENT '거래 횟수(1일~말일)',
    weekly_average             DECIMAL(18, 2) NULL COMMENT '주간 평균 지출',
    daily_average              DECIMAL(18, 2) NULL COMMENT '일 평균 지출',
    previous_month_change      DECIMAL(6, 2)  NULL COMMENT '전월 대비 증감률(%)',
    spending_rank              INT            NULL COMMENT '소비 순위(1일~말일 기준, 기타 포함)',
    mission_period_spending    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '미션 기준 지출액(1~28일)',
    mission_transaction_count  INT            NOT NULL DEFAULT 0 COMMENT '미션 기준 거래 횟수(1~28일)',
    spending_share_score       DECIMAL(6, 4)  NULL COMMENT '지출 비율 점수(0~1)',
    increase_score             DECIMAL(6, 4)  NULL COMMENT '최근 3개월 대비 증가 점수(0~1)',
    amount_rank_score          DECIMAL(6, 4)  NULL COMMENT '지출 순위 점수(0~1)',
    recommendation_score       DECIMAL(6, 4)  NULL COMMENT '최종 추천 점수(0~1)',
    recommendation_rank        INT            NULL COMMENT '절감 추천 순위(TOP 3)',
    recommendation_eligible    TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '추천 후보 필터 통과 여부',
    exclusion_reason           VARCHAR(50)    NULL COMMENT '추천 제외 사유',
    recommendation_reason      VARCHAR(500)   NULL COMMENT '추천 선정 근거',
    coaching_message           VARCHAR(500)   NULL COMMENT 'AI 코칭 문구',
    created_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_category_analyses_analysis_category (monthly_analysis_id, category_id),
    CONSTRAINT fk_monthly_category_analyses_analysis FOREIGN KEY (monthly_analysis_id) REFERENCES monthly_spending_analyses (id),
    CONSTRAINT fk_monthly_category_analyses_category FOREIGN KEY (category_id) REFERENCES spending_categories (id)
) COMMENT '월간 카테고리별 소비·절감 추천 분석';

CREATE INDEX idx_monthly_spending_analyses_user_id ON monthly_spending_analyses (user_id);
CREATE INDEX idx_monthly_category_analyses_monthly_analysis_id ON monthly_category_analyses (monthly_analysis_id);
