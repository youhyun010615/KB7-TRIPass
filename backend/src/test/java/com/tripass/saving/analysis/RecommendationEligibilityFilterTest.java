package com.tripass.saving.analysis;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationEligibilityFilterTest {

    private final RecommendationEligibilityFilter filter = new RecommendationEligibilityFilter();

    @Test
    void 모든_조건을_만족하면_통과한다() {
        CategorySpendingStats stats = stats(
                new BigDecimal("100000"), 10, new BigDecimal("30000"));

        EligibilityResult result = filter.evaluate(stats, 30);

        assertTrue(result.eligible());
    }

    @Test
    void 수집_기간이_30일_미만이면_제외한다() {
        CategorySpendingStats stats = stats(
                new BigDecimal("100000"), 10, new BigDecimal("30000"));

        EligibilityResult result = filter.evaluate(stats, 29);

        assertEquals(RecommendationEligibilityFilter.INSUFFICIENT_COLLECTION_PERIOD, result.exclusionReason());
    }

    @Test
    void 거래_횟수가_5건_미만이면_제외한다() {
        CategorySpendingStats stats = stats(
                new BigDecimal("100000"), 4, new BigDecimal("30000"));

        EligibilityResult result = filter.evaluate(stats, 30);

        assertEquals(RecommendationEligibilityFilter.MIN_TRANSACTION_COUNT_NOT_MET, result.exclusionReason());
    }

    @Test
    void 지출액이_5만원_미만이면_제외한다() {
        CategorySpendingStats stats = stats(
                new BigDecimal("49999"), 10, new BigDecimal("10000"));

        EligibilityResult result = filter.evaluate(stats, 30);

        assertEquals(RecommendationEligibilityFilter.MIN_AMOUNT_NOT_MET, result.exclusionReason());
    }

    @Test
    void 단일_거래_비중이_50퍼센트_이상이면_제외한다() {
        CategorySpendingStats stats = stats(
                new BigDecimal("100000"), 10, new BigDecimal("50000"));

        EligibilityResult result = filter.evaluate(stats, 30);

        assertEquals(RecommendationEligibilityFilter.SINGLE_TRANSACTION_DOMINANT, result.exclusionReason());
    }

    @Test
    void 단일_거래_비중이_50퍼센트_미만이면_통과한다() {
        CategorySpendingStats stats = stats(
                new BigDecimal("100000"), 10, new BigDecimal("40000"));

        EligibilityResult result = filter.evaluate(stats, 30);

        assertTrue(result.eligible());
    }

    private CategorySpendingStats stats(BigDecimal missionSpending, int missionCount, BigDecimal maxSingle) {
        return new CategorySpendingStats(
                1L, missionSpending, BigDecimal.ZERO, missionCount, 1,
                BigDecimal.ZERO, BigDecimal.ZERO, null,
                missionSpending, missionCount, maxSingle
        );
    }
}
