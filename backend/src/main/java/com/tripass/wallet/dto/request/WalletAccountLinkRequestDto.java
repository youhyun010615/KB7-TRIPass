package com.tripass.wallet.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/** 사용자가 월렛에 계좌를 연동할 때 필요한 계좌 ID와 대표 계좌 여부를 담는 DTO입니다. */

@Getter
@NoArgsConstructor
public class WalletAccountLinkRequestDto {

    @NotNull(message = "계좌 ID는 필수입니다.")
    private Long accountId;

    private Boolean isPrimary;
}