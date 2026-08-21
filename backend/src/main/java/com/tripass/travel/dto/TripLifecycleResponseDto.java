package com.tripass.travel.dto;

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

    // 여행/계좌 등록 온보딩 및 여행 저축 집계 상태
    private boolean hasTrip;
    private boolean hasLinkedAccount;
    private boolean savingsTrackingStarted;
    private boolean needsWalletReflectPrompt;
    private BigDecimal walletReflectAmount;
    private boolean onboardingPending;
}
