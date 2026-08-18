package com.tripass.saving.controller;

import com.tripass.saving.dto.SavingReadinessResponseDto;
import com.tripass.saving.dto.SavingReadinessStatus;
import com.tripass.saving.service.SavingReadinessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SavingReadinessControllerMockMvcTest {

    private static final Long USER_ID = 7L;

    @Mock
    private SavingReadinessService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new SavingReadinessController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void returnsStructuredReadinessForCardOnlyUser() throws Exception {
        SavingReadinessResponseDto response = new SavingReadinessResponseDto(
                false,
                false,
                true,
                true,
                false,
                SavingReadinessStatus.TRAVEL_GOAL_REQUIRED
        );
        when(service.getReadiness(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/saving/readiness").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.travelGoalRegistered").value(false))
                .andExpect(jsonPath("$.data.accountLinked").value(false))
                .andExpect(jsonPath("$.data.cardLinked").value(true))
                .andExpect(jsonPath("$.data.financialAssetLinked").value(true))
                .andExpect(jsonPath("$.data.missionPrerequisitesMet").value(false))
                .andExpect(jsonPath("$.data.status").value("TRAVEL_GOAL_REQUIRED"));

        verify(service).getReadiness(USER_ID);
    }

    private Authentication authentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_ID);
        return authentication;
    }
}
