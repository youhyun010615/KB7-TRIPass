package com.tripass.travel.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCountry {

    private Long id;
    private Long tripId;
    private Long countryId;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private BigDecimal targetBudget;
    private Integer displayOrder;
}
