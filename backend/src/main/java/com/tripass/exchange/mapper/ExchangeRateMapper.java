package com.tripass.exchange.mapper;

import com.tripass.exchange.domain.ExchangeRate;
import com.tripass.exchange.dto.ExchangeRateAlertRequestDto;
import com.tripass.exchange.dto.ExchangeRateAlertResponseDto;
import com.tripass.exchange.dto.ExchangeRateHistoryResponseDto;
import com.tripass.exchange.dto.ExchangeRateResponseDto;
import com.tripass.exchange.dto.LatestExchangeRateDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ExchangeRateMapper {
    // Upsert를 위한 쿼리 (MyBatis XML에서 구현)
    void upsertExchangeRate(ExchangeRate exchangeRate);
    
    // 통화 코드로 ID 조회
    Long getCurrencyIdByCode(String currencyCode);

    // 통화 정보가 없을 경우 삽입
    void insertCurrency(@Param("currencyCode") String currencyCode, @Param("currencyName") String currencyName);

    // 통화 ID로 코드 조회
    String getCurrencyCodeById(Long id);

    // 통화 이름 조회
    String getCurrencyNameByCode(String currencyCode);

    // 저장된 ID 조회
    Long findIdByCurrencyAndDate(@Param("baseCurrencyId") Long baseCurrencyId, @Param("targetCurrencyId") Long targetCurrencyId, @Param("rateDate") LocalDate rateDate);

    // 이전 환율 조회
    BigDecimal getPreviousRate(@Param("targetId") Long targetId, @Param("currentDate") LocalDate currentDate);

    // 최신 환율 목록 조회
    List<LatestExchangeRateDto> getLatestRates();

    // 관심 환율 알림 목록 조회
    List<ExchangeRateAlertResponseDto> getAlertsByUserId(@Param("userId") Long userId);

    // 관심 환율 알림 등록
    void insertAlert(@Param("userId") Long userId, @Param("request") ExchangeRateAlertRequestDto request);

    // 사용자별 통화 알림 존재 여부 확인
    int countAlertByUserAndCurrency(@Param("userId") Long userId, @Param("currencyId") Long currencyId);

    // 특정 통화 히스토리 조회
    List<ExchangeRateHistoryResponseDto.RateInfo> getHistoryRates(@Param("currencyCode") String currencyCode, @Param("days") int days);
}
