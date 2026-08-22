package com.tripass.exchange.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class SyncResultDto {
    private String syncPeriod;
    private int totalSavedCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime syncedAt;
    private List<CurrencySyncStatus> syncedCurrencies;

    @Getter
    @Setter
    @Builder
    public static class CurrencySyncStatus {
        private String currencyCode;
        private int savedDays;
    }
}
