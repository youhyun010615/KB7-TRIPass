package com.tripass.travel.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.travel.dto.BudgetCheckResponseDto;
import com.tripass.travel.dto.ChecklistResponseDto;
import com.tripass.travel.dto.ChecklistSummaryResponseDto;
import com.tripass.travel.dto.TravelStatusResponseDto;
import com.tripass.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Security / Custom User Annotation
import org.springframework.web.bind.annotation.*;

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

    /**
     * 3. 여행 체크리스트 전체 현황 조회
     */
    @GetMapping("/{id}/checklists/summary")
    public ResponseEntity<ApiResponse<ChecklistSummaryResponseDto>> getChecklistSummary(
            @PathVariable Long id, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        ChecklistSummaryResponseDto data = travelService.getChecklistSummary(id, userId);
        return ResponseEntity.ok(ApiResponse.success("체크리스트 요약 조회 성공", data));
    }

    /**
     * 단계별 체크리스트 상세 목록 조회
     */
    @GetMapping("/{id}/checklists")
    public ResponseEntity<ApiResponse<List<ChecklistResponseDto>>> getChecklists(
            @PathVariable Long id,
            @RequestParam String type,
            @RequestParam(required = false) String ddayStage,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        List<ChecklistResponseDto> data = travelService.getChecklists(id, type, ddayStage, userId);
        return ResponseEntity.ok(ApiResponse.success("체크리스트 상세 목록 조회 성공", data));
    }

}