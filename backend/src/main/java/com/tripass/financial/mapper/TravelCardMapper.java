package com.tripass.financial.mapper;

import com.tripass.financial.dto.TravelCardDetailResponseDto;
import com.tripass.financial.dto.TravelCardListResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TravelCardMapper {
    List<TravelCardListResponseDto> findAll(
            @Param("keyword") String keyword,
            @Param("currencyCode") String currencyCode,
            @Param("instantUse") Boolean instantUse,
            @Param("transitCard") Boolean transitCard
    );

    TravelCardDetailResponseDto findById(
            @Param("cardId") Long cardId
    );

    List<String> findCurrencyCodesByCardId(
            @Param("cardId") Long cardId
    );
}
