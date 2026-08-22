package com.tripass.saving.dto;

import java.math.BigDecimal;
import java.util.List;

public record MissionDetailResponseDto(
        String categoryCode,
        String categoryName,
        String analysisYearMonth,

        // 핵심 수치
        BigDecimal currentMonthSpending,
        Integer transactionCount,
        BigDecimal spendingRatio,
        Integer spendingRank,
        BigDecimal weeklyAverage,
        BigDecimal dailyAverage,

        // 비교 근거
        BigDecimal previousMonthSpending,
        BigDecimal previousMonthChange,
        BigDecimal threeMonthAverage,
        BigDecimal threeMonthAverageChange,
        BigDecimal projectedMonthSpending,

        // 패턴 분석
        String peakSpendingDay,
        Integer peakSpendingDayCount,

        // AI 분석
        String recommendationReason,
        List<String> analysisInsights,

        // 차트 데이터
        List<MonthlyTrendItem> monthlyTrend,
        List<WeeklyBreakdownItem> weeklyBreakdown,
        List<TopMerchantItem> topMerchants
) {

    public record MonthlyTrendItem(
            String yearMonth,
            BigDecimal spending,
            Integer transactionCount
    ) {}

    public record WeeklyBreakdownItem(
            Integer week,
            BigDecimal spending,
            Integer transactionCount
    ) {}

    public record TopMerchantItem(
            String merchantName,
            BigDecimal totalAmount,
            Integer transactionCount
    ) {}
}
