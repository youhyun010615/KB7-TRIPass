<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTravelStore } from '@/stores/travel'
import { useTravelModeStore } from '@/stores/travelMode'

const router = useRouter()
const travel = useTravelStore()
const travelMode = useTravelModeStore()
const busy = ref(false)
const trip = computed(() => travel.lifecycle || {})

async function finish(destination) {
  if (busy.value) return
  busy.value = true
  try {
    await travel.archiveCurrentTrip()
    travelMode.setMode('savings')
    await router.replace(destination === 'register' ? '/travel/register' : '/')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <main class="ending-page">
    <section class="ending-hero">
      <span class="eyebrow">TRIPASS · JOURNEY COMPLETE</span>
      <div class="plane" aria-hidden="true">✈</div>
      <h1>여행은 잘 보내셨나요?</h1>
      <p>{{ trip.tripName || '소중한 여행' }}의 기록이 모두 준비됐어요.<br>저축부터 여행의 순간까지 천천히 돌아보세요.</p>
    </section>

    <section class="report-stack">
      <button type="button" @click="router.push(`/mypage/reports/pre-trip?tripId=${trip.tripId}`)">
        <span class="report-icon">₩</span>
        <span><small>SAVING STORY</small><strong>여행 저축 리포트</strong><em>여행을 위해 모은 과정과 달성 기록</em></span>
        <b>›</b>
      </button>
      <button type="button" @click="router.push(`/mypage/reports/post-trip?tripId=${trip.tripId}`)">
        <span class="report-icon yellow">✦</span>
        <span><small>TRAVEL STORY</small><strong>여행 리포트</strong><em>예산과 실제 지출, 여행 결과 분석</em></span>
        <b>›</b>
      </button>
      <button type="button" @click="router.push(`/mypage/travel/${trip.tripId}`)">
        <span class="report-icon pale">▤</span>
        <span><small>MY TRIP ARCHIVE</small><strong>여행 기록 전체 보기</strong><em>일정·체크리스트·영수증·완료 미션</em></span>
        <b>›</b>
      </button>
    </section>

    <section class="ending-actions">
      <p>리포트는 언제든 마이페이지의 여행 관리에서 다시 볼 수 있어요.</p>
      <button type="button" class="primary" :disabled="busy" @click="finish('register')">새 여행 목표 설정하기</button>
      <button type="button" class="secondary" :disabled="busy" @click="finish('home')">홈으로 가기</button>
    </section>
  </main>
</template>

<style scoped>
.ending-page{min-height:100vh;padding:58px 20px 36px;background:linear-gradient(180deg,#0b2a6b 0,#174ca7 43%,#eef3fb 43%);color:#10192b}.ending-hero{text-align:center;color:#fff}.eyebrow{font-size:10px;font-weight:900;letter-spacing:.18em;color:#ffd466}.plane{width:74px;height:74px;margin:20px auto 14px;display:grid;place-items:center;border-radius:50%;font-size:32px;background:#ffffff18;border:1px solid #ffffff35;box-shadow:0 14px 36px #061c4c66}.ending-hero h1{font-size:27px;font-weight:950}.ending-hero p{margin-top:10px;font-size:13px;line-height:1.75;color:#dce8ff}.report-stack{margin-top:34px;display:grid;gap:12px}.report-stack button{width:100%;display:grid;grid-template-columns:50px 1fr 18px;align-items:center;gap:13px;padding:17px;border:0;border-radius:20px;background:#fff;text-align:left;box-shadow:0 9px 25px #18386b18}.report-stack button>span:nth-child(2){display:grid;gap:2px}.report-stack small{font-size:9px;font-weight:900;letter-spacing:.12em;color:#2f6fed}.report-stack strong{font-size:15px}.report-stack em{font-size:11px;font-style:normal;color:#8a96aa}.report-stack b{font-size:24px;color:#9aa7bb}.report-icon{width:48px;height:48px;display:grid!important;place-items:center!important;border-radius:15px;background:#eaf1ff;color:#174ca7;font-size:21px;font-weight:900}.report-icon.yellow{background:#fff5d8;color:#d89b00}.report-icon.pale{background:#eef2f7;color:#607086}.ending-actions{margin-top:24px}.ending-actions p{text-align:center;font-size:10px;color:#8491a5;margin-bottom:14px}.ending-actions button{width:100%;height:52px;border-radius:16px;font-size:14px;font-weight:900}.primary{border:0;background:#17499c;color:#fff}.secondary{margin-top:9px;border:1px solid #cfdaea;background:#fff;color:#24426f}
</style>
