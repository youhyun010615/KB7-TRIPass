<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({ open: Boolean, plan: { type: Object, default: null } })
const emit = defineEmits(['close', 'confirm'])

const today = new Date()
const todayIso = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
const start = ref('')
const end = ref('')

watch(() => props.plan, (plan) => {
  start.value = plan?.startDate || ''
  end.value = plan?.endDate || ''
}, { immediate: true })

const minimumEndDate = computed(() => {
  if (!start.value) return todayIso
  const date = new Date(`${start.value}T00:00:00`)
  date.setDate(date.getDate() + 1)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
})

const nights = computed(() => {
  if (!start.value || !end.value) return 0
  return Math.max(0, Math.round((new Date(`${end.value}T00:00:00`) - new Date(`${start.value}T00:00:00`)) / 86400000))
})

const isValid = computed(() => Boolean(start.value && end.value && nights.value > 0))

watch(start, () => {
  if (end.value && end.value < minimumEndDate.value) end.value = ''
})

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
          <div><h2 id="date-sheet-title">여행 날짜 선택</h2><p>해당 국가의 도착일과 출발일을 선택해 주세요.</p></div>
          <button aria-label="닫기" @click="emit('close')">×</button>
        </header>
        <div class="route-visual"><i /><span>✈</span><i /></div>
        <div class="date-fields">
          <label>
            <span>도착일</span>
            <input v-model="start" type="date" :min="todayIso">
          </label>
          <b>→</b>
          <label>
            <span>출발일</span>
            <input v-model="end" type="date" :min="minimumEndDate" :disabled="!start">
          </label>
        </div>
        <section class="date-guide">
          <span>📅</span>
          <div v-if="isValid"><b>총 {{ nights }}박 {{ nights + 1 }}일</b><p>{{ start.replaceAll('-', '.') }}부터 {{ end.replaceAll('-', '.') }}까지</p></div>
          <div v-else><b>여행 기간을 입력해 주세요</b><p>출발일은 도착일보다 하루 이상 늦어야 해요.</p></div>
        </section>
        <button class="confirm" :disabled="!isValid" @click="confirm">이 일정으로 선택하기</button>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.sheet-overlay{position:fixed;z-index:100;inset:0;display:flex;align-items:flex-end;justify-content:center;background:rgba(17,24,39,.46)}.date-sheet{width:min(390px,100%);padding:25px 22px 24px;border-radius:28px 28px 0 0;background:#fff;box-shadow:0 -8px 30px rgba(15,23,42,.14)}header{display:flex;justify-content:space-between}h2{font-size:19px;font-weight:800}header p{margin-top:4px;color:#64748b;font-size:11px}header button{border:0;background:none;color:#64748b;font-size:26px}.route-visual{display:flex;align-items:center;justify-content:center;gap:8px;margin:22px 0 17px;color:#2469e8}.route-visual i{width:80px;border-top:1px dashed #9db9e9}.route-visual span{font-size:24px;transform:rotate(8deg)}.date-fields{display:grid;grid-template-columns:1fr 20px 1fr;align-items:end;gap:7px}.date-fields>b{padding-bottom:14px;color:#94a3b8;text-align:center}.date-fields label{display:flex;flex-direction:column;gap:7px;color:#52637c;font-size:10px;font-weight:700}.date-fields input{min-width:0;width:100%;height:47px;padding:0 9px;border:1px solid #cbd9ee;border-left:4px solid #2469e8;border-radius:12px;color:#173b86;background:#fff;font-size:11px;font-weight:700}.date-fields input:disabled{border-color:#e2e8f0;color:#94a3b8;background:#f8fafc}.date-guide{display:flex;align-items:center;gap:11px;margin-top:18px;padding:13px;border-radius:13px;background:#f1f6ff}.date-guide>span{font-size:22px}.date-guide b{color:#173b86;font-size:11px}.date-guide p{margin-top:3px;color:#64748b;font-size:9px}.confirm{width:100%;height:54px;margin-top:15px;border:0;border-radius:14px;color:#fff;background:#263f8c;font-weight:800}.confirm:disabled{background:#cbd5e1}
</style>
