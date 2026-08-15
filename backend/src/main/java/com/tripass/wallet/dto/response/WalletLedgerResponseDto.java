package com.tripass.wallet.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 월렛 송금 내역 화면에 표시할 원장 거래 내역 정보를 반환하는 응답 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletLedgerResponseDto {

    private Long ledgerId;
    private String direction;
    private String transactionType;
    private String transferMethod;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String memo;
    private LocalDateTime createdAt;
    private String accountName;
}
