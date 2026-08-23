package com.tripass.saving.service;

import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.duplicate.DuplicateTransactionMatcher;
import com.tripass.common.exception.CustomException;
import com.tripass.saving.analysis.CoachingMessageGenerator;
import com.tripass.saving.analysis.MonthlyCategorySpendingCalculator;
import com.tripass.saving.analysis.RecommendationEligibilityFilter;
import com.tripass.saving.analysis.RecommendationScoreCalculator;
import com.tripass.saving.analysis.SavingResultCalculator;
import com.tripass.saving.analysis.TopCategorySelector;
import com.tripass.saving.dto.MonthlyAnalysisResponseDto;
import com.tripass.saving.dto.MonthlyCategoryAnalysisDto;
import com.tripass.saving.dto.MonthlySpendingAnalysisDto;
import com.tripass.saving.dto.SavingResultStatus;
import com.tripass.saving.mapper.MonthlySpendingAnalysisMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlySpendingAnalysisServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long FOOD_ID = 1L;
    private static final Long CAFE_ID = 2L;
    private static final Long LIVING_ID = 3L;
    private static final Long SHOPPING_ID = 4L;
    private static final Long LEISURE_ID = 5L;
    private static final Long TRANSPORT_ID = 6L;
    private static final Long OTHER_ID = 7L;
    // spending_categories에는 영수증·예산 기능이 쓰는 여행 카테고리도 섞여 있다(AI 저축 미션 7개 카테고리가 아님).
    private static final Long LODGING_ID = 100L;
    private static final Long SIGHTSEEING_ID = 101L;
    private static final YearMonth ANALYSIS_MONTH = YearMonth.of(2026, 7);

    @Mock
    private MonthlySpendingAnalysisMapper mapper;

    private MonthlySpendingAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new MonthlySpendingAnalysisService(
                mapper,
                new DuplicateTransactionMatcher(),
                new MonthlyCategorySpendingCalculator(),
                new RecommendationEligibilityFilter(),
                new RecommendationScoreCalculator(),
                new TopCategorySelector(),
                new CoachingMessageGenerator(),
                new SavingResultCalculator()
        );
    }

    // ===== generateMonthlyAnalysis =====

    @Test
    @DisplayName("정상 분석 생성 시 카테고리별 집계와 TOP3 추천 순위가 저장된다")
    void generate_savesAggregatesAndTopRecommendations() {
        stubFirstCreation(new BigDecimal("700000"));
        when(mapper.findActualSavingAmount(USER_ID, ANALYSIS_MONTH.toString()))
                .thenReturn(new BigDecimal("500000"));

        List<TransactionDto> accountTxns = List.of(
                txn(1L, FOOD_ID, LocalDate.of(2026, 7, 1), "20000"),
                txn(2L, FOOD_ID, LocalDate.of(2026, 7, 2), "20000"),
                txn(3L, FOOD_ID, LocalDate.of(2026, 7, 3), "20000"),
                txn(4L, FOOD_ID, LocalDate.of(2026, 7, 4), "20000"),
                txn(5L, FOOD_ID, LocalDate.of(2026, 7, 5), "20000"),
                txn(6L, FOOD_ID, LocalDate.of(2026, 7, 6), "20000"),
                txn(7L, CAFE_ID, LocalDate.of(2026, 7, 1), "15000"),
                txn(8L, CAFE_ID, LocalDate.of(2026, 7, 2), "15000"),
                txn(9L, CAFE_ID, LocalDate.of(2026, 7, 3), "15000"),
                txn(10L, CAFE_ID, LocalDate.of(2026, 7, 4), "15000"),
                txn(11L, CAFE_ID, LocalDate.of(2026, 7, 5), "15000"),
                txn(12L, CAFE_ID, LocalDate.of(2026, 7, 6), "15000"),
                txn(13L, SHOPPING_ID, LocalDate.of(2026, 7, 1), "20000"),
                txn(14L, SHOPPING_ID, LocalDate.of(2026, 7, 2), "20000"), // 5건 미만 -> 필터 탈락
                txn(15L, OTHER_ID, LocalDate.of(2026, 7, 10), "50000")
        );
        when(mapper.findAccountWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(accountTxns);
        when(mapper.findCheckCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCreditCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());

        service.generateMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        List<MonthlyCategoryAnalysisDto> saved = captureInsertedCategories();
        MonthlyCategoryAnalysisDto food = byCategory(saved, FOOD_ID);
        MonthlyCategoryAnalysisDto cafe = byCategory(saved, CAFE_ID);
        MonthlyCategoryAnalysisDto shopping = byCategory(saved, SHOPPING_ID);
        MonthlyCategoryAnalysisDto other = byCategory(saved, OTHER_ID);

        assertEquals(new BigDecimal("120000"), food.getSpendingAmount());
        assertEquals(new BigDecimal("90000"), cafe.getSpendingAmount());

        // FOOD(120000)이 CAFE(90000)보다 지출이 커서 지출비율·순위 점수가 모두 앞서므로 1위여야 한다
        assertEquals(1, food.getRecommendationRank());
        assertEquals(2, cafe.getRecommendationRank());
        assertTrue(food.getRecommendationEligible());
        assertTrue(cafe.getRecommendationEligible());

        // SHOPPING은 거래 5건 미만이라 추천 후보에서 탈락한다
        assertFalse(shopping.getRecommendationEligible());
        assertEquals("MIN_TRANSACTION_COUNT_NOT_MET", shopping.getExclusionReason());
        assertNull(shopping.getRecommendationRank());

        // 기타는 추천 로직 대상이 아니므로 점수·순위·필터 결과가 전혀 채워지지 않는다
        assertFalse(other.getRecommendationEligible());
        assertNull(other.getExclusionReason());
        assertNull(other.getRecommendationRank());
        assertNull(other.getRecommendationScore());

        MonthlySpendingAnalysisDto analysis = captureInsertedAnalysis();
        assertEquals(new BigDecimal("300000"), analysis.getTotalSpending()); // 120000+90000+40000+50000
        assertEquals(new BigDecimal("500000"), analysis.getActualSavingAmount());
        assertEquals(new BigDecimal("-200000"), analysis.getSavingDifferenceAmount());
        assertEquals("목표보다 200,000원 덜 저축했어요.", analysis.getSavingResultMessage());
    }

    @Test
    @DisplayName("체크카드 결제와 매칭되는 계좌 출금은 중복 제거되고 카드 쪽만 남는다")
    void generate_excludesDuplicateAccountTransaction() {
        stubFirstCreation(null);

        TransactionDto accountTxn = txnWithTime(201L, TRANSPORT_ID,
                LocalDate.of(2026, 7, 1), LocalTime.of(12, 0, 0), "10000");
        TransactionDto checkCardTxn = txnWithTime(301L, TRANSPORT_ID,
                LocalDate.of(2026, 7, 1), LocalTime.of(12, 2, 0), "10000");

        when(mapper.findAccountWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of(accountTxn));
        when(mapper.findCheckCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of(checkCardTxn));
        when(mapper.findCreditCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());

        service.generateMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        MonthlyCategoryAnalysisDto transport = byCategory(captureInsertedCategories(), TRANSPORT_ID);
        assertEquals(new BigDecimal("10000"), transport.getSpendingAmount());
        assertEquals(1, transport.getTransactionCount());
    }

    @Test
    @DisplayName("거래가 없는 사용자는 카테고리 결과 없이 총지출 0으로 저장된다")
    void generate_noTransactions_savesZeroSpendingAndNoCategories() {
        stubFirstCreation(null);

        when(mapper.findAccountWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCheckCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCreditCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());

        service.generateMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        assertEquals(new BigDecimal("0"), captureInsertedAnalysis().getTotalSpending());
        verify(mapper, never()).insertCategoryAnalysis(any());
    }

    @Test
    @DisplayName("모든 후보가 필터에서 탈락하면 추천 순위가 하나도 매겨지지 않는다")
    void generate_noEligibleCandidates_noRecommendationRank() {
        stubFirstCreation(null);

        // 두 카테고리 모두 거래 3건뿐이라 최소 거래 횟수(5건) 미달
        List<TransactionDto> accountTxns = List.of(
                txn(1L, FOOD_ID, LocalDate.of(2026, 7, 1), "20000"),
                txn(2L, FOOD_ID, LocalDate.of(2026, 7, 2), "20000"),
                txn(3L, FOOD_ID, LocalDate.of(2026, 7, 3), "20000"),
                txn(4L, CAFE_ID, LocalDate.of(2026, 7, 1), "15000"),
                txn(5L, CAFE_ID, LocalDate.of(2026, 7, 2), "15000"),
                txn(6L, CAFE_ID, LocalDate.of(2026, 7, 3), "15000")
        );
        when(mapper.findAccountWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(accountTxns);
        when(mapper.findCheckCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCreditCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());

        service.generateMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        List<MonthlyCategoryAnalysisDto> saved = captureInsertedCategories();
        assertTrue(saved.stream().noneMatch(dto -> dto.getRecommendationRank() != null));
        assertTrue(saved.stream().noneMatch(MonthlyCategoryAnalysisDto::getRecommendationEligible));
    }

    @Test
    @DisplayName("과거 월에 전체 거래가 하나도 없으면 최근 3개월 평균에서 제외한다")
    void generate_excludesMonthWithNoSpendingAtAll() {
        // CODEF는 연동 시점부터가 아니라 과거 거래내역을 소급 조회하므로, 연동일이 아니라
        // "그 달에 거래가 실제로 존재하는지"로 비교 가능 여부를 판단해야 한다.
        stubFirstCreation(null);

        List<TransactionDto> julyTxns = fiveEvenTransactions(FOOD_ID, LocalDate.of(2026, 7, 1), "11000"); // 55000원
        List<TransactionDto> aprilTxns = List.of(txn(90L, FOOD_ID, LocalDate.of(2026, 4, 5), "60000"));
        // 5월은 어떤 카테고리든 거래가 전혀 없음 -> 조회 가능 여부를 알 수 없으므로 평균에서 제외해야 한다
        List<TransactionDto> juneTxns = List.of(txn(91L, CAFE_ID, LocalDate.of(2026, 6, 5), "10000")); // FOOD 아님

        when(mapper.findAccountWithdrawalTransactions(eq(USER_ID), any(), any()))
                .thenReturn(concat(julyTxns, aprilTxns, juneTxns));
        when(mapper.findCheckCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCreditCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());

        service.generateMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        MonthlyCategoryAnalysisDto food = byCategory(captureInsertedCategories(), FOOD_ID);
        // 5월을 제외하면(6월엔 FOOD 거래가 없어 0원 포함, 4월엔 60000) 평균=(60000+0)/2=30000
        // -> 증가율 (55000-30000)/30000 = 0.8333
        // 5월을 잘못 0원으로 포함해 3으로 나눴다면 평균=20000, 증가율=1.75->1.0000으로 캡핑되어 값이 달라진다
        assertEquals(new BigDecimal("0.8333"), food.getIncreaseScore());
    }

    @Test
    @DisplayName("과거 월에 다른 카테고리 거래가 있으면 조회 가능한 달로 보고 해당 카테고리는 0원으로 포함한다")
    void generate_includesZeroSpendingForCollectedMonth() {
        stubFirstCreation(null);

        List<TransactionDto> julyTxns = fiveEvenTransactions(FOOD_ID, LocalDate.of(2026, 7, 1), "18000"); // 90000원
        List<TransactionDto> aprilTxns = List.of(txn(90L, FOOD_ID, LocalDate.of(2026, 4, 5), "60000"));
        // 5월은 FOOD 거래는 없지만 다른 카테고리 거래가 있어 "조회 가능한 달"로 간주되고, FOOD는 0원으로 포함되어야 한다
        List<TransactionDto> mayTxns = List.of(txn(92L, CAFE_ID, LocalDate.of(2026, 5, 5), "5000"));
        List<TransactionDto> juneTxns = List.of(txn(91L, FOOD_ID, LocalDate.of(2026, 6, 5), "60000"));

        when(mapper.findAccountWithdrawalTransactions(eq(USER_ID), any(), any()))
                .thenReturn(concat(julyTxns, aprilTxns, mayTxns, juneTxns));
        when(mapper.findCheckCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCreditCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());

        service.generateMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        MonthlyCategoryAnalysisDto food = byCategory(captureInsertedCategories(), FOOD_ID);
        // 평균 = (60000+0+60000)/3 = 40000, 증가율 = (90000-40000)/40000 = 1.25 -> 1.0000으로 캡핑
        // 5월을 평균에서 뺐다면 평균은 60000이 되어 증가율이 0.5000으로 다르게 나온다
        assertEquals(new BigDecimal("1.0000"), food.getIncreaseScore());
    }

    @Test
    @DisplayName("최근에 연동했어도 과거 거래가 있으면 추천 후보가 된다")
    void generate_recentConnectionStillEligibleWithPastData() {
        // 연동일 개념 자체가 없어졌으므로(더 이상 findEarliestConnectionDate를 호출하지 않는다),
        // 미션 기준 거래·금액·단일거래 비중만 만족하면 항상 추천 후보가 될 수 있어야 한다.
        stubFirstCreation(null);

        List<TransactionDto> foodTxns = fiveEvenTransactions(FOOD_ID, LocalDate.of(2026, 7, 1), "18000"); // 90000원
        when(mapper.findAccountWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(foodTxns);
        when(mapper.findCheckCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCreditCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());

        service.generateMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        MonthlyCategoryAnalysisDto food = byCategory(captureInsertedCategories(), FOOD_ID);
        assertTrue(food.getRecommendationEligible());
        assertNull(food.getExclusionReason());
        assertEquals(1, food.getRecommendationRank());
    }

    @Test
    @DisplayName("LODGING/SIGHTSEEING 거래는 소비 리포트와 추천 분모에서 제외된다")
    void generate_excludesTravelOnlyCategories() {
        stubFirstCreation(null);

        List<TransactionDto> foodTxns = fiveEvenTransactions(FOOD_ID, LocalDate.of(2026, 7, 1), "20000"); // 100000원
        // LODGING/SIGHTSEEING은 AI 저축 미션 7개 카테고리가 아니라 영수증·예산 기능이 쓰는 여행 카테고리다.
        // 사용자가 거래를 수동으로 이 카테고리로 지정했더라도 월간 분석에는 포함되면 안 된다.
        List<TransactionDto> lodgingTxns = List.of(txn(300L, LODGING_ID, LocalDate.of(2026, 7, 2), "5000000"));
        List<TransactionDto> sightseeingTxns = List.of(txn(301L, SIGHTSEEING_ID, LocalDate.of(2026, 7, 3), "3000000"));

        when(mapper.findAccountWithdrawalTransactions(eq(USER_ID), any(), any()))
                .thenReturn(concat(foodTxns, lodgingTxns, sightseeingTxns));
        when(mapper.findCheckCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCreditCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());

        service.generateMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        List<MonthlyCategoryAnalysisDto> saved = captureInsertedCategories();
        assertTrue(saved.stream().noneMatch(dto -> dto.getCategoryId().equals(LODGING_ID)));
        assertTrue(saved.stream().noneMatch(dto -> dto.getCategoryId().equals(SIGHTSEEING_ID)));

        // 총지출·추천 분모에도 LODGING/SIGHTSEEING 금액이 섞이면 안 된다(100000원이어야지 8100000원이면 안 된다)
        assertEquals(new BigDecimal("100000"), captureInsertedAnalysis().getTotalSpending());

        MonthlyCategoryAnalysisDto food = byCategory(saved, FOOD_ID);
        assertEquals(new BigDecimal("1.0000"), food.getSpendingShareScore()); // 유일한 후보이므로 분모=자기 자신
    }

    @Test
    @DisplayName("재계산 시 기존 리포트 상태와 최초 저장된 저축 목표를 그대로 유지한다")
    void generate_recompute_preservesStatusAndSavingTarget() {
        MonthlySpendingAnalysisDto existing = new MonthlySpendingAnalysisDto();
        existing.setId(55L);
        existing.setUserId(USER_ID);
        existing.setSavingTargetAmount(new BigDecimal("700000")); // 최초 저장된 목표(현재 활성 목표와 다를 수 있음)
        existing.setReportStatus("VIEWED");
        existing.setReportViewedAt(LocalDateTime.of(2026, 8, 1, 9, 0));

        stubConsumptionCategoryIds();
        when(mapper.findMonthlyAnalysis(USER_ID, ANALYSIS_MONTH.toString())).thenReturn(existing);
        when(mapper.updateMonthlyAnalysisPreservingStatus(any())).thenReturn(1);
        when(mapper.findAccountWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCheckCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCreditCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());

        service.generateMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        // 재계산이므로 현재 활성 목표를 새로 조회하면 안 된다
        verify(mapper, never()).findActiveSavingTargetAmount(anyLong());
        verify(mapper, never()).insertMonthlyAnalysis(any());

        ArgumentCaptor<MonthlySpendingAnalysisDto> captor = ArgumentCaptor.forClass(MonthlySpendingAnalysisDto.class);
        verify(mapper).updateMonthlyAnalysisPreservingStatus(captor.capture());
        assertEquals(55L, captor.getValue().getId());
        assertEquals(new BigDecimal("700000"), captor.getValue().getSavingTargetAmount());
    }

    @Test
    @DisplayName("다른 사용자의 리포트를 잘못 갱신하려 하면(갱신 0건) 예외를 던진다")
    void generate_updateAffectsNoRows_throws() {
        MonthlySpendingAnalysisDto existing = new MonthlySpendingAnalysisDto();
        existing.setId(55L);
        existing.setUserId(USER_ID);

        stubConsumptionCategoryIds();
        when(mapper.findMonthlyAnalysis(USER_ID, ANALYSIS_MONTH.toString())).thenReturn(existing);
        when(mapper.updateMonthlyAnalysisPreservingStatus(any())).thenReturn(0);
        when(mapper.findAccountWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCheckCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(mapper.findCreditCardWithdrawalTransactions(eq(USER_ID), any(), any())).thenReturn(List.of());

        CustomException exception = assertThrows(CustomException.class,
                () -> service.generateMonthlyAnalysis(USER_ID, ANALYSIS_MONTH));
        assertEquals("MONTHLY_ANALYSIS_NOT_FOUND", exception.getErrorCode());
    }

    // ===== getMonthlyAnalysis =====

    @Test
    @DisplayName("이전 버전에서 실제 저축액 없이 저장된 리포트는 조회 시 다시 집계한다")
    void get_legacyReportWithoutActualSavingAmount_recalculates() {
        MonthlySpendingAnalysisDto analysis = new MonthlySpendingAnalysisDto();
        analysis.setId(1L);
        analysis.setAnalysisYearMonth("2026-07");
        analysis.setTargetYearMonth("2026-08");
        analysis.setTotalSpending(new BigDecimal("300000"));
        analysis.setSavingTargetAmount(new BigDecimal("700000"));
        analysis.setActualSavingAmount(null);
        analysis.setReportStatus("PENDING");

        when(mapper.findMonthlyAnalysis(USER_ID, "2026-07")).thenReturn(analysis);
        when(mapper.findActualSavingAmount(USER_ID, "2026-07")).thenReturn(new BigDecimal("500000"));
        when(mapper.updateSavingResult(any())).thenAnswer(invocation -> {
            MonthlySpendingAnalysisDto updated = invocation.getArgument(0);
            analysis.setActualSavingAmount(updated.getActualSavingAmount());
            analysis.setSavingDifferenceAmount(updated.getSavingDifferenceAmount());
            analysis.setSavingResultMessage(updated.getSavingResultMessage());
            return 1;
        });
        when(mapper.findCategoryAnalyses(1L)).thenReturn(List.of());
        when(mapper.findRecommendedCategoryAnalyses(1L)).thenReturn(List.of());

        MonthlyAnalysisResponseDto response = service.getMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        assertEquals(SavingResultStatus.AVAILABLE, response.savingResult().status());
        assertEquals(new BigDecimal("500000"), response.savingResult().actualAmount());
        assertEquals(new BigDecimal("-200000"), response.savingResult().differenceAmount());
        verify(mapper, never()).deleteCategoryAnalyses(anyLong());
        verify(mapper, never()).updateMonthlyAnalysisPreservingStatus(any());
    }

    @Test
    @DisplayName("추천 카테고리 응답에는 근거 문구만 담기고 코칭 문구는 담기지 않는다")
    void get_recommendedCategories_containsOnlyReason() {
        MonthlySpendingAnalysisDto analysis = new MonthlySpendingAnalysisDto();
        analysis.setId(1L);
        analysis.setAnalysisYearMonth("2026-07");
        analysis.setTargetYearMonth("2026-08");
        analysis.setTotalSpending(new BigDecimal("300000"));

        MonthlyCategoryAnalysisDto recommended = new MonthlyCategoryAnalysisDto();
        recommended.setCategoryId(FOOD_ID);
        recommended.setCategoryCode("FOOD");
        recommended.setCategoryName("식비");
        recommended.setRecommendationRank(1);
        recommended.setRecommendationReason("최근 3개월 평균보다 소비가 35% 증가했어요.");
        recommended.setCoachingMessage(null);

        when(mapper.findMonthlyAnalysis(USER_ID, "2026-07")).thenReturn(analysis);
        when(mapper.findCategoryAnalyses(1L)).thenReturn(List.of());
        when(mapper.findRecommendedCategoryAnalyses(1L)).thenReturn(List.of(recommended));

        MonthlyAnalysisResponseDto response = service.getMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        assertEquals(1, response.recommendedCategories().size());
        assertEquals("최근 3개월 평균보다 소비가 35% 증가했어요.", response.recommendedCategories().get(0).recommendationReason());
        assertEquals("이번 달에는 식비 소비를 줄여보세요.", response.coachingSummary());
    }

    @Test
    @DisplayName("추천 후보가 없으면 빈 상태 문구를 반환한다")
    void get_noRecommendedCategories_returnsEmptyMessage() {
        MonthlySpendingAnalysisDto analysis = new MonthlySpendingAnalysisDto();
        analysis.setId(1L);
        analysis.setAnalysisYearMonth("2026-07");
        analysis.setTargetYearMonth("2026-08");
        analysis.setTotalSpending(new BigDecimal("300000"));

        when(mapper.findMonthlyAnalysis(USER_ID, "2026-07")).thenReturn(analysis);
        when(mapper.findCategoryAnalyses(1L)).thenReturn(List.of());
        when(mapper.findRecommendedCategoryAnalyses(1L)).thenReturn(List.of());

        MonthlyAnalysisResponseDto response = service.getMonthlyAnalysis(USER_ID, ANALYSIS_MONTH);

        assertTrue(response.recommendedCategories().isEmpty());
        assertEquals("이번 달에는 특별히 줄여야 할 소비 카테고리가 없어요.", response.coachingSummary());
    }

    @Test
    @DisplayName("존재하지 않는 리포트를 조회하면 404 예외를 던진다")
    void get_notFound_throws() {
        when(mapper.findMonthlyAnalysis(USER_ID, "2026-07")).thenReturn(null);

        assertThrows(CustomException.class, () -> service.getMonthlyAnalysis(USER_ID, ANALYSIS_MONTH));
    }

    // ===== 리포트 상태 전이 =====

    @Test
    @DisplayName("존재하는 리포트는 VIEWED로 전이한다")
    void markViewed_existing_updatesStatus() {
        when(mapper.findMonthlyAnalysis(USER_ID, "2026-07")).thenReturn(new MonthlySpendingAnalysisDto());

        service.markReportViewed(USER_ID, ANALYSIS_MONTH);

        verify(mapper).markReportViewed(USER_ID, "2026-07");
    }

    @Test
    @DisplayName("존재하는 리포트는 CLOSED로 전이한다")
    void markClosed_existing_updatesStatus() {
        when(mapper.findMonthlyAnalysis(USER_ID, "2026-07")).thenReturn(new MonthlySpendingAnalysisDto());

        service.markReportClosed(USER_ID, ANALYSIS_MONTH);

        verify(mapper).markReportClosed(USER_ID, "2026-07");
    }

    @Test
    @DisplayName("존재하지 않는 리포트를 열람 처리하려 하면 예외를 던지고 상태를 바꾸지 않는다")
    void markViewed_notFound_throwsAndDoesNotUpdate() {
        when(mapper.findMonthlyAnalysis(USER_ID, "2026-07")).thenReturn(null);

        assertThrows(CustomException.class, () -> service.markReportViewed(USER_ID, ANALYSIS_MONTH));
        verify(mapper, never()).markReportViewed(anyLong(), any());
    }

    // ===== 테스트 헬퍼 =====

    private void stubFirstCreation(BigDecimal savingTarget) {
        stubConsumptionCategoryIds();
        when(mapper.findMonthlyAnalysis(eq(USER_ID), any()))
                .thenReturn(null, new MonthlySpendingAnalysisDto());
        when(mapper.findActiveSavingTargetAmount(USER_ID)).thenReturn(savingTarget);
        doAnswer(invocation -> {
            MonthlySpendingAnalysisDto dto = invocation.getArgument(0);
            dto.setId(999L);
            return null;
        }).when(mapper).insertMonthlyAnalysis(any());
    }

    /** AI 저축 미션 7개 카테고리 코드->id 매핑을 스텁한다. LODGING/SIGHTSEEING 등은 의도적으로 포함하지 않는다. */
    private void stubConsumptionCategoryIds() {
        when(mapper.findCategoryIdByCode("FOOD")).thenReturn(FOOD_ID);
        when(mapper.findCategoryIdByCode("CAFE")).thenReturn(CAFE_ID);
        when(mapper.findCategoryIdByCode("SHOPPING")).thenReturn(SHOPPING_ID);
        when(mapper.findCategoryIdByCode("LIVING")).thenReturn(LIVING_ID);
        when(mapper.findCategoryIdByCode("TRANSPORT")).thenReturn(TRANSPORT_ID);
        when(mapper.findCategoryIdByCode("LEISURE")).thenReturn(LEISURE_ID);
        when(mapper.findCategoryIdByCode("OTHER")).thenReturn(OTHER_ID);
    }

    private List<MonthlyCategoryAnalysisDto> captureInsertedCategories() {
        ArgumentCaptor<MonthlyCategoryAnalysisDto> captor = ArgumentCaptor.forClass(MonthlyCategoryAnalysisDto.class);
        verify(mapper, times(1)).deleteCategoryAnalyses(999L);
        verify(mapper, org.mockito.Mockito.atLeastOnce()).insertCategoryAnalysis(captor.capture());
        return captor.getAllValues();
    }

    private MonthlySpendingAnalysisDto captureInsertedAnalysis() {
        ArgumentCaptor<MonthlySpendingAnalysisDto> captor = ArgumentCaptor.forClass(MonthlySpendingAnalysisDto.class);
        verify(mapper).insertMonthlyAnalysis(captor.capture());
        return captor.getValue();
    }

    private MonthlyCategoryAnalysisDto byCategory(List<MonthlyCategoryAnalysisDto> saved, Long categoryId) {
        return saved.stream()
                .filter(dto -> dto.getCategoryId().equals(categoryId))
                .findFirst()
                .orElseThrow();
    }

    private List<TransactionDto> fiveEvenTransactions(Long categoryId, LocalDate startDate, String amountEach) {
        return List.of(
                txn(201L, categoryId, startDate, amountEach),
                txn(202L, categoryId, startDate.plusDays(1), amountEach),
                txn(203L, categoryId, startDate.plusDays(2), amountEach),
                txn(204L, categoryId, startDate.plusDays(3), amountEach),
                txn(205L, categoryId, startDate.plusDays(4), amountEach)
        );
    }

    @SafeVarargs
    private List<TransactionDto> concat(List<TransactionDto>... lists) {
        return List.of(lists).stream().flatMap(List::stream).toList();
    }

    private TransactionDto txn(Long id, Long categoryId, LocalDate date, String amount) {
        return txnWithTime(id, categoryId, date, null, amount);
    }

    private TransactionDto txnWithTime(Long id, Long categoryId, LocalDate date, LocalTime time, String amount) {
        TransactionDto dto = new TransactionDto();
        dto.setId(id);
        dto.setCategoryId(categoryId);
        dto.setTransactionDate(date);
        dto.setTransactionTime(time);
        dto.setTransactionType("WITHDRAWAL");
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }
}
