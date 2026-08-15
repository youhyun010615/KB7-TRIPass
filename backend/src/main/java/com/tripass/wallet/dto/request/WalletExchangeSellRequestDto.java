package com.tripass.wallet.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 트래블카드 외화를 원화로 재환전해 월렛에 입금할 때 필요한 요청 DTO입니다. */

@Getter
@NoArgsConstructor
public class WalletExchangeSellRequestDto {

    @NotNull(message = "월렛 트래블카드 연동 ID는 필수입니다.")
    private Long walletTravelCardId;

    @NotBlank(message = "통화 코드는 필수입니다.")
    private String currencyCode;

    @NotNull(message = "재환전 외화 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "재환전 외화 금액은 0.01 이상이어야 합니다.")
    private BigDecimal foreignAmount;

    @NotBlank(message = "중복 요청 방지 키는 필수입니다.")
    private String idempotencyKey;
}
