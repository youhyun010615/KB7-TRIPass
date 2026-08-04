<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMonthlyFundStore } from '@/stores/monthlyFund'

const route = useRoute()
const router = useRouter()
const fund = useMonthlyFundStore()
const transaction = computed(() => fund.transactions.find((item) => item.id === Number(route.params.transactionId)))
const category = computed(() => transaction.value?.categoryId === 'prepaid'
  ? { name:'여행비 사전 지출', icon:'✈️', color:'#3475f4' }
  : fund.getCategory(transaction.value?.categoryId))
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
</script>

<template><main class="page"><div class="shell">
  <header><button @click="router.back()">‹</button><h1>거래내역 상세보기</h1></header>
  <template v-if="transaction">
    <section class="hero"><div><small>{{ transaction.date }}</small><h2>{{ transaction.merchant }}</h2><strong>-{{ money(transaction.amount) }}</strong></div><span :style="{background:`${category?.color || '#3475f4'}18`}">{{ category?.icon || transaction.icon }}</span></section>
    <section class="details"><dl><div><dt>거래일시</dt><dd>{{ transaction.date }}</dd></div><div><dt>카테고리</dt><dd class="category">{{ category?.name || '미분류' }}</dd></div><div><dt>거래구분</dt><dd>지출</dd></div><div><dt>결제수단</dt><dd>KB국민은행 여행통장<br>****4821</dd></div><div><dt>사용처</dt><dd>{{ transaction.merchant }}</dd></div><div><dt>거래 후 잔액</dt><dd>{{ money(5_200_000 - transaction.amount) }}</dd></div></dl></section>
    <h3>메모</h3><section class="memo">{{ transaction.memo || '등록된 메모가 없어요.' }}</section>
    <button class="cta" @click="router.push(`/savings/monthly/transactions/${transaction.id}/category`)">카테고리 변경하기</button>
  </template>
  <p v-else class="empty">거래내역을 찾을 수 없어요.</p>
</div></main></template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:auto;padding:52px 18px 30px;background:#f4f6fc}header{display:flex;align-items:center;margin-bottom:18px}header button{width:26px;font-size:26px;text-align:left}header h1{flex:1;padding-right:26px;text-align:center;font-size:18px;font-weight:900}.hero{display:flex;align-items:center;justify-content:space-between;padding:18px;border:1px solid #dce4ef;border-radius:17px;background:#fff}.hero small{color:#94a3b8;font-size:9px}.hero h2{margin-top:5px;font-size:14px}.hero strong{display:block;margin-top:8px;color:#e5484d;font-size:24px}.hero span{display:grid;width:50px;height:50px;place-items:center;border-radius:50%;font-size:20px}.details{margin-top:13px;padding:6px 16px;border:1px solid #dce4ef;border-radius:17px;background:#fff}.details dl>div{display:grid;grid-template-columns:92px 1fr;padding:13px 0;border-bottom:1px solid #edf0f5;font-size:11px}.details dl>div:last-child{border:0}.details dt{color:#94a3b8}.details dd{text-align:right;font-weight:800;line-height:1.5}.details .category{color:#3475f4}h3{margin:20px 3px 9px;font-size:12px}.memo{min-height:54px;padding:15px;border:1px solid #dce4ef;border-radius:13px;background:#fff;font-size:11px}.cta{width:100%;margin-top:18px;padding:15px;border-radius:12px;background:#173f8d;color:#fff;font-weight:900}.empty{padding:80px 0;text-align:center;color:#94a3b8}
</style>
