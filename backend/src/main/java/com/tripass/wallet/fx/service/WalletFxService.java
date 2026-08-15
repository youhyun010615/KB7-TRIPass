package com.tripass.wallet.fx.service;

import com.tripass.wallet.domain.Wallet;
import com.tripass.wallet.domain.WalletLedger;
import com.tripass.wallet.enums.WalletDirection;
import com.tripass.wallet.enums.WalletSourceType;
import com.tripass.wallet.enums.WalletTargetType;
import com.tripass.wallet.enums.WalletTransactionType;
import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.fx.domain.WalletExchangeTransaction;
import com.tripass.wallet.fx.dto.request.WalletExchangeEstimateRequestDto;
import com.tripass.wallet.fx.dto.request.WalletExchangeSellRequestDto;
import com.tripass.wallet.fx.dto.response.WalletCurrencyResponseDto;
import com.tripass.wallet.fx.dto.response.WalletExchangeEstimateResponseDto;
import com.tripass.wallet.fx.dto.response.WalletExchangeResponseDto;
import com.tripass.wallet.fx.enums.ExchangeStatus;
import com.tripass.wallet.fx.enums.ExchangeType;
import com.tripass.wallet.fx.mapper.WalletFxMapper;
import com.tripass.wallet.mapper.WalletMapper;
import com.tripass.wallet.travelcard.domain.TravelCardBalance;
import com.tripass.wallet.travelcard.domain.TravelCardLedger;
import com.tripass.wallet.travelcard.domain.WalletTravelCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static com.tripass.wallet.exception.WalletErrorCode.*;

/** 월렛 원화와 트래블카드 외화 사이 환전 예상 계산과 재환전 처리를 담당하는 서비스입니다. */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletFxService {

    private static final BigDecimal ZERO_FEE_RATE = BigDecimal.ZERO.setScale(4);
    private static final BigDecimal ZERO_FEE_AMOUNT = BigDecimal.ZERO.setScale(0);

    private final WalletMapper walletMapper;
    private final WalletFxMapper walletFxMapper;

    public List<WalletCurrencyResponseDto> getCurrencies() {
        return walletFxMapper.findCurrencies();
    }

    public WalletExchangeEstimateResponseDto estimate(WalletExchangeEstimateRequestDto request) {
        String exchangeType = normalizeExchangeType(request.getExchangeType());
        String currencyCode = normalizeCurrencyCode(request.getCurrencyCode());
        validatePositiveAmount(request.getAmount());

        if (ExchangeType.BUY.name().equals(exchangeType)) {
            return estimateBuy(currencyCode, request.getAmount());
        }

        return estimateSell(currencyCode, request.getAmount());
    }

    @Transactional
    public WalletExchangeResponseDto sell(
            Long userId,
            WalletExchangeSellRequestDto request
    ) {
        String currencyCode = normalizeCurrencyCode(request.getCurrencyCode());
        validatePositiveAmount(request.getForeignAmount());
        validateIdempotencyKey(request.getIdempotencyKey());
        validateTravelCardLedgerIdempotencyKey(request.getIdempotencyKey());

        Wallet wallet = getWalletForUpdate(userId);
        WalletTravelCard walletTravelCard = getWalletTravelCardForUpdate(
                wallet.getId(),
                request.getWalletTravelCardId()
        );
        TravelCardBalance cardBalance = getTravelCardBalanceForUpdate(
                walletTravelCard.getId(),
                currencyCode
        );

        if (cardBalance.getBalanceAmount().compareTo(request.getForeignAmount()) < 0) {
            throw new WalletException(INSUFFICIENT_TRAVEL_CARD_BALANCE);
        }

        WalletExchangeEstimateResponseDto estimate = estimateSell(currencyCode, request.getForeignAmount());
        BigDecimal nextCardBalance = cardBalance.getBalanceAmount().subtract(request.getForeignAmount());
        BigDecimal krwEstimatedAmount = nextCardBalance
                .multiply(estimate.getBaseExchangeRate())
                .setScale(0, RoundingMode.HALF_UP);

        walletFxMapper.updateTravelCardBalance(
                cardBalance.getId(),
                nextCardBalance,
                krwEstimatedAmount
        );

        TravelCardLedger cardLedger = insertTravelCardLedger(
                walletTravelCard.getId(),
                currencyCode,
                WalletDirection.OUT.name(),
                WalletTransactionType.EXCHANGE_SELL.name(),
                request.getForeignAmount(),
                cardBalance.getBalanceAmount(),
                nextCardBalance,
                WalletSourceType.TRAVEL_CARD.name(),
                walletTravelCard.getId(),
                WalletTargetType.WALLET.name(),
                wallet.getId(),
                request.getIdempotencyKey(),
                "트래블카드 외화 재환전"
        );

        BigDecimal nextWalletBalance = wallet.getBalanceAmount().add(estimate.getKrwAmount());
        updateWalletBalance(wallet, nextWalletBalance);

        WalletLedger walletLedger = insertWalletLedger(
                wallet.getId(),
                WalletDirection.IN,
                WalletTransactionType.EXCHANGE_SELL,
                estimate.getKrwAmount(),
                wallet.getBalanceAmount(),
                nextWalletBalance,
                WalletSourceType.TRAVEL_CARD,
                walletTravelCard.getId(),
                WalletTargetType.WALLET,
                wallet.getId(),
                request.getIdempotencyKey(),
                "트래블카드 외화 원화 재환전"
        );

        WalletExchangeTransaction exchangeTransaction = WalletExchangeTransaction.builder()
                .walletId(wallet.getId())
                .walletTravelCardId(walletTravelCard.getId())
                .currencyCode(currencyCode)
                .exchangeType(ExchangeType.SELL.name())
                .krwAmount(estimate.getKrwAmount())
                .foreignAmount(estimate.getForeignAmount())
                .baseExchangeRate(estimate.getBaseExchangeRate())
                .appliedExchangeRate(estimate.getAppliedExchangeRate())
                .feeRate(estimate.getFeeRate())
                .feeAmount(estimate.getFeeAmount())
                .status(ExchangeStatus.COMPLETED.name())
                .completedAt(LocalDateTime.now())
                .walletLedgerId(walletLedger.getId())
                .cardLedgerId(cardLedger.getId())
                .build();

        walletFxMapper.insertWalletExchangeTransaction(exchangeTransaction);

        return WalletExchangeResponseDto.builder()
                .exchangeTransactionId(exchangeTransaction.getId())
                .walletId(wallet.getId())
                .walletTravelCardId(walletTravelCard.getId())
                .currencyCode(currencyCode)
                .exchangeType(ExchangeType.SELL.name())
                .krwAmount(estimate.getKrwAmount())
                .foreignAmount(estimate.getForeignAmount())
                .baseExchangeRate(estimate.getBaseExchangeRate())
                .appliedExchangeRate(estimate.getAppliedExchangeRate())
                .feeRate(estimate.getFeeRate())
                .feeAmount(estimate.getFeeAmount())
                .status(ExchangeStatus.COMPLETED.name())
                .walletBalanceAfter(nextWalletBalance)
                .travelCardBalanceAfter(nextCardBalance)
                .walletLedgerId(walletLedger.getId())
                .cardLedgerId(cardLedger.getId())
                .completedAt(exchangeTransaction.getCompletedAt())
                .build();
    }

    private WalletExchangeEstimateResponseDto estimateBuy(
            String currencyCode,
            BigDecimal krwAmount
    ) {
        BigDecimal baseRate = getExchangeRate(currencyCode);
        BigDecimal appliedRate = baseRate;
        BigDecimal foreignAmount = krwAmount.divide(appliedRate, 2, RoundingMode.DOWN);

        return WalletExchangeEstimateResponseDto.builder()
                .exchangeType(ExchangeType.BUY.name())
                .currencyCode(currencyCode)
                .krwAmount(krwAmount.setScale(0, RoundingMode.HALF_UP))
                .foreignAmount(foreignAmount)
                .baseExchangeRate(baseRate)
                .appliedExchangeRate(appliedRate)
                .feeRate(ZERO_FEE_RATE)
                .feeAmount(ZERO_FEE_AMOUNT)
                .build();
    }

    private WalletExchangeEstimateResponseDto estimateSell(
            String currencyCode,
            BigDecimal foreignAmount
    ) {
        BigDecimal baseRate = getExchangeRate(currencyCode);
        BigDecimal appliedRate = baseRate;
        BigDecimal krwAmount = foreignAmount.multiply(appliedRate).setScale(0, RoundingMode.DOWN);

        return WalletExchangeEstimateResponseDto.builder()
                .exchangeType(ExchangeType.SELL.name())
                .currencyCode(currencyCode)
                .krwAmount(krwAmount)
                .foreignAmount(foreignAmount.setScale(2, RoundingMode.HALF_UP))
                .baseExchangeRate(baseRate)
                .appliedExchangeRate(appliedRate)
                .feeRate(ZERO_FEE_RATE)
                .feeAmount(ZERO_FEE_AMOUNT)
                .build();
    }

    private Wallet getWalletForUpdate(Long userId) {
        Wallet wallet = walletMapper.findWalletByUserIdForUpdate(userId);

        if (wallet == null) {
            throw new WalletException(WALLET_NOT_FOUND);
        }

        return wallet;
    }

    private WalletTravelCard getWalletTravelCardForUpdate(
            Long walletId,
            Long walletTravelCardId
    ) {
        WalletTravelCard walletTravelCard = walletFxMapper.findWalletTravelCardForUpdate(
                walletId,
                walletTravelCardId
        );

        if (walletTravelCard == null) {
            throw new WalletException(WALLET_TRAVEL_CARD_NOT_FOUND);
        }

        return walletTravelCard;
    }

    private TravelCardBalance getTravelCardBalanceForUpdate(
            Long walletTravelCardId,
            String currencyCode
    ) {
        TravelCardBalance balance = walletFxMapper.findTravelCardBalanceForUpdate(
                walletTravelCardId,
                currencyCode
        );

        if (balance == null) {
            throw new WalletException(INSUFFICIENT_TRAVEL_CARD_BALANCE);
        }

        return balance;
    }

    private BigDecimal getExchangeRate(String currencyCode) {
        BigDecimal rate = walletFxMapper.findLatestDealBaseRate(currencyCode);

        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException(EXCHANGE_RATE_NOT_FOUND);
        }

        return rate;
    }

    private void updateWalletBalance(
            Wallet wallet,
            BigDecimal nextBalance
    ) {
        int count = walletMapper.updateWalletBalance(
                wallet.getId(),
                nextBalance,
                wallet.getVersion()
        );

        if (count == 0) {
            throw new WalletException(WALLET_CONFLICT);
        }
    }

    private WalletLedger insertWalletLedger(
            Long walletId,
            WalletDirection direction,
            WalletTransactionType transactionType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            WalletSourceType sourceType,
            Long sourceId,
            WalletTargetType targetType,
            Long targetId,
            String idempotencyKey,
            String memo
    ) {
        WalletLedger ledger = WalletLedger.builder()
                .walletId(walletId)
                .direction(direction.name())
                .transactionType(transactionType.name())
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .sourceType(sourceType.name())
                .sourceId(sourceId)
                .targetType(targetType.name())
                .targetId(targetId)
                .idempotencyKey(idempotencyKey)
                .memo(memo)
                .build();

        walletMapper.insertWalletLedger(ledger);

        return ledger;
    }

    private TravelCardLedger insertTravelCardLedger(
            Long walletTravelCardId,
            String currencyCode,
            String direction,
            String transactionType,
            BigDecimal foreignAmount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String sourceType,
            Long sourceId,
            String targetType,
            Long targetId,
            String idempotencyKey,
            String memo
    ) {
        TravelCardLedger ledger = TravelCardLedger.builder()
                .walletTravelCardId(walletTravelCardId)
                .currencyCode(currencyCode)
                .direction(direction)
                .transactionType(transactionType)
                .foreignAmount(foreignAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .targetType(targetType)
                .targetId(targetId)
                .idempotencyKey(idempotencyKey)
                .memo(memo)
                .build();

        walletFxMapper.insertTravelCardLedger(ledger);

        return ledger;
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException(INVALID_AMOUNT);
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (walletMapper.existsIdempotencyKey(idempotencyKey)) {
            throw new WalletException(DUPLICATED_REQUEST);
        }
    }

    private void validateTravelCardLedgerIdempotencyKey(String idempotencyKey) {
        if (walletFxMapper.existsTravelCardLedgerIdempotencyKey(idempotencyKey)) {
            throw new WalletException(DUPLICATED_REQUEST);
        }
    }

    private String normalizeCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new WalletException(INVALID_CURRENCY_CODE);
        }

        return currencyCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeExchangeType(String exchangeType) {
        if (exchangeType == null || exchangeType.trim().isEmpty()) {
            throw new WalletException(INVALID_AMOUNT);
        }

        String normalized = exchangeType.trim().toUpperCase(Locale.ROOT);

        if (!ExchangeType.BUY.name().equals(normalized) && !ExchangeType.SELL.name().equals(normalized)) {
            throw new WalletException(INVALID_AMOUNT);
        }

        return normalized;
    }
}
