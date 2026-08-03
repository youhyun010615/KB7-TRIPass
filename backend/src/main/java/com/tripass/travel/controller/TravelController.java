package com.tripass.travel.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.travel.dto.BudgetCheckResponseDto;
import com.tripass.travel.dto.TravelStatusResponseDto;
import com.tripass.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TravelController {
    private final TravelService travelService;

    @GetMapping("/{id}/travel-status")
    public ResponseEntity<ApiResponse<TravelStatusResponseDto>> getTravelStatus(@PathVariable Long id) {
        TravelStatusResponseDto data = travelService.getTravelStatus(id);
        return ResponseEntity.ok(ApiResponse.success("여행 대시보드 조회 성공", data));
    }

    @GetMapping("/{id}/budget-check")
    public ResponseEntity<ApiResponse<BudgetCheckResponseDto>> getTripBudget(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "ALL") String scope,
            @RequestParam(required = false) Long countryId) {
        BudgetCheckResponseDto data = travelService.getTripBudget(id, scope, countryId);
        return ResponseEntity.ok(ApiResponse.success("여행 자금 체크 조회 성공", data));
    }
}
