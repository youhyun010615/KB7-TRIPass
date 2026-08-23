package com.tripass.report.service;

import com.tripass.report.dto.*;
import com.tripass.report.exception.ReportErrorCode;
import com.tripass.report.exception.ReportException;
import com.tripass.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final int RECENT_SAVING_HISTORY_LIMIT = 5;
    private static final int NEXT_TRIP_SAVING_MONTHS = 6;

    private final ReportMapper reportMapper;
    private final com.tripass.dev.util.DevDateUtil devDateUtil;

    /** 여행 대비 리포트를 조회합니다. */
    public PreTripReportResponseDto getPreTripReport(Long tripId, Long currentUserId) {
        TripBasicRowDto trip = validateTripOwnerAndGet(tripId, currentUserId);

        List<String> countryNames = reportMapper.findTripCountryNames(tripId);
        List<ReportCountryBudgetDto> countryBudgets = reportMapper.findCountryBudgets(tripId);

        BigDecimal securedFund = orZero(reportMapper.findWalletBalanceByUserId(currentUserId));
        int savingsPercent = percentOf(securedFund, trip.getTotalTargetAmount());

        Long walletId = reportMapper.findWalletIdByUserId(currentUserId);
        List<SavingHistoryDto> savingHistory = walletId == null
                ? List.of()
                : reportMapper.findRecentWalletLedger(walletId, RECENT_SAVING_HISTORY_LIMIT);

        ChecklistProgressRowDto checklist = reportMapper.findChecklistProgress(tripId, "PRE_TRAVEL");
        ScheduleCountRowDto scheduleCount = reportMapper.findScheduleCounts(tripId);

        long dDay = Math.max(0, ChronoUnit.DAYS.between(devDateUtil.today(currentUserId), trip.getStartDate()));

        return PreTripReportResponseDto.builder()
                .tripId(trip.getTripId())
                .tripName(trip.getTripName())
                .countryNames(countryNames)
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .daysUntilTrip(dDay)
                .targetBudget(orZero(trip.getTotalTargetAmount()))
                .securedFund(securedFund)
                .savingsPercent(savingsPercent)
                .savingHistory(savingHistory)
                .countryBudgets(countryBudgets)
                .checklistCompleted(defaultZero(checklist.getCompleted()))
                .checklistTotal(defaultZero(checklist.getTotal()))
                .scheduleCount(defaultZero(scheduleCount.getTotal()))
                .prepaidScheduleCount(defaultZero(scheduleCount.getPrepaid()))
                .onsiteScheduleCount(defaultZero(scheduleCount.getOnsite()))
                .build();
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

    private int percentOf(BigDecimal value, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return 0;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
