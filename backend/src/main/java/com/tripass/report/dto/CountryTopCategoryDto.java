package com.tripass.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryTopCategoryDto {
    private String countryName;
    private String categoryName;
    private BigDecimal amount;
    private BigDecimal countryTotal;
}
