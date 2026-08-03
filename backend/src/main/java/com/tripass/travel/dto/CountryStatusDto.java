package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CountryStatusDto {
    private String countryName;
    private Long remainingFund;
    private Long remainingDays;
}
