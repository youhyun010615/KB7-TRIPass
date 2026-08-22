<script setup>
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import ProductTicket from '@/components/financial/ProductTicket.vue'
import { useFinancialProductsStore } from '@/stores/financialProducts'
const router=useRouter(); const products=useFinancialProductsStore()
const filters=[['all','전체'],['general','일반 적금'],['travel','여행 적금']]
const money=(v)=>`${Number(v).toLocaleString('ko-KR')}원`
</script>
<template><main class="page"><div class="shell"><header><button @click="router.back()">‹</button><h1>여행 적금 찾기</h1><span aria-hidden="true"></span></header>
<ProductTicket title="6개월 동안 월 500,000원" subtitle="출국 전까지 3,000,000원을 준비하는 플랜" />
<label class="search">⌕<input v-model="products.savingsQuery" placeholder="금융기관·상품명을 검색해 주세요"></label>
<nav><button v-for="f in filters" :key="f[0]" :class="{active:products.savingsFilter===f[0]}" @click="products.savingsFilter=f[0]">{{ f[1] }}</button></nav>
<div class="title"><h2>추천 상품</h2><span>금리 높은 순⌄</span></div>
<section class="list"><button v-for="item in products.filteredSavings" :key="item.id" @click="router.push(`/financial/savings/${item.id}`)"><i :style="{background:item.color}">{{ item.bankCode }}</i><div><b>{{ item.name }}</b><small>{{ item.period }}개월 · 월 최대 {{ money(item.maxMonthly) }}</small></div><strong>연 {{ item.maxRate }}%</strong><em>›</em></button><p v-if="!products.filteredSavings.length">검색 결과가 없어요.</p></section><BottomNav /></div></main></template>
<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:auto;padding:14px 18px 100px;background:#f7f5ef}header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center;margin-bottom:16px}header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}header h1{text-align:center;font-size:18px;font-weight:900}.search{display:flex;gap:8px;margin-top:15px;padding:12px;border:1px solid #e1e6ed;border-radius:12px;background:#fff;color:#9aa5b5}.search input{flex:1;font-size:9px;outline:none}nav{display:flex;gap:7px;margin-top:12px}nav button{padding:8px 12px;border-radius:15px;background:#fff;color:#718096;font-size:8px}nav .active{background:#2674e8;color:#fff;font-weight:900}.title{display:flex;justify-content:space-between;margin:18px 2px 10px}.title h2{font-size:12px}.title span{color:#2870d8;font-size:8px}.list button{display:grid;width:100%;grid-template-columns:38px 1fr auto 10px;align-items:center;gap:9px;margin-top:9px;padding:12px;border:1px solid #e1e6ed;border-radius:13px;background:#fff;text-align:left;box-shadow:0 3px 9px #23344c0c}.list i{display:grid;width:36px;height:36px;place-items:center;border-radius:10px;color:#fff;font-size:9px;font-style:normal;font-weight:900}.list b,.list small{display:block}.list b{font-size:10px}.list small{margin-top:4px;color:#8b96a6;font-size:7px}.list strong{color:#1472ee;font-size:11px}.list em{color:#a0aaba;font-size:18px;font-style:normal}.list>p{padding:60px;text-align:center;color:#94a3b8;font-size:10px}
</style>
