<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSavingsPlanStore } from '@/stores/savingsPlan'

const router = useRouter()
const plan = useSavingsPlanStore()
const money = (value) => `${Number(value).toLocaleString('ko-KR')}원`
const isCash = computed(() => plan.savingMethod === 'cash')
const isProduct = computed(() => plan.savingMethod === 'product')

function confirm() {
  if (!plan.canSave) return
  if (isProduct.value) router.push('/financial')
  else router.push('/savings')
}
</script>

<template>
  <main class="setup-page">
    <header class="page-header"><button @click="router.back()">‹</button><h1>목표 저축 설정</h1></header>

    <section class="summary-ticket">
      <div><span>총 목표 금액</span><strong>{{ money(plan.totalTargetAmount) }}</strong></div>
      <div><span>확보한 여행 자금</span><strong class="secured">{{ money(plan.securedAmount) }}</strong></div>
      <div><span>추천 월 저축 목표</span><strong class="recommend">{{ money(plan.recommendedMonthlySavings) }}</strong></div>
    </section>

    <h2>저축 방식 선택</h2>
    <button class="method-card" :class="{ selected: isCash }" @click="plan.selectMethod('cash')">
      <span class="check">{{ isCash ? '✓' : '' }}</span>
      <div><strong>여행 저축 금액</strong><small>현재 여유 자금에서 매월 저축해요.</small></div>
    </button>
    <section v-if="isCash" class="cash-panel">
      <label>매월 저축할 금액</label>
      <div class="money-input"><input :value="money(plan.monthlySavings)" inputmode="numeric" @input="plan.setMonthlySavings($event.target.value)"><span>직접 입력</span></div>
      <div class="fund-row"><span>현재 사용 가능한 여유 자금</span><b>{{ money(plan.availableFunds) }}</b></div>
      <div class="fund-row" :class="{ danger: plan.insufficientFunds }"><span>{{ plan.insufficientFunds ? '부족 금액' : '저축 후 남는 여유 자금' }}</span><b>{{ money(plan.insufficientFunds || plan.remainingAvailableFunds) }}</b></div>
      <p v-if="plan.status === 'success'" class="guide success">✓ 현재 금액이면 목표 일정에 맞출 수 있어요.</p>
      <p v-else-if="plan.status === 'warning'" class="guide warning">! 월 {{ money(plan.additionalRecommendedAmount) }}을 더 저축하면 목표 일정에 맞출 수 있어요.</p>
      <p v-else-if="plan.status === 'error'" class="guide error">⚠ 여유 자금이 {{ money(plan.insufficientFunds) }} 부족해요. {{ money(plan.availableFunds) }} 이하로 입력해 주세요.</p>
    </section>

    <button class="method-card" :class="{ selected: isProduct }" @click="plan.selectMethod('product')">
      <span class="check">{{ isProduct ? '✓' : '' }}</span>
      <div><strong>금융상품 추천</strong><small>부족 금액과 남은 기간에 맞는 상품을 찾아드려요.</small><em>예상 수익 비교 · 기간 맞춤 추천</em></div>
    </button>

    <button class="primary-cta" :disabled="!plan.canSave" @click="confirm">
      {{ isProduct ? '맞춤 금융상품 추천받기' : plan.status === 'error' ? '금액 다시 입력' : '확인' }}
    </button>
  </main>
</template>

<style scoped>
.setup-page { min-height:100vh; padding:0 18px 100px; color:#111827; background:#f7f4ee; }
.page-header { height:76px; display:grid; grid-template-columns:36px 1fr 36px; align-items:end; padding-bottom:14px; }.page-header button { border:0; background:none; font-size:24px; text-align:left; }.page-header h1 { font-size:17px; font-weight:800; }
.summary-ticket { position:relative; display:grid; grid-template-columns:repeat(3,1fr); padding:18px 10px; border-radius:15px; color:#fff; background:linear-gradient(135deg,#294ca3,#163a85); box-shadow:0 7px 16px rgba(23,59,134,.18); }
.summary-ticket::before,.summary-ticket::after { content:''; position:absolute; top:50%; width:14px; height:14px; border-radius:50%; background:#f7f4ee; transform:translateY(-50%); }.summary-ticket::before { left:-7px; }.summary-ticket::after { right:-7px; }
.summary-ticket div { display:grid; gap:5px; padding:0 7px; border-right:1px dashed rgba(255,255,255,.3); }.summary-ticket div:last-child { border:0; }.summary-ticket span { color:#bfcef5; font-size:7px; }.summary-ticket strong { font-size:11px; }.summary-ticket .secured { color:#ffd56a; }.summary-ticket .recommend { color:#82d3ff; }
h2 { margin:22px 0 11px; font-size:15px; }
.method-card { width:100%; display:flex; gap:11px; padding:15px; border:1px solid #e1e7f0; border-radius:15px; background:#fff; text-align:left; box-shadow:0 4px 12px rgba(22,40,80,.05); }.method-card + .method-card { margin-top:12px; }.method-card.selected { border:1.5px solid #2872ff; background:#f2f6ff; }.check { display:grid; place-items:center; width:24px; height:24px; flex:none; border:1px solid #cdd8ea; border-radius:7px; color:#fff; background:#fff; }.selected .check { border-color:#2872ff; background:#2872ff; }.method-card div { display:grid; gap:4px; }.method-card strong { font-size:12px; }.method-card small { color:#64748b; font-size:9px; line-height:1.45; }.method-card em { color:#208965; font-size:8px; font-style:normal; }
.cash-panel { margin:-7px 0 12px; padding:18px 14px 14px; border:1.5px solid #2872ff; border-top:0; border-radius:0 0 15px 15px; background:#f8faff; }.cash-panel label { display:block; color:#64748b; font-size:9px; }.money-input { display:flex; align-items:center; margin:7px 0 12px; padding:10px 12px; border:1px solid #b8c9e9; border-radius:9px; background:#fff; }.money-input input { min-width:0; flex:1; border:0; outline:0; color:#0066ff; background:transparent; font-size:17px; font-weight:800; }.money-input span { color:#94a3b8; font-size:8px; }.fund-row { display:flex; justify-content:space-between; padding:6px 2px; color:#64748b; font-size:9px; }.fund-row b { color:#111827; }.fund-row.danger,.fund-row.danger b { color:#e5484d; }.guide { margin-top:8px; padding:10px; border-radius:9px; font-size:8px; line-height:1.45; }.guide.success { color:#07875f; background:#e8f8f3; }.guide.warning { color:#c76a00; background:#fff4e3; }.guide.error { color:#d9343b; background:#fff0f0; }
.primary-cta { position:fixed; left:50%; bottom:22px; width:min(354px,calc(100% - 36px)); height:54px; transform:translateX(-50%); border:0; border-radius:14px; color:#fff; background:#173b86; font-size:14px; font-weight:800; box-shadow:0 8px 18px rgba(23,59,134,.16); }.primary-cta:disabled { color:#fff; background:#aeb9cc; box-shadow:none; }
</style>
