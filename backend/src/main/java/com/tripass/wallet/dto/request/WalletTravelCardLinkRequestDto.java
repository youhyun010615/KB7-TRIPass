package com.tripass.wallet.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/** 월렛에 트래블카드를 연동할 때 필요한 카드 ID와 마스킹 카드번호를 담는 요청 DTO입니다. */

@Getter
@NoArgsConstructor
public class WalletTravelCardLinkRequestDto {

    @NotNull(message = "트래블카드 ID는 필수입니다.")
    private Long travelCardId;

    private String maskedCardNumber;
}
