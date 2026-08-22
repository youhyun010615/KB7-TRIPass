package com.tripass.wallet.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** 월별 저축 추이 막대 선택 시 정확한 모은 금액, 채우기/빼기 합계와 원장 내역을 반환하는 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletMonthlySavingDetailResponseDto {

    private String month;
    private BigDecimal savedAmount;
    private BigDecimal chargeAmount;
    private BigDecimal withdrawAmount;
    private List<WalletLedgerResponseDto> ledgers;
}
