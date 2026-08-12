package com.tripass.ocr.dto.internal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 영수증 상세 조회용 DB 조회 결과
@Getter
@Setter
@NoArgsConstructor
public class ReceiptDetailRow {

    private Long id;
    private Long tripId;

    private Long countryId;
    private String countryName;

    private Long currencyId;
    private String currencyCode;
    private String currencyName;
    private String currencySymbol;

    private LocalDateTime paymentDateTime;

    private String fileName;
    private String fileUrl;
    private String fileType;
    private String status;

    private String merchantOriginalName;
    private String merchantTranslatedName;

    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private Integer splitCount;

    private String ocrRawText;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}