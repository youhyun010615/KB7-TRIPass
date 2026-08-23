package com.tripass.report.service;

import com.tripass.report.dto.*;
import com.tripass.report.exception.ReportErrorCode;
import com.tripass.report.exception.ReportException;
import com.tripass.report.mapper.ReportMapper;
import com.tripass.wallet.fx.mapper.WalletFxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final int RECENT_SAVING_HISTORY_LIMIT = 5;
    private static final int NEXT_TRIP_SAVING_MONTHS = 6;

    private final ReportMapper reportMapper;
    private final WalletFxMapper walletFxMapper;
    private final com.tripass.dev.util.DevDateUtil devDateUtil;

    /** 여행 대비 리포트를 조회합니다. */
    public PreTripReportResponseDto getPreTripReport(Long tripId, Long currentUserId) {
        TripBasicRowDto trip = validateTripOwnerAndGet(tripId, currentUserId);

        List<String> countryNames = reportMapper.findTripCountryNames(tripId);

        BigDecimal securedFund = orZero(reportMapper.findWalletBalanceByUserId(currentUserId));
        int savingsPercent = percentOf(securedFund, trip.getTotalTargetAmount());

        List<ReportCountryBudgetDto> countryBudgets = allocateCountryBudgets(reportMapper.findCountryBudgets(tripId));

        Long walletId = reportMapper.findWalletIdByUserId(currentUserId);
        List<SavingHistoryDto> savingHistory = walletId == null
                ? List.of()
                : reportMapper.findRecentWalletLedger(walletId, RECENT_SAVING_HISTORY_LIMIT);

        ChecklistProgressRowDto checklist = reportMapper.findChecklistProgress(tripId, "PRE_TRAVEL");
        ScheduleCountRowDto scheduleCount = reportMapper.findScheduleCounts(tripId);

        long dDay = Math.max(0, ChronoUnit.DAYS.between(devDateUtil.today(currentUserId), trip.getStartDate()));

        BigDecimal totalTargetAmount = orZero(trip.getTotalTargetAmount());
        BigDecimal emergencyFund = securedFund.subtract(totalTargetAmount).max(BigDecimal.ZERO);

        List<ChecklistStageProgressDto> checklistStages = buildChecklistStages(tripId);
        List<MonthlySavingsTrendDto> savingsTrend = buildSavingsTrend(currentUserId, walletId, tripId);
        List<SavingsInsightDto> insights = buildInsights(securedFund, savingsPercent, savingsTrend);

        return PreTripReportResponseDto.builder()
                .tripId(trip.getTripId())
                .tripName(trip.getTripName())
                .countryNames(countryNames)
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .daysUntilTrip(dDay)
                .targetBudget(totalTargetAmount)
                .securedFund(securedFund)
                .savingsPercent(savingsPercent)
                .savingHistory(savingHistory)
                .countryBudgets(countryBudgets)
                .checklistCompleted(defaultZero(checklist.getCompleted()))
                .checklistTotal(defaultZero(checklist.getTotal()))
                .scheduleCount(defaultZero(scheduleCount.getTotal()))
                .prepaidScheduleCount(defaultZero(scheduleCount.getPrepaid()))
                .onsiteScheduleCount(defaultZero(scheduleCount.getOnsite()))
                .emergencyFund(emergencyFund)
                .checklistStages(checklistStages)
                .savingsTrend(savingsTrend)
                .insights(insights)
                .build();
    }

    /** 나라별 목표 예산을 해당 통화로 환산한 금액을 채웁니다. */
    private List<ReportCountryBudgetDto> allocateCountryBudgets(List<ReportCountryBudgetDto> countryBudgets) {
        return countryBudgets.stream()
                .map(c -> {
                    BigDecimal budget = orZero(c.getBudget());
                    return ReportCountryBudgetDto.builder()
                            .countryName(c.getCountryName())
                            .budget(budget)
                            .currencyCode(c.getCurrencyCode())
                            .foreignAmount(estimateForeignAmount(budget, c.getCurrencyCode()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /** KRW 금액을 표시용으로 현재 환율에 맞춰 외화로 환산합니다(실거래 아님). 환율을 못 찾으면 null. */
    private BigDecimal estimateForeignAmount(BigDecimal krwAmount, String currencyCode) {
        if (currencyCode == null || "KRW".equals(currencyCode)) {
            return null;
        }
        BigDecimal rate = walletFxMapper.findLatestDealBaseRate(currencyCode);
        if (rate == null || rate.signum() <= 0) {
            return null;
        }
        return krwAmount.divide(rate, 2, RoundingMode.DOWN);
    }

    private static final List<String> CHECKLIST_STAGE_ORDER = List.of("D30", "D7", "D1");

    /** 체크리스트 완료 현황을 D-30/D-7/D-1 단계별 최종 결과로 만듭니다. */
    private List<ChecklistStageProgressDto> buildChecklistStages(Long tripId) {
        Map<String, ChecklistStageProgressRowDto> byStage = reportMapper.findChecklistStageProgress(tripId).stream()
                .collect(Collectors.toMap(ChecklistStageProgressRowDto::getStage, row -> row));
        return CHECKLIST_STAGE_ORDER.stream()
                .map(stage -> {
                    ChecklistStageProgressRowDto row = byStage.get(stage);
                    int total = row == null ? 0 : row.getTotal();
                    int completed = row == null ? 0 : row.getCompleted();
                    int percent = total > 0 ? Math.round(completed * 100f / total) : 0;
                    return ChecklistStageProgressDto.builder()
                            .stage(stage)
                            .completed(completed)
                            .total(total)
                            .message(checklistEvaluationMessage(total, percent))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String checklistEvaluationMessage(int total, int percent) {
        if (total == 0) return "등록된 준비 항목이 없어요.";
        if (percent >= 100) return "완벽하게 준비했어요! 최고예요.";
        if (percent >= 80) return "거의 다 챙겼어요, 훌륭해요!";
        if (percent >= 50) return "절반 넘게 챙겼어요, 잘하셨어요.";
        if (percent > 0) return "조금 아쉬워요, 다음엔 더 꼼꼼히 준비해봐요.";
        return "아쉽게도 많이 못 챙겼어요. 다음 여행엔 미리미리 준비해봐요.";
    }

    /** 저축 시작 달부터 여행 전달까지의 월별 저축 추이(누적 포함)를 만듭니다. */
    private List<MonthlySavingsTrendDto> buildSavingsTrend(Long currentUserId, Long walletId, Long tripId) {
        if (walletId == null) {
            return List.of();
        }
        List<MonthlySavingsTrendDto> trend = reportMapper.findSavingsTrend(currentUserId, walletId, tripId);
        BigDecimal cumulative = BigDecimal.ZERO;
        for (MonthlySavingsTrendDto row : trend) {
            cumulative = cumulative.add(orZero(row.getSavedAmount()));
            row.setCumulativeAmount(cumulative);
        }
        return trend;
    }

    /** 저축 목표 달성 상태를 바탕으로 간단한 분석/인사이트 문구를 만듭니다. */
    private List<SavingsInsightDto> buildInsights(
            BigDecimal securedFund, int savingsPercent, List<MonthlySavingsTrendDto> savingsTrend
    ) {
        List<SavingsInsightDto> insights = new java.util.ArrayList<>();

        // 저축 연속성: 최근 달부터 거슬러 올라가며 계속 저축한 달 수를 셉니다.
        int streak = 0;
        for (int i = savingsTrend.size() - 1; i >= 0; i--) {
            if (orZero(savingsTrend.get(i).getSavedAmount()).signum() > 0) {
                streak++;
            } else {
                break;
            }
        }
        if (streak >= 2) {
            insights.add(SavingsInsightDto.builder()
                    .icon("streak")
                    .title("얼마나 꾸준히 저축했을까요?")
                    .message(String.format("%d개월 연속으로 꾸준히 저축했어요!", streak))
                    .build());
        }

        List<MonthlySavingsTrendDto> trackedMonths = savingsTrend.stream()
                .filter(m -> orZero(m.getSavedAmount()).signum() > 0)
                .collect(Collectors.toList());
        if (!trackedMonths.isEmpty()) {
            BigDecimal total = trackedMonths.stream()
                    .map(MonthlySavingsTrendDto::getSavedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = total.divide(BigDecimal.valueOf(trackedMonths.size()), 0, RoundingMode.HALF_UP);
            insights.add(SavingsInsightDto.builder()
                    .icon("trend")
                    .title("한 달에 얼마씩 모았을까요?")
                    .message(String.format("최근 저축 활동이 있던 달 평균 %,d원씩 모았어요.", average.longValue()))
                    .build());

            MonthlySavingsTrendDto best = trackedMonths.stream()
                    .max((a, b) -> a.getSavedAmount().compareTo(b.getSavedAmount()))
                    .orElse(null);
            if (best != null) {
                insights.add(SavingsInsightDto.builder()
                        .icon("star")
                        .title("언제 가장 많이 모았을까요?")
                        .message(String.format("%s에 %,d원으로 가장 많이 저축했어요.", formatMonthLabel(best.getMonth()), best.getSavedAmount().longValue()))
                        .build());
            }
        }

        if (!savingsTrend.isEmpty()) {
            insights.add(SavingsInsightDto.builder()
                    .icon("journey")
                    .title("저축 여정 총평")
                    .message(String.format("%s부터 지금까지 총 %,d원을 모아 목표의 %d%%를 달성했어요.",
                            formatMonthLabel(savingsTrend.get(0).getMonth()), securedFund.longValue(), savingsPercent))
                    .build());
        }

        return insights;
    }

    /** 여행 후 리포트를 조회합니다. */
    public PostTripReportResponseDto getPostTripReport(Long tripId, Long currentUserId) {
        TripBasicRowDto trip = validateTripOwnerAndGet(tripId, currentUserId);
        if (!"ENDED".equals(trip.getStatus()) && !"ARCHIVED".equals(trip.getStatus())) {
            throw new ReportException(ReportErrorCode.TRIP_NOT_ENDED);
        }

        List<String> countryNames = reportMapper.findTripCountryNames(tripId);
        BigDecimal targetBudget = orZero(trip.getTotalTargetAmount());
        BigDecimal spent = orZero(reportMapper.findTravelExpenseTotal(tripId));
        BigDecimal remaining = targetBudget.subtract(spent);

        int days = (int) Math.max(1, ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1);
        BigDecimal dailyAverage = spent.divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
        BigDecimal savingsRate = targetBudget.signum() == 0
                ? BigDecimal.ZERO
                : remaining.multiply(BigDecimal.valueOf(1000))
                        .divide(targetBudget, 0, RoundingMode.HALF_UP)
                        .divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP);

        List<DailySpendingDto> dailySpending = reportMapper.findDailySpending(tripId);
        List<CategorySpendingDto> categorySpending = reportMapper.findCategorySpending(tripId);
        List<CountrySpendingDto> countrySpending = reportMapper.findCountrySpending(tripId);
        List<CountryTopCategoryDto> countryTopCategories = reportMapper.findCountryTopCategories(tripId);
        int receiptCount = reportMapper.findReceiptCount(tripId);

        BigDecimal nextTripMonthly = spent
                .divide(BigDecimal.valueOf(NEXT_TRIP_SAVING_MONTHS), 0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(10_000));

        return PostTripReportResponseDto.builder()
                .tripId(trip.getTripId())
                .tripName(trip.getTripName())
                .countryNames(countryNames)
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .days(days)
                .targetBudget(targetBudget)
                .spent(spent)
                .remaining(remaining)
                .dailyAverage(dailyAverage)
                .savingsRate(savingsRate)
                .dailySpending(dailySpending)
                .categorySpending(categorySpending)
                .countrySpending(countrySpending)
                .countryTopCategories(countryTopCategories)
                .receiptCount(receiptCount)
                .nextTripMonthlySuggestion(nextTripMonthly)
                .nextTripMonths(NEXT_TRIP_SAVING_MONTHS)
                .build();
    }

    /** 여행이 존재하고 로그인 사용자가 소유자인지 검증한 뒤 기본 정보를 반환합니다. */
    private TripBasicRowDto validateTripOwnerAndGet(Long tripId, Long currentUserId) {
        TripBasicRowDto trip = reportMapper.findTripBasicInfo(tripId);
        if (trip == null) {
            throw new ReportException(ReportErrorCode.TRIP_NOT_FOUND);
        }
        if (!trip.getUserId().equals(currentUserId)) {
            throw new ReportException(ReportErrorCode.TRIP_ACCESS_DENIED);
        }
        return trip;
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String formatMonthLabel(String yearMonth) {
        if (yearMonth == null || !yearMonth.contains("-")) {
            return yearMonth;
        }
        String[] parts = yearMonth.split("-");
        return Integer.parseInt(parts[1]) + "월";
    }

    private int percentOf(BigDecimal value, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return 0;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
