package com.tripass.wallet.travelcard.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 월렛 원화를 환전해 트래블카드 외화로 충전하는 요청값을 담는 DTO입니다. */

@Getter
@Setter
public class WalletTravelCardTopupRequestDto {

    @NotNull(message = "월렛 트래블카드 ID는 필수입니다.")
    private Long walletTravelCardId;

    @NotBlank(message = "통화 코드는 필수입니다.")
    private String currencyCode;

    @NotNull(message = "원화 금액은 필수입니다.")
    @DecimalMin(value = "1.00", message = "원화 금액은 1원 이상이어야 합니다.")
    private BigDecimal krwAmount;

    @NotBlank(message = "멱등키는 필수입니다.")
    private String idempotencyKey;
}
