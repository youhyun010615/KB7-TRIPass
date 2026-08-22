<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelStore } from '@/stores/travel'
import { useTravelScheduleStore } from '@/stores/travelSchedule'

const props = defineProps({
  archiveMode: { type: Boolean, default: false },
})
const route = useRoute()
const router = useRouter()
const store = useTravelScheduleStore()
const travel = useTravelStore()
const schedule = computed(() => store.getSchedule(route.params.scheduleId))

onMounted(async () => {
  const tripId = props.archiveMode ? route.params.id : null
  await store.loadSchedules(tripId).catch(() => {})
  await store.loadScheduleDetail(route.params.scheduleId, tripId).catch(() => {})
})
const country = computed(() => {
  const current = schedule.value
  if (!current) return null
  const matched = travel.selectedPlans.find(
    item => Number(item.tripCountryId) === Number(current.tripCountryId),
  ) || store.countries.find(item => item.code === current.countryCode)
  if (matched) return matched
  const name = current.countryName || ''
  const presentation = travel.countryFlagMap[name]
  return name ? { name, flag: presentation?.emoji || '🌍', class: presentation?.class || '' } : null
})
const wonRate = { EUR:1486.2, USD:1380, CHF:1704.6, JPY:9.23, HKD:184.2 }
const won = computed(() => Math.round((schedule.value?.amount || 0) * (wonRate[schedule.value?.currency] || 1)))
const paymentLabel = computed(() => ({ prepaid:'사전결제 완료', onsite:'현장결제 필요', undecided:'미정' })[schedule.value?.paymentStatus] || '미정')
async function remove() {
  if (!window.confirm('이 여행 일정을 삭제할까요?')) return
  if (await store.remove(route.params.scheduleId)) {
    if (props.archiveMode) {
      router.push(`/mypage/travel/${route.params.id}/schedules`)
    } else {
      router.push('/schedule')
    }
  }
}
</script>

<template>
  <main class="detail-page">
    <header><button type="button" @click="router.back()">‹</button><h1>여행일정 상세 정보</h1><span /></header>
    <template v-if="schedule">
      <section class="details"><h3>일정 정보</h3><dl>
        <div class="wide"><dt>일정명</dt><dd class="schedule-title">{{ schedule.title }}</dd></div>
        <div><dt>국가</dt><dd class="country-detail"><img v-if="country?.flagUrl" :src="country.flagUrl" alt="" /><i v-else-if="country?.class" :class="country.class" aria-hidden="true" /><span v-else>{{ country?.flag }}</span>{{ country?.name || schedule.countryName || '국가 정보 없음' }}</dd></div>
        <div><dt>일시</dt><dd>{{ schedule.date }} {{ schedule.time }}</dd></div>
        <div class="wide"><dt>장소</dt><dd>{{ schedule.placeName || '장소 미정' }}</dd></div>
        <div class="amount-detail"><dt>금액</dt><dd>{{ schedule.currency }} {{ Number(schedule.amount || 0).toLocaleString() }}</dd><small>원화 환산 금액 · 약 {{ won.toLocaleString() }}원</small></div>
        <div><dt>결제 상태</dt><dd><span class="payment-status">{{ paymentLabel }}</span></dd></div>
      </dl></section>
      <section class="memo"><h3>메모</h3><p>{{ schedule.memo || '등록된 메모가 없어요.' }}</p></section>
      <div class="actions"><button type="button" class="edit" @click="router.push({ path: `/schedule/${schedule.id}/edit`, query: archiveMode ? { tripId: route.params.id } : {} })">수정</button><button type="button" class="delete" @click="remove">삭제</button></div>
    </template>
    <p v-else class="empty">일정을 찾을 수 없어요.</p>
    <BottomNav v-if="!archiveMode" />
  </main>
</template>

<style scoped>
.detail-page{min-height:100vh;padding:0 16px 100px;background:#f3f6fc;color:#10192d}.detail-page>header{display:grid;grid-template-columns:40px 1fr 40px;align-items:end;height:64px;padding-bottom:14px}.detail-page>header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}.detail-page h1{text-align:center;font-size:17px;font-weight:900}.details,.memo{padding:18px;border:1px solid #dce4ee;border-radius:18px;background:#fff;box-shadow:0 7px 20px rgba(23,63,141,.05)}.memo{margin-top:13px}.details h3,.memo h3{font-size:15px;font-weight:900}.details dl{display:grid;grid-template-columns:1fr 1fr;gap:0 18px;margin-top:10px}.details dl>div{padding:13px 0;border-bottom:1px solid #edf0f4}.details dl>.wide{grid-column:1/-1}.details dt{color:#8290a3;font-size:10px;font-weight:800}.details dd{margin-top:7px;font-size:12px;font-weight:800;line-height:1.5}.details .schedule-title{color:#173f8d;font-size:17px;font-weight:900}.amount-detail small{display:block;margin-top:3px;color:#6480a7;font-size:9px;font-weight:800}.payment-status{display:inline-flex;padding:5px 8px;border-radius:999px;background:#edf4ff;color:#2662ea;font-size:10px;font-weight:900}.memo{min-height:130px}.memo p{margin-top:13px;padding:14px;border-radius:13px;background:#f6f8fc;color:#475569;font-size:10px;line-height:1.65}.actions{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;margin-top:18px}.actions button{height:48px;border-radius:13px;font-size:12px;font-weight:900}.actions .edit{border:0;background:#2662ea;color:#fff}.actions .delete{border:1px solid #f3d4d6;background:#fff;color:#e5484d}.empty{padding:80px 0;text-align:center;color:#94a3b8}
.country-detail{display:flex;align-items:center;gap:7px}.country-detail img,.country-detail i{width:24px;height:16px;flex:none;border-radius:3px;object-fit:cover;background-position:center;background-size:cover;box-shadow:0 1px 4px rgba(15,35,70,.15)}
</style>
