package com.tripass.saving.controller;

import com.tripass.common.exception.CustomException;
import com.tripass.common.response.ApiResponse;
import com.tripass.saving.dto.MonthlyAnalysisResponseDto;
import com.tripass.saving.dto.SavingResultResponseDto;
import com.tripass.saving.dto.SavingResultStatus;
import com.tripass.saving.service.MonthlySpendingAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonthlySpendingAnalysisControllerTest {

    private static final Long USER_ID = 1L;

    @Mock
    private MonthlySpendingAnalysisService monthlySpendingAnalysisService;

    @InjectMocks
    private MonthlySpendingAnalysisController controller;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_ID);
    }

    @Test
    @DisplayName("리포트 생성 요청은 서비스 결과를 그대로 응답한다")
    void generateMonthlyAnalysis_returnsServiceResult() {
        MonthlyAnalysisResponseDto responseDto = sampleResponse();
        when(monthlySpendingAnalysisService.generateMonthlyAnalysis(USER_ID, YearMonth.of(2026, 7)))
                .thenReturn(responseDto);

        ResponseEntity<ApiResponse<MonthlyAnalysisResponseDto>> response =
                controller.generateMonthlyAnalysis("2026-07", authentication);

        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getCode());
        assertEquals(responseDto, response.getBody().getData());
    }

    @Test
    @DisplayName("리포트 조회 요청은 서비스 결과를 그대로 응답한다")
    void getMonthlyAnalysis_returnsServiceResult() {
        MonthlyAnalysisResponseDto responseDto = sampleResponse();
        when(monthlySpendingAnalysisService.getMonthlyAnalysis(USER_ID, YearMonth.of(2026, 7)))
                .thenReturn(responseDto);

        ResponseEntity<ApiResponse<MonthlyAnalysisResponseDto>> response =
                controller.getMonthlyAnalysis("2026-07", authentication);

        assertEquals(responseDto, response.getBody().getData());
    }

    @Test
    @DisplayName("확인 처리 요청은 인증된 사용자 ID와 파싱된 연월로 서비스를 호출한다")
    void markReportViewed_callsServiceWithParsedYearMonth() {
        ResponseEntity<ApiResponse<Void>> response = controller.markReportViewed("2026-07", authentication);

        verify(monthlySpendingAnalysisService).markReportViewed(USER_ID, YearMonth.of(2026, 7));
        assertEquals("SUCCESS", response.getBody().getCode());
    }

    @Test
    @DisplayName("종료 처리 요청은 인증된 사용자 ID와 파싱된 연월로 서비스를 호출한다")
    void markReportClosed_callsServiceWithParsedYearMonth() {
        ResponseEntity<ApiResponse<Void>> response = controller.markReportClosed("2026-07", authentication);

        verify(monthlySpendingAnalysisService).markReportClosed(USER_ID, YearMonth.of(2026, 7));
        assertEquals("SUCCESS", response.getBody().getCode());
    }

    @Test
    @DisplayName("yearMonth 형식이 올바르지 않으면 400 예외를 던지고 서비스는 호출하지 않는다")
    void invalidYearMonth_throwsBadRequestAndDoesNotCallService() {
        CustomException exception = assertThrows(CustomException.class,
                () -> controller.getMonthlyAnalysis("2026/07", authentication));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("INVALID_YEAR_MONTH", exception.getErrorCode());
    }

    private MonthlyAnalysisResponseDto sampleResponse() {
        return new MonthlyAnalysisResponseDto(
                "2026-07",
                "2026-08",
                "PENDING",
                new BigDecimal("300000"),
                new SavingResultResponseDto(
                        SavingResultStatus.UNAVAILABLE, new BigDecimal("700000"), null, null, "아직 집계할 수 있는 저축 내역이 없어요."),
                List.of(),
                "이번 달 분석할 절감 추천 카테고리가 없어요.",
                List.of()
        );
    }
}
