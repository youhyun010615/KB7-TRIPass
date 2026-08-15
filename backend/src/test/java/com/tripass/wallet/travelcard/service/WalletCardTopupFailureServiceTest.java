package com.tripass.wallet.travelcard.service;

import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.travelcard.mapper.WalletTravelCardMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletCardTopupFailureServiceTest {

    @Mock
    private WalletTravelCardMapper walletTravelCardMapper;

    @InjectMocks
    private WalletCardTopupFailureService walletCardTopupFailureService;

    @Test
    void 대상_충전건이_없으면_실패_기록_예외가_발생한다() {
        when(walletTravelCardMapper.updateWalletCardTopupFailed(99L, "MOCK_FAIL|retry=0|사유"))
                .thenReturn(0);

        WalletException exception = assertThrows(
                WalletException.class,
                () -> walletCardTopupFailureService.markFailed(99L, "MOCK_FAIL|retry=0|사유")
        );

        assertEquals("WALLET_TRAVEL_CARD_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void 충전건의_실패_사유를_기록한다() {
        when(walletTravelCardMapper.updateWalletCardTopupFailed(10L, "MOCK_FORCED_FAIL|retry=1|강제 실패"))
                .thenReturn(1);

        walletCardTopupFailureService.markFailed(10L, "MOCK_FORCED_FAIL|retry=1|강제 실패");

        verify(walletTravelCardMapper).updateWalletCardTopupFailed(10L, "MOCK_FORCED_FAIL|retry=1|강제 실패");
    }
}
