package com.tripass.exchange.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {
    private Long id;
    private Long baseCurrencyId;   // KRW ID
    private Long targetCurrencyId; // 대상 통화 ID
    private Integer currencyUnit;  // 정규화된 단위 (보통 1)
    private BigDecimal dealBaseRate;
    private BigDecimal prevRate;
    private LocalDate rateDate;
    private LocalDateTime fetchedAt;
}
