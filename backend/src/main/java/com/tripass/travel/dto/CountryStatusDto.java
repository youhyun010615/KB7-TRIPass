package com.tripass.travel.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CountryStatusDto {
    private Long tripCountryId;
    private String countryName;
    private Long targetBudget;
    private Long spentAmount;
    private Long totalDays;
    private Long passedDays;
}
