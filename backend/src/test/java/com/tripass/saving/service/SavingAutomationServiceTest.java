package com.tripass.saving.service;

import com.tripass.saving.dto.SavingAutomationResult;
import com.tripass.saving.mapper.SavingAutomationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingAutomationServiceTest {

    @Mock
    private SavingAutomationMapper automationMapper;

    @Mock
    private MonthlySpendingAnalysisService monthlyAnalysisService;

    @Mock
    private WeeklyMissionEvaluationService weeklyEvaluationService;

    @Test
    void 매월_1일에는_직전_달_거래가_있는_사용자의_분석을_생성한다() {
        SavingAutomationService service = createService();
        LocalDate runDate = LocalDate.of(2026, 1, 1);

        when(automationMapper.findMonthlyAnalysisTargetUserIds(
                LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31)))
                .thenReturn(List.of(3L, 7L));

        SavingAutomationResult result = service.generatePreviousMonthAnalyses(runDate);

        verify(monthlyAnalysisService).generateMonthlyAnalysis(3L, YearMonth.of(2025, 12));
        verify(monthlyAnalysisService).generateMonthlyAnalysis(7L, YearMonth.of(2025, 12));
        assertEquals("2025-12", result.target());
        assertEquals(2, result.targetCount());
        assertEquals(2, result.successCount());
        assertEquals(0, result.failureCount());
    }

    @Test
    void 한_사용자의_월간_분석이_실패해도_나머지_사용자를_계속_처리한다() {
        SavingAutomationService service = createService();
        YearMonth analysisMonth = YearMonth.of(2026, 7);

        when(automationMapper.findMonthlyAnalysisTargetUserIds(
                analysisMonth.atDay(1), analysisMonth.atEndOfMonth()))
                .thenReturn(List.of(1L, 2L, 3L));
        doAnswer(invocation -> {
            if (Long.valueOf(2L).equals(invocation.getArgument(0))) {
                throw new RuntimeException("사용자 데이터 오류");
            }
            return null;
        }).when(monthlyAnalysisService).generateMonthlyAnalysis(anyLong(), eq(analysisMonth));

        SavingAutomationResult result = service.generatePreviousMonthAnalyses(LocalDate.of(2026, 8, 1));

        InOrder inOrder = inOrder(monthlyAnalysisService);
        inOrder.verify(monthlyAnalysisService).generateMonthlyAnalysis(1L, analysisMonth);
        inOrder.verify(monthlyAnalysisService).generateMonthlyAnalysis(2L, analysisMonth);
        inOrder.verify(monthlyAnalysisService).generateMonthlyAnalysis(3L, analysisMonth);
        assertEquals(2, result.successCount());
        assertEquals(1, result.failureCount());
    }

    @Test
    void 주간_판정일에_직전_주차의_미션과_월렛_보상을_처리한다() {
        SavingAutomationService service = createService();
        LocalDate runDate = LocalDate.of(2026, 8, 15);

        when(automationMapper.findWeeklyEvaluationTargetUserIds("2026-08", 2, runDate))
                .thenReturn(List.of(5L, 8L));

        SavingAutomationResult result = service.evaluatePreviousWeekMissions(runDate);

        verify(weeklyEvaluationService).evaluate(5L, YearMonth.of(2026, 8), 2);
        verify(weeklyEvaluationService).evaluate(8L, YearMonth.of(2026, 8), 2);
        assertEquals("2026-08-W2", result.target());
        assertEquals(2, result.successCount());
        assertEquals(0, result.failureCount());
    }

    @Test
    void 한_사용자의_주간_판정이_실패해도_다음_사용자를_계속_처리한다() {
        SavingAutomationService service = createService();
        LocalDate runDate = LocalDate.of(2026, 8, 29);
        YearMonth targetMonth = YearMonth.of(2026, 8);

        when(automationMapper.findWeeklyEvaluationTargetUserIds("2026-08", 4, runDate))
                .thenReturn(List.of(10L, 11L));
        doAnswer(invocation -> {
            if (Long.valueOf(10L).equals(invocation.getArgument(0))) {
                throw new RuntimeException("판정 오류");
            }
            return null;
        }).when(weeklyEvaluationService).evaluate(anyLong(), eq(targetMonth), eq(4));

        SavingAutomationResult result = service.evaluatePreviousWeekMissions(runDate);

        verify(weeklyEvaluationService).evaluate(11L, targetMonth, 4);
        assertEquals(1, result.successCount());
        assertEquals(1, result.failureCount());
    }

    @Test
    void 설정된_판정일이_아니면_주간_작업을_실행하지_않는다() {
        SavingAutomationService service = createService();

        assertThrows(IllegalArgumentException.class,
                () -> service.evaluatePreviousWeekMissions(LocalDate.of(2026, 8, 14)));
    }

    @Test
    void 판정일을_고정_주차로_변환한다() {
        assertEquals(1, SavingAutomationService.resolveEvaluationWeek(8));
        assertEquals(2, SavingAutomationService.resolveEvaluationWeek(15));
        assertEquals(3, SavingAutomationService.resolveEvaluationWeek(22));
        assertEquals(4, SavingAutomationService.resolveEvaluationWeek(29));
    }

    private SavingAutomationService createService() {
        return new SavingAutomationService(automationMapper, monthlyAnalysisService, weeklyEvaluationService);
    }
}
