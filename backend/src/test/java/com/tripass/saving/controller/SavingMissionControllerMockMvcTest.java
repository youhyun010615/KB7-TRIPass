package com.tripass.saving.controller;

import com.tripass.common.exception.GlobalExceptionHandler;
import com.tripass.saving.dto.MonthlyMissionResponseDto;
import com.tripass.saving.dto.SavingMissionsResponseDto;
import com.tripass.saving.dto.WeeklyMissionResponseDto;
import com.tripass.saving.service.SavingMissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SavingMissionControllerMockMvcTest {

    private static final Long USER_ID = 3L;

    @Mock
    private SavingMissionService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new SavingMissionController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postCreatesMonthlyAndWeeklyMissions() throws Exception {
        when(service.createMissions(USER_ID, YearMonth.of(2026, 8))).thenReturn(response());

        mockMvc.perform(post("/api/v1/saving/missions/2026-08").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.targetYearMonth").value("2026-08"))
                .andExpect(jsonPath("$.data.missionCount").value(1))
                .andExpect(jsonPath("$.data.totalPlannedSavingAmount").value(19500))
                .andExpect(jsonPath("$.data.missions[0].categoryCode").value("CAFE"))
                .andExpect(jsonPath("$.data.missions[0].weeklyMissions[0].weekNumber").value(3))
                .andExpect(jsonPath("$.data.missions[0].weeklyMissions[0].missionMessage")
                        .value("카페 지출을 9,750원 줄이세요."));

        verify(service).createMissions(USER_ID, YearMonth.of(2026, 8));
    }

    @Test
    void getReturnsSavedMissions() throws Exception {
        when(service.getMissions(USER_ID, YearMonth.of(2026, 8))).thenReturn(response());

        mockMvc.perform(get("/api/v1/saving/missions/2026-08").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.missions[0].startWeek").value(3));
    }

    @Test
    void invalidTargetYearMonthReturns400WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/saving/missions/2026-13").principal(authentication()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_YEAR_MONTH"));

        verifyNoInteractions(service);
    }

    private Authentication authentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_ID);
        return authentication;
    }

    private SavingMissionsResponseDto response() {
        WeeklyMissionResponseDto weekly = new WeeklyMissionResponseDto(
                11L, 3, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 21),
                22_750, 9_750, null, null, "PENDING", "카페 지출을 9,750원 줄이세요.");
        MonthlyMissionResponseDto monthly = new MonthlyMissionResponseDto(
                1L, 2L, "CAFE", "카페", 30, 130_000, 39_000, 91_000,
                19_500, 3, "IN_PROGRESS", List.of(weekly));
        return new SavingMissionsResponseDto("2026-08", 1, 19_500L, List.of(monthly));
    }
}
