package com.tripass.saving.service;

import com.tripass.saving.dto.SavingAutomationResult;
import com.tripass.saving.mapper.SavingAutomationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 월간 분석과 주간 미션 판정을 사용자 단위로 실행합니다.
 *
 * 이 서비스에는 의도적으로 외부 트랜잭션을 선언하지 않습니다. 실제 분석·판정 서비스가
 * 사용자별 트랜잭션을 시작하도록 하여 한 사용자의 실패가 다른 사용자에게 전파되지 않게 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavingAutomationService {

    private static final String MONTHLY_JOB = "MONTHLY_ANALYSIS";
    private static final String WEEKLY_JOB = "WEEKLY_MISSION_EVALUATION";

    private final SavingAutomationMapper automationMapper;
    private final MonthlySpendingAnalysisService monthlyAnalysisService;
    private final WeeklyMissionEvaluationService weeklyEvaluationService;

    /** 실행일의 직전 달 소비 분석 리포트를 생성하거나 갱신합니다. */
    public SavingAutomationResult generatePreviousMonthAnalyses(LocalDate runDate) {
        YearMonth analysisYearMonth = YearMonth.from(runDate).minusMonths(1);
        List<Long> userIds = automationMapper.findMonthlyAnalysisTargetUserIds(
                analysisYearMonth.atDay(1), analysisYearMonth.atEndOfMonth());

        int successCount = 0;
        int failureCount = 0;
        for (Long userId : userIds) {
            try {
                // 기존 리포트가 있으면 MonthlySpendingAnalysisService가 중복 INSERT 대신 갱신합니다.
                monthlyAnalysisService.generateMonthlyAnalysis(userId, analysisYearMonth);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("월간 소비 분석 자동 생성 실패 - 사용자: {}, 분석월: {}, 사유: {}",
                        userId, analysisYearMonth, e.getMessage(), e);
            }
        }

        return new SavingAutomationResult(
                MONTHLY_JOB, analysisYearMonth.toString(), userIds.size(), successCount, failureCount);
    }

    /** 실행일에 대응하는 직전 주차의 미션을 판정하고 성공 금액을 월렛에 반영합니다. */
    public SavingAutomationResult evaluatePreviousWeekMissions(LocalDate runDate) {
        int weekNumber = resolveEvaluationWeek(runDate.getDayOfMonth());
        YearMonth targetYearMonth = YearMonth.from(runDate);
        List<Long> userIds = automationMapper.findWeeklyEvaluationTargetUserIds(
                targetYearMonth.toString(), weekNumber, runDate);

        int successCount = 0;
        int failureCount = 0;
        for (Long userId : userIds) {
            try {
                // 판정 서비스 내부에서 성공 금액 적립까지 같은 트랜잭션으로 처리합니다.
                weeklyEvaluationService.evaluate(userId, targetYearMonth, weekNumber);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("주간 미션 자동 판정 실패 - 사용자: {}, 대상월: {}, 주차: {}, 사유: {}",
                        userId, targetYearMonth, weekNumber, e.getMessage(), e);
            }
        }

        return new SavingAutomationResult(
                WEEKLY_JOB,
                targetYearMonth + "-W" + weekNumber,
                userIds.size(), successCount, failureCount);
    }

    static int resolveEvaluationWeek(int dayOfMonth) {
        return switch (dayOfMonth) {
            case 8 -> 1;
            case 15 -> 2;
            case 22 -> 3;
            case 29 -> 4;
            default -> throw new IllegalArgumentException("주간 미션 판정일은 8일, 15일, 22일, 29일이어야 합니다.");
        };
    }
}
