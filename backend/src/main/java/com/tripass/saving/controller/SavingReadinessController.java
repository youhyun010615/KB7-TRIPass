package com.tripass.saving.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.saving.dto.SavingReadinessResponseDto;
import com.tripass.saving.service.SavingReadinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saving/readiness")
@RequiredArgsConstructor
public class SavingReadinessController {

    private final SavingReadinessService savingReadinessService;

    @GetMapping
    public ResponseEntity<ApiResponse<SavingReadinessResponseDto>> getReadiness(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        SavingReadinessResponseDto data = savingReadinessService.getReadiness(userId);
        return ResponseEntity.ok(ApiResponse.success("저축 미션 준비 상태 조회 성공", data));
    }
}
