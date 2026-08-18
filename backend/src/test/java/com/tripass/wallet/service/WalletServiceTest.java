package com.tripass.wallet.service;

import com.tripass.wallet.domain.Wallet;
import com.tripass.wallet.domain.WalletAutoSavingLog;
import com.tripass.wallet.domain.WalletAutoSavingRule;
import com.tripass.wallet.domain.WalletLedger;
import com.tripass.wallet.domain.WalletWithdrawRecipient;
import com.tripass.wallet.dto.request.*;
import com.tripass.wallet.dto.response.*;
import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.mapper.WalletMapper;
import com.tripass.wallet.travelcard.domain.TravelCardBalance;
import com.tripass.wallet.travelcard.domain.WalletTravelCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    @Test
    void 월렛이_없으면_메인_조회_예외가_발생한다() {
        when(walletMapper.findWalletMainByUserId(1L))
                .thenReturn(null);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.getWalletMain(1L)
        );

        assertEquals("WALLET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 트래블카드가_연동되지_않은_월렛_메인을_조회한다() {
        WalletMainResponseDto response = createWalletMainResponse();

        when(walletMapper.findWalletMainByUserId(1L))
                .thenReturn(response);
        when(walletMapper.findMonthlySavings(1L, 100L))
                .thenReturn(Collections.emptyList());
        when(walletMapper.findLinkedTravelCardForMain(100L))
                .thenReturn(null);

        WalletMainResponseDto result = walletService.getWalletMain(1L);

        assertFalse(result.getTravelCard().getLinked());
        assertTrue(result.getForeignBalances().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getEmergencyAmount());
        assertEquals(20, result.getSavingRate());
    }

    @Test
    void 트래블카드가_연동된_월렛_메인을_조회한다() {
        WalletMainResponseDto response = createWalletMainResponse();
        WalletLinkedTravelCardResponseDto travelCard = WalletLinkedTravelCardResponseDto.builder()
                .walletTravelCardId(5L)
                .build();

        when(walletMapper.findWalletMainByUserId(1L))
                .thenReturn(response);
        when(walletMapper.findMonthlySavings(1L, 100L))
                .thenReturn(Collections.emptyList());
        when(walletMapper.findLinkedTravelCardForMain(100L))
                .thenReturn(travelCard);
        when(walletMapper.findForeignBalancesForMain(5L))
                .thenReturn(Collections.emptyList());

        WalletMainResponseDto result = walletService.getWalletMain(1L);

        assertTrue(result.getTravelCard().getLinked());
        assertEquals(5L, result.getTravelCard().getWalletTravelCardId());
    }

    @Test
    void 목표금액이_없으면_기본_목표금액을_적용한다() {
        WalletMainResponseDto response = createWalletMainResponse();
        response.setTargetAmount(null);

        when(walletMapper.findWalletMainByUserId(1L))
                .thenReturn(response);
        when(walletMapper.findMonthlySavings(anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());
        when(walletMapper.findLinkedTravelCardForMain(anyLong()))
                .thenReturn(null);

        WalletMainResponseDto result = walletService.getWalletMain(1L);

        assertEquals(0, BigDecimal.valueOf(5_000_000).compareTo(result.getTargetAmount()));
    }

    @Test
    void 월렛이_없으면_내역_조회_예외가_발생한다() {
        when(walletMapper.findWalletMainByUserId(1L))
                .thenReturn(null);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.getWalletLedgers(1L)
        );

        assertEquals("WALLET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 월렛_전체_내역을_조회한다() {
        WalletMainResponseDto response = createWalletMainResponse();

        when(walletMapper.findWalletMainByUserId(1L))
                .thenReturn(response);
        when(walletMapper.findLedgersByWalletId(100L))
                .thenReturn(List.of(new WalletLedgerResponseDto()));

        List<WalletLedgerResponseDto> result = walletService.getWalletLedgers(1L);

        assertEquals(1, result.size());
    }

    @Test
    void 월_형식이_올바르지_않으면_월별_저축_상세_예외가_발생한다() {
        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.getMonthlySavingDetail(1L, "2026/08")
        );

        assertEquals("INVALID_MONTH", exception.getErrorCode());
    }

    @Test
    void 요약이_없으면_기본값으로_월별_저축_상세를_반환한다() {
        WalletMainResponseDto response = createWalletMainResponse();

        when(walletMapper.findWalletMainByUserId(1L))
                .thenReturn(response);
        when(walletMapper.findMonthlySavingDetailSummary(100L, "2026-08"))
                .thenReturn(null);
        when(walletMapper.findLedgersByWalletIdAndMonth(100L, "2026-08"))
                .thenReturn(Collections.emptyList());

        WalletMonthlySavingDetailResponseDto result =
                walletService.getMonthlySavingDetail(1L, "2026-08");

        assertEquals("2026-08", result.getMonth());
        assertEquals(BigDecimal.ZERO, result.getSavedAmount());
        assertEquals(BigDecimal.ZERO, result.getChargeAmount());
        assertEquals(BigDecimal.ZERO, result.getWithdrawAmount());
        assertTrue(result.getLedgers().isEmpty());
    }

    @Test
    void 트래블카드가_없으면_전체_외화잔액이_빈_목록이다() {
        when(walletMapper.findWalletByUserId(1L))
                .thenReturn(createWallet());
        when(walletMapper.findLinkedTravelCardForMain(100L))
                .thenReturn(null);

        List<WalletForeignBalanceResponseDto> result = walletService.getAllForeignBalances(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void 금액이_0원_이하이면_충전_예외가_발생한다() {
        WalletChargeRequestDto request = chargeRequest(10L, BigDecimal.ZERO, "key-1");

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.charge(1L, request)
        );

        assertEquals("INVALID_AMOUNT", exception.getErrorCode());
        verifyNoInteractions(walletMapper);
    }

    @Test
    void 중복된_멱등키이면_충전_예외가_발생한다() {
        WalletChargeRequestDto request = chargeRequest(10L, BigDecimal.valueOf(10_000), "dup-key");

        when(walletMapper.existsIdempotencyKey("dup-key"))
                .thenReturn(true);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.charge(1L, request)
        );

        assertEquals("DUPLICATED_REQUEST", exception.getErrorCode());
        verify(walletMapper, never()).findWalletByUserIdForUpdate(anyLong());
    }

    @Test
    void 연동되지_않은_계좌로_충전하면_예외가_발생한다() {
        WalletChargeRequestDto request = chargeRequest(10L, BigDecimal.valueOf(10_000), "key-2");

        when(walletMapper.existsIdempotencyKey("key-2"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletMapper.existsLinkedAccount(100L, 10L))
                .thenReturn(false);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.charge(1L, request)
        );

        assertEquals("WALLET_ACCOUNT_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 월렛을_충전하면_잔액이_늘고_원장이_기록된다() {
        WalletChargeRequestDto request = chargeRequest(10L, BigDecimal.valueOf(300_000), "key-3");
        Wallet wallet = createWallet();

        when(walletMapper.existsIdempotencyKey("key-3"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletMapper.existsLinkedAccount(100L, 10L))
                .thenReturn(true);
        when(walletMapper.decreaseAccountBalance(1L, 10L, BigDecimal.valueOf(300_000)))
                .thenReturn(1);
        when(walletMapper.updateWalletBalance(100L, BigDecimal.valueOf(1_300_000), 0L))
                .thenReturn(1);

        WalletCommandResponseDto result = walletService.charge(1L, request);

        assertEquals(100L, result.getWalletId());
        assertEquals(0, BigDecimal.valueOf(1_300_000).compareTo(result.getBalanceAmount()));

        ArgumentCaptor<WalletLedger> captor = ArgumentCaptor.forClass(WalletLedger.class);
        verify(walletMapper).insertWalletLedger(captor.capture());

        WalletLedger ledger = captor.getValue();
        assertEquals("IN", ledger.getDirection());
        assertEquals("CHARGE", ledger.getTransactionType());
        assertEquals("MANUAL", ledger.getTransferMethod());
        assertEquals(0, BigDecimal.valueOf(300_000).compareTo(ledger.getAmount()));
    }

    @Test
    void 출금계좌_잔액이_부족하면_충전이_실패한다() {
        WalletChargeRequestDto request = chargeRequest(10L, BigDecimal.valueOf(300_000), "key-4");

        when(walletMapper.existsIdempotencyKey("key-4"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletMapper.existsLinkedAccount(100L, 10L))
                .thenReturn(true);
        when(walletMapper.decreaseAccountBalance(1L, 10L, BigDecimal.valueOf(300_000)))
                .thenReturn(0);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.charge(1L, request)
        );

        assertEquals("INSUFFICIENT_ACCOUNT_BALANCE", exception.getErrorCode());
    }

    @Test
    void 잔액이_부족하면_출금_예외가_발생한다() {
        WalletWithdrawRequestDto request = withdrawRequest(20L, BigDecimal.valueOf(2_000_000), "key-5");

        when(walletMapper.existsIdempotencyKey("key-5"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletMapper.existsLinkedAccount(100L, 20L))
                .thenReturn(true);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.withdraw(1L, request)
        );

        assertEquals("INSUFFICIENT_WALLET_BALANCE", exception.getErrorCode());
    }

    @Test
    void 월렛에서_출금하면_잔액이_줄고_원장이_기록된다() {
        WalletWithdrawRequestDto request = withdrawRequest(20L, BigDecimal.valueOf(400_000), "key-6");
        Wallet wallet = createWallet();

        when(walletMapper.existsIdempotencyKey("key-6"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletMapper.existsLinkedAccount(100L, 20L))
                .thenReturn(true);
        when(walletMapper.increaseAccountBalance(1L, 20L, BigDecimal.valueOf(400_000)))
                .thenReturn(1);
        when(walletMapper.updateWalletBalance(100L, BigDecimal.valueOf(600_000), 0L))
                .thenReturn(1);

        WalletCommandResponseDto result = walletService.withdraw(1L, request);

        assertEquals(0, BigDecimal.valueOf(600_000).compareTo(result.getBalanceAmount()));

        ArgumentCaptor<WalletLedger> captor = ArgumentCaptor.forClass(WalletLedger.class);
        verify(walletMapper).insertWalletLedger(captor.capture());
        assertEquals("OUT", captor.getValue().getDirection());
        assertEquals("WITHDRAW", captor.getValue().getTransactionType());
    }

    @Test
    void 직접_입력한_계좌로_출금하고_최근계좌를_저장한다() {
        WalletWithdrawRequestDto request = withdrawRequest(null, BigDecimal.valueOf(200_000), "key-direct");
        ReflectionTestUtils.setField(request, "bankCode", "004");
        ReflectionTestUtils.setField(request, "bankName", "KB국민은행");
        ReflectionTestUtils.setField(request, "accountNumber", "123-456-789012");
        ReflectionTestUtils.setField(request, "accountHolderName", "금융QA");

        when(walletMapper.existsIdempotencyKey("key-direct")).thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L)).thenReturn(createWallet());
        doAnswer(invocation -> {
            WalletWithdrawRecipient recipient = invocation.getArgument(0);
            recipient.setId(77L);
            return 1;
        }).when(walletMapper).upsertWithdrawRecipient(any(WalletWithdrawRecipient.class));
        when(walletMapper.updateWalletBalance(100L, BigDecimal.valueOf(800_000), 0L)).thenReturn(1);

        WalletCommandResponseDto result = walletService.withdraw(1L, request);

        assertEquals(0, BigDecimal.valueOf(800_000).compareTo(result.getBalanceAmount()));
        verify(walletMapper, never()).increaseAccountBalance(anyLong(), anyLong(), any(BigDecimal.class));

        ArgumentCaptor<WalletLedger> ledgerCaptor = ArgumentCaptor.forClass(WalletLedger.class);
        verify(walletMapper).insertWalletLedger(ledgerCaptor.capture());
        assertEquals("RECIPIENT_ACCOUNT", ledgerCaptor.getValue().getTargetType());
        assertEquals(77L, ledgerCaptor.getValue().getTargetId());
    }

    @Test
    void 연동_해제할_계좌가_없으면_예외가_발생한다() {
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletMapper.unlinkWalletAccount(100L, 20L))
                .thenReturn(0);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.unlinkAccount(1L, 20L)
        );

        assertEquals("WALLET_ACCOUNT_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 대표_계좌로_연동하면_기존_대표계좌를_초기화한다() {
        WalletAccountLinkRequestDto request = accountLinkRequest(30L, true);

        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletMapper.existsLinkedAccountByUserId(1L, 30L))
                .thenReturn(true);

        walletService.linkAccount(1L, request);

        verify(walletMapper).resetPrimaryAccount(100L);
        verify(walletMapper).insertWalletAccount(any());
    }

    @Test
    void 자동송금_금액이_0원_이하이면_예외가_발생한다() {
        WalletAutoSavingRequestDto request = autoSavingRequest(BigDecimal.ZERO, 25, true);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.updateAutoSavingRule(1L, request)
        );

        assertEquals("INVALID_AMOUNT", exception.getErrorCode());
    }

    @Test
    void 주계좌가_없으면_자동송금_설정_예외가_발생한다() {
        WalletAutoSavingRequestDto request = autoSavingRequest(BigDecimal.valueOf(500_000), 25, true);

        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletMapper.findPrimaryAccountByWalletId(100L))
                .thenReturn(null);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.updateAutoSavingRule(1L, request)
        );

        assertEquals("PRIMARY_ACCOUNT_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 자동송금_설정은_항상_주계좌를_출금계좌로_저장한다() {
        WalletAutoSavingRequestDto request = autoSavingRequest(BigDecimal.valueOf(500_000), 25, true);
        WalletAccountResponseDto primaryAccount = WalletAccountResponseDto.builder()
                .accountId(11L)
                .accountName("카카오뱅크 입출금통장")
                .build();

        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletMapper.findPrimaryAccountByWalletId(100L))
                .thenReturn(primaryAccount);

        walletService.updateAutoSavingRule(1L, request);

        ArgumentCaptor<WalletAutoSavingRule> captor = ArgumentCaptor.forClass(WalletAutoSavingRule.class);
        verify(walletMapper).upsertAutoSavingRule(captor.capture());
        assertEquals(11L, captor.getValue().getSourceAccountId());
    }

    @Test
    void 대상이_없으면_자동송금_실행건수가_0이다() {
        when(walletMapper.findDueAutoSavingRules(LocalDate.of(2026, 8, 25)))
                .thenReturn(Collections.emptyList());

        int count = walletService.executeDueAutoSavingRules(LocalDate.of(2026, 8, 25));

        assertEquals(0, count);
    }

    @Test
    void 주계좌가_없으면_충전없이_다음_송금일만_갱신하고_실패_기록을_남긴다() {
        WalletAutoSavingRule rule = WalletAutoSavingRule.builder()
                .id(7L)
                .walletId(100L)
                .sourceAccountId(10L)
                .amount(BigDecimal.valueOf(500_000))
                .dayOfMonth(25)
                .build();
        Wallet wallet = createWallet();

        when(walletMapper.findDueAutoSavingRules(LocalDate.of(2026, 8, 25)))
                .thenReturn(List.of(rule));
        when(walletMapper.findWalletById(100L))
                .thenReturn(wallet);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletMapper.findPrimaryAccountByWalletId(100L))
                .thenReturn(null);

        int count = walletService.executeDueAutoSavingRules(LocalDate.of(2026, 8, 25));

        assertEquals(1, count);
        verify(walletMapper).updateAutoSavingNextTransferDate(eq(7L), any());
        verify(walletMapper, never()).insertWalletLedger(any());

        ArgumentCaptor<WalletAutoSavingLog> logCaptor = ArgumentCaptor.forClass(WalletAutoSavingLog.class);
        verify(walletMapper).insertAutoSavingLog(logCaptor.capture());
        assertEquals("FAILED", logCaptor.getValue().getStatus());
        assertEquals(100L, logCaptor.getValue().getWalletId());
    }

    @Test
    void 주계좌_잔액이_부족하면_충전없이_다음_송금일만_갱신하고_실패_기록을_남긴다() {
        WalletAutoSavingRule rule = WalletAutoSavingRule.builder()
                .id(7L)
                .walletId(100L)
                .sourceAccountId(10L)
                .amount(BigDecimal.valueOf(500_000))
                .dayOfMonth(25)
                .build();
        Wallet wallet = createWallet();
        WalletAccountResponseDto primaryAccount = WalletAccountResponseDto.builder()
                .accountId(10L)
                .accountName("신한은행 통장")
                .withdrawableAmount(BigDecimal.valueOf(100_000))
                .build();

        when(walletMapper.findDueAutoSavingRules(LocalDate.of(2026, 8, 25)))
                .thenReturn(List.of(rule));
        when(walletMapper.findWalletById(100L))
                .thenReturn(wallet);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletMapper.findPrimaryAccountByWalletId(100L))
                .thenReturn(primaryAccount);

        int count = walletService.executeDueAutoSavingRules(LocalDate.of(2026, 8, 25));

        assertEquals(1, count);
        verify(walletMapper).updateAutoSavingNextTransferDate(eq(7L), any());
        verify(walletMapper, never()).insertWalletLedger(any());

        ArgumentCaptor<WalletAutoSavingLog> logCaptor = ArgumentCaptor.forClass(WalletAutoSavingLog.class);
        verify(walletMapper).insertAutoSavingLog(logCaptor.capture());
        assertEquals("FAILED", logCaptor.getValue().getStatus());
        assertEquals(100L, logCaptor.getValue().getWalletId());
    }

    @Test
    void 자동송금_대상_규칙을_현재_주계좌에서_충전하고_원장을_남긴다() {
        WalletAutoSavingRule rule = WalletAutoSavingRule.builder()
                .id(7L)
                .walletId(100L)
                .sourceAccountId(10L)
                .amount(BigDecimal.valueOf(500_000))
                .dayOfMonth(25)
                .build();
        Wallet wallet = createWallet();
        WalletAccountResponseDto primaryAccount = WalletAccountResponseDto.builder()
                .accountId(11L)
                .accountName("카카오뱅크 입출금통장")
                .withdrawableAmount(BigDecimal.valueOf(1_000_000))
                .build();

        when(walletMapper.findDueAutoSavingRules(LocalDate.of(2026, 8, 25)))
                .thenReturn(List.of(rule));
        when(walletMapper.findWalletById(100L))
                .thenReturn(wallet);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletMapper.findPrimaryAccountByWalletId(100L))
                .thenReturn(primaryAccount);
        when(walletMapper.decreaseAccountBalance(1L, 11L, BigDecimal.valueOf(500_000)))
                .thenReturn(1);
        when(walletMapper.updateWalletBalance(eq(100L), any(), eq(0L)))
                .thenReturn(1);

        int count = walletService.executeDueAutoSavingRules(LocalDate.of(2026, 8, 25));

        assertEquals(1, count);

        ArgumentCaptor<WalletAutoSavingLog> logCaptor = ArgumentCaptor.forClass(WalletAutoSavingLog.class);
        verify(walletMapper).insertAutoSavingLog(logCaptor.capture());
        assertEquals("SUCCESS", logCaptor.getValue().getStatus());
        assertEquals(100L, logCaptor.getValue().getWalletId());

        ArgumentCaptor<WalletLedger> captor = ArgumentCaptor.forClass(WalletLedger.class);
        verify(walletMapper).insertWalletLedger(captor.capture());
        assertEquals(11L, captor.getValue().getSourceId());
        assertEquals("AUTO_SAVING", captor.getValue().getTransferMethod());
    }

    @Test
    void 트래블카드_상품이_없으면_연동_예외가_발생한다() {
        WalletTravelCardLinkRequestDto request = travelCardLinkRequest(9L, "1234");

        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletMapper.existsTravelCardById(9L))
                .thenReturn(false);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.linkTravelCard(1L, request)
        );

        assertEquals("TRAVEL_CARD_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 연동된_트래블카드가_없으면_해제_예외가_발생한다() {
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletMapper.unlinkWalletTravelCard(100L))
                .thenReturn(0);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.unlinkTravelCard(1L)
        );

        assertEquals("WALLET_TRAVEL_CARD_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 원화로_트래블카드를_충전한다() {
        WalletTravelCardTopupRequestDto request =
                travelCardTopupRequest(5L, "JPY", BigDecimal.valueOf(100_000), "topup-key");
        Wallet wallet = createWallet();
        WalletTravelCard walletTravelCard = WalletTravelCard.builder()
                .id(5L)
                .travelCardId(3L)
                .build();

        when(walletMapper.existsIdempotencyKey("topup-key"))
                .thenReturn(false);
        when(walletMapper.existsTravelCardLedgerIdempotencyKey("topup-key"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletMapper.findWalletTravelCardByIdForUpdate(100L, 5L))
                .thenReturn(walletTravelCard);
        when(walletMapper.existsSupportedCardCurrency(3L, "JPY"))
                .thenReturn(true);
        when(walletMapper.findLatestDealBaseRate("JPY"))
                .thenReturn(BigDecimal.valueOf(9.5));
        when(walletMapper.updateWalletBalance(eq(100L), any(), eq(0L)))
                .thenReturn(1);
        when(walletMapper.findTravelCardBalanceForUpdate(5L, "JPY"))
                .thenReturn(null)
                .thenReturn(TravelCardBalance.builder()
                        .id(50L)
                        .walletTravelCardId(5L)
                        .currencyCode("JPY")
                        .balanceAmount(BigDecimal.ZERO)
                        .krwEstimatedAmount(BigDecimal.ZERO)
                        .build());

        WalletExchangeEstimateResponseDto result = walletService.topupTravelCard(1L, request);

        assertEquals("BUY", result.getExchangeType());
        assertEquals("JPY", result.getCurrencyCode());
        verify(walletMapper).insertWalletCardTopup(any());
        verify(walletMapper).insertWalletExchangeTransaction(any());
    }

    @Test
    void 환전_유형이_BUY이면_매수_예상금액을_반환한다() {
        when(walletMapper.findLatestDealBaseRate("USD"))
                .thenReturn(BigDecimal.valueOf(1_300));

        WalletExchangeEstimateResponseDto result =
                walletService.estimateExchange("buy", "usd", BigDecimal.valueOf(130_000));

        assertEquals("BUY", result.getExchangeType());
        assertEquals("USD", result.getCurrencyCode());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(result.getForeignAmount()));
    }

    @Test
    void 트래블카드_외화잔액이_부족하면_재환전_예외가_발생한다() {
        WalletExchangeSellRequestDto request =
                sellRequest(5L, "JPY", BigDecimal.valueOf(1_000), "sell-key");

        when(walletMapper.existsIdempotencyKey("sell-key"))
                .thenReturn(false);
        when(walletMapper.existsTravelCardLedgerIdempotencyKey("sell-key"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletMapper.findWalletTravelCardByIdForUpdate(100L, 5L))
                .thenReturn(WalletTravelCard.builder().id(5L).travelCardId(3L).build());
        when(walletMapper.findTravelCardBalanceForUpdate(5L, "JPY"))
                .thenReturn(TravelCardBalance.builder()
                        .id(50L)
                        .balanceAmount(BigDecimal.valueOf(500))
                        .build());

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletService.sellExchange(1L, request)
        );

        assertEquals("INSUFFICIENT_TRAVEL_CARD_BALANCE", exception.getErrorCode());
    }

    @Test
    void 기존_월렛이_있으면_새로_생성하지_않는다() {
        Wallet wallet = createWallet();

        when(walletMapper.existsWalletByUserId(1L))
                .thenReturn(true);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);

        Wallet result = walletService.createWalletForUser(1L);

        assertEquals(wallet, result);
        verify(walletMapper, never()).insertWallet(any());
    }

    @Test
    void 월렛이_없으면_새로_생성한다() {
        when(walletMapper.existsWalletByUserId(2L))
                .thenReturn(false);

        Wallet result = walletService.createWalletForUser(2L);

        assertEquals(2L, result.getUserId());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getBalanceAmount()));
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(0L, result.getVersion());
        verify(walletMapper).insertWallet(result);
    }

    private Wallet createWallet() {
        return Wallet.builder()
                .id(100L)
                .userId(1L)
                .balanceAmount(BigDecimal.valueOf(1_000_000))
                .status("ACTIVE")
                .version(0L)
                .build();
    }

    private WalletMainResponseDto createWalletMainResponse() {
        return WalletMainResponseDto.builder()
                .walletId(100L)
                .balanceAmount(BigDecimal.valueOf(1_000_000))
                .totalLinkedAccountBalance(BigDecimal.valueOf(1_000_000))
                .targetAmount(BigDecimal.valueOf(5_000_000))
                .build();
    }

    private WalletChargeRequestDto chargeRequest(
            Long sourceAccountId,
            BigDecimal amount,
            String idempotencyKey
    ) {
        WalletChargeRequestDto request = new WalletChargeRequestDto();
        ReflectionTestUtils.setField(request, "sourceAccountId", sourceAccountId);
        ReflectionTestUtils.setField(request, "amount", amount);
        ReflectionTestUtils.setField(request, "idempotencyKey", idempotencyKey);

        return request;
    }

    private WalletWithdrawRequestDto withdrawRequest(
            Long targetAccountId,
            BigDecimal amount,
            String idempotencyKey
    ) {
        WalletWithdrawRequestDto request = new WalletWithdrawRequestDto();
        ReflectionTestUtils.setField(request, "targetAccountId", targetAccountId);
        ReflectionTestUtils.setField(request, "amount", amount);
        ReflectionTestUtils.setField(request, "idempotencyKey", idempotencyKey);

        return request;
    }

    private WalletAccountLinkRequestDto accountLinkRequest(
            Long accountId,
            Boolean isPrimary
    ) {
        WalletAccountLinkRequestDto request = new WalletAccountLinkRequestDto();
        ReflectionTestUtils.setField(request, "accountId", accountId);
        ReflectionTestUtils.setField(request, "isPrimary", isPrimary);

        return request;
    }

    private WalletAutoSavingRequestDto autoSavingRequest(
            BigDecimal amount,
            Integer dayOfMonth,
            Boolean enabled
    ) {
        WalletAutoSavingRequestDto request = new WalletAutoSavingRequestDto();
        ReflectionTestUtils.setField(request, "amount", amount);
        ReflectionTestUtils.setField(request, "dayOfMonth", dayOfMonth);
        ReflectionTestUtils.setField(request, "enabled", enabled);

        return request;
    }

    private WalletTravelCardLinkRequestDto travelCardLinkRequest(
            Long travelCardId,
            String maskedCardNumber
    ) {
        WalletTravelCardLinkRequestDto request = new WalletTravelCardLinkRequestDto();
        ReflectionTestUtils.setField(request, "travelCardId", travelCardId);
        ReflectionTestUtils.setField(request, "maskedCardNumber", maskedCardNumber);

        return request;
    }

    private WalletTravelCardTopupRequestDto travelCardTopupRequest(
            Long walletTravelCardId,
            String currencyCode,
            BigDecimal krwAmount,
            String idempotencyKey
    ) {
        WalletTravelCardTopupRequestDto request = new WalletTravelCardTopupRequestDto();
        ReflectionTestUtils.setField(request, "walletTravelCardId", walletTravelCardId);
        ReflectionTestUtils.setField(request, "currencyCode", currencyCode);
        ReflectionTestUtils.setField(request, "krwAmount", krwAmount);
        ReflectionTestUtils.setField(request, "idempotencyKey", idempotencyKey);

        return request;
    }

    private WalletExchangeSellRequestDto sellRequest(
            Long walletTravelCardId,
            String currencyCode,
            BigDecimal foreignAmount,
            String idempotencyKey
    ) {
        WalletExchangeSellRequestDto request = new WalletExchangeSellRequestDto();
        ReflectionTestUtils.setField(request, "walletTravelCardId", walletTravelCardId);
        ReflectionTestUtils.setField(request, "currencyCode", currencyCode);
        ReflectionTestUtils.setField(request, "foreignAmount", foreignAmount);
        ReflectionTestUtils.setField(request, "idempotencyKey", idempotencyKey);

        return request;
    }
}
