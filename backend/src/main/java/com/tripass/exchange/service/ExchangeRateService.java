package com.tripass.exchange.service;

import com.tripass.exchange.dto.*;
import com.tripass.exchange.client.ExchangeRateClient;
import com.tripass.exchange.domain.ExchangeRate;
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
        String currencyName = exchangeRateMapper.getCurrencyNameByCode(currencyCode);
        
        ExchangeRateHistoryResponseDto dto = new ExchangeRateHistoryResponseDto();
        dto.setCurrencyCode(currencyCode);
        dto.setCurrencyName(currencyName);
        dto.setRates(rates);
        return dto;
    }

    @Transactional
    public SyncResultDto syncExchangeRates(String date) {
        // KRW 존재 여부 확인 및 생성
        ensureKrwExists();

        // 날짜 파싱
        LocalDate endDate;
        String normalizedDate = date.replace("-", "");
        try {
            endDate = LocalDate.of(
                    Integer.parseInt(normalizedDate.substring(0, 4)),
                    Integer.parseInt(normalizedDate.substring(4, 6)),
                    Integer.parseInt(normalizedDate.substring(6, 8))
            );
        } catch (Exception e) {
            log.error("날짜 파싱 실패: {}", date);
            return null; // 또는 적절한 에러 처리
        }
        
        LocalDate startDate = endDate.minusDays(89); // 90일 범위 계산
        
        // 통계용 맵 (CurrencyCode -> SavedDays)
        java.util.Map<String, Integer> currencySyncMap = new java.util.HashMap<>();
        int totalSavedCount = 0;
        
        // 90일치 '달력' 날짜 루프
        LocalDate currentDate = endDate;
        for (int i = 0; i < 90; i++) {
            String dateParam = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            try {
                List<ExternalExchangeRateDto> dtoList = exchangeRateClient.fetchExchangeRates(dateParam);
                if (!dtoList.isEmpty()) {
                    List<ExchangeRateResponseDto> savedRates = processAndSave(dtoList, currentDate);
                    totalSavedCount += savedRates.size();
                    
                    // 통화별 카운트 업데이트
                    for (ExchangeRateResponseDto rate : savedRates) {
                        String code = exchangeRateMapper.getCurrencyCodeById(rate.getTargetCurrencyId());
                        currencySyncMap.put(code, currencySyncMap.getOrDefault(code, 0) + 1);
                    }
                }
            } catch (Exception e) {
                log.error("동기화 실패 - 날짜: {}", dateParam, e);
            }
            currentDate = currentDate.minusDays(1);
        }
        
        // 결과 DTO 구성
        List<SyncResultDto.CurrencySyncStatus> statusList = currencySyncMap.entrySet().stream()
                .map(entry -> SyncResultDto.CurrencySyncStatus.builder()
                        .currencyCode(entry.getKey())
                        .savedDays(entry.getValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return SyncResultDto.builder()
                .syncPeriod(startDate.toString() + " ~ " + endDate.toString())
                .totalSavedCount(totalSavedCount)
                .syncedAt(LocalDateTime.now())
                .syncedCurrencies(statusList)
                .build();
    }

    private void ensureKrwExists() {
        Long krwId = exchangeRateMapper.getCurrencyIdByCode("KRW");
        if (krwId == null) {
            log.info("KRW 통화 정보가 없어 새로 등록합니다.");
            exchangeRateMapper.insertCurrency("KRW", "대한민국 원");
        }
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

                ExchangeRate exchangeRate = new ExchangeRate();
                exchangeRate.setBaseCurrencyId(krwId);
                exchangeRate.setTargetCurrencyId(targetId);
                exchangeRate.setCurrencyUnit(1);
                exchangeRate.setDealBaseRate(rate);
                exchangeRate.setPrevRate(prevRate);
                exchangeRate.setRateDate(rateDate);
                exchangeRate.setFetchedAt(LocalDateTime.now());
                
                exchangeRateMapper.upsertExchangeRate(exchangeRate);
                
                Long savedId = exchangeRateMapper.findIdByCurrencyAndDate(krwId, targetId, rateDate);
                
                ExchangeRateResponseDto dtoResponse = new ExchangeRateResponseDto();
                dtoResponse.setId(savedId);
                dtoResponse.setBaseCurrencyId(krwId);
                dtoResponse.setTargetCurrencyId(targetId);
                dtoResponse.setCurrencyUnit(1);
                dtoResponse.setDealBaseRate(rate);
                dtoResponse.setPrevRate(prevRate);
                dtoResponse.setRateDate(rateDate);
                dtoResponse.setFetchedAt(LocalDateTime.now());

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

    public ExchangeRateConvertResponseDto convertCurrency(String fromCurrency, String toCurrency, double amount) {
        if (fromCurrency == null || fromCurrency.isEmpty() || 
            toCurrency == null || toCurrency.isEmpty() || 
            amount <= 0) {
            throw new IllegalArgumentException("필수 파라미터가 누락되었거나 금액이 0 이하입니다.");
        }

        if (fromCurrency.equals(toCurrency)) {
            return ExchangeRateConvertResponseDto.builder()
                    .fromAmount(amount)
                    .fromCurrency(fromCurrency)
                    .toAmount(amount)
                    .toCurrency(toCurrency)
                    .appliedRate(1.0)
                    .build();
        }

        BigDecimal rate = getConversionRate(fromCurrency, toCurrency);
        double convertedAmount = BigDecimal.valueOf(amount)
                .multiply(rate)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        return ExchangeRateConvertResponseDto.builder()
                .fromAmount(amount)
                .fromCurrency(fromCurrency)
                .toAmount(convertedAmount)
                .toCurrency(toCurrency)
                .appliedRate(rate.doubleValue())
                .build();
    }

    private BigDecimal getConversionRate(String fromCurrency, String toCurrency) {
        // ... (환율 조회 로직)
        // 1. 만약 from이 KRW이면: 1 / (to의 환율)
        // 2. 만약 to가 KRW이면: (from의 환율)
        // 3. 둘 다 KRW가 아니면: (from의 환율) / (to의 환율)
        // 실제로는 exchange_rates에 KRW(base) -> 타겟(target) 데이터만 있음
        
        BigDecimal fromRate = getRateToKrw(fromCurrency);
        BigDecimal toRate = getRateToKrw(toCurrency);
        
        return fromRate.divide(toRate, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal getRateToKrw(String currencyCode) {
        if ("KRW".equals(currencyCode)) return BigDecimal.ONE;
        
        List<LatestExchangeRateDto> latestRates = exchangeRateMapper.getLatestRates();
        return latestRates.stream()
                .filter(rate -> rate.getCurrencyCode().equals(currencyCode))
                .map(LatestExchangeRateDto::getDealBaseRate)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 통화입니다: " + currencyCode));
    }

    private BigDecimal parseRate(String rateStr) {
        return new BigDecimal(rateStr.replace(",", ""));
    }
}