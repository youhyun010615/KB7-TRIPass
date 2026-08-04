<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMonthlyFundStore } from '@/stores/monthlyFund'

const route = useRoute()
const router = useRouter()
const fund = useMonthlyFundStore()
const category = computed(() => fund.categorySummary(route.params.categoryId) || fund.categorySummaries[0])
const transactions = computed(() => fund.transactions.filter((item) => item.categoryId === category.value.id))
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
</script>

<template>
  <main class="detail-page">
    <header><button @click="router.back()">‹</button><h1>{{ category.name }} 상세</h1></header>
    <section class="category-summary" :style="{ '--accent': category.color }">
      <div class="category-title"><span>{{ category.icon }}</span><b>{{ category.name }}</b></div>
      <div class="numbers">
        <span>목표 금액<b>{{ money(category.target) }}</b></span>
        <span>사용 금액<b>{{ money(category.spent) }}</b></span>
        <span>남은 금액<b>{{ money(category.remaining) }}</b></span>
        <span>사용률<b>{{ category.percent }}%</b></span>
      </div>
      <div class="progress"><i :style="{ width: `${category.percent}%` }" /></div>
    </section>
    <h2><small>이번 달</small> 거래 내역 <b>{{ transactions.length }}건</b></h2>
    <section class="transaction-list">
      <button v-for="item in transactions" :key="item.id" @click="router.push(`/savings/monthly/transactions/${item.id}/category`)">
        <span class="merchant-icon">{{ item.icon }}</span>
        <span class="merchant"><small>{{ item.date }}</small><b>{{ item.merchant }}</b></span>
        <span class="amount"><b>{{ money(item.amount) }}</b><small>✎ 분류 수정 ›</small></span>
      </button>
      <p v-if="!transactions.length" class="empty">이번 달 거래 내역이 없어요.</p>
    </section>
  </main>
</template>

<style scoped>
.detail-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 16px 30px;background:#fff;color:#151f33}header{display:grid;grid-template-columns:30px 1fr;align-items:center}header button{font-size:28px;text-align:left}h1{font-size:19px;font-weight:900}.category-summary{margin-top:17px;padding:17px 14px;border:1px solid color-mix(in srgb,var(--accent) 20%,white);border-radius:17px;background:color-mix(in srgb,var(--accent) 7%,white);box-shadow:0 7px 16px #1e346212}.category-title{display:flex;align-items:center;gap:10px;font-size:16px}.category-title span{display:grid;width:38px;height:38px;place-items:center;border-radius:50%;background:#ffffffaa}.numbers{display:grid;grid-template-columns:repeat(4,1fr);margin-top:17px}.numbers span{text-align:center;color:#64748b;font-size:9px}.numbers span+span{border-left:1px solid #e2e8f0}.numbers b{display:block;margin-top:6px;color:#202535;font-size:12px;white-space:nowrap}.numbers span:not(:first-child) b{color:var(--accent)}.progress{height:5px;margin-top:18px;overflow:hidden;border-radius:9px;background:#e4e8ed}.progress i{display:block;height:100%;border-radius:inherit;background:var(--accent)}h2{margin:30px 4px 12px;font-size:16px;font-weight:900}h2 small{margin-right:8px;color:#64748b;font-size:10px;font-weight:500}h2 b{float:right;color:#94a3b8;font-size:10px}.transaction-list{padding:12px;border:1px solid #e5eaf2;border-radius:20px;background:#f8fafc}.transaction-list button{display:grid;grid-template-columns:42px 1fr auto;align-items:center;width:100%;margin-bottom:9px;padding:12px;border:1px solid #e5eaf2;border-radius:13px;background:white;box-shadow:0 4px 10px #1e34620d;text-align:left}.merchant-icon{display:grid;width:34px;height:34px;place-items:center;border-radius:50%;background:#f1eaff}.merchant small,.merchant b,.amount b,.amount small{display:block}.merchant small{color:#94a3b8;font-size:9px}.merchant b{margin-top:4px;font-size:13px}.amount{text-align:right}.amount b{font-size:13px}.amount small{margin-top:5px;color:#64748b;font-size:9px}.empty{padding:40px;text-align:center;color:#94a3b8;font-size:12px}
</style>
