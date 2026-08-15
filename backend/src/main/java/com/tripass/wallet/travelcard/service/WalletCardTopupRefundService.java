package com.tripass.wallet.travelcard.service;

import com.tripass.wallet.domain.Wallet;
import com.tripass.wallet.domain.WalletLedger;
import com.tripass.wallet.enums.WalletDirection;
import com.tripass.wallet.enums.WalletSourceType;
import com.tripass.wallet.enums.WalletTargetType;
import com.tripass.wallet.enums.WalletTransactionType;
import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.mapper.WalletMapper;
import com.tripass.wallet.travelcard.domain.WalletCardTopup;
import com.tripass.wallet.travelcard.mapper.WalletTravelCardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.tripass.wallet.exception.WalletErrorCode.WALLET_CONFLICT;
import static com.tripass.wallet.exception.WalletErrorCode.WALLET_NOT_FOUND;
import static com.tripass.wallet.exception.WalletErrorCode.WALLET_TRAVEL_CARD_NOT_FOUND;

/** 트래블카드 충전 실패 또는 취소 건을 월렛 원화 잔액으로 보상 환불 처리하는 서비스입니다. */

@Service
@RequiredArgsConstructor
@Transactional
public class WalletCardTopupRefundService {

    private static final String REFUND_KEY_PREFIX = "REFUND:TOPUP:";

    private final WalletMapper walletMapper;
    private final WalletTravelCardMapper walletTravelCardMapper;

    public void refund(Long topupId) {
        WalletCardTopup topup = walletTravelCardMapper.findWalletCardTopupForUpdate(topupId);

        if (topup == null) {
            throw new WalletException(WALLET_TRAVEL_CARD_NOT_FOUND);
        }

        if (Boolean.TRUE.equals(topup.getRefunded())) {
            return;
        }

        if (topup.getWalletLedgerId() == null) {
            return;
        }

        String refundKey = REFUND_KEY_PREFIX + topup.getId();

        if (walletMapper.existsIdempotencyKey(refundKey)) {
            walletTravelCardMapper.updateWalletCardTopupRefunded(topup.getId(), topup.getWalletLedgerId());
            return;
        }

        Wallet wallet = walletMapper.findWalletById(topup.getWalletId());

        if (wallet == null) {
            throw new WalletException(WALLET_NOT_FOUND);
        }

        wallet = walletMapper.findWalletByUserIdForUpdate(wallet.getUserId());
        BigDecimal nextBalance = wallet.getBalanceAmount().add(topup.getKrwAmount());

        int count = walletMapper.updateWalletBalance(wallet.getId(), nextBalance, wallet.getVersion());

        if (count == 0) {
            throw new WalletException(WALLET_CONFLICT);
        }

        WalletLedger refundLedger = WalletLedger.builder()
                .walletId(wallet.getId())
                .direction(WalletDirection.IN.name())
                .transactionType(WalletTransactionType.REFUND.name())
                .amount(topup.getKrwAmount())
                .balanceBefore(wallet.getBalanceAmount())
                .balanceAfter(nextBalance)
                .sourceType(WalletSourceType.TRAVEL_CARD.name())
                .sourceId(topup.getWalletTravelCardId())
                .targetType(WalletTargetType.WALLET.name())
                .targetId(wallet.getId())
                .idempotencyKey(refundKey)
                .memo("트래블카드 충전 실패 환불")
                .build();

        walletMapper.insertWalletLedger(refundLedger);
        walletTravelCardMapper.updateWalletCardTopupRefunded(topup.getId(), refundLedger.getId());
    }
}
