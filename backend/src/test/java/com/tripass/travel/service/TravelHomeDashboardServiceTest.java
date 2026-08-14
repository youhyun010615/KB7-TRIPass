package com.tripass.travel.service;

import com.tripass.travel.dto.TripCountryGoalResponseDto;
import com.tripass.travel.dto.TripGoalResponseDto;
import com.tripass.travel.dto.TripHomeDashboardResponseDto;
import com.tripass.travel.mapper.TravelMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelHomeDashboardServiceTest {

    @Mock
    private TravelMapper travelMapper;

    @InjectMocks
    private TravelService travelService;

    @Test
    @DisplayName("활성 여행의 홈 저축 현황을 계산한다")
    void getActiveTripHome() {
        Long userId = 1L;
        Long tripId = 10L;
        TripGoalResponseDto trip = TripGoalResponseDto.builder()
                .tripId(tripId)
                .tripName("유럽 여행")
                .status("PLANNING")
                .startDate(LocalDate.now().plusMonths(3))
                .endDate(LocalDate.now().plusMonths(3).plusDays(10))
                .totalTargetAmount(new BigDecimal("2000000"))
                .build();
        List<TripCountryGoalResponseDto> countries = List.of(
                TripCountryGoalResponseDto.builder()
                        .tripCountryId(101L)
                        .countryId(1L)
                        .countryName("독일")
                        .currencyCode("EUR")
                        .arrivalDate(trip.getStartDate())
                        .departureDate(trip.getStartDate().plusDays(4))
                        .displayOrder(1)
                        .targetBudget(new BigDecimal("800000"))
                        .build(),
                TripCountryGoalResponseDto.builder()
                        .tripCountryId(102L)
                        .countryId(2L)
                        .countryName("스위스")
                        .currencyCode("CHF")
                        .arrivalDate(trip.getStartDate().plusDays(5))
                        .departureDate(trip.getEndDate())
                        .displayOrder(2)
                        .targetBudget(new BigDecimal("1200000"))
                        .build()
        );

        when(travelMapper.findActiveTripGoalByUserId(userId)).thenReturn(trip);
        when(travelMapper.findTripGoalCountries(tripId)).thenReturn(countries);
        when(travelMapper.findTripWalletBalanceByUserId(userId)).thenReturn(new BigDecimal("500000"));
        when(travelMapper.findPrepaidExpenseTotalByTripId(tripId)).thenReturn(new BigDecimal("1500000"));
        when(travelMapper.findMonthlySavingAmountByTripId(tripId)).thenReturn(new BigDecimal("500000"));

        TripHomeDashboardResponseDto result = travelService.getActiveTripHome(userId);

        assertEquals(tripId, result.getTripId());
        assertEquals(new BigDecimal("1500000"), result.getRemainingTargetAmount());
        assertEquals(new BigDecimal("25.00"), result.getSavingProgressPercent());
        assertEquals(new BigDecimal("1500000"), result.getPrepaidExpenseTotal());
        assertEquals(new BigDecimal("500000"), result.getMonthlySavingTarget());
        assertEquals(new BigDecimal("40.00"), result.getCountries().get(0).getTargetSharePercent());
        assertEquals(new BigDecimal("60.00"), result.getCountries().get(1).getTargetSharePercent());
    }

    @Test
    @DisplayName("목표 금액이 없으면 진행률과 국가 비중을 0으로 반환한다")
    void getActiveTripHomeWithZeroTarget() {
        Long userId = 1L;
        Long tripId = 10L;
        TripGoalResponseDto trip = TripGoalResponseDto.builder()
                .tripId(tripId)
                .tripName("예산 미확정 여행")
                .status("PLANNING")
                .startDate(LocalDate.now().plusMonths(1))
                .endDate(LocalDate.now().plusMonths(1).plusDays(3))
                .totalTargetAmount(BigDecimal.ZERO)
                .build();
        List<TripCountryGoalResponseDto> countries = List.of(
                TripCountryGoalResponseDto.builder()
                        .tripCountryId(101L)
                        .countryId(1L)
                        .countryName("독일")
                        .currencyCode("EUR")
                        .arrivalDate(trip.getStartDate())
                        .departureDate(trip.getEndDate())
                        .displayOrder(1)
                        .targetBudget(BigDecimal.ZERO)
                        .build()
        );

        when(travelMapper.findActiveTripGoalByUserId(userId)).thenReturn(trip);
        when(travelMapper.findTripGoalCountries(tripId)).thenReturn(countries);
        when(travelMapper.findTripWalletBalanceByUserId(userId)).thenReturn(BigDecimal.ZERO);
        when(travelMapper.findPrepaidExpenseTotalByTripId(tripId)).thenReturn(BigDecimal.ZERO);
        when(travelMapper.findMonthlySavingAmountByTripId(tripId)).thenReturn(null);

        TripHomeDashboardResponseDto result = travelService.getActiveTripHome(userId);

        assertEquals(BigDecimal.ZERO, result.getSavingProgressPercent());
        assertEquals(BigDecimal.ZERO, result.getRemainingTargetAmount());
        assertEquals(BigDecimal.ZERO, result.getMonthlySavingTarget());
        assertEquals(BigDecimal.ZERO, result.getCountries().get(0).getTargetSharePercent());
    }
}
