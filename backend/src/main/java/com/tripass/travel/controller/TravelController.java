package com.tripass.travel.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.travel.dto.*;
import com.tripass.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;

    /**
     * 0. 여행 등록
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TripCreateResponseDto>> createTrip(
            @Valid @RequestBody TripCreateRequestDto request,
            @AuthenticationPrincipal Long userId) {
        TripCreateResponseDto data = travelService.createTrip(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("여행 등록 성공", data));
    }

    /**
     * 0-1. 로그인 사용자의 현재(가장 최근) 여행 조회
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<TripCurrentResponseDto>> getCurrentTrip(
            @AuthenticationPrincipal Long userId) {
        TripCurrentResponseDto data = travelService.getCurrentTrip(userId);
        return ResponseEntity.ok(ApiResponse.success("현재 여행 조회 성공", data));
    }

    /**
     * 1. 여행 대시보드 상태 조회
     */
    @GetMapping("/{id}/travel-status")
    public ResponseEntity<ApiResponse<TravelStatusResponseDto>> getTravelStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
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
            @AuthenticationPrincipal Long userId) {
        BudgetCheckResponseDto data = travelService.getTripBudget(id, scope, countryId, userId);
        return ResponseEntity.ok(ApiResponse.success("여행 자금 체크 조회 성공", data));
    }
}