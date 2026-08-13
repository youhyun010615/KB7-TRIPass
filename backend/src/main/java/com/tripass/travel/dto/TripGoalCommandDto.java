package com.tripass.travel.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Builder
public class TripGoalCommandDto {

    @Setter
    private Long id;
    private Long userId;
    private String tripName;
    private LocalDate startDate;
    private LocalDate endDate;
}
