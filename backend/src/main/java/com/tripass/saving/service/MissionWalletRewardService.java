package com.tripass.saving.service;

import com.tripass.saving.dto.WeeklySavingMissionDto;
import com.tripass.saving.mapper.SavingMissionMapper;
import com.tripass.wallet.domain.Wallet;
import com.tripass.wallet.domain.WalletLedger;
import com.tripass.wallet.enums.WalletDirection;
import com.tripass.wallet.enums.WalletSourceType;
import com.tripass.wallet.enums.WalletTargetType;
import com.tripass.wallet.enums.WalletTransactionType;
import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.tripass.wallet.exception.WalletErrorCode.WALLET_CONFLICT;
import static com.tripass.wallet.exception.WalletErrorCode.WALLET_NOT_FOUND;

/** 성공한 주간 절약 미션의 보상액을 TRIP 월렛에 한 번만 적립합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionWalletRewardService {

    private static final String IDEMPOTENCY_KEY_PREFIX = "MISSION_REWARD:WEEKLY:";

    private final WalletMapper walletMapper;
    private final SavingMissionMapper missionMapper;

    @Transactional
    public void rewardSuccessfulMissions(Long userId, List<WeeklySavingMissionDto> missions) {
        List<RewardCandidate> candidates = new ArrayList<>();

        for (WeeklySavingMissionDto mission : missions) {
            if (!isRewardCandidate(mission)) {
                continue;
            }

            String idempotencyKey = idempotencyKey(mission.getId());
            WalletLedger existingLedger = walletMapper.findWalletLedgerByIdempotencyKey(idempotencyKey);
            if (existingLedger != null) {
                markRewarded(mission, toWon(existingLedger.getAmount()), existingLedger.getId());
                continue;
            }

            int rewardAmount = calculateRewardAmount(mission);
            if (rewardAmount > 0) {
                candidates.add(new RewardCandidate(mission, rewardAmount, idempotencyKey));
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        Wallet wallet = walletMapper.findWalletByUserIdForUpdate(userId);
        if (wallet == null) {
            throw new WalletException(WALLET_NOT_FOUND);
        }

        BigDecimal balanceBefore = defaultZero(wallet.getBalanceAmount());
        BigDecimal totalReward = candidates.stream()
                .map(candidate -> BigDecimal.valueOf(candidate.rewardAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal finalBalance = balanceBefore.add(totalReward);

        int updated = walletMapper.updateWalletBalance(wallet.getId(), finalBalance, wallet.getVersion());
        if (updated == 0) {
            throw new WalletException(WALLET_CONFLICT);
        }

        BigDecimal runningBalance = balanceBefore;
        for (RewardCandidate candidate : candidates) {
            BigDecimal reward = BigDecimal.valueOf(candidate.rewardAmount());
            BigDecimal balanceAfter = runningBalance.add(reward);
            WalletLedger ledger = createLedger(
                    wallet.getId(), candidate.mission(), reward,
                    runningBalance, balanceAfter, candidate.idempotencyKey());
            walletMapper.insertWalletLedger(ledger);
            markRewarded(candidate.mission(), candidate.rewardAmount(), ledger.getId());
            runningBalance = balanceAfter;
        }
    }

    private boolean isRewardCandidate(WeeklySavingMissionDto mission) {
        return "SUCCESS".equals(mission.getStatus()) && mission.getWalletLedgerId() == null;
    }

    private int calculateRewardAmount(WeeklySavingMissionDto mission) {
        return Math.max(defaultZero(mission.getActualSaving()), 0);
    }

    private WalletLedger createLedger(
            Long walletId,
            WeeklySavingMissionDto mission,
            BigDecimal rewardAmount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String idempotencyKey
    ) {
        return WalletLedger.builder()
                .walletId(walletId)
                .direction(WalletDirection.IN.name())
                .transactionType(WalletTransactionType.MISSION_REWARD.name())
                .amount(rewardAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .sourceType(WalletSourceType.MISSION.name())
                .sourceId(mission.getId())
                .targetType(WalletTargetType.WALLET.name())
                .targetId(walletId)
                .idempotencyKey(idempotencyKey)
                .memo(mission.getWeekNumber() + "주차 " + mission.getCategoryName() + " 절약 미션 성공 보상")
                .build();
    }

    private void markRewarded(WeeklySavingMissionDto mission, int rewardAmount, Long walletLedgerId) {
        int updated = missionMapper.updateWeeklyMissionReward(mission.getId(), rewardAmount, walletLedgerId);
        if (updated == 0) {
            throw new WalletException(WALLET_CONFLICT);
        }
        mission.setRewardAmount(rewardAmount);
        mission.setWalletLedgerId(walletLedgerId);
        mission.setRewardedAt(LocalDateTime.now());
    }

    private String idempotencyKey(Long weeklyMissionId) {
        return IDEMPOTENCY_KEY_PREFIX + weeklyMissionId;
    }

    private int toWon(BigDecimal amount) {
        try {
            return amount.intValueExact();
        } catch (ArithmeticException e) {
            throw new WalletException(WALLET_CONFLICT);
        }
    }

    private int defaultZero(Integer amount) {
        return amount == null ? 0 : amount;
    }

    private BigDecimal defaultZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private record RewardCandidate(
            WeeklySavingMissionDto mission,
            int rewardAmount,
            String idempotencyKey
    ) {
    }
}
