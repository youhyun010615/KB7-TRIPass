package com.tripass.exchange.mapper;

import com.tripass.exchange.dto.ExchangeMarketDataDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarketDataMapper {
    /**
     * 특정 통화의 최신 시장 데이터 조회 (국민은행 기준)
     */
    ExchangeMarketDataDto getLatestMarketDataByCurrencyCode(@Param("currencyCode") String currencyCode);
}
