package com.tripass.travel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/** 여행 목표 금액과 TRIP 월렛 잔액으로 월 저축 목표를 계산하는 순수 계산기입니다. */
public final class TripSavingCalculator {

    private TripSavingCalculator() {
    }

    public static int calculateRemainingMonths(LocalDate tripStartDate, LocalDate today) {
        long months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(tripStartDate));
        return (int) Math.max(1, months);
    }

    public static BigDecimal calculateMonthlySavingTarget(
            BigDecimal localTravelTargetTotal,
            BigDecimal walletBalance,
            int remainingMonths
    ) {
        BigDecimal normalizedWalletBalance = walletBalance == null ? BigDecimal.ZERO : walletBalance;
        BigDecimal remainingTarget = localTravelTargetTotal.subtract(normalizedWalletBalance).max(BigDecimal.ZERO);
        return remainingTarget.divide(BigDecimal.valueOf(remainingMonths), 0, RoundingMode.CEILING);
    }
}
