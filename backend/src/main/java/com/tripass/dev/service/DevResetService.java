package com.tripass.dev.service;

import com.tripass.dev.mapper.DevResetMapper;
import com.tripass.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DevResetService {

    private final DevResetMapper devResetMapper;
    private final WalletService walletService;

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
        // 삭제 직후 새 월렛을 다시 만들어둔다. 그렇지 않으면 첫 충전/출금 등 쓰기 작업
        // 전까지는 월렛 행이 없는 상태라, 홈/월렛 화면의 조회 API가 지갑을 못 찾고
        // "월렛을 찾을 수 없습니다" 오류를 낸다(가입 직후에는 회원가입 로직이 항상
        // 월렛을 만들어주므로 이 상태가 재현되지 않는다).
        walletService.createWalletForUser(userId);

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

    @Transactional
    public void resetDemoState(Long userId, LocalDate virtualDate) {
        log.info("[DEV] 데모 상태 리셋 시작 - userId: {}, virtualDate: {}", userId, virtualDate);

        devResetMapper.deleteNonSeedWalletCardTopupByUser(userId);
        devResetMapper.deleteNonSeedWalletExchangeTransactionByUser(userId);
        devResetMapper.deleteNonSeedTravelCardLedgerByUser(userId);
        devResetMapper.recalcTravelCardBalanceFromSeedLedger(userId, virtualDate);
        devResetMapper.restoreAccountBalanceFromNonSeedWalletTransactions(userId);
        devResetMapper.deleteNonSeedWalletAccountTransactions(userId);
        devResetMapper.deleteNonSeedWalletLedgerByUser(userId);
        devResetMapper.recalcWalletBalanceFromSeedLedger(userId, virtualDate);

        log.info("[DEV] 데모 상태 리셋 완료 - userId: {}", userId);
    }
}
