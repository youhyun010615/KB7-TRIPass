package com.tripass.saving.controller;

import com.tripass.common.exception.CustomException;
import com.tripass.common.response.ApiResponse;
import com.tripass.saving.dto.SavingMissionsResponseDto;
import com.tripass.saving.service.SavingMissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/saving/missions")
@RequiredArgsConstructor
public class SavingMissionController {

    private final SavingMissionService savingMissionService;

    @PostMapping("/{targetYearMonth}")
    public ResponseEntity<ApiResponse<SavingMissionsResponseDto>> createMissions(
            @PathVariable String targetYearMonth,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        SavingMissionService.CreationResult result =
                savingMissionService.createMissions(userId, parseYearMonth(targetYearMonth));
        ApiResponse<SavingMissionsResponseDto> response = ApiResponse.success(
                result.created() ? "월간·주간 미션 생성 성공" : "이미 생성된 월간·주간 미션 조회 성공",
                result.data());
        return result.created()
                ? ResponseEntity.status(HttpStatus.CREATED).body(response)
                : ResponseEntity.ok(response);
    }

    @GetMapping("/{targetYearMonth}")
    public ResponseEntity<ApiResponse<SavingMissionsResponseDto>> getMissions(
            @PathVariable String targetYearMonth,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        SavingMissionsResponseDto data =
                savingMissionService.getMissions(userId, parseYearMonth(targetYearMonth));
        return ResponseEntity.ok(ApiResponse.success("월간·주간 미션 조회 성공", data));
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_YEAR_MONTH",
                    "targetYearMonth 형식이 올바르지 않습니다. 예: 2026-08");
        }
    }
}
