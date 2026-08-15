package com.tripass.wallet.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** 월렛 충전 또는 출금 처리 후 변경된 월렛 ID와 잔액을 반환하는 응답 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletCommandResponseDto {

    private Long walletId;
    private BigDecimal balanceAmount;
}
