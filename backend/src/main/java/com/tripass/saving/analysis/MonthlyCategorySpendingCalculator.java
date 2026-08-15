package com.tripass.saving.analysis;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카테고리별 소비를 1일~말일(소비 순위 표시용)과 1~28일(미션 기준)로 동시에 집계한다.
 */
@Component
public class MonthlyCategorySpendingCalculator {

    private static final int MISSION_CUTOFF_DAY = 28;
    private static final int WEEKS_PER_MONTH = 4;

    /**
     * @param spendings                          분석월 소비 내역
     * @param analysisMonth                       분석월(일 평균 계산에 사용할 말일 정보용)
     * @param previousMonthTotalSpendingByCategory 카테고리별 전월 총지출(1일~말일). 데이터 없으면 비어있어도 된다.
     */
    public List<CategorySpendingStats> aggregate(
            List<CategorizedSpending> spendings,
            YearMonth analysisMonth,
            Map<Long, BigDecimal> previousMonthTotalSpendingByCategory
    ) {
        Map<Long, List<CategorizedSpending>> byCategory = spendings.stream()
                .collect(Collectors.groupingBy(CategorizedSpending::categoryId));

        BigDecimal grandTotal = spendings.stream()
                .map(CategorizedSpending::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryAccumulation> accumulations = byCategory.entrySet().stream()
                .map(entry -> accumulate(entry.getKey(), entry.getValue()))
                .toList();

        Map<Long, Integer> rankByCategory = rankByTotalSpending(accumulations);
        int daysInMonth = analysisMonth.lengthOfMonth();

        return accumulations.stream()
                .map(acc -> new CategorySpendingStats(
                        acc.categoryId(),
                        acc.totalSpending(),
                        calculateRatio(acc.totalSpending(), grandTotal),
                        acc.transactionCount(),
                        rankByCategory.get(acc.categoryId()),
                        // KRW는 소수 단위가 없으므로(DB 컬럼도 INT) 원 단위로 반올림해서 계산한다.
                        acc.totalSpending().divide(BigDecimal.valueOf(WEEKS_PER_MONTH), 0, RoundingMode.HALF_UP),
                        acc.totalSpending().divide(BigDecimal.valueOf(daysInMonth), 0, RoundingMode.HALF_UP),
                        calculatePreviousMonthChange(
                                acc.totalSpending(),
                                previousMonthTotalSpendingByCategory.get(acc.categoryId())),
                        acc.missionPeriodSpending(),
                        acc.missionTransactionCount(),
                        acc.maxSingleMissionTransactionAmount()
                ))
                .toList();
    }

    private CategoryAccumulation accumulate(Long categoryId, List<CategorizedSpending> transactions) {
        BigDecimal totalSpending = BigDecimal.ZERO;
        int transactionCount = 0;
        BigDecimal missionPeriodSpending = BigDecimal.ZERO;
        int missionTransactionCount = 0;
        BigDecimal maxSingleMissionTransactionAmount = BigDecimal.ZERO;

        for (CategorizedSpending spending : transactions) {
            totalSpending = totalSpending.add(spending.amount());
            transactionCount++;
            if (spending.transactionDate().getDayOfMonth() <= MISSION_CUTOFF_DAY) {
                missionPeriodSpending = missionPeriodSpending.add(spending.amount());
                missionTransactionCount++;
                if (spending.amount().compareTo(maxSingleMissionTransactionAmount) > 0) {
                    maxSingleMissionTransactionAmount = spending.amount();
                }
            }
        }

        return new CategoryAccumulation(
                categoryId, totalSpending, transactionCount,
                missionPeriodSpending, missionTransactionCount, maxSingleMissionTransactionAmount
        );
    }

    /**
     * 총지출 내림차순 → 거래 횟수 내림차순 → 카테고리 ID 오름차순으로 결정적인 순위를 매긴다.
     * (동률이면 항상 같은 순서가 나오도록 하여, 집계 결과의 Map 순서 등에 영향받지 않게 한다.)
     */
    private Map<Long, Integer> rankByTotalSpending(List<CategoryAccumulation> accumulations) {
        List<CategoryAccumulation> sorted = accumulations.stream()
                .sorted(Comparator
                        .comparing(CategoryAccumulation::totalSpending, Comparator.reverseOrder())
                        .thenComparing(CategoryAccumulation::transactionCount, Comparator.reverseOrder())
                        .thenComparing(CategoryAccumulation::categoryId))
                .toList();

        Map<Long, Integer> rank = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            rank.put(sorted.get(i).categoryId(), i + 1);
        }
        return rank;
    }

    private BigDecimal calculateRatio(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePreviousMonthChange(BigDecimal currentTotal, BigDecimal previousTotal) {
        if (previousTotal == null || previousTotal.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return currentTotal.subtract(previousTotal)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousTotal, 2, RoundingMode.HALF_UP);
    }

    private record CategoryAccumulation(
            Long categoryId,
            BigDecimal totalSpending,
            int transactionCount,
            BigDecimal missionPeriodSpending,
            int missionTransactionCount,
            BigDecimal maxSingleMissionTransactionAmount
    ) {
    }
}
