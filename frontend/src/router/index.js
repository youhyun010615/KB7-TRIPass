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

    // ── PRO (담당: 송형진) ──────────────────────────────────
    {
      path: '/profile/financial',
      name: 'FinancialProfile',
      component: () => import('@/views/financial/FinancialProfileView.vue'),
      meta: { requiresAuth: true },
    },

    // ── GDS (담당: 송형진) ──────────────────────────────────
    {
      path: '/financial',
      name: 'Financial',
      component: () => import('@/views/financial/FinancialView.vue'),
      meta: { requiresAuth: true },
    },

    // ── AST (담당: 이아영) ──────────────────────────────────
    {
      path: '/asset',
      name: 'Asset',
      component: () => import('@/views/asset/AssetView.vue'),
      meta: { requiresAuth: true },
    },

    // ── FXC (담당: 권원영) ──────────────────────────────────
    {
      path: '/exchange',
      name: 'Exchange',
      component: () => import('@/views/exchange/ExchangeView.vue'),
      meta: { requiresAuth: true },
    },

    // ── TRV / BUD (담당: 권원영) ────────────────────────────
    {
      path: '/travel/register',
      name: 'TravelRegister',
      component: () => import('@/views/travel/TravelRegisterView.vue'),
      meta: { requiresAuth: true },
    },

    // ── SCH (담당: 홍유진) ──────────────────────────────────
    {
      path: '/schedule',
      name: 'Schedule',
      component: () => import('@/views/schedule/ScheduleView.vue'),
      meta: { requiresAuth: true },
    },

    // ── OCR / EXP (담당: 홍유진) ────────────────────────────
    {
      path: '/receipt',
      name: 'Receipt',
      component: () => import('@/views/receipt/ReceiptView.vue'),
      meta: { requiresAuth: true },
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
      component: () => import('@/views/mypage/ProfileEditView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/password',
      name: 'MypagePassword',
      component: () => import('@/views/mypage/PasswordChangeView.vue'),
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
      path: '/mypage/checklists',
      name: 'MypageChecklists',
      component: () => import('@/views/mypage/ChecklistListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mypage/notification',
      name: 'MypageNotification',
      component: () => import('@/views/mypage/NotificationView.vue'),
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
