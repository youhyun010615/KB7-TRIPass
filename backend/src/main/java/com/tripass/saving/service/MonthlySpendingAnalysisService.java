package com.tripass.saving.service;

import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.duplicate.DuplicateTransactionMatcher;
import com.tripass.common.exception.CustomException;
import com.tripass.saving.analysis.CategorizedSpending;
import com.tripass.saving.analysis.CategorySpendingStats;
import com.tripass.saving.analysis.CoachingMessageGenerator;
import com.tripass.saving.analysis.EligibilityResult;
import com.tripass.saving.analysis.MonthlyCategorySpendingCalculator;
import com.tripass.saving.analysis.RecommendationEligibilityFilter;
import com.tripass.saving.analysis.RecommendationEvidence;
import com.tripass.saving.analysis.RecommendationScoreCalculator;
import com.tripass.saving.analysis.SavingResultCalculator;
import com.tripass.saving.analysis.ScoredCandidate;
import com.tripass.saving.analysis.TopCategorySelector;
import com.tripass.saving.classification.ConsumptionCategoryCode;
import com.tripass.saving.dto.MonthlyAnalysisResponseDto;
import com.tripass.saving.dto.MonthlyCategoryAnalysisDto;
import com.tripass.saving.dto.MonthlySpendingAnalysisDto;
import com.tripass.saving.dto.RecommendedCategoryResponseDto;
import com.tripass.saving.dto.SavingResultResponseDto;
import com.tripass.saving.dto.SpendingCategoryResponseDto;
import com.tripass.saving.mapper.MonthlySpendingAnalysisMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 월간 AI 소비 분석 리포트 생성·조회·상태 전이를 오케스트레이션한다.
 *
 * 거래 원본 조회·저장은 {@link MonthlySpendingAnalysisMapper}, 중복 거래 제거는
 * {@link DuplicateTransactionMatcher}(#206), 집계·필터·점수·TOP 3·코칭 문구는
 * {@code com.tripass.saving.analysis} 패키지의 순수 계산기들에 위임하고,
 * 이 클래스는 그 결과를 조합해 저장하고 응답을 구성하는 역할만 한다.
 */
@Service
@Transactional(readOnly = true)
public class MonthlySpendingAnalysisService {

    private static final int MISSION_COMPARISON_MONTHS = 3;

    private final MonthlySpendingAnalysisMapper mapper;
    private final DuplicateTransactionMatcher duplicateTransactionMatcher;
    private final MonthlyCategorySpendingCalculator monthlyCategorySpendingCalculator;
    private final RecommendationEligibilityFilter recommendationEligibilityFilter;
    private final RecommendationScoreCalculator recommendationScoreCalculator;
    private final TopCategorySelector topCategorySelector;
    private final CoachingMessageGenerator coachingMessageGenerator;
    private final SavingResultCalculator savingResultCalculator;

    public MonthlySpendingAnalysisService(
            MonthlySpendingAnalysisMapper mapper,
            DuplicateTransactionMatcher duplicateTransactionMatcher,
            MonthlyCategorySpendingCalculator monthlyCategorySpendingCalculator,
            RecommendationEligibilityFilter recommendationEligibilityFilter,
            RecommendationScoreCalculator recommendationScoreCalculator,
            TopCategorySelector topCategorySelector,
            CoachingMessageGenerator coachingMessageGenerator,
            SavingResultCalculator savingResultCalculator
    ) {
        this.mapper = mapper;
        this.duplicateTransactionMatcher = duplicateTransactionMatcher;
        this.monthlyCategorySpendingCalculator = monthlyCategorySpendingCalculator;
        this.recommendationEligibilityFilter = recommendationEligibilityFilter;
        this.recommendationScoreCalculator = recommendationScoreCalculator;
        this.topCategorySelector = topCategorySelector;
        this.coachingMessageGenerator = coachingMessageGenerator;
        this.savingResultCalculator = savingResultCalculator;
    }

    public MonthlyAnalysisResponseDto getMonthlyAnalysis(Long userId, YearMonth analysisYearMonth) {
        return buildResponse(userId, analysisYearMonth);
    }

    @Transactional
    public void markReportViewed(Long userId, YearMonth analysisYearMonth) {
        ensureAnalysisExists(userId, analysisYearMonth);
        mapper.markReportViewed(userId, analysisYearMonth.toString());
    }

    @Transactional
    public void markReportClosed(Long userId, YearMonth analysisYearMonth) {
        ensureAnalysisExists(userId, analysisYearMonth);
        mapper.markReportClosed(userId, analysisYearMonth.toString());
    }

    /**
     * 분석월 리포트를 계산해 저장한다. 기존 리포트가 있으면 집계값만 재계산하고
     * 리포트 상태(PENDING/VIEWED/CLOSED)와 최초 저장된 저축 목표액은 그대로 유지한다.
     */
    @Transactional
    public MonthlyAnalysisResponseDto generateMonthlyAnalysis(Long userId, YearMonth analysisYearMonth) {
        YearMonth targetYearMonth = analysisYearMonth.plusMonths(1);

        // spending_categories에는 영수증·예산 기능이 함께 쓰는 LODGING/SIGHTSEEING(여행 카테고리)도 섞여 있어,
        // 사용자가 거래를 수동으로 그 카테고리로 지정하면 AI 저축 미션 7개 카테고리가 아닌 값이 category_id에 들어올 수 있다.
        // 월간 분석은 AI 소비 카테고리 7개만 대상으로 하므로 그 외 카테고리 거래는 집계·추천 전부에서 제외한다.
        Map<String, Long> consumptionCategoryIds = resolveConsumptionCategoryIds();
        Long otherCategoryId = consumptionCategoryIds.get(ConsumptionCategoryCode.OTHER.name());
        Set<Long> validCategoryIds = new HashSet<>(consumptionCategoryIds.values());

        LocalDate periodStart = analysisYearMonth.minusMonths(MISSION_COMPARISON_MONTHS).atDay(1);
        LocalDate periodEnd = analysisYearMonth.atEndOfMonth();

        List<CategorizedSpending> allSpending = collectDedupedSpending(userId, periodStart, periodEnd).stream()
                .filter(spending -> validCategoryIds.contains(spending.categoryId()))
                .toList();
        List<CategorizedSpending> analysisMonthSpending = spendingInMonth(allSpending, analysisYearMonth);

        Map<Long, BigDecimal> previousMonthTotalByCategory =
                buildPreviousMonthTotalByCategory(allSpending, analysisYearMonth);

        List<CategorySpendingStats> categoryStats = monthlyCategorySpendingCalculator.aggregate(
                analysisMonthSpending, analysisYearMonth, previousMonthTotalByCategory);

        BigDecimal totalSpending = categoryStats.stream()
                .map(CategorySpendingStats::totalSpending)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategorySpendingStats> sixCategoryStats = categoryStats.stream()
                .filter(stats -> !stats.categoryId().equals(otherCategoryId))
                .toList();

        Map<Long, EligibilityResult> eligibilityByCategory = sixCategoryStats.stream()
                .collect(Collectors.toMap(
                        CategorySpendingStats::categoryId,
                        recommendationEligibilityFilter::evaluate));

        List<CategorySpendingStats> eligibleCandidates = sixCategoryStats.stream()
                .filter(stats -> eligibilityByCategory.get(stats.categoryId()).eligible())
                .toList();

        BigDecimal sixCategoryTotalMissionSpending = sixCategoryStats.stream()
                .map(CategorySpendingStats::missionPeriodSpending)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Long> sixCategoryIds = sixCategoryStats.stream().map(CategorySpendingStats::categoryId).toList();
        Map<Long, List<BigDecimal>> previousThreeMonthsMissionSpending = buildPreviousThreeMonthsMissionSpending(
                allSpending, analysisYearMonth, sixCategoryIds);

        List<ScoredCandidate> scoredCandidates = recommendationScoreCalculator.calculate(
                eligibleCandidates, sixCategoryTotalMissionSpending, previousThreeMonthsMissionSpending);

        Map<Long, CategorySpendingStats> statsById = categoryStats.stream()
                .collect(Collectors.toMap(CategorySpendingStats::categoryId, stats -> stats));

        List<Long> topCategoryIds = topCategorySelector.selectTopCategoryIds(scoredCandidates, statsById);

        Map<Long, ScoredCandidate> scoredById = scoredCandidates.stream()
                .collect(Collectors.toMap(ScoredCandidate::categoryId, candidate -> candidate));

        MonthlySpendingAnalysisDto existing = mapper.findMonthlyAnalysis(userId, analysisYearMonth.toString());

        // saving_plans에는 월별 이력이 없으므로, 최초 생성 시에만 현재 활성 목표를 새로 조회하고
        // 재계산 시에는 최초 저장된 목표를 그대로 유지한다(그렇지 않으면 목표 변경이 과거 리포트에 소급 적용된다).
        BigDecimal savingTargetAmount = existing != null
                ? existing.getSavingTargetAmount()
                : mapper.findActiveSavingTargetAmount(userId);

        // 실제 저축액을 산출할 TRIP 월렛 거래 원장이 아직 없으므로 항상 null(UNAVAILABLE)로 저장한다(후속 이슈).
        SavingResultResponseDto savingResult = savingResultCalculator.calculate(savingTargetAmount, null);

        Long monthlySpendingAnalysisId = saveMonthlyAnalysis(
                userId, analysisYearMonth, targetYearMonth, totalSpending, savingResult, existing);

        saveCategoryAnalyses(monthlySpendingAnalysisId, categoryStats, otherCategoryId,
                eligibilityByCategory, scoredById, topCategoryIds);

        return buildResponse(userId, analysisYearMonth);
    }

    /** AI 저축 미션 7개 소비 카테고리(코드 -> id)만 조회한다. LODGING/SIGHTSEEING 등 다른 용도 카테고리는 포함하지 않는다. */
    private Map<String, Long> resolveConsumptionCategoryIds() {
        Map<String, Long> categoryIds = new HashMap<>();
        for (ConsumptionCategoryCode code : ConsumptionCategoryCode.values()) {
            Long categoryId = mapper.findCategoryIdByCode(code.name());
            if (categoryId == null) {
                throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "CATEGORY_NOT_FOUND",
                        "소비 카테고리를 찾을 수 없습니다: " + code.name());
            }
            categoryIds.put(code.name(), categoryId);
        }
        return categoryIds;
    }

    // ===== 거래 수집·중복 제거 =====

    private List<CategorizedSpending> collectDedupedSpending(Long userId, LocalDate periodStart, LocalDate periodEnd) {
        List<TransactionDto> accountTransactions =
                mapper.findAccountWithdrawalTransactions(userId, periodStart, periodEnd);
        List<TransactionDto> checkCardTransactions =
                mapper.findCheckCardWithdrawalTransactions(userId, periodStart, periodEnd);
        List<TransactionDto> creditCardTransactions =
                mapper.findCreditCardWithdrawalTransactions(userId, periodStart, periodEnd);

        Set<Long> duplicateAccountTransactionIds = duplicateTransactionMatcher.findDuplicateAccountTransactionIds(
                accountTransactions, checkCardTransactions);
        List<TransactionDto> dedupedAccountTransactions = accountTransactions.stream()
                .filter(transaction -> !duplicateAccountTransactionIds.contains(transaction.getId()))
                .toList();

        return Stream.of(dedupedAccountTransactions, checkCardTransactions, creditCardTransactions)
                .flatMap(List::stream)
                .map(this::toCategorizedSpending)
                .toList();
    }

    private CategorizedSpending toCategorizedSpending(TransactionDto transaction) {
        return new CategorizedSpending(
                transaction.getCategoryId(), transaction.getTransactionDate(), transaction.getAmount());
    }

    private List<CategorizedSpending> spendingInMonth(List<CategorizedSpending> spendings, YearMonth month) {
        return spendings.stream()
                .filter(spending -> YearMonth.from(spending.transactionDate()).equals(month))
                .toList();
    }

    // ===== 이전 달 비교 데이터 =====
    // CODEF 연동은 연동 시점부터가 아니라 과거 거래내역을 소급 조회하므로, 연동일이 아니라 실제 거래
    // 존재 여부로 "그 달이 조회 가능했는지"를 판단한다. 그 달에 거래가 하나도 없으면 수집 여부를
    // 알 수 없으므로 평균에서 제외하고, 다른 카테고리 거래는 있는데 특정 카테고리만 없으면 0원으로 포함한다.
    // 실제 CODEF 조회 범위를 저장해 정확히 판단하는 건 후속 개선으로 분리한다.

    private boolean hasAnySpendingInMonth(List<CategorizedSpending> allSpending, YearMonth month) {
        return allSpending.stream()
                .anyMatch(spending -> YearMonth.from(spending.transactionDate()).equals(month));
    }

    /** 전월(1개월 전) 대비 증감률 계산용, 전체 기간(1일~말일) 카테고리별 총지출. */
    private Map<Long, BigDecimal> buildPreviousMonthTotalByCategory(
            List<CategorizedSpending> allSpending, YearMonth analysisYearMonth
    ) {
        YearMonth previousMonth = analysisYearMonth.minusMonths(1);
        if (!hasAnySpendingInMonth(allSpending, previousMonth)) {
            return Map.of();
        }
        List<CategorySpendingStats> previousMonthStats = monthlyCategorySpendingCalculator.aggregate(
                spendingInMonth(allSpending, previousMonth), previousMonth, Map.of());
        return previousMonthStats.stream()
                .collect(Collectors.toMap(CategorySpendingStats::categoryId, CategorySpendingStats::totalSpending));
    }

    /** 최근 3개월 평균 대비 증가율 계산용, 조회 가능한 달들의 미션기간(1~28일) 카테고리별 지출 목록. */
    private Map<Long, List<BigDecimal>> buildPreviousThreeMonthsMissionSpending(
            List<CategorizedSpending> allSpending, YearMonth analysisYearMonth, List<Long> categoryIds
    ) {
        Map<Long, List<BigDecimal>> result = new HashMap<>();
        for (int monthsAgo = 1; monthsAgo <= MISSION_COMPARISON_MONTHS; monthsAgo++) {
            YearMonth month = analysisYearMonth.minusMonths(monthsAgo);
            if (!hasAnySpendingInMonth(allSpending, month)) {
                continue;
            }
            List<CategorySpendingStats> monthStats = monthlyCategorySpendingCalculator.aggregate(
                    spendingInMonth(allSpending, month), month, Map.of());
            Map<Long, BigDecimal> missionSpendingByCategory = monthStats.stream()
                    .collect(Collectors.toMap(CategorySpendingStats::categoryId, CategorySpendingStats::missionPeriodSpending));

            for (Long categoryId : categoryIds) {
                result.computeIfAbsent(categoryId, key -> new ArrayList<>())
                        .add(missionSpendingByCategory.getOrDefault(categoryId, BigDecimal.ZERO));
            }
        }
        return result;
    }

    // ===== 저장 =====

    private Long saveMonthlyAnalysis(
            Long userId, YearMonth analysisYearMonth, YearMonth targetYearMonth, BigDecimal totalSpending,
            SavingResultResponseDto savingResult, MonthlySpendingAnalysisDto existing
    ) {
        MonthlySpendingAnalysisDto dto = new MonthlySpendingAnalysisDto();
        dto.setUserId(userId);
        dto.setAnalysisYearMonth(analysisYearMonth.toString());
        dto.setTargetYearMonth(targetYearMonth.toString());
        dto.setTotalSpending(roundToWon(totalSpending));
        // saving_target_amount는 saving_plans(다른 도메인 테이블)에서 오므로 소수 단위가 섞여 있을 수 있다.
        // KRW 금액 컬럼은 INT라 저장 전에 반드시 원 단위로 반올림해야 한다.
        dto.setSavingTargetAmount(roundToWon(savingResult.targetAmount()));
        dto.setActualSavingAmount(roundToWon(savingResult.actualAmount()));
        dto.setSavingDifferenceAmount(roundToWon(savingResult.differenceAmount()));
        dto.setSavingResultMessage(savingResult.resultMessage());

        if (existing == null) {
            mapper.insertMonthlyAnalysis(dto);
            return dto.getId();
        }

        dto.setId(existing.getId());
        int updated = mapper.updateMonthlyAnalysisPreservingStatus(dto);
        if (updated == 0) {
            throw new CustomException(HttpStatus.NOT_FOUND, "MONTHLY_ANALYSIS_NOT_FOUND", "월간 분석 리포트를 찾을 수 없습니다.");
        }
        return existing.getId();
    }

    private void saveCategoryAnalyses(
            Long monthlySpendingAnalysisId, List<CategorySpendingStats> categoryStats, Long otherCategoryId,
            Map<Long, EligibilityResult> eligibilityByCategory, Map<Long, ScoredCandidate> scoredById,
            List<Long> topCategoryIds
    ) {
        mapper.deleteCategoryAnalyses(monthlySpendingAnalysisId);

        for (CategorySpendingStats stats : categoryStats) {
            MonthlyCategoryAnalysisDto dto = toCategoryAnalysisDto(monthlySpendingAnalysisId, stats);

            if (!stats.categoryId().equals(otherCategoryId)) {
                EligibilityResult eligibility = eligibilityByCategory.get(stats.categoryId());
                dto.setRecommendationEligible(eligibility.eligible());
                dto.setExclusionReason(eligibility.exclusionReason());

                ScoredCandidate scored = scoredById.get(stats.categoryId());
                if (scored != null) {
                    dto.setSpendingShareScore(scored.spendingShareScore());
                    dto.setIncreaseScore(scored.increaseScore());
                    dto.setAmountRankScore(scored.amountRankScore());
                    dto.setRecommendationScore(scored.recommendationScore());

                    int topIndex = topCategoryIds.indexOf(stats.categoryId());
                    if (topIndex >= 0) {
                        dto.setRecommendationRank(topIndex + 1);
                        RecommendationEvidence evidence = RecommendationEvidence.from(scored, stats);
                        dto.setRecommendationReason(coachingMessageGenerator.generateReason(scored, evidence));
                    }
                }
            }

            mapper.insertCategoryAnalysis(dto);
        }
    }

    private MonthlyCategoryAnalysisDto toCategoryAnalysisDto(Long monthlySpendingAnalysisId, CategorySpendingStats stats) {
        MonthlyCategoryAnalysisDto dto = new MonthlyCategoryAnalysisDto();
        dto.setMonthlySpendingAnalysisId(monthlySpendingAnalysisId);
        dto.setCategoryId(stats.categoryId());
        dto.setSpendingAmount(roundToWon(stats.totalSpending()));
        dto.setSpendingRatio(stats.spendingRatio());
        dto.setTransactionCount(stats.transactionCount());
        dto.setWeeklyAverage(roundToWon(stats.weeklyAverage()));
        dto.setDailyAverage(roundToWon(stats.dailyAverage()));
        dto.setPreviousMonthChange(stats.previousMonthChange());
        dto.setSpendingRank(stats.spendingRank());
        dto.setMissionPeriodSpending(roundToWon(stats.missionPeriodSpending()));
        dto.setMissionTransactionCount(stats.missionTransactionCount());
        dto.setRecommendationEligible(false);
        return dto;
    }

    /** KRW 금액 컬럼은 DB에서 INT라 저장 전 원 단위로 반올림해야 한다(소수 값을 그대로 넣으면 안 된다). */
    private BigDecimal roundToWon(BigDecimal amount) {
        return amount == null ? null : amount.setScale(0, RoundingMode.HALF_UP);
    }

    // ===== 응답 조립 =====

    private void ensureAnalysisExists(Long userId, YearMonth analysisYearMonth) {
        if (mapper.findMonthlyAnalysis(userId, analysisYearMonth.toString()) == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "MONTHLY_ANALYSIS_NOT_FOUND", "월간 분석 리포트를 찾을 수 없습니다.");
        }
    }

    private MonthlyAnalysisResponseDto buildResponse(Long userId, YearMonth analysisYearMonth) {
        MonthlySpendingAnalysisDto analysis = mapper.findMonthlyAnalysis(userId, analysisYearMonth.toString());
        if (analysis == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "MONTHLY_ANALYSIS_NOT_FOUND", "월간 분석 리포트를 찾을 수 없습니다.");
        }

        List<MonthlyCategoryAnalysisDto> categoryRows = mapper.findCategoryAnalyses(analysis.getId());
        List<MonthlyCategoryAnalysisDto> recommendedRows = mapper.findRecommendedCategoryAnalyses(analysis.getId());

        SavingResultResponseDto savingResult = savingResultCalculator.calculate(
                analysis.getSavingTargetAmount(), analysis.getActualSavingAmount());

        List<SpendingCategoryResponseDto> spendingCategories = categoryRows.stream()
                .map(category -> new SpendingCategoryResponseDto(
                        category.getSpendingRank(), category.getCategoryCode(), category.getCategoryName(),
                        category.getSpendingAmount(), category.getSpendingRatio(), category.getTransactionCount()))
                .toList();

        List<RecommendedCategoryResponseDto> recommendedCategories = recommendedRows.stream()
                .map(category -> new RecommendedCategoryResponseDto(
                        category.getRecommendationRank(), category.getCategoryCode(), category.getCategoryName(),
                        category.getRecommendationReason()))
                .toList();

        String coachingSummary = coachingMessageGenerator.generateSummary(
                recommendedRows.stream().map(MonthlyCategoryAnalysisDto::getCategoryName).toList());

        return new MonthlyAnalysisResponseDto(
                analysis.getAnalysisYearMonth(),
                analysis.getTargetYearMonth(),
                analysis.getReportStatus(),
                analysis.getTotalSpending(),
                savingResult,
                spendingCategories,
                coachingSummary,
                recommendedCategories
        );
    }
}
