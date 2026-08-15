package com.tripass.saving.service;

import com.tripass.common.exception.CustomException;
import com.tripass.saving.analysis.MissionReductionCalculator;
import com.tripass.saving.dto.CategorySelectionItemDto;
import com.tripass.saving.dto.MissionCategorySelectionDto;
import com.tripass.saving.dto.MissionOptionResponseDto;
import com.tripass.saving.dto.MissionSelectionRequestDto;
import com.tripass.saving.dto.MissionSelectionResponseDto;
import com.tripass.saving.dto.MissionSelectionsResponseDto;
import com.tripass.saving.dto.MonthlyCategoryAnalysisDto;
import com.tripass.saving.dto.MonthlySpendingAnalysisDto;
import com.tripass.saving.mapper.MissionCategorySelectionMapper;
import com.tripass.saving.mapper.MonthlySpendingAnalysisMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionCategorySelectionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ANALYSIS_ID = 100L;
    private static final Long FOOD_ID = 1L;
    private static final Long CAFE_ID = 2L;
    private static final Long SHOPPING_ID = 4L;
    private static final YearMonth ANALYSIS_MONTH = YearMonth.of(2026, 7);

    @Mock
    private MonthlySpendingAnalysisMapper monthlySpendingAnalysisMapper;

    @Mock
    private MissionCategorySelectionMapper missionCategorySelectionMapper;

    private MissionCategorySelectionService service;

    @BeforeEach
    void setUp() {
        service = new MissionCategorySelectionService(
                monthlySpendingAnalysisMapper, missionCategorySelectionMapper, new MissionReductionCalculator());
    }

    // ===== getMissionOptions =====

    @Test
    @DisplayName("TOP 3 카테고리마다 10/30/50% 옵션과 추천 순위·근거를 함께 반환한다")
    void getMissionOptions_returnsOptionsForEachTopCategory() {
        stubAnalysisExists();
        when(monthlySpendingAnalysisMapper.findRecommendedCategoryAnalyses(ANALYSIS_ID))
                .thenReturn(List.of(topCategory(FOOD_ID, "FOOD", "식비", "100000", 1,
                        "최근 3개월 평균보다 소비가 35% 증가했어요.")));

        List<MissionOptionResponseDto> options = service.getMissionOptions(USER_ID, ANALYSIS_MONTH);

        assertEquals(1, options.size());
        MissionOptionResponseDto food = options.get(0);
        assertEquals(FOOD_ID, food.categoryId());
        assertEquals(100000, food.baselineSpendingAmount());
        assertEquals(1, food.recommendationRank());
        assertEquals("최근 3개월 평균보다 소비가 35% 증가했어요.", food.recommendationReason());
        assertEquals(3, food.options().size());
        assertEquals(10000, food.options().get(0).monthlyReductionTarget()); // 10%
    }

    @Test
    @DisplayName("추천 후보가 없으면 빈 목록을 반환한다")
    void getMissionOptions_noTopCategories_returnsEmptyList() {
        stubAnalysisExists();
        when(monthlySpendingAnalysisMapper.findRecommendedCategoryAnalyses(ANALYSIS_ID)).thenReturn(List.of());

        List<MissionOptionResponseDto> options = service.getMissionOptions(USER_ID, ANALYSIS_MONTH);

        assertTrue(options.isEmpty());
    }

    @Test
    @DisplayName("분석 리포트가 없으면 404 예외를 던진다")
    void getMissionOptions_analysisNotFound_throws() {
        when(monthlySpendingAnalysisMapper.findMonthlyAnalysis(USER_ID, "2026-07")).thenReturn(null);

        CustomException exception = assertThrows(CustomException.class,
                () -> service.getMissionOptions(USER_ID, ANALYSIS_MONTH));
        assertEquals("MONTHLY_ANALYSIS_NOT_FOUND", exception.getErrorCode());
    }

    // ===== saveMissionSelections =====

    @Test
    @DisplayName("TOP 3 중 여러 카테고리를 서로 다른 절감률로 선택해서 저장한다")
    void saveMissionSelections_savesMultipleCategoriesWithDifferentRates() {
        stubAnalysisExists();
        when(monthlySpendingAnalysisMapper.findRecommendedCategoryAnalyses(ANALYSIS_ID)).thenReturn(List.of(
                topCategory(CAFE_ID, "CAFE", "카페", "50000"),
                topCategory(SHOPPING_ID, "SHOPPING", "쇼핑", "200000")
        ));
        when(missionCategorySelectionMapper.findSelections(ANALYSIS_ID)).thenReturn(List.of());

        MissionSelectionRequestDto request = requestOf(
                item(CAFE_ID, 30), item(SHOPPING_ID, 10));

        service.saveMissionSelections(USER_ID, ANALYSIS_MONTH, request);

        ArgumentCaptor<MissionCategorySelectionDto> captor = ArgumentCaptor.forClass(MissionCategorySelectionDto.class);
        verify(missionCategorySelectionMapper, org.mockito.Mockito.times(2)).upsertSelection(captor.capture());

        MissionCategorySelectionDto cafeSaved = byCategory(captor.getAllValues(), CAFE_ID);
        assertEquals(30, cafeSaved.getReductionRate());
        assertEquals(15000, cafeSaved.getMonthlyReductionTarget()); // 50000 * 30%

        MissionCategorySelectionDto shoppingSaved = byCategory(captor.getAllValues(), SHOPPING_ID);
        assertEquals(10, shoppingSaved.getReductionRate());
        assertEquals(20000, shoppingSaved.getMonthlyReductionTarget()); // 200000 * 10%

        verify(missionCategorySelectionMapper)
                .deleteSelectionsExcept(eq(ANALYSIS_ID), eq(List.of(CAFE_ID, SHOPPING_ID)));
    }

    @Test
    @DisplayName("저장 전에 월간 분석 행을 먼저 잠가 동시 PUT의 잠금 순서를 직렬화한다")
    void saveMissionSelections_locksAnalysisBeforeUpsertAndDelete() {
        stubAnalysisExists();
        when(monthlySpendingAnalysisMapper.findRecommendedCategoryAnalyses(ANALYSIS_ID))
                .thenReturn(List.of(topCategory(FOOD_ID, "FOOD", "식비", "100000")));
        when(missionCategorySelectionMapper.findSelections(ANALYSIS_ID)).thenReturn(List.of());

        service.saveMissionSelections(USER_ID, ANALYSIS_MONTH, requestOf(item(FOOD_ID, 10)));

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(missionCategorySelectionMapper);
        inOrder.verify(missionCategorySelectionMapper).lockMonthlySpendingAnalysis(ANALYSIS_ID);
        inOrder.verify(missionCategorySelectionMapper).upsertSelection(any());
        inOrder.verify(missionCategorySelectionMapper).deleteSelectionsExcept(eq(ANALYSIS_ID), anyList());
    }

    @Test
    @DisplayName("빈 selections를 보내면 전체 선택을 해제한다")
    void saveMissionSelections_emptySelections_deletesAll() {
        stubAnalysisExists();
        when(missionCategorySelectionMapper.findSelections(ANALYSIS_ID)).thenReturn(List.of());

        service.saveMissionSelections(USER_ID, ANALYSIS_MONTH, requestOf());

        verify(missionCategorySelectionMapper, never()).upsertSelection(any());
        verify(missionCategorySelectionMapper).deleteSelectionsExcept(ANALYSIS_ID, List.of());
        verify(monthlySpendingAnalysisMapper, never()).findRecommendedCategoryAnalyses(any());
    }

    @Test
    @DisplayName("TOP 3에 없는 카테고리를 선택하면 400 예외를 던지고 저장하지 않는다")
    void saveMissionSelections_categoryNotRecommended_throws() {
        stubAnalysisExists();
        when(monthlySpendingAnalysisMapper.findRecommendedCategoryAnalyses(ANALYSIS_ID))
                .thenReturn(List.of(topCategory(FOOD_ID, "FOOD", "식비", "100000")));

        MissionSelectionRequestDto request = requestOf(item(CAFE_ID, 10));

        CustomException exception = assertThrows(CustomException.class,
                () -> service.saveMissionSelections(USER_ID, ANALYSIS_MONTH, request));
        assertEquals("CATEGORY_NOT_RECOMMENDED", exception.getErrorCode());
        verify(missionCategorySelectionMapper, never()).upsertSelection(any());
        verify(missionCategorySelectionMapper, never()).deleteSelectionsExcept(any(), anyList());
    }

    @Test
    @DisplayName("같은 카테고리를 중복 선택하면 400 예외를 던진다")
    void saveMissionSelections_duplicateCategory_throws() {
        stubAnalysisExists();

        MissionSelectionRequestDto request = requestOf(item(FOOD_ID, 10), item(FOOD_ID, 30));

        CustomException exception = assertThrows(CustomException.class,
                () -> service.saveMissionSelections(USER_ID, ANALYSIS_MONTH, request));
        assertEquals("DUPLICATE_CATEGORY_SELECTION", exception.getErrorCode());
        verify(monthlySpendingAnalysisMapper, never()).findRecommendedCategoryAnalyses(any());
    }

    @Test
    @DisplayName("절감률이 10/30/50이 아니면 400 예외를 던진다")
    void saveMissionSelections_invalidReductionRate_throws() {
        stubAnalysisExists();
        when(monthlySpendingAnalysisMapper.findRecommendedCategoryAnalyses(ANALYSIS_ID))
                .thenReturn(List.of(topCategory(FOOD_ID, "FOOD", "식비", "100000")));

        MissionSelectionRequestDto request = requestOf(item(FOOD_ID, 20));

        CustomException exception = assertThrows(CustomException.class,
                () -> service.saveMissionSelections(USER_ID, ANALYSIS_MONTH, request));
        assertEquals("INVALID_REDUCTION_RATE", exception.getErrorCode());
        verify(missionCategorySelectionMapper, never()).upsertSelection(any());
    }

    @Test
    @DisplayName("분석 리포트가 없으면 404 예외를 던지고 아무것도 저장하지 않는다")
    void saveMissionSelections_analysisNotFound_throws() {
        when(monthlySpendingAnalysisMapper.findMonthlyAnalysis(USER_ID, "2026-07")).thenReturn(null);

        CustomException exception = assertThrows(CustomException.class,
                () -> service.saveMissionSelections(USER_ID, ANALYSIS_MONTH, requestOf(item(FOOD_ID, 10))));
        assertEquals("MONTHLY_ANALYSIS_NOT_FOUND", exception.getErrorCode());
        verify(missionCategorySelectionMapper, never()).upsertSelection(any());
    }

    // ===== getMissionSelections =====

    @Test
    @DisplayName("저장된 선택 결과에 주간 금액을 다시 계산해서 채우고, 전체 합계도 함께 반환한다")
    void getMissionSelections_returnsSavedSelectionsWithComputedWeeklyAmountsAndTotals() {
        stubAnalysisExists();
        MissionCategorySelectionDto foodSaved = new MissionCategorySelectionDto();
        foodSaved.setCategoryId(FOOD_ID);
        foodSaved.setCategoryCode("FOOD");
        foodSaved.setCategoryName("식비");
        foodSaved.setReductionRate(10);
        foodSaved.setBaselineSpendingAmount(100000);
        foodSaved.setMonthlyReductionTarget(10000);
        foodSaved.setMonthlyUsageTarget(90000);

        MissionCategorySelectionDto cafeSaved = new MissionCategorySelectionDto();
        cafeSaved.setCategoryId(CAFE_ID);
        cafeSaved.setCategoryCode("CAFE");
        cafeSaved.setCategoryName("카페");
        cafeSaved.setReductionRate(30);
        cafeSaved.setBaselineSpendingAmount(50000);
        cafeSaved.setMonthlyReductionTarget(15000);
        cafeSaved.setMonthlyUsageTarget(35000);

        when(missionCategorySelectionMapper.findSelections(ANALYSIS_ID)).thenReturn(List.of(foodSaved, cafeSaved));

        MissionSelectionsResponseDto result = service.getMissionSelections(USER_ID, ANALYSIS_MONTH);

        assertEquals(2, result.selectedMissionCount());
        assertEquals(25000L, result.totalMonthlyReductionTarget()); // 10000 + 15000
        assertEquals(6250L, result.totalWeeklyExpectedSaving()); // 2500 + 3750

        MissionSelectionResponseDto food = result.selections().get(0);
        assertEquals(10000, food.monthlyReductionTarget());
        assertEquals(90000, food.monthlyUsageTarget());
        assertEquals(22500, food.weeklyUsageLimit());
        assertEquals(2500, food.weeklyExpectedSaving());
    }

    // ===== 테스트 헬퍼 =====

    private void stubAnalysisExists() {
        MonthlySpendingAnalysisDto analysis = new MonthlySpendingAnalysisDto();
        analysis.setId(ANALYSIS_ID);
        when(monthlySpendingAnalysisMapper.findMonthlyAnalysis(USER_ID, "2026-07")).thenReturn(analysis);
    }

    private MonthlyCategoryAnalysisDto topCategory(Long categoryId, String code, String name, String missionSpending) {
        return topCategory(categoryId, code, name, missionSpending, null, null);
    }

    private MonthlyCategoryAnalysisDto topCategory(
            Long categoryId, String code, String name, String missionSpending,
            Integer recommendationRank, String recommendationReason
    ) {
        MonthlyCategoryAnalysisDto dto = new MonthlyCategoryAnalysisDto();
        dto.setCategoryId(categoryId);
        dto.setCategoryCode(code);
        dto.setCategoryName(name);
        dto.setMissionPeriodSpending(new BigDecimal(missionSpending));
        dto.setRecommendationRank(recommendationRank);
        dto.setRecommendationReason(recommendationReason);
        return dto;
    }

    private MissionSelectionRequestDto requestOf(CategorySelectionItemDto... items) {
        return MissionSelectionRequestDto.builder().selections(List.of(items)).build();
    }

    private CategorySelectionItemDto item(Long categoryId, int reductionRate) {
        return CategorySelectionItemDto.builder().categoryId(categoryId).reductionRate(reductionRate).build();
    }

    private MissionCategorySelectionDto byCategory(List<MissionCategorySelectionDto> saved, Long categoryId) {
        return saved.stream().filter(dto -> dto.getCategoryId().equals(categoryId)).findFirst().orElseThrow();
    }
}
