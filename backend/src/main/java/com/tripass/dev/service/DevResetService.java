package com.tripass.dev.service;

import com.tripass.dev.mapper.DevResetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DevResetService {

    private final DevResetMapper devResetMapper;

    @Transactional
    public void resetAccount(Long userId) {
        log.info("[DEV] 계정 데이터 초기화 시작 - userId: {}", userId);

        devResetMapper.deleteWeeklySavingMissionsByUser(userId);
        devResetMapper.deleteMonthlySavingMissionsByUser(userId);
        devResetMapper.deleteMissionCategorySelectionsByUser(userId);
        devResetMapper.deleteMonthlyCategoryAnalysesByUser(userId);
        devResetMapper.deleteMonthlySpendingAnalysesByUser(userId);
        devResetMapper.deleteCategoryBudgetsByUser(userId);

        devResetMapper.deletePreExpensesByUser(userId);
        devResetMapper.deleteReceiptItemsByUser(userId);
        devResetMapper.deleteReceiptParticipantsByUser(userId);
        devResetMapper.deleteReceiptsByUser(userId);
        devResetMapper.deleteTransactionsByUser(userId);

        devResetMapper.deleteWalletCardTopupByUser(userId);
        devResetMapper.deleteWalletExchangeTransactionByUser(userId);
        devResetMapper.deleteTravelCardLedgerByUser(userId);
        devResetMapper.deleteTravelCardBalanceByUser(userId);
        devResetMapper.deleteWalletTravelCardByUser(userId);
        devResetMapper.deleteWalletAutoSavingLogsByUser(userId);
        devResetMapper.deleteWalletAutoSavingRuleByUser(userId);
        devResetMapper.deleteWalletLedgerByUser(userId);
        devResetMapper.deleteWalletAccountByUser(userId);
        devResetMapper.deleteWalletByUser(userId);

        devResetMapper.deleteUserTravelCardsByUser(userId);
        devResetMapper.deleteFinancialSchedulesByUser(userId);
        devResetMapper.deleteFixedExpensesByUser(userId);
        devResetMapper.deleteIncomeSourcesByUser(userId);
        devResetMapper.deleteSavingPlansByUser(userId);

        devResetMapper.deleteCardsByUser(userId);
        devResetMapper.deleteAccountsByUser(userId);
        devResetMapper.deleteCodefConnectedInstitutionsByUser(userId);
        devResetMapper.deleteCodefConnectionsByUser(userId);

        devResetMapper.deleteTripWalletsByUser(userId);
        devResetMapper.deleteTripReportsByUser(userId);
        devResetMapper.deleteTripSchedulesByUser(userId);
        devResetMapper.deleteTripChecklistItemsByUser(userId);
        devResetMapper.deleteTripBudgetRecommendationsByUser(userId);
        devResetMapper.deleteTripCountriesByUser(userId);
        devResetMapper.deleteTripsByUser(userId);

        devResetMapper.deleteExchangeRateAlertsByUser(userId);
        devResetMapper.deleteNotificationsByUser(userId);
        devResetMapper.deleteWalletWithdrawRecipientByUser(userId);

        devResetMapper.resetUserOnboarding(userId);

        log.info("[DEV] 계정 데이터 초기화 완료 - userId: {}", userId);
    }
}
