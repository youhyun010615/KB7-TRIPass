<script setup>
import { useRouter } from 'vue-router'
import { useMonthlyFundStore } from '@/stores/monthlyFund'

const router = useRouter()
const fund = useMonthlyFundStore()
const money = (value) => Number(value || 0).toLocaleString('ko-KR')
</script>

<template>
  <main class="form-page">
    <header><button @click="router.back()">‹</button><h1>카테고리 목표 수정</h1></header>

    <section class="summary-ticket">
      <small>TRIPASS · MONTHLY BUDGET</small>
      <div><span>분배 가능 금액<b>{{ money(fund.availableFunds) }}원</b></span><span>목표 총합<b>{{ money(fund.categoryTargetTotal) }}원</b></span><span>여유자금<b>{{ money(fund.freeFunds) }}원</b></span></div>
    </section>

    <section class="edit-card">
      <h2>카테고리별 목표 금액</h2>
      <p>이달에 사용할 금액을 카테고리별로 설정해 주세요.</p>
      <label v-for="item in fund.categories" :key="item.id">
        <span class="icon" :style="{ background: `${item.color}18` }">{{ item.icon }}</span>
        <span class="name"><b>{{ item.name }}</b><small>목표 금액</small></span>
        <span class="input"><input :value="money(item.target)" inputmode="numeric" @input="fund.updateCategoryTarget(item.id, $event.target.value)"><i>원</i></span>
      </label>
    </section>

    <button class="cta" @click="router.push('/savings/monthly')">수정사항 반영하기</button>
  </main>
</template>

<style scoped>
.form-page { width: min(100%,390px); min-height: 100vh; margin: 0 auto; padding: 48px 16px 24px; background:#f7f4ee; color:#151f33; }
header { display:grid; grid-template-columns:30px 1fr; align-items:center; margin-bottom:18px; } header button{font-size:28px;text-align:left} h1{font-size:19px;font-weight:900}
.summary-ticket{padding:17px;border-radius:16px;background:linear-gradient(120deg,#112e70,#2454b3);color:white;box-shadow:0 9px 18px #173d8929}.summary-ticket small{font-size:8px;color:#bfcef2;letter-spacing:1px}.summary-ticket div{display:grid;grid-template-columns:repeat(3,1fr);margin-top:15px}.summary-ticket span{display:flex;flex-direction:column;gap:5px;font-size:8px;color:#bdcced}.summary-ticket span+span{padding-left:10px;border-left:1px dashed #ffffff55}.summary-ticket b{font-size:13px;color:white}
.edit-card{margin-top:14px;padding:18px 14px;border-radius:18px;background:white;box-shadow:0 6px 15px #1e346211}.edit-card h2{font-size:15px;font-weight:900}.edit-card>p{margin:5px 0 12px;color:#94a3b8;font-size:9px}.edit-card label{display:grid;grid-template-columns:38px 1fr 120px;align-items:center;gap:9px;padding:11px 0}.icon{display:grid;width:36px;height:36px;place-items:center;border-radius:11px}.name b{display:block;font-size:12px}.name small{display:block;margin-top:3px;color:#94a3b8;font-size:8px}.input{display:flex;align-items:center;height:38px;padding:0 10px;border:1px solid #d9e2f0;border-radius:10px}.input input{min-width:0;width:100%;text-align:right;font-size:12px;font-weight:800;outline:none}.input i{margin-left:4px;color:#94a3b8;font-size:9px}.cta{width:100%;height:52px;margin-top:16px;border-radius:13px;background:#173d89;color:white;font-size:14px;font-weight:900}
</style>
