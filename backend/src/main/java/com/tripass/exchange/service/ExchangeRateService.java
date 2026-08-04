package com.tripass.exchange.service;

import com.tripass.exchange.dto.ExchangeRateHistoryResponseDto;
import com.tripass.exchange.dto.ExchangeRateResponseDto;
import com.tripass.exchange.client.ExchangeRateClient;
import com.tripass.exchange.domain.ExchangeRate;
import com.tripass.exchange.dto.ExternalExchangeRateDto;
import com.tripass.exchange.mapper.ExchangeRateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateClient exchangeRateClient;
    private final ExchangeRateMapper exchangeRateMapper;

    public List<ExchangeRateResponseDto> getLatestRates(List<String> currencyCodes) {
        return exchangeRateMapper.getLatestRates(currencyCodes);
    }

    public ExchangeRateHistoryResponseDto getHistoryRates(String currencyCode, int days) {
        List<ExchangeRateHistoryResponseDto.RateInfo> rates = exchangeRateMapper.getHistoryRates(currencyCode, days);
        // 통화명 등을 조회하는 로직은 추가 가능 (현재는 코드만)
        return ExchangeRateHistoryResponseDto.builder()
                .currencyCode(currencyCode)
                .rates(rates)
                .build();
    }

    @Transactional
    public void syncExchangeRates(String date) {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                List<ExternalExchangeRateDto> dtoList = exchangeRateClient.fetchExchangeRates(date);
                if (dtoList.isEmpty()) {
                    log.warn("데이터 없음(재시도 {}/{})", attempt + 1, maxRetries);
                } else {
                    processAndSave(dtoList, date);
                    return; // 성공 시 종료
                }
            } catch (Exception e) {
                log.error("동기화 실패(재시도 {}/{})", attempt + 1, maxRetries, e);
            }
            
            attempt++;
            try { Thread.sleep(10000); } catch (InterruptedException ignored) {} // 10초 대기
        }
    }

    private void processAndSave(List<ExternalExchangeRateDto> dtoList, String date) {
        Long krwId = exchangeRateMapper.getCurrencyIdByCode("KRW");

        for (ExternalExchangeRateDto dto : dtoList) {
            try {
                String curCode = extractCurrencyCode(dto.getCurUnit());
                Long targetId = exchangeRateMapper.getCurrencyIdByCode(curCode);

                if (targetId == null) continue;

                BigDecimal rate = parseRate(dto.getDealBasR());
                int unit = extractUnit(dto.getCurUnit());

                // 정규화 (unit이 1이 아닌 경우 해당 단위로 나누어 1단위로 변환)
                if (unit != 1) {
                    rate = rate.divide(new BigDecimal(unit), 8, RoundingMode.HALF_UP);
                }

                ExchangeRate entity = ExchangeRate.builder()
                        .baseCurrencyId(krwId)
                        .targetCurrencyId(targetId)
                        .currencyUnit(1) // DB에는 항상 정규화된 1단위로 저장
                        .dealBaseRate(rate)
                        .rateDate(LocalDate.parse(date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8)))
                        .fetchedAt(LocalDateTime.now())
                        .build();

                exchangeRateMapper.upsertExchangeRate(entity);
            } catch (Exception e) {
                log.error("환율 데이터 가공 실패: {}", dto.getCurUnit(), e);
            }
        }
        log.info("환율 데이터 동기화 완료 (날짜: {})", date);
    }

    private String extractCurrencyCode(String curUnit) {
        // "JPY(100)" -> "JPY"
        return curUnit.contains("(") ? curUnit.substring(0, curUnit.indexOf("(")) : curUnit;
    }

    private int extractUnit(String curUnit) {
        // "JPY(100)" -> 100, "USD" -> 1
        if (!curUnit.contains("(") || !curUnit.contains(")")) return 1;
        
        String unitStr = curUnit.substring(curUnit.indexOf("(") + 1, curUnit.indexOf(")"));
        try {
            return Integer.parseInt(unitStr);
        } catch (NumberFormatException e) {
            log.warn("통화 단위 파싱 실패: {}, 기본값 1 적용", curUnit);
            return 1;
        }
    }

    private BigDecimal parseRate(String rateStr) {
        return new BigDecimal(rateStr.replace(",", ""));
    }
}
