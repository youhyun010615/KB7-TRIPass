package com.tripass.saving.dto;

import java.math.BigDecimal;

/**
 * 지난달 저축 결과.
 *
 * targetAmount 또는 actualAmount를 산출할 데이터가 없으면 status는 UNAVAILABLE이 된다.
 * 이때 actualAmount와 differenceAmount는 0으로 대체하지 않고 null로 남기며,
 * resultMessage에는 null 대신 집계 불가 안내 문구를 반환한다.
 */
public record SavingResultResponseDto(
        SavingResultStatus status,
        BigDecimal targetAmount,
        BigDecimal actualAmount,
        BigDecimal differenceAmount,
        String resultMessage
) {
}
