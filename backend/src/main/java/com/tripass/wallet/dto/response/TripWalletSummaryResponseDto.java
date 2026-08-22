package com.tripass.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripWalletSummaryResponseDto {

    private Long tripId;
    private BigDecimal walletBalance;
    private BigDecimal targetAmount;
    private BigDecimal targetSpentAmount;
    private BigDecimal emergencyInitialAmount;
    private BigDecimal emergencySpentAmount;
    private BigDecimal emergencyRemainingAmount;
    private BigDecimal externalChargedAmount;
    private BigDecimal externalChargeSpentAmount;
    private BigDecimal totalTripSpentAmount;
    private Integer usagePercent;
}
