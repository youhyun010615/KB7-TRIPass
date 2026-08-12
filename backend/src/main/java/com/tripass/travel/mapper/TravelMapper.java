package com.tripass.travel.mapper;

import com.tripass.checklist.dto.ChecklistResponseDto;
import com.tripass.checklist.dto.ChecklistSummaryResponseDto;
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
}