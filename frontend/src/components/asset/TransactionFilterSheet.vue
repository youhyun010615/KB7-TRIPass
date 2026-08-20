<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  startDate: { type: String, required: true },
  endDate: { type: String, required: true },
  period: { type: String, default: '3개월' },
  type: { type: String, default: 'all' },
  sort: { type: String, default: 'latest' },
  supportsType: { type: Boolean, default: false },
})
const emit = defineEmits(['apply'])

const open = ref(false)
const draftStart = ref(props.startDate)
const draftEnd = ref(props.endDate)
const draftPeriod = ref(props.period)
const draftType = ref(props.type)
const draftSort = ref(props.sort)

watch(() => [props.startDate, props.endDate, props.period, props.type, props.sort], () => {
  draftStart.value = props.startDate
  draftEnd.value = props.endDate
  draftPeriod.value = props.period
  draftType.value = props.type
  draftSort.value = props.sort
})

function localDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function choosePeriod(months, label) {
  draftPeriod.value = label
  if (!months) return
  const end = new Date(`${draftEnd.value}T00:00:00`)
  const start = new Date(end.getFullYear(), end.getMonth() - months, end.getDate() + 1)
  draftStart.value = localDate(start)
}

function chooseCustom() {
  draftPeriod.value = '직접입력'
}

function apply() {
  emit('apply', {
    startDate: draftStart.value,
    endDate: draftEnd.value,
    period: draftPeriod.value,
    type: draftType.value,
    sort: draftSort.value,
  })
  open.value = false
}
</script>

<template>
  <section class="filter-summary">
    <div class="filter-summary-top">
      <button type="button" class="condition-button" @click="open = true">
        <b>{{ period }}</b><i></i><b>{{ supportsType ? ({ all: '전체', deposit: '입금', withdrawal: '출금' }[type]) : '전체' }}</b><i></i><b>{{ sort === 'latest' ? '최신순' : '과거순' }}</b><span>☷</span>
      </button>
    </div>
    <div class="filter-summary-bottom">
      <strong>{{ startDate.replaceAll('-', '.') }}~{{ endDate.replaceAll('-', '.') }}</strong>
      <button type="button" @click="open = true">조회조건 변경</button>
    </div>
  </section>

  <Teleport to="body">
    <Transition name="sheet">
      <div v-if="open" class="filter-backdrop" @click.self="open = false">
        <section class="filter-sheet" role="dialog" aria-modal="true" aria-label="조회조건 선택">
          <span class="handle"></span>
          <h2>조회조건 선택</h2>

          <div class="filter-group">
            <h3>조회기간</h3>
            <div class="period-options">
              <button v-for="option in [{ label: '1개월', months: 1 }, { label: '3개월', months: 3 }, { label: '6개월', months: 6 }]" :key="option.label" type="button" :class="{ active: draftPeriod === option.label }" @click="choosePeriod(option.months, option.label)">{{ option.label }}</button>
              <button type="button" :class="{ active: draftPeriod === '직접입력' }" @click="chooseCustom">직접입력</button>
            </div>
            <div class="date-range" :class="{ active: draftPeriod === '직접입력' }">
              <label><input v-model="draftStart" type="date" :max="draftEnd" @change="chooseCustom"></label>
              <span>~</span>
              <label><input v-model="draftEnd" type="date" :min="draftStart" @change="chooseCustom"></label>
            </div>
            <p>최근 2년 이내의 거래내역만 조회됩니다.</p>
          </div>

          <div v-if="supportsType" class="filter-group">
            <h3>거래구분</h3>
            <div class="type-options">
              <button v-for="option in [{ id: 'all', label: '전체' }, { id: 'deposit', label: '입금' }, { id: 'withdrawal', label: '출금' }]" :key="option.id" type="button" :class="{ active: draftType === option.id }" @click="draftType = option.id">{{ option.label }}</button>
            </div>
          </div>

          <div class="filter-group">
            <h3>정렬순서</h3>
            <div class="sort-options">
              <button type="button" :class="{ active: draftSort === 'latest' }" @click="draftSort = 'latest'">최신순</button>
              <button type="button" :class="{ active: draftSort === 'oldest' }" @click="draftSort = 'oldest'">과거순</button>
            </div>
          </div>

          <div class="sheet-actions"><button type="button" @click="open = false">취소</button><button type="button" @click="apply">확인</button></div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.filter-summary{margin:14px 0 12px;padding:12px 2px 11px;border-bottom:1px solid #e5eaf1}.filter-summary-top,.filter-summary-bottom{display:flex;align-items:center;justify-content:space-between}.filter-summary-top{justify-content:flex-end}.condition-button{display:flex;align-items:center;color:#5f6f84;font-size:9.5px;font-weight:800}.condition-button i{height:11px;margin:0 6px;border-left:1px solid #cbd3df}.condition-button span{margin-left:6px;color:#173f8d;font-size:12px}.filter-summary-bottom{margin-top:15px}.filter-summary-bottom strong{color:#5f6f84;font-size:10px;font-weight:700}.filter-summary-bottom button{color:#286ce0;font-size:8.5px;font-weight:800}
.filter-backdrop{position:fixed;inset:0;z-index:200;display:flex;align-items:flex-end;justify-content:center;background:rgba(15,23,42,.52)}.filter-sheet{width:min(100%,430px);max-height:92vh;overflow-y:auto;padding:10px 20px 24px;border-radius:25px 25px 0 0;background:#fff;color:#1d2533}.handle{display:block;width:38px;height:4px;margin:0 auto 18px;border-radius:99px;background:#d9dee7}.filter-sheet h2{font-size:20px;font-weight:900}.filter-group{margin-top:22px}.filter-group h3{margin-bottom:11px;font-size:14px;font-weight:900}.period-options,.type-options,.sort-options{display:grid;gap:8px}.period-options{grid-template-columns:repeat(4,1fr)}.type-options{grid-template-columns:repeat(3,1fr)}.sort-options{grid-template-columns:repeat(2,1fr)}.filter-group button{height:45px;border:1px solid #d4dae3;border-radius:13px;color:#526072;background:#fff;font-size:11px;font-weight:700}.filter-group button.active{border:2px solid #173f8d;color:#173f8d;background:#f3f7ff;font-weight:900}.date-range{display:grid;grid-template-columns:1fr auto 1fr;align-items:center;gap:8px;margin-top:10px;padding:12px;border:1px solid #d4dae3;border-radius:13px}.date-range.active{border-color:#173f8d;box-shadow:0 0 0 2px rgba(23,63,141,.08)}.date-range input{width:100%;min-width:0;font-size:9px;font-weight:700}.date-range span{color:#7d8796}.filter-group p{margin-top:7px;color:#9aa3b1;font-size:8px}.sheet-actions{display:grid;grid-template-columns:1fr 2fr;gap:9px;margin-top:27px}.sheet-actions button{height:48px;border-radius:14px;background:#eaf0f7;color:#173f8d;font-size:13px;font-weight:900}.sheet-actions button:last-child{background:#173f8d;color:#fff}.sheet-enter-active,.sheet-leave-active{transition:opacity .2s}.sheet-enter-active .filter-sheet,.sheet-leave-active .filter-sheet{transition:transform .23s}.sheet-enter-from,.sheet-leave-to{opacity:0}.sheet-enter-from .filter-sheet,.sheet-leave-to .filter-sheet{transform:translateY(100%)}
</style>
