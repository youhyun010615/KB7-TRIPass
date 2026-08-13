package com.tripass.travel.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TripBudgetRecommendationCommandDto {
    private Long tripCountryId;
    private Integer travelerCount;
    private String travelStyle;
    private BigDecimal airfareAmount;
    private BigDecimal lodgingAmount;
    private BigDecimal activityAmount;
    private BigDecimal foodAmount;
    private BigDecimal otherAmount;
    private String aiReason;
    private String aiModel;
}
