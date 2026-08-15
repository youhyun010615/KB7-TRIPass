package com.tripass.wallet.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 월렛 원화로 트래블카드 외화를 충전할 때 필요한 요청 DTO입니다. */

@Getter
@NoArgsConstructor
public class WalletTravelCardTopupRequestDto {

    @NotNull(message = "월렛 트래블카드 연동 ID는 필수입니다.")
    private Long walletTravelCardId;

    @NotBlank(message = "통화 코드는 필수입니다.")
    private String currencyCode;

    @NotNull(message = "충전 원화 금액은 필수입니다.")
    @DecimalMin(value = "1", message = "충전 원화 금액은 1원 이상이어야 합니다.")
    private BigDecimal krwAmount;

    @NotBlank(message = "중복 요청 방지 키는 필수입니다.")
    private String idempotencyKey;
}
