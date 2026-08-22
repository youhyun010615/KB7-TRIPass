package com.tripass.saving.service;

import com.tripass.common.exception.CustomException;
import com.tripass.saving.analysis.MissionStartWeekPolicy;
import com.tripass.saving.dto.MissionCategorySelectionDto;
import com.tripass.saving.dto.MonthlySavingMissionDto;
import com.tripass.saving.dto.MonthlySpendingAnalysisDto;
import com.tripass.saving.dto.WeeklySavingMissionDto;
import com.tripass.saving.mapper.SavingMissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingMissionServiceTest {

    private static final Long USER_ID = 3L;
    private static final Long ANALYSIS_ID = 10L;
    private static final YearMonth TARGET_MONTH = YearMonth.of(2026, 8);

    @Mock
    private SavingMissionMapper mapper;

    private SavingMissionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new SavingMissionService(mapper, new MissionStartWeekPolicy(), clock);
    }

    @Test
    void createsOnlyRemainingFullWeeksAndReflectsRemainderInWeekFour() {
        MonthlySpendingAnalysisDto analysis = analysis();
        MissionCategorySelectionDto selection = selection();
        MonthlySavingMissionDto stored = storedMonthlyMission();

        when(mapper.findAnalysisByTargetMonth(USER_ID, "2026-08")).thenReturn(analysis);
        when(mapper.findMonthlyMissions(USER_ID, "2026-08")).thenReturn(List.of(), List.of(stored));
        when(mapper.findSelections(ANALYSIS_ID)).thenReturn(List.of(selection));
        when(mapper.findWeeklyMissions(100L)).thenReturn(List.of());
        doAnswer(invocation -> {
            MonthlySavingMissionDto value = invocation.getArgument(0);
            value.setId(100L);
            return null;
        }).when(mapper).insertMonthlyMission(any(MonthlySavingMissionDto.class));

        SavingMissionService.CreationResult result = service.createMissions(USER_ID, TARGET_MONTH);

        ArgumentCaptor<MonthlySavingMissionDto> monthlyCaptor =
                ArgumentCaptor.forClass(MonthlySavingMissionDto.class);
        verify(mapper).insertMonthlyMission(monthlyCaptor.capture());
        assertEquals(3, monthlyCaptor.getValue().getStartWeek());
        // 39,001 / 4 = 9,750, 4주차에 나머지 1원 반영. 3~4주차만 생성한다.
        assertEquals(19_501, monthlyCaptor.getValue().getPlannedSavingAmount());

        ArgumentCaptor<WeeklySavingMissionDto> weeklyCaptor =
                ArgumentCaptor.forClass(WeeklySavingMissionDto.class);
        verify(mapper, org.mockito.Mockito.times(2)).insertWeeklyMission(weeklyCaptor.capture());
        List<WeeklySavingMissionDto> weekly = weeklyCaptor.getAllValues();
        assertEquals(List.of(3, 4), weekly.stream().map(WeeklySavingMissionDto::getWeekNumber).toList());
        assertEquals(9_750, weekly.get(0).getWeeklyExpectedSaving());
        assertEquals(9_751, weekly.get(1).getWeeklyExpectedSaving());
        assertEquals("2026-08-15", weekly.get(0).getPeriodStartDate().toString());
        assertEquals("2026-08-28", weekly.get(1).getPeriodEndDate().toString());
        assertEquals(true, result.created());
        assertEquals(1, result.data().missionCount());
        verify(mapper).markReportClosed(USER_ID, "2026-07");
    }

    @Test
    void repeatedCreateReturnsExistingMissionsWithoutInserting() {
        when(mapper.findAnalysisByTargetMonth(USER_ID, "2026-08")).thenReturn(analysis());
        when(mapper.findMonthlyMissions(USER_ID, "2026-08")).thenReturn(List.of(storedMonthlyMission()));
        when(mapper.findSelections(ANALYSIS_ID)).thenReturn(List.of(selection()));
        when(mapper.findWeeklyMissions(100L)).thenReturn(List.of());

        SavingMissionService.CreationResult result = service.createMissions(USER_ID, TARGET_MONTH);

        assertEquals(false, result.created());
        assertEquals(1, result.data().missionCount());
        verify(mapper, never()).insertMonthlyMission(any());
        verify(mapper, never()).insertWeeklyMission(any());
    }

    @Test
    void addsOnlyNewCategoryWhenMissionAlreadyExists() {
        MonthlySavingMissionDto existing = storedMonthlyMission();
        MissionCategorySelectionDto existingSelection = selection();
        MissionCategorySelectionDto newSelection = selection(
                21L, 4L, "SHOPPING", "쇼핑", 10, 200_000, 20_000, 180_000);
        MonthlySavingMissionDto added = storedMonthlyMission(
                101L, 4L, "SHOPPING", "쇼핑", 10, 20_000, 10_000);

        when(mapper.findAnalysisByTargetMonth(USER_ID, "2026-08")).thenReturn(analysis());
        when(mapper.findMonthlyMissions(USER_ID, "2026-08"))
                .thenReturn(List.of(existing), List.of(existing, added));
        when(mapper.findSelections(ANALYSIS_ID)).thenReturn(List.of(existingSelection, newSelection));
        when(mapper.findWeeklyMissions(100L)).thenReturn(List.of());
        when(mapper.findWeeklyMissions(101L)).thenReturn(List.of());
        doAnswer(invocation -> {
            MonthlySavingMissionDto value = invocation.getArgument(0);
            value.setId(101L);
            return null;
        }).when(mapper).insertMonthlyMission(any(MonthlySavingMissionDto.class));

        SavingMissionService.CreationResult result = service.createMissions(USER_ID, TARGET_MONTH);

        ArgumentCaptor<MonthlySavingMissionDto> monthlyCaptor =
                ArgumentCaptor.forClass(MonthlySavingMissionDto.class);
        verify(mapper).insertMonthlyMission(monthlyCaptor.capture());
        assertEquals(4L, monthlyCaptor.getValue().getCategoryId());
        assertEquals(10_000, monthlyCaptor.getValue().getPlannedSavingAmount());
        verify(mapper, org.mockito.Mockito.times(2)).insertWeeklyMission(any(WeeklySavingMissionDto.class));
        assertEquals(true, result.created());
        assertEquals(2, result.data().missionCount());
    }

    @Test
    void requiresAtLeastOneSavedSelection() {
        when(mapper.findAnalysisByTargetMonth(USER_ID, "2026-08")).thenReturn(analysis());
        when(mapper.findMonthlyMissions(USER_ID, "2026-08")).thenReturn(List.of());
        when(mapper.findSelections(ANALYSIS_ID)).thenReturn(List.of());

        CustomException exception = assertThrows(CustomException.class,
                () -> service.createMissions(USER_ID, TARGET_MONTH));

        assertEquals("MISSION_SELECTION_REQUIRED", exception.getErrorCode());
    }

    @Test
    void rejectsCreationAfterDay22() {
        Clock lateClock = Clock.fixed(Instant.parse("2026-08-23T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new SavingMissionService(mapper, new MissionStartWeekPolicy(), lateClock);
        when(mapper.findAnalysisByTargetMonth(USER_ID, "2026-08")).thenReturn(analysis());
        when(mapper.findMonthlyMissions(USER_ID, "2026-08")).thenReturn(List.of());
        when(mapper.findSelections(ANALYSIS_ID)).thenReturn(List.of(selection()));

        CustomException exception = assertThrows(CustomException.class,
                () -> service.createMissions(USER_ID, TARGET_MONTH));

        assertEquals("MISSION_START_NOT_AVAILABLE", exception.getErrorCode());
    }

    private MonthlySpendingAnalysisDto analysis() {
        MonthlySpendingAnalysisDto dto = new MonthlySpendingAnalysisDto();
        dto.setId(ANALYSIS_ID);
        dto.setUserId(USER_ID);
        dto.setAnalysisYearMonth("2026-07");
        dto.setTargetYearMonth("2026-08");
        return dto;
    }

    private MissionCategorySelectionDto selection() {
        return selection(20L, 2L, "CAFE", "카페", 30, 130_003, 39_001, 91_002);
    }

    private MissionCategorySelectionDto selection(
            Long id, Long categoryId, String categoryCode, String categoryName, int reductionRate,
            int baselineSpendingAmount, int monthlyReductionTarget, int monthlyUsageTarget
    ) {
        MissionCategorySelectionDto dto = new MissionCategorySelectionDto();
        dto.setId(id);
        dto.setMonthlySpendingAnalysisId(ANALYSIS_ID);
        dto.setCategoryId(categoryId);
        dto.setCategoryCode(categoryCode);
        dto.setCategoryName(categoryName);
        dto.setReductionRate(reductionRate);
        dto.setBaselineSpendingAmount(baselineSpendingAmount);
        dto.setMonthlyReductionTarget(monthlyReductionTarget);
        dto.setMonthlyUsageTarget(monthlyUsageTarget);
        return dto;
    }

    private MonthlySavingMissionDto storedMonthlyMission() {
        return storedMonthlyMission(100L, 2L, "CAFE", "카페", 30, 39_001, 19_501);
    }

    private MonthlySavingMissionDto storedMonthlyMission(
            Long id, Long categoryId, String categoryCode, String categoryName, int reductionRate,
            int monthlyReductionTarget, int plannedSavingAmount
    ) {
        MonthlySavingMissionDto dto = new MonthlySavingMissionDto();
        dto.setId(id);
        dto.setUserId(USER_ID);
        dto.setCategoryId(categoryId);
        dto.setCategoryCode(categoryCode);
        dto.setCategoryName(categoryName);
        dto.setTargetYearMonth("2026-08");
        dto.setReductionRate(reductionRate);
        dto.setBaselineSpendingAmount(130_003);
        dto.setMonthlyReductionTarget(monthlyReductionTarget);
        dto.setMonthlyUsageTarget(91_002);
        dto.setPlannedSavingAmount(plannedSavingAmount);
        dto.setStartWeek(3);
        dto.setStatus("IN_PROGRESS");
        return dto;
    }
}
