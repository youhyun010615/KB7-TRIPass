package com.tripass.travel.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TravelTransactionDto {
    private Long transactionId;
    private Long tripId;
    private String countryName;
    private String categoryName;
    private String merchantName;
    private BigDecimal amount; // 원화 기준
    private BigDecimal originalAmount; // 현지 통화 금액
    private BigDecimal appliedExchangeRate; // 적용 환율
    private String currencySymbol; // 통화 기호
    private LocalDate transactionDate;
    private String transactionType; // DEPOSIT / WITHDRAWAL
}
