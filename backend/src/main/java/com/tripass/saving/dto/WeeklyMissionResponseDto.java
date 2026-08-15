package com.tripass.saving.dto;

import java.time.LocalDate;

public record WeeklyMissionResponseDto(
        Long id,
        Integer weekNumber,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        Integer weeklyUsageLimit,
        Integer weeklyExpectedSaving,
        Integer actualSpending,
        Integer actualSaving,
        String status,
        String missionMessage
) {
}
