package com.tripass.wallet.fx.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 환전 예상 금액 계산 요청값을 담는 DTO입니다. */

@Getter
@Setter
public class WalletExchangeEstimateRequestDto {

    @NotBlank(message = "환전 유형은 필수입니다.")
    private String exchangeType;

    @NotBlank(message = "통화 코드는 필수입니다.")
    private String currencyCode;

    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다.")
    private BigDecimal amount;
}
