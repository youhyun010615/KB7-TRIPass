package com.tripass.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** 월렛 메인 화면에 필요한 잔액, 목표, 월별 저축 추이, 카드 연동 정보를 반환하는 응답 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletMainResponseDto {

    private Long walletId;
    private BigDecimal balanceAmount;
    private BigDecimal totalLinkedAccountBalance;
    private BigDecimal targetAmount;
    private BigDecimal emergencyAmount;
    private Integer savingRate;
    private WalletPrimaryAccountResponseDto primaryAccount;
    private List<WalletMonthlySavingResponseDto> monthlySavings;
    private WalletLinkedTravelCardResponseDto travelCard;
    private List<WalletForeignBalanceResponseDto> foreignBalances;
}
