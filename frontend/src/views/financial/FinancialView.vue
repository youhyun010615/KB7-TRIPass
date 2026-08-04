<script setup>
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import ProductTicket from '@/components/financial/ProductTicket.vue'

const router = useRouter()
</script>

<template><main class="page"><div class="shell">
  <header><h1>금융상품</h1></header>
  <ProductTicket title="파리 & 스위스 출국까지 D-230" subtitle="목표에 맞는 금융상품을 비교해 보세요" />
  <h2 class="question">어떤 상품이 필요하세요?</h2>
  <section class="product-card"><span class="icon savings">₩</span><div><h3>여행자금 적금</h3><p>목표 금액과 남은 기간에 맞는<br>적금 상품을 추천해 드려요</p></div><button @click="router.push('/financial/savings')">적금 상품 보기 ›</button><em>연 3.8%부터</em></section>
  <section class="product-card"><span class="icon card">▣</span><div><h3>트래블카드</h3><p>현재 결제 수수료와<br>환전 혜택을 비교해요</p></div><button @click="router.push('/financial/cards')">카드 상품 보기 ›</button><em>최대 300만원</em></section>
  <section class="basis"><h2>내 여행에 맞춘 추천 기준</h2><p><span>✓</span> 출국일 이전 만기 상품</p><p><span>✓</span> 월 500,000원 납입 계획 반영</p></section>
  <BottomNav />
</div></main></template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#111827}.shell{width:min(100%,390px);min-height:100vh;margin:auto;padding:52px 18px 100px;background:#f7f5ef}header{margin-bottom:18px}header h1{font-size:19px;font-weight:900}.question{margin:22px 2px 11px;font-size:13px}.product-card{position:relative;display:grid;grid-template-columns:42px 1fr;gap:11px;margin-top:11px;padding:15px;border:1px solid #e0e6ef;border-radius:16px;background:#fff;box-shadow:0 5px 13px #27364b0d}.icon{display:grid;width:40px;height:40px;place-items:center;border-radius:12px;font-size:17px;font-weight:900}.icon.savings{background:#e7f0ff;color:#1672f9}.icon.card{background:#e3f8f2;color:#0fa77f}.product-card h3{font-size:12px}.product-card p{margin-top:4px;color:#7f8b9d;font-size:8px;line-height:1.5}.product-card button{grid-column:2;margin-top:5px;text-align:right;color:#174494;font-size:8px;font-weight:900}.product-card em{position:absolute;left:15px;bottom:14px;padding:5px 8px;border-radius:10px;background:#f2f6ff;color:#406dbb;font-size:7px;font-style:normal}.basis{margin-top:22px;padding:15px;border:1px solid #e2e7ed;border-radius:15px;background:#fff}.basis h2{font-size:11px}.basis p{margin-top:12px;color:#4e5d70;font-size:9px}.basis span{margin-right:8px;color:#0fa874;font-weight:900}
</style>
