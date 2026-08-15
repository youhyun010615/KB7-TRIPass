package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripGoalResponseDto {
    private Long tripId;
    private String tripName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalTargetAmount;
    private List<TripCountryGoalResponseDto> countries;
}
