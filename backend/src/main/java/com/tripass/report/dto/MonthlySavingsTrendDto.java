package com.tripass.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySavingsTrendDto {
    private String month;
    private BigDecimal savedAmount;
    private BigDecimal cumulativeAmount;
}
