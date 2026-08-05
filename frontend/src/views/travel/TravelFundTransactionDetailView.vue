<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTravelFundStore } from '@/stores/travelFund'

const route = useRoute()
const router = useRouter()
const fund = useTravelFundStore()
const transaction = computed(() => fund.getTransaction(route.params.transactionId))
const category = computed(() => fund.getCategory(transaction.value?.categoryId))
const country = computed(() => fund.getCountry(transaction.value?.countryCode))
const money = value => `${Number(value || 0).toLocaleString('ko-KR')}원`
const dateTime = value => `${value.replaceAll('-', '.')} 12:30`
</script>

<template>
  <main class="page">
    <header><button type="button" @click="router.back()">‹</button><h1>거래내역 상세보기</h1></header>
    <template v-if="transaction">
      <section class="hero"><div><small>{{ country.flag }} {{ country.name }} 여행</small><h2>{{ transaction.merchant }}</h2><strong>-{{ money(transaction.amount) }}</strong></div><span :style="{ background: `${category.color}18` }">{{ transaction.icon }}</span></section>
      <section class="details"><dl>
        <div><dt>거래일시</dt><dd>{{ dateTime(transaction.date) }}</dd></div>
        <div><dt>여행지</dt><dd>{{ country.city }} · {{ country.name }}</dd></div>
        <div><dt>카테고리</dt><dd class="category" :style="{ color: category.color }">{{ category.name }}</dd></div>
        <div><dt>거래구분</dt><dd>지출</dd></div>
        <div><dt>결제수단</dt><dd>KB국민은행 여행통장<br>****5320</dd></div>
        <div><dt>사용처</dt><dd>{{ transaction.merchant }}</dd></div>
        <div><dt>거래 후 여행 잔액</dt><dd>{{ money((country.target - country.prepaid) - transaction.amount) }}</dd></div>
      </dl></section>
      <h3>메모</h3><section class="memo">{{ transaction.memo }}</section>
      <p class="period-note">이 거래는 등록한 {{ country.name }} 여행 기간에 포함된 내역이에요.</p>
    </template>
    <p v-else class="empty">거래내역을 찾을 수 없어요.</p>
  </main>
</template>

<style scoped>
.page{min-height:100vh;padding:52px 18px 30px;background:#f8f6f1;color:#10192d}header{display:flex;align-items:center;margin-bottom:18px}header button{width:26px;font-size:26px;text-align:left}header h1{flex:1;padding-right:26px;text-align:center;font-size:18px;font-weight:900}.hero{display:flex;align-items:center;justify-content:space-between;padding:18px;border:1px solid #dce4ef;border-radius:17px;background:#fff}.hero small{color:#64748b;font-size:9px}.hero h2{margin-top:5px;font-size:14px}.hero strong{display:block;margin-top:8px;color:#e5484d;font-size:24px}.hero span{display:grid;width:50px;height:50px;place-items:center;border-radius:50%;font-size:20px}.details{margin-top:13px;padding:6px 16px;border:1px solid #dce4ef;border-radius:17px;background:#fff}.details dl>div{display:grid;grid-template-columns:100px 1fr;padding:13px 0;border-bottom:1px solid #edf0f5;font-size:11px}.details dl>div:last-child{border:0}.details dt{color:#94a3b8}.details dd{text-align:right;font-weight:800;line-height:1.5}h3{margin:20px 3px 9px;font-size:12px}.memo{min-height:54px;padding:15px;border:1px solid #dce4ef;border-radius:13px;background:#fff;font-size:11px}.period-note{margin-top:12px;padding:11px;border-radius:10px;background:#eef4ff;color:#52719e;font-size:9px;text-align:center}.empty{padding:80px 0;text-align:center;color:#94a3b8}
</style>
