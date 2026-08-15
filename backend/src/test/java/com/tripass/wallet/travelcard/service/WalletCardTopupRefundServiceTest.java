package com.tripass.wallet.travelcard.service;

import com.tripass.wallet.domain.Wallet;
import com.tripass.wallet.domain.WalletLedger;
import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.mapper.WalletMapper;
import com.tripass.wallet.travelcard.domain.WalletCardTopup;
import com.tripass.wallet.travelcard.mapper.WalletTravelCardMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletCardTopupRefundServiceTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WalletTravelCardMapper walletTravelCardMapper;

    @InjectMocks
    private WalletCardTopupRefundService walletCardTopupRefundService;

    @Test
    void 충전건이_없으면_환불_예외가_발생한다() {
        when(walletTravelCardMapper.findWalletCardTopupForUpdate(99L))
                .thenReturn(null);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletCardTopupRefundService.refund(99L)
        );

        assertEquals("WALLET_TRAVEL_CARD_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 이미_환불된_충전건은_아무_작업도_하지_않는다() {
        WalletCardTopup topup = WalletCardTopup.builder()
                .id(10L)
                .refunded(true)
                .build();

        when(walletTravelCardMapper.findWalletCardTopupForUpdate(10L))
                .thenReturn(topup);

        walletCardTopupRefundService.refund(10L);

        verifyNoInteractions(walletMapper);
    }

    @Test
    void 월렛_원장이_없는_충전건은_아무_작업도_하지_않는다() {
        WalletCardTopup topup = WalletCardTopup.builder()
                .id(10L)
                .refunded(false)
                .walletLedgerId(null)
                .build();

        when(walletTravelCardMapper.findWalletCardTopupForUpdate(10L))
                .thenReturn(topup);

        walletCardTopupRefundService.refund(10L);

        verifyNoInteractions(walletMapper);
    }

    @Test
    void 이미_환불_멱등키가_존재하면_원장_추가없이_환불_상태만_보정한다() {
        WalletCardTopup topup = WalletCardTopup.builder()
                .id(10L)
                .refunded(false)
                .walletLedgerId(55L)
                .build();

        when(walletTravelCardMapper.findWalletCardTopupForUpdate(10L))
                .thenReturn(topup);
        when(walletMapper.existsIdempotencyKey("REFUND:TOPUP:10"))
                .thenReturn(true);

        walletCardTopupRefundService.refund(10L);

        verify(walletTravelCardMapper).updateWalletCardTopupRefunded(10L, 55L);
        verify(walletMapper, never()).insertWalletLedger(any());
    }

    @Test
    void 월렛이_없으면_환불_예외가_발생한다() {
        WalletCardTopup topup = WalletCardTopup.builder()
                .id(10L)
                .walletId(100L)
                .refunded(false)
                .walletLedgerId(55L)
                .build();

        when(walletTravelCardMapper.findWalletCardTopupForUpdate(10L))
                .thenReturn(topup);
        when(walletMapper.existsIdempotencyKey("REFUND:TOPUP:10"))
                .thenReturn(false);
        when(walletMapper.findWalletById(100L))
                .thenReturn(null);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletCardTopupRefundService.refund(10L)
        );

        assertEquals("WALLET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 낙관적_락_충돌이면_환불_예외가_발생한다() {
        WalletCardTopup topup = WalletCardTopup.builder()
                .id(10L)
                .walletId(100L)
                .krwAmount(BigDecimal.valueOf(50_000))
                .refunded(false)
                .walletLedgerId(55L)
                .build();
        Wallet wallet = createWallet();

        when(walletTravelCardMapper.findWalletCardTopupForUpdate(10L))
                .thenReturn(topup);
        when(walletMapper.existsIdempotencyKey("REFUND:TOPUP:10"))
                .thenReturn(false);
        when(walletMapper.findWalletById(100L))
                .thenReturn(wallet);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletMapper.updateWalletBalance(100L, BigDecimal.valueOf(1_050_000), 0L))
                .thenReturn(0);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletCardTopupRefundService.refund(10L)
        );

        assertEquals("WALLET_CONFLICT", exception.getErrorCode());
    }

    @Test
    void 충전_실패건을_환불하면_잔액이_증가하고_원장이_기록된다() {
        WalletCardTopup topup = WalletCardTopup.builder()
                .id(10L)
                .walletId(100L)
                .walletTravelCardId(5L)
                .krwAmount(BigDecimal.valueOf(50_000))
                .refunded(false)
                .walletLedgerId(55L)
                .build();
        Wallet wallet = createWallet();

        when(walletTravelCardMapper.findWalletCardTopupForUpdate(10L))
                .thenReturn(topup);
        when(walletMapper.existsIdempotencyKey("REFUND:TOPUP:10"))
                .thenReturn(false);
        when(walletMapper.findWalletById(100L))
                .thenReturn(wallet);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletMapper.updateWalletBalance(100L, BigDecimal.valueOf(1_050_000), 0L))
                .thenReturn(1);

        walletCardTopupRefundService.refund(10L);

        ArgumentCaptor<WalletLedger> captor = ArgumentCaptor.forClass(WalletLedger.class);
        verify(walletMapper).insertWalletLedger(captor.capture());

        WalletLedger ledger = captor.getValue();
        assertEquals("IN", ledger.getDirection());
        assertEquals("REFUND", ledger.getTransactionType());
        assertEquals(0, BigDecimal.valueOf(50_000).compareTo(ledger.getAmount()));

        verify(walletTravelCardMapper).updateWalletCardTopupRefunded(eq(10L), any());
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
}
