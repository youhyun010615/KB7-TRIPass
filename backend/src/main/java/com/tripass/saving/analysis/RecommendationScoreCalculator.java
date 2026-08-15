package com.tripass.saving.analysis;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 필터를 통과한 후보들의 절감 추천 점수를 계산한다.
 *
 * 추천 점수 = 지출 비율 점수 × 0.5 + 최근 3개월 대비 증가 점수 × 0.3 + 지출 순위 점수 × 0.2
 */
@Component
public class RecommendationScoreCalculator {

    public static final BigDecimal SPENDING_SHARE_WEIGHT = new BigDecimal("0.5");
    public static final BigDecimal INCREASE_WEIGHT = new BigDecimal("0.3");
    public static final BigDecimal AMOUNT_RANK_WEIGHT = new BigDecimal("0.2");
    private static final int SCALE = 4;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

    /**
     * @param eligibleCandidates                 필터를 통과한 후보 목록
     * @param sixCategoryTotalMissionSpending     미션 대상 6개 카테고리의 1~28일 총지출(필터 통과 여부 무관)
     * @param previousMonthsSpendingByCategory    카테고리별 최근 최대 3개월(각 1~28일) 지출 목록
     */
    public List<ScoredCandidate> calculate(
            List<CategorySpendingStats> eligibleCandidates,
            BigDecimal sixCategoryTotalMissionSpending,
            Map<Long, List<BigDecimal>> previousMonthsSpendingByCategory
    ) {
        int candidateCount = eligibleCandidates.size();
        List<ScoredCandidate> result = new ArrayList<>();

        for (CategorySpendingStats stats : eligibleCandidates) {
            // 동일 지출액은 동일 순위를 받도록, 정렬 위치가 아니라 "자신보다 지출액이 큰 후보 수"로 순위를 센다.
            // 입력 리스트의 순서(예: HashMap 기반 집계 결과)에 좌우되지 않는 결정적인 값이다.
            int rank = 1 + (int) eligibleCandidates.stream()
                    .filter(other -> other.missionPeriodSpending().compareTo(stats.missionPeriodSpending()) > 0)
                    .count();

            BigDecimal shareScore = calculateSpendingShareScore(
                    stats.missionPeriodSpending(), sixCategoryTotalMissionSpending);
            BigDecimal actualIncreaseRate = calculateActualIncreaseRate(
                    stats.missionPeriodSpending(),
                    previousMonthsSpendingByCategory.getOrDefault(stats.categoryId(), List.of()));
            BigDecimal increaseScore = capIncreaseScore(actualIncreaseRate);
            BigDecimal amountRankScore = calculateAmountRankScore(candidateCount, rank);

            BigDecimal finalScore = shareScore.multiply(SPENDING_SHARE_WEIGHT)
                    .add(increaseScore.multiply(INCREASE_WEIGHT))
                    .add(amountRankScore.multiply(AMOUNT_RANK_WEIGHT))
                    .setScale(SCALE, RoundingMode.HALF_UP);

            result.add(new ScoredCandidate(
                    stats.categoryId(), shareScore, increaseScore, amountRankScore, finalScore, actualIncreaseRate));
        }
        return result;
    }

    private BigDecimal calculateSpendingShareScore(BigDecimal categorySpending, BigDecimal totalSpending) {
        if (totalSpending.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO;
        }
        return categorySpending.divide(totalSpending, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 최근 3개월 평균 대비 실제 증가율(캡핑 없음). 코칭 문구 등 사용자에게 실제 값을 보여줄 때 사용한다.
     * 이전 데이터가 없으면 null을 반환한다.
     */
    private BigDecimal calculateActualIncreaseRate(BigDecimal currentSpending, List<BigDecimal> previousMonthsSpending) {
        if (previousMonthsSpending.isEmpty()) {
            return null;
        }
        BigDecimal sum = previousMonthsSpending.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(previousMonthsSpending.size()), SCALE, RoundingMode.HALF_UP);
        if (average.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return currentSpending.subtract(average).divide(average, SCALE, RoundingMode.HALF_UP);
    }

    /** 추천 점수 계산용으로 [0,1] 구간에 캡핑한 증가 점수. */
    private BigDecimal capIncreaseScore(BigDecimal actualIncreaseRate) {
        if (actualIncreaseRate == null) {
            return ZERO;
        }
        return actualIncreaseRate.min(BigDecimal.ONE).max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAmountRankScore(int candidateCount, int rank) {
        if (candidateCount == 0) {
            return ZERO;
        }
        return BigDecimal.valueOf(candidateCount - rank + 1)
                .divide(BigDecimal.valueOf(candidateCount), SCALE, RoundingMode.HALF_UP);
    }
}
