package com.tripass.report.mapper;

import com.tripass.report.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ReportMapper {

    TripBasicRowDto findTripBasicInfo(Long tripId);

    List<String> findTripCountryNames(Long tripId);

    List<ReportCountryBudgetDto> findCountryBudgets(Long tripId);

    BigDecimal findWalletBalanceByUserId(Long userId);

    Long findWalletIdByUserId(Long userId);

    List<SavingHistoryDto> findRecentWalletLedger(@Param("walletId") Long walletId, @Param("limit") int limit);

    ChecklistProgressRowDto findChecklistProgress(@Param("tripId") Long tripId, @Param("checklistType") String checklistType);

    ScheduleCountRowDto findScheduleCounts(Long tripId);

    BigDecimal findTravelExpenseTotal(Long tripId);

    List<DailySpendingDto> findDailySpending(Long tripId);

    List<CategorySpendingDto> findCategorySpending(Long tripId);

    List<CountrySpendingDto> findCountrySpending(Long tripId);

    List<CountryTopCategoryDto> findCountryTopCategories(Long tripId);

    int findReceiptCount(Long tripId);
}
