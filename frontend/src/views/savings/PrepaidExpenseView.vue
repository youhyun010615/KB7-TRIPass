<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAssetStore } from '@/stores/asset'
import { useMonthlyFundStore } from '@/stores/monthlyFund'
import { useTravelStore } from '@/stores/travel'

const router = useRouter()
const route = useRoute()
const asset = useAssetStore()
const fund = useMonthlyFundStore()
const travel = useTravelStore()
const transaction = computed(() => fund.transactions.find((item) => item.id === Number(route.query.transactionId)))
const normalizedDate = computed(() => {
  const match = transaction.value?.date?.match(/(\d{1,2})\/(\d{1,2})/)
  return match ? `2026-${match[1].padStart(2, '0')}-${match[2].padStart(2, '0')}` : ''
})
const form = reactive({ name: transaction.value?.merchant || '', countryCode: travel.selectedCountryCodes[0] || '', date: normalizedDate.value, amount: transaction.value ? Number(transaction.value.amount).toLocaleString('ko-KR') : '', memo: '' })
const touched = ref(false)
const amountNumber = computed(() => Number(String(form.amount).replace(/[^0-9]/g, '')) || 0)
const valid = computed(() => form.name.trim() && form.countryCode && form.date && amountNumber.value > 0)

function formatAmount(event) {
  form.amount = (Number(event.target.value.replace(/[^0-9]/g, '')) || '').toLocaleString('ko-KR')
}

function submit() {
  touched.value = true
  if (!transaction.value || !valid.value || !asset.addPrepaidExpense({ ...form, icon: resolveIcon(form.name) })) return
  fund.updateTransactionCategory(transaction.value.id, 'prepaid')
  asset.selectedPrepaidScope = form.countryCode
  router.push('/asset/prepaid')
}

function resolveIcon(name) {
  if (/항공|비행/.test(name)) return '✈️'
  if (/보험/.test(name)) return '◇'
  if (/호텔|숙박/.test(name)) return '▦'
  if (/교통|픽업/.test(name)) return '▣'
  if (/투어|액티비티/.test(name)) return '●'
  return '◆'
}
</script>

<template>
  <main class="prepaid-page">
    <header><button @click="router.back()">‹</button><div><h1>사전 지불 금액 등록</h1><p>여행을 위해 미리 지불한 항목 정보를 입력해 주세요.</p></div></header>
    <section class="field auto" :class="{ error: touched && !form.name.trim() }"><div class="label"><span>▦</span><b>항목명 <i>자동</i></b><small>거래내역에서 자동으로 불러왔어요.</small></div><input v-model="form.name" readonly></section>
    <section class="field" :class="{ error: touched && !form.countryCode }"><div class="label"><span>◎</span><b>국가</b><small>공통 또는 여행 목표 국가를 선택해 주세요.</small></div><select v-model="form.countryCode"><option value="" disabled>국가를 선택해 주세요</option><option value="COMMON">🌍 공통 항목</option><option v-for="item in travel.countries" :key="item.code" :value="item.code">{{ item.flag }} {{ item.name }} · {{ item.city }}</option></select></section>
    <section class="field auto" :class="{ error: touched && !form.date }"><div class="label"><span>□</span><b>지출일 <i>자동</i></b></div><input v-model="form.date" type="date" readonly></section>
    <section class="field amount auto" :class="{ error: touched && !amountNumber }"><div class="label"><span>₩</span><b>금액 <i>자동</i></b></div><div class="money-input"><input :value="form.amount" inputmode="numeric" readonly><b>원</b></div></section>
    <section class="field"><div class="label"><span>▤</span><b>메모 (선택)</b></div><textarea v-model="form.memo" maxlength="100" placeholder="메모를 입력해 주세요 (선택사항)"/><small class="count">{{ form.memo.length }}/100</small></section>
    <p v-if="touched && !valid" class="error-copy">필수 정보를 모두 입력해 주세요.</p>
    <p v-if="!transaction" class="error-copy">거래내역을 통해서만 사전 지출로 등록할 수 있어요.</p>
    <button class="cta" :disabled="!transaction || !valid" @click="submit">등록하기</button>
  </main>
</template>

<style scoped>
.prepaid-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 14px 24px;background:#f7f4ee;color:#151f33}header{display:grid;grid-template-columns:30px 1fr;align-items:start;margin-bottom:20px}header button{font-size:28px;text-align:left}h1{font-size:19px;font-weight:900}header p{margin-top:6px;color:#94a3b8;font-size:9px}.field{position:relative;margin-bottom:10px;padding:14px;border:1px solid #dfe6f1;border-radius:16px;background:white;box-shadow:0 4px 10px #1e34620b}.field:focus-within{border-color:#86b7ff}.field.error{border-color:#f29a9a;background:#fffafa}.field.auto{background:#f8fbff}.label{display:grid;grid-template-columns:25px 1fr auto;align-items:center;margin-bottom:12px}.label>span{color:#3181ff}.label b{font-size:12px}.label i{margin-left:8px;color:#ef4444;font-size:10px}.label small{color:#64748b;font-size:8px}.field input,.field select,.field textarea,.money-input{width:100%;border:1px solid #d9e2f0;border-radius:11px;background:white;color:#334155;font-size:11px;outline:none}.field>input,.field select{height:48px;padding:0 13px}.field input:read-only{color:#536174;background:#f8fafc}.field textarea{height:78px;padding:13px;resize:none}.money-input{display:flex;align-items:center;padding:0 12px}.money-input input{height:46px;border:0}.money-input b{font-size:11px}.amount{border-color:#cdeedd;background:#fbfffd}.count{position:absolute;right:24px;bottom:23px;color:#94a3b8;font-size:8px}.error-copy{margin:4px 4px 10px;color:#e5484d;font-size:10px}.cta{width:100%;height:54px;margin-top:18px;border-radius:12px;background:#174494;color:white;font-size:14px;font-weight:900}.cta:disabled{background:#aab5c7}
</style>
