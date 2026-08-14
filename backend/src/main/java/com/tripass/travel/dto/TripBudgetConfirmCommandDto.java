package com.tripass.travel.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TripBudgetConfirmCommandDto {
    private Long tripCountryId;
    private BigDecimal airfareAmount;
    private BigDecimal lodgingAmount;
    private BigDecimal activityAmount;
    private BigDecimal transportAmount;
    private BigDecimal foodAmount;
    private BigDecimal otherAmount;
    private BigDecimal localTravelTarget;
}
