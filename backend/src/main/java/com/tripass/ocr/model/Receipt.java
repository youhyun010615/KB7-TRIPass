package com.tripass.ocr.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 해외 영수증 모델
@Getter
@Setter
@NoArgsConstructor
public class Receipt {

    // 해외 영수증 PK
    private Long id;

    // 영수증을 등록한 회원 PK
    private Long userId;

    // 영수증이 포함된 여행 PK
    private Long tripId;

    // 결제 국가 PK
    private Long countryId;

    // 결제 통화 PK
    private Long currencyId;

    // 영수증 결제일시
    private LocalDateTime paymentDateTime;

    // 업로드한 원본 파일명
    private String fileName;

    // 서버에 저장된 파일 접근 경로
    private String fileUrl;

    // 파일 형식
    private String fileType;

    // 영수증 처리 상태
    private String status;

    // OCR로 인식한 원문 상호명
    private String merchantOriginalName;

    // 한국어로 번역한 상호명
    private String merchantTranslatedName;

    // 현지 통화 기준 총액
    private BigDecimal totalAmount;

    // 현지 통화 기준 세금
    private BigDecimal taxAmount;

    // OCR 전체 원문
    private String ocrRawText;

    // 금액 분할 인원수
    private Integer splitCount;

    // OCR 처리 실패 메시지
    private String errorMessage;

    // 논리 삭제 여부
    private Boolean isDeleted;

    // 논리 삭제일시
    private LocalDateTime deletedAt;

    // 생성일시
    private LocalDateTime createdAt;

    // 수정일시
    private LocalDateTime updatedAt;

    // OCR 처리 완료일시
    private LocalDateTime processedAt;
}