<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({ open: Boolean, plan: { type: Object, default: null } })
const emit = defineEmits(['close', 'confirm'])
const start = ref('2026-08-15')
const end = ref('2026-08-22')
const days = Array.from({ length: 35 }, (_, i) => i + 1)

watch(() => props.plan, (plan) => {
  start.value = plan?.startDate || '2026-08-15'
  end.value = plan?.endDate || '2026-08-22'
}, { immediate: true })

const startDay = computed(() => Number(start.value.slice(-2)))
const endDay = computed(() => Number(end.value.slice(-2)))
const nights = computed(() => Math.max(0, Math.round((new Date(end.value) - new Date(start.value)) / 86400000)))

function selectDay(day) {
  if (day < 1 || day > 31) return
  const date = `2026-08-${String(day).padStart(2, '0')}`
  if (!start.value || (start.value && end.value)) {
    start.value = date
    end.value = ''
  } else if (new Date(date) >= new Date(start.value)) {
    end.value = date
  } else {
    start.value = date
  }
}

function confirm() {
  if (!start.value || !end.value) return
  emit('confirm', { startDate: start.value, endDate: end.value })
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="sheet-overlay" @click.self="emit('close')">
      <section class="date-sheet">
        <header><div><h2>여행 날짜 선택</h2><p>출발일과 도착일을 선택해 주세요</p></div><button @click="emit('close')">×</button></header>
        <div class="date-tickets">
          <button :class="{ active: !end }" @click="start = ''; end = ''"><small>출발일</small><b>{{ start || '날짜 선택' }}</b></button>
          <span>→</span>
          <button :class="{ active: start && !end }"><small>도착일</small><b>{{ end || '날짜 선택' }}</b></button>
        </div>
        <div class="calendar-head"><button>‹</button><strong>2026년 8월</strong><button>›</button></div>
        <div class="weekdays"><span v-for="day in ['일','월','화','수','목','금','토']" :key="day">{{ day }}</span></div>
        <div class="calendar-grid">
          <button
            v-for="day in days"
            :key="day"
            :disabled="day > 31"
            :class="{ selected: day === startDay || day === endDay, range: end && day > startDay && day < endDay }"
            @click="selectDay(day)"
          >{{ day <= 31 ? day : '' }}</button>
        </div>
        <p class="duration">{{ end ? `총 ${nights}박 ${nights + 1}일` : '도착일을 선택해 주세요' }}</p>
        <button class="confirm" :disabled="!end" @click="confirm">
          {{ end ? `${startDay}일 ~ ${endDay}일 선택 완료` : '날짜 선택 완료' }}
        </button>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.sheet-overlay { position:fixed; z-index:100; inset:0; display:flex; align-items:flex-end; justify-content:center; background:rgba(17,24,39,.46); }
.date-sheet { width:min(390px,100%); padding:25px 22px 24px; border-radius:28px 28px 0 0; background:#fff; box-shadow:0 -8px 30px rgba(15,23,42,.14); }
header { display:flex; justify-content:space-between; } h2 { font-size:19px; font-weight:800; } header p { margin-top:4px; color:#64748b; font-size:11px; } header button { border:0; background:none; color:#64748b; font-size:26px; }
.date-tickets { display:flex; align-items:center; gap:10px; margin-top:22px; }.date-tickets button { flex:1; padding:10px 12px; text-align:left; border:1px solid #cbd9ee; border-left:4px solid #0066ff; border-radius:14px; background:#fff; }.date-tickets small,.date-tickets b { display:block; }.date-tickets small { color:#64748b; font-size:9px; }.date-tickets b { margin-top:8px; color:#0066ff; font-size:12px; }.date-tickets span { color:#94a3b8; }
.calendar-head { display:flex; justify-content:space-between; align-items:center; margin-top:25px; }.calendar-head button { border:0; background:none; font-size:22px; }.calendar-head strong { font-size:15px; }
.weekdays,.calendar-grid { display:grid; grid-template-columns:repeat(7,1fr); }.weekdays { margin-top:15px; }.weekdays span { text-align:center; color:#64748b; font-size:10px; }.weekdays span:first-child { color:#e5484d; }.weekdays span:last-child { color:#0066ff; }
.calendar-grid { margin-top:8px; row-gap:8px; }.calendar-grid button { position:relative; height:34px; border:0; background:transparent; font-size:11px; }.calendar-grid button.range { background:#e6f0ff; }.calendar-grid button.selected { z-index:1; border-radius:50%; color:#fff; background:#0066ff; font-weight:800; }
.duration { margin:14px 0 10px; color:#64748b; text-align:center; font-size:10px; }.confirm { width:100%; height:54px; border:0; border-radius:14px; color:#fff; background:#263f8c; font-weight:800; }.confirm:disabled { background:#cbd5e1; }
</style>
