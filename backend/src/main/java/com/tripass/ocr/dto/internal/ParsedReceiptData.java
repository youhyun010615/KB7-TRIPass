package com.tripass.ocr.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

//OCR 원문에서 구조화한 영수증 내부 데이터
@Getter
@AllArgsConstructor
public class ParsedReceiptData {

    //원문 상호명
    private String originalMerchantName;

    //결제일시
    private LocalDateTime paymentDateTime;

    //ISO 4217 통화 코드
    private String currencyCode;

    //현지 통화 기준 총 결제 금액
    private BigDecimal totalAmount;

    //원문 품목 목록
    private List<ParsedReceiptItem> items;
}
