<script setup>
import { computed } from 'vue';
import foodIcon from '@/assets/icons/food.svg';
import cafeIcon from '@/assets/icons/cafe.svg';
import shoppingIcon from '@/assets/icons/shopping-cart.svg';
import taxiIcon from '@/assets/icons/taxi.svg';
import leisureIcon from '@/assets/icons/hobby_drink.svg';

const props = defineProps({
  missionData: { type: Object, required: true },
});

defineEmits(['open']);

const categoryMeta = {
  FOOD: { icon: '🍽', iconSrc: foodIcon, color: '#2457aa' },
  CAFE: { icon: '☕', iconSrc: cafeIcon, color: '#8b5a2b' },
  LIVING: { icon: '🧺', color: '#13a184' },
  SHOPPING: { icon: '🛍', iconSrc: shoppingIcon, color: '#f59e0b' },
  HOBBY: { icon: '🎮', iconSrc: leisureIcon, color: '#8b5cf6' },
  TRANSPORT: { icon: '🚌', iconSrc: taxiIcon, color: '#0ea5e9' },
  OTHER: { icon: '•••', color: '#64748b' },
};

const missions = computed(() => props.missionData?.missions || []);
const monthLabel = computed(() => {
  const month = Number(props.missionData?.targetYearMonth?.split('-')[1]);
  return Number.isFinite(month) ? `${month}월` : '이번 달';
});
const rewardAmount = computed(() =>
  Number(props.missionData?.totalRewardAmount || 0),
);
const plannedAmount = computed(() =>
  Number(props.missionData?.totalPlannedSavingAmount || 0),
);
const rewardPercent = computed(() =>
  plannedAmount.value > 0
    ? Math.min(100, Math.round((rewardAmount.value / plannedAmount.value) * 100))
    : 0,
);

function metaOf(code) {
  return categoryMeta[code] || categoryMeta.OTHER;
}

function currentWeek(mission) {
  const weeks = mission.weeklyMissions || [];
  return (
    weeks.find((week) => week.status === 'IN_PROGRESS') ||
    weeks.find((week) => week.status === 'PENDING') ||
    weeks.at(-1) ||
    null
  );
}

function spendingPercent(mission) {
  const week = currentWeek(mission);
  const limit = Number(week?.weeklyUsageLimit || 0);
  return limit > 0
    ? Math.min(100, Math.round((Number(week?.actualSpending || 0) / limit) * 100))
    : 0;
}

function remainingLimit(mission) {
  const week = currentWeek(mission);
  return Math.max(
    0,
    Number(week?.weeklyUsageLimit || 0) - Number(week?.actualSpending || 0),
  );
}

function statusLabel(status) {
  return {
    PENDING: '예정',
    IN_PROGRESS: '진행 중',
    SUCCESS: '성공',
    FAILED: '실패',
    CANCELLED: '중단',
  }[status] || '진행 중';
}

function formatCurrency(value) {
  return `${Math.max(0, Number(value) || 0).toLocaleString('ko-KR')}원`;
}
</script>

<template>
  <section class="home-mission-card">
    <header>
      <div>
        <small>이달의 저축 미션</small>
        <h2>{{ monthLabel }} 저축 미션</h2>
        <p>선택한 소비 절감 목표를 실천하고 있어요.</p>
      </div>
      <button type="button" @click="$emit('open')">전체 보기 <b>›</b></button>
    </header>

    <div class="mission-saving-summary">
      <div>
        <small>미션으로 모은 금액</small>
        <strong>{{ formatCurrency(rewardAmount) }}</strong>
      </div>
      <div class="summary-progress">
        <span><b>{{ missions.length }}개</b> 미션 진행</span>
        <strong>{{ rewardPercent }}%</strong>
      </div>
      <div class="summary-bar"><i :style="{ width: `${rewardPercent}%` }" /></div>
      <p>목표 절약액 {{ formatCurrency(plannedAmount) }}</p>
    </div>

    <div class="mission-list">
      <article
        v-for="mission in missions"
        :key="mission.id"
        :style="{ '--mission-color': metaOf(mission.categoryCode).color }"
      >
        <span class="mission-icon">
          <img v-if="metaOf(mission.categoryCode).iconSrc" :src="metaOf(mission.categoryCode).iconSrc" alt="" />
          <template v-else>{{ metaOf(mission.categoryCode).icon }}</template>
        </span>
        <div class="mission-info">
          <div>
            <strong>{{ mission.categoryName }} 소비 줄이기</strong>
            <span>{{ statusLabel(currentWeek(mission)?.status || mission.status) }}</span>
          </div>
          <small>
            {{ currentWeek(mission)?.weekNumber || '-' }}주차 · 남은 한도
            {{ formatCurrency(remainingLimit(mission)) }}
          </small>
          <div class="mission-progress">
            <i :style="{ width: `${spendingPercent(mission)}%` }" />
          </div>
        </div>
      </article>
    </div>

    <button type="button" class="mission-open-button" @click="$emit('open')">
      <span>✦</span>
      <div>
        <small>AI SAVING COACH</small>
        <b>미션 현황 확인하고 추가 도전하기</b>
      </div>
      <i>›</i>
    </button>
  </section>
</template>

<style scoped>
.home-mission-card{position:relative;overflow:hidden;padding:19px;border:1px solid #bcd2ff;border-radius:24px;background:linear-gradient(150deg,#eff5ff 0%,#e6f0ff 100%);box-shadow:0 13px 30px #1a489a1f;color:#173f8d;animation:mission-enter .48s cubic-bezier(.22,1,.36,1) both}.home-mission-card::after{position:absolute;top:-65px;right:-58px;width:170px;height:170px;border-radius:50%;background:#5688e51a;content:''}.home-mission-card header{position:relative;z-index:1;display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.home-mission-card header small{color:#f06a2a;font-size:8px;font-weight:950;letter-spacing:.14em}.home-mission-card h2{margin-top:5px;font-size:20px;font-weight:950;letter-spacing:-.05em}.home-mission-card header p{margin-top:4px;color:#6f83a8;font-size:10px}.home-mission-card header button{flex:none;color:#286ce0;font-size:10px;font-weight:900}.home-mission-card header button b{font-size:15px}.mission-saving-summary{position:relative;z-index:1;margin-top:16px;padding:15px;border-radius:17px;background:linear-gradient(135deg,#173e8d,#2e6dd2);color:#fff}.mission-saving-summary>div:first-child small{display:block;color:#c6d8f8;font-size:9px}.mission-saving-summary>div:first-child strong{display:block;margin-top:3px;color:#ffd36a;font-size:22px;font-weight:950}.summary-progress{display:flex;justify-content:space-between;margin-top:12px;color:#d4e2fb;font-size:9px}.summary-progress b,.summary-progress strong{color:#fff}.summary-bar{height:7px;margin-top:7px;overflow:hidden;border-radius:99px;background:#ffffff2b}.summary-bar i{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#ff8547,#ffd45c);transition:width .55s ease}.mission-saving-summary>p{margin-top:6px;text-align:right;color:#bcd0f4;font-size:8px}.mission-list{position:relative;z-index:1;display:grid;gap:9px;margin-top:12px}.mission-list article{display:flex;align-items:center;gap:11px;padding:13px;border:1px solid #d5e1f5;border-radius:16px;background:#fff}.mission-icon{display:grid;flex:0 0 39px;height:39px;place-items:center;border-radius:13px;background:color-mix(in srgb,var(--mission-color) 12%,white);font-size:18px}.mission-icon img{width:19px;height:19px}.mission-info{min-width:0;flex:1}.mission-info>div:first-child{display:flex;align-items:center;justify-content:space-between;gap:8px}.mission-info strong{overflow:hidden;color:#233b68;font-size:12px;font-weight:900;text-overflow:ellipsis;white-space:nowrap}.mission-info>div:first-child span{flex:none;padding:4px 7px;border-radius:99px;background:#e2f7f1;color:#119278;font-size:8px;font-weight:900}.mission-info small{display:block;margin-top:4px;color:#7485a1;font-size:8px}.mission-progress{height:5px;margin-top:8px;overflow:hidden;border-radius:99px;background:#e8eef8}.mission-progress i{display:block;height:100%;border-radius:inherit;background:var(--mission-color);transition:width .55s ease}.mission-open-button{position:relative;z-index:1;display:flex;width:100%;align-items:center;gap:10px;margin-top:13px;padding:13px;border-radius:15px;background:#fff;color:#173f8d;text-align:left}.mission-open-button>span{display:grid;width:32px;height:32px;place-items:center;border-radius:11px;background:#e7efff;color:#2f6dd4}.mission-open-button>div{min-width:0;flex:1}.mission-open-button small{display:block;color:#f06a2a;font-size:7px;font-weight:950;letter-spacing:.12em}.mission-open-button b{display:block;margin-top:2px;font-size:10px;font-weight:900}.mission-open-button>i{font-size:20px;font-style:normal}@keyframes mission-enter{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}
</style>
