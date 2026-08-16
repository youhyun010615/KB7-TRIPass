package com.tripass.exchange.service;

import org.springframework.dao.DuplicateKeyException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.tripass.exchange.domain.ExchangeMarketData;
import org.springframework.dao.DuplicateKeyException;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateClient exchangeRateClient;
    private final ExchangeRateMapper exchangeRateMapper;

    private static class CurrencyInfo {
        String code;
        int unit;
        CurrencyInfo(String code, int unit) { this.code = code; this.unit = unit; }
    }

    private static final Map<String, CurrencyInfo> CURRENCY_INFO_MAP = Map.ofEntries(
            entry("중국", new CurrencyInfo("CNH", 1)),
            entry("일본", new CurrencyInfo("JPY", 100)),
            entry("미국", new CurrencyInfo("USD", 1)),
            entry("유럽연합", new CurrencyInfo("EUR", 1)),
            entry("홍콩", new CurrencyInfo("HKD", 1)),
            entry("대만", new CurrencyInfo("TWD", 1)),
            entry("싱가폴", new CurrencyInfo("SGD", 1)),
            entry("태국", new CurrencyInfo("THB", 1)),
            entry("필리핀", new CurrencyInfo("PHP", 1)),
            entry("베트남", new CurrencyInfo("VND", 100)),
            entry("영국", new CurrencyInfo("GBP", 1)),
            entry("호주", new CurrencyInfo("AUD", 1)),
            entry("캐나다", new CurrencyInfo("CAD", 1)),
            entry("브라질", new CurrencyInfo("BRL", 1)),
            entry("칠레", new CurrencyInfo("CLP", 1)),
            entry("멕시코", new CurrencyInfo("MXN", 1)),
            entry("뉴질랜드", new CurrencyInfo("NZD", 1)),
            entry("인도네시아", new CurrencyInfo("IDR", 100)),
            entry("말레이시아", new CurrencyInfo("MYR", 1)),
            entry("튀르키예", new CurrencyInfo("TRY", 1)),
            entry("인도", new CurrencyInfo("INR", 1)),
            entry("이스라엘", new CurrencyInfo("ILS", 1)),
            entry("사우디", new CurrencyInfo("SAR", 1)),
            entry("쿠웨이트", new CurrencyInfo("KWD", 1)),
            entry("바레인", new CurrencyInfo("BHD", 1)),
            entry("U.A.E", new CurrencyInfo("AED", 1)),
            entry("카자흐스탄", new CurrencyInfo("KZT", 1)),
            entry("파키스탄", new CurrencyInfo("PKR", 1)),
            entry("방글라데시", new CurrencyInfo("BDT", 1)),
            entry("브루나이", new CurrencyInfo("BND", 1)),
            entry("오만", new CurrencyInfo("OMR", 1)),
            entry("요르단", new CurrencyInfo("JOD", 1)),
            entry("스위스", new CurrencyInfo("CHF", 1)),
            entry("러시아", new CurrencyInfo("RUB", 1)),
            entry("스웨덴", new CurrencyInfo("SEK", 1)),
            entry("덴마크", new CurrencyInfo("DKK", 1)),
            entry("노르웨이", new CurrencyInfo("NOK", 1)),
            entry("헝가리", new CurrencyInfo("HUF", 1)),
            entry("체코", new CurrencyInfo("CZK", 1)),
            entry("폴란드", new CurrencyInfo("PLN", 1)),
            entry("남아공", new CurrencyInfo("ZAR", 1)),
            entry("이집트", new CurrencyInfo("EGP", 1))
    );

    public List<LatestExchangeRateDto> getLatestRates() {
        return exchangeRateMapper.getLatestRates();
    }

    public List<ExchangeRateAlert> findAllActiveAlerts() {
        return exchangeRateMapper.findAllActiveAlerts();
    }

    public String getCurrencyCodeById(Long id) {
        return exchangeRateMapper.getCurrencyCodeById(id);
    }

    public Map<Long, BigDecimal> getLatestRatesMap() {
        LocalDate targetDate = LocalDate.now();
        List<ExchangeRate> rates = exchangeRateMapper.findAllByDate(targetDate);
        
        if (rates == null || rates.isEmpty()) {
            rates = exchangeRateMapper.findMostRecentRates();
        }
        
        return rates.stream()
                .collect(Collectors.toMap(ExchangeRate::getTargetCurrencyId, ExchangeRate::getDealBaseRate));
    }


    public List<ExchangeRateAlertResponseDto> getAlertsByUserId(Long userId) {
        return exchangeRateMapper.getAlertsByUserId(userId);
    }

    @Transactional
    public Long registerAlert(Long userId, ExchangeRateAlertRequestDto request) {
        Long currencyId = exchangeRateMapper.getCurrencyIdByCode(request.getCurrencyCode());
        if (currencyId == null) {
            throw new ExchangeException(ExchangeErrorCode.RATE_NOT_FOUND);
        }

        validateAlertRequest(currencyId, request.getTargetRate());

        // 애플리케이션 레벨 사전 중복 검사 (userId 기반)
        int count = exchangeRateMapper.countAlertByUserAndCurrency(userId, currencyId);
        if (count > 0) {
            throw new ExchangeException(ExchangeErrorCode.DUPLICATE_ALERT);
        }

        ExchangeRateAlert alert = new ExchangeRateAlert();
        alert.setUserId(userId);
        alert.setCurrencyId(currencyId);
        alert.setTargetRate(request.getTargetRate());

        try {
            exchangeRateMapper.insertAlert(alert);
        } catch (DuplicateKeyException e) {
            throw new ExchangeException(ExchangeErrorCode.DUPLICATE_ALERT);
        }

        return alert.getId();
    }

    @Transactional
    public ExchangeRateAlertUpdateResponseDto updateAlert(Long id, Long userId, ExchangeRateAlertUpdateRequestDto request) {
        validateAlertRequest(null, request.getTargetRate());

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

    private void validateAlertRequest(Long currencyId, Double targetRate) {
        if (currencyId != null && !exchangeRateMapper.existsCurrencyById(currencyId)) {
             throw new ExchangeException(ExchangeErrorCode.RATE_NOT_FOUND);
        }

        if (targetRate == null || targetRate <= 0) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_INPUT_VALUE);
        }
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
    public SyncResultDto syncExchangeRates() {
        LocalDate today = LocalDate.now();
        // 주말(토, 일)인 경우 금요일로 조정
        if (today.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
            today = today.minusDays(1);
        } else if (today.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            today = today.minusDays(2);
        }
        
        String dateString = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return syncExchangeRates(dateString);
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
        
        // 기존 전체 데이터 삭제
        exchangeRateMapper.truncateExchangeRates();
        
        Map<String, Integer> currencySyncMap = new HashMap<>();
        Map<Long, BigDecimal> lastRates = new HashMap<>(); // prevRate 계산용 맵
        int totalSavedCount = 0;
        
        LocalDate currentDate = startDate;
        for (int i = 0; i < 90; i++) {
            String dateParam = currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            try {
                List<ExternalExchangeRateDto> dtoList = exchangeRateClient.fetchExchangeRates(dateParam);
                if (!dtoList.isEmpty()) {
                    // lastRates 맵을 전달
                    List<ExchangeRateResponseDto> savedRates = processAndSave(dtoList, currentDate, lastRates);
                    totalSavedCount += savedRates.size();
                    
                    for (ExchangeRateResponseDto rate : savedRates) {
                        String code = exchangeRateMapper.getCurrencyCodeById(rate.getTargetCurrencyId());
                        currencySyncMap.put(code, currencySyncMap.getOrDefault(code, 0) + 1);
                    }
                }
            } catch (Exception e) {
                log.error("동기화 실패 - 날짜: {}", dateParam, e);
            }
            currentDate = currentDate.plusDays(1);
        }
        
        // 마이뱅크 크롤링 및 수수료 저장
        crawlAndSaveMarketData();
        
        List<SyncResultDto.CurrencySyncStatus> statusList = currencySyncMap.entrySet().stream()
                .map(entry -> SyncResultDto.CurrencySyncStatus.builder()
                        .currencyCode(entry.getKey())
                        .savedDays(entry.getValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return SyncResultDto.builder()
                .syncPeriod(startDate + " ~ " + endDate)
                .totalSavedCount(totalSavedCount)
                .syncedAt(LocalDateTime.now())
                .syncedCurrencies(statusList)
                .build();
    }

    private void crawlAndSaveMarketData() {
        try {
            Document doc = Jsoup.connect("https://exchange.mibank.me/bank?bank_cd=004&exchange_type=buy").get();
            Elements rows = doc.select("table.main_table.content tbody tr");
            
            for (Element row : rows) {
                Elements cols = row.select("td");
                
                // 유효성 체크
                if (cols.size() < 9) continue; 
                
                String countryName = cols.get(1).text();
                CurrencyInfo info = CURRENCY_INFO_MAP.get(countryName);
                if (info == null) {
                    log.warn("매핑되지 않은 국가명: {}", countryName);
                    continue;
                }
                
                Long currencyId = exchangeRateMapper.getCurrencyIdByCode(info.code);
                if (currencyId == null) {
                    log.warn("DB에 존재하지 않는 통화코드: {}", info.code);
                    continue;
                }
                
                ExchangeMarketData marketData = new ExchangeMarketData();
                marketData.setCurrencyId(currencyId);
                marketData.setUnit(info.unit);
                
                // 데이터 파싱
                marketData.setBuyRate(parseRate(cols.get(2).select(".counter").text()));
                marketData.setBuyFeeRate(new BigDecimal(cols.get(3).text().replace("%", "").trim()));
                marketData.setSellRate(parseRate(cols.get(4).select(".counter").text()));
                marketData.setSellFeeRate(new BigDecimal(cols.get(5).text().replace("%", "").trim()));
                marketData.setBaseRate(parseRate(cols.get(8).select(".counter").text()));
                marketData.setFetchedAt(LocalDateTime.now());
                
                log.info("저장할 환율시장 데이터: currencyId={}, unit={}, baseRate={}, buyRate={}, sellRate={}", 
                         currencyId, info.unit, marketData.getBaseRate(), marketData.getBuyRate(), marketData.getSellRate());
                
                exchangeRateMapper.upsertMarketData(marketData);
            }
        } catch (Exception e) {
            log.error("마이뱅크 크롤링 실패", e);
        }
    }


    private void ensureKrwExists() {
        Long krwId = exchangeRateMapper.getCurrencyIdByCode("KRW");
        if (krwId == null) {
            log.info("KRW 통화 정보가 없어 새로 등록합니다.");
            exchangeRateMapper.insertCurrency("KRW", "대한민국 원");
        }
    }

    private List<ExchangeRateResponseDto> processAndSave(List<ExternalExchangeRateDto> dtoList, LocalDate rateDate, Map<Long, BigDecimal> lastRates) {
        List<ExchangeRateResponseDto> savedRates = new ArrayList<>();
        Long krwId = exchangeRateMapper.getCurrencyIdByCode("KRW");

        for (ExternalExchangeRateDto dto : dtoList) {
            try {
                String curCode = extractCurrencyCode(dto.getCurUnit());
                
                // KRW는 건너뜀
                if ("KRW".equals(curCode)) {
                    continue;
                }

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
                
                // 메모리 맵에서 직전 환율 가져오기
                BigDecimal prevRate = lastRates.get(targetId);

                ExchangeRate exchangeRate = new ExchangeRate();
                exchangeRate.setBaseCurrencyId(krwId);
                exchangeRate.setTargetCurrencyId(targetId);
                exchangeRate.setCurrencyUnit(1);
                exchangeRate.setDealBaseRate(rate);
                exchangeRate.setPrevRate(prevRate);
                exchangeRate.setRateDate(rateDate);
                exchangeRate.setFetchedAt(LocalDateTime.now());
                
                exchangeRateMapper.upsertExchangeRate(exchangeRate);
                
                // 다음날 prevRate를 위해 현재 rate를 맵에 저장
                lastRates.put(targetId, rate);
                
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
        if (curUnit == null || !curUnit.contains("(") || !curUnit.contains(")")) return 1;

        try {
            String unitStr = curUnit.substring(curUnit.indexOf("(") + 1, curUnit.indexOf(")"));
            return Integer.parseInt(unitStr);
        } catch (NumberFormatException e) {
            log.warn("단위 추출 실패: {}. 기본값 1 적용", curUnit);
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
