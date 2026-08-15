package com.tripass.travel.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TravelRecentTransactionDto {
    private Long transactionId;
    private String description;
    private Long amount;
    private Long originalAmount;
    private Double appliedExchangeRate;
    private String currency;
    private LocalDateTime transactionDate;
    private String category;
}
