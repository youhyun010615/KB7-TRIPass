package com.tripass.wallet.travelcard.service;

import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.travelcard.mapper.WalletTravelCardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.tripass.wallet.exception.WalletErrorCode.WALLET_TRAVEL_CARD_NOT_FOUND;

/** 트래블카드 충전 실패 건의 실패 사유 기록과 상태 변경을 담당하는 서비스입니다. */

@Service
@RequiredArgsConstructor
@Transactional
public class WalletCardTopupFailureService {

    private final WalletTravelCardMapper walletTravelCardMapper;

    public void markFailed(
            Long topupId,
            String failureReason
    ) {
        int count = walletTravelCardMapper.updateWalletCardTopupFailed(
                topupId, failureReason, java.time.LocalDateTime.now());

        if (count == 0) {
            throw new WalletException(WALLET_TRAVEL_CARD_NOT_FOUND);
        }
    }
}
