package com.tripass.asset.service.provider;

import com.tripass.asset.dto.CardDto;
import com.tripass.asset.dto.TransactionDto;
import com.tripass.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 실제 CODEF 카드 API 연동 위치를 고정하는 구현체입니다.
 *
 * 나중에 CODEF 연동을 추가할 때 Controller와 Service를 바꾸지 않고 이 구현체만
 * 완성하도록 의도적으로 분리했습니다.
 */
@Component
public class CodefCardTransactionProvider implements CardTransactionProvider {

    @Override
    public String getProviderType() {
        return "codef";
    }

    @Override
    public List<CardDto> getCards(Long userId) {
        throw notConfigured();
    }

    @Override
    public List<TransactionDto> getTransactions(
            Long userId,
            Long cardId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        throw notConfigured();
    }

    private CustomException notConfigured() {
        return new CustomException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CODEF_CARD_NOT_CONFIGURED",
                "CODEF 카드 API가 아직 연동되지 않았습니다."
        );
    }
}
