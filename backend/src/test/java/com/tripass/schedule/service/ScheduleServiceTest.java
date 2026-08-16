package com.tripass.schedule.service;

import com.tripass.schedule.dto.*;
import com.tripass.schedule.enums.SchedulePaymentStatus;
import com.tripass.schedule.enums.ScheduleStatus;
import com.tripass.schedule.exception.ScheduleException;
import com.tripass.schedule.mapper.ScheduleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private ScheduleMapper scheduleMapper;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void 여행이_존재하지_않으면_목록_조회_예외가_발생한다() {
        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(null);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.getSchedules(1L, USER_ID)
        );

        assertEquals(
                "TRIP_NOT_FOUND",
                exception.getErrorCode()
        );

        verify(scheduleMapper, never())
                .findAllByTripId(1L);
    }

    @Test
    void 다른_사용자의_여행이면_목록_조회_예외가_발생한다() {
        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(OTHER_USER_ID);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.getSchedules(1L, USER_ID)
        );

        assertEquals(
                "TRIP_ACCESS_DENIED",
                exception.getErrorCode()
        );

        verify(scheduleMapper, never())
                .findAllByTripId(1L);
    }

    @Test
    void 여행_일정_목록을_현지_시간으로_조회한다() {
        ScheduleListRowDto row =
                createScheduleListRow();

        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(scheduleMapper.findAllByTripId(1L))
                .thenReturn(List.of(row));

        List<ScheduleListResponseDto> result =
                scheduleService.getSchedules(1L, USER_ID);

        assertEquals(1, result.size());

        ScheduleListResponseDto schedule =
                result.get(0);

        assertEquals(10L, schedule.getId());
        assertEquals(
                "루브르 박물관",
                schedule.getScheduleName()
        );
        assertEquals(
                OffsetDateTime.parse(
                        "2026-08-28T10:30:00+02:00"
                ),
                schedule.getScheduledAt()
        );
        assertEquals(
                ZoneOffset.ofHours(2),
                schedule.getScheduledAt().getOffset()
        );
        assertEquals(
                SchedulePaymentStatus.PREPAID,
                schedule.getPaymentStatus()
        );
        assertEquals(
                ScheduleStatus.UPCOMING,
                schedule.getScheduleStatus()
        );
    }

    @Test
    void 일정이_없으면_빈_목록을_반환한다() {
        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(scheduleMapper.findAllByTripId(1L))
                .thenReturn(Collections.emptyList());

        List<ScheduleListResponseDto> result =
                scheduleService.getSchedules(1L, USER_ID);

        assertTrue(result.isEmpty());

        verify(scheduleMapper)
                .findAllByTripId(1L);
    }

    @Test
    void 여행이_존재하지_않으면_상세_조회_예외가_발생한다() {
        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(null);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.getScheduleDetail(
                        1L,
                        2L,
                        USER_ID
                )
        );

        assertEquals(
                "TRIP_NOT_FOUND",
                exception.getErrorCode()
        );

        verify(scheduleMapper, never())
                .findDetailByTripIdAndScheduleId(
                        1L,
                        2L
                );
    }

    @Test
    void 여행_일정_상세를_현지_시간으로_조회한다() {
        ScheduleDetailRowDto row =
                createScheduleDetailRow();

        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(
                scheduleMapper
                        .findDetailByTripIdAndScheduleId(
                                1L,
                                2L
                        )
        ).thenReturn(row);

        ScheduleDetailResponseDto result =
                scheduleService.getScheduleDetail(
                        1L,
                        2L,
                        USER_ID
                );

        assertEquals(2L, result.getId());
        assertEquals("프랑스", result.getCountryName());
        assertEquals(
                OffsetDateTime.parse(
                        "2026-08-28T10:30:00+02:00"
                ),
                result.getScheduledAt()
        );
        assertEquals(
                SchedulePaymentStatus.PREPAID,
                result.getPaymentStatus()
        );
        assertEquals(
                ScheduleStatus.UPCOMING,
                result.getScheduleStatus()
        );
        assertEquals(
                "입장 10분 전까지 도착",
                result.getMemo()
        );
    }

    @Test
    void 여행_일정이_존재하지_않으면_예외가_발생한다() {
        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(
                scheduleMapper
                        .findDetailByTripIdAndScheduleId(
                                1L,
                                999L
                        )
        ).thenReturn(null);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.getScheduleDetail(
                        1L,
                        999L,
                        USER_ID
                )
        );

        assertEquals(
                "SCHEDULE_NOT_FOUND",
                exception.getErrorCode()
        );
    }

    @Test
    void 국가_기본_통화로_여행_일정을_등록한다() {
        ScheduleCreateRequestDto request =
                createScheduleRequest(null);

        TripCountryContextRowDto tripCountry =
                createTripCountryContext();

        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(
                scheduleMapper.findTripCountryContext(
                        1L,
                        1L
                )
        ).thenReturn(tripCountry);

        when(
                scheduleMapper.insertSchedule(
                        any(ScheduleCreateCommandDto.class)
                )
        ).thenAnswer(invocation -> {
            ScheduleCreateCommandDto command =
                    invocation.getArgument(0);

            command.setId(10L);

            return 1;
        });

        ScheduleCreateResponseDto result =
                scheduleService.createSchedule(
                        1L,
                        request,
                        USER_ID
                );

        ArgumentCaptor<ScheduleCreateCommandDto> captor =
                ArgumentCaptor.forClass(
                        ScheduleCreateCommandDto.class
                );

        verify(scheduleMapper)
                .insertSchedule(captor.capture());

        ScheduleCreateCommandDto saved =
                captor.getValue();

        assertEquals(1L, saved.getTripId());
        assertEquals(1L, saved.getTripCountryId());
        assertEquals(1L, saved.getCurrencyId());
        assertEquals(
                Timestamp.from(
                        Instant.parse(
                                "2026-08-28T08:30:00Z"
                        )
                ),
                saved.getScheduledAt()
        );
        assertEquals(
                "PREPAID",
                saved.getPaymentStatus()
        );
        assertEquals(
                "UPCOMING",
                saved.getScheduleStatus()
        );
        assertEquals(10L, result.getScheduleId());

        verify(
                scheduleMapper,
                never()
        ).findCurrencyIdByCode(
                any(String.class)
        );
    }

    @Test
    void 사용자가_선택한_통화로_여행_일정을_등록한다() {
        ScheduleCreateRequestDto request =
                createScheduleRequest("USD");

        TripCountryContextRowDto tripCountry =
                createTripCountryContext();

        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(
                scheduleMapper.findTripCountryContext(
                        1L,
                        1L
                )
        ).thenReturn(tripCountry);

        when(
                scheduleMapper.findCurrencyIdByCode(
                        "USD"
                )
        ).thenReturn(3L);

        when(
                scheduleMapper.insertSchedule(
                        any(ScheduleCreateCommandDto.class)
                )
        ).thenAnswer(invocation -> {
            ScheduleCreateCommandDto command =
                    invocation.getArgument(0);

            command.setId(11L);

            return 1;
        });

        ScheduleCreateResponseDto result =
                scheduleService.createSchedule(
                        1L,
                        request,
                        USER_ID
                );

        ArgumentCaptor<ScheduleCreateCommandDto> captor =
                ArgumentCaptor.forClass(
                        ScheduleCreateCommandDto.class
                );

        verify(scheduleMapper)
                .insertSchedule(captor.capture());

        ScheduleCreateCommandDto saved =
                captor.getValue();

        assertEquals(3L, saved.getCurrencyId());
        assertEquals(11L, result.getScheduleId());
    }

    @Test
    void 다른_사용자의_여행에는_일정을_등록할_수_없다() {
        ScheduleCreateRequestDto request =
                createScheduleRequest(null);

        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(OTHER_USER_ID);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.createSchedule(
                        1L,
                        request,
                        USER_ID
                )
        );

        assertEquals(
                "TRIP_ACCESS_DENIED",
                exception.getErrorCode()
        );

        verify(
                scheduleMapper,
                never()
        ).insertSchedule(
                any(ScheduleCreateCommandDto.class)
        );
    }

    @Test
    void 선택한_여행_국가가_없으면_예외가_발생한다() {
        ScheduleCreateRequestDto request =
                createScheduleRequest(null);

        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(
                scheduleMapper.findTripCountryContext(
                        1L,
                        1L
                )
        ).thenReturn(null);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.createSchedule(
                        1L,
                        request,
                        USER_ID
                )
        );

        assertEquals(
                "TRIP_COUNTRY_NOT_FOUND",
                exception.getErrorCode()
        );

        verify(
                scheduleMapper,
                never()
        ).insertSchedule(
                any(ScheduleCreateCommandDto.class)
        );
    }

    @Test
    void 선택한_통화가_없으면_예외가_발생한다() {
        ScheduleCreateRequestDto request =
                createScheduleRequest("ABC");

        TripCountryContextRowDto tripCountry =
                createTripCountryContext();

        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(
                scheduleMapper.findTripCountryContext(
                        1L,
                        1L
                )
        ).thenReturn(tripCountry);

        when(
                scheduleMapper.findCurrencyIdByCode(
                        "ABC"
                )
        ).thenReturn(null);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.createSchedule(
                        1L,
                        request,
                        USER_ID
                )
        );

        assertEquals(
                "CURRENCY_NOT_FOUND",
                exception.getErrorCode()
        );

        verify(
                scheduleMapper,
                never()
        ).insertSchedule(
                any(ScheduleCreateCommandDto.class)
        );
    }

    private ScheduleListRowDto createScheduleListRow() {
        ScheduleListRowDto row =
                new ScheduleListRowDto();

        row.setId(10L);
        row.setTripId(1L);
        row.setTripCountryId(1L);
        row.setCountryName("프랑스");
        row.setTimeZone("Europe/Paris");
        row.setScheduleName("루브르 박물관");
        row.setScheduledAt(
                Timestamp.from(
                        Instant.parse(
                                "2026-08-28T08:30:00Z"
                        )
                )
        );
        row.setAmount(new BigDecimal("85.00"));
        row.setCurrencyCode("EUR");
        row.setCurrencySymbol("€");
        row.setPaymentStatus("PREPAID");
        row.setScheduleStatus("UPCOMING");
        row.setPlaceName("루브르 박물관");
        row.setPlaceAddress(
                "Rue de Rivoli, Paris"
        );

        return row;
    }

    private ScheduleDetailRowDto createScheduleDetailRow() {
        ScheduleDetailRowDto row =
                new ScheduleDetailRowDto();

        row.setId(2L);
        row.setTripId(1L);
        row.setTripCountryId(1L);
        row.setCountryName("프랑스");
        row.setTimeZone("Europe/Paris");
        row.setScheduleName(
                "루브르 박물관 가이드 투어"
        );
        row.setScheduledAt(
                Timestamp.from(
                        Instant.parse(
                                "2026-08-28T08:30:00Z"
                        )
                )
        );
        row.setAmount(new BigDecimal("85.00"));
        row.setCurrencyCode("EUR");
        row.setCurrencySymbol("€");
        row.setPaymentStatus("PREPAID");
        row.setScheduleStatus("UPCOMING");
        row.setPlaceName("루브르 박물관");
        row.setPlaceAddress(
                "Rue de Rivoli, Paris"
        );
        row.setMemo("입장 10분 전까지 도착");

        return row;
    }

    private TripCountryContextRowDto
    createTripCountryContext() {
        TripCountryContextRowDto row =
                new TripCountryContextRowDto();

        row.setTripCountryId(1L);
        row.setCountryId(1L);
        row.setCountryName("프랑스");
        row.setTimeZone("Europe/Paris");
        row.setDefaultCurrencyId(1L);
        row.setDefaultCurrencyCode("EUR");
        row.setArrivalDate(LocalDate.of(2026, 8, 25));
        row.setDepartureDate(LocalDate.of(2026, 8, 31));

        return row;
    }

    private ScheduleCreateRequestDto
    createScheduleRequest(
            String currencyCode
    ) {
        return ScheduleCreateRequestDto.builder()
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
                .currencyCode(currencyCode)
                .paymentStatus(
                        SchedulePaymentStatus.PREPAID
                )
                .placeName("루브르 박물관")
                .placeAddress(
                        "Rue de Rivoli, Paris"
                )
                .memo("입장 10분 전까지 도착")
                .build();
    }

    @Test
    void 여행_일정을_수정한다() {
        ScheduleUpdateRequestDto request =
                ScheduleUpdateRequestDto.builder()
                        .tripCountryId(1L)
                        .scheduleName(
                                "루브르 박물관 자유 관람"
                        )
                        .scheduledAt(
                                LocalDateTime.parse(
                                        "2026-08-28T11:00:00"
                                )
                        )
                        .amount(new BigDecimal("90.00"))
                        .currencyCode("EUR")
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
                        .memo("11시로 시간 변경")
                        .build();

        TripCountryContextRowDto tripCountry =
                createTripCountryContext();

        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(
                scheduleMapper
                        .existsScheduleByTripIdAndScheduleId(
                                1L,
                                10L
                        )
        ).thenReturn(true);

        when(
                scheduleMapper.findTripCountryContext(
                        1L,
                        1L
                )
        ).thenReturn(tripCountry);

        when(
                scheduleMapper.findCurrencyIdByCode(
                        "EUR"
                )
        ).thenReturn(1L);

        ScheduleUpdateResponseDto result =
                scheduleService.updateSchedule(
                        1L,
                        10L,
                        request,
                        USER_ID
                );

        ArgumentCaptor<ScheduleUpdateCommandDto> captor =
                ArgumentCaptor.forClass(
                        ScheduleUpdateCommandDto.class
                );

        verify(scheduleMapper)
                .updateSchedule(captor.capture());

        ScheduleUpdateCommandDto command =
                captor.getValue();

        assertEquals(1L, command.getTripId());
        assertEquals(10L, command.getScheduleId());
        assertEquals(
                "루브르 박물관 자유 관람",
                command.getScheduleName()
        );
        assertEquals(
                Timestamp.from(
                        Instant.parse(
                                "2026-08-28T09:00:00Z"
                        )
                ),
                command.getScheduledAt()
        );
        assertEquals(
                "PREPAID",
                command.getPaymentStatus()
        );
        assertEquals(10L, result.getScheduleId());
    }

    @Test
    void 다른_사용자의_여행_일정은_수정할_수_없다() {
        ScheduleUpdateRequestDto request =
                ScheduleUpdateRequestDto.builder()
                        .tripCountryId(1L)
                        .scheduleName("루브르 박물관")
                        .scheduledAt(
                                LocalDateTime.parse(
                                        "2026-08-28T11:00:00"
                                )
                        )
                        .paymentStatus(
                                SchedulePaymentStatus.PREPAID
                        )
                        .scheduleStatus(
                                ScheduleStatus.UPCOMING
                        )
                        .build();

        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(OTHER_USER_ID);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.updateSchedule(
                        1L,
                        10L,
                        request,
                        USER_ID
                )
        );

        assertEquals(
                "TRIP_ACCESS_DENIED",
                exception.getErrorCode()
        );

        verify(
                scheduleMapper,
                never()
        ).updateSchedule(
                any(ScheduleUpdateCommandDto.class)
        );
    }

    @Test
    void 수정할_여행_일정이_없으면_예외가_발생한다() {
        ScheduleUpdateRequestDto request =
                ScheduleUpdateRequestDto.builder()
                        .tripCountryId(1L)
                        .scheduleName("루브르 박물관")
                        .scheduledAt(
                                LocalDateTime.parse(
                                        "2026-08-28T11:00:00"
                                )
                        )
                        .paymentStatus(
                                SchedulePaymentStatus.PREPAID
                        )
                        .scheduleStatus(
                                ScheduleStatus.UPCOMING
                        )
                        .build();

        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(
                scheduleMapper
                        .existsScheduleByTripIdAndScheduleId(
                                1L,
                                999L
                        )
        ).thenReturn(false);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.updateSchedule(
                        1L,
                        999L,
                        request,
                        USER_ID
                )
        );

        assertEquals(
                "SCHEDULE_NOT_FOUND",
                exception.getErrorCode()
        );

        verify(
                scheduleMapper,
                never()
        ).updateSchedule(
                any(ScheduleUpdateCommandDto.class)
        );
    }

    @Test
    void 여행_일정을_삭제한다() {
        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(
                scheduleMapper.softDeleteSchedule(
                        1L,
                        10L
                )
        ).thenReturn(1);

        scheduleService.deleteSchedule(
                1L,
                10L,
                USER_ID
        );

        verify(scheduleMapper)
                .softDeleteSchedule(
                        1L,
                        10L
                );
    }

    @Test
    void 삭제할_여행_일정이_없으면_예외가_발생한다() {
        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(USER_ID);

        when(
                scheduleMapper.softDeleteSchedule(
                        1L,
                        999L
                )
        ).thenReturn(0);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.deleteSchedule(
                        1L,
                        999L,
                        USER_ID
                )
        );

        assertEquals(
                "SCHEDULE_NOT_FOUND",
                exception.getErrorCode()
        );
    }

    @Test
    void 여행이_없으면_일정을_삭제하지_않는다() {
        when(scheduleMapper.findTripUserId(99L))
                .thenReturn(null);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.deleteSchedule(
                        99L,
                        10L,
                        USER_ID
                )
        );

        assertEquals(
                "TRIP_NOT_FOUND",
                exception.getErrorCode()
        );

        verify(
                scheduleMapper,
                never()
        ).softDeleteSchedule(
                99L,
                10L
        );
    }

    @Test
    void 다른_사용자의_여행_일정은_삭제할_수_없다() {
        when(scheduleMapper.findTripUserId(1L))
                .thenReturn(OTHER_USER_ID);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.deleteSchedule(
                        1L,
                        10L,
                        USER_ID
                )
        );

        assertEquals(
                "TRIP_ACCESS_DENIED",
                exception.getErrorCode()
        );

        verify(
                scheduleMapper,
                never()
        ).softDeleteSchedule(
                1L,
                10L
        );
    }
}
