<script setup>
import { computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAssetStore } from '@/stores/asset'

const router = useRouter()
const asset = useAssetStore()
const form = reactive({ name:'', day:1, amount:'', accountId:asset.accounts[0]?.id, alert:'당일', memo:'' })
const amountNumber = computed(() => Number(String(form.amount).replace(/[^0-9]/g,'')) || 0)
const valid = computed(() => form.name.trim() && form.day >= 1 && form.day <= 31 && amountNumber.value > 0 && form.accountId)
function formatAmount(event){ form.amount = (Number(event.target.value.replace(/[^0-9]/g,'')) || '').toLocaleString('ko-KR') }
function submit(){ if(valid.value && asset.addFixedExpense(form)) router.push('/asset/fixed-expenses') }
</script>

<template><main class="page"><div class="shell">
  <header><button @click="router.back()">‹</button><h1>고정지출 등록</h1></header>
  <div class="steps"><i></i><i></i><i></i></div>
  <h2>매달 정해진 날짜에 나가는<br>고정 지출을 등록해 주세요</h2>
  <section class="card">
    <label>항목명<input v-model="form.name" placeholder="예: 월세, 통신비" /></label>
    <div class="row"><label>반복 예정일<input v-model.number="form.day" type="number" min="1" max="31" /></label><label>예정 금액<input :value="form.amount" inputmode="numeric" placeholder="0" @input="formatAmount" /></label></div>
    <label>연결 계좌<select v-model="form.accountId"><option v-for="account in asset.accounts" :key="account.id" :value="account.id">{{ account.name }} {{ account.number }}</option></select></label>
    <label>알림 시점<select v-model="form.alert"><option>당일</option><option>1일 전</option><option>3일 전</option><option>7일 전</option></select></label>
    <label>메모 (선택)<textarea v-model="form.memo" placeholder="메모를 입력해 주세요" maxlength="100"></textarea><small>{{ form.memo.length }}/100</small></label>
  </section>
  <p>여행 저축 금액은 매월 1일 일정으로 자동 반영돼요.</p>
  <button class="cta" :disabled="!valid" @click="submit">저장하기</button>
</div></main></template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{position:relative;width:min(100%,390px);min-height:100vh;margin:auto;padding:52px 18px 100px;background:#f7f5ef}header{display:flex;align-items:center}header button{font-size:25px}h1{flex:1;text-align:center;font-size:18px;font-weight:900;padding-right:20px}.steps{display:flex;gap:8px;margin:14px 0 22px}.steps i{height:3px;flex:1;background:#d8e1f2}.steps i:first-child{background:#173f8d}h2{font-size:21px;line-height:1.45}.card{margin-top:18px;padding:16px;border:1px solid #e2e7ef;border-radius:16px;background:#fff}.card label{display:block;margin-bottom:14px;color:#536174;font-size:10px;font-weight:800}.card input,.card select,.card textarea{width:100%;margin-top:7px;padding:13px;border:1px solid #dce3ed;border-radius:10px;background:#fff;color:#111827;font:inherit}.card textarea{height:76px;resize:none}.card small{display:block;margin-top:4px;text-align:right;color:#a0a9b7}.row{display:grid;grid-template-columns:1fr 1fr;gap:10px}.shell>p{margin-top:12px;padding:11px;border-radius:10px;background:#eef3ff;color:#5370aa;font-size:9px}.cta{position:absolute;right:18px;bottom:28px;left:18px;padding:15px;border-radius:12px;background:#173f8d;color:#fff;font-weight:900}.cta:disabled{background:#aeb9ca}
</style>
