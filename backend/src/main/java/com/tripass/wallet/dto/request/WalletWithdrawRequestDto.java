package com.tripass.wallet.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 사용자가 월렛 잔액을 연동 계좌로 출금할 때 필요한 요청 값을 담는 DTO입니다. */

@Getter
@NoArgsConstructor
public class WalletWithdrawRequestDto {

    private Long targetAccountId;

    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;

    @NotNull(message = "출금 금액은 필수입니다.")
    @DecimalMin(value = "1", message = "출금 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

    @NotBlank(message = "중복 요청 방지 키는 필수입니다.")
    private String idempotencyKey;
}
