package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCountryCatalogResponseDto {
    private Long countryId;
    private String countryName;
    private String currencyCode;
    private String currencyName;
    private String flagUrl;
}
