package com.tripass.travel.service;

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
     * 단계별 체크리스트 상세 목록 조회 (유저 인가 및 이월 항목 그룹화 처리)
     */
    @Transactional
    public ChecklistGroupResponseDto getChecklists(Long tripId, String type, String ddayStage, Long currentUserId) {
        // 1. type 파라미터 유효성 검증
        if (!"PRE_TRAVEL".equals(type) && !"PREV_TRAVEL".equals(type) && !"RETURN".equals(type)) {
            throw new TravelException(TravelErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 체크리스트 type입니다.");
        }

        // 2. 여행 존재 여부 및 소유권 검증 (400, 404, 403)
        Trip trip = validateTripOwnerAndGetTrip(tripId, currentUserId);

        String checklistType = "PREV_TRAVEL".equals(type) ? "PRE_TRAVEL" : type;

        // 2. D-Day 일수 계산 (출발일 - 오늘)
        if ("PRE_TRAVEL".equals(checklistType)) {
            long daysUntilTrip = ChronoUnit.DAYS.between(LocalDate.now(), trip.getStartDate());
            log.info("days until Trip: {}일 남음", daysUntilTrip);

            /*
             * [이월 UPDATE 핵심 판단 기준]
             * - daysUntilTrip <= 7 (D-7 시점 이하 진입): 이미 지난 D-30의 미완료 항목들을 이월(is_carried_over=1) 마킹
             * - daysUntilTrip <= 1 (D-1 시점 이하 진입): 이미 지난 D-30, D-7의 미완료 항목들을 이월(is_carried_over=1) 마킹
             */
            if (daysUntilTrip <= 7) {
                travelMapper.updateCarriedOverStatus(tripId, daysUntilTrip);
            }
        } else if ("RETURN".equals(checklistType)) {
            ddayStage = null;
        }

        // 3. 최신화된 체크리스트 목록 조회
        List<ChecklistResponseDto> allItems = travelMapper.selectChecklistsByTripIdAndType(tripId, checklistType, ddayStage);

        // 4. 이월 항목(밀린 체크리스트)과 현재 단계 본래 항목으로 분리하여 응답
        List<ChecklistResponseDto> carriedOverChecklists = allItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsCarriedOver()))
                .collect(Collectors.toList());

        List<ChecklistResponseDto> currentChecklists = allItems.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsCarriedOver()))
                .collect(Collectors.toList());

        return ChecklistGroupResponseDto.builder()
                .carriedOverChecklists(carriedOverChecklists)
                .currentChecklists(currentChecklists)
                .build();
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

    private Trip validateTripOwnerAndGetTrip(Long tripId, Long currentUserId) {
        // 1. Path Variable 기본 유효성 검증
        if (tripId == null || tripId <= 0) {
            throw new TravelException(TravelErrorCode.INVALID_PATH_VARIABLE);
        }

        // 2. 여행 정보 조회 (start_date 포함)
        Trip trip = travelMapper.selectTripById(tripId);

        // 2-1. 존재하지 않는 여행인 경우 (404 Not Found)
        if (trip == null) {
            throw new TravelException(TravelErrorCode.TRIP_NOT_FOUND);
        }

        // 2-2. 타인의 여행 정보에 접근하려는 경우 (403 Forbidden)
        if (!trip.getUserId().equals(currentUserId)) {
            throw new TravelException(TravelErrorCode.FORBIDDEN_TRIP_ACCESS);
        }

        return trip;
    }
}