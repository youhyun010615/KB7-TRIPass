package com.tripass.schedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tripass.common.exception.GlobalExceptionHandler;
import com.tripass.schedule.dto.ScheduleListResponseDto;
import com.tripass.schedule.exception.ScheduleException;
import com.tripass.schedule.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static com.tripass.schedule.exception.ScheduleErrorCode.INVALID_TRIP_ID;
import static com.tripass.schedule.exception.ScheduleErrorCode.TRIP_NOT_FOUND;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private ScheduleService scheduleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(
                        objectMapper
                );

        ScheduleController controller =
                new ScheduleController(scheduleService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void 여행_일정_목록을_조회한다() throws Exception {
        ScheduleListResponseDto schedule =
                ScheduleListResponseDto.builder()
                        .id(10L)
                        .tripId(1L)
                        .tripCountryId(2L)
                        .countryName("프랑스")
                        .timeZone("Europe/Paris")
                        .scheduleName("루브르 박물관")
                        .startAt(
                                OffsetDateTime.parse(
                                        "2026-08-28T10:30:00+02:00"
                                )
                        )
                        .endAt(
                                OffsetDateTime.parse(
                                        "2026-08-28T11:30:00+02:00"
                                )
                        )
                        .amount(new BigDecimal("85.00"))
                        .currencyCode("EUR")
                        .currencySymbol("€")
                        .paymentStatus("PREPAID")
                        .scheduleStatus("UPCOMING")
                        .placeName("루브르 박물관")
                        .placeAddress("Rue de Rivoli, Paris")
                        .build();

        when(scheduleService.getSchedules(1L))
                .thenReturn(List.of(schedule));

        mockMvc.perform(
                        get(
                                "/api/v1/trips/{tripId}/schedules",
                                1L
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("SUCCESS")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("여행 일정 목록 조회 성공")
                )
                .andExpect(
                        jsonPath("$.data[0].id")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.data[0].tripId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.data[0].scheduleName")
                                .value("루브르 박물관")
                )
                .andExpect(
                        jsonPath("$.data[0].startAt")
                                .value(
                                        "2026-08-28T10:30:00+02:00"
                                )
                )
                .andExpect(
                        jsonPath("$.data[0].endAt")
                                .value(
                                        "2026-08-28T11:30:00+02:00"
                                )
                )
                .andExpect(
                        jsonPath("$.data[0].timeZone")
                                .value("Europe/Paris")
                )
                .andExpect(
                        jsonPath("$.data[0].currencyCode")
                                .value("EUR")
                );
    }

    @Test
    void 일정이_없으면_빈_배열을_반환한다()
            throws Exception {
        when(scheduleService.getSchedules(1L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(
                        get(
                                "/api/v1/trips/{tripId}/schedules",
                                1L
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("SUCCESS")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("여행 일정 목록 조회 성공")
                )
                .andExpect(
                        jsonPath("$.data")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.data")
                                .isEmpty()
                );
    }

    @Test
    void 여행_ID가_0이면_400을_반환한다()
            throws Exception {
        when(scheduleService.getSchedules(0L))
                .thenThrow(
                        new ScheduleException(
                                INVALID_TRIP_ID
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/trips/{tripId}/schedules",
                                0L
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_TRIP_ID")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("잘못된 여행 ID입니다.")
                );
    }

    @Test
    void 여행이_없으면_404를_반환한다()
            throws Exception {
        when(scheduleService.getSchedules(99L))
                .thenThrow(
                        new ScheduleException(
                                TRIP_NOT_FOUND
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/trips/{tripId}/schedules",
                                99L
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("TRIP_NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "여행 정보를 찾을 수 없습니다."
                                )
                );
    }
}