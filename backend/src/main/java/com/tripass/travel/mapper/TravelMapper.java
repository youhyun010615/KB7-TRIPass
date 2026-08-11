package com.tripass.travel.mapper;

import com.tripass.travel.domain.Trip;
import com.tripass.travel.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TravelMapper {
    TravelStatusResponseDto getTripDashboard(@Param("tripId") Long tripId);
    
    BudgetCheckResponseDto getTripBudget(@Param("tripId") Long tripId, @Param("scope") String scope, @Param("countryId") Long countryId);

    // 여행 존재 여부 확인
    boolean existsByTripId(@Param("tripId") Long tripId);

    // 여행 소유자 ID 조회 (권한 검증용)
    Long selectUserIdByTripId(@Param("tripId") Long tripId);

    // 체크리스트 집계 요약 조회
    ChecklistSummaryResponseDto selectChecklistSummaryByTripId(@Param("tripId") Long tripId);

    // 여행이 생성되는 순간, 기본 제공 체크리스트가 생성이 됨.
    int insertDefaultChecklistsFromTemplate(@Param("tripId") Long tripId);

    // 단계별 체크리스트 상세 목록 조회
    List<ChecklistResponseDto> selectChecklistsByTripIdAndType(
            @Param("tripId") Long tripId,
            @Param("checklistType") String checklistType,
            @Param("ddayStage") String ddayStage
    );

    int updateCarriedOverStatus(
            @Param("tripId") Long tripId,
            @Param("daysUntilTrip") long daysUntilTrip
    );


    Trip selectTripById(@Param("tripId") Long tripId);
}