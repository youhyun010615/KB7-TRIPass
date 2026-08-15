package com.tripass.wallet.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 매월 지정일에 연동 계좌에서 월렛으로 자동 송금하는 설정 정보를 담는 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletAutoSavingRule {

    private Long id;
    private Long walletId;
    private Long sourceAccountId;
    private BigDecimal amount;
    private Integer dayOfMonth;
    private Boolean enabled;
    private LocalDate nextTransferDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}