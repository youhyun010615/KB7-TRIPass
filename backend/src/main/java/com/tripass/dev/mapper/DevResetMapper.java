package com.tripass.dev.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface DevResetMapper {

    void deleteWeeklySavingMissionsByUser(@Param("userId") Long userId);

    void deleteMonthlySavingMissionsByUser(@Param("userId") Long userId);

    void deleteMissionCategorySelectionsByUser(@Param("userId") Long userId);

    void deleteMonthlyCategoryAnalysesByUser(@Param("userId") Long userId);

    void deleteMonthlySpendingAnalysesByUser(@Param("userId") Long userId);

    void deleteCategoryBudgetsByUser(@Param("userId") Long userId);

    void deletePreExpensesByUser(@Param("userId") Long userId);

    void deleteReceiptItemsByUser(@Param("userId") Long userId);

    void deleteReceiptParticipantsByUser(@Param("userId") Long userId);

    void deleteReceiptsByUser(@Param("userId") Long userId);

    void deleteTransactionsByUser(@Param("userId") Long userId);

    void deleteWalletCardTopupByUser(@Param("userId") Long userId);

    void deleteWalletExchangeTransactionByUser(@Param("userId") Long userId);

    void deleteTravelCardLedgerByUser(@Param("userId") Long userId);

    void deleteTravelCardBalanceByUser(@Param("userId") Long userId);

    void deleteWalletTravelCardByUser(@Param("userId") Long userId);

    void deleteWalletAutoSavingLogsByUser(@Param("userId") Long userId);

    void deleteWalletAutoSavingRuleByUser(@Param("userId") Long userId);

    void deleteWalletLedgerByUser(@Param("userId") Long userId);

    void deleteWalletAccountByUser(@Param("userId") Long userId);

    void deleteWalletByUser(@Param("userId") Long userId);

    void deleteUserTravelCardsByUser(@Param("userId") Long userId);

    void deleteFixedExpensesByUser(@Param("userId") Long userId);

    void deleteIncomeSourcesByUser(@Param("userId") Long userId);

    void deleteFinancialSchedulesByUser(@Param("userId") Long userId);

    void deleteSavingPlansByUser(@Param("userId") Long userId);

    void deleteCardsByUser(@Param("userId") Long userId);

    void deleteAccountsByUser(@Param("userId") Long userId);

    void deleteCodefConnectedInstitutionsByUser(@Param("userId") Long userId);

    void deleteCodefConnectionsByUser(@Param("userId") Long userId);

    void deleteTripWalletsByUser(@Param("userId") Long userId);

    void deleteTripReportsByUser(@Param("userId") Long userId);

    void deleteTripSchedulesByUser(@Param("userId") Long userId);

    void deleteTripChecklistItemsByUser(@Param("userId") Long userId);

    void deleteTripBudgetRecommendationsByUser(@Param("userId") Long userId);

    void deleteTripCountriesByUser(@Param("userId") Long userId);

    void deleteTripsByUser(@Param("userId") Long userId);

    void deleteExchangeRateAlertsByUser(@Param("userId") Long userId);

    void deleteNotificationsByUser(@Param("userId") Long userId);

    void deleteWalletWithdrawRecipientByUser(@Param("userId") Long userId);

    void resetUserOnboarding(@Param("userId") Long userId);

    void deleteNonSeedWalletLedgerByUser(@Param("userId") Long userId);

    void deleteNonSeedWalletExchangeTransactionByUser(@Param("userId") Long userId);

    void deleteNonSeedWalletCardTopupByUser(@Param("userId") Long userId);

    void deleteNonSeedTravelCardLedgerByUser(@Param("userId") Long userId);

    void recalcTravelCardBalanceFromSeedLedger(@Param("userId") Long userId, @Param("virtualDate") LocalDate virtualDate);

    void recalcWalletBalanceFromSeedLedger(@Param("userId") Long userId, @Param("virtualDate") LocalDate virtualDate);

    void restoreAccountBalanceFromNonSeedWalletTransactions(@Param("userId") Long userId);

    void deleteNonSeedWalletAccountTransactions(@Param("userId") Long userId);
}
