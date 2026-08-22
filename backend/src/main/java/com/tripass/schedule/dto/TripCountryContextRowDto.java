package com.tripass.schedule.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class TripCountryContextRowDto {

    private Long tripCountryId;
    private Long countryId;
    private String countryName;
    private String timeZone;
    private Long defaultCurrencyId;
    private String defaultCurrencyCode;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
}