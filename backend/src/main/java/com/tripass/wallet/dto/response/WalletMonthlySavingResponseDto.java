package com.tripass.wallet.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** 월렛 메인 화면의 월별 저축 추이 그래프에 사용할 월별 모은 금액 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletMonthlySavingResponseDto {

    private String month;
    private BigDecimal savedAmount;
    private Boolean currentMonth;
    private Boolean available;
}
