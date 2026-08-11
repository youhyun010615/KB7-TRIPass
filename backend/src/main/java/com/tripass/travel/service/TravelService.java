package com.tripass.travel.service;

import com.tripass.travel.dto.BudgetCheckResponseDto;
import com.tripass.travel.dto.ChecklistResponseDto;
import com.tripass.travel.dto.ChecklistSummaryResponseDto;
import com.tripass.travel.dto.TravelStatusResponseDto;
import com.tripass.travel.exception.TravelErrorCode;
import com.tripass.travel.exception.TravelException;
import com.tripass.travel.mapper.TravelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
     * 단계별 체크리스트 상세 목록 조회
     */
    public List<ChecklistResponseDto> getChecklists(Long tripId, String type, String ddayStage, Long currentUserId) {
        // 1. type 파라미터 유효성 검증 (PRE_TRAVEL, RETURN 외의 값일 경우 400 에러)
        if (!"PRE_TRAVEL".equals(type) && !"PREV_TRAVEL".equals(type) && !"RETURN".equals(type)) {
            throw new TravelException(TravelErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 체크리스트 type입니다. (PRE_TRAVEL / RETURN)");
        }

        if ("RETURN".equals(type) && ddayStage != null && !ddayStage.isBlank()) {
            throw new TravelException(TravelErrorCode.INVALID_INPUT_VALUE, "RETURN 타입 조회 시 ddayStage 파라미터를 넘길 수 없습니다.");
        }

        // 2. 여행 존재 여부 및 소유권 검증 (400, 404, 403)
        validateTripOwner(tripId, currentUserId);

        // 3. 타입 명세 호환성 처리 (PREV_TRAVEL 들어올 경우 DB 저장 규격인 PRE_TRAVEL로 변경)
        String checklistType = "PREV_TRAVEL".equals(type) ? "PRE_TRAVEL" : type;

        // 4. 체크리스트 상세 목록 DB 조회
        return travelMapper.selectChecklistsByTripIdAndType(tripId, checklistType, ddayStage);
    }

    /**
     * 여행 체크리스트 전체 현황 요약 조회
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