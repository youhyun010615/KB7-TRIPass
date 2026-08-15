package com.tripass.saving.controller;

import com.tripass.common.exception.GlobalExceptionHandler;
import com.tripass.saving.dto.MissionOptionResponseDto;
import com.tripass.saving.dto.MissionSelectionResponseDto;
import com.tripass.saving.dto.ReductionRateOptionDto;
import com.tripass.saving.service.MissionCategorySelectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.YearMonth;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MissionCategorySelectionControllerTest}와 달리 실제 URL 매핑·HTTP 메서드·JSON 직렬화·
 * 예외 -> HTTP 상태 변환을 MockMvc로 검증한다.
 */
class MissionCategorySelectionControllerMockMvcTest {

    private static final Long USER_ID = 1L;

    @Mock
    private MissionCategorySelectionService missionCategorySelectionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        MissionCategorySelectionController controller =
                new MissionCategorySelectionController(missionCategorySelectionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication authentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_ID);
        return authentication;
    }

    @Test
    @DisplayName("GET /api/v1/saving/analyses/{yearMonth}/mission-options는 옵션 목록을 JSON으로 응답한다")
    void get_missionOptions_returnsJson() throws Exception {
        when(missionCategorySelectionService.getMissionOptions(USER_ID, YearMonth.of(2026, 7)))
                .thenReturn(List.of(new MissionOptionResponseDto(
                        1L, "FOOD", "식비", 100000,
                        List.of(new ReductionRateOptionDto(10, 10000, 90000, 22500, 2500)))));

        mockMvc.perform(get("/api/v1/saving/analyses/2026-07/mission-options").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].categoryCode").value("FOOD"))
                .andExpect(jsonPath("$.data[0].options[0].reductionRate").value(10))
                .andExpect(jsonPath("$.data[0].options[0].monthlyReductionTarget").value(10000));
    }

    @Test
    @DisplayName("PUT /api/v1/saving/analyses/{yearMonth}/mission-selections는 요청 바디를 저장하고 결과를 응답한다")
    void put_missionSelections_savesAndReturnsJson() throws Exception {
        when(missionCategorySelectionService.saveMissionSelections(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(YearMonth.of(2026, 7)),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new MissionSelectionResponseDto(
                        1L, "FOOD", "식비", 30, 100000, 30000, 70000, 17500, 7500)));

        String body = "{\"selections\":[{\"categoryId\":1,\"reductionRate\":30}]}";

        mockMvc.perform(put("/api/v1/saving/analyses/2026-07/mission-selections")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reductionRate").value(30))
                .andExpect(jsonPath("$.data[0].monthlyReductionTarget").value(30000));

        verify(missionCategorySelectionService).saveMissionSelections(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(YearMonth.of(2026, 7)),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("PUT 요청에 selections가 없으면(빈 배열이 아니라 필드 자체 누락) 400을 응답한다")
    void put_missionSelections_missingSelectionsField_returnsHttp400() throws Exception {
        mockMvc.perform(put("/api/v1/saving/analyses/2026-07/mission-selections")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/saving/analyses/{yearMonth}/mission-selections는 저장된 선택을 JSON으로 응답한다")
    void get_missionSelections_returnsJson() throws Exception {
        when(missionCategorySelectionService.getMissionSelections(USER_ID, YearMonth.of(2026, 7)))
                .thenReturn(List.of(new MissionSelectionResponseDto(
                        1L, "FOOD", "식비", 30, 100000, 30000, 70000, 17500, 7500)));

        mockMvc.perform(get("/api/v1/saving/analyses/2026-07/mission-selections").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryCode").value("FOOD"))
                .andExpect(jsonPath("$.data[0].weeklyUsageLimit").value(17500));
    }

    @Test
    @DisplayName("잘못된 yearMonth 형식은 서비스를 호출하지 않고 HTTP 400으로 응답한다")
    void invalidYearMonth_returnsHttp400() throws Exception {
        mockMvc.perform(get("/api/v1/saving/analyses/2026%2F07/mission-options").principal(authentication()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_YEAR_MONTH"));
    }
}
