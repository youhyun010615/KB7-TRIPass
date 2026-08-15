package com.tripass.saving.analysis;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopCategorySelectorTest {

    private final TopCategorySelector selector = new TopCategorySelector();

    @Test
    void 점수가_높은_순으로_최대_3개를_선정한다() {
        List<ScoredCandidate> scored = List.of(
                scoredCandidate(1L, "0.9"),
                scoredCandidate(2L, "0.7"),
                scoredCandidate(3L, "0.5"),
                scoredCandidate(4L, "0.3")
        );
        Map<Long, CategorySpendingStats> statsById = Map.of(
                1L, stats(1L, "100000", 5),
                2L, stats(2L, "100000", 5),
                3L, stats(3L, "100000", 5),
                4L, stats(4L, "100000", 5)
        );

        List<Long> result = selector.selectTopCategoryIds(scored, statsById);

        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    void 점수가_같으면_미션지출액이_큰_카테고리를_우선한다() {
        List<ScoredCandidate> scored = List.of(
                scoredCandidate(1L, "0.5"),
                scoredCandidate(2L, "0.5")
        );
        Map<Long, CategorySpendingStats> statsById = Map.of(
                1L, stats(1L, "80000", 5),
                2L, stats(2L, "120000", 5)
        );

        List<Long> result = selector.selectTopCategoryIds(scored, statsById);

        assertEquals(List.of(2L, 1L), result);
    }

    @Test
    void 점수와_금액이_같으면_거래_횟수가_많은_카테고리를_우선한다() {
        List<ScoredCandidate> scored = List.of(
                scoredCandidate(1L, "0.5"),
                scoredCandidate(2L, "0.5")
        );
        Map<Long, CategorySpendingStats> statsById = Map.of(
                1L, stats(1L, "100000", 5),
                2L, stats(2L, "100000", 8)
        );

        List<Long> result = selector.selectTopCategoryIds(scored, statsById);

        assertEquals(List.of(2L, 1L), result);
    }

    @Test
    void 점수_금액_횟수가_모두_같으면_카테고리_ID_오름차순이다() {
        List<ScoredCandidate> scored = List.of(
                scoredCandidate(5L, "0.5"),
                scoredCandidate(2L, "0.5")
        );
        Map<Long, CategorySpendingStats> statsById = Map.of(
                5L, stats(5L, "100000", 5),
                2L, stats(2L, "100000", 5)
        );

        List<Long> result = selector.selectTopCategoryIds(scored, statsById);

        assertEquals(List.of(2L, 5L), result);
    }

    @Test
    void 후보가_3개_미만이면_통과한_만큼만_반환한다() {
        List<ScoredCandidate> scored = List.of(scoredCandidate(1L, "0.5"));
        Map<Long, CategorySpendingStats> statsById = Map.of(1L, stats(1L, "100000", 5));

        List<Long> result = selector.selectTopCategoryIds(scored, statsById);

        assertEquals(List.of(1L), result);
    }

    private ScoredCandidate scoredCandidate(Long categoryId, String score) {
        return new ScoredCandidate(
                categoryId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(score), null);
    }

    private CategorySpendingStats stats(Long categoryId, String missionSpending, int missionCount) {
        BigDecimal amount = new BigDecimal(missionSpending);
        return new CategorySpendingStats(
                categoryId, amount, BigDecimal.ZERO, missionCount, 1,
                BigDecimal.ZERO, BigDecimal.ZERO, null,
                amount, missionCount, BigDecimal.ZERO
        );
    }
}
