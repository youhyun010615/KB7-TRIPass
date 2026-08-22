package com.tripass.wallet.domain;

import lombok.*;

import java.time.LocalDateTime;

/** 사용자가 월렛 출금에 직접 입력한 최근 수취계좌입니다. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletWithdrawRecipient {
    private Long id;
    private Long userId;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private LocalDateTime lastUsedAt;
    private Integer useCount;
}
