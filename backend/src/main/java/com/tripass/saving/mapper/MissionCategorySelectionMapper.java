package com.tripass.saving.mapper;

import com.tripass.saving.dto.MissionCategorySelectionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * mission_category_selections의 원본 조회와 저장만 담당한다.
 * TOP 3 여부·절감률 유효성·소유권 검증은 Service 계층의 책임이다.
 */
@Mapper
public interface MissionCategorySelectionMapper {

    /** 특정 월간 분석에 저장된 선택 결과를 카테고리 정보와 함께 조회한다. */
    List<MissionCategorySelectionDto> findSelections(
            @Param("monthlySpendingAnalysisId") Long monthlySpendingAnalysisId);

    /**
     * 요청에 포함되지 않은 기존 선택을 삭제한다(선택 해제). categoryIds가 비어 있으면
     * 해당 월간 분석의 선택을 전부 삭제한다("빈 배열 = 전체 선택 해제").
     */
    void deleteSelectionsExcept(
            @Param("monthlySpendingAnalysisId") Long monthlySpendingAnalysisId,
            @Param("categoryIds") List<Long> categoryIds);

    /**
     * (monthly_spending_analysis_id, category_id) 유니크 키 기준으로 upsert한다.
     * 이미 선택된 카테고리를 다시 선택하면 created_at은 유지되고 값만 갱신된다.
     */
    void upsertSelection(MissionCategorySelectionDto dto);
}
