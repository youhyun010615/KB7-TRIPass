<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { fetchMyTrips } from '@/api/travel'
import { countryPresentation, flagIconClass } from '@/stores/travel'
import { daysUntilTrip, tripPhase } from '@/utils/tripLifecycle'

const props = defineProps({
  tripId: { type: [Number, String], required: true },
  showStatus: { type: Boolean, default: true },
})

const trip = ref(null)

function splitNames(value) {
  return String(value || '').split(' · ').map(name => name.trim()).filter(Boolean)
}

function codeOf(name) {
  return countryPresentation[name]?.code || ''
}

const countryCodes = computed(() => {
  const explicit = (trip.value?.countries || []).map(country => country.code || country.countryCode).filter(Boolean)
  return explicit.length ? explicit : splitNames(trip.value?.countryNames).map(codeOf).filter(Boolean)
})

const daysUntilStart = computed(() => {
  return daysUntilTrip(trip.value)
})

const statusLabel = computed(() => {
  const phase = tripPhase(trip.value)
  if (phase === 'ENDED') return '여행 완료'
  if (phase === 'TRAVELING') return '여행 중'
  return daysUntilStart.value === null ? '여행 전' : `출국까지 D-${Math.max(0, daysUntilStart.value)}`
})

function dateText(value) {
  return value ? String(value).replaceAll('-', '.') : ''
}

const dateRange = computed(() => `${dateText(trip.value?.startDate)} - ${dateText(trip.value?.endDate)}`)
const totalDays = computed(() => trip.value?.totalDays || (() => {
  if (!trip.value?.startDate || !trip.value?.endDate) return 0
  return Math.floor((new Date(trip.value.endDate) - new Date(trip.value.startDate)) / 86400000) + 1
})())

async function loadTrip() {
  if (!props.tripId) return
  try {
    const trips = await fetchMyTrips()
    trip.value = (trips || []).find(item => String(item.tripId) === String(props.tripId)) || null
  } catch (error) {
    console.error('여행 요약 조회 실패:', error)
    trip.value = null
  }
}

onMounted(loadTrip)
watch(() => props.tripId, loadTrip)
</script>

<template>
  <section v-if="trip" class="archive-summary-card">
    <div class="summary-orbit" aria-hidden="true" />
    <div class="summary-main">
      <div class="summary-title">
        <h2>{{ trip.tripName }}</h2>
        <div class="summary-flags" aria-label="여행 국가">
          <span v-for="(code, index) in countryCodes" :key="`${code}-${index}`" :class="flagIconClass(code)" class="fi-inline" />
        </div>
      </div>
      <p>{{ dateRange }}<template v-if="totalDays"> · {{ totalDays }}일</template></p>
    </div>
    <strong v-if="showStatus" :class="{ traveling: statusLabel === '여행 중' }">{{ statusLabel }}</strong>
  </section>
</template>

<style scoped>
.archive-summary-card{position:relative;display:flex;min-height:112px;align-items:flex-start;justify-content:space-between;gap:14px;overflow:hidden;padding:24px 20px;border-radius:22px;background:linear-gradient(135deg,#dce9fb 0%,#c8daf6 100%);color:#10234a;box-shadow:0 10px 24px rgba(35,73,136,.13)}
.summary-orbit{position:absolute;top:-62px;right:-42px;width:154px;height:154px;border-radius:50%;background:rgba(255,255,255,.24)}
.summary-main{position:relative;z-index:1;min-width:0;flex:1}.summary-title{display:flex;min-width:0;align-items:center;gap:8px}.summary-title h2{min-width:0;overflow:hidden;font-size:18px;font-weight:950;letter-spacing:-.04em;text-overflow:ellipsis;white-space:nowrap}.summary-flags{display:flex;flex:0 0 auto;align-items:center;gap:3px}.summary-flags span{width:15px;height:10px;border-radius:2px;background-size:cover;box-shadow:0 1px 3px rgba(0,0,0,.17)}
.summary-main p{margin-top:12px;color:#6680a8;font-family:'Space Mono',monospace;font-size:11px;font-weight:800;letter-spacing:-.03em}
.archive-summary-card>strong{position:relative;z-index:1;flex:0 0 auto;padding:8px 12px;border-radius:999px;background:rgba(255,255,255,.76);color:#173f8d;font-size:11px;font-weight:900;white-space:nowrap}.archive-summary-card>strong.traveling{background:#e1f7ed;color:#087f61;animation:status-pulse 1.8s ease-in-out infinite}
@keyframes status-pulse{0%,100%{box-shadow:0 0 0 0 rgba(8,127,97,.22)}50%{box-shadow:0 0 0 6px rgba(8,127,97,0)}}
@media(prefers-reduced-motion:reduce){.archive-summary-card>strong.traveling{animation:none}}
</style>
