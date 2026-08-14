package com.tripass.ocr.dto.internal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 영수증 목록 조회 DB 조회 결과
@Getter
@Setter
@NoArgsConstructor
public class ReceiptSummaryRow {

    private Long id;
    private Long tripId;

    private Long countryId;
    private String countryName;

    private Long categoryId;
    private String categoryName;

    private String merchantOriginalName;
    private String merchantTranslatedName;

    private LocalDateTime paymentDateTime;

    private String currencyCode;
    private String currencySymbol;

    private BigDecimal totalAmount;
    private Integer splitCount;

    private String fileUrl;
}