package com.tripass.saving.controller;

import com.tripass.common.exception.CustomException;
import com.tripass.common.response.ApiResponse;
import com.tripass.saving.dto.WeeklyMissionEvaluationResponseDto;
import com.tripass.saving.service.WeeklyMissionEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/saving/missions")
@RequiredArgsConstructor
public class WeeklyMissionEvaluationController {

    private final WeeklyMissionEvaluationService evaluationService;

    @PostMapping("/{targetYearMonth}/weeks/{weekNumber}/evaluate")
    public ResponseEntity<ApiResponse<WeeklyMissionEvaluationResponseDto>> evaluate(
            @PathVariable String targetYearMonth,
            @PathVariable int weekNumber,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        WeeklyMissionEvaluationResponseDto data = evaluationService.evaluate(
                userId, parseYearMonth(targetYearMonth), weekNumber);
        return ResponseEntity.ok(ApiResponse.success("주간 미션 판정 성공", data));
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
