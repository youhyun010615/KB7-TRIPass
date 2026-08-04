<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMonthlyFundStore } from '@/stores/monthlyFund'
import { useTravelStore } from '@/stores/travel'

const router = useRouter()
const fund = useMonthlyFundStore()
const travel = useTravelStore()
const form = reactive({ name: '', countryCode: travel.selectedCountryCodes[0] || '', date: '', amount: '', memo: '' })
const touched = ref(false)
const amountNumber = computed(() => Number(String(form.amount).replace(/[^0-9]/g, '')) || 0)
const valid = computed(() => form.name.trim() && form.countryCode && form.date && amountNumber.value > 0)

function formatAmount(event) {
  form.amount = (Number(event.target.value.replace(/[^0-9]/g, '')) || '').toLocaleString('ko-KR')
}

function submit() {
  touched.value = true
  if (!valid.value || !fund.addPrepaidExpense(form)) return
  router.push('/asset/prepaid')
}
</script>

<template>
  <main class="prepaid-page">
    <header><button @click="router.back()">‹</button><div><h1>사전 지불 금액 등록</h1><p>여행을 위해 미리 지불한 항목 정보를 입력해 주세요.</p></div></header>
    <section class="field" :class="{ error: touched && !form.name.trim() }"><div class="label"><span>▦</span><b>항목명 <i>자동</i></b><small>예: 항공권, 호텔, 투어 등</small></div><input v-model="form.name" placeholder="항목명을 입력해 주세요"></section>
    <section class="field" :class="{ error: touched && !form.countryCode }"><div class="label"><span>◎</span><b>국가</b><small>공통 또는 여행 목표 국가를 선택해 주세요.</small></div><select v-model="form.countryCode"><option value="" disabled>국가를 선택해 주세요</option><option value="COMMON">🌍 공통 항목</option><option v-for="item in travel.countries" :key="item.code" :value="item.code">{{ item.flag }} {{ item.name }} · {{ item.city }}</option></select></section>
    <section class="field" :class="{ error: touched && !form.date }"><div class="label"><span>□</span><b>지출일 <i>자동</i></b></div><input v-model="form.date" type="date"></section>
    <section class="field amount" :class="{ error: touched && !amountNumber }"><div class="label"><span>₩</span><b>금액 <i>자동</i></b></div><div class="money-input"><input :value="form.amount" inputmode="numeric" placeholder="금액을 입력해 주세요" @input="formatAmount"><b>원</b></div></section>
    <section class="field"><div class="label"><span>▤</span><b>메모 (선택)</b></div><textarea v-model="form.memo" maxlength="100" placeholder="메모를 입력해 주세요 (선택사항)"/><small class="count">{{ form.memo.length }}/100</small></section>
    <p v-if="touched && !valid" class="error-copy">필수 정보를 모두 입력해 주세요.</p>
    <button class="cta" :disabled="!valid" @click="submit">등록하기</button>
  </main>
</template>

<style scoped>
.prepaid-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 14px 24px;background:#f7f4ee;color:#151f33}header{display:grid;grid-template-columns:30px 1fr;align-items:start;margin-bottom:20px}header button{font-size:28px;text-align:left}h1{font-size:19px;font-weight:900}header p{margin-top:6px;color:#94a3b8;font-size:9px}.field{position:relative;margin-bottom:10px;padding:14px;border:1px solid #dfe6f1;border-radius:16px;background:white;box-shadow:0 4px 10px #1e34620b}.field:focus-within{border-color:#86b7ff}.field.error{border-color:#f29a9a;background:#fffafa}.label{display:grid;grid-template-columns:25px 1fr auto;align-items:center;margin-bottom:12px}.label>span{color:#3181ff}.label b{font-size:12px}.label i{margin-left:8px;color:#ef4444;font-size:10px}.label small{color:#64748b;font-size:8px}.field input,.field select,.field textarea,.money-input{width:100%;border:1px solid #d9e2f0;border-radius:11px;background:white;color:#334155;font-size:11px;outline:none}.field>input,.field select{height:48px;padding:0 13px}.field textarea{height:78px;padding:13px;resize:none}.money-input{display:flex;align-items:center;padding:0 12px}.money-input input{height:46px;border:0}.money-input b{font-size:11px}.amount{border-color:#cdeedd;background:#fbfffd}.count{position:absolute;right:24px;bottom:23px;color:#94a3b8;font-size:8px}.error-copy{margin:4px 4px 10px;color:#e5484d;font-size:10px}.cta{width:100%;height:54px;margin-top:18px;border-radius:12px;background:#174494;color:white;font-size:14px;font-weight:900}.cta:disabled{background:#aab5c7}
</style>
