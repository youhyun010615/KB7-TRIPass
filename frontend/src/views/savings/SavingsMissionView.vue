<script setup>
import { computed, ref } from 'vue'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTripWalletStore } from '@/stores/tripWallet'

const wallet = useTripWalletStore()
const selected = ref([])
const complete = ref(false)
const missions = [
  { id: 1, rank: 1, category: '식비', icon: '🍴', spend: '342,000원', ratio: '38%', reduction: '120,000원', reason: '지난달 평균보다 외식 지출이 27% 늘었어요.', task: '이번 주 식비 55,000원으로 생활하기', target: '55,000원', current: '31,000원', progress: 56 },
  { id: 2, rank: 2, category: '카페', icon: '☕', spend: '171,000원', ratio: '19%', reduction: '60,000원', reason: '평균 주간 지출이 42,000원이에요.', task: '이번 주 카페 25,000원으로 생활하기', target: '25,000원', current: '18,000원', progress: 72 },
  { id: 3, rank: 3, category: '쇼핑', icon: '🛍', spend: '126,000원', ratio: '14%', reduction: '40,000원', reason: '일시적인 지출 증가가 확인됐어요.', task: '이번 주 쇼핑 지출 줄이기', target: '20,000원', current: '10,000원', progress: 50 },
]
const selectedMissions = computed(() => missions.filter(mission => selected.value.includes(mission.id)))
const expectedSaving = computed(() => selectedMissions.value.reduce((sum, mission) => sum + Number(mission.reduction.replace(/[^0-9]/g, '')), 0))

function toggle(id) { selected.value = selected.value.includes(id) ? selected.value.filter(value => value !== id) : [...selected.value, id] }
function finishMission() {
  if (!expectedSaving.value || complete.value) return
  wallet.deposit(expectedSaving.value, '8월 저축 미션 달성', 'AI 저축 미션')
  complete.value = true
}
</script>

<template>
  <main class="mission-page">
    <header><p>AI SAVING COACH</p><h1>이달의 저축 미션</h1><span>지난달 소비를 분석해 여행 자금을 더 모을 수 있는 방법을 찾았어요.</span></header>

    <section class="analysis-card">
      <div class="analysis-head"><div><small>7월 AI 분석 리포트</small><h2>이번 달 목표를 위해<br><b>{{ expectedSaving ? `${expectedSaving.toLocaleString('ko-KR')}원` : '220,000원' }}</b>을 줄여볼까요?</h2></div><span>✈</span></div>
      <div v-for="mission in missions" :key="mission.id" class="rank-row">
        <b>{{ mission.rank }}</b><span>{{ mission.icon }} {{ mission.category }}</span><small>{{ mission.spend }} · {{ mission.ratio }}</small><strong>{{ mission.reduction }} 줄이기</strong>
      </div>
    </section>

    <section class="mission-list"><div class="list-heading"><h2>추천 미션</h2><span>최대 3개 선택</span></div>
      <article v-for="mission in missions" :key="mission.id" :class="{ chosen: selected.includes(mission.id) }" @click="toggle(mission.id)">
        <button class="mission-check" :class="{ checked: selected.includes(mission.id) }">{{ selected.includes(mission.id) ? '✓' : '' }}</button>
        <div class="mission-title"><span>{{ mission.icon }}</span><div><small>{{ mission.category }} · AI 추천</small><b>{{ mission.task }}</b></div></div>
        <p>{{ mission.reason }}</p>
        <div class="mission-progress"><i :style="{ width: `${mission.progress}%` }"/><span>{{ mission.current }} / {{ mission.target }}</span></div>
      </article>
    </section>

    <section v-if="selected.length" class="selected-summary"><div><small>선택한 미션을 모두 달성하면</small><b>여행 자금 {{ expectedSaving.toLocaleString('ko-KR') }}원을 더 모을 수 있어요</b></div><button :disabled="complete" @click="finishMission">{{ complete ? '월렛 반영 완료' : '미션 시작하기' }}</button></section>
    <BottomNav />
  </main>
</template>

<style scoped>
.mission-page{min-height:100vh;padding:42px 18px 98px;background:#f5f7fe;color:#14254b}.mission-page header p{color:#2872e9;font-size:10px;font-weight:900;letter-spacing:.14em}.mission-page header h1{margin-top:8px;font-size:27px;font-weight:900;letter-spacing:-.06em}.mission-page header>span{display:block;margin-top:7px;color:#8491a8;font-size:12px;line-height:1.45}.analysis-card{margin-top:20px;padding:17px;border:1px solid #cce0ff;border-radius:20px;background:linear-gradient(135deg,#ebf4ff,#dcecff)}.analysis-head{display:flex;justify-content:space-between}.analysis-head small{color:#2872e9;font-size:10px;font-weight:900}.analysis-head h2{margin-top:8px;font-size:19px;font-weight:900;line-height:1.3;letter-spacing:-.04em}.analysis-head h2 b{color:#1566df}.analysis-head>span{display:grid;width:44px;height:44px;place-items:center;border-radius:15px;background:#fff3c9;font-size:24px}.rank-row{display:grid;grid-template-columns:23px 1fr auto;align-items:center;gap:8px;margin-top:12px;padding:11px;border-radius:12px;background:#fff}.rank-row>b{display:grid;width:21px;height:21px;place-items:center;border-radius:50%;background:#dce9ff;color:#2968d5;font-size:10px}.rank-row span{font-size:12px;font-weight:900}.rank-row small{grid-column:2;color:#7185a4;font-size:9px}.rank-row strong{grid-row:1/3;grid-column:3;color:#f05252;font-size:10px}.mission-list{margin-top:23px}.list-heading{display:flex;align-items:center;justify-content:space-between}.list-heading h2{font-size:18px;font-weight:900}.list-heading span{color:#8593a9;font-size:10px}.mission-list article{position:relative;margin-top:11px;padding:15px 14px 13px 48px;border:1px solid #e0e6f0;border-radius:18px;background:#fff;box-shadow:0 6px 12px #16365d08;cursor:pointer}.mission-list article.chosen{border-color:#3678e8;background:#f8fbff}.mission-check{position:absolute;top:18px;left:14px;display:grid;width:22px;height:22px;place-items:center;border:1.5px solid #bbc8dc;border-radius:50%;color:#fff;font-size:12px}.mission-check.checked{border-color:#2871e7;background:#2871e7}.mission-title{display:flex;align-items:center;gap:8px}.mission-title>span{font-size:18px}.mission-title small{display:block;color:#6d85aa;font-size:9px}.mission-title b{display:block;margin-top:3px;font-size:13px}.mission-list article>p{margin:11px 0 9px;color:#7d8ba2;font-size:10px}.mission-progress{position:relative;height:7px;border-radius:99px;background:#e6ebf3}.mission-progress i{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#2e72e5,#79aaff)}.mission-progress span{position:absolute;top:10px;right:0;color:#52739d;font-size:9px}.selected-summary{position:sticky;bottom:76px;display:flex;align-items:center;gap:12px;margin-top:26px;padding:15px;border-radius:18px;background:#173d8b;color:#fff;box-shadow:0 8px 20px #1c3c8038}.selected-summary div{flex:1}.selected-summary small{display:block;color:#afc7f6;font-size:9px}.selected-summary b{display:block;margin-top:4px;font-size:11px;line-height:1.35}.selected-summary button{flex:none;padding:10px 12px;border-radius:10px;background:#ff8538;color:#fff;font-size:11px;font-weight:900}.selected-summary button:disabled{background:#38b49f}
</style>
