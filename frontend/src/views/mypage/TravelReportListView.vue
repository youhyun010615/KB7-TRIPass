<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelReportStore } from '@/stores/travelReport'

const route=useRoute(); const router=useRouter(); const store=useTravelReportStore()
const tripId=computed(()=>{
  const parsed = Number(route.query.tripId)
  return route.query.tripId && Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const report=computed(()=>store.tripSummary)

function load(id) {
  store.loadPreTripReport(id)
  store.loadTripBasic(id)
}
onMounted(() => { if (tripId.value) load(tripId.value) })
watch(tripId, id => { if (id) load(id) })
</script>

<template><main class="page"><header><button @click="router.back()">‹</button><h1>여행 리포트</h1><span/></header>
  <p v-if="!tripId" class="loading error">여행 정보를 찾을 수 없어요.</p>
  <p v-else-if="!report && store.errorMessage" class="loading error">{{ store.errorMessage }}</p>
  <p v-else-if="!report" class="loading">불러오는 중...</p>
  <template v-else>
  <section class="ticket"><small>TRIP REPORT ARCHIVE</small><div><h2>{{ report.flags }} {{ report.title }}</h2><b>D-{{ report.dDay }}</b></div><i/><p>생성된 리포트</p><div class="total"><strong>2개</strong><em>여행 전 · 여행 후</em></div></section>
  <h3>리포트 목록</h3>
  <button class="report-card" @click="router.push(`/mypage/reports/pre-trip?tripId=${tripId}`)"><span class="blue">▥</span><div><b>여행 대비 리포트</b><small>예산과 여행 자금 준비 현황</small></div><em>확인</em><strong>›</strong></button>
  <button class="report-card" :class="{disabled:report.status!=='완료'}" :disabled="report.status!=='완료'" @click="router.push(`/mypage/reports/post-trip?tripId=${tripId}`)"><span class="orange">▤</span><div><b>여행 후 리포트</b><small>지출 분석과 여행 기록 요약</small></div><em class="after">{{ report.status==='완료'?'여행 후':'준비 중' }}</em><strong>›</strong></button>
  <aside>✈️ <span><b>여행 단계에 맞춰 리포트를 확인해 보세요</b><small>여행 전후의 자금 변화를 한눈에 볼 수 있어요.</small></span></aside>
  </template>
  <BottomNav/></main></template>

<style scoped>
.page{min-height:100vh;padding:0 18px 95px;background:#f8f6f1;color:#111a2d}.page>header{display:grid;height:68px;grid-template-columns:40px 1fr 40px;align-items:end;padding-bottom:18px}.page>header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}.page>header h1{text-align:center;font-size:20px;font-weight:900}.loading{padding:40px 0;color:#8290a3;font-size:11px;text-align:center}.loading.error{color:#e5484d}.ticket{position:relative;padding:22px 20px;border-radius:20px;background:linear-gradient(135deg,#17397f,#102b66);color:#fff}.ticket:before,.ticket:after{position:absolute;top:51%;width:18px;height:18px;border-radius:50%;background:#f8f6f1;content:''}.ticket:before{left:-9px}.ticket:after{right:-9px}.ticket small{color:#c7d7f6;font-size:9px;font-weight:800}.ticket>div{display:flex;align-items:center;justify-content:space-between}.ticket h2{margin-top:18px;font-size:16px}.ticket>div>b{margin-top:18px;color:#ffb21c;font-size:12px}.ticket i{display:block;margin:18px 0 14px;border-top:1px dashed #829dcb}.ticket p{color:#b9cae7;font-size:9px}.ticket .total{margin-top:7px}.ticket .total strong{font-size:22px}.ticket .total em{color:#52c9ff;font-size:11px;font-style:normal;font-weight:900}.page>h3{margin:24px 2px 13px;font-size:16px}.report-card{display:grid;width:100%;grid-template-columns:48px 1fr auto 12px;gap:13px;align-items:center;margin-bottom:12px;padding:20px 16px;border:1px solid #e4e8ef;border-radius:20px;background:#fff;text-align:left;box-shadow:0 8px 18px #1727490c}.report-card>span{display:grid;width:46px;height:46px;border-radius:14px;place-items:center;font-size:20px}.blue{background:#eaf3ff;color:#1671ef}.orange{background:#fff1dc;color:#f08719}.report-card div b,.report-card div small{display:block}.report-card div b{font-size:14px}.report-card div small{margin-top:6px;color:#7f8da2;font-size:9px}.report-card em{padding:7px 11px;border-radius:15px;background:#e9f3ff;color:#0869eb;font-size:9px;font-style:normal;font-weight:900}.report-card em.after{background:#fff1d9;color:#e98211}.report-card>strong{color:#7d8999;font-size:23px}.report-card.disabled{opacity:.55}aside{display:flex;gap:11px;margin-top:22px;padding:17px;border-radius:16px;background:#e6f1ff}aside b,aside small{display:block}aside b{color:#143879;font-size:10px}aside small{margin-top:7px;color:#71839f;font-size:9px}
</style>
