package com.tripass.financial.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TravelCardListResponseDto {

    private Long id;
    private String cardName;
    private String cardCompany;
    private String bankName;
    private boolean instantUse;
    private String appliedRateInfo;
    private String exchangeFee;
    private String paymentFee;
    private boolean autoChargeSupported;
    private boolean transitCard;
    private int supportedCurrencyCount;
}
