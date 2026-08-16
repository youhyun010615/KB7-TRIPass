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

    /** 이미 월간 미션으로 생성되어 더 이상 수정할 수 없는 선택을 조회한다. */
    List<MissionCategorySelectionDto> findStartedSelections(
            @Param("monthlySpendingAnalysisId") Long monthlySpendingAnalysisId);

    /**
     * 같은 월간 분석에 대한 동시 PUT 요청을 직렬화하기 위해 부모 행에 배타 잠금을 건다.
     * upsertSelection과 deleteSelectionsExcept가 서로 다른 카테고리 행을 각각 먼저 잠그면
     * 교착 상태(deadlock)가 날 수 있는데, 저장을 시작하기 전에 이 잠금을 먼저 걸어두면
     * 동시 요청 중 하나가 먼저 끝날 때까지 나머지가 대기하게 되어 교착이 원천적으로 생기지 않는다.
     */
    void lockMonthlySpendingAnalysis(@Param("monthlySpendingAnalysisId") Long monthlySpendingAnalysisId);

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
