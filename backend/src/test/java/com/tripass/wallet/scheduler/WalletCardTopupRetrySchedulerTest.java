package com.tripass.wallet.scheduler;

import com.tripass.wallet.travelcard.domain.WalletCardTopup;
import com.tripass.wallet.travelcard.dto.response.WalletCardTopupResponseDto;
import com.tripass.wallet.travelcard.enums.CardTopupStatus;
import com.tripass.wallet.travelcard.mapper.WalletTravelCardMapper;
import com.tripass.wallet.travelcard.service.WalletCardTopupRefundService;
import com.tripass.wallet.travelcard.service.WalletTravelCardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletCardTopupRetrySchedulerTest {

    @Mock
    private WalletTravelCardMapper walletTravelCardMapper;

    @Mock
    private WalletTravelCardService walletTravelCardService;

    @Mock
    private WalletCardTopupRefundService walletCardTopupRefundService;

    @InjectMocks
    private WalletCardTopupRetryScheduler walletCardTopupRetryScheduler;

    @Test
    void 재시도_대상이_없으면_아무_처리도_하지_않는다() {
        when(walletTravelCardMapper.findRetryableTopups(30))
                .thenReturn(Collections.emptyList());

        walletCardTopupRetryScheduler.retryPendingTopups();

        verifyNoInteractions(walletTravelCardService);
        verifyNoInteractions(walletCardTopupRefundService);
    }

    @Test
    void 재시도가_성공하면_환불을_호출하지_않는다() {
        WalletCardTopup topup = WalletCardTopup.builder().id(1L).build();

        when(walletTravelCardMapper.findRetryableTopups(30))
                .thenReturn(List.of(topup));
        when(walletTravelCardService.processTopup(1L))
                .thenReturn(WalletCardTopupResponseDto.builder()
                        .topupId(1L)
                        .status(CardTopupStatus.COMPLETED.name())
                        .build());

        walletCardTopupRetryScheduler.retryPendingTopups();

        verify(walletTravelCardService).processTopup(1L);
        verifyNoInteractions(walletCardTopupRefundService);
    }

    @Test
    void 재시도가_실패했지만_최대_재시도_미만이면_환불하지_않는다() {
        WalletCardTopup topup = WalletCardTopup.builder().id(1L).build();

        when(walletTravelCardMapper.findRetryableTopups(30))
                .thenReturn(List.of(topup));
        when(walletTravelCardService.processTopup(1L))
                .thenReturn(WalletCardTopupResponseDto.builder()
                        .topupId(1L)
                        .status(CardTopupStatus.FAILED.name())
                        .retryCount(2)
                        .build());

        walletCardTopupRetryScheduler.retryPendingTopups();

        verifyNoInteractions(walletCardTopupRefundService);
    }

    @Test
    void 재시도가_최대_횟수에_도달하면_환불을_처리한다() {
        WalletCardTopup topup = WalletCardTopup.builder().id(1L).build();

        when(walletTravelCardMapper.findRetryableTopups(30))
                .thenReturn(List.of(topup));
        when(walletTravelCardService.processTopup(1L))
                .thenReturn(WalletCardTopupResponseDto.builder()
                        .topupId(1L)
                        .status(CardTopupStatus.FAILED.name())
                        .retryCount(3)
                        .build());

        walletCardTopupRetryScheduler.retryPendingTopups();

        verify(walletCardTopupRefundService).refund(1L);
    }

    @Test
    void 한_건이_예외로_실패해도_다음_건은_계속_재시도된다() {
        WalletCardTopup failing = WalletCardTopup.builder().id(1L).build();
        WalletCardTopup succeeding = WalletCardTopup.builder().id(2L).build();

        when(walletTravelCardMapper.findRetryableTopups(30))
                .thenReturn(List.of(failing, succeeding));
        when(walletTravelCardService.processTopup(1L))
                .thenThrow(new RuntimeException("목 카드사 통신 오류"));
        when(walletTravelCardService.processTopup(2L))
                .thenReturn(WalletCardTopupResponseDto.builder()
                        .topupId(2L)
                        .status(CardTopupStatus.COMPLETED.name())
                        .build());

        assertDoesNotThrow(() -> walletCardTopupRetryScheduler.retryPendingTopups());

        verify(walletTravelCardService).processTopup(1L);
        verify(walletTravelCardService).processTopup(2L);
    }
}
