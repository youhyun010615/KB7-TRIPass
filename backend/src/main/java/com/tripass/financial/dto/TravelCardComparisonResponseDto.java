package com.tripass.financial.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@ApiModel(description = "트래블카드 비교 조회 응답")
public class TravelCardComparisonResponseDto {

    @ApiModelProperty(value = "트래블카드 ID", example = "1")
    private Long id;

    @ApiModelProperty(
            value = "카드명",
            example = "KB국민 트래블러스 체크카드"
    )
    private String cardName;

    @ApiModelProperty(value = "카드사", example = "KB국민카드")
    private String cardCompany;

    @ApiModelProperty(value = "발급 또는 연계 은행", example = "KB국민은행")
    private String bankName;

    @ApiModelProperty(
            value = "카드 이용에 필요한 계좌 또는 외화 서비스",
            example = "KB Pay 외화머니 또는 KB국민은행 외화통장"
    )
    private String requiredAccount;

    @ApiModelProperty(
            value = "별도 계좌 신규 개설 없이 즉시 사용 가능 여부",
            example = "true"
    )
    private boolean instantUse;

    @ApiModelProperty(
            value = "적용 환율 정보",
            example = "56종 통화 환율우대 100%"
    )
    private String appliedRateInfo;

    @ApiModelProperty(
            value = "해외 결제 통화 처리 방식",
            allowableValues = "DIRECT, USD_CONVERSION",
            example = "DIRECT"
    )
    private String settlementType;

    @ApiModelProperty(
            value = "연결 외화머니 또는 외화계좌의 외화 보유한도",
            example = "미화 환산 기준 최대 USD 50,000"
    )
    private String foreignCurrencyHoldingLimit;

    @ApiModelProperty(
            value = "환전 수수료 정보",
            example = "56종 통화 환율우대 100%"
    )
    private String exchangeFee;

    @ApiModelProperty(
            value = "재환전 수수료 정보",
            example = "환급 시 환율우대 100%"
    )
    private String reExchangeFee;

    @ApiModelProperty(
            value = "해외 결제 수수료 정보",
            example = "해외 가맹점 국제브랜드 및 해외서비스 수수료 면제"
    )
    private String paymentFee;

    @ApiModelProperty(
            value = "해외 ATM 출금 수수료 정보",
            example = "해외 ATM 국제브랜드 및 건당 출금 수수료 면제"
    )
    private String withdrawalFee;

    @ApiModelProperty(value = "자동 충전 지원 여부", example = "false")
    private boolean autoChargeSupported;

    @ApiModelProperty(value = "교통카드 지원 여부", example = "true")
    private boolean transitCard;

    @ApiModelProperty(value = "지원 통화 개수", example = "56")
    private int supportedCurrencyCount;
}