package com.tripass.saving.dto;

/**
 * 절감 추천 TOP 3 항목 — 지난달 1~28일 기준, 기타 제외 6개 카테고리 중 필터·점수화 결과.
 *
 * 리포트 화면에서는 지출액이 이미 소비 순위 영역에 표시되므로 여기서는 반복하지 않는다
 * (금액·점수는 내부 산정용으로만 저장하고, 응답에는 노출하지 않는다).
 */
public record RecommendedCategoryResponseDto(
        Integer rank,
        String categoryCode,
        String categoryName,
        String recommendationReason,
        String coachingMessage
) {
}
