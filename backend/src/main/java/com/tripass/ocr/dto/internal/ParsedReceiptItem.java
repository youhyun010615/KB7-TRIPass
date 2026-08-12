package com.tripass.ocr.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

//OCR 원문에서 추출한 영수증 품목 정보
@Getter
@AllArgsConstructor
public class ParsedReceiptItem {

    //원문 품목명
    private String originalName;

    //구매 수량
    private Integer quantity;

    //현지 통화 기준 품목 금액
    private BigDecimal amount;

    //영수증에 표시된 순서
    private int displayOrder;
}
