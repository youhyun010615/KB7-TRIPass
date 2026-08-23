package com.tripass.saving.service;

import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.duplicate.DuplicateTransactionMatcher;
import com.tripass.common.exception.CustomException;
import com.tripass.saving.dto.WeeklyMissionEvaluationItemDto;
import com.tripass.saving.dto.WeeklyMissionEvaluationResponseDto;
import com.tripass.saving.dto.WeeklySavingMissionDto;
import com.tripass.saving.mapper.MonthlySpendingAnalysisMapper;
import com.tripass.saving.mapper.SavingMissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class WeeklyMissionEvaluationService {

    private static final int FIRST_WEEK = 1;
    private static final int LAST_WEEK = 4;

    private final SavingMissionMapper missionMapper;
    private final MonthlySpendingAnalysisMapper analysisMapper;
    private final DuplicateTransactionMatcher duplicateMatcher;
    private final MissionWalletRewardService walletRewardService;
    private final Clock clock;
    private final com.tripass.dev.util.DevDateUtil devDateUtil;

    @Autowired
    public WeeklyMissionEvaluationService(
            SavingMissionMapper missionMapper,
            MonthlySpendingAnalysisMapper analysisMapper,
            DuplicateTransactionMatcher duplicateMatcher,
            MissionWalletRewardService walletRewardService,
            com.tripass.dev.util.DevDateUtil devDateUtil
    ) {
        this(missionMapper, analysisMapper, duplicateMatcher, walletRewardService,
                Clock.system(ZoneId.of("Asia/Seoul")), devDateUtil);
    }

    WeeklyMissionEvaluationService(
            SavingMissionMapper missionMapper,
            MonthlySpendingAnalysisMapper analysisMapper,
            DuplicateTransactionMatcher duplicateMatcher,
            MissionWalletRewardService walletRewardService,
            Clock clock,
            com.tripass.dev.util.DevDateUtil devDateUtil
    ) {
        this.missionMapper = missionMapper;
        this.analysisMapper = analysisMapper;
        this.duplicateMatcher = duplicateMatcher;
        this.walletRewardService = walletRewardService;
        this.clock = clock;
        this.devDateUtil = devDateUtil;
    }

    @Transactional
    public WeeklyMissionEvaluationResponseDto evaluate(Long userId, YearMonth targetYearMonth, int weekNumber) {
        validateWeekNumber(weekNumber);
        List<WeeklySavingMissionDto> missions = missionMapper.findWeeklyMissionsForEvaluation(
                userId, targetYearMonth.toString(), weekNumber);
        if (missions.isEmpty()) {
            throw new CustomException(HttpStatus.NOT_FOUND, "WEEKLY_MISSION_NOT_FOUND",
                    "판정할 주간 미션을 찾을 수 없습니다.");
        }

        LocalDate periodEnd = missions.get(0).getPeriodEndDate();
        if (!devDateUtil.today(userId).isAfter(periodEnd)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "WEEKLY_MISSION_NOT_ENDED",
                    "주간 미션 기간이 종료된 후 판정할 수 있습니다.");
        }

        if (missions.stream().anyMatch(mission -> "PENDING".equals(mission.getStatus()))) {
            Map<Long, Integer> spendingByCategory = aggregateSpendingByCategory(
                    userId, missions.get(0).getPeriodStartDate(), periodEnd);
            Set<Long> monthlyMissionIds = new HashSet<>();

            for (WeeklySavingMissionDto mission : missions) {
                monthlyMissionIds.add(mission.getMonthlySavingMissionId());
                if (!"PENDING".equals(mission.getStatus())) {
                    continue;
                }
                int actualSpending = spendingByCategory.getOrDefault(mission.getCategoryId(), 0);
                boolean success = actualSpending <= mission.getWeeklyUsageLimit();
                int weeklyBaselineSpending = mission.getWeeklyUsageLimit()
                        + mission.getWeeklyExpectedSaving();
                int actualSaving = success ? Math.max(weeklyBaselineSpending - actualSpending, 0) : 0;
                String status = success ? "SUCCESS" : "FAILED";

                int updated = missionMapper.updateWeeklyMissionEvaluation(
                        mission.getId(), actualSpending, actualSaving, status);
                if (updated == 1) {
                    mission.setActualSpending(actualSpending);
                    mission.setActualSaving(actualSaving);
                    mission.setStatus(status);
                }
            }
            monthlyMissionIds.forEach(missionMapper::completeMonthlyMissionIfAllWeeksEvaluated);
        }

        walletRewardService.rewardSuccessfulMissions(userId, missions);

        return buildResponse(targetYearMonth, weekNumber, missions);
    }

    private Map<Long, Integer> aggregateSpendingByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        List<TransactionDto> accounts = analysisMapper.findAccountWithdrawalTransactions(userId, startDate, endDate);
        List<TransactionDto> checks = analysisMapper.findCheckCardWithdrawalTransactions(userId, startDate, endDate);
        List<TransactionDto> credits = analysisMapper.findCreditCardWithdrawalTransactions(userId, startDate, endDate);

        Set<Long> duplicateAccountIds = duplicateMatcher.findDuplicateAccountTransactionIds(accounts, checks);
        List<TransactionDto> transactions = new ArrayList<>();
        accounts.stream().filter(transaction -> !duplicateAccountIds.contains(transaction.getId()))
                .forEach(transactions::add);
        transactions.addAll(checks);
        transactions.addAll(credits);

        Map<Long, BigDecimal> totals = new HashMap<>();
        for (TransactionDto transaction : transactions) {
            totals.merge(transaction.getCategoryId(), transaction.getAmount(), BigDecimal::add);
        }

        Map<Long, Integer> result = new HashMap<>();
        totals.forEach((categoryId, amount) -> result.put(categoryId, toWon(amount)));
        return result;
    }

    private int toWon(BigDecimal amount) {
        try {
            return amount.setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (ArithmeticException e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "WEEKLY_SPENDING_OUT_OF_RANGE",
                    "주간 지출 금액을 처리할 수 없습니다.");
        }
    }

    private WeeklyMissionEvaluationResponseDto buildResponse(
            YearMonth targetYearMonth, int weekNumber, List<WeeklySavingMissionDto> missions
    ) {
        List<WeeklyMissionEvaluationItemDto> items = missions.stream()
                .map(mission -> new WeeklyMissionEvaluationItemDto(
                        mission.getId(), mission.getCategoryId(), mission.getCategoryCode(), mission.getCategoryName(),
                        mission.getWeeklyUsageLimit(), mission.getActualSpending(), mission.getActualSaving(),
                        mission.getRewardAmount(), mission.getRewardedAt(), mission.getStatus()))
                .toList();
        long successCount = items.stream().filter(item -> "SUCCESS".equals(item.status())).count();
        long failedCount = items.stream().filter(item -> "FAILED".equals(item.status())).count();
        return new WeeklyMissionEvaluationResponseDto(
                targetYearMonth.toString(), weekNumber,
                missions.get(0).getPeriodStartDate(), missions.get(0).getPeriodEndDate(),
                items.size(), successCount, failedCount, items);
    }

    private void validateWeekNumber(int weekNumber) {
        if (weekNumber < FIRST_WEEK || weekNumber > LAST_WEEK) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_WEEK_NUMBER",
                    "weekNumber는 1~4 사이여야 합니다.");
        }
    }
}
