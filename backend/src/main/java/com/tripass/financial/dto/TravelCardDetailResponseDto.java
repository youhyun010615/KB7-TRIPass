package com.tripass.financial.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TravelCardDetailResponseDto {

    private Long id;
    private String cardName;
    private String cardCompany;
    private String bankName;
    private String requiredAccount;
    private boolean instantUse;
    private String appliedRateInfo;
    // 해외 결제 통화 처리 방식(DIRECT/USD_CONVERSION)
    private String settlementType;
    // 연결 외화머니/외화계좌의 외화 보유한도
    private String foreignCurrencyHoldingLimit;
    private String exchangeFee;
    private String reExchangeFee;
    private String paymentFee;
    private String withdrawalFee;
    private boolean autoChargeSupported;
    private boolean transitCard;
    private List<String> supportedCurrencies;
}
