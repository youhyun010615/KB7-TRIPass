package com.tripass.travel.service;

import com.tripass.travel.domain.Trip;
import com.tripass.travel.domain.TripCountry;
import com.tripass.travel.dto.TripCountryCreateRequestDto;
import com.tripass.travel.dto.TripCountryDetailDto;
import com.tripass.travel.dto.TripCreateRequestDto;
import com.tripass.travel.dto.TripCreateResponseDto;
import com.tripass.travel.dto.TripCurrentResponseDto;
import com.tripass.travel.exception.TravelException;
import com.tripass.travel.mapper.TravelMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelServiceTest {

    @Mock
    private TravelMapper travelMapper;

    @InjectMocks
    private TravelService travelService;

    @Test
    void 여행과_국가별_일정을_등록한다() {
        TripCreateRequestDto request = TripCreateRequestDto.builder()
                .tripName("유럽 2개국 배낭여행")
                .countries(List.of(
                        TripCountryCreateRequestDto.builder()
                                .countryName("프랑스")
                                .startDate(LocalDate.of(2026, 8, 28))
                                .endDate(LocalDate.of(2026, 9, 12))
                                .targetBudget(BigDecimal.valueOf(2_500_000))
                                .build(),
                        TripCountryCreateRequestDto.builder()
                                .countryName("스위스")
                                .startDate(LocalDate.of(2026, 9, 12))
                                .endDate(LocalDate.of(2026, 9, 27))
                                .targetBudget(BigDecimal.valueOf(2_500_000))
                                .build()
                ))
                .build();

        when(travelMapper.insertTrip(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            trip.setId(1L);
            return 1;
        });
        when(travelMapper.findCountryIdByName("프랑스")).thenReturn(10L);
        when(travelMapper.findCountryIdByName("스위스")).thenReturn(11L);
        when(travelMapper.insertTripCountry(any(TripCountry.class))).thenAnswer(invocation -> {
            TripCountry tripCountry = invocation.getArgument(0);
            tripCountry.setId(tripCountry.getCountryId() == 10L ? 100L : 101L);
            return 1;
        });

        TripCreateResponseDto result = travelService.createTrip(5L, request);

        assertEquals(1L, result.getTripId());
        assertEquals(2, result.getCountries().size());
        assertEquals(100L, result.getCountries().get(0).getTripCountryId());
        assertEquals("프랑스", result.getCountries().get(0).getCountryName());
        assertEquals(101L, result.getCountries().get(1).getTripCountryId());
        assertEquals("스위스", result.getCountries().get(1).getCountryName());

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(travelMapper).insertTrip(tripCaptor.capture());
        Trip savedTrip = tripCaptor.getValue();

        assertEquals(5L, savedTrip.getUserId());
        assertEquals("유럽 2개국 배낭여행", savedTrip.getTripName());
        assertEquals("PLANNING", savedTrip.getStatus());
        assertEquals(LocalDate.of(2026, 8, 28), savedTrip.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 27), savedTrip.getEndDate());
        assertEquals(0, BigDecimal.valueOf(5_000_000).compareTo(savedTrip.getTotalTargetAmount()));

        ArgumentCaptor<TripCountry> countryCaptor = ArgumentCaptor.forClass(TripCountry.class);
        verify(travelMapper, org.mockito.Mockito.times(2)).insertTripCountry(countryCaptor.capture());
        List<TripCountry> savedCountries = countryCaptor.getAllValues();

        assertEquals(10L, savedCountries.get(0).getCountryId());
        assertEquals(1, savedCountries.get(0).getDisplayOrder());
        assertEquals(11L, savedCountries.get(1).getCountryId());
        assertEquals(2, savedCountries.get(1).getDisplayOrder());
    }

    @Test
    void 국가의_출국일이_도착일보다_빠르면_예외가_발생한다() {
        TripCreateRequestDto request = TripCreateRequestDto.builder()
                .tripName("일정 오류 여행")
                .countries(List.of(
                        TripCountryCreateRequestDto.builder()
                                .countryName("프랑스")
                                .startDate(LocalDate.of(2026, 9, 12))
                                .endDate(LocalDate.of(2026, 8, 28))
                                .targetBudget(BigDecimal.valueOf(1_000_000))
                                .build()
                ))
                .build();

        TravelException exception = assertThrows(
                TravelException.class,
                () -> travelService.createTrip(5L, request)
        );

        assertEquals("INVALID_TRIP_COUNTRY_DATES", exception.getErrorCode());
        verify(travelMapper, never()).insertTrip(any(Trip.class));
    }

    @Test
    void 존재하지_않는_국가명이면_예외가_발생한다() {
        TripCreateRequestDto request = TripCreateRequestDto.builder()
                .tripName("알 수 없는 나라 여행")
                .countries(List.of(
                        TripCountryCreateRequestDto.builder()
                                .countryName("아틀란티스")
                                .startDate(LocalDate.of(2026, 8, 28))
                                .endDate(LocalDate.of(2026, 9, 12))
                                .targetBudget(BigDecimal.valueOf(1_000_000))
                                .build()
                ))
                .build();

        when(travelMapper.insertTrip(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            trip.setId(1L);
            return 1;
        });
        when(travelMapper.findCountryIdByName("아틀란티스")).thenReturn(null);

        TravelException exception = assertThrows(
                TravelException.class,
                () -> travelService.createTrip(5L, request)
        );

        assertEquals("COUNTRY_NOT_FOUND", exception.getErrorCode());
        verify(travelMapper, never()).insertTripCountry(any(TripCountry.class));
    }

    @Test
    void 가장_최근_여행과_국가_상세를_조회한다() {
        Trip trip = Trip.builder()
                .id(5L)
                .userId(5L)
                .tripName("유럽 2개국 배낭여행")
                .status("PLANNING")
                .startDate(LocalDate.of(2026, 8, 28))
                .endDate(LocalDate.of(2026, 9, 27))
                .totalTargetAmount(BigDecimal.valueOf(5_000_000))
                .build();

        List<TripCountryDetailDto> countryDetails = List.of(
                TripCountryDetailDto.builder()
                        .tripCountryId(10L)
                        .countryName("프랑스")
                        .arrivalDate(LocalDate.of(2026, 8, 28))
                        .departureDate(LocalDate.of(2026, 9, 12))
                        .targetBudget(BigDecimal.valueOf(2_500_000))
                        .build(),
                TripCountryDetailDto.builder()
                        .tripCountryId(11L)
                        .countryName("스위스")
                        .arrivalDate(LocalDate.of(2026, 9, 12))
                        .departureDate(LocalDate.of(2026, 9, 27))
                        .targetBudget(BigDecimal.valueOf(2_500_000))
                        .build()
        );

        when(travelMapper.findLatestTripByUserId(5L)).thenReturn(trip);
        when(travelMapper.findTripCountryDetailsByTripId(5L)).thenReturn(countryDetails);

        TripCurrentResponseDto result = travelService.getCurrentTrip(5L);

        assertEquals(5L, result.getTripId());
        assertEquals("유럽 2개국 배낭여행", result.getTripName());
        assertEquals("PLANNING", result.getStatus());
        assertEquals(2, result.getCountries().size());
        assertEquals(10L, result.getCountries().get(0).getTripCountryId());
        assertEquals("프랑스", result.getCountries().get(0).getCountryName());
    }

    @Test
    void 등록된_여행이_없으면_null을_반환한다() {
        when(travelMapper.findLatestTripByUserId(5L)).thenReturn(null);

        TripCurrentResponseDto result = travelService.getCurrentTrip(5L);

        assertNull(result);
        verify(travelMapper, never()).findTripCountryDetailsByTripId(any(Long.class));
    }
}
