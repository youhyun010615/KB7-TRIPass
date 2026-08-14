package com.tripass.ocr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

//OCR로 분석한 영수증 품목 응답 DTO
@Getter
@AllArgsConstructor
public class ReceiptAnalyzeItemResponse {

    //영수증에 적힌 원문 품목명
    private String originalName;

    //한국어로 번역한 품목명
    private String translatedName;

    //구매 수량 - 인식하지 못하면 null
    private Integer quantity;

    //현지 통화 기준 품목 금액
    private BigDecimal amount;

    //영수증에 표시된 순서
    private int displayOrder;
}
