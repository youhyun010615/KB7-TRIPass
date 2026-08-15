package com.tripass.wallet.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 사용자별 원화 월렛의 잔액, 상태, 동시성 버전 정보를 담는 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    private Long id;
    private Long userId;
    private BigDecimal balanceAmount;
    private String status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
