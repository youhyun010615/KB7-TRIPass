package com.tripass.travel.service;

import com.tripass.travel.dto.AiBudgetResultDto;
import com.tripass.travel.dto.CountryBudgetBaselineDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountryBudgetCalculatorTest {

    @Test
    void 기준단가에_여행일수와_숙박일수를_곱해_교통비를_포함한_예산을_계산한다() {
        CountryBudgetBaselineDto baseline = CountryBudgetBaselineDto.builder()
                .roundTripAirfare(new BigDecimal("1200000"))
                .lodgingPerNight(new BigDecimal("130000"))
                .foodPerDay(new BigDecimal("85000"))
                .activityPerDay(new BigDecimal("45000"))
                .transportPerDay(new BigDecimal("18000"))
                .miscPerDay(new BigDecimal("25000"))
                .dataSource("Paris: BudgetYourTrip; ICN-CDG KAYAK")
                .referenceDate(LocalDate.of(2026, 8, 11))
                .build();

        AiBudgetResultDto result = CountryBudgetCalculator.calculate(baseline, 5, 4);

        assertEquals(new BigDecimal("1200000"), result.getAirfareAmount());
        assertEquals(new BigDecimal("520000"), result.getLodgingAmount());
        assertEquals(new BigDecimal("425000"), result.getFoodAmount());
        assertEquals(new BigDecimal("225000"), result.getActivityAmount());
        assertEquals(new BigDecimal("90000"), result.getTransportAmount());
        assertEquals(new BigDecimal("125000"), result.getOtherAmount());
        assertEquals("COUNTRY_BASELINE", result.getModel());
        assertTrue(result.getReason().contains("Paris: BudgetYourTrip; ICN-CDG KAYAK"));
    }

    @Test
    void 잘못된_여행일수나_음수_기준값을_거부한다() {
        CountryBudgetBaselineDto baseline = CountryBudgetBaselineDto.builder()
                .roundTripAirfare(new BigDecimal("1200000"))
                .lodgingPerNight(new BigDecimal("130000"))
                .foodPerDay(new BigDecimal("85000"))
                .activityPerDay(new BigDecimal("45000"))
                .transportPerDay(new BigDecimal("18000"))
                .miscPerDay(new BigDecimal("-1"))
                .referenceDate(LocalDate.of(2026, 8, 19))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> CountryBudgetCalculator.calculate(baseline, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> CountryBudgetCalculator.calculate(baseline, 3, 3));
        assertThrows(IllegalArgumentException.class,
                () -> CountryBudgetCalculator.calculate(baseline, 3, 2));
    }
}
