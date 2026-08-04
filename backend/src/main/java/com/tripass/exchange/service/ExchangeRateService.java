package com.tripass.exchange.service;

import com.tripass.exchange.dto.ExchangeRateHistoryResponseDto;
import com.tripass.exchange.dto.ExchangeRateResponseDto;
import com.tripass.exchange.client.ExchangeRateClient;
import com.tripass.exchange.domain.ExchangeRate;
import com.tripass.exchange.dto.ExternalExchangeRateDto;
import com.tripass.exchange.dto.LatestExchangeRateDto;
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

    public List<LatestExchangeRateDto> getLatestRates() {
        return exchangeRateMapper.getLatestRates();
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
    public List<ExchangeRateResponseDto> syncExchangeRates(String date) {
        List<ExchangeRateResponseDto> allSavedRates = new java.util.ArrayList<>();
        
        // 날짜 파싱 (YYYYMMDD 또는 YYYY-MM-DD 지원)
        LocalDate currentDate;
        String normalizedDate = date.replace("-", "");
        try {
            currentDate = LocalDate.of(
                    Integer.parseInt(normalizedDate.substring(0, 4)),
                    Integer.parseInt(normalizedDate.substring(4, 6)),
                    Integer.parseInt(normalizedDate.substring(6, 8))
            );
        } catch (Exception e) {
            log.error("날짜 파싱 실패: {}", date);
            return java.util.Collections.emptyList();
        }

        // 7일치 '영업일' 날짜 루프 (데이터가 존재하는 날짜를 찾을 때까지 거슬러 올라감)
        int syncedDays = 0;
        while (syncedDays < 7) {
            String dateParam = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            int maxRetries = 3;
            int attempt = 0;
            boolean success = false;
            
            while (attempt < maxRetries && !success) {
                try {
                    List<ExternalExchangeRateDto> dtoList = exchangeRateClient.fetchExchangeRates(dateParam);
                    if (dtoList.isEmpty()) {
                        log.warn("데이터 없음(휴일/주말일 가능성) - 날짜: {}", dateParam);
                        success = true; // 데이터를 못 가져오면(주말/휴일) 다음 날짜로 넘어감
                    } else {
                        allSavedRates.addAll(processAndSave(dtoList, currentDate));
                        success = true;
                        syncedDays++; // 데이터를 성공적으로 저장했을 때만 카운트 증가
                    }
                } catch (Exception e) {
                    log.error("동기화 실패(재시도 {}/{}) - 날짜: {}", attempt + 1, maxRetries, dateParam, e);
                    attempt++;
                    if (!success) try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }
            currentDate = currentDate.minusDays(1); // 날짜 하루씩 감소
        }
        return allSavedRates;
    }

    private List<ExchangeRateResponseDto> processAndSave(List<ExternalExchangeRateDto> dtoList, LocalDate rateDate) {
        List<ExchangeRateResponseDto> savedRates = new java.util.ArrayList<>();
        Long krwId = exchangeRateMapper.getCurrencyIdByCode("KRW");

        for (ExternalExchangeRateDto dto : dtoList) {
            try {
                String curCode = extractCurrencyCode(dto.getCurUnit());
                Long targetId = exchangeRateMapper.getCurrencyIdByCode(curCode);

                // 통화 정보가 없으면 동적 추가
                if (targetId == null) {
                    log.info("새로운 통화 등록 시작: {} ({})", curCode, dto.getCurNm());
                    exchangeRateMapper.insertCurrency(curCode, dto.getCurNm());
                    targetId = exchangeRateMapper.getCurrencyIdByCode(curCode);
                    log.info("새로운 통화 등록 완료, 조회된 ID: {}", targetId);
                    
                    if (targetId == null) {
                        log.error("통화 등록 후 ID 조회 실패: {}", curCode);
                        continue;
                    }
                }

                BigDecimal rate = parseRate(dto.getDealBasR());
                int unit = extractUnit(dto.getCurUnit());

                // 정규화 (unit이 1이 아닌 경우 해당 단위로 나누어 1단위로 변환)
                if (unit != 1) {
                    rate = rate.divide(new BigDecimal(unit), 8, RoundingMode.HALF_UP);
                }
                
                // 타겟 ID 유효성 확인
                if (targetId == null) {
                    log.error("통화 ID가 null입니다. curCode: {}", curCode);
                    continue;
                }
                
                // 이전 환율 조회
                BigDecimal prevRate = exchangeRateMapper.getPreviousRate(targetId, rateDate);
                log.debug("이전 환율 조회 결과: targetId={}, date={}, prevRate={}", targetId, rateDate, prevRate);

                exchangeRateMapper.upsertExchangeRate(
                    ExchangeRate.builder()
                        .baseCurrencyId(krwId)
                        .targetCurrencyId(targetId)
                        .currencyUnit(1)
                        .dealBaseRate(rate)
                        .prevRate(prevRate)
                        .rateDate(rateDate)
                        .fetchedAt(LocalDateTime.now())
                        .build()
                );
                
                Long savedId = exchangeRateMapper.findIdByCurrencyAndDate(krwId, targetId, rateDate);
                
                ExchangeRateResponseDto dtoResponse = ExchangeRateResponseDto.builder()
                        .id(savedId)
                        .baseCurrencyId(krwId)
                        .targetCurrencyId(targetId)
                        .currencyUnit(1)
                        .dealBaseRate(rate)
                        .prevRate(prevRate)
                        .rateDate(rateDate)
                        .fetchedAt(LocalDateTime.now())
                        .build();

                savedRates.add(dtoResponse);
            } catch (Exception e) {
                log.error("환율 데이터 가공 실패: {} - 원인: {}", dto.getCurUnit(), e.getMessage(), e);
            }
        }
        log.info("환율 데이터 동기화 완료 (날짜: {})", rateDate);
        return savedRates;
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
