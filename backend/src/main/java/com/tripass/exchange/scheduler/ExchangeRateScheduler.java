package com.tripass.exchange.scheduler;

import com.tripass.exchange.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateService exchangeRateService;

    /**
     * zone = "Asia/Seoul"을 지정하여 서버 시간대와 관계없이 한국 시간 기준으로 동작합니다.
     */
    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Seoul")
    public void dailyExchangeRateSync() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        log.info("정기 환율 동기화 시작 - 날짜: {}", today);
        
        try {
            exchangeRateService.syncExchangeRates(today);
            log.info("정기 환율 동기화 완료");
        } catch (Exception e) {
            log.error("정기 환율 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
