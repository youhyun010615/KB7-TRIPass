package com.tripass.saving.controller;

import com.tripass.common.exception.GlobalExceptionHandler;
import com.tripass.saving.dto.WeeklyMissionEvaluationItemDto;
import com.tripass.saving.dto.WeeklyMissionEvaluationResponseDto;
import com.tripass.saving.service.WeeklyMissionEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeeklyMissionEvaluationControllerMockMvcTest {

    @Mock private WeeklyMissionEvaluationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new WeeklyMissionEvaluationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void evaluateReturnsWeeklyResult() throws Exception {
        when(service.evaluate(3L, YearMonth.of(2026, 8), 1)).thenReturn(response());

        mockMvc.perform(post("/api/v1/saving/missions/2026-08/weeks/1/evaluate")
                        .principal(authentication()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("주간 미션 판정 성공"))
                .andExpect(jsonPath("$.data.weekNumber").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.missions[0].actualSaving").value(10000));

        verify(service).evaluate(3L, YearMonth.of(2026, 8), 1);
    }

    @Test
    void invalidYearMonthReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/saving/missions/2026-13/weeks/1/evaluate")
                        .principal(authentication()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_YEAR_MONTH"));

        verifyNoInteractions(service);
    }

    private Authentication authentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(3L);
        return authentication;
    }

    private WeeklyMissionEvaluationResponseDto response() {
        WeeklyMissionEvaluationItemDto item = new WeeklyMissionEvaluationItemDto(
                11L, 2L, "CAFE", "카페", 30_000, 20_000, 10_000,
                10_000, null, "SUCCESS");
        return new WeeklyMissionEvaluationResponseDto(
                "2026-08", 1, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 7),
                1, 1, 0, List.of(item));
    }
}
