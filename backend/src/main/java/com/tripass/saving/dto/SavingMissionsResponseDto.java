package com.tripass.saving.dto;

import java.util.List;

public record SavingMissionsResponseDto(
        String targetYearMonth,
        Integer missionCount,
        Long totalPlannedSavingAmount,
        List<MonthlyMissionResponseDto> missions
) {
}
