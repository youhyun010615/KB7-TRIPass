USE tripass;

-- #220: 카테고리별 절감률 선택
-- monthly_category_analyses와 분리한다. #210 리포트가 재계산될 때 monthly_category_analyses는
-- delete 후 재삽입되므로, 같은 테이블에 두면 재계산할 때마다 사용자의 선택이 사라진다.
-- 행이 존재하면 선택된 것으로 간주한다(별도 선택 여부 컬럼을 두지 않는다).
CREATE TABLE mission_category_selections
(
    id                            BIGINT    NOT NULL AUTO_INCREMENT COMMENT '미션 카테고리 선택 ID',
    monthly_spending_analysis_id  BIGINT    NOT NULL COMMENT '월간 분석 ID',
    category_id                   BIGINT    NOT NULL COMMENT '소비 카테고리 ID',
    reduction_rate                TINYINT   NOT NULL COMMENT '절감률(10/30/50, %)',
    baseline_spending_amount      INT       NOT NULL COMMENT '선택 당시 monthly_category_analyses.mission_period_spending 스냅샷(원)',
    monthly_reduction_target      INT       NOT NULL COMMENT '월 절감 목표 금액(원)',
    monthly_usage_target          INT       NOT NULL COMMENT '월 사용 목표 금액(원)',
    created_at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mission_category_selections_analysis_category (monthly_spending_analysis_id, category_id),
    CONSTRAINT fk_mission_category_selections_analysis
        FOREIGN KEY (monthly_spending_analysis_id) REFERENCES monthly_spending_analyses (id),
    CONSTRAINT fk_mission_category_selections_category
        FOREIGN KEY (category_id) REFERENCES spending_categories (id),
    CONSTRAINT chk_mission_category_selections_reduction_rate CHECK (reduction_rate IN (10, 30, 50))
) COMMENT '카테고리별 절감률 선택';
