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
    // 해외 결제 통화 처리 방식(DIRECT/USD_CONVERSION)
    private String settlementType;
    private String exchangeFee;
    private String paymentFee;
    private boolean autoChargeSupported;
    private boolean transitCard;
    private int supportedCurrencyCount;
}
