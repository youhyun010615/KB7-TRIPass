package com.tripass.wallet.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/** 월렛 출금 화면에 노출할 최근 수취계좌입니다. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletWithdrawRecipientResponseDto {
    private Long recipientId;
    private String bankCode;
    private String bankName;
    private String maskedAccountNumber;
    private String accountHolderName;
    private LocalDateTime lastUsedAt;
    private Integer useCount;
}
