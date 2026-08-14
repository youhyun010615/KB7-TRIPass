package com.tripass.travel.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class TripCountryCommandDto {
    private Long tripId;
    private Long countryId;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private BigDecimal targetBudget;
    private Integer displayOrder;
}
