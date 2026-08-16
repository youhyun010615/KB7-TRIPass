package com.tripass.saving.service;

import com.tripass.common.exception.CustomException;
import com.tripass.saving.analysis.MissionStartWeekPolicy;
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
     * 동일 월의 미션이 이미 있으면 재생성하지 않고 기존 결과를 반환한다.
     */
    @Transactional
    public CreationResult createMissions(Long userId, YearMonth targetYearMonth) {
        MonthlySpendingAnalysisDto analysis = resolveAnalysis(userId, targetYearMonth);
        mapper.lockMonthlySpendingAnalysis(analysis.getId());

        List<MonthlySavingMissionDto> existing = mapper.findMonthlyMissions(userId, targetYearMonth.toString());
        if (!existing.isEmpty()) {
            return new CreationResult(false, buildResponse(targetYearMonth, existing));
        }

        int startWeek = resolveStartWeek(targetYearMonth, LocalDate.now(clock));
        List<MissionCategorySelectionDto> selections = mapper.findSelections(analysis.getId());
        if (selections.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "MISSION_SELECTION_REQUIRED",
                    "선택한 절감 미션이 없습니다. 카테고리별 절감률을 먼저 선택해 주세요.");
        }

        for (MissionCategorySelectionDto selection : selections) {
            createCategoryMission(userId, analysis, targetYearMonth, selection, startWeek);
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
            MonthlySpendingAnalysisDto analysis,
            YearMonth targetYearMonth,
            MissionCategorySelectionDto selection,
            int startWeek
    ) {
        int plannedSavingAmount = 0;
        for (int week = startWeek; week <= LAST_MISSION_WEEK; week++) {
            plannedSavingAmount += amountForWeek(selection.getMonthlyReductionTarget(), week);
        }

        MonthlySavingMissionDto monthly = new MonthlySavingMissionDto();
        monthly.setUserId(userId);
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
        monthly.setStatus("IN_PROGRESS");
        mapper.insertMonthlyMission(monthly);

        for (int week = startWeek; week <= LAST_MISSION_WEEK; week++) {
            mapper.insertWeeklyMission(toWeeklyMission(monthly.getId(), targetYearMonth, selection, week));
        }
    }

    private WeeklySavingMissionDto toWeeklyMission(
            Long monthlyMissionId,
            YearMonth targetYearMonth,
            MissionCategorySelectionDto selection,
            int week
    ) {
        int startDay = (week - 1) * 7 + 1;
        WeeklySavingMissionDto weekly = new WeeklySavingMissionDto();
        weekly.setMonthlySavingMissionId(monthlyMissionId);
        weekly.setWeekNumber(week);
        weekly.setPeriodStartDate(targetYearMonth.atDay(startDay));
        weekly.setPeriodEndDate(targetYearMonth.atDay(startDay + 6));
        weekly.setWeeklyUsageLimit(amountForWeek(selection.getMonthlyUsageTarget(), week));
        weekly.setWeeklyExpectedSaving(amountForWeek(selection.getMonthlyReductionTarget(), week));
        weekly.setStatus("PENDING");
        return weekly;
    }

    /** 4로 나눈 원 단위 나머지는 4주차에 반영한다. */
    private int amountForWeek(int monthlyAmount, int week) {
        int base = monthlyAmount / LAST_MISSION_WEEK;
        return week == LAST_MISSION_WEEK ? base + monthlyAmount % LAST_MISSION_WEEK : base;
    }

    private int resolveStartWeek(YearMonth targetYearMonth, LocalDate selectedDate) {
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
                weekly.getWeeklyUsageLimit(), weekly.getWeeklyExpectedSaving(), weekly.getActualSpending(),
                weekly.getActualSaving(), weekly.getRewardAmount(), weekly.getRewardedAt(), weekly.getStatus(),
                categoryName + " 지출을 " + amount + "원 줄이세요.");
    }

    /** Controller가 최초 생성(201)과 멱등 재호출(200)을 구분할 수 있게 하는 내부 결과. */
    public record CreationResult(boolean created, SavingMissionsResponseDto data) {
    }
}
