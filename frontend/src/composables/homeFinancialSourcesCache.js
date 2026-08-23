import { ref } from 'vue';

// SavingsModeHome.vue는 다른 탭(환율 등)에 갔다가 돌아올 때마다 새로 마운트되어
// 컴포넌트 내부 ref가 기본값(0, false)으로 초기화된다. 연동 계좌/카드 수를 여기
// 모듈 스코프에 유지해두면, 재조회하는 동안에도 마지막으로 알고 있던 값을 그대로
// 보여줄 수 있어 "연동된 게 없음" 상태로 잠깐 돌아가 화면이 깜빡이는 것을 막는다.
export const linkedAccountCount = ref(0);
export const linkedCardCount = ref(0);
export const financialSourcesLoadedOnce = ref(false);
