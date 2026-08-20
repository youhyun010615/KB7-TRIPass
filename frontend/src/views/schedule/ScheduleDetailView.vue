<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelScheduleStore } from '@/stores/travelSchedule'

const route = useRoute()
const router = useRouter()
const store = useTravelScheduleStore()
const schedule = computed(() => store.getSchedule(route.params.scheduleId))

onMounted(() => {
  store.loadScheduleDetail(route.params.scheduleId).catch(() => {})
})
const country = computed(() => store.countries.find(item => item.code === schedule.value?.countryCode))
const wonRate = { EUR:1486.2, USD:1380, CHF:1704.6, JPY:9.23, HKD:184.2 }
const won = computed(() => Math.round((schedule.value?.amount || 0) * (wonRate[schedule.value?.currency] || 1)))
const paymentLabel = computed(() => ({ prepaid:'💳 사전결제 완료', onsite:'💵 현장결제 필요', undecided:'❔ 미정' })[schedule.value?.paymentStatus] || '❔ 미정')
async function remove() {
  if (!window.confirm('이 여행 일정을 삭제할까요?')) return
  if (await store.remove(route.params.scheduleId)) router.push('/schedule')
}
</script>

<template>
  <main class="detail-page">
    <header><button type="button" @click="router.back()">‹</button><h1>여행일정 상세 정보</h1><span /></header>
    <template v-if="schedule">
      <section class="hero"><span>{{ paymentLabel }}</span><small>{{ country?.flag }} {{ country?.name }} 여행</small><h2>{{ schedule.title }}</h2><strong>{{ schedule.currency }} {{ schedule.amount.toLocaleString() }} <em>(약 {{ won.toLocaleString() }}원)</em></strong></section>
      <section class="details"><dl>
        <div><dt>🌐 국가</dt><dd>{{ country?.flag }} {{ country?.name }}</dd></div>
        <div><dt>▣ 일시</dt><dd>{{ schedule.date }} {{ schedule.time }}</dd></div>
        <div><dt>📍 장소명</dt><dd>{{ schedule.placeName || '장소 미정' }}</dd></div>
        <div><dt>🗺 주소</dt><dd>{{ schedule.placeAddress || '주소 미정' }}</dd></div>
      </dl></section>
      <section class="memo"><h3>📝 메모</h3><p>{{ schedule.memo || '등록된 메모가 없어요.' }}</p></section>
      <div class="actions"><button type="button" @click="remove">삭제</button><button type="button" @click="router.push(`/schedule/${schedule.id}/edit`)">수정</button></div>
    </template>
    <p v-else class="empty">일정을 찾을 수 없어요.</p>
    <BottomNav />
  </main>
</template>

<style scoped>
.detail-page{min-height:100vh;padding:0 16px 90px;background:#f8f6f1;color:#10192d}.detail-page>header{display:grid;grid-template-columns:32px 1fr 32px;align-items:end;height:59px;padding-bottom:13px}.detail-page>header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}.detail-page h1{text-align:center;font-size:17px;font-weight:900}.hero{padding:18px;border:1px solid #dce4ee;border-radius:17px;background:#fff}.hero>span{display:inline-block;padding:5px 8px;border-radius:8px;background:#eaf3ff;color:#2670dc;font-size:8px}.hero small,.hero h2,.hero strong{display:block}.hero small{margin-top:13px;color:#64748b;font-size:9px}.hero h2{margin-top:5px;font-size:16px}.hero strong{margin-top:12px;color:#1267e5;font-size:22px}.hero strong em{color:#64748b;font-size:9px;font-style:normal}.details,.memo{margin-top:12px;padding:7px 16px;border:1px solid #dce4ee;border-radius:17px;background:#fff}.details dl>div{padding:14px 0;border-bottom:1px solid #edf0f4}.details dl>div:last-child{border:0}.details dt{color:#64748b;font-size:9px}.details dd{margin-top:7px;font-size:11px;font-weight:800;line-height:1.5}.memo{min-height:105px;padding:16px}.memo h3{font-size:11px}.memo p{margin-top:12px;color:#475569;font-size:10px;line-height:1.6}.actions{display:grid;grid-template-columns:1fr 2fr;gap:9px;margin-top:20px}.actions button{height:50px;border-radius:12px;background:#fff0f0;color:#e5484d;font-size:12px;font-weight:900}.actions button:last-child{background:#19489c;color:#fff}.empty{padding:80px 0;text-align:center;color:#94a3b8}
</style>
