package com.tripass.wallet.fx.service;

import com.tripass.wallet.domain.Wallet;
import com.tripass.wallet.domain.WalletLedger;
import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.fx.dto.request.WalletExchangeEstimateRequestDto;
import com.tripass.wallet.fx.dto.request.WalletExchangeSellRequestDto;
import com.tripass.wallet.fx.dto.response.WalletCurrencyResponseDto;
import com.tripass.wallet.fx.dto.response.WalletExchangeEstimateResponseDto;
import com.tripass.wallet.fx.dto.response.WalletExchangeResponseDto;
import com.tripass.wallet.fx.mapper.WalletFxMapper;
import com.tripass.wallet.mapper.WalletMapper;
import com.tripass.wallet.travelcard.domain.TravelCardBalance;
import com.tripass.wallet.travelcard.domain.WalletTravelCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletFxServiceTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WalletFxMapper walletFxMapper;

    @InjectMocks
    private WalletFxService walletFxService;

    @Test
    void 환전_통화_목록을_조회한다() {
        when(walletFxMapper.findCurrencies())
                .thenReturn(List.of(new WalletCurrencyResponseDto()));

        List<WalletCurrencyResponseDto> result = walletFxService.getCurrencies();

        assertEquals(1, result.size());
    }

    @Test
    void 환전_유형이_올바르지_않으면_예상금액_계산_예외가_발생한다() {
        WalletExchangeEstimateRequestDto request = estimateRequest("swap", "usd", BigDecimal.valueOf(1_000));

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletFxService.estimate(request)
        );

        assertEquals("INVALID_AMOUNT", exception.getErrorCode());
    }

    @Test
    void BUY_유형이면_원화를_외화로_환산한_예상금액을_반환한다() {
        WalletExchangeEstimateRequestDto request = estimateRequest("buy", "usd", BigDecimal.valueOf(130_000));

        when(walletFxMapper.findLatestDealBaseRate("USD"))
                .thenReturn(BigDecimal.valueOf(1_300));

        WalletExchangeEstimateResponseDto result = walletFxService.estimate(request);

        assertEquals("BUY", result.getExchangeType());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(result.getForeignAmount()));
    }

    @Test
    void SELL_유형이면_외화를_원화로_환산한_예상금액을_반환한다() {
        WalletExchangeEstimateRequestDto request = estimateRequest("sell", "usd", BigDecimal.valueOf(100));

        when(walletFxMapper.findLatestDealBaseRate("USD"))
                .thenReturn(BigDecimal.valueOf(1_300));

        WalletExchangeEstimateResponseDto result = walletFxService.estimate(request);

        assertEquals("SELL", result.getExchangeType());
        assertEquals(0, BigDecimal.valueOf(130_000).compareTo(result.getKrwAmount()));
    }

    @Test
    void 트래블카드_외화잔액이_없으면_재환전_예외가_발생한다() {
        WalletExchangeSellRequestDto request = sellRequest(5L, "JPY", BigDecimal.valueOf(1_000), "sell-key");

        when(walletMapper.existsIdempotencyKey("sell-key"))
                .thenReturn(false);
        when(walletFxMapper.existsTravelCardLedgerIdempotencyKey("sell-key"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletFxMapper.findWalletTravelCardForUpdate(100L, 5L))
                .thenReturn(WalletTravelCard.builder().id(5L).build());
        when(walletFxMapper.findTravelCardBalanceForUpdate(5L, "JPY"))
                .thenReturn(null);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletFxService.sell(1L, request)
        );

        assertEquals("INSUFFICIENT_TRAVEL_CARD_BALANCE", exception.getErrorCode());
    }

    @Test
    void 외화를_원화로_재환전하면_월렛_잔액이_늘고_카드_잔액이_줄어든다() {
        WalletExchangeSellRequestDto request = sellRequest(5L, "JPY", BigDecimal.valueOf(1_000), "sell-ok");
        Wallet wallet = createWallet();

        when(walletMapper.existsIdempotencyKey("sell-ok"))
                .thenReturn(false);
        when(walletFxMapper.existsTravelCardLedgerIdempotencyKey("sell-ok"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletFxMapper.findWalletTravelCardForUpdate(100L, 5L))
                .thenReturn(WalletTravelCard.builder().id(5L).build());
        when(walletFxMapper.findTravelCardBalanceForUpdate(5L, "JPY"))
                .thenReturn(TravelCardBalance.builder()
                        .id(50L)
                        .balanceAmount(BigDecimal.valueOf(2_000))
                        .build());
        when(walletFxMapper.findLatestDealBaseRate("JPY"))
                .thenReturn(BigDecimal.valueOf(9.5));
        when(walletMapper.updateWalletBalance(eq(100L), any(), eq(0L)))
                .thenReturn(1);

        WalletExchangeResponseDto result = walletFxService.sell(1L, request);

        assertEquals("JPY", result.getCurrencyCode());
        assertEquals(0, BigDecimal.valueOf(1_000).compareTo(result.getTravelCardBalanceAfter()));
        assertEquals(0, BigDecimal.valueOf(1_009_500).compareTo(result.getWalletBalanceAfter()));

        ArgumentCaptor<WalletLedger> captor = ArgumentCaptor.forClass(WalletLedger.class);
        verify(walletMapper).insertWalletLedger(captor.capture());
        assertEquals("IN", captor.getValue().getDirection());
        assertEquals("EXCHANGE_SELL", captor.getValue().getTransactionType());

        verify(walletFxMapper).insertTravelCardLedger(any());
        verify(walletFxMapper).insertWalletExchangeTransaction(any());
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

    private WalletExchangeEstimateRequestDto estimateRequest(
            String exchangeType,
            String currencyCode,
            BigDecimal amount
    ) {
        WalletExchangeEstimateRequestDto request = new WalletExchangeEstimateRequestDto();
        request.setExchangeType(exchangeType);
        request.setCurrencyCode(currencyCode);
        request.setAmount(amount);

        return request;
    }

    private WalletExchangeSellRequestDto sellRequest(
            Long walletTravelCardId,
            String currencyCode,
            BigDecimal foreignAmount,
            String idempotencyKey
    ) {
        WalletExchangeSellRequestDto request = new WalletExchangeSellRequestDto();
        request.setWalletTravelCardId(walletTravelCardId);
        request.setCurrencyCode(currencyCode);
        request.setForeignAmount(foreignAmount);
        request.setIdempotencyKey(idempotencyKey);

        return request;
    }
}
