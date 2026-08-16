package com.tripass.saving.controller;

import com.tripass.common.exception.GlobalExceptionHandler;
import com.tripass.saving.dto.MonthlyAnalysisResponseDto;
import com.tripass.saving.dto.SavingResultResponseDto;
import com.tripass.saving.dto.SavingResultStatus;
import com.tripass.saving.service.MonthlySpendingAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러 메서드를 직접 호출하는 {@link MonthlySpendingAnalysisControllerTest}와 달리,
 * 실제 URL 매핑·HTTP 메서드·JSON 직렬화·예외 -> HTTP 상태 변환을 MockMvc로 검증한다.
 */
class MonthlySpendingAnalysisControllerMockMvcTest {

    private static final Long USER_ID = 1L;

    @Mock
    private MonthlySpendingAnalysisService monthlySpendingAnalysisService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        MonthlySpendingAnalysisController controller = new MonthlySpendingAnalysisController(monthlySpendingAnalysisService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    private Authentication authentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_ID);
        return authentication;
    }

    @Test
    @DisplayName("POST /api/v1/saving/analyses/{yearMonth}는 리포트를 생성하고 JSON으로 응답한다")
    void post_generatesReport() throws Exception {
        when(monthlySpendingAnalysisService.generateMonthlyAnalysis(USER_ID, YearMonth.of(2026, 7)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/saving/analyses/2026-07").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.analysisYearMonth").value("2026-07"))
                .andExpect(jsonPath("$.data.savingResult.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data.totalSpending").value(300000));
    }

    @Test
    @DisplayName("GET /api/v1/saving/analyses/{yearMonth}는 저장된 리포트를 JSON으로 응답한다")
    void get_returnsReport() throws Exception {
        when(monthlySpendingAnalysisService.getMonthlyAnalysis(USER_ID, YearMonth.of(2026, 7)))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/saving/analyses/2026-07").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportStatus").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /api/v1/saving/analyses/{yearMonth}/view는 확인 처리를 위임한다")
    void patch_view_marksReportViewed() throws Exception {
        mockMvc.perform(patch("/api/v1/saving/analyses/2026-07/view").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(monthlySpendingAnalysisService).markReportViewed(USER_ID, YearMonth.of(2026, 7));
    }

    @Test
    @DisplayName("PATCH /api/v1/saving/analyses/{yearMonth}/close는 종료 처리를 위임한다")
    void patch_close_marksReportClosed() throws Exception {
        mockMvc.perform(patch("/api/v1/saving/analyses/2026-07/close").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(monthlySpendingAnalysisService).markReportClosed(USER_ID, YearMonth.of(2026, 7));
    }

    @Test
    @DisplayName("잘못된 yearMonth 형식은 서비스를 호출하지 않고 HTTP 400으로 응답한다")
    void invalidYearMonth_returnsHttp400() throws Exception {
        mockMvc.perform(get("/api/v1/saving/analyses/2026%2F07").principal(authentication()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_YEAR_MONTH"));

        verify(monthlySpendingAnalysisService, org.mockito.Mockito.never())
                .getMonthlyAnalysis(any(), any());
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
