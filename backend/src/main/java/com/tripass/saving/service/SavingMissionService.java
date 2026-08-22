package com.tripass.saving.service;

import com.tripass.common.exception.CustomException;
import com.tripass.saving.analysis.MissionStartWeekPolicy;
import com.tripass.saving.analysis.MissionStartWeekPolicy.MissionStartResult;
import com.tripass.saving.dto.MissionCategorySelectionDto;
import com.tripass.saving.dto.MonthlyMissionResponseDto;
import com.tripass.saving.dto.MonthlySavingMissionDto;
import com.tripass.saving.dto.MonthlySpendingAnalysisDto;
import com.tripass.saving.dto.SavingMissionsResponseDto;
import com.tripass.saving.dto.WeeklyMissionResponseDto;
import com.tripass.saving.dto.WeeklySavingMissionDto;
import com.tripass.saving.mapper.SavingMissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SavingMissionService {

    private static final int LAST_MISSION_WEEK = 4;

    private final SavingMissionMapper mapper;
    private final MissionStartWeekPolicy startWeekPolicy;
    private final Clock clock;

    @Autowired
    public SavingMissionService(SavingMissionMapper mapper, MissionStartWeekPolicy startWeekPolicy) {
        this(mapper, startWeekPolicy, Clock.systemDefaultZone());
    }

    SavingMissionService(SavingMissionMapper mapper, MissionStartWeekPolicy startWeekPolicy, Clock clock) {
        this.mapper = mapper;
        this.startWeekPolicy = startWeekPolicy;
        this.clock = clock;
    }

    /**
     * #220에서 확정한 선택을 스냅샷으로 복사해 월간·주간 미션을 생성한다.
     * 동일 월에 이미 생성된 카테고리는 유지하고, 새로 선택한 카테고리만 추가 생성한다.
     */
    @Transactional
    public CreationResult createMissions(Long userId, YearMonth targetYearMonth) {
        validateTripStatusForMission(userId);
        MonthlySpendingAnalysisDto analysis = resolveAnalysis(userId, targetYearMonth);
        mapper.lockMonthlySpendingAnalysis(analysis.getId());

        List<MonthlySavingMissionDto> existing = mapper.findMonthlyMissions(userId, targetYearMonth.toString());
        List<MissionCategorySelectionDto> selections = mapper.findSelections(analysis.getId());
        if (selections.isEmpty() && existing.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "MISSION_SELECTION_REQUIRED",
                    "선택한 절감 미션이 없습니다. 카테고리별 절감률을 먼저 선택해 주세요.");
        }

        Set<Long> startedCategoryIds = existing.stream()
                .map(MonthlySavingMissionDto::getCategoryId)
                .collect(Collectors.toSet());
        List<MissionCategorySelectionDto> newSelections = selections.stream()
                .filter(selection -> !startedCategoryIds.contains(selection.getCategoryId()))
                .toList();
        if (newSelections.isEmpty()) {
            return new CreationResult(false, buildResponse(targetYearMonth, existing));
        }

        MissionStartResult startResult = resolveStartResult(targetYearMonth, LocalDate.now(clock));
        Long tripId = mapper.findActiveTripIdByUserId(userId);
        for (MissionCategorySelectionDto selection : newSelections) {
            createCategoryMission(userId, tripId, analysis, targetYearMonth, selection, startResult);
        }
        mapper.markReportClosed(userId, analysis.getAnalysisYearMonth());

        SavingMissionsResponseDto response =
                buildResponse(targetYearMonth, mapper.findMonthlyMissions(userId, targetYearMonth.toString()));
        return new CreationResult(true, response);
    }

    public SavingMissionsResponseDto getMissions(Long userId, YearMonth targetYearMonth) {
        return buildResponse(targetYearMonth, mapper.findMonthlyMissions(userId, targetYearMonth.toString()));
    }

    private void createCategoryMission(
            Long userId,
            Long tripId,
            MonthlySpendingAnalysisDto analysis,
            YearMonth targetYearMonth,
            MissionCategorySelectionDto selection,
            MissionStartResult startResult
    ) {
        int startWeek = startResult.startWeek();
        int eligibleDayCount = startResult.eligibleDayCount();

        int plannedSavingAmount = 0;
        for (int week = startWeek; week <= LAST_MISSION_WEEK; week++) {
            int weeklyAmount = amountForWeek(selection.getMonthlyReductionTarget(), week);
            if (week == startWeek && eligibleDayCount < 7) {
                weeklyAmount = weeklyAmount * eligibleDayCount / 7;
            }
            plannedSavingAmount += weeklyAmount;
        }

        MonthlySavingMissionDto monthly = new MonthlySavingMissionDto();
        monthly.setUserId(userId);
        monthly.setTripId(tripId);
        monthly.setMonthlySpendingAnalysisId(analysis.getId());
        monthly.setMissionCategorySelectionId(selection.getId());
        monthly.setCategoryId(selection.getCategoryId());
        monthly.setTargetYearMonth(targetYearMonth.toString());
        monthly.setReductionRate(selection.getReductionRate());
        monthly.setBaselineSpendingAmount(selection.getBaselineSpendingAmount());
        monthly.setMonthlyReductionTarget(selection.getMonthlyReductionTarget());
        monthly.setMonthlyUsageTarget(selection.getMonthlyUsageTarget());
        monthly.setPlannedSavingAmount(plannedSavingAmount);
        monthly.setStartWeek(startWeek);
        monthly.setSelectedAt(LocalDate.now(clock).toString());
        monthly.setMissionStartDate(startResult.missionStartDate().toString());
        monthly.setStatus("IN_PROGRESS");
        mapper.insertMonthlyMission(monthly);

        for (int week = startWeek; week <= LAST_MISSION_WEEK; week++) {
            mapper.insertWeeklyMission(toWeeklyMission(
                    monthly.getId(), targetYearMonth, selection, week,
                    week == startWeek ? eligibleDayCount : 7,
                    week == startWeek ? startResult.missionStartDate() : null));
        }
    }

    private WeeklySavingMissionDto toWeeklyMission(
            Long monthlyMissionId,
            YearMonth targetYearMonth,
            MissionCategorySelectionDto selection,
            int week,
            int eligibleDays,
            LocalDate overrideStartDate
    ) {
        int weekStartDay = (week - 1) * 7 + 1;
        LocalDate periodStart = overrideStartDate != null ? overrideStartDate : targetYearMonth.atDay(weekStartDay);
        LocalDate periodEnd = targetYearMonth.atDay(weekStartDay + 6);

        int weeklyLimit = amountForWeek(selection.getMonthlyUsageTarget(), week);
        int weeklyExpected = amountForWeek(selection.getMonthlyReductionTarget(), week);
        if (eligibleDays < 7) {
            weeklyLimit = weeklyLimit * eligibleDays / 7;
            weeklyExpected = weeklyExpected * eligibleDays / 7;
        }

        WeeklySavingMissionDto weekly = new WeeklySavingMissionDto();
        weekly.setMonthlySavingMissionId(monthlyMissionId);
        weekly.setWeekNumber(week);
        weekly.setPeriodStartDate(periodStart);
        weekly.setPeriodEndDate(periodEnd);
        weekly.setWeeklyUsageLimit(weeklyLimit);
        weekly.setWeeklyExpectedSaving(weeklyExpected);
        weekly.setEligibleDayCount(eligibleDays);
        weekly.setStatus("PENDING");
        return weekly;
    }

    /** 4로 나눈 원 단위 나머지는 4주차에 반영한다. */
    private int amountForWeek(int monthlyAmount, int week) {
        int base = monthlyAmount / LAST_MISSION_WEEK;
        return week == LAST_MISSION_WEEK ? base + monthlyAmount % LAST_MISSION_WEEK : base;
    }

    private MissionStartResult resolveStartResult(YearMonth targetYearMonth, LocalDate selectedDate) {
        try {
            return startWeekPolicy.resolve(targetYearMonth, selectedDate);
        } catch (IllegalArgumentException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "MISSION_START_NOT_AVAILABLE", e.getMessage());
        }
    }

    private MonthlySpendingAnalysisDto resolveAnalysis(Long userId, YearMonth targetYearMonth) {
        MonthlySpendingAnalysisDto analysis =
                mapper.findAnalysisByTargetMonth(userId, targetYearMonth.toString());
        if (analysis == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "MONTHLY_ANALYSIS_NOT_FOUND",
                    "미션 적용월에 해당하는 월간 분석 리포트를 찾을 수 없습니다.");
        }
        if ("PENDING".equals(analysis.getReportStatus())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "MISSION_REPORT_NOT_VIEWED",
                    "지난달 분석 리포트를 먼저 확인해야 미션을 등록할 수 있어요.");
        }
        return analysis;
    }

    private SavingMissionsResponseDto buildResponse(
            YearMonth targetYearMonth, List<MonthlySavingMissionDto> monthlyMissions
    ) {
        List<MonthlyMissionResponseDto> missions = monthlyMissions.stream()
                .map(this::toMonthlyResponse)
                .toList();
        long totalPlanned = missions.stream().mapToLong(MonthlyMissionResponseDto::plannedSavingAmount).sum();
        long totalReward = missions.stream().mapToLong(MonthlyMissionResponseDto::rewardAmount).sum();
        return new SavingMissionsResponseDto(
                targetYearMonth.toString(), missions.size(), totalPlanned, totalReward, missions);
    }

    private MonthlyMissionResponseDto toMonthlyResponse(MonthlySavingMissionDto monthly) {
        List<WeeklyMissionResponseDto> weekly = mapper.findWeeklyMissions(monthly.getId()).stream()
                .map(item -> toWeeklyResponse(monthly.getCategoryName(), item))
                .toList();
        int rewardAmount = weekly.stream()
                .map(WeeklyMissionResponseDto::rewardAmount)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        return new MonthlyMissionResponseDto(
                monthly.getId(), monthly.getCategoryId(), monthly.getCategoryCode(), monthly.getCategoryName(),
                monthly.getReductionRate(), monthly.getBaselineSpendingAmount(), monthly.getMonthlyReductionTarget(),
                monthly.getMonthlyUsageTarget(), monthly.getPlannedSavingAmount(), rewardAmount, monthly.getStartWeek(),
                monthly.getStatus(), weekly);
    }

    private WeeklyMissionResponseDto toWeeklyResponse(String categoryName, WeeklySavingMissionDto weekly) {
        String amount = NumberFormat.getNumberInstance(Locale.KOREA).format(weekly.getWeeklyExpectedSaving());
        return new WeeklyMissionResponseDto(
                weekly.getId(), weekly.getWeekNumber(), weekly.getPeriodStartDate(), weekly.getPeriodEndDate(),
                weekly.getWeeklyUsageLimit(), weekly.getWeeklyExpectedSaving(), weekly.getEligibleDayCount(),
                weekly.getActualSpending(), weekly.getActualSaving(), weekly.getRewardAmount(), weekly.getRewardedAt(),
                weekly.getStatus(), categoryName + " 지출을 " + amount + "원 줄이세요.");
    }

    private void validateTripStatusForMission(Long userId) {
        String status = mapper.findActiveTripStatusByUserId(userId);
        if (status == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "TRIP_REQUIRED_FOR_MISSION",
                    "여행 계획을 등록해야 미션을 진행할 수 있어요.");
        }
        if (!"PLANNING".equals(status)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "MISSION_NOT_ALLOWED",
                    "여행 중이거나 종료된 여행에서는 미션을 생성할 수 없습니다.");
        }
        if (!Boolean.TRUE.equals(mapper.isSavingsTrackingStartedForUser(userId))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "ACCOUNT_REQUIRED_FOR_MISSION",
                    "계좌를 등록해야 미션을 진행할 수 있어요.");
        }
        if (mapper.countActiveCardsByUserId(userId) == 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "CARD_REQUIRED_FOR_MISSION",
                    "카드를 등록해야 소비 분석 기반 미션을 진행할 수 있어요.");
        }
    }

    /** Controller가 최초 생성(201)과 멱등 재호출(200)을 구분할 수 있게 하는 내부 결과. */
    public record CreationResult(boolean created, SavingMissionsResponseDto data) {
    }
}
