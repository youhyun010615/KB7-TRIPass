<script setup>
import { computed, ref, watch } from 'vue'
import calendarIcon from '@/assets/icons/calendar.svg'
import { today as currentDate } from '@/utils/devDate'

const props = defineProps({
  open: Boolean,
  plan: { type: Object, default: null },
  periods: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'confirm'])

const today = currentDate()
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
    range: start.value && end.value && day.iso >= start.value && day.iso <= end.value,
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
    <Transition name="fade">
      <div v-if="open" class="sheet-overlay" @click.self="emit('close')">
        <Transition name="slide-up">
          <section v-if="open" class="date-sheet" role="dialog" aria-modal="true" aria-labelledby="date-sheet-title">
            <header>
              <div><h2 id="date-sheet-title">여행 날짜 선택</h2><p>출발일과 도착일을 차례로 터치하세요.</p></div>
              <button aria-label="닫기" @click="emit('close')">×</button>
            </header>

            <section class="current-country" :style="{ '--country-color': plan?.accent || '#2469e8' }">
              <span>{{ plan?.flag || '✈️' }}</span>
              <div><small>현재 편집 중 · {{ currentOrder }}번째 방문국</small><b>{{ plan?.name }}</b></div>
              <em>{{ start && end ? `${start.slice(5).replace('-', '.')} ~ ${end.slice(5).replace('-', '.')}` : '날짜 선택 중' }}</em>
            </section>

            <div class="selection-tickets">
              <div :class="{ active: !start || end }"><small>{{ previousPeriod ? '이전 일정 연결 · 출발일' : '첫 번째 터치 · 출발일' }}</small><b>{{ start || '날짜 선택' }}</b></div>
              <span>→</span>
              <div :class="{ active: start && !end }"><small>두 번째 터치 · 도착일</small><b>{{ end || '날짜 선택' }}</b></div>
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
              <img :src="calendarIcon" alt="" aria-hidden="true">
              <div v-if="isValid"><b>총 {{ nights }}박 {{ nights + 1 }}일</b><p>{{ start.replaceAll('-', '.') }}부터 {{ end.replaceAll('-', '.') }}까지</p></div>
              <div v-else><b>{{ start ? '도착일을 한 번 더 터치하세요' : '출발일을 먼저 터치하세요' }}</b><p>이전 국가 일정은 달력에 형광펜으로 표시돼요.</p></div>
            </section>
            <button class="confirm" :disabled="!isValid" @click="confirm">이 일정으로 선택하기</button>
          </section>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-up-enter-active, .slide-up-leave-active { transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-up-enter-from, .slide-up-leave-to { transform: translateY(100%); }

.sheet-overlay{position:fixed;z-index:100;inset:0;display:flex;align-items:flex-end;justify-content:center;background:rgba(11,42,107,.42);backdrop-filter:blur(2px)}.date-sheet{width:min(390px,100%);max-height:calc(100vh - 76px);max-height:calc(100dvh - 76px);overflow-y:auto;padding:22px 20px calc(24px + env(safe-area-inset-bottom));border-radius:24px 24px 0 0;background:#fff;box-shadow:0 -10px 30px rgba(16,25,43,.2)}
/* ... 나머지 스타일은 동일 ... */

<style scoped>
.sheet-overlay{position:fixed;z-index:100;inset:0;display:flex;align-items:flex-end;justify-content:center;background:rgba(11,42,107,.42);backdrop-filter:blur(2px)}.date-sheet{width:min(390px,100%);max-height:calc(100vh - 76px);max-height:calc(100dvh - 76px);overflow-y:auto;padding:22px 20px calc(24px + env(safe-area-inset-bottom));border-radius:24px 24px 0 0;background:#fff;box-shadow:0 -10px 30px rgba(16,25,43,.2)}header{display:flex;justify-content:space-between}h2{font-size:16.5px;font-weight:900}header p{margin-top:4px;color:#98a2b3;font-size:11px;font-weight:600}header button{border:0;background:none;color:#98a2b3;font-size:24px}.current-country{display:flex;align-items:center;gap:8px;margin-top:14px;padding:11px 13px;border:1.5px solid #1e9e6c;border-radius:13px;background:#eaf7ef}.current-country>span{font-size:15px}.current-country div{display:flex;flex:1;flex-direction:column;gap:2px}.current-country small{color:#1e9e6c;font-size:9.5px;font-weight:700}.current-country b{font-size:13.5px}.current-country em{padding:4px 9px;border-radius:99px;color:#1e9e6c;background:#fff;font-size:10.5px;font-style:normal;font-weight:800}.selection-tickets{display:grid;grid-template-columns:minmax(0,1fr) 18px minmax(0,1fr);align-items:center;gap:9px;margin-top:12px}.selection-tickets>div{min-width:0;padding:9px 11px;border:1.5px solid #2f6fed;border-radius:11px;background:#fff}.selection-tickets>div.active{border-color:#2f6fed;background:#fff}.selection-tickets small,.selection-tickets b{display:block}.selection-tickets small{color:#98a2b3;font-size:9.5px;font-weight:700}.selection-tickets b{margin-top:2px;color:#10192b;font-size:12.5px;font-weight:800;white-space:nowrap}.selection-tickets>span{color:#98a2b3;text-align:center}.calendar-head{display:flex;align-items:center;justify-content:space-between;margin-top:14px}.calendar-head button{width:28px;height:28px;border:0;background:transparent;color:#10192b;font-size:20px}.calendar-head strong{font-size:14px;font-weight:800}.weekdays,.calendar-grid{display:grid;grid-template-columns:repeat(7,minmax(0,1fr))}.weekdays{margin-top:8px}.weekdays span{padding:5px 0;color:#98a2b3;text-align:center;font-size:10.5px}.weekdays span:first-child{color:#e4576c}.weekdays span:last-child{color:#98a2b3}.calendar-grid{row-gap:3px}.calendar-grid button{position:relative;display:grid;min-width:0;height:32px;padding:0;place-items:center;border:0;border-radius:0;background:transparent;font-size:10.5px;font-weight:700}.calendar-grid button>span{display:grid;width:28px;height:28px;place-items:center;border-radius:50%}.calendar-grid button.past,.calendar-grid button.unavailable{color:#b4bcc9}.calendar-grid button.range{background:#e4f5ec;color:#173b86}.calendar-grid button.selected{z-index:1;color:#fff;background:#e4f5ec;font-weight:800}.calendar-grid button.selected>span{color:#fff;background:#1e9e6c}.calendar-grid button.occupied:not(.selected){border-radius:2px;color:#334155;background:color-mix(in srgb,var(--period-color) 18%,white);box-shadow:none;font-weight:800}.period-legend{margin-top:10px;padding:0;background:transparent}.period-legend>p{margin-bottom:6px;color:#98a2b3;font-size:10px;font-weight:700}.period-legend>div{display:flex;align-items:center;gap:6px;margin-top:6px;padding:10px 13px;border-radius:11px;background:#f6f8fc;box-shadow:none}.period-legend div>span{font-size:13px}.period-legend div>b{color:#10192b;font-size:12px}.period-legend div>em{margin-left:auto;color:var(--period-color);font-size:11px;font-style:normal;font-weight:700}.date-guide{display:flex;align-items:center;gap:10px;margin-top:11px;padding:12px 14px;border-radius:12px;background:#eaf1ff}.date-guide>img{width:19px;height:19px}.date-guide b{color:#0b2a6b;font-size:12px}.date-guide p{margin-top:2px;color:#4c6099;font-size:10px}.confirm{width:100%;height:50px;margin-top:12px;border:0;border-radius:14px;color:#fff;background:#0b2a6b;font-size:13.5px;font-weight:800}.confirm:disabled{background:#cbd5e1}
</style>
