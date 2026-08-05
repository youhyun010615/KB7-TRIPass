<script setup>
import { computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelScheduleStore } from '@/stores/travelSchedule'

const route = useRoute()
const router = useRouter()
const store = useTravelScheduleStore()
const editing = computed(() => Boolean(route.params.scheduleId))
const original = editing.value ? store.getSchedule(route.params.scheduleId) : null
const form = reactive(original ? { ...original } : {
  title:'', date:store.travelStart, time:'10:00', currency:'EUR', amount:0, paymentStatus:'prepaid', place:'', memo:'',
})
const country = computed(() => store.countryForDate(form.date))
const valid = computed(() => form.title.trim() && country.value && form.time && form.currency && Number(form.amount) >= 0 && form.place.trim())
const error = computed(() => form.date && !country.value ? '등록된 여행 기간 안의 날짜를 선택해 주세요.' : '')
const currencies = ['EUR','USD','CHF','JPY','HKD']

function applyDate() {
  if (country.value) form.currency = country.value.currency
}
function submit() {
  if (!valid.value) return
  const payload = { ...form, alert:'1시간 전', amount:Number(form.amount), title:form.title.trim(), place:form.place.trim(), memo:form.memo.trim() }
  const success = editing.value ? store.update(route.params.scheduleId, payload) : store.save(payload)
  if (success) router.push(editing.value ? `/schedule/${route.params.scheduleId}` : '/schedule')
}
</script>

<template>
  <main class="form-page">
    <header><button type="button" @click="router.back()">‹</button><h1>여행일정 {{ editing ? '수정' : '추가' }}</h1><span /></header>
    <section class="form-card">
      <label><span>▱ 일정명</span><input v-model="form.title" placeholder="예: 루브르 박물관 가이드 투어"></label>
      <label><span>▣ 일시</span><div class="split"><input v-model="form.date" type="date" :min="store.travelStart" :max="store.travelEnd" @change="applyDate"><input v-model="form.time" type="time"></div><small v-if="error" class="error">{{ error }}</small><small v-else-if="country">{{ country.flag }} {{ country.name }} 여행 일정</small></label>
      <label><span>₩ 금액</span><div class="money"><select v-model="form.currency"><option v-for="item in currencies" :key="item">{{ item }}</option></select><input v-model.number="form.amount" type="number" min="0"><em>{{ form.currency }}</em></div></label>
      <div class="payment"><button type="button" :class="{ active:form.paymentStatus==='prepaid' }" @click="form.paymentStatus='prepaid'">💳 사전결제 완료</button><button type="button" :class="{ active:form.paymentStatus==='onsite' }" @click="form.paymentStatus='onsite'">💵 현장결제 필요</button></div>
      <label><span>📍 장소/주소</span><input v-model="form.place" placeholder="장소 또는 주소를 입력해 주세요"></label>
      <div class="auto-alert"><span>🔔</span><div><b>일정 1시간 전에 알려드려요</b><small>모든 여행 일정에 자동으로 적용돼요.</small></div><em>자동</em></div>
      <label><span>📝 메모</span><textarea v-model="form.memo" maxlength="100" placeholder="일정에 필요한 내용을 메모해 주세요."/><small>{{ form.memo.length }}/100</small></label>
    </section>
    <button class="submit" :disabled="!valid" type="button" @click="submit">{{ editing ? '수정 완료' : '등록하기' }}</button>
    <BottomNav />
  </main>
</template>

<style scoped>
.form-page{min-height:100vh;padding:0 16px 150px;background:#f8f6f1;color:#10192d}.form-page>header{display:grid;grid-template-columns:32px 1fr 32px;align-items:end;height:78px;padding-bottom:13px}.form-page>header button{font-size:28px;text-align:left}.form-page h1{text-align:center;font-size:17px;font-weight:900}.form-card{overflow:hidden;border:1px solid #dce4ee;border-radius:16px;background:#fff}.form-card>label{display:block;padding:14px;border-bottom:1px solid #edf0f4}.form-card label>span{display:block;margin-bottom:8px;font-size:10px;font-weight:900}.form-card input,.form-card select,.form-card textarea{width:100%;padding:11px;border:1px solid #e0e6ef;border-radius:9px;background:#fff;font-size:10px;outline:none}.form-card input:focus,.form-card select:focus,.form-card textarea:focus{border-color:#3477e9}.split{display:grid;grid-template-columns:1fr 90px;gap:7px}.money{display:grid;grid-template-columns:75px 1fr 40px;align-items:center;gap:6px}.money em{color:#64748b;font-size:9px;font-style:normal}.form-card textarea{height:78px;resize:none}.form-card label>small{display:block;margin-top:6px;color:#6480a7;font-size:8px;text-align:right}.form-card label>.error{color:#e5484d;text-align:left}.payment{display:grid;grid-template-columns:1fr 1fr;gap:7px;padding:13px;border-bottom:1px solid #edf0f4}.payment button{padding:10px;border:1px solid #dfe5ed;border-radius:9px;color:#64748b;font-size:9px}.payment button.active{border-color:#3477e9;background:#eef4ff;color:#246dd7;font-weight:900}.submit{position:fixed;right:max(calc((100vw - 390px)/2 + 16px),16px);bottom:78px;left:max(calc((100vw - 390px)/2 + 16px),16px);z-index:40;height:52px;border-radius:13px;background:#19489c;color:#fff;font-size:13px;font-weight:900}.submit:disabled{background:#a7b2c6}
.auto-alert{display:grid;grid-template-columns:36px 1fr auto;align-items:center;gap:10px;padding:14px;border-bottom:1px solid #edf0f4;background:#f6f9ff}.auto-alert>span{display:grid;width:34px;height:34px;place-items:center;border-radius:50%;background:#e7f1ff}.auto-alert b,.auto-alert small{display:block}.auto-alert b{font-size:11px}.auto-alert small{margin-top:4px;color:#72819a;font-size:8px}.auto-alert em{padding:5px 8px;border-radius:9px;background:#e7f1ff;color:#2670dc;font-size:8px;font-style:normal;font-weight:800}
</style>
