package com.tripass.wallet.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/** 월 목표 자동 송금 설정에 필요한 금액, 송금일, 활성 여부를 담는 DTO입니다. 출금 계좌는 항상 주계좌로 고정됩니다. */

@Getter
@NoArgsConstructor
public class WalletAutoSavingRequestDto {

    @NotNull(message = "자동 송금 금액은 필수입니다.")
    @DecimalMin(value = "1", message = "자동 송금 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

    @NotNull(message = "자동 송금일은 필수입니다.")
    @Min(value = 1, message = "자동 송금일은 1일 이상이어야 합니다.")
    @Max(value = 28, message = "자동 송금일은 28일 이하로 설정해주세요.")
    private Integer dayOfMonth;

    @NotNull(message = "활성 여부는 필수입니다.")
    private Boolean enabled;
}