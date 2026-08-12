package com.tripass.ocr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

// 저장된 영수증 품목 응답 DTO
@Getter
@AllArgsConstructor
public class ReceiptItemResponse {

    // 영수증 품목 PK
    private Long id;

    // 원문 품목명
    private String originalName;

    // 번역된 품목명
    private String translatedName;

    // 구매 수량
    private Integer quantity;

    // 현지 통화 기준 품목 금액
    private BigDecimal amount;

    // 품목 표시 순서
    private Integer displayOrder;
}