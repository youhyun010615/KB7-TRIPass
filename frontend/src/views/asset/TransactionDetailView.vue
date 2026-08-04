<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAssetStore } from '@/stores/asset'

const route = useRoute()
const router = useRouter()
const asset = useAssetStore()
const item = computed(() => asset.getTransaction(route.params.transactionId))
const money = (value) => `${value > 0 ? '+' : '-'}${Math.abs(value || 0).toLocaleString('ko-KR')}원`
const categoryVisuals = {
  급여: { icon: '₩', tone: '#e4fbf5', color: '#12ad92' },
  카페: { icon: '☕', tone: '#fff4e5', color: '#c46b19' },
  식비: { icon: '🍴', tone: '#f2edff', color: '#7547d8' },
  생활비: { icon: '🛒', tone: '#eaf8f1', color: '#1c9a67' },
  교통비: { icon: '🚌', tone: '#eaf2ff', color: '#3475f4' },
  자동이체: { icon: '↻', tone: '#edf1f7', color: '#53657d' },
}
const visual = computed(() => categoryVisuals[item.value?.category] || { icon: '▦', tone: '#edf4ff', color: '#3475f4' })
const isDeposit = computed(() => Number(item.value?.amount || 0) > 0)
const rows = computed(() => item.value ? [
  { label: '거래일시', value: `${item.value.dateLabel} ${item.value.time}` },
  { label: '카테고리 설정', value: item.value.category, accent: true },
  { label: '거래구분', value: isDeposit.value ? '입금' : '출금' },
  { label: isDeposit.value ? '입금 계좌' : '결제수단', value: item.value.method },
  { label: isDeposit.value ? '입금처' : '사용처', value: item.value.user || item.value.merchant },
  { label: '거래 후 잔액', value: `${item.value.balanceAfter.toLocaleString('ko-KR')}원` },
] : [])
</script>

<template>
  <main class="detail-page">
    <header><button @click="router.back()">‹</button><h1>거래내역 상세보기</h1></header>
    <template v-if="item">
      <section class="hero"><div><b>{{ item.merchant }}</b><strong :class="isDeposit ? 'deposit' : 'withdrawal'">{{ money(item.amount) }}</strong></div><span :style="{ background: visual.tone, color: visual.color }">{{ visual.icon }}</span></section>
      <section class="info-card"><div v-for="row in rows" :key="row.label"><small>{{ row.label }}</small><b :class="{ accent: row.accent }">{{ row.value }}</b></div></section>
      <h2>메모</h2><section class="memo">{{ item.memo || '등록된 메모가 없어요.' }}</section>
    </template>
    <p v-else class="empty">거래내역을 찾을 수 없어요.</p>
  </main>
</template>

<style scoped>
.detail-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 30px;background:#f4f6fc;color:#10192d}header{display:grid;grid-template-columns:30px 1fr;align-items:center;margin-bottom:18px}header button{font-size:26px;text-align:left}h1{font-size:18px;font-weight:900}.hero{display:flex;align-items:center;justify-content:space-between;padding:18px;border:1px solid #e0e6ef;border-radius:16px;background:white}.hero b,.hero strong{display:block}.hero b{font-size:13px}.hero strong{margin-top:8px;font-size:24px}.hero span{display:grid;width:46px;height:46px;place-items:center;border-radius:50%;font-size:20px;font-weight:900}.deposit{color:#13b89f}.withdrawal{color:#e32927}.info-card{margin-top:12px;padding:10px 16px;border:1px solid #dbe3ef;border-radius:17px;background:white}.info-card div{display:grid;grid-template-columns:100px 1fr;padding:13px 0;border-bottom:1px solid #edf0f5}.info-card div:last-child{border:0}.info-card small{color:#94a3b8;font-size:10px}.info-card b{text-align:right;font-size:11px;line-height:1.4}.info-card .accent{color:#3475f4}h2{margin:20px 3px 9px;font-size:12px;font-weight:900}.memo{padding:15px;border:1px solid #dbe3ef;border-radius:12px;background:white;font-size:11px}.empty{padding:80px 0;text-align:center;color:#94a3b8}
</style>
