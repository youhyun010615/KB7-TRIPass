package com.tripass.saving.dto;

/**
 * 저장된 카테고리 선택 결과(GET/PUT mission-selections 응답 항목).
 * weeklyUsageLimit/weeklyExpectedSaving은 DB에 저장된 값이 아니라, 저장된 월 단위 금액을 4로
 * 나눠 매번 계산한 기본값이다(부분 주차 등 실제 값은 후속 미션 생성 단계에서 별도로 정해진다).
 */
public record MissionSelectionResponseDto(
        Long categoryId,
        String categoryCode,
        String categoryName,
        Integer reductionRate,
        Integer baselineSpendingAmount,
        Integer monthlyReductionTarget,
        Integer monthlyUsageTarget,
        Integer weeklyUsageLimit,
        Integer weeklyExpectedSaving
) {
}
