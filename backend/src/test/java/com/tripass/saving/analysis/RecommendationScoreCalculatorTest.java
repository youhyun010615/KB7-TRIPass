package com.tripass.saving.analysis;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecommendationScoreCalculatorTest {

    private final RecommendationScoreCalculator calculator = new RecommendationScoreCalculator();
    private static final Long FOOD = 1L;
    private static final Long CAFE = 2L;

    @Test
    void 지출_비율_점수는_6개_카테고리_총지출_대비_비중이다() {
        List<CategorySpendingStats> candidates = List.of(
                stats(FOOD, new BigDecimal("300000"), 10),
                stats(CAFE, new BigDecimal("200000"), 10)
        );

        List<ScoredCandidate> scored = calculator.calculate(
                candidates, new BigDecimal("500000"), Map.of());

        ScoredCandidate food = byCategory(scored, FOOD);
        ScoredCandidate cafe = byCategory(scored, CAFE);

        assertEquals(new BigDecimal("0.6000"), food.spendingShareScore());
        assertEquals(new BigDecimal("0.4000"), cafe.spendingShareScore());
    }

    @Test
    void 동일_지출액_후보는_순위_점수가_같다() {
        Long a = 1L;
        Long b = 2L;
        Long c = 3L;
        List<CategorySpendingStats> candidates = List.of(
                stats(a, new BigDecimal("100000"), 10),
                stats(b, new BigDecimal("100000"), 10),
                stats(c, new BigDecimal("50000"), 10)
        );

        List<ScoredCandidate> scored = calculator.calculate(
                candidates, new BigDecimal("500000"), Map.of());

        assertEquals(byCategory(scored, a).amountRankScore(), byCategory(scored, b).amountRankScore());
    }

    @Test
    void 동일_지출액_후보는_입력_순서를_바꿔도_결과가_같다() {
        Long a = 1L;
        Long b = 2L;
        CategorySpendingStats candidateA = stats(a, new BigDecimal("100000"), 10);
        CategorySpendingStats candidateB = stats(b, new BigDecimal("100000"), 10);

        List<ScoredCandidate> forward = calculator.calculate(
                List.of(candidateA, candidateB), new BigDecimal("500000"), Map.of());
        List<ScoredCandidate> reversed = calculator.calculate(
                List.of(candidateB, candidateA), new BigDecimal("500000"), Map.of());

        assertEquals(
                byCategory(forward, a).amountRankScore(),
                byCategory(reversed, a).amountRankScore());
        assertEquals(
                byCategory(forward, b).amountRankScore(),
                byCategory(reversed, b).amountRankScore());
    }

    @Test
    void 순위_점수는_미션지출_금액_순위로_계산한다() {
        List<CategorySpendingStats> candidates = List.of(
                stats(FOOD, new BigDecimal("300000"), 10),
                stats(CAFE, new BigDecimal("200000"), 10)
        );

        List<ScoredCandidate> scored = calculator.calculate(
                candidates, new BigDecimal("500000"), Map.of());

        assertEquals(new BigDecimal("1.0000"), byCategory(scored, FOOD).amountRankScore()); // 1위: (2-1+1)/2
        assertEquals(new BigDecimal("0.5000"), byCategory(scored, CAFE).amountRankScore()); // 2위: (2-2+1)/2
    }

    @Test
    void 증가_점수는_최근_3개월_평균_대비_증가율이다() {
        List<CategorySpendingStats> candidates = List.of(stats(FOOD, new BigDecimal("150000"), 10));
        Map<Long, List<BigDecimal>> previous = Map.of(
                FOOD, List.of(new BigDecimal("100000"), new BigDecimal("100000"), new BigDecimal("100000")));

        List<ScoredCandidate> scored = calculator.calculate(candidates, new BigDecimal("150000"), previous);

        // 증가율 = (150000-100000)/100000 = 0.5
        assertEquals(new BigDecimal("0.5000"), byCategory(scored, FOOD).increaseScore());
    }

    @Test
    void 증가율이_100퍼센트를_초과하면_1로_고정한다() {
        List<CategorySpendingStats> candidates = List.of(stats(FOOD, new BigDecimal("400000"), 10));
        Map<Long, List<BigDecimal>> previous = Map.of(FOOD, List.of(new BigDecimal("100000")));

        List<ScoredCandidate> scored = calculator.calculate(candidates, new BigDecimal("400000"), previous);

        assertEquals(new BigDecimal("1.0000"), byCategory(scored, FOOD).increaseScore());
        // 점수는 1.0000으로 캡핑되지만, 실제 증가율(3배=300%)은 캡핑 없이 그대로 보존해야 코칭 문구가 정확하다.
        assertEquals(new BigDecimal("3.0000"), byCategory(scored, FOOD).actualIncreaseRate());
    }

    @Test
    void 지출이_감소했으면_증가_점수는_0이지만_실제_증가율은_음수로_보존한다() {
        List<CategorySpendingStats> candidates = List.of(stats(FOOD, new BigDecimal("50000"), 10));
        Map<Long, List<BigDecimal>> previous = Map.of(FOOD, List.of(new BigDecimal("100000")));

        List<ScoredCandidate> scored = calculator.calculate(candidates, new BigDecimal("50000"), previous);

        assertEquals(new BigDecimal("0.0000"), byCategory(scored, FOOD).increaseScore());
        assertEquals(new BigDecimal("-0.5000"), byCategory(scored, FOOD).actualIncreaseRate());
    }

    @Test
    void 이전_데이터가_없으면_증가_점수는_0이고_실제_증가율은_null이다() {
        List<CategorySpendingStats> candidates = List.of(stats(FOOD, new BigDecimal("100000"), 10));

        List<ScoredCandidate> scored = calculator.calculate(candidates, new BigDecimal("100000"), Map.of());

        assertEquals(new BigDecimal("0.0000"), byCategory(scored, FOOD).increaseScore());
        assertNull(byCategory(scored, FOOD).actualIncreaseRate());
    }

    @Test
    void 최종_점수는_가중합으로_계산한다() {
        // share=1.0(단일후보), increase=0.5, rank=1.0(단일후보)
        List<CategorySpendingStats> candidates = List.of(stats(FOOD, new BigDecimal("150000"), 10));
        Map<Long, List<BigDecimal>> previous = Map.of(FOOD, List.of(new BigDecimal("100000")));

        List<ScoredCandidate> scored = calculator.calculate(candidates, new BigDecimal("150000"), previous);

        // 1.0*0.5 + 0.5*0.3 + 1.0*0.2 = 0.5+0.15+0.2 = 0.85
        assertEquals(new BigDecimal("0.8500"), byCategory(scored, FOOD).recommendationScore());
    }

    private CategorySpendingStats stats(Long categoryId, BigDecimal missionSpending, int missionCount) {
        return new CategorySpendingStats(
                categoryId, missionSpending, BigDecimal.ZERO, missionCount, 1,
                BigDecimal.ZERO, BigDecimal.ZERO, null,
                missionSpending, missionCount, BigDecimal.ZERO
        );
    }

    private ScoredCandidate byCategory(List<ScoredCandidate> scored, Long categoryId) {
        return scored.stream().filter(s -> s.categoryId().equals(categoryId)).findFirst().orElseThrow();
    }
}
