package com.tripass.travel.service;

import com.tripass.travel.dto.AiBudgetResultDto;
import com.tripass.travel.dto.CountryBudgetBaselineDto;

import java.math.BigDecimal;
import java.util.Objects;

/** 국가별 조사 기준 단가와 일정만으로 여행 예산을 계산한다. */
public final class CountryBudgetCalculator {

    private CountryBudgetCalculator() {
    }

    public static AiBudgetResultDto calculate(CountryBudgetBaselineDto baseline, long travelDays, long stayNights) {
        validateInputs(baseline, travelDays, stayNights);

        return AiBudgetResultDto.builder()
                .airfareAmount(baseline.getRoundTripAirfare())
                .lodgingAmount(baseline.getLodgingPerNight().multiply(BigDecimal.valueOf(stayNights)))
                .activityAmount(baseline.getActivityPerDay().multiply(BigDecimal.valueOf(travelDays)))
                .foodAmount(baseline.getFoodPerDay().multiply(BigDecimal.valueOf(travelDays)))
                .transportAmount(baseline.getTransportPerDay().multiply(BigDecimal.valueOf(travelDays)))
                .otherAmount(baseline.getMiscPerDay().multiply(BigDecimal.valueOf(travelDays)))
                .reason(buildFallbackReason(baseline, travelDays, stayNights))
                .model("COUNTRY_BASELINE")
                .build();
    }

    private static void validateInputs(CountryBudgetBaselineDto baseline, long travelDays, long stayNights) {
        Objects.requireNonNull(baseline, "국가별 예산 기준값은 필수입니다.");
        if (travelDays < 1) {
            throw new IllegalArgumentException("여행 일수는 1일 이상이어야 합니다.");
        }
        if (stayNights < 0 || stayNights >= travelDays) {
            throw new IllegalArgumentException("숙박 일수는 0 이상이고 여행 일수보다 작아야 합니다.");
        }

        requireNonNegative(baseline.getRoundTripAirfare(), "왕복 항공료");
        requireNonNegative(baseline.getLodgingPerNight(), "1박 숙박비");
        requireNonNegative(baseline.getFoodPerDay(), "1일 식비");
        requireNonNegative(baseline.getActivityPerDay(), "1일 액티비티비");
        requireNonNegative(baseline.getTransportPerDay(), "1일 교통비");
        requireNonNegative(baseline.getMiscPerDay(), "1일 기타비");
    }

    private static void requireNonNegative(BigDecimal amount, String fieldName) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " 기준값은 0 이상이어야 합니다.");
        }
    }

    private static String buildFallbackReason(CountryBudgetBaselineDto baseline, long travelDays, long stayNights) {
        String source = baseline.getDataSource() == null || baseline.getDataSource().isBlank()
                ? "국가별 조사 기준 단가"
                : baseline.getDataSource();
        return "%s 기준으로 %d일 체류·%d박 일정을 계산했습니다. 항공·숙소는 사전 지출이며, 식비·액티비티·교통·기타만 여행 저축 목표에 반영됩니다. 자료: %s"
                .formatted(baseline.getReferenceDate(), travelDays, stayNights, source);
    }
}
