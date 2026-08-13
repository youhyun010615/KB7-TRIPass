package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiBudgetResultDto {
    private BigDecimal airfareAmount;
    private BigDecimal lodgingAmount;
    private BigDecimal activityAmount;
    private BigDecimal foodAmount;
    private BigDecimal otherAmount;
    private String reason;
    private String model;
}
