package com.tripass.saving.scheduler;

import com.tripass.saving.dto.SavingAutomationResult;
import com.tripass.saving.service.SavingAutomationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingAutomationSchedulerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private SavingAutomationService automationService;

    @Test
    void 고정된_한국_시간을_기준으로_월간_분석을_실행한다() {
        LocalDate runDate = LocalDate.of(2026, 8, 1);
        SavingAutomationScheduler scheduler = new SavingAutomationScheduler(
                automationService, fixedClock("2026-07-31T17:00:00Z"));
        when(automationService.generatePreviousMonthAnalyses(runDate))
                .thenReturn(new SavingAutomationResult("MONTHLY_ANALYSIS", "2026-07", 2, 2, 0));

        scheduler.generateMonthlyAnalyses();

        verify(automationService).generatePreviousMonthAnalyses(runDate);
    }

    @Test
    void 고정된_한국_시간을_기준으로_주간_미션을_판정한다() {
        LocalDate runDate = LocalDate.of(2026, 8, 15);
        SavingAutomationScheduler scheduler = new SavingAutomationScheduler(
                automationService, fixedClock("2026-08-14T17:00:00Z"));
        when(automationService.evaluatePreviousWeekMissions(runDate))
                .thenReturn(new SavingAutomationResult("WEEKLY_MISSION_EVALUATION", "2026-08-W2", 1, 1, 0));

        scheduler.evaluateWeeklyMissions();

        verify(automationService).evaluatePreviousWeekMissions(runDate);
    }

    @Test
    void 배치_전체_오류가_발생해도_스케줄러_밖으로_전파하지_않는다() {
        LocalDate runDate = LocalDate.of(2026, 8, 1);
        SavingAutomationScheduler scheduler = new SavingAutomationScheduler(
                automationService, fixedClock("2026-07-31T17:00:00Z"));
        when(automationService.generatePreviousMonthAnalyses(runDate))
                .thenThrow(new RuntimeException("DB 연결 오류"));

        assertDoesNotThrow(scheduler::generateMonthlyAnalyses);
    }

    @Test
    void 월간_분석과_주간_판정의_실행_시간을_환경설정으로_관리한다() throws Exception {
        Method monthly = SavingAutomationScheduler.class.getDeclaredMethod("generateMonthlyAnalyses");
        Method weekly = SavingAutomationScheduler.class.getDeclaredMethod("evaluateWeeklyMissions");

        Scheduled monthlySchedule = monthly.getAnnotation(Scheduled.class);
        Scheduled weeklySchedule = weekly.getAnnotation(Scheduled.class);

        assertNotNull(monthlySchedule);
        assertNotNull(weeklySchedule);
        assertEquals("${saving.scheduler.monthly-analysis-cron}", monthlySchedule.cron());
        assertEquals("${saving.scheduler.weekly-evaluation-cron}", weeklySchedule.cron());
        assertEquals("${saving.scheduler.zone}", monthlySchedule.zone());
        assertEquals("${saving.scheduler.zone}", weeklySchedule.zone());
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), SEOUL);
    }
}
