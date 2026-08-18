package com.tripass.saving.dto;

/**
 * 프론트엔드가 일반 조회 오류와 사용자 사전 조건 미충족을 구분하기 위한 상태 응답.
 */
public record SavingReadinessResponseDto(
        boolean travelGoalRegistered,
        boolean accountLinked,
        boolean cardLinked,
        boolean financialAssetLinked,
        boolean missionPrerequisitesMet,
        SavingReadinessStatus status
) {
}
