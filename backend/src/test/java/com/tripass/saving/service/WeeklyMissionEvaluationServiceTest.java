package com.tripass.saving.service;

import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.duplicate.DuplicateTransactionMatcher;
import com.tripass.common.exception.CustomException;
import com.tripass.saving.dto.WeeklyMissionEvaluationResponseDto;
import com.tripass.saving.dto.WeeklySavingMissionDto;
import com.tripass.saving.mapper.MonthlySpendingAnalysisMapper;
import com.tripass.saving.mapper.SavingMissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WeeklyMissionEvaluationServiceTest {

    private static final Long USER_ID = 3L;
    private static final YearMonth TARGET_MONTH = YearMonth.of(2026, 8);

    @Mock private SavingMissionMapper missionMapper;
    @Mock private MonthlySpendingAnalysisMapper analysisMapper;
    @Mock private MissionWalletRewardService walletRewardService;
    private WeeklyMissionEvaluationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Clock clock = Clock.fixed(Instant.parse("2026-08-08T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new WeeklyMissionEvaluationService(
                missionMapper, analysisMapper, new DuplicateTransactionMatcher(), walletRewardService, clock);
    }

    @Test
    void spendingEqualToLimitIsSuccess() {
        WeeklySavingMissionDto mission = mission(11L, 2L, 30_000);
        givenMissions(mission);
        when(analysisMapper.findCreditCardWithdrawalTransactions(USER_ID, date(1), date(7)))
                .thenReturn(List.of(transaction(101L, 2L, 30_000, LocalTime.of(12, 0))));
        when(missionMapper.updateWeeklyMissionEvaluation(11L, 30_000, 10_000, "SUCCESS")).thenReturn(1);

        WeeklyMissionEvaluationResponseDto result = service.evaluate(USER_ID, TARGET_MONTH, 1);

        assertEquals(1, result.successCount());
        assertEquals("SUCCESS", result.missions().get(0).status());
        assertEquals(30_000, result.missions().get(0).actualSpending());
        assertEquals(10_000, result.missions().get(0).actualSaving());
        verify(walletRewardService).rewardSuccessfulMissions(USER_ID, List.of(mission));
    }

    @Test
    void spendingAboveLimitIsFailed() {
        WeeklySavingMissionDto mission = mission(11L, 2L, 30_000);
        givenMissions(mission);
        when(analysisMapper.findAccountWithdrawalTransactions(USER_ID, date(1), date(7)))
                .thenReturn(List.of(transaction(101L, 2L, 30_001, LocalTime.of(12, 0))));
        when(missionMapper.updateWeeklyMissionEvaluation(11L, 30_001, 0, "FAILED")).thenReturn(1);

        WeeklyMissionEvaluationResponseDto result = service.evaluate(USER_ID, TARGET_MONTH, 1);

        assertEquals(1, result.failedCount());
        assertEquals("FAILED", result.missions().get(0).status());
    }

    @Test
    void noSpendingIsSuccessAndSavesWholeBaselineSpending() {
        WeeklySavingMissionDto mission = mission(11L, 2L, 30_000);
        givenMissions(mission);
        when(missionMapper.updateWeeklyMissionEvaluation(11L, 0, 40_000, "SUCCESS")).thenReturn(1);

        WeeklyMissionEvaluationResponseDto result = service.evaluate(USER_ID, TARGET_MONTH, 1);

        assertEquals(0, result.missions().get(0).actualSpending());
        assertEquals(40_000, result.missions().get(0).actualSaving());
    }

    @Test
    void checkCardAndAccountDuplicateIsCountedOnce() {
        WeeklySavingMissionDto mission = mission(11L, 2L, 15_000);
        givenMissions(mission);
        TransactionDto account = transaction(101L, 2L, 10_000, LocalTime.of(12, 0));
        TransactionDto check = transaction(201L, 2L, 10_000, LocalTime.of(12, 3));
        when(analysisMapper.findAccountWithdrawalTransactions(USER_ID, date(1), date(7)))
                .thenReturn(List.of(account));
        when(analysisMapper.findCheckCardWithdrawalTransactions(USER_ID, date(1), date(7)))
                .thenReturn(List.of(check));
        when(missionMapper.updateWeeklyMissionEvaluation(11L, 10_000, 15_000, "SUCCESS")).thenReturn(1);

        WeeklyMissionEvaluationResponseDto result = service.evaluate(USER_ID, TARGET_MONTH, 1);

        assertEquals(10_000, result.missions().get(0).actualSpending());
    }

    @Test
    void categoriesAreAggregatedSeparately() {
        WeeklySavingMissionDto cafe = mission(11L, 2L, 20_000);
        WeeklySavingMissionDto food = mission(12L, 3L, 20_000);
        givenMissions(cafe, food);
        when(analysisMapper.findCreditCardWithdrawalTransactions(USER_ID, date(1), date(7)))
                .thenReturn(List.of(
                        transaction(201L, 2L, 7_000, LocalTime.of(12, 0)),
                        transaction(202L, 3L, 15_000, LocalTime.of(13, 0))));
        when(missionMapper.updateWeeklyMissionEvaluation(anyLong(), anyInt(), anyInt(), eq("SUCCESS")))
                .thenReturn(1);

        WeeklyMissionEvaluationResponseDto result = service.evaluate(USER_ID, TARGET_MONTH, 1);

        assertEquals(7_000, result.missions().get(0).actualSpending());
        assertEquals(15_000, result.missions().get(1).actualSpending());
    }

    @Test
    void alreadyEvaluatedWeekReturnsStoredResultWithoutReadingTransactions() {
        WeeklySavingMissionDto mission = mission(11L, 2L, 30_000);
        mission.setStatus("SUCCESS");
        mission.setActualSpending(20_000);
        mission.setActualSaving(10_000);
        givenMissions(mission);

        WeeklyMissionEvaluationResponseDto result = service.evaluate(USER_ID, TARGET_MONTH, 1);

        assertEquals(20_000, result.missions().get(0).actualSpending());
        verifyNoInteractions(analysisMapper);
        verify(missionMapper, never()).updateWeeklyMissionEvaluation(anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void cannotEvaluateBeforePeriodEnds() {
        Clock earlyClock = Clock.fixed(Instant.parse("2026-08-07T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new WeeklyMissionEvaluationService(
                missionMapper, analysisMapper, new DuplicateTransactionMatcher(), walletRewardService, earlyClock);
        givenMissions(mission(11L, 2L, 30_000));

        CustomException error = assertThrows(CustomException.class,
                () -> service.evaluate(USER_ID, TARGET_MONTH, 1));

        assertEquals("WEEKLY_MISSION_NOT_ENDED", error.getErrorCode());
        verifyNoInteractions(analysisMapper);
    }

    @Test
    void invalidWeekIsRejectedBeforeMapperCall() {
        CustomException error = assertThrows(CustomException.class,
                () -> service.evaluate(USER_ID, TARGET_MONTH, 5));

        assertEquals("INVALID_WEEK_NUMBER", error.getErrorCode());
        verifyNoInteractions(missionMapper, analysisMapper);
    }

    @Test
    void missingOrOtherUsersMissionReturnsNotFound() {
        when(missionMapper.findWeeklyMissionsForEvaluation(USER_ID, "2026-08", 1)).thenReturn(List.of());

        CustomException error = assertThrows(CustomException.class,
                () -> service.evaluate(USER_ID, TARGET_MONTH, 1));

        assertEquals("WEEKLY_MISSION_NOT_FOUND", error.getErrorCode());
    }

    private void givenMissions(WeeklySavingMissionDto... missions) {
        when(missionMapper.findWeeklyMissionsForEvaluation(USER_ID, "2026-08", 1))
                .thenReturn(List.of(missions));
        when(analysisMapper.findAccountWithdrawalTransactions(USER_ID, date(1), date(7))).thenReturn(List.of());
        when(analysisMapper.findCheckCardWithdrawalTransactions(USER_ID, date(1), date(7))).thenReturn(List.of());
        when(analysisMapper.findCreditCardWithdrawalTransactions(USER_ID, date(1), date(7))).thenReturn(List.of());
    }

    private WeeklySavingMissionDto mission(Long id, Long categoryId, int limit) {
        WeeklySavingMissionDto mission = new WeeklySavingMissionDto();
        mission.setId(id);
        mission.setMonthlySavingMissionId(id + 100);
        mission.setCategoryId(categoryId);
        mission.setCategoryCode(categoryId == 2L ? "CAFE" : "FOOD");
        mission.setCategoryName(categoryId == 2L ? "카페" : "식비");
        mission.setWeekNumber(1);
        mission.setPeriodStartDate(date(1));
        mission.setPeriodEndDate(date(7));
        mission.setWeeklyUsageLimit(limit);
        mission.setWeeklyExpectedSaving(10_000);
        mission.setStatus("PENDING");
        return mission;
    }

    private TransactionDto transaction(Long id, Long categoryId, int amount, LocalTime time) {
        TransactionDto transaction = new TransactionDto();
        transaction.setId(id);
        transaction.setCategoryId(categoryId);
        transaction.setAmount(BigDecimal.valueOf(amount));
        transaction.setTransactionDate(date(3));
        transaction.setTransactionTime(time);
        return transaction;
    }

    private LocalDate date(int day) {
        return LocalDate.of(2026, 8, day);
    }
}
