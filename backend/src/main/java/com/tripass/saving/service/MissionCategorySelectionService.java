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
import java.util.function.Function;
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
                            category.getRecommendationRank(), category.getRecommendationReason(),
                            baseline, options);
                })
                .toList();
    }

    /** 저장된 선택 결과와 합계를 조회한다. */
    public MissionSelectionsResponseDto getMissionSelections(Long userId, YearMonth analysisYearMonth) {
        Long monthlySpendingAnalysisId = resolveAnalysisId(userId, analysisYearMonth);
        return buildSelectionsResponse(monthlySpendingAnalysisId);
    }

    /**
     * 선택을 전체 교체 방식으로 저장한다. 요청에 없는 기존 선택은 삭제되므로,
     * 빈 selections를 보내면 전체 선택 해제가 된다.
     */
    @Transactional
    public MissionSelectionsResponseDto saveMissionSelections(
            Long userId, YearMonth analysisYearMonth, MissionSelectionRequestDto request
    ) {
        validateTripStatusForMission(userId);
        Long monthlySpendingAnalysisId = resolveAnalysisId(userId, analysisYearMonth);
        // 같은 분석에 대한 동시 PUT이 upsert/delete 순서가 엇갈리며 교착되지 않도록 먼저 직렬화한다.
        missionCategorySelectionMapper.lockMonthlySpendingAnalysis(monthlySpendingAnalysisId);

        List<CategorySelectionItemDto> selections = request.getSelections();

        validateNoDuplicateCategories(selections);
        validateStartedSelectionsUnchanged(monthlySpendingAnalysisId, selections);

        // 빈 selections(전체 선택 해제)면 TOP 3를 조회할 필요조차 없다.
        if (!selections.isEmpty()) {
            saveSelections(monthlySpendingAnalysisId, selections);
        }

        List<Long> requestedCategoryIds = selections.stream().map(CategorySelectionItemDto::getCategoryId).toList();
        missionCategorySelectionMapper.deleteSelectionsExcept(monthlySpendingAnalysisId, requestedCategoryIds);

        return buildSelectionsResponse(monthlySpendingAnalysisId);
    }

    /**
     * 이미 생성된 미션의 선택값은 스냅샷과 연결돼 있으므로 유지한다.
     * 대신 아직 시작하지 않은 추천 카테고리는 같은 요청에 추가할 수 있다.
     */
    private void validateStartedSelectionsUnchanged(
            Long monthlySpendingAnalysisId, List<CategorySelectionItemDto> requestedSelections
    ) {
        Map<Long, CategorySelectionItemDto> requestedByCategory = requestedSelections.stream()
                .collect(Collectors.toMap(CategorySelectionItemDto::getCategoryId, Function.identity()));

        for (MissionCategorySelectionDto started :
                missionCategorySelectionMapper.findStartedSelections(monthlySpendingAnalysisId)) {
            CategorySelectionItemDto requested = requestedByCategory.get(started.getCategoryId());
            if (requested == null || !started.getReductionRate().equals(requested.getReductionRate())) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "MISSION_ALREADY_STARTED",
                        "이미 시작한 미션의 카테고리와 절감률은 변경할 수 없습니다.");
            }
        }
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

    private void validateTripStatusForMission(Long userId) {
        String status = missionCategorySelectionMapper.findActiveTripStatusByUserId(userId);
        if (status == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "TRIP_REQUIRED_FOR_MISSION",
                    "여행 계획을 등록해야 미션을 진행할 수 있어요.");
        }
        if (!"PLANNING".equals(status)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "MISSION_NOT_ALLOWED",
                    "여행 중이거나 종료된 여행에서는 미션을 선택할 수 없습니다.");
        }
        if (!Boolean.TRUE.equals(missionCategorySelectionMapper.isSavingsTrackingStartedForUser(userId))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "ACCOUNT_REQUIRED_FOR_MISSION",
                    "계좌를 등록해야 미션을 진행할 수 있어요.");
        }
        if (missionCategorySelectionMapper.countActiveCardsByUserId(userId) == 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "CARD_REQUIRED_FOR_MISSION",
                    "카드를 등록해야 소비 분석 기반 미션을 선택할 수 있어요.");
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

    private MissionSelectionsResponseDto buildSelectionsResponse(Long monthlySpendingAnalysisId) {
        List<MissionSelectionResponseDto> selections =
                missionCategorySelectionMapper.findSelections(monthlySpendingAnalysisId).stream()
                        .map(this::toSelectionResponse)
                        .toList();

        // 개별 값은 INT 컬럼이지만 합계는 int 범위를 넘을 수 있으니 long으로 더한다.
        long totalMonthlyReductionTarget = selections.stream()
                .mapToLong(MissionSelectionResponseDto::monthlyReductionTarget).sum();
        long totalWeeklyExpectedSaving = selections.stream()
                .mapToLong(MissionSelectionResponseDto::weeklyExpectedSaving).sum();

        return new MissionSelectionsResponseDto(
                selections.size(), totalMonthlyReductionTarget, totalWeeklyExpectedSaving, selections);
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
