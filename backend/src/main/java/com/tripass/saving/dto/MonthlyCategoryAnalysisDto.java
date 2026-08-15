package com.tripass.saving.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * monthly_category_analyses 테이블 매핑 DTO.
 *
 * categoryCode/categoryName은 실제 테이블 컬럼이 아니라, spending_categories와 JOIN한
 * 조회 쿼리에서만 채워지는 편의 필드다(INSERT 시에는 사용하지 않는다).
 */
@Getter
@Setter
public class MonthlyCategoryAnalysisDto {

    private Long id;
    private Long monthlySpendingAnalysisId;
    private Long categoryId;
    private String categoryCode; // JOIN 조회 전용
    private String categoryName; // JOIN 조회 전용

    private BigDecimal spendingAmount;      // 1일~말일 지출액(소비순위 표시용)
    private BigDecimal spendingRatio;       // 전체 지출 대비 비율(%)
    private Integer transactionCount;       // 1일~말일 거래 횟수
    private BigDecimal weeklyAverage;
    private BigDecimal dailyAverage;
    private BigDecimal previousMonthChange; // 전월 대비 증감률(%)
    private Integer spendingRank;           // 소비 순위(기타 포함)

    private BigDecimal missionPeriodSpending;     // 1~28일 지출액(미션 기준)
    private Integer missionTransactionCount;      // 1~28일 거래 횟수(미션 기준)

    private BigDecimal spendingShareScore;  // 지출 비율 점수(0~1)
    private BigDecimal increaseScore;       // 최근 3개월 대비 증가 점수(0~1)
    private BigDecimal amountRankScore;     // 지출 순위 점수(0~1)
    private BigDecimal recommendationScore; // 최종 추천 점수(0~1)
    private Integer recommendationRank;     // 절감 추천 순위(TOP 3)
    private Boolean recommendationEligible; // 추천 후보 필터 통과 여부
    private String exclusionReason;         // 추천 제외 사유
    private String recommendationReason;    // 추천 선정 근거
    private String coachingMessage;         // AI 코칭 문구

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
