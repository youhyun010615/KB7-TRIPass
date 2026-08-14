package com.tripass.saving.dto;

import java.math.BigDecimal;

/**
 * 지난달 저축 결과.
 */
public record SavingResultResponseDto(
        BigDecimal targetAmount,
        BigDecimal actualAmount,
        BigDecimal differenceAmount,
        String resultMessage
) {
}
