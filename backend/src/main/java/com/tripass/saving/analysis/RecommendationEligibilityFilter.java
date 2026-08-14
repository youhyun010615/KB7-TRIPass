package com.tripass.saving.analysis;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 절감 추천 후보 필터. 모두 통과해야 추천 점수 계산 대상이 된다.
 */
@Component
public class RecommendationEligibilityFilter {

    public static final String MIN_TRANSACTION_COUNT_NOT_MET = "MIN_TRANSACTION_COUNT_NOT_MET";
    public static final String MIN_AMOUNT_NOT_MET = "MIN_AMOUNT_NOT_MET";
    public static final String INSUFFICIENT_COLLECTION_PERIOD = "INSUFFICIENT_COLLECTION_PERIOD";
    public static final String SINGLE_TRANSACTION_DOMINANT = "SINGLE_TRANSACTION_DOMINANT";

    private static final int MIN_TRANSACTION_COUNT = 5;
    private static final BigDecimal MIN_SPENDING_AMOUNT = new BigDecimal("50000");
    private static final int MIN_COLLECTION_PERIOD_DAYS = 30;
    private static final BigDecimal SINGLE_TRANSACTION_DOMINANCE_THRESHOLD = new BigDecimal("0.5");

    public EligibilityResult evaluate(CategorySpendingStats stats, int collectionPeriodDays) {
        if (collectionPeriodDays < MIN_COLLECTION_PERIOD_DAYS) {
            return EligibilityResult.excluded(INSUFFICIENT_COLLECTION_PERIOD);
        }
        if (stats.missionTransactionCount() < MIN_TRANSACTION_COUNT) {
            return EligibilityResult.excluded(MIN_TRANSACTION_COUNT_NOT_MET);
        }
        if (stats.missionPeriodSpending().compareTo(MIN_SPENDING_AMOUNT) < 0) {
            return EligibilityResult.excluded(MIN_AMOUNT_NOT_MET);
        }
        if (isSingleTransactionDominant(stats)) {
            return EligibilityResult.excluded(SINGLE_TRANSACTION_DOMINANT);
        }
        return EligibilityResult.pass();
    }

    private boolean isSingleTransactionDominant(CategorySpendingStats stats) {
        if (stats.missionPeriodSpending().compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal ratio = stats.maxSingleMissionTransactionAmount()
                .divide(stats.missionPeriodSpending(), 4, RoundingMode.HALF_UP);
        return ratio.compareTo(SINGLE_TRANSACTION_DOMINANCE_THRESHOLD) >= 0;
    }
}
