package com.tripass.travel.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TripSavingCalculatorTest {

    @Test
    void walletBalanceAfterGoalIsFullyCoveredReturnsZero() {
        BigDecimal result = TripSavingCalculator.calculateMonthlySavingTarget(
                new BigDecimal("5000000"), new BigDecimal("6000000"), 5
        );

        assertEquals(new BigDecimal("0"), result);
    }

    @Test
    void targetIsRoundedUpByRemainingMonths() {
        BigDecimal result = TripSavingCalculator.calculateMonthlySavingTarget(
                new BigDecimal("5000000"), new BigDecimal("2000000"), 7
        );

        assertEquals(new BigDecimal("428572"), result);
    }

    @Test
    void tripStartingThisMonthUsesAtLeastOneMonth() {
        int result = TripSavingCalculator.calculateRemainingMonths(
                LocalDate.of(2026, 8, 30), LocalDate.of(2026, 8, 13)
        );

        assertEquals(1, result);
    }
}
