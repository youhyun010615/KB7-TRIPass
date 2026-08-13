import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ── AUTH (담당: 송형진) ─────────────────────────────────
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/signup',
      name: 'Signup',
      component: () => import('@/views/auth/SignupView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/find-id',
      name: 'FindId',
      component: () => import('@/views/auth/FindIdView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/find-password',
      name: 'FindPassword',
      component: () => import('@/views/auth/FindPasswordView.vue'),
      meta: { requiresAuth: false },
    },

    // ── HOME / SAV (담당: 권유현) ───────────────────────────
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/savings',
      name: 'Savings',
      component: () => import('@/views/savings/SavingsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/missions',
      name: 'SavingsMissions',
      component: () => import('@/views/savings/SavingsMissionView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/wallet',
      name: 'TripWallet',
      component: () => import('@/views/savings/TripWalletView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/savings/plan',
      name: 'SavingsPlanSetup',
      component: () => import('@/views/savings/SavingsPlanSetupView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/savings/accounts',
      name: 'SavingsAccounts',
      component: () => import('@/views/savings/SavingsAccountsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/savings/monthly',
      name: 'MonthlyFund',
      component: () => import('@/views/savings/MonthlyFundView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/savings/monthly/categories',
      name: 'MonthlyFundCategoryGoals',
      component: () => import('@/views/savings/CategoryGoalsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/savings/monthly/categories/:categoryId',
      name: 'MonthlyFundCategoryDetail',
      component: () => import('@/views/savings/CategoryDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/savings/monthly/transactions/:transactionId',
      name: 'MonthlyFundTransactionDetail',
      component: () =>
        import('@/views/savings/MonthlyTransactionDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/savings/monthly/transactions/:transactionId/category',
      name: 'MonthlyFundCategorySelect',
      component: () => import('@/views/savings/CategorySelectView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/savings/monthly/prepaid/new',
      name: 'MonthlyFundPrepaidNew',
      component: () => import('@/views/savings/PrepaidExpenseView.vue'),
      meta: { requiresAuth: true },
    },

    // ── PRO (담당: 송형진) ──────────────────────────────────
    {
      path: '/profile/financial',
      name: 'FinancialProfile',
      component: () => import('@/views/financial/FinancialProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/financial-schedule',
      name: 'FinancialSchedule',
      component: () => import('@/views/financial/FinancialScheduleView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/financial-schedule/calendar',
      name: 'FinancialScheduleCalendar',
      component: () =>
        import('@/views/financial/FinancialScheduleCalendarView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/financial-schedule/:eventId',
      name: 'FinancialScheduleDetail',
      component: () =>
        import('@/views/financial/FinancialScheduleDetailView.vue'),
      meta: { requiresAuth: true },
    },

    // ── GDS (담당: 송형진) ──────────────────────────────────
    {
      path: '/financial',
      name: 'Financial',
      component: () => import('@/views/financial/FinancialView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/financial/savings',
      name: 'FinancialSavings',
      component: () => import('@/views/financial/FinancialSavingsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/financial/savings/:productId',
      name: 'FinancialSavingsDetail',
      component: () =>
        import('@/views/financial/FinancialSavingsDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/financial/cards',
      name: 'FinancialCards',
      component: () => import('@/views/financial/FinancialCardsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/financial/cards/compare',
      name: 'FinancialCardsCompare',
      component: () =>
        import('@/views/financial/FinancialCardsCompareView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/financial/cards/:productId',
      name: 'FinancialCardDetail',
      component: () => import('@/views/financial/FinancialCardDetailView.vue'),
      meta: { requiresAuth: true },
    },

    // ── AST (담당: 이아영) ──────────────────────────────────
    {
      path: '/asset',
      name: 'Asset',
      component: () => import('@/views/asset/AssetView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/asset/accounts/:accountId',
      name: 'AssetAccountTransactions',
      component: () => import('@/views/asset/AccountTransactionsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/asset/transactions',
      name: 'AssetTransactions',
      component: () => import('@/views/asset/TransactionListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/asset/transactions/calendar',
      name: 'AssetTransactionCalendar',
      component: () => import('@/views/asset/TransactionCalendarView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/asset/transactions/:transactionId',
      name: 'AssetTransactionDetail',
      component: () => import('@/views/asset/TransactionDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/asset/fixed-expenses',
      name: 'AssetFixedExpenses',
      component: () => import('@/views/asset/FixedExpenseListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/asset/fixed-expenses/new',
      name: 'AssetFixedExpenseNew',
      component: () => import('@/views/asset/FixedExpenseFormView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/asset/fixed-expenses/:fixedExpenseId',
      name: 'AssetFixedExpenseDetail',
      component: () => import('@/views/asset/FixedExpenseDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/asset/prepaid',
      name: 'AssetPrepaidExpenses',
      component: () => import('@/views/asset/PrepaidExpenseListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/asset/prepaid/:prepaidExpenseId',
      name: 'AssetPrepaidExpenseDetail',
      component: () => import('@/views/asset/PrepaidExpenseDetailView.vue'),
      meta: { requiresAuth: true },
    },

    // ── FXC (담당: 권원영) ──────────────────────────────────
    {
      path: '/exchange',
      name: 'Exchange',
      component: () => import('@/views/exchange/ExchangeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/exchange/currencies',
      name: 'CurrencyList',
      component: () => import('@/views/exchange/CurrencyListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/exchange/alerts',
      name: 'ExchangeAlerts',
      component: () => import('@/views/exchange/ExchangeAlertsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/exchange/alerts/new',
      name: 'ExchangeAlertNew',
      component: () => import('@/views/exchange/ExchangeAlertFormView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/exchange/alerts/:alertId',
      name: 'ExchangeAlertEdit',
      component: () => import('@/views/exchange/ExchangeAlertFormView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/exchange/banks/:bankId',
      name: 'ExchangeBankDetail',
      component: () => import('@/views/exchange/BankDetailView.vue'),
      meta: { requiresAuth: true },
    },

    // ── TRV / BUD (담당: 권원영) ────────────────────────────
    {
      path: '/travel/register',
      name: 'TravelRegister',
      component: () => import('@/views/travel/TravelRegisterView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/travel/register/schedule',
      name: 'TravelRegisterSchedule',
      component: () => import('@/views/travel/TravelRegisterView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/travel/funds',
      name: 'TravelFundCheck',
      component: () => import('@/views/travel/TravelFundCheckView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/travel/funds/categories/:categoryId',
      name: 'TravelFundCategoryDetail',
      component: () =>
        import('@/views/travel/TravelFundCategoryDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/travel/funds/transactions/:transactionId',
      name: 'TravelFundTransactionDetail',
      component: () =>
        import('@/views/travel/TravelFundTransactionDetailView.vue'),
      meta: { requiresAuth: true },
    },

    // ── SCH (담당: 홍유진) ──────────────────────────────────
    {
      path: '/schedule',
      name: 'Schedule',
      component: () => import('@/views/schedule/ScheduleView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/schedule/new',
      name: 'ScheduleNew',
      component: () => import('@/views/schedule/ScheduleFormView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/schedule/notifications',
      name: 'ScheduleNotifications',
      component: () => import('@/views/schedule/ScheduleNotificationView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/schedule/:scheduleId/edit',
      name: 'ScheduleEdit',
      component: () => import('@/views/schedule/ScheduleFormView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/schedule/:scheduleId',
      name: 'ScheduleDetail',
      component: () => import('@/views/schedule/ScheduleDetailView.vue'),
      meta: { requiresAuth: true },
    },

    // ── OCR / EXP (담당: 홍유진) ────────────────────────────
    {
      path: '/trips/:tripId/receipts',
      name: 'Receipt',
      component: () =>
          import('@/views/receipt/ReceiptView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/trips/:tripId/receipts/capture',
      name: 'ReceiptCapture',
      component: () =>
          import('@/views/receipt/ReceiptCaptureView.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/trips/:tripId/receipts/new',
      name: 'ReceiptManualNew',
      component: () =>
          import('@/views/receipt/ReceiptResultView.vue'),
      meta: {
        requiresAuth: true,
        receiptMode: 'manual',
      },
    },
    {
      path: '/trips/:tripId/receipts/ocr-result',
      name: 'ReceiptOcrResult',
      component: () =>
          import('@/views/receipt/ReceiptResultView.vue'),
      meta: {
        requiresAuth: true,
        receiptMode: 'ocr',
      },
    },
    {
      path: '/trips/:tripId/receipts/:receiptId/edit',
      name: 'ReceiptEdit',
      component: () =>
          import('@/views/receipt/ReceiptResultView.vue'),
      meta: {
        requiresAuth: true,
        receiptMode: 'edit',
      },
    },
    {
      path: '/trips/:tripId/receipts/:receiptId',
      name: 'ReceiptDetail',
      component: () =>
          import('@/views/receipt/ReceiptResultView.vue'),
      meta: {
        requiresAuth: true,
        receiptMode: 'detail',
      },
    },

    // ── MYP (담당: 권유현) ──────────────────────────────────
    {
      path: '/mypage',
      name: 'Mypage',
      component: () => import('@/views/mypage/MypageView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/profile',
      name: 'MypageProfile',
      component: () => import('@/views/mypage/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/profile/edit',
      name: 'MypageProfileEdit',
      redirect: '/mypage/password',
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/password',
      name: 'MypagePassword',
      component: () => import('@/views/mypage/PasswordChangeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/financial-profile',
      name: 'MypageFinancialProfile',
      component: () => import('@/views/mypage/FinancialProfileDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/financial-profile/edit',
      name: 'MypageFinancialProfileEdit',
      component: () => import('@/views/mypage/FinancialProfileEditView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/travel',
      name: 'MypageTravel',
      component: () => import('@/views/mypage/TravelManageView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/travel/:id',
      name: 'MypageTravelDetail',
      component: () => import('@/views/mypage/TravelDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/reports',
      name: 'MypageReports',
      component: () => import('@/views/mypage/TravelReportListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/reports/pre-trip',
      name: 'MypagePreTripReport',
      component: () => import('@/views/mypage/PreTripReportView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/reports/post-trip',
      name: 'MypagePostTripReport',
      component: () => import('@/views/mypage/PostTripReportView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/checklists',
      name: 'MypageChecklists',
      component: () => import('@/views/mypage/checklist/ChecklistListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/checklists/preparation',
      name: 'MypagePreparationChecklist',
      component: () =>
        import('@/views/mypage/checklist/PreparationChecklistView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/checklists/return',
      name: 'MypageReturnChecklist',
      component: () =>
        import('@/views/mypage/checklist/ReturnChecklistView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/notification',
      name: 'MypageNotification',
      component: () => import('@/views/mypage/NotificationView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/notifications',
      name: 'NotificationInbox',
      component: () => import('@/views/mypage/NotificationInboxView.vue'),
      meta: { requiresAuth: true },
    },
  ],
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'Login' };
  }

  // 로그인 후 금융 프로필 미완료 시 등록 페이지로 강제 이동
  if (
    authStore.isLoggedIn &&
    !authStore.isProfileComplete &&
    to.name !== 'FinancialProfile' &&
    to.meta.requiresAuth
  ) {
    return { name: 'FinancialProfile' };
  }
});

export default router;
