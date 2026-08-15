package com.tripass.saving.analysis;

import java.math.BigDecimal;

/**
 * 카테고리 하나에 대한 월간 소비 집계 결과.
 *
 * totalSpending/spendingRatio/transactionCount/spendingRank는 1일~말일 기준(소비 순위 표시용),
 * missionPeriodSpending/missionTransactionCount는 1~28일 기준(절감 추천·미션 판정용)이다.
 */
public record CategorySpendingStats(
        Long categoryId,
        BigDecimal totalSpending,
        BigDecimal spendingRatio,
        int transactionCount,
        int spendingRank,
        BigDecimal weeklyAverage,
        BigDecimal dailyAverage,
        BigDecimal previousMonthChange, // 전월 대비 증감률(%), 전월 데이터가 없으면 null
        BigDecimal missionPeriodSpending,
        int missionTransactionCount,
        BigDecimal maxSingleMissionTransactionAmount
) {
}
