package com.tripass.wallet.travelcard.service;

import com.tripass.wallet.domain.Wallet;
import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.mapper.WalletMapper;
import com.tripass.wallet.travelcard.client.MockTravelCardClient;
import com.tripass.wallet.travelcard.client.TravelCardTopupResult;
import com.tripass.wallet.travelcard.domain.TravelCardBalance;
import com.tripass.wallet.travelcard.domain.WalletCardTopup;
import com.tripass.wallet.travelcard.domain.WalletTravelCard;
import com.tripass.wallet.travelcard.dto.request.WalletTravelCardLinkRequestDto;
import com.tripass.wallet.travelcard.dto.request.WalletTravelCardTopupRequestDto;
import com.tripass.wallet.travelcard.dto.response.TravelCardCurrencyBalanceResponseDto;
import com.tripass.wallet.travelcard.dto.response.UserTravelCardOptionResponseDto;
import com.tripass.wallet.travelcard.dto.response.WalletCardTopupResponseDto;
import com.tripass.wallet.travelcard.dto.response.WalletTravelCardResponseDto;
import com.tripass.wallet.travelcard.enums.CardTopupStatus;
import com.tripass.wallet.travelcard.mapper.WalletTravelCardMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletTravelCardServiceTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WalletTravelCardMapper walletTravelCardMapper;

    @Mock
    private MockTravelCardClient mockTravelCardClient;

    @InjectMocks
    private WalletTravelCardService walletTravelCardService;

    @Test
    void 월렛이_없으면_트래블카드_조회_예외가_발생한다() {
        when(walletMapper.findWalletByUserId(1L))
                .thenReturn(null);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletTravelCardService.getTravelCard(1L)
        );

        assertEquals("WALLET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 연동된_트래블카드_정보를_조회한다() {
        when(walletMapper.findWalletByUserId(1L))
                .thenReturn(createWallet());
        when(walletTravelCardMapper.findLinkedTravelCardByWalletId(100L))
                .thenReturn(WalletTravelCardResponseDto.builder()
                        .walletTravelCardId(5L)
                        .cardName("트래블로그")
                        .build());

        WalletTravelCardResponseDto result = walletTravelCardService.getTravelCard(1L);

        assertEquals(5L, result.getWalletTravelCardId());
    }

    @Test
    void 사용자가_보유한_트래블카드_후보_목록을_조회한다() {
        when(walletTravelCardMapper.findUserTravelCardOptions(1L))
                .thenReturn(List.of(UserTravelCardOptionResponseDto.builder().userTravelCardId(9L).build()));

        List<UserTravelCardOptionResponseDto> result = walletTravelCardService.getUserTravelCardOptions(1L);

        assertEquals(1, result.size());
    }

    @Test
    void 트래블카드_상품이_없으면_연동_예외가_발생한다() {
        WalletTravelCardLinkRequestDto request = linkRequest(9L, 3L, "1234");

        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletTravelCardMapper.existsTravelCardById(9L))
                .thenReturn(false);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletTravelCardService.linkTravelCard(1L, request)
        );

        assertEquals("TRAVEL_CARD_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 본인이_보유하지_않은_트래블카드이면_연동_예외가_발생한다() {
        WalletTravelCardLinkRequestDto request = linkRequest(9L, 3L, "1234");

        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletTravelCardMapper.existsTravelCardById(9L))
                .thenReturn(true);
        when(walletTravelCardMapper.existsUserTravelCard(1L, 3L))
                .thenReturn(false);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletTravelCardService.linkTravelCard(1L, request)
        );

        assertEquals("TRAVEL_CARD_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 트래블카드를_연동하면_기존_연동을_해제하고_새로_등록한다() {
        WalletTravelCardLinkRequestDto request = linkRequest(9L, 3L, "1234-5678");

        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletTravelCardMapper.existsTravelCardById(9L))
                .thenReturn(true);
        when(walletTravelCardMapper.existsUserTravelCard(1L, 3L))
                .thenReturn(true);

        walletTravelCardService.linkTravelCard(1L, request);

        verify(walletTravelCardMapper).unlinkWalletTravelCard(100L);
        verify(walletTravelCardMapper).insertWalletTravelCard(any());
    }

    @Test
    void 연동된_트래블카드가_없으면_해제_예외가_발생한다() {
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(createWallet());
        when(walletTravelCardMapper.unlinkWalletTravelCard(100L))
                .thenReturn(0);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletTravelCardService.unlinkTravelCard(1L)
        );

        assertEquals("WALLET_TRAVEL_CARD_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 충전건이_없으면_processTopup_예외가_발생한다() {
        when(walletTravelCardMapper.findWalletCardTopupForUpdate(99L))
                .thenReturn(null);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletTravelCardService.processTopup(99L)
        );

        assertEquals("WALLET_TRAVEL_CARD_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 이미_완료된_충전건은_재처리하지_않는다() {
        WalletCardTopup completed = WalletCardTopup.builder()
                .id(10L)
                .status(CardTopupStatus.COMPLETED.name())
                .build();

        when(walletTravelCardMapper.findWalletCardTopupForUpdate(10L))
                .thenReturn(completed);

        WalletCardTopupResponseDto result = walletTravelCardService.processTopup(10L);

        assertEquals("COMPLETED", result.getStatus());
        verify(walletTravelCardMapper, never()).updateWalletCardTopupProcessing(anyLong());
    }

    @Test
    void 목_카드사_충전이_실패하면_원장_변경없이_실패로_기록된다() {
        WalletTravelCardTopupRequestDto request = topupRequest(5L, "JPY", BigDecimal.valueOf(50_000), "FAIL-key");
        Wallet wallet = createWallet();
        WalletCardTopup requestedTopup = WalletCardTopup.builder()
                .id(1L)
                .walletId(100L)
                .walletTravelCardId(5L)
                .currencyCode("JPY")
                .krwAmount(BigDecimal.valueOf(50_000))
                .foreignAmount(BigDecimal.valueOf(5263.15))
                .status(CardTopupStatus.REQUESTED.name())
                .idempotencyKey("FAIL-key")
                .retryCount(0)
                .refunded(false)
                .build();

        when(walletTravelCardMapper.existsWalletCardTopupIdempotencyKey("FAIL-key"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletTravelCardMapper.findWalletTravelCardForUpdate(100L, 5L))
                .thenReturn(WalletTravelCard.builder().id(5L).travelCardId(3L).build());
        when(walletTravelCardMapper.existsSupportedCardCurrency(3L, "JPY"))
                .thenReturn(true);
        when(walletTravelCardMapper.findLatestDealBaseRate("JPY"))
                .thenReturn(BigDecimal.valueOf(9.5));
        when(walletTravelCardMapper.findWalletCardTopupForUpdate(null))
                .thenReturn(requestedTopup);
        when(walletTravelCardMapper.findWalletCardTopupForUpdate(1L))
                .thenReturn(requestedTopup);
        when(mockTravelCardClient.requestTopup(requestedTopup, "FAIL-key"))
                .thenReturn(TravelCardTopupResult.failed("MOCK_FORCED_FAIL", "강제 실패"));

        WalletCardTopupResponseDto result = walletTravelCardService.topup(1L, request);

        assertNotNull(result);
        verify(walletTravelCardMapper).updateWalletCardTopupFailed(eq(1L), contains("MOCK_FORCED_FAIL"));
        verify(walletMapper, never()).updateWalletBalance(anyLong(), any(), anyLong());
    }

    @Test
    void 목_카드사_충전이_성공하면_월렛과_카드_잔액이_반영된다() {
        WalletTravelCardTopupRequestDto request = topupRequest(5L, "JPY", BigDecimal.valueOf(50_000), "topup-ok");
        Wallet wallet = createWallet();
        WalletCardTopup requestedTopup = WalletCardTopup.builder()
                .id(1L)
                .walletId(100L)
                .walletTravelCardId(5L)
                .currencyCode("JPY")
                .krwAmount(BigDecimal.valueOf(50_000))
                .foreignAmount(BigDecimal.valueOf(5263.15))
                .status(CardTopupStatus.REQUESTED.name())
                .idempotencyKey("topup-ok")
                .retryCount(0)
                .refunded(false)
                .build();

        when(walletTravelCardMapper.existsWalletCardTopupIdempotencyKey("topup-ok"))
                .thenReturn(false);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletTravelCardMapper.findWalletTravelCardForUpdate(100L, 5L))
                .thenReturn(WalletTravelCard.builder().id(5L).travelCardId(3L).build());
        when(walletTravelCardMapper.existsSupportedCardCurrency(3L, "JPY"))
                .thenReturn(true);
        when(walletTravelCardMapper.findLatestDealBaseRate("JPY"))
                .thenReturn(BigDecimal.valueOf(9.5));
        when(walletTravelCardMapper.findWalletCardTopupForUpdate(null))
                .thenReturn(requestedTopup);
        when(walletTravelCardMapper.findWalletCardTopupForUpdate(1L))
                .thenReturn(requestedTopup);
        when(mockTravelCardClient.requestTopup(requestedTopup, "topup-ok"))
                .thenReturn(TravelCardTopupResult.succeeded("EXT-1"));
        when(walletMapper.findWalletById(100L))
                .thenReturn(wallet);
        when(walletMapper.updateWalletBalance(eq(100L), any(), eq(0L)))
                .thenReturn(1);
        when(walletTravelCardMapper.findTravelCardBalanceForUpdate(5L, "JPY"))
                .thenReturn(null)
                .thenReturn(TravelCardBalance.builder()
                        .id(50L)
                        .walletTravelCardId(5L)
                        .currencyCode("JPY")
                        .balanceAmount(BigDecimal.ZERO)
                        .krwEstimatedAmount(BigDecimal.ZERO)
                        .build());

        WalletCardTopupResponseDto result = walletTravelCardService.topup(1L, request);

        assertEquals("JPY", result.getCurrencyCode());
        assertEquals(5L, result.getWalletTravelCardId());
        verify(walletTravelCardMapper).updateWalletCardTopupCompleted(eq(1L), any(), any(), eq("EXT-1"));
        verify(walletTravelCardMapper).insertWalletExchangeTransaction(any());
        verify(walletTravelCardMapper, never()).updateWalletCardTopupFailed(anyLong(), anyString());
    }

    @Test
    void 활성_트래블카드가_없으면_외화잔액_목록이_빈다() {
        when(walletMapper.findWalletByUserId(1L))
                .thenReturn(createWallet());
        when(walletTravelCardMapper.findActiveWalletTravelCardByWalletId(100L))
                .thenReturn(null);

        List<TravelCardCurrencyBalanceResponseDto> result = walletTravelCardService.getBalances(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void 활성_트래블카드가_있으면_외화잔액_목록을_조회한다() {
        when(walletMapper.findWalletByUserId(1L))
                .thenReturn(createWallet());
        when(walletTravelCardMapper.findActiveWalletTravelCardByWalletId(100L))
                .thenReturn(WalletTravelCard.builder().id(5L).build());
        when(walletTravelCardMapper.findTravelCardBalances(5L))
                .thenReturn(List.of(TravelCardCurrencyBalanceResponseDto.builder().currencyCode("JPY").build()));

        List<TravelCardCurrencyBalanceResponseDto> result = walletTravelCardService.getBalances(1L);

        assertEquals(1, result.size());
    }

    @Test
    void 활성_트래블카드가_없으면_충전내역이_빈다() {
        when(walletMapper.findWalletByUserId(1L))
                .thenReturn(createWallet());
        when(walletTravelCardMapper.findActiveWalletTravelCardByWalletId(100L))
                .thenReturn(null);

        List<?> result = walletTravelCardService.getLedgers(1L);

        assertTrue(result.isEmpty());
        verify(walletTravelCardMapper, never()).findTravelCardLedgers(anyLong());
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

    private WalletTravelCardLinkRequestDto linkRequest(
            Long travelCardId,
            Long userTravelCardId,
            String maskedCardNumber
    ) {
        WalletTravelCardLinkRequestDto request = new WalletTravelCardLinkRequestDto();
        request.setTravelCardId(travelCardId);
        request.setUserTravelCardId(userTravelCardId);
        request.setMaskedCardNumber(maskedCardNumber);

        return request;
    }

    private WalletTravelCardTopupRequestDto topupRequest(
            Long walletTravelCardId,
            String currencyCode,
            BigDecimal krwAmount,
            String idempotencyKey
    ) {
        WalletTravelCardTopupRequestDto request = new WalletTravelCardTopupRequestDto();
        request.setWalletTravelCardId(walletTravelCardId);
        request.setCurrencyCode(currencyCode);
        request.setKrwAmount(krwAmount);
        request.setIdempotencyKey(idempotencyKey);

        return request;
    }
}
