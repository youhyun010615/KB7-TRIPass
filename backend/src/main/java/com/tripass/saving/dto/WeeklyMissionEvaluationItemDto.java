package com.tripass.saving.dto;

public record WeeklyMissionEvaluationItemDto(
        Long weeklyMissionId,
        Long categoryId,
        String categoryCode,
        String categoryName,
        Integer weeklyUsageLimit,
        Integer actualSpending,
        Integer actualSaving,
        Integer rewardAmount,
        java.time.LocalDateTime rewardedAt,
        String status
) {
}
