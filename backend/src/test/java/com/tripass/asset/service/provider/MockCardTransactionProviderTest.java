package com.tripass.asset.service.provider;

import com.tripass.asset.dto.CardDto;
import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.mapper.CardTransactionMapper;
import com.tripass.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockCardTransactionProviderTest {

    @Mock
    private CardTransactionMapper cardTransactionMapper;

    @InjectMocks
    private MockCardTransactionProvider provider;

    @Test
    @DisplayName("로그인 회원이 보유한 카드의 거래만 조회한다")
    void getOwnedCardTransactions() {
        Long userId = 1L;
        Long cardId = 10L;
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 31);
        CardDto ownedCard = new CardDto();
        ownedCard.setId(cardId);
        when(cardTransactionMapper.findCardByIdAndUserId(cardId, userId)).thenReturn(ownedCard);
        when(cardTransactionMapper.findCardTransactions(cardId, startDate, endDate))
                .thenReturn(List.of(new TransactionDto(), new TransactionDto()));

        List<TransactionDto> result = provider.getTransactions(
                userId,
                cardId,
                startDate,
                endDate
        );

        assertEquals(2, result.size());
        verify(cardTransactionMapper).findCardByIdAndUserId(cardId, userId);
        verify(cardTransactionMapper).findCardTransactions(cardId, startDate, endDate);
    }

    @Test
    @DisplayName("다른 회원의 카드 ID로는 거래을 조회할 수 없다")
    void rejectUnownedCard() {
        Long userId = 1L;
        Long cardId = 99L;
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 31);
        when(cardTransactionMapper.findCardByIdAndUserId(cardId, userId)).thenReturn(null);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> provider.getTransactions(userId, cardId, startDate, endDate)
        );

        assertEquals("CARD_NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getStatus().value());
        verify(cardTransactionMapper, never()).findCardTransactions(cardId, startDate, endDate);
    }
}
