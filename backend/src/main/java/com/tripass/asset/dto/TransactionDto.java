/**
 * Codef 수시입출 거래내역 조회 결과를 담는 DTO
 * transactions 테이블에 저장할 데이터 구조
 */

package com.tripass.asset.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class TransactionDto {

    private Long id;
    private Long accountId;
    private Long cardId;
    private String sourceCardType;   // 전체 거래 조회 시 중복 제거용 CREDIT / CHECK
    private String externalKey;       // 중복 수집 방지 키 (계좌: ACC:계좌ID:날짜:시각:금액, 카드: CARD:카드ID:승인번호 또는 날짜:시각:금액)
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private String transactionType;   // DEPOSIT / WITHDRAWAL
    private String transactionRegion; // DOMESTIC / OVERSEAS
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String merchantName;      // 거래처
    private String merchantType;      // CODEF 가맹점 업종
    private String paymentMethodName; // 거래 상세의 계좌명 또는 카드명
    private String memo;
    private Long categoryId;
    private String categoryName;
    private String categorySource;
    private BigDecimal categoryConfidence;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime categoryClassifiedAt;
}
