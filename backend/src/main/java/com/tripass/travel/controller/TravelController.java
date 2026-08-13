package com.tripass.travel.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.travel.dto.*;
import com.tripass.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Security / Custom User Annotation
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;

    private Long getAuthenticatedUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /**
     * 1. 여행 대시보드 상태 조회
     */
    @GetMapping("/{id}/travel-status")
    public ResponseEntity<ApiResponse<TravelStatusResponseDto>> getTravelStatus(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        TravelStatusResponseDto data = travelService.getTravelStatus(id, userId);
        return ResponseEntity.ok(ApiResponse.success("여행 대시보드 조회 성공", data));
    }

    /**
     * 2. 여행 자금 체크 조회
     */
    @GetMapping("/{id}/budget-check")
    public ResponseEntity<ApiResponse<BudgetCheckResponseDto>> getTripBudget(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "ALL") String scope,
            @RequestParam(required = false) Long countryId,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);


        BudgetCheckResponseDto data = travelService.getTripBudget(id, scope, countryId, userId);
        return ResponseEntity.ok(ApiResponse.success("여행 자금 체크 조회 성공", data));
    }

    /** 여행 목표 등록: 여행명, 방문 국가 순서, 국가별 일정을 먼저 저장합니다. */
    @PostMapping
    public ResponseEntity<ApiResponse<TripGoalCreateResponseDto>> createTripGoal(
            @Valid @RequestBody TripGoalCreateRequestDto request,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        TripGoalCreateResponseDto data = travelService.createTripGoal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("여행 목표 등록 성공", data));
    }

    /** 로그인 사용자의 진행 중 여행 목표를 조회합니다. */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<TripGoalResponseDto>> getActiveTripGoal(
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        TripGoalResponseDto data = travelService.getActiveTripGoal(userId);
        return ResponseEntity.ok(ApiResponse.success("진행 중인 여행 목표 조회 성공", data));
    }

    /** 국가·통화 선택 모달용 국가 목록을 검색합니다. */
    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<List<TripCountryCatalogResponseDto>>> getCountries(
            @RequestParam(required = false) String keyword
    ) {
        List<TripCountryCatalogResponseDto> data = travelService.getCountries(keyword);
        return ResponseEntity.ok(ApiResponse.success("여행 국가 목록 조회 성공", data));
    }

    /** 여행 시작 전, 여행명과 국가 방문 순서·일정을 수정합니다. */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TripGoalResponseDto>> updateTripGoal(
            @PathVariable("id") Long tripId,
            @Valid @RequestBody TripGoalUpdateRequestDto request,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        TripGoalResponseDto data = travelService.updateTripGoal(tripId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("여행 목표 수정 성공", data));
    }

    /** 특정 여행 목표를 조회합니다. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripGoalResponseDto>> getTripGoal(
            @PathVariable("id") Long tripId,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        TripGoalResponseDto data = travelService.getTripGoal(tripId, userId);
        return ResponseEntity.ok(ApiResponse.success("여행 목표 조회 성공", data));
    }
}
