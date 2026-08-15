package com.tripass.saving.dto;

import java.util.List;

/**
 * 절감 추천 TOP 3 카테고리 하나에 대한 절감률별 예상 금액 목록(GET /mission-options 응답 항목).
 */
public record MissionOptionResponseDto(
        Long categoryId,
        String categoryCode,
        String categoryName,
        Integer recommendationRank,
        String recommendationReason,
        Integer baselineSpendingAmount,
        List<ReductionRateOptionDto> options
) {
}
