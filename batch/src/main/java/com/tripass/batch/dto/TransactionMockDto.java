package com.tripass.batch.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class TransactionMockDto {
    private Long accountId;
    private Long categoryId;
    private Long tripId;
    private Long tripCountryId;
    private Long currencyId;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private String transactionType;   // DEPOSIT, WITHDRAWAL
    private String transactionRegion; // DOMESTIC, OVERSEAS
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String merchantName;
    private BigDecimal originalAmount;
    private BigDecimal appliedExchangeRate;
    private String paymentMethod;
    private Boolean isPreExpense;
    private String memo;
    private String externalKey;
    private Byte isDeleted;
}