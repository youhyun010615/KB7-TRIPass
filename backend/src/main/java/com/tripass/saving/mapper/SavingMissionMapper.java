package com.tripass.saving.mapper;

import com.tripass.saving.dto.MissionCategorySelectionDto;
import com.tripass.saving.dto.MonthlySavingMissionDto;
import com.tripass.saving.dto.MonthlySpendingAnalysisDto;
import com.tripass.saving.dto.WeeklySavingMissionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SavingMissionMapper {
    MonthlySpendingAnalysisDto findAnalysisByTargetMonth(
            @Param("userId") Long userId, @Param("targetYearMonth") String targetYearMonth);

    void lockMonthlySpendingAnalysis(@Param("monthlySpendingAnalysisId") Long monthlySpendingAnalysisId);

    List<MissionCategorySelectionDto> findSelections(@Param("monthlySpendingAnalysisId") Long monthlySpendingAnalysisId);

    void insertMonthlyMission(MonthlySavingMissionDto dto);

    void insertWeeklyMission(WeeklySavingMissionDto dto);

    List<MonthlySavingMissionDto> findMonthlyMissions(
            @Param("userId") Long userId, @Param("targetYearMonth") String targetYearMonth);

    List<WeeklySavingMissionDto> findWeeklyMissions(@Param("monthlySavingMissionId") Long monthlySavingMissionId);

    List<WeeklySavingMissionDto> findWeeklyMissionsForEvaluation(
            @Param("userId") Long userId,
            @Param("targetYearMonth") String targetYearMonth,
            @Param("weekNumber") int weekNumber);

    int updateWeeklyMissionEvaluation(
            @Param("id") Long id,
            @Param("actualSpending") int actualSpending,
            @Param("actualSaving") int actualSaving,
            @Param("status") String status);

    int updateWeeklyMissionReward(
            @Param("id") Long id,
            @Param("rewardAmount") int rewardAmount,
            @Param("walletLedgerId") Long walletLedgerId);

    int completeMonthlyMissionIfAllWeeksEvaluated(@Param("monthlySavingMissionId") Long monthlySavingMissionId);

    int markReportClosed(@Param("userId") Long userId, @Param("analysisYearMonth") String analysisYearMonth);

    String findActiveTripStatusByUserId(@Param("userId") Long userId);

    Boolean isSavingsTrackingStartedForUser(@Param("userId") Long userId);

    Long findActiveTripIdByUserId(@Param("userId") Long userId);
}
