package com.tripass.saving.mapper;

import com.tripass.asset.dto.TransactionDto;
import com.tripass.saving.dto.MonthlyCategoryAnalysisDto;
import com.tripass.saving.dto.MonthlySpendingAnalysisDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 월간 AI 소비 분석 리포트의 원본 데이터 조회와 결과 저장만 담당한다.
 * 중복 거래 제거·집계·필터·점수 계산·TOP 3 선정·코칭 문구 생성은 Service 계층의 책임이다.
 */
@Mapper
public interface MonthlySpendingAnalysisMapper {

    // ===== 분석용 거래 원본 조회 =====
    // 계좌/체크카드/신용카드를 SQL에서 미리 구분해 반환한다. 체크카드 거래는 계좌 출금과의
    // 중복 매칭(DuplicateTransactionMatcher, #206) 대상이고, 신용카드 거래는 매칭 대상이 아니다.

    List<TransactionDto> findAccountWithdrawalTransactions(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<TransactionDto> findCheckCardWithdrawalTransactions(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<TransactionDto> findCreditCardWithdrawalTransactions(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /** 카테고리 코드(예: "OTHER")로 spending_categories.id를 조회한다. */
    Long findCategoryIdByCode(@Param("categoryCode") String categoryCode);

    // ===== 저축 목표 =====

    /**
     * 사용자의 활성 여행(PLANNING/TRAVELING)에 연결된 저축 계획의 "현재" 월 저축 목표액. 없으면 null.
     *
     * saving_plans에는 월별 이력이 없고 현재 값만 존재하므로, 이 메서드는 리포트를 처음 생성할 때만
     * 사용해야 한다. 기존 리포트를 재계산할 때는 이 값으로 saving_target_amount를 덮어쓰지 말고
     * {@link #findMonthlyAnalysis}로 조회한 기존 값을 그대로 유지해야 한다(그렇지 않으면 목표 금액이
     * 이후 변경될 때 과거 리포트의 목표까지 소급 변경되어 버린다).
     */
    BigDecimal findActiveSavingTargetAmount(@Param("userId") Long userId);

    /**
     * 분석 월의 월렛 순저축액(IN - OUT)을 조회한다.
     * 월렛 원장이 없는 달은 실제 저축액 0원으로 반환하며, 잔액 보정(ADJUST)은 저축에서 제외한다.
     */
    BigDecimal findActualSavingAmount(
            @Param("userId") Long userId,
            @Param("analysisYearMonth") String analysisYearMonth
    );

    // ===== 월간 분석 저장·조회 =====

    MonthlySpendingAnalysisDto findMonthlyAnalysis(
            @Param("userId") Long userId,
            @Param("analysisYearMonth") String analysisYearMonth
    );

    void insertMonthlyAnalysis(MonthlySpendingAnalysisDto dto);

    /**
     * 재계산 시 집계값만 갱신하고 리포트 상태(report_status/report_viewed_at/report_closed_at)는
     * 건드리지 않는다. id와 userId가 모두 일치해야 갱신되므로(dto.userId 필수), Service의 다른 버그로
     * 잘못된 분석 ID가 전달되더라도 다른 사용자의 리포트를 수정하지 못한다.
     *
     * @return 갱신된 행 수(0이면 id·userId 불일치 등으로 대상이 없었다는 뜻이므로 Service에서 감지해야 한다)
     */
    int updateMonthlyAnalysisPreservingStatus(MonthlySpendingAnalysisDto dto);

    /** 이전 버전 리포트의 저축 결과만 보정하고 소비·카테고리 분석은 유지한다. */
    int updateSavingResult(MonthlySpendingAnalysisDto dto);

    // ===== 카테고리 분석 저장·조회 =====

    void deleteCategoryAnalyses(@Param("monthlySpendingAnalysisId") Long monthlySpendingAnalysisId);

    void insertCategoryAnalysis(MonthlyCategoryAnalysisDto dto);

    /** 소비 순위(전체 카테고리, 기타 포함) 순으로 정렬해 반환한다. */
    List<MonthlyCategoryAnalysisDto> findCategoryAnalyses(@Param("monthlySpendingAnalysisId") Long monthlySpendingAnalysisId);

    /** 절감 추천 순위(TOP 3, 기타 제외)가 매겨진 카테고리만 순위순으로 반환한다. */
    List<MonthlyCategoryAnalysisDto> findRecommendedCategoryAnalyses(@Param("monthlySpendingAnalysisId") Long monthlySpendingAnalysisId);

    // ===== 리포트 상태 전이 =====
    // PENDING -> VIEWED -> CLOSED 방향으로만 전이한다. 조건에 맞지 않으면(이미 전이됐거나 CLOSED)
    // 0건 갱신되며 오류를 던지지 않는다(동일 API 재호출에 대한 멱등 처리).

    int markReportViewed(@Param("userId") Long userId, @Param("analysisYearMonth") String analysisYearMonth);

    int markReportClosed(@Param("userId") Long userId, @Param("analysisYearMonth") String analysisYearMonth);

    List<MonthlyCategoryAnalysisDto> findCategoryAnalysisHistory(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("months") int months
    );

    List<java.util.Map<String, Object>> findWeeklyBreakdown(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<java.util.Map<String, Object>> findTopMerchants(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit
    );

    java.util.Map<String, Object> findPeakSpendingDay(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<java.util.Map<String, Object>> findMonthlySpendingTrend(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
