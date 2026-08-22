<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useChecklistStore } from '@/stores/checklist'
import { useTravelReportStore } from '@/stores/travelReport'
import { flagIconClass } from '@/stores/travel'
import checklistIcon from '@/assets/icons/checklist.svg'
import TravelArchiveSummaryCard from '@/components/mypage/TravelArchiveSummaryCard.vue'

const route = useRoute()
const router = useRouter()
const checklistStore = useChecklistStore()
const reportStore = useTravelReportStore()

const tripId = computed(() => {
  const parsed = Number(route.query.tripId)
  return route.query.tripId && Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const trip = computed(() => reportStore.tripSummary)
const displayStatus = computed(() => trip.value?.status === '여행 중' ? '여행중' : trip.value?.status)
const summary = computed(() => checklistStore.summary)
const totalProgress = computed(() => ({ done: summary.value.totalCompletedCount || 0, total: summary.value.totalItemCount || 0 }))
const prepProgress = computed(() => ({ done: summary.value.prepCompletedCount || 0, total: summary.value.prepItemCount || 0 }))
const returnProgress = computed(() => ({ done: summary.value.returnCompletedCount || 0, total: summary.value.returnItemCount || 0 }))

function load(id) {
  Promise.all([
    checklistStore.loadSummary(id),
    checklistStore.fetchTripDday(id),
    reportStore.loadPreTripReport(id),
    reportStore.loadTripBasic(id),
  ])
}

onMounted(() => { if (tripId.value) load(tripId.value); else router.back() })
watch(tripId, (id) => { if (id) load(id) })
</script>

<template>
  <main class="page">
    <header><button type="button" @click="router.back()">‹</button><h1>체크리스트</h1><span /></header>
    <p v-if="!tripId" class="loading error">여행 정보를 찾을 수 없어요.</p>
    <p v-else-if="!trip" class="loading">불러오는 중...</p>
    <template v-else>
      <TravelArchiveSummaryCard :trip-id="tripId" />

      <div class="list-heading"><h3>체크리스트 목록</h3><span>{{ totalProgress.done }}/{{ totalProgress.total }} 완료</span></div>
      <p class="list-description">완료하지 못한 준비 항목은 다음 단계로 이월돼요.<br>체크리스트는 직접 추가할 수도 있어요.</p>

      <button class="menu-card" type="button" @click="router.push(`/mypage/checklists/preparation?tripId=${tripId}`)">
        <span class="menu-icon"><img :src="checklistIcon" alt="" /></span>
        <span class="copy"><b>여행 준비 체크리스트</b><small>D-30 · D-7 · D-1 준비 항목</small></span>
        <em>{{ prepProgress.done }}/{{ prepProgress.total }}</em><strong>›</strong>
      </button>
      <button class="menu-card" type="button" @click="router.push(`/mypage/checklists/return?tripId=${tripId}`)">
        <span class="menu-icon"><img :src="checklistIcon" alt="" /></span>
        <span class="copy"><b>귀국 체크리스트</b><small>귀국일 점검 및 정리 항목</small></span>
        <em :class="{ scheduled: returnProgress.total === 0 || returnProgress.done === 0 }">{{ returnProgress.total > 0 ? `${returnProgress.done}/${returnProgress.total}` : '예정' }}</em><strong>›</strong>
      </button>
    </template>
    <BottomNav flat />
  </main>
</template>

<style scoped>
.page{min-height:100vh;padding:0 18px 95px;background:#f3f6fc;color:#111a2d}.page>header{display:grid;height:68px;grid-template-columns:40px 1fr 40px;align-items:end;padding-bottom:18px}.page>header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}.page>header h1{text-align:center;font-size:20px;font-weight:900}.loading{padding:40px 0;color:#8290a3;font-size:11px;text-align:center}.loading.error{color:#e5484d}
.ticket{overflow:hidden;padding:15px 16px 16px;border-radius:18px;background:linear-gradient(145deg,#1f5ab9 0%,#14357f 62%,#102d6d 100%);color:#fff;box-shadow:0 12px 26px rgba(24,51,99,.2)}.ticket-head{display:flex;align-items:center;justify-content:space-between}.ticket-head small,.ticket-head b{margin:0;color:#ffd466;font-family:'Space Mono',monospace;font-size:8px;font-weight:800;letter-spacing:.13em}.ticket-head b{padding:5px 9px;border:1px solid rgba(255,212,94,.62);border-radius:999px;background:rgba(255,212,94,.13);font-size:9px;font-weight:900;letter-spacing:.06em}.ticket-title-row{display:flex;align-items:flex-start;flex-direction:column;gap:7px;margin-top:15px}.ticket-title-main{display:flex;min-width:0;align-items:center;gap:8px}.ticket-title-row h2{min-width:0;overflow:hidden;margin:0;font-size:17px;font-weight:900;text-overflow:ellipsis;white-space:nowrap}.ticket-title-row>p{color:rgba(255,255,255,.72);font-family:'Space Mono',monospace;font-size:9px;font-weight:700}.ticket-flags{display:flex;flex:0 0 auto;gap:3px}.ticket-flags .fi-inline{width:13px;height:9px;border-radius:2px;background-size:cover;box-shadow:0 1px 3px rgba(0,0,0,.18)}.ticket-head b.traveling{animation:status-pulse 1.8s ease-in-out infinite}
.list-heading{display:flex;align-items:center;gap:8px;margin:21px 2px 0}.list-heading h3{font-size:17px;font-weight:900}.list-heading span{color:#2662ea;font-size:10px;font-weight:850}.list-description{margin:6px 2px 13px;color:#77869d;font-size:10px;font-weight:600;line-height:1.65}
.menu-card{display:grid;width:100%;grid-template-columns:40px 1fr auto 10px;gap:10px;align-items:center;margin-bottom:10px;padding:14px;border:1px solid #dfe7f4;border-radius:16px;background:#fff;text-align:left;box-shadow:0 6px 16px rgba(23,63,141,.05)}.menu-icon{display:grid;width:38px;height:38px;place-items:center;border-radius:12px;background:#eaf3ff}.menu-icon img{width:21px;height:21px;object-fit:contain}.copy b,.copy small{display:block}.copy b{font-size:14px;font-weight:900}.copy small{margin-top:4px;color:#7f8da2;font-size:9px}.menu-card em{padding:6px 9px;border-radius:15px;background:#e9f0ff;color:#2662ea;font-size:9px;font-style:normal;font-weight:900}.menu-card em.scheduled{background:#fff4d7;color:#a86c00}.menu-card>strong{color:#7d8999;font-size:20px}
@keyframes status-pulse{0%,100%{box-shadow:0 0 0 0 rgba(255,212,94,.35)}50%{box-shadow:0 0 0 5px rgba(255,212,94,0)}}@media(prefers-reduced-motion:reduce){.ticket-head b.traveling{animation:none}}
</style>
