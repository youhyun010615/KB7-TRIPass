package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripLifecycleResponseDto {
    private Long tripId;
    private String tripName;
    private String lifecycle;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean travelModeAvailable;
    private boolean missionAvailable;
    private boolean startReportAvailable;
    private boolean startReportAcknowledged;
    private boolean endingReviewRequired;
}
