package com.tripass.saving.dto;

/**
 * 절감률(10/30/50%) 하나를 선택했을 때의 예상 금액. 저장되지 않고 매번 계산해서 응답한다.
 */
public record ReductionRateOptionDto(
        Integer reductionRate,
        Integer monthlyReductionTarget,
        Integer monthlyUsageTarget,
        Integer weeklyUsageLimit,
        Integer weeklyExpectedSaving
) {
}
