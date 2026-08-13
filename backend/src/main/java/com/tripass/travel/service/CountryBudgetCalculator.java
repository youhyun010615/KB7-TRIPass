package com.tripass.travel.service;

import com.tripass.travel.dto.AiBudgetResultDto;
import com.tripass.travel.dto.CountryBudgetBaselineDto;

import java.math.BigDecimal;

/** 국가별 조사 기준 단가와 일정만으로 여행 예산을 계산한다. */
public final class CountryBudgetCalculator {

    private CountryBudgetCalculator() {
    }

    public static AiBudgetResultDto calculate(CountryBudgetBaselineDto baseline, long travelDays, long stayNights) {
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

    private static String buildFallbackReason(CountryBudgetBaselineDto baseline, long travelDays, long stayNights) {
        return "%s 기준으로 %d일 체류·%d박 일정을 계산했습니다. 항공·숙소는 사전 지출이며, 식비·액티비티·교통·기타만 여행 저축 목표에 반영됩니다."
                .formatted(baseline.getReferenceDate(), travelDays, stayNights);
    }
}
