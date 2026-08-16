package com.tripass.saving.scheduler;

import com.tripass.saving.dto.SavingAutomationResult;
import com.tripass.saving.service.SavingAutomationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** 월간 소비 분석 생성과 주간 절감 미션 판정을 정해진 날짜에 실행합니다. */
@Slf4j
@Component
public class SavingAutomationScheduler {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    private final SavingAutomationService automationService;
    private final Clock clock;

    @Autowired
    public SavingAutomationScheduler(SavingAutomationService automationService) {
        this(automationService, Clock.system(DEFAULT_ZONE));
    }

    SavingAutomationScheduler(SavingAutomationService automationService, Clock clock) {
        this.automationService = automationService;
        this.clock = clock;
    }

    /** 매월 1일에 직전 달의 월간 AI 소비 분석 리포트를 생성합니다. */
    @Scheduled(
            cron = "${saving.scheduler.monthly-analysis-cron}",
            zone = "${saving.scheduler.zone}"
    )
    public void generateMonthlyAnalyses() {
        LocalDate runDate = LocalDate.now(clock);
        log.info("월간 소비 분석 자동 생성 시작 - 실행일: {}", runDate);
        try {
            logResult(automationService.generatePreviousMonthAnalyses(runDate));
        } catch (Exception e) {
            log.error("월간 소비 분석 자동 생성 작업 실패 - 실행일: {}, 사유: {}",
                    runDate, e.getMessage(), e);
        }
    }

    /** 매월 8·15·22·29일에 종료된 직전 주차 미션을 판정합니다. */
    @Scheduled(
            cron = "${saving.scheduler.weekly-evaluation-cron}",
            zone = "${saving.scheduler.zone}"
    )
    public void evaluateWeeklyMissions() {
        LocalDate runDate = LocalDate.now(clock);
        log.info("주간 미션 자동 판정 시작 - 실행일: {}", runDate);
        try {
            logResult(automationService.evaluatePreviousWeekMissions(runDate));
        } catch (Exception e) {
            log.error("주간 미션 자동 판정 작업 실패 - 실행일: {}, 사유: {}",
                    runDate, e.getMessage(), e);
        }
    }

    private void logResult(SavingAutomationResult result) {
        log.info("저축 자동 처리 완료 - 작업: {}, 기준: {}, 대상: {}, 성공: {}, 실패: {}",
                result.jobName(), result.target(), result.targetCount(),
                result.successCount(), result.failureCount());
    }
}
