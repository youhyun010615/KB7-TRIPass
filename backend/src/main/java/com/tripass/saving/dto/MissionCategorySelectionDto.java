package com.tripass.saving.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * mission_category_selections 테이블 매핑 DTO.
 *
 * 행이 존재하면 선택된 것으로 간주한다(별도 선택 여부 컬럼 없음). categoryCode/categoryName은
 * 실제 테이블 컬럼이 아니라 spending_categories와 JOIN한 조회 쿼리에서만 채워지는 편의 필드다.
 */
@Getter
@Setter
public class MissionCategorySelectionDto {

    private Long id;
    private Long monthlySpendingAnalysisId;
    private Long categoryId;
    private String categoryCode; // JOIN 조회 전용
    private String categoryName; // JOIN 조회 전용

    private Integer reductionRate;             // 절감률(10/30/50, %)
    private Integer baselineSpendingAmount;    // 선택 당시 미션 기준 지출액 스냅샷(원)
    private Integer monthlyReductionTarget;    // 월 절감 목표 금액(원)
    private Integer monthlyUsageTarget;        // 월 사용 목표 금액(원)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
