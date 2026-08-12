package com.tripass.ocr.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// OCR 분석 결과를 수정한 후 영수증을 저장하는 요청 DTO
@Getter
@NoArgsConstructor
public class ReceiptSaveRequest {

    // 영수증을 연결할 여행 PK
    private Long tripId;

    // 결제 국가 PK
    private Long countryId;

    // 결제 통화 PK
    @NotNull(message = "결제 통화를 선택해 주세요.")
    private Long currencyId;

    // 결제일시
    @NotNull(message = "결제일시를 입력해 주세요.")
    private LocalDateTime paymentDateTime;

    // 원문 상호명
    @Size(max = 255, message = "원문 상호명은 255자 이하로 입력해 주세요.")
    private String merchantOriginalName;

    // 번역된 상호명
    @Size(max = 255, message = "번역 상호명은 255자 이하로 입력해 주세요.")
    private String merchantTranslatedName;

    // 현지 통화 기준 총 결제금액
    @NotNull(message = "총 결제금액을 입력해 주세요.")
    @DecimalMin(
            value = "0.01",
            message = "총 결제금액은 0보다 커야 합니다."
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "총 결제금액은 정수 13자리, 소수 2자리 이하여야 합니다."
    )
    private BigDecimal totalAmount;

    // 현지 통화 기준 세금
    @DecimalMin(
            value = "0.00",
            message = "세금은 0 이상이어야 합니다."
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "세금은 정수 13자리, 소수 2자리 이하여야 합니다."
    )
    private BigDecimal taxAmount;

    // OCR 전체 원문
    private String ocrRawText;

    // 금액 분할 인원수
    @NotNull(message = "분할 인원수를 입력해 주세요.")
    @Min(
            value = 1,
            message = "분할 인원수는 1명 이상이어야 합니다."
    )
    private Integer splitCount;

    // 영수증 품목
    @Valid
    private List<ReceiptItemSaveRequest> items =
            new ArrayList<>();
}