package com.tripass.saving.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklyMissionEvaluationResponseDto(
        String targetYearMonth,
        int weekNumber,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        int evaluatedMissionCount,
        long successCount,
        long failedCount,
        List<WeeklyMissionEvaluationItemDto> missions
) {
}
