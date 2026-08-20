import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { requestFcmToken } from '@/api/firebase';
import { registerFcmToken } from '@/api/notification';
import { useTravelModeStore } from './travelMode';
import { useTravelStore } from './travel';

export const useAuthStore = defineStore('auth', () => {
  // Access Token은 브라우저 메모리에서만 관리한다.
  const accessToken = ref(null);
  // 사용자 공개 정보는 새로고침 후에도 화면에 표시하기 위해 저장한다.
  const user = ref(JSON.parse(localStorage.getItem('tripass-user') || 'null'));
  const isProfileComplete = ref(
    localStorage.getItem('isProfileComplete') === 'true',
  );
  // null은 아직 계좌 연동 여부를 확인하지 않은 상태를 의미한다.
  const hasLinkedAccount = ref(null);

  const isLoggedIn = computed(() => Boolean(accessToken.value));

  // 새 Access Token을 메모리에 저장한다.
  function setToken(token) {
    accessToken.value = token;
    // 기존 버전에서 저장했던 Access Token을 제거한다.
    localStorage.removeItem('accessToken');
  }

  // 로그인한 회원의 공개 정보를 저장한다.
  function setUser(userInfo) {
    user.value = userInfo;
    localStorage.setItem('tripass-user', JSON.stringify(userInfo));
  }

  // 로그인 성공 후 처리 (FCM 토큰 등록 포함)
  async function handleLoginSuccess(token, userInfo) {
    clearAllCache();
    useTravelModeStore().resetForNewSession();
    useTravelStore().resetGoal();
    setToken(token);
    setUser(userInfo);
    hasLinkedAccount.value = null;

    // FCM 토큰 등록
    try {
      const fcmToken = await requestFcmToken();
      if (fcmToken) {
        await registerFcmToken(fcmToken);
        console.log('FCM 토큰이 서버에 등록되었습니다.');
      } else {
        console.log('FCM 토큰을 획득하지 못했습니다.');
      }
    } catch (error) {
      console.error('FCM 토큰 등록 실패:', error);
    }
  }

  // 기존 회원 정보의 일부를 변경한다.
  function updateUser(userInfo) {
    setUser({ ...user.value, ...userInfo });
  }

  function completeProfile() {
    isProfileComplete.value = true;
    localStorage.setItem('isProfileComplete', 'true');
  }

  function resetProfileCompletion() {
    isProfileComplete.value = false;
    localStorage.removeItem('isProfileComplete');
  }

  function setHasLinkedAccount(linked) {
    hasLinkedAccount.value = Boolean(linked);
  }

  function clearAllCache() {
    const keysToRemove = [];
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && (key.startsWith('tripass') || key === 'accessToken'
        || key === 'isProfileComplete' || key === 'travelMode'
        || key === 'travelModeDestination' || key === 'travelModeCurrency')) {
        keysToRemove.push(key);
      }
    }
    keysToRemove.forEach((key) => localStorage.removeItem(key));
  }

  function logout() {
    accessToken.value = null;
    user.value = null;
    isProfileComplete.value = false;
    hasLinkedAccount.value = null;
    clearAllCache();
  }

  return {
    accessToken,
    user,
    isLoggedIn,
    isProfileComplete,
    hasLinkedAccount,
    setToken,
    setUser,
    handleLoginSuccess,
    updateUser,
    completeProfile,
    resetProfileCompletion,
    setHasLinkedAccount,
    logout,
  };
});
