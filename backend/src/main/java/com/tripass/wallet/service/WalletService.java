package com.tripass.wallet.service;

import com.tripass.wallet.domain.*;
import com.tripass.wallet.dto.request.*;
import com.tripass.wallet.dto.response.*;
import com.tripass.wallet.enums.*;
import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.fx.domain.WalletExchangeTransaction;
import com.tripass.wallet.mapper.WalletMapper;
import com.tripass.wallet.travelcard.domain.TravelCardBalance;
import com.tripass.wallet.travelcard.domain.TravelCardLedger;
import com.tripass.wallet.travelcard.domain.WalletCardTopup;
import com.tripass.wallet.travelcard.domain.WalletTravelCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.tripass.wallet.exception.WalletErrorCode.*;

/** 월렛 잔액 변경, 원장 기록, 계좌 연동 검증 등 월렛 핵심 비즈니스 로직을 처리하는 서비스입니다. */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private static final BigDecimal DEFAULT_TARGET_AMOUNT = BigDecimal.valueOf(5_000_000);
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final WalletMapper walletMapper;

    public WalletMainResponseDto getWalletMain(Long userId) {
        WalletMainResponseDto response = walletMapper.findWalletMainByUserId(userId);

        if (response == null) {
            throw new WalletException(WALLET_NOT_FOUND);
        }

        BigDecimal balanceAmount = defaultZero(response.getBalanceAmount());
        BigDecimal totalLinkedAccountBalance = defaultZero(response.getTotalLinkedAccountBalance());
        BigDecimal targetAmount = defaultTargetAmount(response.getTargetAmount());
        BigDecimal emergencyAmount = balanceAmount.subtract(targetAmount);

        response.setBalanceAmount(balanceAmount);
        response.setTotalLinkedAccountBalance(totalLinkedAccountBalance);
        response.setTargetAmount(targetAmount);
        response.setEmergencyAmount(emergencyAmount.compareTo(BigDecimal.ZERO) > 0 ? emergencyAmount : BigDecimal.ZERO);
        response.setSavingRate(calculateSavingRate(balanceAmount, targetAmount));
        response.setMonthlySavings(walletMapper.findMonthlySavings(userId, response.getWalletId()));

        WalletLinkedTravelCardResponseDto travelCard = walletMapper.findLinkedTravelCardForMain(response.getWalletId());
        if (travelCard == null) {
            response.setTravelCard(WalletLinkedTravelCardResponseDto.builder()
                    .linked(false)
                    .build());
            response.setForeignBalances(Collections.emptyList());
        } else {
            travelCard.setLinked(true);
            response.setTravelCard(travelCard);
            response.setForeignBalances(walletMapper.findForeignBalancesForMain(travelCard.getWalletTravelCardId()));
        }

        return response;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal defaultTargetAmount(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0
                ? DEFAULT_TARGET_AMOUNT
                : value;
    }

    private Integer calculateSavingRate(
            BigDecimal totalLinkedAccountBalance,
            BigDecimal targetAmount
    ) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return totalLinkedAccountBalance
                .multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 0, RoundingMode.DOWN)
                .intValue();
    }

    private Long getWalletId(Long userId) {
        WalletMainResponseDto response = walletMapper.findWalletMainByUserId(userId);

        if (response == null) {
            throw new WalletException(WALLET_NOT_FOUND);
        }

        return response.getWalletId();
    }

    public List<WalletLedgerResponseDto> getWalletLedgers(Long userId) {
        Long walletId = getWalletId(userId);

        return walletMapper.findLedgersByWalletId(walletId);
    }

    public WalletMonthlySavingDetailResponseDto getMonthlySavingDetail(
            Long userId,
            String month
    ) {
        validateMonth(month);

        Long walletId = getWalletId(userId);
        WalletMonthlySavingDetailResponseDto detail =
                walletMapper.findMonthlySavingDetailSummary(walletId, month);

        if (detail == null) {
            detail = WalletMonthlySavingDetailResponseDto.builder()
                    .month(month)
                    .savedAmount(BigDecimal.ZERO)
                    .chargeAmount(BigDecimal.ZERO)
                    .withdrawAmount(BigDecimal.ZERO)
                    .build();
        }

        detail.setLedgers(walletMapper.findLedgersByWalletIdAndMonth(walletId, month));

        return detail;
    }

    public List<WalletForeignBalanceResponseDto> getAllForeignBalances(Long userId) {
        Wallet wallet = getWallet(userId);
        WalletLinkedTravelCardResponseDto travelCard = walletMapper.findLinkedTravelCardForMain(wallet.getId());

        if (travelCard == null) {
            return Collections.emptyList();
        }

        return walletMapper.findAllForeignBalances(travelCard.getWalletTravelCardId());
    }

    public List<WalletAccountResponseDto> getAccounts(Long userId) {
        return walletMapper.findAccountsByUserId(userId);
    }

    public List<WalletAccountResponseDto> getAccountOptions(Long userId) {
        getWalletId(userId);

        return walletMapper.findAccountOptionsByUserId(userId);
    }

    @Transactional
    public WalletCommandResponseDto charge(
            Long userId,
            WalletChargeRequestDto request
    ) {
        validatePositiveAmount(request.getAmount());
        validateIdempotencyKey(request.getIdempotencyKey());

        Wallet wallet = getWalletForUpdate(userId);

        validateLinkedAccount(wallet.getId(), request.getSourceAccountId());
        decreaseAccountBalance(userId, request.getSourceAccountId(), request.getAmount());

        BigDecimal nextBalance =
                wallet.getBalanceAmount().add(request.getAmount());

        updateWalletBalance(wallet, nextBalance);

        insertLedger(
                wallet.getId(),
                WalletDirection.IN,
                WalletTransactionType.CHARGE,
                TransferMethod.MANUAL,
                request.getAmount(),
                wallet.getBalanceAmount(),
                nextBalance,
                WalletSourceType.ACCOUNT,
                request.getSourceAccountId(),
                WalletTargetType.WALLET,
                wallet.getId(),
                request.getIdempotencyKey(),
                "월렛 수동 충전"
        );

        return WalletCommandResponseDto.builder()
                .walletId(wallet.getId())
                .balanceAmount(nextBalance)
                .build();
    }

    @Transactional
    public WalletCommandResponseDto withdraw(
            Long userId,
            WalletWithdrawRequestDto request
    ) {
        validatePositiveAmount(request.getAmount());
        validateIdempotencyKey(request.getIdempotencyKey());

        Wallet wallet = getWalletForUpdate(userId);

        validateLinkedAccount(wallet.getId(), request.getTargetAccountId());
        validateSufficientBalance(wallet.getBalanceAmount(), request.getAmount());
        increaseAccountBalance(userId, request.getTargetAccountId(), request.getAmount());

        BigDecimal nextBalance =
                wallet.getBalanceAmount().subtract(request.getAmount());

        updateWalletBalance(wallet, nextBalance);

        insertLedger(
                wallet.getId(),
                WalletDirection.OUT,
                WalletTransactionType.WITHDRAW,
                null,
                request.getAmount(),
                wallet.getBalanceAmount(),
                nextBalance,
                WalletSourceType.WALLET,
                wallet.getId(),
                WalletTargetType.ACCOUNT,
                request.getTargetAccountId(),
                request.getIdempotencyKey(),
                "월렛 출금"
        );

        return WalletCommandResponseDto.builder()
                .walletId(wallet.getId())
                .balanceAmount(nextBalance)
                .build();
    }

    @Transactional
    public void linkAccount(
            Long userId,
            WalletAccountLinkRequestDto request
    ) {
        Wallet wallet = getWalletForUpdate(userId);

        validateUserAccount(userId, request.getAccountId());

        WalletAccount walletAccount = WalletAccount.builder()
                .walletId(wallet.getId())
                .accountId(request.getAccountId())
                .isPrimary(Boolean.TRUE.equals(request.getIsPrimary()))
                .status(WalletAccountStatus.LINKED.name())
                .build();

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            walletMapper.resetPrimaryAccount(wallet.getId());
        }

        walletMapper.insertWalletAccount(walletAccount);
    }

    @Transactional
    public void unlinkAccount(
            Long userId,
            Long accountId
    ) {
        Wallet wallet = getWalletForUpdate(userId);

        int count = walletMapper.unlinkWalletAccount(wallet.getId(), accountId);

        if (count == 0) {
            throw new WalletException(WALLET_ACCOUNT_NOT_FOUND);
        }
    }

    @Transactional
    public void updatePrimaryAccount(
            Long userId,
            Long accountId
    ) {
        Wallet wallet = getWalletForUpdate(userId);

        validateLinkedAccount(wallet.getId(), accountId);

        walletMapper.resetPrimaryAccount(wallet.getId());
        walletMapper.updatePrimaryAccount(wallet.getId(), accountId);
    }

    public WalletAutoSavingResponseDto getAutoSavingRule(Long userId) {
        Wallet wallet = getWallet(userId);

        return walletMapper.findAutoSavingRuleResponseByWalletId(wallet.getId());
    }

    @Transactional
    public void updateAutoSavingRule(
            Long userId,
            WalletAutoSavingRequestDto request
    ) {
        validatePositiveAmount(request.getAmount());

        Wallet wallet = getWalletForUpdate(userId);
        WalletAccountResponseDto primaryAccount = getPrimaryAccount(wallet.getId());

        WalletAutoSavingRule rule = WalletAutoSavingRule.builder()
                .walletId(wallet.getId())
                .sourceAccountId(primaryAccount.getAccountId())
                .amount(request.getAmount())
                .dayOfMonth(request.getDayOfMonth())
                .enabled(request.getEnabled())
                .nextTransferDate(calculateNextTransferDate(request.getDayOfMonth()))
                .build();

        walletMapper.upsertAutoSavingRule(rule);
    }

    @Transactional
    public void deleteAutoSavingRule(Long userId) {
        Wallet wallet = getWalletForUpdate(userId);

        walletMapper.deleteAutoSavingRule(wallet.getId());
    }

    @Transactional
    public int executeDueAutoSavingRules(LocalDate baseDate) {
        List<WalletAutoSavingRule> rules = walletMapper.findDueAutoSavingRules(baseDate);
        int successCount = 0;

        for (WalletAutoSavingRule rule : rules) {
            executeAutoSavingRule(rule, baseDate);
            successCount++;
        }

        return successCount;
    }

    private void executeAutoSavingRule(
            WalletAutoSavingRule rule,
            LocalDate baseDate
    ) {
        Wallet wallet = walletMapper.findWalletByUserIdForUpdate(findWalletOwnerId(rule.getWalletId()));

        if (wallet == null) {
            return;
        }

        // 규칙에 저장된 계좌가 아니라, 실행 시점의 최신 주계좌를 매번 다시 조회한다.
        // 그래야 주계좌를 나중에 바꿔도 다음 자동 채우기부터 바로 반영된다.
        WalletAccountResponseDto primaryAccount = walletMapper.findPrimaryAccountByWalletId(wallet.getId());

        if (primaryAccount == null) {
            logAutoSavingFailure(
                    wallet.getId(),
                    rule.getAmount(),
                    "주계좌가 설정되어 있지 않아 자동 채우기가 진행되지 않았어요. 주계좌를 설정해 주세요."
            );
            walletMapper.updateAutoSavingNextTransferDate(
                    rule.getId(),
                    calculateNextTransferDateFrom(baseDate, rule.getDayOfMonth())
            );
            return;
        }

        BigDecimal availableAmount = primaryAccount.getWithdrawableAmount() != null
                ? primaryAccount.getWithdrawableAmount()
                : BigDecimal.ZERO;

        if (availableAmount.compareTo(rule.getAmount()) < 0) {
            logAutoSavingFailure(
                    wallet.getId(),
                    rule.getAmount(),
                    "주계좌(" + primaryAccount.getAccountName() + ") 잔액이 부족해 자동 채우기가 진행되지 않았어요."
            );
            walletMapper.updateAutoSavingNextTransferDate(
                    rule.getId(),
                    calculateNextTransferDateFrom(baseDate, rule.getDayOfMonth())
            );
            return;
        }

        decreaseAccountBalance(wallet.getUserId(), primaryAccount.getAccountId(), rule.getAmount());

        BigDecimal nextBalance = wallet.getBalanceAmount().add(rule.getAmount());
        updateWalletBalance(wallet, nextBalance);

        insertLedger(
                wallet.getId(),
                WalletDirection.IN,
                WalletTransactionType.CHARGE,
                TransferMethod.AUTO_SAVING,
                rule.getAmount(),
                wallet.getBalanceAmount(),
                nextBalance,
                WalletSourceType.ACCOUNT,
                primaryAccount.getAccountId(),
                WalletTargetType.WALLET,
                wallet.getId(),
                buildAutoSavingIdempotencyKey(rule.getId(), baseDate),
                "월 목표 자동 송금"
        );

        walletMapper.updateAutoSavingNextTransferDate(
                rule.getId(),
                calculateNextTransferDateFrom(baseDate, rule.getDayOfMonth())
        );

        logAutoSavingSuccess(wallet.getId(), rule.getAmount());
    }

    private void logAutoSavingSuccess(
            Long walletId,
            BigDecimal amount
    ) {
        walletMapper.insertAutoSavingLog(
                WalletAutoSavingLog.builder()
                        .walletId(walletId)
                        .status("SUCCESS")
                        .amount(amount)
                        .build()
        );
    }

    private void logAutoSavingFailure(
            Long walletId,
            BigDecimal amount,
            String reason
    ) {
        walletMapper.insertAutoSavingLog(
                WalletAutoSavingLog.builder()
                        .walletId(walletId)
                        .status("FAILED")
                        .amount(amount)
                        .reason(reason)
                        .build()
        );
    }

    public List<WalletAutoSavingLogResponseDto> getAutoSavingLogs(Long userId) {
        Wallet wallet = walletMapper.findWalletByUserId(userId);

        if (wallet == null) {
            return Collections.emptyList();
        }

        return walletMapper.findAutoSavingLogsByWalletId(wallet.getId());
    }

    private WalletAccountResponseDto getPrimaryAccount(Long walletId) {
        WalletAccountResponseDto primaryAccount = walletMapper.findPrimaryAccountByWalletId(walletId);

        if (primaryAccount == null) {
            throw new WalletException(PRIMARY_ACCOUNT_NOT_FOUND);
        }

        return primaryAccount;
    }

    private Long findWalletOwnerId(Long walletId) {
        Wallet wallet = walletMapper.findWalletById(walletId);

        if (wallet == null) {
            return null;
        }

        return wallet.getUserId();
    }

    public WalletTravelCardResponseDto getTravelCard(Long userId) {
        Wallet wallet = getWallet(userId);

        return walletMapper.findLinkedTravelCardByWalletId(wallet.getId());
    }

    @Transactional
    public void linkTravelCard(
            Long userId,
            WalletTravelCardLinkRequestDto request
    ) {
        Wallet wallet = getWalletForUpdate(userId);

        if (!walletMapper.existsTravelCardById(request.getTravelCardId())) {
            throw new WalletException(TRAVEL_CARD_NOT_FOUND);
        }

        walletMapper.unlinkWalletTravelCard(wallet.getId());

        WalletTravelCard walletTravelCard = WalletTravelCard.builder()
                .walletId(wallet.getId())
                .travelCardId(request.getTravelCardId())
                .maskedCardNumber(request.getMaskedCardNumber())
                .status("LINKED")
                .build();

        walletMapper.insertWalletTravelCard(walletTravelCard);
    }

    @Transactional
    public void unlinkTravelCard(Long userId) {
        Wallet wallet = getWalletForUpdate(userId);

        int count = walletMapper.unlinkWalletTravelCard(wallet.getId());

        if (count == 0) {
            throw new WalletException(WALLET_TRAVEL_CARD_NOT_FOUND);
        }
    }

    @Transactional
    public WalletExchangeEstimateResponseDto topupTravelCard(
            Long userId,
            WalletTravelCardTopupRequestDto request
    ) {
        validatePositiveAmount(request.getKrwAmount());
        validateIdempotencyKey(request.getIdempotencyKey());
        validateTravelCardLedgerIdempotencyKey(request.getIdempotencyKey());

        Wallet wallet = getWalletForUpdate(userId);
        WalletTravelCard walletTravelCard = getWalletTravelCardForUpdate(
                wallet.getId(),
                request.getWalletTravelCardId()
        );
        String currencyCode = normalizeCurrencyCode(request.getCurrencyCode());

        validateSupportedCardCurrency(walletTravelCard.getTravelCardId(), currencyCode);
        validateSufficientBalance(wallet.getBalanceAmount(), request.getKrwAmount());

        WalletExchangeEstimateResponseDto estimate =
                estimateBuy(currencyCode, request.getKrwAmount());

        BigDecimal nextWalletBalance =
                wallet.getBalanceAmount().subtract(request.getKrwAmount());
        updateWalletBalance(wallet, nextWalletBalance);

        WalletLedger walletLedger = insertLedger(
                wallet.getId(),
                WalletDirection.OUT,
                WalletTransactionType.CARD_TOPUP,
                null,
                request.getKrwAmount(),
                wallet.getBalanceAmount(),
                nextWalletBalance,
                WalletSourceType.WALLET,
                wallet.getId(),
                WalletTargetType.TRAVEL_CARD,
                walletTravelCard.getId(),
                request.getIdempotencyKey(),
                "트래블카드 외화 충전"
        );

        TravelCardBalance balance = getOrCreateTravelCardBalance(walletTravelCard.getId(), currencyCode);
        BigDecimal nextCardBalance =
                balance.getBalanceAmount().add(estimate.getForeignAmount());
        BigDecimal krwEstimatedAmount =
                nextCardBalance.multiply(estimate.getBaseExchangeRate()).setScale(0, RoundingMode.HALF_UP);

        walletMapper.updateTravelCardBalance(
                balance.getId(),
                nextCardBalance,
                krwEstimatedAmount
        );

        TravelCardLedger cardLedger = insertTravelCardLedger(
                walletTravelCard.getId(),
                currencyCode,
                WalletDirection.IN.name(),
                WalletTransactionType.CARD_TOPUP.name(),
                estimate.getForeignAmount(),
                balance.getBalanceAmount(),
                nextCardBalance,
                WalletSourceType.WALLET.name(),
                wallet.getId(),
                WalletTargetType.TRAVEL_CARD.name(),
                walletTravelCard.getId(),
                request.getIdempotencyKey(),
                "월렛 원화 환전 충전"
        );

        walletMapper.insertWalletCardTopup(
                WalletCardTopup.builder()
                        .walletId(wallet.getId())
                        .walletTravelCardId(walletTravelCard.getId())
                        .currencyCode(currencyCode)
                        .krwAmount(request.getKrwAmount())
                        .foreignAmount(estimate.getForeignAmount())
                        .status("COMPLETED")
                        .idempotencyKey(request.getIdempotencyKey())
                        .externalTransactionId("LEGACY-" + request.getIdempotencyKey())
                        .retryCount(1)
                        .lastTriedAt(LocalDateTime.now())
                        .completedAt(LocalDateTime.now())
                        .walletLedgerId(walletLedger.getId())
                        .cardLedgerId(cardLedger.getId())
                        .refunded(false)
                        .build()
        );

        insertExchangeTransaction(
                wallet.getId(),
                walletTravelCard.getId(),
                currencyCode,
                "BUY",
                estimate.getKrwAmount(),
                estimate.getForeignAmount(),
                estimate,
                walletLedger.getId(),
                cardLedger.getId()
        );

        return estimate;
    }

    public List<TravelCardCurrencyBalanceResponseDto> getTravelCardCurrencies(Long userId) {
        Wallet wallet = getWallet(userId);
        WalletTravelCard walletTravelCard = walletMapper.findActiveWalletTravelCardByWalletId(wallet.getId());

        if (walletTravelCard == null) {
            return Collections.emptyList();
        }

        return walletMapper.findTravelCardBalances(walletTravelCard.getId());
    }

    public WalletExchangeEstimateResponseDto estimateExchange(
            String type,
            String currencyCode,
            BigDecimal amount
    ) {
        validatePositiveAmount(amount);

        String normalizedType = normalizeExchangeType(type);
        String normalizedCurrencyCode = normalizeCurrencyCode(currencyCode);

        if ("BUY".equals(normalizedType)) {
            return estimateBuy(normalizedCurrencyCode, amount);
        }

        return estimateSell(normalizedCurrencyCode, amount);
    }

    @Transactional
    public WalletExchangeEstimateResponseDto sellExchange(
            Long userId,
            WalletExchangeSellRequestDto request
    ) {
        validatePositiveAmount(request.getForeignAmount());
        validateIdempotencyKey(request.getIdempotencyKey());
        validateTravelCardLedgerIdempotencyKey(request.getIdempotencyKey());

        Wallet wallet = getWalletForUpdate(userId);
        WalletTravelCard walletTravelCard = getWalletTravelCardForUpdate(
                wallet.getId(),
                request.getWalletTravelCardId()
        );
        String currencyCode = normalizeCurrencyCode(request.getCurrencyCode());

        TravelCardBalance balance =
                walletMapper.findTravelCardBalanceForUpdate(walletTravelCard.getId(), currencyCode);

        if (balance == null || balance.getBalanceAmount().compareTo(request.getForeignAmount()) < 0) {
            throw new WalletException(INSUFFICIENT_TRAVEL_CARD_BALANCE);
        }

        WalletExchangeEstimateResponseDto estimate =
                estimateSell(currencyCode, request.getForeignAmount());

        BigDecimal nextCardBalance =
                balance.getBalanceAmount().subtract(request.getForeignAmount());
        BigDecimal krwEstimatedAmount =
                nextCardBalance.multiply(estimate.getBaseExchangeRate()).setScale(0, RoundingMode.HALF_UP);

        walletMapper.updateTravelCardBalance(
                balance.getId(),
                nextCardBalance,
                krwEstimatedAmount
        );

        TravelCardLedger cardLedger = insertTravelCardLedger(
                walletTravelCard.getId(),
                currencyCode,
                WalletDirection.OUT.name(),
                "CARD_WITHDRAW",
                request.getForeignAmount(),
                balance.getBalanceAmount(),
                nextCardBalance,
                WalletSourceType.TRAVEL_CARD.name(),
                walletTravelCard.getId(),
                WalletTargetType.WALLET.name(),
                wallet.getId(),
                request.getIdempotencyKey(),
                "외화 재환전"
        );

        BigDecimal nextWalletBalance =
                wallet.getBalanceAmount().add(estimate.getKrwAmount());
        updateWalletBalance(wallet, nextWalletBalance);

        WalletLedger walletLedger = insertLedger(
                wallet.getId(),
                WalletDirection.IN,
                WalletTransactionType.EXCHANGE_SELL,
                null,
                estimate.getKrwAmount(),
                wallet.getBalanceAmount(),
                nextWalletBalance,
                WalletSourceType.TRAVEL_CARD,
                walletTravelCard.getId(),
                WalletTargetType.WALLET,
                wallet.getId(),
                request.getIdempotencyKey(),
                "트래블카드 외화 재환전"
        );

        insertExchangeTransaction(
                wallet.getId(),
                walletTravelCard.getId(),
                currencyCode,
                "SELL",
                estimate.getKrwAmount(),
                estimate.getForeignAmount(),
                estimate,
                walletLedger.getId(),
                cardLedger.getId()
        );

        return estimate;
    }

    @Transactional
    public Wallet createWalletForUser(Long userId) {
        if (walletMapper.existsWalletByUserId(userId)) {
            return walletMapper.findWalletByUserIdForUpdate(userId);
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balanceAmount(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE.name())
                .version(0L)
                .build();

        walletMapper.insertWallet(wallet);

        return wallet;
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

    private void validateLinkedAccount(
            Long walletId,
            Long accountId
    ) {
        if (!walletMapper.existsLinkedAccount(walletId, accountId)) {
            throw new WalletException(WALLET_ACCOUNT_NOT_FOUND);
        }
    }

    private void validateUserAccount(
            Long userId,
            Long accountId
    ) {
        if (!walletMapper.existsLinkedAccountByUserId(userId, accountId)) {
            throw new WalletException(WALLET_ACCOUNT_NOT_FOUND);
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (walletMapper.existsIdempotencyKey(idempotencyKey)) {
            throw new WalletException(DUPLICATED_REQUEST);
        }
    }

    private void validateTravelCardLedgerIdempotencyKey(String idempotencyKey) {
        if (walletMapper.existsTravelCardLedgerIdempotencyKey(idempotencyKey)) {
            throw new WalletException(DUPLICATED_REQUEST);
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException(INVALID_AMOUNT);
        }
    }

    private void validateMonth(String month) {
        if (month == null || !MONTH_PATTERN.matcher(month).matches()) {
            throw new WalletException(INVALID_MONTH);
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

    private void decreaseAccountBalance(
            Long userId,
            Long accountId,
            BigDecimal amount
    ) {
        int count = walletMapper.decreaseAccountBalance(userId, accountId, amount);

        if (count == 0) {
            throw new WalletException(INSUFFICIENT_ACCOUNT_BALANCE);
        }
    }

    private void increaseAccountBalance(
            Long userId,
            Long accountId,
            BigDecimal amount
    ) {
        int count = walletMapper.increaseAccountBalance(userId, accountId, amount);

        if (count == 0) {
            throw new WalletException(WALLET_ACCOUNT_NOT_FOUND);
        }
    }

    private WalletLedger insertLedger(
            Long walletId,
            WalletDirection direction,
            WalletTransactionType transactionType,
            TransferMethod transferMethod,
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
                .transferMethod(transferMethod == null ? null : transferMethod.name())
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

        walletMapper.insertTravelCardLedger(ledger);

        return ledger;
    }

    private WalletTravelCard getWalletTravelCardForUpdate(
            Long walletId,
            Long walletTravelCardId
    ) {
        WalletTravelCard walletTravelCard =
                walletMapper.findWalletTravelCardByIdForUpdate(walletId, walletTravelCardId);

        if (walletTravelCard == null) {
            throw new WalletException(WALLET_TRAVEL_CARD_NOT_FOUND);
        }

        return walletTravelCard;
    }

    private void validateSupportedCardCurrency(
            Long travelCardId,
            String currencyCode
    ) {
        if (!walletMapper.existsSupportedCardCurrency(travelCardId, currencyCode)) {
            throw new WalletException(TRAVEL_CARD_CURRENCY_NOT_SUPPORTED);
        }
    }

    private TravelCardBalance getOrCreateTravelCardBalance(
            Long walletTravelCardId,
            String currencyCode
    ) {
        TravelCardBalance balance =
                walletMapper.findTravelCardBalanceForUpdate(walletTravelCardId, currencyCode);

        if (balance != null) {
            return balance;
        }

        TravelCardBalance newBalance = TravelCardBalance.builder()
                .walletTravelCardId(walletTravelCardId)
                .currencyCode(currencyCode)
                .balanceAmount(BigDecimal.ZERO)
                .krwEstimatedAmount(BigDecimal.ZERO)
                .build();

        walletMapper.insertTravelCardBalance(newBalance);

        return walletMapper.findTravelCardBalanceForUpdate(walletTravelCardId, currencyCode);
    }

    private WalletExchangeEstimateResponseDto estimateBuy(
            String currencyCode,
            BigDecimal krwAmount
    ) {
        BigDecimal rate = getExchangeRate(currencyCode);
        BigDecimal feeRate = BigDecimal.ZERO;
        BigDecimal feeAmount = BigDecimal.ZERO;
        BigDecimal appliedRate = rate;
        BigDecimal foreignAmount = krwAmount.divide(appliedRate, 2, RoundingMode.DOWN);

        return WalletExchangeEstimateResponseDto.builder()
                .exchangeType("BUY")
                .currencyCode(currencyCode)
                .krwAmount(krwAmount)
                .foreignAmount(foreignAmount)
                .baseExchangeRate(rate)
                .appliedExchangeRate(appliedRate)
                .feeRate(feeRate)
                .feeAmount(feeAmount)
                .build();
    }

    private WalletExchangeEstimateResponseDto estimateSell(
            String currencyCode,
            BigDecimal foreignAmount
    ) {
        BigDecimal rate = getExchangeRate(currencyCode);
        BigDecimal feeRate = BigDecimal.ZERO;
        BigDecimal feeAmount = BigDecimal.ZERO;
        BigDecimal appliedRate = rate;
        BigDecimal krwAmount = foreignAmount.multiply(appliedRate).setScale(0, RoundingMode.DOWN);

        return WalletExchangeEstimateResponseDto.builder()
                .exchangeType("SELL")
                .currencyCode(currencyCode)
                .krwAmount(krwAmount)
                .foreignAmount(foreignAmount)
                .baseExchangeRate(rate)
                .appliedExchangeRate(appliedRate)
                .feeRate(feeRate)
                .feeAmount(feeAmount)
                .build();
    }

    private BigDecimal getExchangeRate(String currencyCode) {
        BigDecimal rate = walletMapper.findLatestDealBaseRate(currencyCode);

        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException(EXCHANGE_RATE_NOT_FOUND);
        }

        return rate;
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

    private String normalizeExchangeType(String type) {
        if (type == null) {
            throw new WalletException(INVALID_AMOUNT);
        }

        String normalizedType = type.trim().toUpperCase(Locale.ROOT);

        if (!"BUY".equals(normalizedType) && !"SELL".equals(normalizedType)) {
            throw new WalletException(INVALID_AMOUNT);
        }

        return normalizedType;
    }

    private void insertExchangeTransaction(
            Long walletId,
            Long walletTravelCardId,
            String currencyCode,
            String exchangeType,
            BigDecimal krwAmount,
            BigDecimal foreignAmount,
            WalletExchangeEstimateResponseDto estimate,
            Long walletLedgerId,
            Long cardLedgerId
    ) {
        walletMapper.insertWalletExchangeTransaction(
                WalletExchangeTransaction.builder()
                        .walletId(walletId)
                        .walletTravelCardId(walletTravelCardId)
                        .currencyCode(currencyCode)
                        .exchangeType(exchangeType)
                        .krwAmount(krwAmount)
                        .foreignAmount(foreignAmount)
                        .baseExchangeRate(estimate.getBaseExchangeRate())
                        .appliedExchangeRate(estimate.getAppliedExchangeRate())
                        .feeRate(estimate.getFeeRate())
                        .feeAmount(estimate.getFeeAmount())
                        .status("COMPLETED")
                        .completedAt(LocalDateTime.now())
                        .walletLedgerId(walletLedgerId)
                        .cardLedgerId(cardLedgerId)
                        .build()
        );
    }

    private LocalDate calculateNextTransferDate(Integer dayOfMonth) {
        LocalDate today = LocalDate.now();
        LocalDate transferDate = today.withDayOfMonth(dayOfMonth);

        if (!transferDate.isAfter(today)) {
            transferDate = transferDate.plusMonths(1);
        }

        return transferDate;
    }

    private LocalDate calculateNextTransferDateFrom(
            LocalDate baseDate,
            Integer dayOfMonth
    ) {
        LocalDate nextDate = baseDate.withDayOfMonth(dayOfMonth);

        if (!nextDate.isAfter(baseDate)) {
            nextDate = nextDate.plusMonths(1);
        }

        return nextDate;
    }

    private String buildAutoSavingIdempotencyKey(
            Long ruleId,
            LocalDate baseDate
    ) {
        return "AUTO_SAVING:" + ruleId + ":" + baseDate;
    }
}
