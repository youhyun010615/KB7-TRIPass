package com.tripass.asset.service;

import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.service.provider.CardTransactionProvider;
import com.tripass.asset.service.provider.CardTransactionProviderRouter;
import com.tripass.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardTransactionServiceTest {

    @Mock
    private CardTransactionProviderRouter providerRouter;

    @Mock
    private CardTransactionProvider provider;

    @Test
    @DisplayName("연월 조건을 해당 월의 첫날과 마지막 날로 변환한다")
    void getTransactionsByYearMonth() {
        CardTransactionService service = new CardTransactionService(providerRouter);
        when(providerRouter.getProvider()).thenReturn(provider);
        when(provider.getTransactions(
                1L,
                10L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(List.of(new TransactionDto()));

        List<TransactionDto> result = service.getTransactions(
                1L,
                10L,
                "2026-07",
                null,
                null
        );

        assertEquals(1, result.size());
        verify(provider).getTransactions(
                1L,
                10L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );
    }

    @Test
    @DisplayName("시작일과 종료일 범위로 거래을 조회한다")
    void getTransactionsByDateRange() {
        CardTransactionService service = new CardTransactionService(providerRouter);
        when(providerRouter.getProvider()).thenReturn(provider);
        when(provider.getTransactions(
                1L,
                10L,
                LocalDate.of(2026, 7, 8),
                LocalDate.of(2026, 7, 14)
        )).thenReturn(List.of());

        service.getTransactions(
                1L,
                10L,
                null,
                "2026-07-08",
                "2026-07-14"
        );

        verify(provider).getTransactions(
                1L,
                10L,
                LocalDate.of(2026, 7, 8),
                LocalDate.of(2026, 7, 14)
        );
    }

    @Test
    @DisplayName("연월과 날짜 범위를 동시에 사용하면 400 예외를 반환한다")
    void rejectMixedPeriodConditions() {
        CardTransactionService service = new CardTransactionService(providerRouter);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.getTransactions(
                        1L,
                        10L,
                        "2026-07",
                        "2026-07-01",
                        "2026-07-31"
                )
        );

        assertEquals("INVALID_TRANSACTION_PERIOD", exception.getErrorCode());
        assertEquals(400, exception.getStatus().value());
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 400 예외를 반환한다")
    void rejectReversedDateRange() {
        CardTransactionService service = new CardTransactionService(providerRouter);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.getTransactions(
                        1L,
                        10L,
                        null,
                        "2026-07-31",
                        "2026-07-01"
                )
        );

        assertEquals("INVALID_TRANSACTION_PERIOD", exception.getErrorCode());
    }
}
