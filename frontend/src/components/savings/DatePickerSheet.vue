<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  open: Boolean,
  plan: { type: Object, default: null },
  periods: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'confirm'])

const today = new Date()
today.setHours(0, 0, 0, 0)
const start = ref('')
const end = ref('')
const viewDate = ref(new Date(today.getFullYear(), today.getMonth(), 1))

const toIso = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const fromIso = (value) => new Date(`${value}T00:00:00`)

const monthLabel = computed(() => `${viewDate.value.getFullYear()}년 ${viewDate.value.getMonth() + 1}월`)
const currentOrder = computed(() => props.periods.findIndex((period) => period.countryId === props.plan?.countryId) + 1)
const previousPeriod = computed(() => currentOrder.value > 1 ? props.periods[currentOrder.value - 2] : null)
const previousPeriods = computed(() => props.periods.slice(0, Math.max(0, currentOrder.value - 1)).filter((period) => (
  period.startDate && period.endDate
)))

watch(() => props.plan, (plan) => {
  const previousDeparture = previousPeriod.value?.endDate || ''
  start.value = plan?.startDate || previousDeparture
  end.value = plan?.endDate || ''
  const baseDate = plan?.startDate || previousDeparture
  const base = baseDate ? fromIso(baseDate) : today
  viewDate.value = new Date(base.getFullYear(), base.getMonth(), 1)
}, { immediate: true })

const calendarDays = computed(() => {
  const year = viewDate.value.getFullYear()
  const month = viewDate.value.getMonth()
  const firstWeekday = new Date(year, month, 1).getDay()
  const lastDate = new Date(year, month + 1, 0).getDate()
  const days = [
    ...Array.from({ length: firstWeekday }, (_, index) => ({ key: `blank-${index}`, blank: true })),
    ...Array.from({ length: lastDate }, (_, index) => {
      const date = new Date(year, month, index + 1)
      const iso = toIso(date)
      return {
        key: iso,
        iso,
        day: index + 1,
        past: date < today,
        beforePrevious: Boolean(previousPeriod.value?.endDate && iso < previousPeriod.value.endDate),
      }
    }),
  ]
  return [
    ...days,
    ...Array.from({ length: Math.max(0, 42 - days.length) }, (_, index) => ({
      key: `trailing-blank-${index}`,
      blank: true,
    })),
  ]
})

const nights = computed(() => {
  if (!start.value || !end.value) return 0
  return Math.round((fromIso(end.value) - fromIso(start.value)) / 86400000)
})
const isValid = computed(() => Boolean(start.value && end.value && nights.value > 0))

function periodFor(iso) {
  return previousPeriods.value.find((period) => iso >= period.startDate && iso <= period.endDate)
}

function dayClasses(day) {
  if (day.blank) return { blank: true }
  return {
    past: day.past,
    unavailable: day.beforePrevious,
    selected: day.iso === start.value || day.iso === end.value,
    range: start.value && end.value && day.iso > start.value && day.iso < end.value,
    occupied: Boolean(periodFor(day.iso)),
  }
}

function dayStyle(day) {
  const period = day.iso ? periodFor(day.iso) : null
  return period ? { '--period-color': period.accent || '#94a3b8' } : undefined
}

function selectDay(day) {
  if (day.blank || day.past || day.beforePrevious) return
  if (!start.value || end.value) {
    start.value = day.iso
    end.value = ''
    return
  }
  if (day.iso <= start.value) {
    start.value = day.iso
    end.value = ''
    return
  }
  end.value = day.iso
}

function changeMonth(offset) {
  viewDate.value = new Date(viewDate.value.getFullYear(), viewDate.value.getMonth() + offset, 1)
}

function confirm() {
  if (!isValid.value) return
  emit('confirm', { startDate: start.value, endDate: end.value })
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="sheet-overlay" @click.self="emit('close')">
      <section class="date-sheet" role="dialog" aria-modal="true" aria-labelledby="date-sheet-title">
        <header>
          <div><h2 id="date-sheet-title">여행 날짜 선택</h2><p>도착일과 출발일을 차례로 터치하세요.</p></div>
          <button aria-label="닫기" @click="emit('close')">×</button>
        </header>

        <section class="current-country" :style="{ '--country-color': plan?.accent || '#2469e8' }">
          <span>{{ plan?.flag || '✈️' }}</span>
          <div><small>현재 편집 중 · {{ currentOrder }}번째 방문국</small><b>{{ plan?.name }}</b></div>
          <em>{{ start && end ? `${start.slice(5).replace('-', '.')} ~ ${end.slice(5).replace('-', '.')}` : '날짜 선택 중' }}</em>
        </section>

        <div class="selection-tickets">
          <div :class="{ active: !start || end }"><small>{{ previousPeriod ? '이전 일정 연결 · 도착일' : '첫 번째 터치 · 도착일' }}</small><b>{{ start || '날짜 선택' }}</b></div>
          <span>→</span>
          <div :class="{ active: start && !end }"><small>두 번째 터치 · 출발일</small><b>{{ end || '날짜 선택' }}</b></div>
        </div>

        <div class="calendar-head"><button @click="changeMonth(-1)">‹</button><strong>{{ monthLabel }}</strong><button @click="changeMonth(1)">›</button></div>
        <div class="weekdays"><span v-for="day in ['일','월','화','수','목','금','토']" :key="day">{{ day }}</span></div>
        <div class="calendar-grid" :style="{ '--country-color': plan?.accent || '#2469e8' }">
          <button
            v-for="day in calendarDays"
            :key="day.key"
            type="button"
            :disabled="day.blank || day.past || day.beforePrevious"
            :class="dayClasses(day)"
            :style="dayStyle(day)"
            :aria-label="day.iso"
            @click="selectDay(day)"
          ><span>{{ day.day }}</span></button>
        </div>

        <div v-if="previousPeriods.length" class="period-legend">
          <p>형광펜으로 표시된 이전 방문 일정</p>
          <div v-for="period in previousPeriods" :key="period.countryId" :style="{ '--period-color': period.accent || '#94a3b8' }">
            <span>{{ period.flag }}</span><b>{{ period.name }}</b><em>{{ period.startDate.slice(5).replace('-', '.') }} ~ {{ period.endDate.slice(5).replace('-', '.') }}</em>
          </div>
        </div>

        <section class="date-guide">
          <span>📅</span>
          <div v-if="isValid"><b>총 {{ nights }}박 {{ nights + 1 }}일</b><p>{{ start.replaceAll('-', '.') }}부터 {{ end.replaceAll('-', '.') }}까지</p></div>
          <div v-else><b>{{ start ? '출발일을 한 번 더 터치하세요' : '도착일을 먼저 터치하세요' }}</b><p>이전 국가 일정은 달력에 형광펜으로 표시돼요.</p></div>
        </section>
        <button class="confirm" :disabled="!isValid" @click="confirm">이 일정으로 선택하기</button>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.sheet-overlay{position:fixed;z-index:100;inset:0;display:flex;align-items:flex-start;justify-content:center;background:rgba(17,24,39,.46)}.date-sheet{width:min(390px,100%);height:92vh;height:92dvh;margin-top:8vh;margin-top:8dvh;overflow-y:auto;padding:23px 20px 22px;border-radius:28px 28px 0 0;background:#fff;box-shadow:0 -8px 30px rgba(15,23,42,.14)}header{display:flex;justify-content:space-between}h2{font-size:18px;font-weight:800}header p{margin-top:4px;color:#64748b;font-size:10px}header button{border:0;background:none;color:#64748b;font-size:26px}.current-country{display:flex;align-items:center;gap:10px;margin-top:14px;padding:12px;border:1px solid color-mix(in srgb,var(--country-color) 35%,white);border-left:5px solid var(--country-color);border-radius:13px;background:color-mix(in srgb,var(--country-color) 8%,white)}.current-country>span{font-size:26px}.current-country div{display:flex;flex:1;flex-direction:column;gap:2px}.current-country small{color:#64748b;font-size:8px}.current-country b{font-size:15px}.current-country em{padding:5px 7px;border-radius:8px;color:var(--country-color);background:#fff;font-size:8px;font-style:normal;font-weight:800}.selection-tickets{display:grid;grid-template-columns:1fr 18px 1fr;align-items:center;gap:6px;margin-top:12px}.selection-tickets>div{padding:9px 10px;border:1px solid #e2e8f0;border-radius:11px;background:#f8fafc}.selection-tickets>div.active{border-color:#6ea0ef;background:#eef5ff}.selection-tickets small,.selection-tickets b{display:block}.selection-tickets small{color:#94a3b8;font-size:7px}.selection-tickets b{margin-top:5px;color:#173b86;font-size:10px}.selection-tickets>span{color:#94a3b8;text-align:center}.calendar-head{display:flex;align-items:center;justify-content:space-between;margin-top:17px}.calendar-head button{width:32px;height:32px;border:0;border-radius:50%;background:#f3f6fb;font-size:20px}.calendar-head strong{font-size:14px}.weekdays,.calendar-grid{display:grid;grid-template-columns:repeat(7,1fr)}.weekdays{margin-top:10px}.weekdays span{padding:6px 0;color:#64748b;text-align:center;font-size:9px}.weekdays span:first-child{color:#e5484d}.weekdays span:last-child{color:#2469e8}.calendar-grid{row-gap:3px}.calendar-grid button{position:relative;height:36px;border:0;border-radius:50%;background:transparent;font-size:10px}.calendar-grid button.past,.calendar-grid button.unavailable{color:#cbd5e1}.calendar-grid button.range{border-radius:0;background:color-mix(in srgb,var(--country-color) 12%,white);color:#173b86}.calendar-grid button.selected{z-index:1;border-radius:50%;color:#fff;background:var(--country-color);font-weight:800}.calendar-grid button.occupied:not(.selected){border-radius:3px;color:#334155;background:color-mix(in srgb,var(--period-color) 18%,white);box-shadow:inset 0 -4px 0 color-mix(in srgb,var(--period-color) 72%,white);font-weight:800}.period-legend{margin-top:10px;padding:10px;border-radius:11px;background:#f7f9fc}.period-legend>p{margin-bottom:7px;color:#64748b;font-size:8px}.period-legend>div{display:flex;align-items:center;gap:6px;margin-top:5px;padding:7px 9px;border-radius:7px;background:color-mix(in srgb,var(--period-color) 16%,white);box-shadow:inset 0 -4px 0 color-mix(in srgb,var(--period-color) 65%,white)}.period-legend div>span{font-size:13px}.period-legend div>b{color:#334155;font-size:9px}.period-legend div>em{margin-left:auto;color:#64748b;font-size:8px;font-style:normal}.date-guide{display:flex;align-items:center;gap:10px;margin-top:11px;padding:11px;border-radius:12px;background:#f1f6ff}.date-guide>span{font-size:20px}.date-guide b{color:#173b86;font-size:10px}.date-guide p{margin-top:3px;color:#64748b;font-size:8px}.confirm{width:100%;height:50px;margin-top:12px;border:0;border-radius:14px;color:#fff;background:#263f8c;font-weight:800}.confirm:disabled{background:#cbd5e1}
</style>
