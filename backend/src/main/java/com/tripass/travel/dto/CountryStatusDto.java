package com.tripass.travel.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CountryStatusDto {
    private String countryName;
    private Long remainingFund;
    private Long remainingDays;
}
