package com.tripass.wallet.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** 월렛 계좌 연동 화면에서 표시할 계좌 정보와 연동 상태를 반환하는 응답 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletAccountResponseDto {

    private Long accountId;
    private String bankName;
    private String accountName;
    private String maskedAccountNumber;
    private BigDecimal balance;
    private BigDecimal withdrawableAmount;
    private Boolean linked;
    private Boolean primary;
}
