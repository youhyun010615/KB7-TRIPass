package com.tripass.ocr.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 영수증 품목 모델
@Getter
@Setter
@NoArgsConstructor
public class ReceiptItem {

    // 영수증 품목 PK
    private Long id;

    // 품목이 포함된 영수증 PK
    private Long receiptId;

    // OCR로 인식한 원문 품목명
    private String originalName;

    // 한국어로 번역한 품목명
    private String translatedName;

    // 구매 수량
    private Integer quantity;

    // 현지 통화 기준 품목 금액
    private BigDecimal amount;

    // 품목 표시 순서
    private Integer displayOrder;

    // 논리 삭제 여부
    private Boolean isDeleted;

    // 논리 삭제일시
    private LocalDateTime deletedAt;

    // 생성일시
    private LocalDateTime createdAt;

    // 수정일시
    private LocalDateTime updatedAt;
}