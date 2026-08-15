package com.tripass.saving.dto;

import java.util.List;

/**
 * 카테고리별 선택 목록과, 화면 상단에 바로 쓸 수 있는 합계를 함께 담는다(PUT/GET mission-selections 응답).
 * 합계는 Service에서 selections를 합산해 채운다.
 */
public record MissionSelectionsResponseDto(
        Integer selectedMissionCount,
        Long totalMonthlyReductionTarget,
        Long totalWeeklyExpectedSaving,
        List<MissionSelectionResponseDto> selections
) {
}
