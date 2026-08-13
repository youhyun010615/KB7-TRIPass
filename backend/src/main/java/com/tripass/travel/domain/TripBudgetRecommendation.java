package com.tripass.travel.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripBudgetRecommendation {

    private Long id;
    private Long tripCountryId;
    private Integer travelerCount;
    private String travelStyle;
    private BigDecimal recommendedAirfareAmount;
    private BigDecimal recommendedLodgingAmount;
    private BigDecimal recommendedActivityAmount;
    private BigDecimal recommendedTransportAmount;
    private BigDecimal recommendedFoodAmount;
    private BigDecimal recommendedOtherAmount;
    private String aiReason;
    private String aiModel;
}
