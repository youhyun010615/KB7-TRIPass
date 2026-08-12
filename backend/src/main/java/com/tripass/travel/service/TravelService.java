package com.tripass.travel.service;

import com.tripass.checklist.dto.ChecklistGroupResponseDto;
import com.tripass.checklist.dto.ChecklistResponseDto;
import com.tripass.checklist.dto.ChecklistSummaryResponseDto;
import com.tripass.travel.domain.Trip;
import com.tripass.travel.dto.*;
import com.tripass.travel.exception.TravelErrorCode;
import com.tripass.travel.exception.TravelException;
import com.tripass.travel.mapper.TravelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelService {

    private final TravelMapper travelMapper;

    /**
     * 여행 대시보드 상태 조회
     */
    public TravelStatusResponseDto getTravelStatus(Long tripId, Long currentUserId) {
        validateTripOwner(tripId, currentUserId);

        TravelStatusResponseDto result = travelMapper.getTripDashboard(tripId);
        if (result == null) {
            throw new TravelException(TravelErrorCode.TRIP_NOT_FOUND, "해당 여행 정보를 찾을 수 없습니다. (id: " + tripId + ")");
        }
        return result;
    }

    /**
     * 여행 자금 체크 조회
     */
    public BudgetCheckResponseDto getTripBudget(Long tripId, String scope, Long countryId, Long currentUserId) {
        if ("COUNTRY".equals(scope) && countryId == null) {
            throw new TravelException(TravelErrorCode.MISSING_COUNTRY_ID);
        }

        validateTripOwner(tripId, currentUserId);

        return travelMapper.getTripBudget(tripId, scope, countryId);
    }

    /**
     * 공통 여행 ID 유효성 & 작성자(소유자) 권한 검증 메서드
     */
    private void validateTripOwner(Long tripId, Long currentUserId) {
        // 1. Path Variable 기본 유효성 검증
        if (tripId == null || tripId <= 0) {
            throw new TravelException(TravelErrorCode.INVALID_PATH_VARIABLE);
        }

        // 2. 여행 소유자(user_id) DB 조회
        Long ownerId = travelMapper.selectUserIdByTripId(tripId);

        // 2-1. 존재하지 않는 여행인 경우 (404 Not Found)
        if (ownerId == null) {
            throw new TravelException(TravelErrorCode.TRIP_NOT_FOUND);
        }

        // 2-2. 타인의 여행 정보에 접근하려는 경우 (403 Forbidden)
        if (!ownerId.equals(currentUserId)) {
            throw new TravelException(TravelErrorCode.FORBIDDEN_TRIP_ACCESS);
        }
    }

}