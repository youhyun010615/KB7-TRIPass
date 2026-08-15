package com.tripass.wallet.domain;

import lombok.*;

import java.time.LocalDateTime;

/** 월렛과 사용자의 연동 계좌 관계 및 대표 계좌 여부를 담는 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletAccount {

    private Long id;
    private Long walletId;
    private Long accountId;
    private Boolean isPrimary;
    private String status;
    private LocalDateTime linkedAt;
    private LocalDateTime unlinkedAt;
}