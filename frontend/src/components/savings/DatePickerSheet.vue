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

watch(() => props.plan, (plan) => {
  start.value = plan?.startDate || ''
  end.value = plan?.endDate || ''
  const base = plan?.startDate ? fromIso(plan.startDate) : today
  viewDate.value = new Date(base.getFullYear(), base.getMonth(), 1)
}, { immediate: true })

const monthLabel = computed(() => `${viewDate.value.getFullYear()}년 ${viewDate.value.getMonth() + 1}월`)
const calendarDays = computed(() => {
  const year = viewDate.value.getFullYear()
  const month = viewDate.value.getMonth()
  const firstWeekday = new Date(year, month, 1).getDay()
  const lastDate = new Date(year, month + 1, 0).getDate()
  return [
    ...Array.from({ length: firstWeekday }, (_, index) => ({ key: `blank-${index}`, blank: true })),
    ...Array.from({ length: lastDate }, (_, index) => {
      const date = new Date(year, month, index + 1)
      return { key: toIso(date), iso: toIso(date), day: index + 1, past: date < today }
    }),
  ]
})

const otherPeriods = computed(() => props.periods.filter((period) => (
  period.countryId !== props.plan?.countryId && period.startDate && period.endDate
)))
const nights = computed(() => {
  if (!start.value || !end.value) return 0
  return Math.round((fromIso(end.value) - fromIso(start.value)) / 86400000)
})
const isValid = computed(() => Boolean(start.value && end.value && nights.value > 0))

function periodFor(iso) {
  return otherPeriods.value.find((period) => iso >= period.startDate && iso <= period.endDate)
}

function dayClasses(day) {
  if (day.blank) return { blank: true }
  return {
    past: day.past,
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
  if (day.blank || day.past) return
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
          <div><h2 id="date-sheet-title">{{ plan?.name }} 여행 날짜</h2><p>달력에서 도착일과 출발일을 차례로 터치하세요.</p></div>
          <button aria-label="닫기" @click="emit('close')">×</button>
        </header>

        <div class="selection-tickets">
          <div :class="{ active: !start || end }"><small>첫 번째 터치 · 도착일</small><b>{{ start || '날짜 선택' }}</b></div>
          <span>→</span>
          <div :class="{ active: start && !end }"><small>두 번째 터치 · 출발일</small><b>{{ end || '날짜 선택' }}</b></div>
        </div>

        <div class="calendar-head"><button @click="changeMonth(-1)">‹</button><strong>{{ monthLabel }}</strong><button @click="changeMonth(1)">›</button></div>
        <div class="weekdays"><span v-for="day in ['일','월','화','수','목','금','토']" :key="day">{{ day }}</span></div>
        <div class="calendar-grid">
          <button
            v-for="day in calendarDays"
            :key="day.key"
            type="button"
            :disabled="day.blank || day.past"
            :class="dayClasses(day)"
            :style="dayStyle(day)"
            :aria-label="day.iso"
            @click="selectDay(day)"
          ><span>{{ day.day }}</span><i v-if="day.iso && periodFor(day.iso)" /></button>
        </div>

        <div v-if="otherPeriods.length" class="period-legend">
          <p>이미 선택한 다른 국가 일정</p>
          <span v-for="period in otherPeriods" :key="period.countryId"><i :style="{ background: period.accent }" />{{ period.name }} {{ period.startDate.slice(5).replace('-', '.') }}~{{ period.endDate.slice(5).replace('-', '.') }}</span>
        </div>

        <section class="date-guide">
          <span>📅</span>
          <div v-if="isValid"><b>총 {{ nights }}박 {{ nights + 1 }}일</b><p>{{ start.replaceAll('-', '.') }}부터 {{ end.replaceAll('-', '.') }}까지</p></div>
          <div v-else><b>{{ start ? '출발일을 한 번 더 터치하세요' : '도착일을 먼저 터치하세요' }}</b><p>다른 국가 일정은 색상 점으로 함께 표시돼요.</p></div>
        </section>
        <button class="confirm" :disabled="!isValid" @click="confirm">이 일정으로 선택하기</button>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.sheet-overlay{position:fixed;z-index:100;inset:0;display:flex;align-items:flex-end;justify-content:center;background:rgba(17,24,39,.46)}.date-sheet{width:min(390px,100%);max-height:92vh;overflow-y:auto;padding:23px 20px 22px;border-radius:28px 28px 0 0;background:#fff;box-shadow:0 -8px 30px rgba(15,23,42,.14)}header{display:flex;justify-content:space-between}h2{font-size:18px;font-weight:800}header p{margin-top:4px;color:#64748b;font-size:10px}header button{border:0;background:none;color:#64748b;font-size:26px}.selection-tickets{display:grid;grid-template-columns:1fr 18px 1fr;align-items:center;gap:6px;margin-top:17px}.selection-tickets>div{padding:9px 10px;border:1px solid #e2e8f0;border-radius:11px;background:#f8fafc}.selection-tickets>div.active{border-color:#6ea0ef;background:#eef5ff}.selection-tickets small,.selection-tickets b{display:block}.selection-tickets small{color:#94a3b8;font-size:7px}.selection-tickets b{margin-top:5px;color:#173b86;font-size:10px}.selection-tickets>span{color:#94a3b8;text-align:center}.calendar-head{display:flex;align-items:center;justify-content:space-between;margin-top:20px}.calendar-head button{width:32px;height:32px;border:0;border-radius:50%;background:#f3f6fb;font-size:20px}.calendar-head strong{font-size:14px}.weekdays,.calendar-grid{display:grid;grid-template-columns:repeat(7,1fr)}.weekdays{margin-top:10px}.weekdays span{padding:6px 0;color:#64748b;text-align:center;font-size:9px}.weekdays span:first-child{color:#e5484d}.weekdays span:last-child{color:#2469e8}.calendar-grid{row-gap:3px}.calendar-grid button{position:relative;height:36px;border:0;border-radius:50%;background:transparent;font-size:10px}.calendar-grid button.past{color:#cbd5e1}.calendar-grid button.range{border-radius:0;background:#e8f1ff;color:#173b86}.calendar-grid button.selected{z-index:1;border-radius:50%;color:#fff;background:#2469e8;font-weight:800}.calendar-grid button.occupied:not(.selected){color:#334155;font-weight:700}.calendar-grid button i{position:absolute;left:50%;bottom:3px;width:4px;height:4px;border-radius:50%;background:var(--period-color);transform:translateX(-50%)}.period-legend{margin-top:10px;padding:9px 10px;border-radius:10px;background:#f7f9fc}.period-legend p{margin-bottom:5px;color:#64748b;font-size:8px}.period-legend span{display:inline-flex;align-items:center;gap:4px;margin:3px 10px 0 0;color:#475569;font-size:8px}.period-legend i{width:6px;height:6px;border-radius:50%}.date-guide{display:flex;align-items:center;gap:10px;margin-top:11px;padding:11px;border-radius:12px;background:#f1f6ff}.date-guide>span{font-size:20px}.date-guide b{color:#173b86;font-size:10px}.date-guide p{margin-top:3px;color:#64748b;font-size:8px}.confirm{width:100%;height:50px;margin-top:12px;border:0;border-radius:14px;color:#fff;background:#263f8c;font-weight:800}.confirm:disabled{background:#cbd5e1}
</style>
