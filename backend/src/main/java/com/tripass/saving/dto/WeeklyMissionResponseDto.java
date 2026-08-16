package com.tripass.saving.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeeklyMissionResponseDto(
        Long id,
        Integer weekNumber,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        Integer weeklyUsageLimit,
        Integer weeklyExpectedSaving,
        Integer actualSpending,
        Integer actualSaving,
        Integer rewardAmount,
        LocalDateTime rewardedAt,
        String status,
        String missionMessage
) {
}
