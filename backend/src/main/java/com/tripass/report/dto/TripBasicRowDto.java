package com.tripass.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class TripBasicRowDto {
    private Long tripId;
    private Long userId;
    private String tripName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalTargetAmount;
}
