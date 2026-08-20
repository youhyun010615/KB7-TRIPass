<script setup>
import { computed } from 'vue';
import aiIcon from '@/assets/icons/ai.svg';
import foodIcon from '@/assets/icons/food.svg';
import cafeIcon from '@/assets/icons/cafe.svg';
import shoppingIcon from '@/assets/icons/shopping-cart.svg';
import taxiIcon from '@/assets/icons/taxi.svg';
import leisureIcon from '@/assets/icons/hobby_drink.svg';
import livingIcon from '@/assets/icons/home-dollar.svg';

const props = defineProps({
  missionData: { type: Object, required: true },
});

defineEmits(['open']);

const categoryMeta = {
  FOOD: { icon: '🍽', iconSrc: foodIcon, color: '#2457aa' },
  CAFE: { icon: '☕', iconSrc: cafeIcon, color: '#8b5a2b' },
  LIVING: { icon: '🏠', iconSrc: livingIcon, color: '#13a184' },
  SHOPPING: { icon: '🛍', iconSrc: shoppingIcon, color: '#f59e0b' },
  LEISURE: { icon: '🎮', iconSrc: leisureIcon, color: '#8b5cf6' },
  TRANSPORT: { icon: '🚌', iconSrc: taxiIcon, color: '#0ea5e9' },
  OTHER: { icon: '•••', color: '#64748b' },
};

const missions = computed(() => props.missionData?.missions || []);
const monthLabel = computed(() => {
  const month = Number(props.missionData?.targetYearMonth?.split('-')[1]);
  return Number.isFinite(month) ? `${month}월` : '이번 달';
});
const currentWeekNumber = computed(() => {
  for (const mission of missions.value) {
    const week = currentWeek(mission);
    if (week) return week.weekNumber;
  }
  return null;
});
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
    PENDING: '진행 중',
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
        <h2>
          {{ monthLabel }}{{ currentWeekNumber ? ` ${currentWeekNumber}주차` : '' }} 저축 미션
          <span class="mission-title-ai-badge" aria-hidden="true"><img :src="aiIcon" alt="" /></span>
        </h2>
        <p>선택한 소비 절감 목표를 실천하고 있어요.</p>
      </div>
      <button type="button" @click="$emit('open')">전체 보기 <b>›</b></button>
    </header>

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
            <strong>{{ mission.categoryName }} {{ mission.reductionRate }}% 줄이기</strong>
            <span>{{ statusLabel(currentWeek(mission)?.status || mission.status) }}</span>
          </div>
          <div class="mission-progress">
            <i :style="{ width: `${spendingPercent(mission)}%` }" />
          </div>
          <small>남은 한도 {{ formatCurrency(remainingLimit(mission)) }}</small>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.home-mission-card{position:relative;overflow:hidden;padding:19px;border:1px solid #bcd2ff;border-radius:24px;background:linear-gradient(150deg,#eff5ff 0%,#e6f0ff 100%);box-shadow:0 13px 30px #1a489a1f;color:#173f8d;animation:mission-enter .48s cubic-bezier(.22,1,.36,1) both}.home-mission-card::after{position:absolute;top:-65px;right:-58px;width:170px;height:170px;border-radius:50%;background:#5688e51a;content:''}.home-mission-card header{position:relative;z-index:1;display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.home-mission-card header small{color:#f06a2a;font-size:8px;font-weight:950;letter-spacing:.14em}.home-mission-card h2{display:flex;align-items:center;gap:2px;margin-top:5px;font-size:17px;font-weight:950;letter-spacing:-.05em}.mission-title-ai-badge{display:inline-flex;flex:none;align-items:center;animation:mission-title-ai-pulse 1.6s ease-in-out infinite}.mission-title-ai-badge img{width:22px;height:22px;object-fit:contain}@keyframes mission-title-ai-pulse{0%,100%{opacity:.6;transform:scale(.85) rotate(-3deg);filter:invert(34%) sepia(94%) saturate(1272%) hue-rotate(199deg) brightness(91%) drop-shadow(0 0 0 rgba(23,104,242,0))}50%{opacity:1;transform:scale(1.25) rotate(3deg);filter:invert(34%) sepia(94%) saturate(1272%) hue-rotate(199deg) brightness(91%) drop-shadow(0 0 7px rgba(23,104,242,.6))}}@media(prefers-reduced-motion:reduce){.mission-title-ai-badge{animation:none}}.home-mission-card header p{margin-top:4px;color:#6f83a8;font-size:10px}.home-mission-card header button{flex:none;color:#286ce0;font-size:10px;font-weight:900}.home-mission-card header button b{font-size:15px}.mission-list{position:relative;z-index:1;display:grid;gap:9px;margin-top:16px}.mission-list article{display:flex;align-items:center;gap:11px;padding:13px;border:1px solid #d5e1f5;border-radius:16px;background:#fff}.mission-icon{display:grid;flex:0 0 39px;height:39px;place-items:center;border-radius:13px;background:color-mix(in srgb,var(--mission-color) 12%,white);font-size:18px}.mission-icon img{width:19px;height:19px}.mission-info{min-width:0;flex:1}.mission-info>div:first-child{display:flex;align-items:center;justify-content:space-between;gap:8px}.mission-info strong{overflow:hidden;color:#233b68;font-size:12px;font-weight:900;text-overflow:ellipsis;white-space:nowrap}.mission-info>div:first-child span{flex:none;padding:4px 7px;border-radius:99px;background:#e2f7f1;color:#119278;font-size:8px;font-weight:900}.mission-info small{display:block;margin-top:6px;color:#7485a1;font-size:8px}.mission-progress{height:5px;margin-top:8px;overflow:hidden;border-radius:99px;background:#e8eef8}.mission-progress i{display:block;height:100%;border-radius:inherit;background:var(--mission-color);transition:width .55s ease}@keyframes mission-enter{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}
</style>
