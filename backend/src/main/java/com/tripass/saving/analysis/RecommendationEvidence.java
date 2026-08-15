package com.tripass.saving.analysis;

import java.math.BigDecimal;

/**
 * 사용자에게 보여줄 절감 추천 근거. 추천 여부·우선순위 판단에는 정규화된 {@link ScoredCandidate} 점수를 쓰지만,
 * 코칭 문구에 노출하는 수치는 캡핑되지 않은 실제 분석값이어야 하므로 별도로 둔다.
 */
public record RecommendationEvidence(
        BigDecimal spendingRatio, // 미션 대상 6개 카테고리 총지출 대비 이 카테고리의 비중(%)
        BigDecimal actualIncreaseRate, // 최근 3개월 평균 대비 실제 증가율(%), 이전 데이터 없으면 null
        int transactionCount, // 1~28일 거래 횟수
        BigDecimal missionPeriodSpending // 1~28일 지출액
) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static RecommendationEvidence from(ScoredCandidate candidate, CategorySpendingStats stats) {
        BigDecimal actualIncreaseRatePercent = candidate.actualIncreaseRate() != null
                ? candidate.actualIncreaseRate().multiply(HUNDRED)
                : null;
        return new RecommendationEvidence(
                candidate.spendingShareScore().multiply(HUNDRED),
                actualIncreaseRatePercent,
                stats.missionTransactionCount(),
                stats.missionPeriodSpending()
        );
    }
}
