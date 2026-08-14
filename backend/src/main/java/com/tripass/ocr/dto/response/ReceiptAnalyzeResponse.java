package com.tripass.ocr.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;



//해외 영수증 OCR 분석 결과 응답 DTO
@Getter
@AllArgsConstructor
public class ReceiptAnalyzeResponse {

    //Vision API가 감지한 원문 언어 코드
    private String detectedLanguageCode;

    //영수증에 적힌 원문 상호명
    private String originalMerchantName;

    //한국어로 번역한 상호명
    private String translatedMerchantName;

    //결제일시 - 인식하지 못하면 null
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss"
    )
    private LocalDateTime paymentDateTime;

    //ISO 4217 통화 코드 - USD, JPY, EUR 등
    private String currencyCode;

    //현지 통화 기준 총 결제 금액
    private BigDecimal totalAmount;


    //금액 분할 인원수 - 분석 직후 기본값 1
    private int splitCount;

    //1인당 금액 - 총액을 인식하지 못하면 null
    private BigDecimal splitAmount;

    //분석한 품목 목록
    private List<ReceiptAnalyzeItemResponse> items;

    //OCR로 인식한 전체 원문
    private String rawText;
}
