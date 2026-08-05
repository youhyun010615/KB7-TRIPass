package com.tripass.schedule.service;

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
import java.time.*;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleMapper scheduleMapper;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void 여행_ID가_null이면_예외가_발생한다() {
        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.getSchedules(null)
        );

        assertEquals(
                "INVALID_TRIP_ID",
                exception.getErrorCode()
        );

        verifyNoInteractions(scheduleMapper);
    }

    @Test
    void 여행_ID가_0이면_예외가_발생한다() {
        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.getSchedules(0L)
        );

        assertEquals(
                "INVALID_TRIP_ID",
                exception.getErrorCode()
        );

        verifyNoInteractions(scheduleMapper);
    }

    @Test
    void 여행_ID가_음수이면_예외가_발생한다() {
        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.getSchedules(-1L)
        );

        assertEquals(
                "INVALID_TRIP_ID",
                exception.getErrorCode()
        );

        verifyNoInteractions(scheduleMapper);
    }

    @Test
    void 여행이_존재하지_않으면_예외가_발생한다() {
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
    void 사용자_입력_시간을_UTC로_저장한_후_동일한_현지_시간으로_복원한다() {
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

        ScheduleListResponseDto result =
                scheduleService.getSchedules(1L)
                        .get(0);

        assertEquals(
                inputStartAt,
                result.getStartAt().toLocalDateTime()
        );

        assertEquals(
                inputEndAt,
                result.getEndAt().toLocalDateTime()
        );

        assertEquals(
                ZoneOffset.ofHours(2),
                result.getStartAt().getOffset()
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
        assertEquals(null, result.get(0).getEndAt());
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
         * 2026-08-28T08:30:00Z
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
         * 2026-08-28T09:30:00Z
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
}