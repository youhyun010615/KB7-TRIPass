package com.tripass.exchange.service;

import com.tripass.exchange.client.ExchangeRateClient;
import com.tripass.exchange.domain.ExchangeMarketData;
import com.tripass.exchange.domain.ExchangeRate;
import com.tripass.exchange.domain.ExchangeRateAlert;
import com.tripass.exchange.dto.*;
import com.tripass.exchange.exception.ExchangeErrorCode;
import com.tripass.exchange.exception.ExchangeException;
import com.tripass.exchange.mapper.ExchangeRateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
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

    /**
     * [초기 복구용 1회 실행 메서드]
     * 날아간 과거 90일치 데이터를 Truncate 없이 Upsert로 복구합니다.
     */
    public SyncResultDto initPast90DaysRates() {
        ensureKrwExists();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(89);

        int totalSavedCount = 0;
        Map<String, Integer> currencySyncMap = new HashMap<>();
        Map<Long, BigDecimal> lastRates = new HashMap<>();

        LocalDate currentDate = startDate;
        for (int i = 0; i < 90; i++) {
            // 주말은 API가 없으므로 스킵
            if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            String dateParam = currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            try {
                List<ExternalExchangeRateDto> dtoList = exchangeRateClient.fetchExchangeRates(dateParam);
                if (dtoList != null && !dtoList.isEmpty()) {
                    List<ExchangeRateResponseDto> savedRates = saveBatchRatesWithMemoryMap(dtoList, currentDate, lastRates);
                    totalSavedCount += savedRates.size();
                    for (ExchangeRateResponseDto rate : savedRates) {
                        String code = exchangeRateMapper.getCurrencyCodeById(rate.getTargetCurrencyId());
                        currencySyncMap.put(code, currencySyncMap.getOrDefault(code, 0) + 1);
                    }
                }
            } catch (Exception e) {
                log.error("과거 환율 복구 실패 - 일자: {}", dateParam, e);
            }
            currentDate = currentDate.plusDays(1);
        }

        crawlAndSaveMarketData();

        List<SyncResultDto.CurrencySyncStatus> statusList = currencySyncMap.entrySet().stream()
                .map(entry -> SyncResultDto.CurrencySyncStatus.builder()
                        .currencyCode(entry.getKey())
                        .savedDays(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        return SyncResultDto.builder()
                .syncPeriod(startDate + " ~ " + endDate)
                .totalSavedCount(totalSavedCount)
                .syncedAt(LocalDateTime.now())
                .syncedCurrencies(statusList)
                .build();
    }

    /**
     * [일일 스케줄러용 - 당일 1일치 동기화]
     */
    public SyncResultDto syncExchangeRates() {
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY) {
            today = today.minusDays(1);
        } else if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            today = today.minusDays(2);
        }
        return syncExchangeRates(today.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    }

    /**
     * [특정 1일치 단건 수집 및 동기화]
     */
    public SyncResultDto syncExchangeRates(String date) {
        ensureKrwExists();

        LocalDate targetDate;
        String normalizedDate = date.replace("-", "");
        try {
            targetDate = LocalDate.of(
                    Integer.parseInt(normalizedDate.substring(0, 4)),
                    Integer.parseInt(normalizedDate.substring(4, 6)),
                    Integer.parseInt(normalizedDate.substring(6, 8))
            );
        } catch (Exception e) {
            log.error("날짜 파싱 실패: {}", date);
            throw new ExchangeException(ExchangeErrorCode.INVALID_INPUT_VALUE);
        }

        String dateParam = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<ExternalExchangeRateDto> dtoList;
        try {
            // 외부 API 통신 (트랜잭션 밖에서 수행)
            dtoList = exchangeRateClient.fetchExchangeRates(dateParam);
        } catch (Exception e) {
            log.error("수출입은행 API 호출 실패 - 일자: {}", dateParam, e);
            throw new ExchangeException(ExchangeErrorCode.RATE_NOT_FOUND);
        }

        if (dtoList == null || dtoList.isEmpty()) {
            log.warn("해당 일자({}) 고시 데이터 없음 (비영업일/고시 전)", dateParam);
            return SyncResultDto.builder()
                    .syncPeriod(targetDate.toString())
                    .totalSavedCount(0)
                    .syncedAt(LocalDateTime.now())
                    .syncedCurrencies(Collections.emptyList())
                    .build();
        }

        // DB Upsert 수행
        List<ExchangeRateResponseDto> savedRates = saveSingleDayRates(dtoList, targetDate);

        // 마이뱅크 크롤링 수행
        crawlAndSaveMarketData();

        List<SyncResultDto.CurrencySyncStatus> statusList = savedRates.stream()
                .map(rate -> SyncResultDto.CurrencySyncStatus.builder()
                        .currencyCode(exchangeRateMapper.getCurrencyCodeById(rate.getTargetCurrencyId()))
                        .savedDays(1)
                        .build())
                .collect(Collectors.toList());

        return SyncResultDto.builder()
                .syncPeriod(targetDate.toString())
                .totalSavedCount(savedRates.size())
                .syncedAt(LocalDateTime.now())
                .syncedCurrencies(statusList)
                .build();
    }

    /**
     * 1일치 단건 데이터를 DB에 Upsert (prevRate는 getPreviousRate 쿼리로 매핑)
     */
    @Transactional
    public List<ExchangeRateResponseDto> saveSingleDayRates(List<ExternalExchangeRateDto> dtoList, LocalDate rateDate) {
        List<ExchangeRateResponseDto> savedRates = new ArrayList<>();
        Long krwId = exchangeRateMapper.getCurrencyIdByCode("KRW");

        for (ExternalExchangeRateDto dto : dtoList) {
            try {
                String curCode = extractCurrencyCode(dto.getCurUnit());
                if ("KRW".equals(curCode)) continue;

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

                // XML에 이미 정의된 getPreviousRate 쿼리 활용
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

                ExchangeRateResponseDto dtoResponse = new ExchangeRateResponseDto();
                dtoResponse.setBaseCurrencyId(krwId);
                dtoResponse.setTargetCurrencyId(targetId);
                dtoResponse.setCurrencyUnit(1);
                dtoResponse.setDealBaseRate(rate);
                dtoResponse.setPrevRate(prevRate);
                dtoResponse.setRateDate(rateDate);
                dtoResponse.setFetchedAt(LocalDateTime.now());

                savedRates.add(dtoResponse);
            } catch (Exception e) {
                log.error("단건 환율 가공 및 저장 실패: {}", dto.getCurUnit(), e);
            }
        }
        return savedRates;
    }

    /**
     * 90일치 일괄 복구 시 사용하는 내부 저장 메서드
     */
    @Transactional
    public List<ExchangeRateResponseDto> saveBatchRatesWithMemoryMap(List<ExternalExchangeRateDto> dtoList, LocalDate rateDate, Map<Long, BigDecimal> lastRates) {
        List<ExchangeRateResponseDto> savedRates = new ArrayList<>();
        Long krwId = exchangeRateMapper.getCurrencyIdByCode("KRW");

        for (ExternalExchangeRateDto dto : dtoList) {
            try {
                String curCode = extractCurrencyCode(dto.getCurUnit());
                if ("KRW".equals(curCode)) continue;

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
                lastRates.put(targetId, rate);

                ExchangeRateResponseDto dtoResponse = new ExchangeRateResponseDto();
                dtoResponse.setBaseCurrencyId(krwId);
                dtoResponse.setTargetCurrencyId(targetId);
                dtoResponse.setCurrencyUnit(1);
                dtoResponse.setDealBaseRate(rate);
                dtoResponse.setPrevRate(prevRate);
                dtoResponse.setRateDate(rateDate);
                dtoResponse.setFetchedAt(LocalDateTime.now());

                savedRates.add(dtoResponse);
            } catch (Exception e) {
                log.error("환율 복구 저장 실패: {}", dto.getCurUnit(), e);
            }
        }
        return savedRates;
    }

    private void crawlAndSaveMarketData() {
        try {
            Document doc = Jsoup.connect("https://exchange.mibank.me/bank?bank_cd=004&exchange_type=buy")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .get();

            Elements rows = doc.select("table.main_table.content tbody tr");

            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() < 9) continue;

                String countryName = cols.get(1).text().trim();
                CurrencyInfo info = CURRENCY_INFO_MAP.get(countryName);
                if (info == null) continue;

                Long currencyId = exchangeRateMapper.getCurrencyIdByCode(info.code);
                if (currencyId == null) continue;

                ExchangeMarketData marketData = new ExchangeMarketData();
                marketData.setCurrencyId(currencyId);
                marketData.setUnit(info.unit);
                marketData.setBuyRate(parseRate(cols.get(2).select(".counter").text()));
                marketData.setBuyFeeRate(new BigDecimal(cols.get(3).text().replace("%", "").trim()));
                marketData.setSellRate(parseRate(cols.get(4).select(".counter").text()));
                marketData.setSellFeeRate(new BigDecimal(cols.get(5).text().replace("%", "").trim()));
                marketData.setBaseRate(parseRate(cols.get(8).select(".counter").text()));
                marketData.setFetchedAt(LocalDateTime.now());

                exchangeRateMapper.upsertMarketData(marketData);
            }
        } catch (Exception e) {
            log.error("마이뱅크 크롤링 수집 실패 (스킵)", e);
        }
    }

    public List<LatestExchangeRateDto> getLatestRates() {
        return exchangeRateMapper.getLatestRates();
    }

    public List<CountryExchangeRateDto> getLatestRatesByCountry() {
        return exchangeRateMapper.getLatestRatesByCountry();
    }

    public List<ExchangeRateAlert> findAllActiveAlerts() {
        return exchangeRateMapper.findAllActiveAlerts();
    }

    public String getCurrencyCodeById(Long id) {
        return exchangeRateMapper.getCurrencyCodeById(id);
    }

    public Long getCurrencyIdByCountryId(Long countryId) {
        return exchangeRateMapper.getCurrencyIdByCountryId(countryId);
    }

    public Map<Long, BigDecimal> getLatestRatesMap() {
        LocalDate targetDate = LocalDate.now();
        List<ExchangeRate> rates = exchangeRateMapper.findAllByDate(targetDate);

        if (rates == null || rates.isEmpty()) {
            rates = exchangeRateMapper.findMostRecentRates();
        }

        if (rates == null) return Collections.emptyMap();

        return rates.stream()
                .collect(Collectors.toMap(ExchangeRate::getTargetCurrencyId, ExchangeRate::getDealBaseRate, (r1, r2) -> r1));
    }

    public List<ExchangeRateAlertResponseDto> getAlertsByUserId(Long userId) {
        return exchangeRateMapper.getAlertsByUserId(userId);
    }

    @Transactional
    public Long registerAlert(Long userId, ExchangeRateAlertRequestDto request) {
        Long countryId = request.getCountryId();
        if (countryId == null) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_INPUT_VALUE);
        }

        validateAlertRequest(null, request.getTargetRate());

        int count = exchangeRateMapper.countAlertByUserAndCountry(userId, countryId);
        if (count > 0) {
            throw new ExchangeException(ExchangeErrorCode.DUPLICATE_ALERT);
        }

        ExchangeRateAlert alert = new ExchangeRateAlert();
        alert.setUserId(userId);
        alert.setCountryId(countryId);
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
        if (currencyCode == null || currencyCode.isEmpty()) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_INPUT_VALUE);
        }

        if (days != 7 && days != 30 && days != 90) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_DAYS_RANGE);
        }

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

    private void ensureKrwExists() {
        Long krwId = exchangeRateMapper.getCurrencyIdByCode("KRW");
        if (krwId == null) {
            log.info("KRW 통화 정보 신규 등록");
            exchangeRateMapper.insertCurrency("KRW", "대한민국 원");
        }
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
        if (rateStr == null || rateStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(rateStr.replace(",", "").trim());
    }
}