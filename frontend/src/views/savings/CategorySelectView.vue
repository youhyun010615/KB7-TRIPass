<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMonthlyFundStore } from '@/stores/monthlyFund'

const route = useRoute()
const router = useRouter()
const fund = useMonthlyFundStore()
const transaction = computed(() => fund.transactions.find((item) => item.id === Number(route.params.transactionId)))
const selected = ref(transaction.value?.categoryId || 'food')

function applyCategory() {
  if (!transaction.value || !fund.updateTransactionCategory(transaction.value.id, selected.value)) return
  router.back()
}
</script>

<template>
  <main class="select-page">
    <header><div><h1>카테고리 선택</h1><p>거래에 맞는 카테고리를 선택해 주세요.</p></div><button @click="router.back()">×</button></header>
    <section>
      <label v-for="item in fund.categories" :key="item.id" :class="{ selected: selected === item.id }">
        <span class="icon" :style="{ background: `${item.color}18` }">{{ item.icon }}</span>
        <span><b>{{ item.name }}</b><small>{{ item.description }}</small></span>
        <input v-model="selected" type="radio" :value="item.id">
      </label>
      <button class="prepaid" @click="router.push('/savings/monthly/prepaid/new')"><span>✈️</span><span><b>여행비 사전 지출</b><small>항공권 등 여행을 위해 사전에 지출하는 금액</small></span><i>›</i></button>
    </section>
    <button class="cta" @click="applyCategory">적용하기</button>
  </main>
</template>

<style scoped>
.select-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:68px 20px 24px;background:#fff;color:#151f33}header{display:flex;align-items:start;justify-content:space-between}header h1{font-size:22px;font-weight:900}header p{margin-top:10px;color:#64748b;font-size:11px}header button{font-size:28px;color:#64748b}section{margin-top:28px}label,.prepaid{display:grid;grid-template-columns:42px 1fr 24px;align-items:center;gap:10px;width:100%;margin-bottom:10px;padding:13px 12px;border:1px solid #e5eaf2;border-radius:14px;background:white;box-shadow:0 4px 10px #1e34620b;text-align:left}.selected{border-color:#86b7ff;background:#eef7ff}.icon{display:grid;width:38px;height:38px;place-items:center;border-radius:50%}label b,label small,.prepaid b,.prepaid small{display:block}label b,.prepaid b{font-size:13px}label small,.prepaid small{margin-top:4px;color:#64748b;font-size:9px}input{width:20px;height:20px;accent-color:#0758c8}.prepaid i{text-align:center;color:#64748b;font-size:22px}.cta{position:sticky;bottom:18px;width:100%;height:54px;margin-top:12px;border-radius:12px;background:#173d89;color:white;font-size:15px;font-weight:900}
</style>
