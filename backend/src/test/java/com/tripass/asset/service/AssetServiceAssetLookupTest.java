package com.tripass.asset.service;

import com.tripass.asset.dto.AccountDto;
import com.tripass.asset.dto.CardDto;
import com.tripass.asset.dto.SupportedInstitutionDto;
import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.mapper.AssetMapper;
import com.tripass.asset.duplicate.DuplicateTransactionMatcher;
import com.tripass.common.exception.CustomException;
import com.tripass.saving.classification.TransactionCategoryClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceAssetLookupTest {

    @Mock
    private AssetMapper assetMapper;

    @Mock
    private TransactionCategoryClassifier transactionCategoryClassifier;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetService(assetMapper, transactionCategoryClassifier, new DuplicateTransactionMatcher());
    }

    @Test
    void 은행과_카드_기관목록을_업권별로_조회한다() {
        SupportedInstitutionDto bank = new SupportedInstitutionDto();
        SupportedInstitutionDto card = new SupportedInstitutionDto();
        when(assetMapper.findSupportedInstitutionsByBusinessType("BK")).thenReturn(List.of(bank));
        when(assetMapper.findSupportedInstitutionsByBusinessType("CD")).thenReturn(List.of(card));

        assertEquals(List.of(bank), assetService.getSupportedBankInstitutions());
        assertEquals(List.of(card), assetService.getSupportedCardInstitutions());
        verify(assetMapper).findSupportedInstitutionsByBusinessType("BK");
        verify(assetMapper).findSupportedInstitutionsByBusinessType("CD");
    }

    @Test
    void 선택한_계좌의_거래만_조회한다() {
        AccountDto account = new AccountDto();
        account.setId(10L);
        when(assetMapper.findAccountById(10L, 3L)).thenReturn(account);
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);
        List<TransactionDto> transactions = List.of(new TransactionDto());
        when(assetMapper.findTransactionsByAccountIdWithFilter(10L, start, end, "WITHDRAWAL"))
                .thenReturn(transactions);

        assertEquals(transactions,
                assetService.getAccountTransactions(3L, 10L, start, end, "WITHDRAWAL").getTransactions());
        verify(assetMapper).findTransactionsByAccountIdWithFilter(10L, start, end, "WITHDRAWAL");
    }

    @Test
    void 선택한_카드의_거래만_조회한다() {
        CardDto card = new CardDto();
        card.setId(20L);
        when(assetMapper.findCardByIdAndUserId(20L, 3L)).thenReturn(card);
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);
        List<TransactionDto> transactions = List.of(new TransactionDto());
        when(assetMapper.findTransactionsByCardId(20L, start, end)).thenReturn(transactions);

        assertEquals(transactions, assetService.getCardTransactions(3L, 20L, "2026-07-01", "2026-07-31"));
        verify(assetMapper).findTransactionsByCardId(20L, start, end);
    }

    @Test
    void 다른_사용자의_카드_거래는_조회하지_않는다() {
        when(assetMapper.findCardByIdAndUserId(20L, 3L)).thenReturn(null);

        assertThrows(CustomException.class,
                () -> assetService.getCardTransactions(3L, 20L, "2026-07-01", "2026-07-31"));
    }

    @Test
    void 전체_거래조회에서_체크카드와_중복된_계좌출금만_제외한다() {
        TransactionDto account = transaction(1L, 10L, null, null, "12:00:00");
        TransactionDto checkCard = transaction(2L, null, 20L, "CHECK", "12:03:00");
        TransactionDto creditCard = transaction(3L, null, 21L, "CREDIT", "12:02:00");
        when(assetMapper.findTransactionsByUserId(3L, null, null))
                .thenReturn(List.of(account, checkCard, creditCard));

        List<Long> ids = assetService.getAllTransactions(3L, null, null).stream()
                .map(TransactionDto::getId)
                .toList();

        assertEquals(List.of(2L, 3L), ids);
    }

    private TransactionDto transaction(Long id, Long accountId, Long cardId, String cardType, String time) {
        TransactionDto transaction = new TransactionDto();
        transaction.setId(id);
        transaction.setAccountId(accountId);
        transaction.setCardId(cardId);
        transaction.setSourceCardType(cardType);
        transaction.setTransactionType("WITHDRAWAL");
        transaction.setTransactionDate(LocalDate.of(2026, 7, 10));
        transaction.setTransactionTime(LocalTime.parse(time));
        transaction.setAmount(new BigDecimal("10000"));
        return transaction;
    }
}
