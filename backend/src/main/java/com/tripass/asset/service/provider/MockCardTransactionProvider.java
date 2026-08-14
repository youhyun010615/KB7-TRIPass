package com.tripass.asset.service.provider;

import com.tripass.asset.dto.CardDto;
import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.mapper.CardTransactionMapper;
import com.tripass.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * #193 시연용 카드 거래 Provider.
 *
 * 응답 결과를 서비스에 하드코딩하지 않고, 사용자별 DB Seed를 조회하여
 * 실제 CODEF 연동과 같은 흐름으로 동작하게 합니다.
 */
@Component
public class MockCardTransactionProvider implements CardTransactionProvider {

    private final CardTransactionMapper cardTransactionMapper;

    public MockCardTransactionProvider(CardTransactionMapper cardTransactionMapper) {
        this.cardTransactionMapper = cardTransactionMapper;
    }

    @Override
    public String getProviderType() {
        return "mock";
    }

    @Override
    public List<CardDto> getCards(Long userId) {
        return cardTransactionMapper.findCardsByUserId(userId);
    }

    @Override
    public List<TransactionDto> getTransactions(
            Long userId,
            Long cardId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        // cardId만으로 거래을 조회하기 전에 반드시 로그인 회원의 소유권을 검증한다.
        CardDto card = cardTransactionMapper.findCardByIdAndUserId(cardId, userId);
        if (card == null) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "CARD_NOT_FOUND",
                    "연동된 카드를 찾을 수 없습니다."
            );
        }

        return cardTransactionMapper.findCardTransactions(cardId, startDate, endDate);
    }
}
