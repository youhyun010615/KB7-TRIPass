package com.tripass.travel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public final class TripSavingCalculator {

    private TripSavingCalculator() {
    }

    public static int calculateRemainingMonths(LocalDate tripStartDate, LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth tripMonth = YearMonth.from(tripStartDate);
        long months = ChronoUnit.MONTHS.between(currentMonth, tripMonth);
        return (int) Math.max(1, months);
    }

    /**
     * 이번 달 저축 목표를 계산한다.
     * - 여행 시작 달은 저축 제외
     * - D-day 기반 일일 저축 금액 산정
     * - 이번 달: 일일 금액 × 이번 달 남은 일수
     * - 다음 달부터: (잔여 목표 - 이번 달 목표) / 남은 개월수
     *
     * @return 이번 달 저축 목표 금액
     */
    public static BigDecimal calculateCurrentMonthSavingTarget(
            BigDecimal totalTarget,
            BigDecimal walletBalance,
            LocalDate today,
            LocalDate tripStartDate
    ) {
        BigDecimal remaining = remainingTarget(totalTarget, walletBalance);
        if (remaining.signum() == 0) return BigDecimal.ZERO;

        YearMonth currentMonth = YearMonth.from(today);
        YearMonth tripMonth = YearMonth.from(tripStartDate);

        if (!currentMonth.isBefore(tripMonth)) return BigDecimal.ZERO;

        long daysUntilTrip = ChronoUnit.DAYS.between(today, tripStartDate);
        if (daysUntilTrip <= 0) return BigDecimal.ZERO;

        BigDecimal dailyRate = remaining.divide(
                BigDecimal.valueOf(daysUntilTrip), 10, RoundingMode.HALF_UP);

        int daysLeftInMonth = currentMonth.lengthOfMonth() - today.getDayOfMonth() + 1;

        return dailyRate.multiply(BigDecimal.valueOf(daysLeftInMonth))
                .setScale(0, RoundingMode.CEILING);
    }

    /**
     * 다음 달부터의 월별 저축 목표를 계산한다.
     * (잔여 목표 - 이번 달 목표) / 남은 full 개월수
     *
     * @return 향후 월별 저축 목표 금액 (saving_plans 테이블에 저장)
     */
    public static BigDecimal calculateFullMonthSavingTarget(
            BigDecimal totalTarget,
            BigDecimal walletBalance,
            LocalDate today,
            LocalDate tripStartDate
    ) {
        BigDecimal remaining = remainingTarget(totalTarget, walletBalance);
        if (remaining.signum() == 0) return BigDecimal.ZERO;

        YearMonth currentMonth = YearMonth.from(today);
        YearMonth tripMonth = YearMonth.from(tripStartDate);

        if (!currentMonth.isBefore(tripMonth)) return BigDecimal.ZERO;

        BigDecimal currentMonthTarget = calculateCurrentMonthSavingTarget(
                totalTarget, walletBalance, today, tripStartDate);

        long fullMonths = ChronoUnit.MONTHS.between(currentMonth.plusMonths(1), tripMonth);
        if (fullMonths <= 0) return BigDecimal.ZERO;

        BigDecimal remainingAfterCurrentMonth = remaining.subtract(currentMonthTarget).max(BigDecimal.ZERO);
        return remainingAfterCurrentMonth.divide(
                BigDecimal.valueOf(fullMonths), 0, RoundingMode.CEILING);
    }

    /**
     * 월별 저축 목표를 계산한다 (saving_plans 저장용).
     * 이번 달이 partial이면 다음 달부터의 균등 분할 금액을 반환하고,
     * 이번 달이 1일이면 이번 달 목표와 동일하게 반환한다.
     */
    public static BigDecimal calculateMonthlySavingTarget(
            BigDecimal totalTarget,
            BigDecimal walletBalance,
            LocalDate today,
            LocalDate tripStartDate
    ) {
        BigDecimal remaining = remainingTarget(totalTarget, walletBalance);
        if (remaining.signum() == 0) return BigDecimal.ZERO;

        YearMonth currentMonth = YearMonth.from(today);
        YearMonth tripMonth = YearMonth.from(tripStartDate);

        if (!currentMonth.isBefore(tripMonth)) return BigDecimal.ZERO;

        long fullMonths = ChronoUnit.MONTHS.between(currentMonth.plusMonths(1), tripMonth);

        if (fullMonths <= 0) {
            return calculateCurrentMonthSavingTarget(totalTarget, walletBalance, today, tripStartDate);
        }

        return calculateFullMonthSavingTarget(totalTarget, walletBalance, today, tripStartDate);
    }

    private static BigDecimal remainingTarget(BigDecimal totalTarget, BigDecimal walletBalance) {
        BigDecimal balance = walletBalance == null ? BigDecimal.ZERO : walletBalance;
        return totalTarget.subtract(balance).max(BigDecimal.ZERO);
    }
}
