package com.tripass.saving.controller;

import com.tripass.common.exception.CustomException;
import com.tripass.common.response.ApiResponse;
import com.tripass.saving.dto.MissionDetailResponseDto;
import com.tripass.saving.dto.MonthlyAnalysisResponseDto;
import com.tripass.saving.service.MonthlySpendingAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/saving/analyses")
@RequiredArgsConstructor
public class MonthlySpendingAnalysisController {

    private final MonthlySpendingAnalysisService monthlySpendingAnalysisService;

    private Long getAuthenticatedUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_YEAR_MONTH",
                    "yearMonth 형식이 올바르지 않습니다. 예: 2026-07");
        }
    }

    /**
     * 분석월 리포트를 계산해 저장한다. 기존 리포트가 있으면 집계값만 재계산한다.
     * 최초 생성·재계산을 이 엔드포인트 하나가 함께 처리하는 upsert 성격이라, 리소스가 새로 생성됐는지
     * 여부와 무관하게 항상 200 OK로 응답한다(순수 생성 전용 엔드포인트가 아니므로 201은 쓰지 않는다).
     */
    @PostMapping("/{yearMonth}")
    public ResponseEntity<ApiResponse<MonthlyAnalysisResponseDto>> generateMonthlyAnalysis(
            @PathVariable String yearMonth,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        MonthlyAnalysisResponseDto data =
                monthlySpendingAnalysisService.generateMonthlyAnalysis(userId, parseYearMonth(yearMonth));
        return ResponseEntity.ok(ApiResponse.success("월간 분석 리포트 생성 성공", data));
    }

    /** 저장된 분석월 리포트를 조회한다. */
    @GetMapping("/{yearMonth}")
    public ResponseEntity<ApiResponse<MonthlyAnalysisResponseDto>> getMonthlyAnalysis(
            @PathVariable String yearMonth,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        MonthlyAnalysisResponseDto data =
                monthlySpendingAnalysisService.getMonthlyAnalysis(userId, parseYearMonth(yearMonth));
        return ResponseEntity.ok(ApiResponse.success("월간 분석 리포트 조회 성공", data));
    }

    @GetMapping("/{yearMonth}/categories/{categoryCode}")
    public ResponseEntity<ApiResponse<MissionDetailResponseDto>> getMissionDetail(
            @PathVariable String yearMonth,
            @PathVariable String categoryCode,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        MissionDetailResponseDto data =
                monthlySpendingAnalysisService.getMissionDetail(userId, parseYearMonth(yearMonth), categoryCode);
        return ResponseEntity.ok(ApiResponse.success("카테고리 상세 분석 조회 성공", data));
    }

    /** 리포트를 확인 처리한다(PENDING -> VIEWED). 이미 VIEWED/CLOSED면 상태를 유지한다. */
    @PatchMapping("/{yearMonth}/view")
    public ResponseEntity<ApiResponse<Void>> markReportViewed(
            @PathVariable String yearMonth,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        monthlySpendingAnalysisService.markReportViewed(userId, parseYearMonth(yearMonth));
        return ResponseEntity.ok(ApiResponse.success("리포트 확인 처리 성공", null));
    }

    /** 리포트를 종료 처리한다(PENDING/VIEWED -> CLOSED). 이미 CLOSED면 상태를 유지한다. */
    @PatchMapping("/{yearMonth}/close")
    public ResponseEntity<ApiResponse<Void>> markReportClosed(
            @PathVariable String yearMonth,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        monthlySpendingAnalysisService.markReportClosed(userId, parseYearMonth(yearMonth));
        return ResponseEntity.ok(ApiResponse.success("리포트 종료 처리 성공", null));
    }
}
