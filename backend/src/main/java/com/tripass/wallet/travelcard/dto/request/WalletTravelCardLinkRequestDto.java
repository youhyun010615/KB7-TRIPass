package com.tripass.wallet.travelcard.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 월렛에 트래블카드를 연동할 때 필요한 카드 ID와 마스킹 카드번호를 담는 DTO입니다. */

@Getter
@Setter
public class WalletTravelCardLinkRequestDto {

    @NotNull(message = "트래블카드 ID는 필수입니다.")
    private Long travelCardId;

    private Long userTravelCardId;

    @Size(max = 50, message = "마스킹 카드번호는 50자 이하여야 합니다.")
    private String maskedCardNumber;
}
