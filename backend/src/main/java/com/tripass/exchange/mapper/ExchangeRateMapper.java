package com.tripass.exchange.mapper;

import com.tripass.exchange.domain.ExchangeRate;
import com.tripass.exchange.dto.ExchangeRateHistoryResponseDto;
import com.tripass.exchange.dto.ExchangeRateResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExchangeRateMapper {
    // Upsert를 위한 쿼리 (MyBatis XML에서 구현)
    void upsertExchangeRate(ExchangeRate exchangeRate);
    
    // 통화 코드로 ID 조회
    Long getCurrencyIdByCode(String currencyCode);

    // 최신 환율 목록 조회
    List<ExchangeRateResponseDto> getLatestRates(@Param("currencyCodes") List<String> currencyCodes);

    // 특정 통화 히스토리 조회
    List<ExchangeRateHistoryResponseDto.RateInfo> getHistoryRates(@Param("currencyCode") String currencyCode, @Param("days") int days);
}
