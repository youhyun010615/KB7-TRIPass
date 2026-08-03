package com.tripass.travel.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.travel.dto.TravelStatusResponseDto;
import com.tripass.travel.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
