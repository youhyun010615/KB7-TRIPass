package com.tripass.wallet.travelcard.service;

import com.tripass.wallet.domain.Wallet;
import com.tripass.wallet.domain.WalletLedger;
import com.tripass.wallet.enums.WalletDirection;
import com.tripass.wallet.enums.WalletSourceType;
import com.tripass.wallet.enums.WalletTargetType;
import com.tripass.wallet.enums.WalletTransactionType;
import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.fx.domain.WalletExchangeTransaction;
import com.tripass.wallet.fx.enums.ExchangeStatus;
import com.tripass.wallet.fx.enums.ExchangeType;
import com.tripass.wallet.mapper.WalletMapper;
import com.tripass.wallet.travelcard.client.MockTravelCardClient;
import com.tripass.wallet.travelcard.client.TravelCardTopupResult;
import com.tripass.wallet.travelcard.domain.TravelCardBalance;
import com.tripass.wallet.travelcard.domain.TravelCardLedger;
import com.tripass.wallet.travelcard.domain.WalletCardTopup;
import com.tripass.wallet.travelcard.domain.WalletTravelCard;
import com.tripass.wallet.travelcard.dto.request.WalletTravelCardLinkRequestDto;
import com.tripass.wallet.travelcard.dto.request.WalletTravelCardTopupRequestDto;
import com.tripass.wallet.travelcard.dto.response.TravelCardCurrencyBalanceResponseDto;
import com.tripass.wallet.travelcard.dto.response.TravelCardLedgerResponseDto;
import com.tripass.wallet.travelcard.dto.response.TravelCardTransactionResponseDto;
import com.tripass.wallet.travelcard.dto.response.UserTravelCardOptionResponseDto;
import com.tripass.wallet.travelcard.dto.response.WalletCardTopupResponseDto;
import com.tripass.wallet.travelcard.dto.response.WalletTravelCardResponseDto;
import com.tripass.wallet.travelcard.enums.CardTopupStatus;
import com.tripass.wallet.travelcard.enums.TravelCardLedgerType;
import com.tripass.wallet.travelcard.enums.TravelCardLinkStatus;
import com.tripass.wallet.travelcard.mapper.WalletTravelCardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.tripass.wallet.exception.WalletErrorCode.*;

/** 트래블카드 연동, 목 카드사 충전 처리, 외화 잔액 조회 비즈니스 로직을 처리하는 서비스입니다. */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletTravelCardService {

    private static final BigDecimal ZERO_FEE_RATE = BigDecimal.ZERO.setScale(4);
    private static final BigDecimal ZERO_FEE_AMOUNT = BigDecimal.ZERO.setScale(0);

    private final WalletMapper walletMapper;
    private final WalletTravelCardMapper walletTravelCardMapper;
    private final MockTravelCardClient mockTravelCardClient;

    public WalletTravelCardResponseDto getTravelCard(Long userId) {
        Wallet wallet = getWallet(userId);

        return walletTravelCardMapper.findLinkedTravelCardByWalletId(wallet.getId());
    }

    public List<UserTravelCardOptionResponseDto> getUserTravelCardOptions(Long userId) {
        return walletTravelCardMapper.findUserTravelCardOptions(userId);
    }

    @Transactional
    public void linkTravelCard(
            Long userId,
            WalletTravelCardLinkRequestDto request
    ) {
        Wallet wallet = getWalletForUpdate(userId);

        if (!walletTravelCardMapper.existsTravelCardById(request.getTravelCardId())) {
            throw new WalletException(TRAVEL_CARD_NOT_FOUND);
        }
        if (request.getUserTravelCardId() == null
                || !walletTravelCardMapper.existsUserTravelCard(userId, request.getUserTravelCardId())) {
            throw new WalletException(TRAVEL_CARD_NOT_FOUND);
        }

        walletTravelCardMapper.unlinkWalletTravelCard(wallet.getId());

        WalletTravelCard walletTravelCard = WalletTravelCard.builder()
                .walletId(wallet.getId())
                .userTravelCardId(request.getUserTravelCardId())
                .travelCardId(request.getTravelCardId())
                .maskedCardNumber(request.getMaskedCardNumber())
                .status(TravelCardLinkStatus.LINKED.name())
                .build();

        walletTravelCardMapper.insertWalletTravelCard(walletTravelCard);
    }

    @Transactional
    public void unlinkTravelCard(Long userId) {
        Wallet wallet = getWalletForUpdate(userId);
        int count = walletTravelCardMapper.unlinkWalletTravelCard(wallet.getId());

        if (count == 0) {
            throw new WalletException(WALLET_TRAVEL_CARD_NOT_FOUND);
        }
    }

    @Transactional
    public WalletCardTopupResponseDto topup(
            Long userId,
            WalletTravelCardTopupRequestDto request
    ) {
        WalletCardTopup topup = createTopupRequest(userId, request);

        return processTopup(topup.getId(), request.getIdempotencyKey());
    }

    @Transactional
    public WalletCardTopupResponseDto processTopup(Long topupId) {
        WalletCardTopup topup = walletTravelCardMapper.findWalletCardTopupForUpdate(topupId);

        if (topup == null) {
            throw new WalletException(WALLET_TRAVEL_CARD_NOT_FOUND);
        }

        return processTopup(topup.getId(), topup.getIdempotencyKey());
    }

    public List<TravelCardCurrencyBalanceResponseDto> getBalances(Long userId) {
        Wallet wallet = getWallet(userId);
        WalletTravelCard walletTravelCard = walletTravelCardMapper.findActiveWalletTravelCardByWalletId(wallet.getId());

        if (walletTravelCard == null) {
            return Collections.emptyList();
        }

        return walletTravelCardMapper.findTravelCardBalances(walletTravelCard.getId());
    }

    public List<TravelCardTransactionResponseDto> getTransactions(Long userId) {
        Wallet wallet = getWallet(userId);
        WalletTravelCard walletTravelCard = walletTravelCardMapper.findActiveWalletTravelCardByWalletId(wallet.getId());

        if (walletTravelCard == null) {
            return Collections.emptyList();
        }

        return walletTravelCardMapper.findTravelCardTransactions(wallet.getId(), walletTravelCard.getId());
    }

    public List<TravelCardLedgerResponseDto> getLedgers(Long userId) {
        Wallet wallet = getWallet(userId);
        WalletTravelCard walletTravelCard = walletTravelCardMapper.findActiveWalletTravelCardByWalletId(wallet.getId());

        if (walletTravelCard == null) {
            return Collections.emptyList();
        }

        return walletTravelCardMapper.findTravelCardLedgers(walletTravelCard.getId());
    }

    private WalletCardTopup createTopupRequest(
            Long userId,
            WalletTravelCardTopupRequestDto request
    ) {
        validatePositiveAmount(request.getKrwAmount());
        validateIdempotencyKey(request.getIdempotencyKey());

        Wallet wallet = getWalletForUpdate(userId);
        WalletTravelCard walletTravelCard = getWalletTravelCardForUpdate(
                wallet.getId(),
                request.getWalletTravelCardId()
        );
        String currencyCode = normalizeCurrencyCode(request.getCurrencyCode());

        validateSupportedCardCurrency(walletTravelCard.getTravelCardId(), currencyCode);
        validateSufficientBalance(wallet.getBalanceAmount(), request.getKrwAmount());

        BigDecimal baseRate = getExchangeRate(currencyCode);
        BigDecimal foreignAmount = request.getKrwAmount().divide(baseRate, 2, RoundingMode.DOWN);

        WalletCardTopup topup = WalletCardTopup.builder()
                .walletId(wallet.getId())
                .walletTravelCardId(walletTravelCard.getId())
                .currencyCode(currencyCode)
                .krwAmount(request.getKrwAmount())
                .foreignAmount(foreignAmount)
                .status(CardTopupStatus.REQUESTED.name())
                .idempotencyKey(request.getIdempotencyKey())
                .retryCount(0)
                .refunded(false)
                .build();

        walletTravelCardMapper.insertWalletCardTopup(topup);

        return topup;
    }

    private WalletCardTopupResponseDto processTopup(
            Long topupId,
            String idempotencyKey
    ) {
        WalletCardTopup topup = walletTravelCardMapper.findWalletCardTopupForUpdate(topupId);

        if (topup == null) {
            throw new WalletException(WALLET_TRAVEL_CARD_NOT_FOUND);
        }

        if (CardTopupStatus.COMPLETED.name().equals(topup.getStatus())) {
            return buildResponse(topup, null, null, null, null, null);
        }

        walletTravelCardMapper.updateWalletCardTopupProcessing(topup.getId());
        topup = walletTravelCardMapper.findWalletCardTopupForUpdate(topup.getId());

        TravelCardTopupResult result = mockTravelCardClient.requestTopup(topup, idempotencyKey);

        if (!result.isSuccess()) {
            walletTravelCardMapper.updateWalletCardTopupFailed(
                    topup.getId(),
                    buildFailureReason(result, topup.getRetryCount())
            );

            WalletCardTopup failedTopup = walletTravelCardMapper.findWalletCardTopupForUpdate(topup.getId());
            return buildResponse(failedTopup, null, null, null, null, null);
        }

        return completeTopup(topup, result.getExternalTransactionId());
    }

    private WalletCardTopupResponseDto completeTopup(
            WalletCardTopup topup,
            String externalTransactionId
    ) {
        Wallet wallet = walletMapper.findWalletById(topup.getWalletId());

        if (wallet == null) {
            throw new WalletException(WALLET_NOT_FOUND);
        }

        wallet = walletMapper.findWalletByUserIdForUpdate(wallet.getUserId());
        validateSufficientBalance(wallet.getBalanceAmount(), topup.getKrwAmount());

        BigDecimal baseRate = getExchangeRate(topup.getCurrencyCode());
        BigDecimal nextWalletBalance = wallet.getBalanceAmount().subtract(topup.getKrwAmount());
        updateWalletBalance(wallet, nextWalletBalance);

        WalletLedger walletLedger = insertWalletLedger(
                wallet.getId(),
                WalletDirection.OUT,
                WalletTransactionType.CARD_TOPUP,
                topup.getKrwAmount(),
                wallet.getBalanceAmount(),
                nextWalletBalance,
                WalletSourceType.WALLET,
                wallet.getId(),
                WalletTargetType.TRAVEL_CARD,
                topup.getWalletTravelCardId(),
                topup.getIdempotencyKey(),
                "트래블카드 외화 충전"
        );

        TravelCardBalance balance = getOrCreateTravelCardBalance(topup.getWalletTravelCardId(), topup.getCurrencyCode());
        BigDecimal nextCardBalance = balance.getBalanceAmount().add(topup.getForeignAmount());
        BigDecimal krwEstimatedAmount = nextCardBalance.multiply(baseRate).setScale(0, RoundingMode.HALF_UP);

        walletTravelCardMapper.updateTravelCardBalance(
                balance.getId(),
                nextCardBalance,
                krwEstimatedAmount
        );

        TravelCardLedger cardLedger = insertTravelCardLedger(
                topup.getWalletTravelCardId(),
                topup.getCurrencyCode(),
                WalletDirection.IN.name(),
                TravelCardLedgerType.CARD_TOPUP.name(),
                topup.getForeignAmount(),
                balance.getBalanceAmount(),
                nextCardBalance,
                WalletSourceType.WALLET.name(),
                wallet.getId(),
                WalletTargetType.TRAVEL_CARD.name(),
                topup.getWalletTravelCardId(),
                topup.getIdempotencyKey(),
                "목 카드사 외화 충전 성공"
        );

        walletTravelCardMapper.updateWalletCardTopupCompleted(
                topup.getId(),
                walletLedger.getId(),
                cardLedger.getId(),
                externalTransactionId
        );

        insertExchangeTransaction(topup, baseRate, walletLedger.getId(), cardLedger.getId());

        WalletCardTopup completedTopup = walletTravelCardMapper.findWalletCardTopupForUpdate(topup.getId());

        return buildResponse(completedTopup, baseRate, baseRate, nextWalletBalance, nextCardBalance, externalTransactionId);
    }

    private Wallet getWalletForUpdate(Long userId) {
        Wallet wallet = walletMapper.findWalletByUserIdForUpdate(userId);

        if (wallet == null) {
            throw new WalletException(WALLET_NOT_FOUND);
        }

        return wallet;
    }

    private Wallet getWallet(Long userId) {
        Wallet wallet = walletMapper.findWalletByUserId(userId);

        if (wallet == null) {
            throw new WalletException(WALLET_NOT_FOUND);
        }

        return wallet;
    }

    private WalletTravelCard getWalletTravelCardForUpdate(
            Long walletId,
            Long walletTravelCardId
    ) {
        WalletTravelCard walletTravelCard = walletTravelCardMapper.findWalletTravelCardForUpdate(
                walletId,
                walletTravelCardId
        );

        if (walletTravelCard == null) {
            throw new WalletException(WALLET_TRAVEL_CARD_NOT_FOUND);
        }

        return walletTravelCard;
    }

    private TravelCardBalance getOrCreateTravelCardBalance(
            Long walletTravelCardId,
            String currencyCode
    ) {
        TravelCardBalance balance = walletTravelCardMapper.findTravelCardBalanceForUpdate(
                walletTravelCardId,
                currencyCode
        );

        if (balance != null) {
            return balance;
        }

        TravelCardBalance newBalance = TravelCardBalance.builder()
                .walletTravelCardId(walletTravelCardId)
                .currencyCode(currencyCode)
                .balanceAmount(BigDecimal.ZERO)
                .krwEstimatedAmount(BigDecimal.ZERO)
                .build();

        walletTravelCardMapper.insertTravelCardBalance(newBalance);

        return walletTravelCardMapper.findTravelCardBalanceForUpdate(walletTravelCardId, currencyCode);
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

        walletTravelCardMapper.insertTravelCardLedger(ledger);

        return ledger;
    }

    private void insertExchangeTransaction(
            WalletCardTopup topup,
            BigDecimal baseRate,
            Long walletLedgerId,
            Long cardLedgerId
    ) {
        WalletExchangeTransaction exchangeTransaction = WalletExchangeTransaction.builder()
                .walletId(topup.getWalletId())
                .walletTravelCardId(topup.getWalletTravelCardId())
                .currencyCode(topup.getCurrencyCode())
                .exchangeType(ExchangeType.BUY.name())
                .krwAmount(topup.getKrwAmount())
                .foreignAmount(topup.getForeignAmount())
                .baseExchangeRate(baseRate)
                .appliedExchangeRate(baseRate)
                .feeRate(ZERO_FEE_RATE)
                .feeAmount(ZERO_FEE_AMOUNT)
                .status(ExchangeStatus.COMPLETED.name())
                .completedAt(LocalDateTime.now())
                .walletLedgerId(walletLedgerId)
                .cardLedgerId(cardLedgerId)
                .build();

        walletTravelCardMapper.insertWalletExchangeTransaction(exchangeTransaction);
    }

    private WalletCardTopupResponseDto buildResponse(
            WalletCardTopup topup,
            BigDecimal baseExchangeRate,
            BigDecimal appliedExchangeRate,
            BigDecimal walletBalanceAfter,
            BigDecimal travelCardBalanceAfter,
            String externalTransactionId
    ) {
        return WalletCardTopupResponseDto.builder()
                .topupId(topup.getId())
                .walletId(topup.getWalletId())
                .walletTravelCardId(topup.getWalletTravelCardId())
                .currencyCode(topup.getCurrencyCode())
                .krwAmount(topup.getKrwAmount())
                .foreignAmount(topup.getForeignAmount())
                .baseExchangeRate(baseExchangeRate)
                .appliedExchangeRate(appliedExchangeRate)
                .feeRate(ZERO_FEE_RATE)
                .feeAmount(ZERO_FEE_AMOUNT)
                .status(topup.getStatus())
                .externalTransactionId(externalTransactionId == null ? topup.getExternalTransactionId() : externalTransactionId)
                .failureReason(topup.getFailureReason())
                .retryCount(topup.getRetryCount())
                .walletBalanceAfter(walletBalanceAfter)
                .travelCardBalanceAfter(travelCardBalanceAfter)
                .walletLedgerId(topup.getWalletLedgerId())
                .cardLedgerId(topup.getCardLedgerId())
                .completedAt(topup.getCompletedAt())
                .build();
    }

    private BigDecimal getExchangeRate(String currencyCode) {
        BigDecimal rate = walletTravelCardMapper.findLatestDealBaseRate(currencyCode);

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

    private void validateSupportedCardCurrency(
            Long travelCardId,
            String currencyCode
    ) {
        if (!walletTravelCardMapper.existsSupportedCardCurrency(travelCardId, currencyCode)) {
            throw new WalletException(TRAVEL_CARD_CURRENCY_NOT_SUPPORTED);
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException(INVALID_AMOUNT);
        }
    }

    private void validateSufficientBalance(
            BigDecimal balance,
            BigDecimal amount
    ) {
        if (balance.compareTo(amount) < 0) {
            throw new WalletException(INSUFFICIENT_WALLET_BALANCE);
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (walletMapper.existsIdempotencyKey(idempotencyKey)
                || walletTravelCardMapper.existsTravelCardLedgerIdempotencyKey(idempotencyKey)
                || walletTravelCardMapper.existsWalletCardTopupIdempotencyKey(idempotencyKey)) {
            throw new WalletException(DUPLICATED_REQUEST);
        }
    }

    private String normalizeCurrencyCode(String currencyCode) {
        if (currencyCode == null) {
            throw new WalletException(INVALID_CURRENCY_CODE);
        }

        String normalizedCurrencyCode = currencyCode.trim().toUpperCase(Locale.ROOT);

        if (!normalizedCurrencyCode.matches("^[A-Z]{3}$")) {
            throw new WalletException(INVALID_CURRENCY_CODE);
        }

        return normalizedCurrencyCode;
    }

    private String buildFailureReason(
            TravelCardTopupResult result,
            Integer retryCount
    ) {
        return result.getFailureCode() + "|retry=" + retryCount + "|" + result.getFailureMessage();
    }
}
