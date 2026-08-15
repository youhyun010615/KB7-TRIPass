package com.tripass.saving.dto;

import java.math.BigDecimal;

/**
 * 소비 순위 항목 — 지난달 1일~말일 기준, 기타 포함 7개 카테고리.
 */
public record SpendingCategoryResponseDto(
        Integer rank,
        String categoryCode,
        String categoryName,
        BigDecimal amount,
        BigDecimal ratio,
        Integer transactionCount
) {
}
