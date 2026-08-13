package com.tripass.ocr.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 영수증 저장 결과 및 상세 조회 응답 DTO
@Getter
@AllArgsConstructor
public class ReceiptDetailResponse {

    // 해외 영수증 PK
    private Long id;

    // 연결된 여행 PK
    private Long tripId;

    // 결제 국가 PK
    private Long countryId;

    // 결제 국가명
    private String countryName;

    // 지출 카테고리 PK
    private Long categoryId;

    // 지출 카테고리 이름
    private String categoryName;

    // 결제 통화 PK
    private Long currencyId;

    // 통화 코드
    private String currencyCode;

    // 통화명
    private String currencyName;

    // 통화 기호
    private String currencySymbol;

    // 결제일시
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss"
    )
    private LocalDateTime paymentDateTime;

    // 원본 파일명
    private String fileName;

    // 영수증 이미지 접근 경로
    private String fileUrl;

    // 파일 형식
    private String fileType;

    // 영수증 메모
    private String memo;

    // 처리 상태
    private String status;

    // 원문 상호명
    private String merchantOriginalName;

    // 번역된 상호명
    private String merchantTranslatedName;

    // 현지 통화 기준 총액
    private BigDecimal totalAmount;

    // 금액 분할 인원수
    private Integer splitCount;

    // 한 명당 분할 금액
    private BigDecimal splitAmount;

    // OCR 전체 원문
    private String ocrRawText;

    // 영수증 품목 목록
    private List<ReceiptItemResponse> items;

    // 로그인 회원을 제외한 공동결제 참여자 목록
    private List<ReceiptParticipantResponse> participants;

    // 생성일시
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss"
    )
    private LocalDateTime createdAt;

    // 수정일시
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss"
    )
    private LocalDateTime updatedAt;
}