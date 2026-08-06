package com.tripass.exchange.service;

import com.tripass.exchange.domain.ExchangeRateAlert;
import com.tripass.exchange.dto.*;
import com.tripass.exchange.client.ExchangeRateClient;
import com.tripass.exchange.domain.ExchangeRate;
import com.tripass.exchange.exception.ExchangeErrorCode;
import com.tripass.exchange.exception.ExchangeException;
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

    public List<ExchangeRateAlertResponseDto> getAlertsByUserId(Long userId) {
        return exchangeRateMapper.getAlertsByUserId(userId);
    }

    public Long registerAlert(Long userId, ExchangeRateAlertRequestDto request) {
        if (request.getCurrencyId() == null || request.getTargetAmount() <= 0) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_INPUT_VALUE);
        }

        // 통화 존재 여부 확인
        if (!exchangeRateMapper.existsCurrencyById(request.getCurrencyId())) {
            throw new ExchangeException(ExchangeErrorCode.RATE_NOT_FOUND);
        }

        int count = exchangeRateMapper.countAlertByUserAndCurrency(userId, request.getCurrencyId());

        if (count > 0) {
            throw new ExchangeException(ExchangeErrorCode.DUPLICATE_ALERT);
        }
        
        ExchangeRateAlert alert = new ExchangeRateAlert();
        alert.setUserId(userId);
        alert.setCurrencyId(request.getCurrencyId());
        alert.setTargetRate(request.getTargetRate());
        alert.setTargetAmount(request.getTargetAmount());
        
        exchangeRateMapper.insertAlert(alert);
        
        return alert.getId();
    }

    @Transactional
    public ExchangeRateAlertUpdateResponseDto updateAlert(Long id, Long userId, ExchangeRateAlertUpdateRequestDto request) {

        ExchangeRateAlertUpdateResponseDto existingAlert = exchangeRateMapper.getAlertById(id);
        if (existingAlert == null) {
            throw new ExchangeException(ExchangeErrorCode.ALERT_NOT_FOUND);
        }

        if (!existingAlert.getUserId().equals(userId)) {
            throw new ExchangeException(ExchangeErrorCode.FORBIDDEN_ACCESS);
        }

        exchangeRateMapper.updateAlert(id, request);
        return exchangeRateMapper.getAlertById(id);
    }

    @Transactional
    public void deleteAlert(Long id, Long userId) {
        ExchangeRateAlertUpdateResponseDto existingAlert = exchangeRateMapper.getAlertById(id);
        if (existingAlert == null) {
            throw new ExchangeException(ExchangeErrorCode.ALERT_NOT_FOUND);
        }

        if (!existingAlert.getUserId().equals(userId)) {
            throw new ExchangeException(ExchangeErrorCode.FORBIDDEN_ACCESS);
        }

        exchangeRateMapper.deleteAlert(id);
    }


    public ExchangeRateHistoryResponseDto getHistoryRates(String currencyCode, int days) {
        // 1. 입력값 검증 (null/empty)
        if (currencyCode == null || currencyCode.isEmpty()) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_INPUT_VALUE);
        }

        // 2. days 정책 제한 (7, 30, 90일만 허용)
        if (days != 7 && days != 30 && days != 90) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_DAYS_RANGE);
        }

        // 3. 통화 코드 존재 여부 확인
        if (exchangeRateMapper.getCurrencyIdByCode(currencyCode) == null) {
            throw new ExchangeException(ExchangeErrorCode.RATE_NOT_FOUND);
        }

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
        ensureKrwExists();

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
            throw new ExchangeException(ExchangeErrorCode.INVALID_INPUT_VALUE);
        }
        
        LocalDate startDate = endDate.minusDays(89); 
        
        java.util.Map<String, Integer> currencySyncMap = new java.util.HashMap<>();
        int totalSavedCount = 0;
        
        LocalDate currentDate = endDate;
        for (int i = 0; i < 90; i++) {
            String dateParam = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            try {
                List<ExternalExchangeRateDto> dtoList = exchangeRateClient.fetchExchangeRates(dateParam);
                if (!dtoList.isEmpty()) {
                    List<ExchangeRateResponseDto> savedRates = processAndSave(dtoList, currentDate);
                    totalSavedCount += savedRates.size();
                    
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

                if (targetId == null) {
                    exchangeRateMapper.insertCurrency(curCode, dto.getCurNm());
                    targetId = exchangeRateMapper.getCurrencyIdByCode(curCode);
                    
                    if (targetId == null) continue;
                }

                BigDecimal rate = parseRate(dto.getDealBasR());
                int unit = extractUnit(dto.getCurUnit());

                if (unit != 1) {
                    rate = rate.divide(new BigDecimal(unit), 8, RoundingMode.HALF_UP);
                }
                
                if (targetId == null) continue;
                
                BigDecimal prevRate = exchangeRateMapper.getPreviousRate(targetId, rateDate);

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
        return savedRates;
    }

    private String extractCurrencyCode(String curUnit) {
        return curUnit.contains("(") ? curUnit.substring(0, curUnit.indexOf("(")) : curUnit;
    }

    private int extractUnit(String curUnit) {
        if (!curUnit.contains("(") || !curUnit.contains(")")) return 1;
        
        String unitStr = curUnit.substring(curUnit.indexOf("(") + 1, curUnit.indexOf(")"));
        try {
            return Integer.parseInt(unitStr);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public ExchangeRateConvertResponseDto convertCurrency(String fromCurrency, String toCurrency, double amount) {
        if (fromCurrency == null || fromCurrency.isEmpty() || 
            toCurrency == null || toCurrency.isEmpty() || 
            amount <= 0) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_INPUT_VALUE);
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
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.UNSUPPORTED_CURRENCY));
    }

    private BigDecimal parseRate(String rateStr) {
        return new BigDecimal(rateStr.replace(",", ""));
    }
}
