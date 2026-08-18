package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripListItemResponseDto {
    private Long tripId;
    private String tripName;
    private String status;
    private String countryNames;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
}
