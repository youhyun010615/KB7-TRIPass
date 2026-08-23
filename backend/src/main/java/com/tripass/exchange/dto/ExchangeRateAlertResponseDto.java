package com.tripass.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateAlertResponseDto {
    private Long id;
    private Long countryId;
    private String countryName;
    private String flagUrl;
    private String currencyCode;
    private Double targetRate;
}
