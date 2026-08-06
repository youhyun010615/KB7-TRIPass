package com.tripass.schedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tripass.common.exception.GlobalExceptionHandler;
import com.tripass.schedule.dto.ScheduleCreateRequestDto;
import com.tripass.schedule.dto.ScheduleCreateResponseDto;
import com.tripass.schedule.dto.ScheduleDetailResponseDto;
import com.tripass.schedule.dto.ScheduleListResponseDto;
import com.tripass.schedule.enums.SchedulePaymentStatus;
import com.tripass.schedule.enums.ScheduleStatus;
import com.tripass.schedule.exception.ScheduleException;
import com.tripass.schedule.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static com.tripass.schedule.exception.ScheduleErrorCode.SCHEDULE_NOT_FOUND;
import static com.tripass.schedule.exception.ScheduleErrorCode.TRIP_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private ScheduleService scheduleService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(
                        objectMapper
                );

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        ScheduleController controller =
                new ScheduleController(scheduleService);

        MethodValidationInterceptor validationInterceptor =
                new MethodValidationInterceptor();

        ProxyFactory proxyFactory =
                new ProxyFactory(controller);

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(validationInterceptor);

        ScheduleController proxiedController =
                (ScheduleController) proxyFactory.getProxy();

        mockMvc = MockMvcBuilders
                .standaloneSetup(proxiedController)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .setMessageConverters(converter)
                .setValidator(validator)
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
                        .scheduledAt(
                                OffsetDateTime.parse(
                                        "2026-08-28T10:30:00+02:00"
                                )
                        )
                        .amount(new BigDecimal("85.00"))
                        .currencyCode("EUR")
                        .currencySymbol("€")
                        .paymentStatus(
                                SchedulePaymentStatus.PREPAID
                        )
                        .scheduleStatus(
                                ScheduleStatus.UPCOMING
                        )
                        .placeName("루브르 박물관")
                        .placeAddress(
                                "Rue de Rivoli, Paris"
                        )
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
                        jsonPath("$.data[0].scheduleName")
                                .value("루브르 박물관")
                )
                .andExpect(
                        jsonPath("$.data[0].scheduledAt")
                                .value(
                                        "2026-08-28T10:30:00+02:00"
                                )
                )
                .andExpect(
                        jsonPath("$.data[0].paymentStatus")
                                .value("PREPAID")
                )
                .andExpect(
                        jsonPath("$.data[0].scheduleStatus")
                                .value("UPCOMING")
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
        mockMvc.perform(
                        get(
                                "/api/v1/trips/{tripId}/schedules",
                                0L
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_INPUT")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "여행 ID는 양수여야 합니다."
                                )
                );

        verifyNoInteractions(scheduleService);
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

    @Test
    void 여행_일정_상세를_조회한다()
            throws Exception {
        ScheduleDetailResponseDto response =
                ScheduleDetailResponseDto.builder()
                        .id(2L)
                        .tripId(1L)
                        .tripCountryId(1L)
                        .countryName("프랑스")
                        .timeZone("Europe/Paris")
                        .scheduleName(
                                "루브르 박물관 가이드 투어"
                        )
                        .scheduledAt(
                                OffsetDateTime.parse(
                                        "2026-08-28T10:30:00+02:00"
                                )
                        )
                        .amount(new BigDecimal("85.00"))
                        .currencyCode("EUR")
                        .currencySymbol("€")
                        .paymentStatus(
                                SchedulePaymentStatus.PREPAID
                        )
                        .scheduleStatus(
                                ScheduleStatus.UPCOMING
                        )
                        .placeName("루브르 박물관")
                        .placeAddress(
                                "Rue de Rivoli, Paris"
                        )
                        .memo("입장 10분 전까지 도착")
                        .build();

        when(
                scheduleService.getScheduleDetail(
                        1L,
                        2L
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/v1/trips/{tripId}/schedules/{scheduleId}",
                                1L,
                                2L
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("SUCCESS")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "여행 일정 상세 조회 성공"
                                )
                )
                .andExpect(
                        jsonPath("$.data.id")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.data.scheduleName")
                                .value(
                                        "루브르 박물관 가이드 투어"
                                )
                )
                .andExpect(
                        jsonPath("$.data.scheduledAt")
                                .value(
                                        "2026-08-28T10:30:00+02:00"
                                )
                )
                .andExpect(
                        jsonPath("$.data.paymentStatus")
                                .value("PREPAID")
                )
                .andExpect(
                        jsonPath("$.data.scheduleStatus")
                                .value("UPCOMING")
                )
                .andExpect(
                        jsonPath("$.data.memo")
                                .value(
                                        "입장 10분 전까지 도착"
                                )
                );
    }

    @Test
    void 여행_일정_ID가_0이면_400을_반환한다()
            throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/trips/{tripId}/schedules/{scheduleId}",
                                1L,
                                0L
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_INPUT")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "여행 일정 ID는 양수여야 합니다."
                                )
                );

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 여행_일정이_없으면_404를_반환한다()
            throws Exception {
        when(
                scheduleService.getScheduleDetail(
                        1L,
                        999L
                )
        ).thenThrow(
                new ScheduleException(
                        SCHEDULE_NOT_FOUND
                )
        );

        mockMvc.perform(
                        get(
                                "/api/v1/trips/{tripId}/schedules/{scheduleId}",
                                1L,
                                999L
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("SCHEDULE_NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "여행 일정을 찾을 수 없습니다."
                                )
                );
    }

    @Test
    void 여행_일정을_등록한다() throws Exception {
        ScheduleCreateRequestDto request =
                ScheduleCreateRequestDto.builder()
                        .tripCountryId(1L)
                        .scheduleName(
                                "루브르 박물관 가이드 투어"
                        )
                        .scheduledAt(
                                LocalDateTime.parse(
                                        "2026-08-28T10:30:00"
                                )
                        )
                        .amount(new BigDecimal("85.00"))
                        .currencyCode("EUR")
                        .paymentStatus(
                                SchedulePaymentStatus.PREPAID
                        )
                        .placeName("루브르 박물관")
                        .placeAddress(
                                "Rue de Rivoli, Paris"
                        )
                        .memo("입장 10분 전까지 도착")
                        .build();

        ScheduleCreateResponseDto response =
                ScheduleCreateResponseDto.builder()
                        .scheduleId(10L)
                        .build();

        when(
                scheduleService.createSchedule(
                        eq(1L),
                        any(ScheduleCreateRequestDto.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post(
                                "/api/v1/trips/{tripId}/schedules",
                                1L
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.code")
                                .value("SUCCESS")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("여행 일정 등록 성공")
                )
                .andExpect(
                        jsonPath("$.data.scheduleId")
                                .value(10)
                );
    }

    @Test
    void 일정명이_비어있으면_400을_반환한다()
            throws Exception {
        ScheduleCreateRequestDto request =
                ScheduleCreateRequestDto.builder()
                        .tripCountryId(1L)
                        .scheduleName("")
                        .scheduledAt(
                                LocalDateTime.parse(
                                        "2026-08-28T10:30:00"
                                )
                        )
                        .paymentStatus(
                                SchedulePaymentStatus.PREPAID
                        )
                        .build();

        mockMvc.perform(
                        post(
                                "/api/v1/trips/{tripId}/schedules",
                                1L
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_INPUT")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("일정명은 필수입니다.")
                );

        verifyNoInteractions(scheduleService);
    }

    @Test
    void 결제_상태가_올바르지_않으면_400을_반환한다()
            throws Exception {
        String requestBody = """
                {
                  "tripCountryId": 1,
                  "scheduleName": "루브르 박물관",
                  "scheduledAt": "2026-08-28T10:30:00",
                  "currencyCode": "EUR",
                  "paymentStatus": "INVALID"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/trips/{tripId}/schedules",
                                1L
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_INPUT")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "요청값의 형식이 올바르지 않습니다."
                                )
                );

        verifyNoInteractions(scheduleService);
    }
}