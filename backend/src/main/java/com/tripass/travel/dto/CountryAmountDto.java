package com.tripass.travel.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CountryAmountDto {
    private String countryName;
    private Long amount;
}
