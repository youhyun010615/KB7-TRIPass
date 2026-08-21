package com.tripass.ocr.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// OCR 분석 결과를 수정한 후 영수증을 저장하는 요청 DTO
@Getter
@NoArgsConstructor
public class ReceiptSaveRequest {


    // 결제 국가 PK
    @NotNull(message = "결제 국가를 선택해 주세요.")
    private Long countryId;

    // 지출 카테고리 PK (선택)
    private Long categoryId;

    // 결제 통화 코드
    @NotBlank(message = "결제 통화를 선택해 주세요.")
    @Size(
            min = 3,
            max = 3,
            message = "통화 코드는 3자리여야 합니다."
    )
    private String currencyCode;


    // 결제일시
    @NotNull(message = "결제일시를 입력해 주세요.")
    private LocalDateTime paymentDateTime;

    // 영수증 메모
    @Size(
            max = 500,
            message = "메모는 500자 이내로 입력해 주세요."
    )
    private String memo;

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


    // OCR 전체 원문
    private String ocrRawText;


    // 영수증 품목
    @Valid
    private List<ReceiptItemSaveRequest> items = new ArrayList<>();


    // 로그인 회원을 제외한 공동결제 참여자 목록
    @Valid
    @NotNull(
            message = "공동결제 참여자 목록을 확인해 주세요."
    )
    @Size(
            max = 19,
            message = "공동결제 참여자는 최대 19명까지 입력할 수 있습니다."
    )
    private List<ReceiptParticipantSaveRequest> participants =
            new ArrayList<>();

}