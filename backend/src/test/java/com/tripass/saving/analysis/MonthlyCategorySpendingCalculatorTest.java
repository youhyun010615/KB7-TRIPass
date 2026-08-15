package com.tripass.saving.analysis;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MonthlyCategorySpendingCalculatorTest {

    private final MonthlyCategorySpendingCalculator calculator = new MonthlyCategorySpendingCalculator();
    private static final Long FOOD = 1L;
    private static final Long CAFE = 2L;
    private static final YearMonth JULY_2026 = YearMonth.of(2026, 7); // 31일

    @Test
    void 카테고리별_지출액과_비율_순위를_계산한다() {
        List<CategorizedSpending> spendings = List.of(
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 5), new BigDecimal("30000")),
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 10), new BigDecimal("20000")),
                new CategorizedSpending(CAFE, LocalDate.of(2026, 7, 3), new BigDecimal("10000"))
        );

        Map<Long, CategorySpendingStats> statsByCategory =
                statsByCategoryId(calculator.aggregate(spendings, JULY_2026, Map.of()));

        CategorySpendingStats food = statsByCategory.get(FOOD);
        assertEquals(new BigDecimal("50000"), food.totalSpending());
        assertEquals(new BigDecimal("83.33"), food.spendingRatio()); // 50000/60000*100
        assertEquals(2, food.transactionCount());
        assertEquals(1, food.spendingRank());

        CategorySpendingStats cafe = statsByCategory.get(CAFE);
        assertEquals(new BigDecimal("10000"), cafe.totalSpending());
        assertEquals(2, cafe.spendingRank());
    }

    @Test
    void 이십구일_이후_거래는_전체_지출에는_포함하고_미션_기준에서는_제외한다() {
        List<CategorizedSpending> spendings = List.of(
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 20), new BigDecimal("30000")),
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 29), new BigDecimal("15000"))
        );

        CategorySpendingStats food =
                statsByCategoryId(calculator.aggregate(spendings, JULY_2026, Map.of())).get(FOOD);

        assertEquals(new BigDecimal("45000"), food.totalSpending());
        assertEquals(2, food.transactionCount());
        assertEquals(new BigDecimal("30000"), food.missionPeriodSpending());
        assertEquals(1, food.missionTransactionCount());
    }

    @Test
    void 미션_기간_내_최대_단일_거래금액을_추적한다() {
        List<CategorizedSpending> spendings = List.of(
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 1), new BigDecimal("10000")),
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 2), new BigDecimal("70000")),
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 3), new BigDecimal("20000"))
        );

        CategorySpendingStats food =
                statsByCategoryId(calculator.aggregate(spendings, JULY_2026, Map.of())).get(FOOD);

        assertEquals(new BigDecimal("70000"), food.maxSingleMissionTransactionAmount());
    }

    @Test
    void 거래가_없으면_빈_목록을_반환한다() {
        assertEquals(0, calculator.aggregate(List.of(), JULY_2026, Map.of()).size());
    }

    @Test
    void 주간_평균은_4로_일_평균은_해당_월_일수로_나눈다() {
        List<CategorizedSpending> spendings = List.of(
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 1), new BigDecimal("50000"))
        );

        CategorySpendingStats food =
                statsByCategoryId(calculator.aggregate(spendings, JULY_2026, Map.of())).get(FOOD);

        assertEquals(new BigDecimal("12500"), food.weeklyAverage()); // 50000/4
        assertEquals(new BigDecimal("1613"), food.dailyAverage());   // 50000/31일, HALF_UP 반올림(원 단위)
    }

    @Test
    void 전월_지출이_있으면_증감률을_계산한다() {
        List<CategorizedSpending> spendings = List.of(
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 1), new BigDecimal("120000"))
        );

        CategorySpendingStats food = statsByCategoryId(
                calculator.aggregate(spendings, JULY_2026, Map.of(FOOD, new BigDecimal("100000")))
        ).get(FOOD);

        assertEquals(new BigDecimal("20.00"), food.previousMonthChange()); // (120000-100000)/100000*100
    }

    @Test
    void 전월_지출이_없으면_증감률은_null이다() {
        List<CategorizedSpending> spendings = List.of(
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 1), new BigDecimal("50000"))
        );

        CategorySpendingStats food =
                statsByCategoryId(calculator.aggregate(spendings, JULY_2026, Map.of())).get(FOOD);

        assertNull(food.previousMonthChange());
    }

    @Test
    void 총지출이_같으면_거래_횟수가_많은_카테고리가_더_높은_순위다() {
        List<CategorizedSpending> spendings = List.of(
                new CategorizedSpending(FOOD, LocalDate.of(2026, 7, 1), new BigDecimal("50000")),
                new CategorizedSpending(CAFE, LocalDate.of(2026, 7, 1), new BigDecimal("25000")),
                new CategorizedSpending(CAFE, LocalDate.of(2026, 7, 2), new BigDecimal("25000"))
        );

        Map<Long, CategorySpendingStats> stats =
                statsByCategoryId(calculator.aggregate(spendings, JULY_2026, Map.of()));

        assertEquals(1, stats.get(CAFE).spendingRank()); // 거래 2건
        assertEquals(2, stats.get(FOOD).spendingRank()); // 거래 1건
    }

    private Map<Long, CategorySpendingStats> statsByCategoryId(List<CategorySpendingStats> stats) {
        return stats.stream()
                .collect(java.util.stream.Collectors.toMap(CategorySpendingStats::categoryId, s -> s));
    }
}
