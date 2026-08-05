package com.tripass.exchange.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ExchangeRateAlertUpdateResponseDto {
    private Long id;
    private Long userId;
    private String currencyCode;
    private Double targetRate;
    private Double targetAmount;
    private LocalDateTime updatedAt;
}
