package com.tripass.ocr.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 영수증 목록 조회용 요약 응답 DTO
@Getter
@AllArgsConstructor
public class ReceiptSummaryResponse {

    // 해외 영수증 PK
    private Long id;

    // 연결된 여행 PK
    private Long tripId;

    private Long countryId;
    private String countryName;

    private Long categoryId;
    private String categoryName;

    // 원문 상호명
    private String merchantOriginalName;

    // 번역된 상호명
    private String merchantTranslatedName;

    // 결제일시
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss"
    )
    private LocalDateTime paymentDateTime;

    // 통화 코드
    private String currencyCode;

    // 통화 기호
    private String currencySymbol;

    // 현지 통화 기준 총액
    private BigDecimal totalAmount;

    // 금액 분할 인원수
    private Integer splitCount;

    // 한 명당 분할 금액
    private BigDecimal splitAmount;

    // 영수증 이미지 접근 경로
    private String fileUrl;

    // 공동결제 참여자 이름 (쉼표 구분)
    private String participantNames;
}