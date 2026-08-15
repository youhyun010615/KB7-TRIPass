package com.tripass.saving.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 월간 AI 소비 분석 리포트 응답.
 *
 * spendingCategories(소비 순위, 기타 포함, 1일~말일)와 recommendedCategories(절감 추천, 기타 제외,
 * 1~28일)는 목적·기간·대상 카테고리가 서로 달라 별도 필드로 분리한다.
 */
public record MonthlyAnalysisResponseDto(
        String analysisYearMonth,
        String targetYearMonth,
        String reportStatus,
        BigDecimal totalSpending,
        SavingResultResponseDto savingResult,
        List<SpendingCategoryResponseDto> spendingCategories,
        String coachingSummary,
        List<RecommendedCategoryResponseDto> recommendedCategories
) {
}
