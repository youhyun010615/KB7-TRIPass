package com.tripass.schedule.service;

import com.tripass.schedule.dto.ScheduleDetailResponseDto;
import com.tripass.schedule.dto.ScheduleDetailRowDto;
import com.tripass.schedule.dto.ScheduleListResponseDto;
import com.tripass.schedule.dto.ScheduleListRowDto;
import com.tripass.schedule.exception.ScheduleException;
import com.tripass.schedule.mapper.ScheduleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleMapper scheduleMapper;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void 여행이_존재하지_않으면_목록_조회_예외가_발생한다() {
        when(scheduleMapper.existsTripById(1L))
                .thenReturn(false);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.getSchedules(1L)
        );

        assertEquals(
                "TRIP_NOT_FOUND",
                exception.getErrorCode()
        );

        verify(scheduleMapper)
                .existsTripById(1L);

        verify(scheduleMapper, never())
                .findAllByTripId(1L);
    }

    @Test
    void 여행_일정_목록을_조회하고_현지_시간으로_변환한다() {
        LocalDateTime inputStartAt =
                LocalDateTime.parse(
                        "2026-08-28T10:30:00"
                );

        LocalDateTime inputEndAt =
                LocalDateTime.parse(
                        "2026-08-28T11:30:00"
                );

        ZoneId parisZone =
                ZoneId.of("Europe/Paris");

        Instant storedStartInstant = inputStartAt
                .atZone(parisZone)
                .toInstant();

        Instant storedEndInstant = inputEndAt
                .atZone(parisZone)
                .toInstant();

        ScheduleListRowDto row =
                createParisScheduleRow();

        row.setStartAt(
                Timestamp.from(storedStartInstant)
        );

        row.setEndAt(
                Timestamp.from(storedEndInstant)
        );

        when(scheduleMapper.existsTripById(1L))
                .thenReturn(true);

        when(scheduleMapper.findAllByTripId(1L))
                .thenReturn(List.of(row));

        List<ScheduleListResponseDto> result =
                scheduleService.getSchedules(1L);

        assertEquals(1, result.size());

        ScheduleListResponseDto schedule =
                result.get(0);

        assertEquals(10L, schedule.getId());
        assertEquals(1L, schedule.getTripId());
        assertEquals(
                "루브르 박물관",
                schedule.getScheduleName()
        );

        assertEquals(
                inputStartAt,
                schedule.getStartAt().toLocalDateTime()
        );

        assertEquals(
                inputEndAt,
                schedule.getEndAt().toLocalDateTime()
        );

        assertEquals(
                ZoneOffset.ofHours(2),
                schedule.getStartAt().getOffset()
        );

        assertEquals(
                "Europe/Paris",
                schedule.getTimeZone()
        );
    }

    @Test
    void 종료_시간이_없으면_null을_반환한다() {
        ScheduleListRowDto row =
                createParisScheduleRow();

        row.setEndAt(null);

        when(scheduleMapper.existsTripById(1L))
                .thenReturn(true);

        when(scheduleMapper.findAllByTripId(1L))
                .thenReturn(List.of(row));

        List<ScheduleListResponseDto> result =
                scheduleService.getSchedules(1L);

        assertEquals(1, result.size());
        assertNull(result.get(0).getEndAt());
    }

    @Test
    void 일정이_없으면_빈_목록을_반환한다() {
        when(scheduleMapper.existsTripById(1L))
                .thenReturn(true);

        when(scheduleMapper.findAllByTripId(1L))
                .thenReturn(Collections.emptyList());

        List<ScheduleListResponseDto> result =
                scheduleService.getSchedules(1L);

        assertTrue(result.isEmpty());

        verify(scheduleMapper)
                .existsTripById(1L);

        verify(scheduleMapper)
                .findAllByTripId(1L);
    }

    @Test
    void 여행이_존재하지_않으면_상세_조회_예외가_발생한다() {
        when(scheduleMapper.existsTripById(1L))
                .thenReturn(false);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.getScheduleDetail(
                        1L,
                        2L
                )
        );

        assertEquals(
                "TRIP_NOT_FOUND",
                exception.getErrorCode()
        );

        verify(scheduleMapper)
                .existsTripById(1L);

        verify(scheduleMapper, never())
                .findDetailByTripIdAndScheduleId(
                        1L,
                        2L
                );
    }

    @Test
    void 여행_일정_상세를_조회하고_현지_시간으로_변환한다() {
        ScheduleDetailRowDto row =
                createScheduleDetailRow();

        when(scheduleMapper.existsTripById(1L))
                .thenReturn(true);

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
                        2L
                );

        assertEquals(2L, result.getId());
        assertEquals(1L, result.getTripId());
        assertEquals("프랑스", result.getCountryName());
        assertEquals(
                "Europe/Paris",
                result.getTimeZone()
        );

        assertEquals(
                OffsetDateTime.parse(
                        "2026-08-28T10:30:00+02:00"
                ),
                result.getStartAt()
        );

        assertEquals(
                OffsetDateTime.parse(
                        "2026-08-28T12:00:00+02:00"
                ),
                result.getEndAt()
        );

        assertEquals(
                "입장 10분 전까지 도착",
                result.getMemo()
        );
    }

    @Test
    void 여행_일정이_존재하지_않으면_예외가_발생한다() {
        when(scheduleMapper.existsTripById(1L))
                .thenReturn(true);

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
                        999L
                )
        );

        assertEquals(
                "SCHEDULE_NOT_FOUND",
                exception.getErrorCode()
        );

        verify(scheduleMapper)
                .findDetailByTripIdAndScheduleId(
                        1L,
                        999L
                );
    }

    private ScheduleListRowDto createParisScheduleRow() {
        ScheduleListRowDto row =
                new ScheduleListRowDto();

        row.setId(10L);
        row.setTripId(1L);
        row.setTripCountryId(2L);
        row.setCountryName("프랑스");
        row.setTimeZone("Europe/Paris");
        row.setScheduleName("루브르 박물관");

        /*
         * UTC 2026-08-28 08:30
         * = 파리 2026-08-28 10:30 +02:00
         */
        row.setStartAt(
                Timestamp.from(
                        Instant.parse(
                                "2026-08-28T08:30:00Z"
                        )
                )
        );

        /*
         * UTC 2026-08-28 09:30
         * = 파리 2026-08-28 11:30 +02:00
         */
        row.setEndAt(
                Timestamp.from(
                        Instant.parse(
                                "2026-08-28T09:30:00Z"
                        )
                )
        );

        row.setAmount(new BigDecimal("85.00"));
        row.setCurrencyCode("EUR");
        row.setCurrencySymbol("€");
        row.setPaymentStatus("PREPAID");
        row.setScheduleStatus("UPCOMING");
        row.setPlaceName("루브르 박물관");
        row.setPlaceAddress("Rue de Rivoli, Paris");

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

        /*
         * UTC 2026-08-28 08:30
         * = 파리 2026-08-28 10:30 +02:00
         */
        row.setStartAt(
                Timestamp.from(
                        Instant.parse(
                                "2026-08-28T08:30:00Z"
                        )
                )
        );

        /*
         * UTC 2026-08-28 10:00
         * = 파리 2026-08-28 12:00 +02:00
         */
        row.setEndAt(
                Timestamp.from(
                        Instant.parse(
                                "2026-08-28T10:00:00Z"
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
}