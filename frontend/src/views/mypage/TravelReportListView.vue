<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelReportStore } from '@/stores/travelReport'
import { flagIconClass } from '@/stores/travel'
import reportIcon from '@/assets/icons/report.svg'

const route=useRoute(); const router=useRouter(); const store=useTravelReportStore()
const tripId=computed(()=>{
  const parsed = Number(route.query.tripId)
  return route.query.tripId && Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const report=computed(()=>store.tripSummary)
const reportCount=computed(()=>report.value?.status === '여행 완료' ? 2 : 1)
const displayStatus=computed(()=>report.value?.status === '여행 중' ? '여행중' : report.value?.status)

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
  <section class="ticket">
    <div class="ticket-head">
      <small>TRIP REPORT ARCHIVE</small>
      <b :class="{ traveling: displayStatus === '여행중' }">{{ displayStatus }}</b>
    </div>
    <div class="ticket-title-row">
      <div class="ticket-title-main">
        <h2>{{ report.title }}</h2>
        <div class="ticket-flags" aria-label="여행 국가">
          <span v-for="code in report.countryCodes" :key="code" :class="flagIconClass(code)" class="fi-inline" />
        </div>
      </div>
      <p>{{ report.dateRange }}</p>
    </div>
  </section>
  <div class="report-list-heading">
    <h3>리포트 목록</h3>
    <span>생성된 리포트 {{ reportCount }}개</span>
  </div>
  <p class="report-list-description">여행 전후의 자금 흐름을 리포트를 통해 확인해보세요.<br>여행 후 리포트는 여행 후에 볼 수 있어요.</p>
  <button class="report-card" @click="router.push(`/mypage/reports/pre-trip?tripId=${tripId}`)"><span class="blue"><img :src="reportIcon" alt="" /></span><div><b>여행 저축 리포트</b><small>여행 전 저축과 자금 준비 기록</small></div><em>생성완료</em><strong>›</strong></button>
  <button class="report-card" :class="{disabled:report.status!=='여행 완료'}" :disabled="report.status!=='여행 완료'" @click="router.push(`/mypage/reports/post-trip?tripId=${tripId}`)"><span class="orange">▤</span><div><b>여행 후 리포트</b></div><em class="after">{{ report.status==='여행 완료'?'확인':'준비 중' }}</em><strong>›</strong></button>
  </template>
  <BottomNav/></main></template>

<style scoped>
.page{min-height:100vh;padding:0 18px 95px;background:#f8f6f1;color:#111a2d}.page>header{display:grid;height:68px;grid-template-columns:40px 1fr 40px;align-items:end;padding-bottom:18px}.page>header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}.page>header h1{text-align:center;font-size:20px;font-weight:900}.loading{padding:40px 0;color:#8290a3;font-size:11px;text-align:center}.loading.error{color:#e5484d}.ticket{position:relative;padding:22px 20px;border-radius:20px;background:linear-gradient(135deg,#17397f,#102b66);color:#fff}.ticket:before,.ticket:after{position:absolute;top:51%;width:18px;height:18px;border-radius:50%;background:#f8f6f1;content:''}.ticket:before{left:-9px}.ticket:after{right:-9px}.ticket small{color:#c7d7f6;font-size:9px;font-weight:800}.ticket>div{display:flex;align-items:center;justify-content:space-between}.ticket h2{margin-top:18px;font-size:16px}.ticket>div>b{margin-top:18px;color:#ffb21c;font-size:12px}.ticket i{display:block;margin:18px 0 14px;border-top:1px dashed #829dcb}.ticket p{color:#b9cae7;font-size:9px}.ticket .total{margin-top:7px}.ticket .total strong{font-size:22px}.ticket .total em{color:#52c9ff;font-size:11px;font-style:normal;font-weight:900}.page>h3{margin:24px 2px 13px;font-size:16px}.report-card{display:grid;width:100%;grid-template-columns:48px 1fr auto 12px;gap:13px;align-items:center;margin-bottom:12px;padding:20px 16px;border:1px solid #e4e8ef;border-radius:20px;background:#fff;text-align:left;box-shadow:0 8px 18px #1727490c}.report-card>span{display:grid;width:46px;height:46px;border-radius:14px;place-items:center;font-size:20px}.blue{background:#eaf3ff;color:#1671ef}.orange{background:#fff1dc;color:#f08719}.report-card div b,.report-card div small{display:block}.report-card div b{font-size:14px}.report-card div small{margin-top:6px;color:#7f8da2;font-size:9px}.report-card em{padding:7px 11px;border-radius:15px;background:#e9f3ff;color:#0869eb;font-size:9px;font-style:normal;font-weight:900}.report-card em.after{background:#fff1d9;color:#e98211}.report-card>strong{color:#7d8999;font-size:23px}.report-card.disabled{opacity:.55}aside{display:flex;gap:11px;margin-top:22px;padding:17px;border-radius:16px;background:#e6f1ff}aside b,aside small{display:block}aside b{color:#143879;font-size:10px}aside small{margin-top:7px;color:#71839f;font-size:9px}
</style>
<style scoped>
.page{background:#f3f6fc}.ticket{background:linear-gradient(145deg,#2662ea,#173f8d);box-shadow:0 16px 34px rgba(23,63,141,.2)}.ticket:before,.ticket:after{background:#f3f6fc}.ticket>div>b,.ticket .total em{color:#ffd45e}.report-card{border-color:#dfe7f4;box-shadow:0 8px 22px rgba(23,63,141,.06)}.report-card em{background:#e9f0ff;color:#2662ea}.report-card em.after{background:#fff4d7;color:#a86c00}aside{background:#eaf1ff}
.ticket{padding:18px;border-radius:18px;box-shadow:0 10px 24px rgba(23,63,141,.14)}.ticket h2,.ticket>div>b{margin-top:14px}.ticket i{margin:14px 0 11px}.page>h3{margin:20px 2px 11px}.report-card{grid-template-columns:40px 1fr auto 10px;gap:10px;margin-bottom:10px;padding:14px;border-radius:16px;box-shadow:0 6px 16px rgba(23,63,141,.05)}.report-card>span{width:38px;height:38px;border-radius:12px;font-size:17px}.report-card div small{margin-top:4px}.report-card em{padding:6px 9px}.report-card>strong{font-size:20px}aside{gap:9px;margin-top:17px;padding:13px;border:1px solid #d8e4f8;border-radius:14px}

/* 여행 일정 카드와 동일한 카드 규격·색상·타이포그래피 */
.ticket {
  overflow: hidden;
  padding: 15px 16px 16px;
  border-radius: 18px;
  background: linear-gradient(145deg, #1f5ab9 0%, #14357f 62%, #102d6d 100%);
  color: #fff;
  box-shadow: 0 12px 26px rgba(24, 51, 99, .2);
}
.ticket::before,
.ticket::after { display: none; }
.ticket .ticket-head,
.ticket .ticket-title-row,
.ticket .ticket-period-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ticket .ticket-head small,
.ticket .ticket-head b {
  margin: 0;
  color: #ffd466;
  font-family: 'Space Mono', monospace;
  font-size: 8px;
  font-weight: 800;
  letter-spacing: .13em;
}
.ticket .ticket-head b {
  padding: 5px 9px;
  border: 1px solid rgba(255, 212, 94, .62);
  border-radius: 999px;
  background: rgba(255, 212, 94, .13);
  font-size: 9px;
  font-weight: 900;
  letter-spacing: .06em;
}
.ticket .ticket-title-row {
  align-items: flex-start;
  flex-direction: column;
  gap: 7px;
  margin-top: 15px;
}
.ticket .ticket-title-main{display:flex;min-width:0;align-items:center;gap:8px}
.ticket .ticket-title-row h2 {
  min-width: 0;
  overflow: hidden;
  margin: 0;
  font-size: 17px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ticket .ticket-title-row>p{color:rgba(255,255,255,.72);font-family:'Space Mono',monospace;font-size:9px;font-weight:700}
.ticket-flags{display:flex;flex:0 0 auto;gap:3px}.ticket-flags .fi-inline{width:13px;height:9px;border-radius:2px;background-size:cover;box-shadow:0 1px 3px rgba(0,0,0,.18)}
.ticket .ticket-head b.traveling{animation:report-status-pulse 1.8s ease-in-out infinite}@keyframes report-status-pulse{0%,100%{box-shadow:0 0 0 0 rgba(255,212,94,.35)}50%{box-shadow:0 0 0 5px rgba(255,212,94,0)}}
.ticket .ticket-period-row { margin-top: 9px; }
.ticket .ticket-period-row p {
  color: rgba(255, 255, 255, .72);
  font-size: 10px;
  font-weight: 650;
}
.ticket .ticket-period-row strong {
  color: #ffd466;
  font-family: 'Space Mono', monospace;
  font-size: 9px;
  font-weight: 900;
  letter-spacing: .06em;
}
.report-list-heading{display:flex;align-items:center;gap:8px;margin:21px 2px 0}.report-list-heading h3{font-size:14px;font-weight:900}.report-list-heading span{color:#2662ea;font-size:10px;font-weight:850}.report-list-description{margin:6px 2px 13px;color:#77869d;font-size:10px;font-weight:600;line-height:1.65}.page>h3{margin:0}.report-card div b{font-size:14px;font-weight:900}.report-card>span img{width:21px;height:21px;object-fit:contain}@media(prefers-reduced-motion:reduce){.ticket .ticket-head b.traveling{animation:none}}
</style>
