package com.tripass.asset.service.provider;

import com.tripass.asset.dto.CardDto;
import com.tripass.asset.dto.TransactionDto;

import java.time.LocalDate;
import java.util.List;

/**
 * 카드 원천 데이터를 제공하는 공통 규격입니다.
 *
 * 현재는 DB Seed를 조회하는 Mock 구현체를 사용하고, CODEF 카드 API가 준비되면
 * 동일한 DTO를 반환하는 CODEF 구현체로 교체합니다.
 */
public interface CardTransactionProvider {

    String getProviderType();

    List<CardDto> getCards(Long userId);

    List<TransactionDto> getTransactions(
            Long userId,
            Long cardId,
            LocalDate startDate,
            LocalDate endDate
    );
}
