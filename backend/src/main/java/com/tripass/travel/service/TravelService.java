package com.tripass.travel.service;

import com.tripass.travel.dto.BudgetCheckResponseDto;
import com.tripass.travel.dto.ChecklistSummaryResponseDto;
import com.tripass.travel.dto.TravelStatusResponseDto;
import com.tripass.travel.exception.TravelErrorCode;
import com.tripass.travel.exception.TravelException;
import com.tripass.travel.mapper.TravelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelService {

    private final TravelMapper travelMapper;

    /**
     * 여행 대시보드 상태 조회 (유저 인가 검증)
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
     * 여행 자금 체크 조회 (유저 인가 검증)
     */
    public BudgetCheckResponseDto getTripBudget(Long tripId, String scope, Long countryId, Long currentUserId) {
        if ("COUNTRY".equals(scope) && countryId == null) {
            throw new TravelException(TravelErrorCode.MISSING_COUNTRY_ID);
        }

        validateTripOwner(tripId, currentUserId);

        return travelMapper.getTripBudget(tripId, scope, countryId);
    }

    /**
     * 여행 체크리스트 전체 현황 요약 조회 (유저 인가 검증)
     */
    public ChecklistSummaryResponseDto getChecklistSummary(Long tripId, Long currentUserId) {
        // 1. 여행 존재 여부 및 본인 소유 권한 검증 (400, 404, 403)
        validateTripOwner(tripId, currentUserId);

        // 2. 체크리스트 통계 정보 DB 조회
        ChecklistSummaryResponseDto summary = travelMapper.selectChecklistSummaryByTripId(tripId);

        // 3. 등록된 체크리스트 항목이 하나도 없는 경우 0으로 기본값 반환
        if (summary == null || summary.getTotalItemCount() == 0) {
            return ChecklistSummaryResponseDto.builder()
                    .totalCompletedCount(0)
                    .totalItemCount(0)
                    .prepCompletedCount(0)
                    .prepItemCount(0)
                    .returnCompletedCount(0)
                    .returnItemCount(0)
                    .build();
        }

        return summary;
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