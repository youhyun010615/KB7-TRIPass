package com.tripass.wallet.dto.response;

import lombok.*;

import java.util.List;

/** 월렛 출금 화면에서 선택할 등록 계좌와 최근 수취계좌를 함께 반환합니다. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletWithdrawOptionsResponseDto {
    private List<WalletAccountResponseDto> registeredAccounts;
    private List<WalletWithdrawRecipientResponseDto> recentAccounts;
}
