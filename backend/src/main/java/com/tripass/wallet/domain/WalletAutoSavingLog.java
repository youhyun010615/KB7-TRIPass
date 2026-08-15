package com.tripass.wallet.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 월 목표 자동 채우기가 실행될 때마다 성공/실패 결과를 남기는 실행 기록 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletAutoSavingLog {

    private Long id;
    private Long walletId;
    private String status;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime executedAt;
}
