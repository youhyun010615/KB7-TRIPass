package com.tripass.ocr.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

// 영수증 품목 저장 요청 DTO
@Getter
@NoArgsConstructor
public class ReceiptItemSaveRequest {


    // 기존 품목 PK
    // 신규 품목이면 null
    private Long id;

    // 원문 품목명
    @NotBlank(message = "원문 품목명을 입력해 주세요.")
    @Size(max = 255, message = "품목명은 255자 이하로 입력해 주세요.")
    private String originalName;

    // 번역된 품목명
    @Size(max = 255, message = "번역 품목명은 255자 이하로 입력해 주세요.")
    private String translatedName;

    // 구매 수량
    @Min(
            value = 1,
            message = "품목 수량은 1개 이상이어야 합니다."
    )
    private Integer quantity;

    // 현지 통화 기준 품목 금액
    @DecimalMin(
            value = "0.00",
            message = "품목 금액은 0 이상이어야 합니다."
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "품목 금액은 정수 13자리, 소수 2자리 이하여야 합니다."
    )
    private BigDecimal amount;
}