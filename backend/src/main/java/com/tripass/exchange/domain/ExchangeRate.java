package com.tripass.exchange.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ExchangeRate {
    private Long id;
    private Long baseCurrencyId;   // KRW ID
    private Long targetCurrencyId; // 대상 통화 ID
    private Integer currencyUnit;  // 정규화된 단위 (보통 1)
    private BigDecimal dealBaseRate;
    private BigDecimal prevRate;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate rateDate;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fetchedAt;

    @Override
    public String toString() {
        return "ExchangeRate{" +
                "baseCurrencyId=" + baseCurrencyId +
                ", targetCurrencyId=" + targetCurrencyId +
                ", currencyUnit=" + currencyUnit +
                ", dealBaseRate=" + dealBaseRate +
                ", rateDate=" + rateDate +
                ", fetchedAt=" + fetchedAt +
                '}';
    }
}
