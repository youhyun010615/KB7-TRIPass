package com.tripass.asset.mapper;

import com.tripass.asset.dto.CardDto;
import com.tripass.asset.dto.TransactionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CardTransactionMapper {

    List<CardDto> findCardsByUserId(@Param("userId") Long userId);

    CardDto findCardByIdAndUserId(
            @Param("cardId") Long cardId,
            @Param("userId") Long userId
    );

    List<TransactionDto> findCardTransactions(
            @Param("cardId") Long cardId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
