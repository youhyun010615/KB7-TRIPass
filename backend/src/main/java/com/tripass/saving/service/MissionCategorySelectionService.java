package com.tripass.saving.service;

import com.tripass.common.exception.CustomException;
import com.tripass.saving.analysis.MissionReductionCalculator;
import com.tripass.saving.dto.CategorySelectionItemDto;
import com.tripass.saving.dto.MissionCategorySelectionDto;
import com.tripass.saving.dto.MissionOptionResponseDto;
import com.tripass.saving.dto.MissionSelectionRequestDto;
import com.tripass.saving.dto.MissionSelectionResponseDto;
import com.tripass.saving.dto.MonthlyCategoryAnalysisDto;
import com.tripass.saving.dto.MonthlySpendingAnalysisDto;
import com.tripass.saving.dto.ReductionRateOptionDto;
import com.tripass.saving.mapper.MissionCategorySelectionMapper;
import com.tripass.saving.mapper.MonthlySpendingAnalysisMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 절감 추천 TOP 3 카테고리에 대한 절감률(10/30/50%) 선택을 조회·저장한다.
 *
 * TOP 3 조회·소유권 검증은 {@link MonthlySpendingAnalysisMapper}(#210)를 그대로 재사용하고,
 * 이 클래스는 선택 검증과 {@link MissionCategorySelectionMapper} 저장만 책임진다.
 */
@Service
@Transactional(readOnly = true)
public class MissionCategorySelectionService {

    private final MonthlySpendingAnalysisMapper monthlySpendingAnalysisMapper;
    private final MissionCategorySelectionMapper missionCategorySelectionMapper;
    private final MissionReductionCalculator missionReductionCalculator;

    public MissionCategorySelectionService(
            MonthlySpendingAnalysisMapper monthlySpendingAnalysisMapper,
            MissionCategorySelectionMapper missionCategorySelectionMapper,
            MissionReductionCalculator missionReductionCalculator
    ) {
        this.monthlySpendingAnalysisMapper = monthlySpendingAnalysisMapper;
        this.missionCategorySelectionMapper = missionCategorySelectionMapper;
        this.missionReductionCalculator = missionReductionCalculator;
    }

    /** TOP 3 카테고리별로 10/30/50% 절감률 옵션과 예상 금액을 계산한다. 저장하지 않는다. */
    public List<MissionOptionResponseDto> getMissionOptions(Long userId, YearMonth analysisYearMonth) {
        Long monthlySpendingAnalysisId = resolveAnalysisId(userId, analysisYearMonth);
        List<MonthlyCategoryAnalysisDto> topCategories =
                monthlySpendingAnalysisMapper.findRecommendedCategoryAnalyses(monthlySpendingAnalysisId);

        return topCategories.stream()
                .map(category -> {
                    int baseline = toBaselineAmount(category.getMissionPeriodSpending());
                    List<ReductionRateOptionDto> options = missionReductionCalculator.calculateOptions(baseline);
                    return new MissionOptionResponseDto(
                            category.getCategoryId(), category.getCategoryCode(), category.getCategoryName(),
                            baseline, options);
                })
                .toList();
    }

    /** 저장된 선택 결과를 조회한다. */
    public List<MissionSelectionResponseDto> getMissionSelections(Long userId, YearMonth analysisYearMonth) {
        Long monthlySpendingAnalysisId = resolveAnalysisId(userId, analysisYearMonth);
        return buildSelectionResponses(monthlySpendingAnalysisId);
    }

    /**
     * 선택을 전체 교체 방식으로 저장한다. 요청에 없는 기존 선택은 삭제되므로,
     * 빈 selections를 보내면 전체 선택 해제가 된다.
     */
    @Transactional
    public List<MissionSelectionResponseDto> saveMissionSelections(
            Long userId, YearMonth analysisYearMonth, MissionSelectionRequestDto request
    ) {
        Long monthlySpendingAnalysisId = resolveAnalysisId(userId, analysisYearMonth);
        List<CategorySelectionItemDto> selections = request.getSelections();

        validateNoDuplicateCategories(selections);

        // 빈 selections(전체 선택 해제)면 TOP 3를 조회할 필요조차 없다.
        if (!selections.isEmpty()) {
            saveSelections(monthlySpendingAnalysisId, selections);
        }

        List<Long> requestedCategoryIds = selections.stream().map(CategorySelectionItemDto::getCategoryId).toList();
        missionCategorySelectionMapper.deleteSelectionsExcept(monthlySpendingAnalysisId, requestedCategoryIds);

        return buildSelectionResponses(monthlySpendingAnalysisId);
    }

    private void saveSelections(Long monthlySpendingAnalysisId, List<CategorySelectionItemDto> selections) {
        // TOP 3(기타 제외)만 선택 가능하다. 카테고리 개수가 원래 최대 3개라, 이 검증만으로
        // "최대 3개까지"도 자연히 지켜진다(TOP 3에 없는 4번째 카테고리는 애초에 통과할 수 없다).
        Map<Long, MonthlyCategoryAnalysisDto> topCategoriesById =
                monthlySpendingAnalysisMapper.findRecommendedCategoryAnalyses(monthlySpendingAnalysisId).stream()
                        .collect(Collectors.toMap(MonthlyCategoryAnalysisDto::getCategoryId, category -> category));

        for (CategorySelectionItemDto selection : selections) {
            MonthlyCategoryAnalysisDto topCategory = topCategoriesById.get(selection.getCategoryId());
            if (topCategory == null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "CATEGORY_NOT_RECOMMENDED",
                        "절감 추천 TOP 3에 포함된 카테고리만 선택할 수 있습니다.");
            }
            validateReductionRate(selection.getReductionRate());

            int baseline = toBaselineAmount(topCategory.getMissionPeriodSpending());
            ReductionRateOptionDto calculated =
                    missionReductionCalculator.calculate(baseline, selection.getReductionRate());

            missionCategorySelectionMapper.upsertSelection(
                    toSelectionDto(monthlySpendingAnalysisId, selection.getCategoryId(), baseline, calculated));
        }
    }

    private Long resolveAnalysisId(Long userId, YearMonth analysisYearMonth) {
        MonthlySpendingAnalysisDto analysis =
                monthlySpendingAnalysisMapper.findMonthlyAnalysis(userId, analysisYearMonth.toString());
        if (analysis == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "MONTHLY_ANALYSIS_NOT_FOUND", "월간 분석 리포트를 찾을 수 없습니다.");
        }
        return analysis.getId();
    }

    private void validateNoDuplicateCategories(List<CategorySelectionItemDto> selections) {
        long distinctCount = selections.stream().map(CategorySelectionItemDto::getCategoryId).distinct().count();
        if (distinctCount != selections.size()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "DUPLICATE_CATEGORY_SELECTION",
                    "같은 카테고리를 중복해서 선택할 수 없습니다.");
        }
    }

    private void validateReductionRate(Integer reductionRate) {
        if (reductionRate == null || !MissionReductionCalculator.AVAILABLE_REDUCTION_RATES.contains(reductionRate)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_REDUCTION_RATE",
                    "절감률은 10, 30, 50 중 하나여야 합니다.");
        }
    }

    private int toBaselineAmount(BigDecimal missionPeriodSpending) {
        return missionPeriodSpending.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private MissionCategorySelectionDto toSelectionDto(
            Long monthlySpendingAnalysisId, Long categoryId, int baseline, ReductionRateOptionDto calculated
    ) {
        MissionCategorySelectionDto dto = new MissionCategorySelectionDto();
        dto.setMonthlySpendingAnalysisId(monthlySpendingAnalysisId);
        dto.setCategoryId(categoryId);
        dto.setReductionRate(calculated.reductionRate());
        dto.setBaselineSpendingAmount(baseline);
        dto.setMonthlyReductionTarget(calculated.monthlyReductionTarget());
        dto.setMonthlyUsageTarget(calculated.monthlyUsageTarget());
        return dto;
    }

    private List<MissionSelectionResponseDto> buildSelectionResponses(Long monthlySpendingAnalysisId) {
        return missionCategorySelectionMapper.findSelections(monthlySpendingAnalysisId).stream()
                .map(this::toSelectionResponse)
                .toList();
    }

    private MissionSelectionResponseDto toSelectionResponse(MissionCategorySelectionDto dto) {
        // 주간 금액은 저장돼 있지 않으므로, 저장된 월 단위 금액·절감률로 다시 계산해서 채운다.
        ReductionRateOptionDto recalculated =
                missionReductionCalculator.calculate(dto.getBaselineSpendingAmount(), dto.getReductionRate());
        return new MissionSelectionResponseDto(
                dto.getCategoryId(), dto.getCategoryCode(), dto.getCategoryName(),
                dto.getReductionRate(), dto.getBaselineSpendingAmount(),
                dto.getMonthlyReductionTarget(), dto.getMonthlyUsageTarget(),
                recalculated.weeklyUsageLimit(), recalculated.weeklyExpectedSaving());
    }
}
