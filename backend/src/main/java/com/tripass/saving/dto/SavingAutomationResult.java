package com.tripass.saving.dto;

/**
 * 저축 자동 처리 작업의 실행 결과입니다.
 *
 * @param jobName 처리 작업명
 * @param target 처리 기준(분석 연월 또는 미션 연월/주차)
 * @param targetCount 전체 처리 대상 사용자 수
 * @param successCount 정상 처리 사용자 수
 * @param failureCount 처리 실패 사용자 수
 */
public record SavingAutomationResult(
        String jobName,
        String target,
        int targetCount,
        int successCount,
        int failureCount
) {
}
