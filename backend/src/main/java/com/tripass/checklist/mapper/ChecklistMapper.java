package com.tripass.checklist.mapper;

import com.tripass.checklist.domain.TripChecklistItem;
import com.tripass.checklist.dto.*;
import com.tripass.travel.domain.Trip;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChecklistMapper {

    // 여행 정보 및 작성자 조회 (소유권 검증 및 출발일 확인용)
    Trip selectTripById(@Param("tripId") Long tripId);

    // 체크리스트 전체 요약 통계 조회
    ChecklistSummaryResponseDto selectChecklistSummaryByTripId(@Param("tripId") Long tripId);

    // D-Day 기준 이월 상태 동적 UPDATE
    int updateCarriedOverStatus(@Param("tripId") Long tripId, @Param("daysUntilTrip") long daysUntilTrip);

    // 상세 체크리스트 목록 SELECT
    List<ChecklistResponseDto> selectChecklistsByTripIdAndType(
            @Param("tripId") Long tripId,
            @Param("checklistType") String checklistType,
            @Param("ddayStage") String ddayStage
    );

    // 체크리스트 항목 존재 여부 확인
    boolean existsChecklistItem(@Param("tripId") Long tripId, @Param("itemId") Long itemId);

    // 체크리스트 항목 완료 상태 UPDATE
    int updateChecklistItemCompletion(@Param("itemId") Long itemId, @Param("isCompleted") Boolean isCompleted);

    // 템플릿 복사 (여행 생성 시 활용)
    int insertDefaultChecklistsFromTemplate(@Param("tripId") Long tripId);

    // 커스텀 체크리스트 항목 생성
    int insertChecklistItem(TripChecklistItem item);

    // 특정 체크리스트 항목 단건 조회 (is_custom 여부 확인용)
    TripChecklistItem selectChecklistItemById(@Param("tripId") Long tripId, @Param("itemId") Long itemId);

    // 체크리스트 항목 삭제 (Soft Delete)
    int deleteChecklistItem(@Param("itemId") Long itemId);
}