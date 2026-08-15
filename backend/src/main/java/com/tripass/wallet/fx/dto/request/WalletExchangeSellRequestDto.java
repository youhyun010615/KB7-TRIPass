package com.tripass.wallet.fx.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 트래블카드 외화를 원화 월렛으로 재환전하는 요청값을 담는 DTO입니다. */

@Getter
@Setter
public class WalletExchangeSellRequestDto {

    @NotNull(message = "월렛 트래블카드 ID는 필수입니다.")
    private Long walletTravelCardId;

    @NotBlank(message = "통화 코드는 필수입니다.")
    private String currencyCode;

    @NotNull(message = "외화 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "외화 금액은 0보다 커야 합니다.")
    private BigDecimal foreignAmount;

    @NotBlank(message = "멱등키는 필수입니다.")
    private String idempotencyKey;
}
