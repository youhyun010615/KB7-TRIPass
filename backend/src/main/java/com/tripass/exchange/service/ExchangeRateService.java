package com.tripass.exchange.service;

import com.tripass.exchange.dto.ExchangeRateHistoryResponseDto;
import com.tripass.exchange.dto.ExchangeRateResponseDto;
import com.tripass.exchange.dto.SyncResultDto;
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
        String currencyName = exchangeRateMapper.getCurrencyNameByCode(currencyCode);
        
        ExchangeRateHistoryResponseDto dto = new ExchangeRateHistoryResponseDto();
        dto.setCurrencyCode(currencyCode);
        dto.setCurrencyName(currencyName);
        dto.setRates(rates);
        return dto;
    }

    @Transactional
    public SyncResultDto syncExchangeRates(String date) {
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

    private BigDecimal parseRate(String rateStr) {
        return new BigDecimal(rateStr.replace(",", ""));
    }
}
