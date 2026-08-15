package com.tripass.wallet.travelcard.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 트래블카드 외화 충전, 차감, 환불, 보정 내역을 반환하는 DTO입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelCardLedgerResponseDto {

    private Long ledgerId;
    private String currencyCode;
    private String direction;
    private String transactionType;
    private BigDecimal foreignAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String memo;
    private LocalDateTime createdAt;
}
