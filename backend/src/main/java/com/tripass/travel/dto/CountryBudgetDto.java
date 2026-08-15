package com.tripass.travel.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CountryBudgetDto {
    private Long tripCountryId;
    private String countryName;
    private Long targetBudget;
    private Long spentAmount;
}
