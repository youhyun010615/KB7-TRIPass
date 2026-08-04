<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAssetStore } from '@/stores/asset'

const route = useRoute()
const router = useRouter()
const asset = useAssetStore()
const item = computed(() => asset.getTransaction(route.params.transactionId))
const money = (value) => `${value > 0 ? '+' : '-'}${Math.abs(value || 0).toLocaleString('ko-KR')}원`
const rows = computed(() => item.value ? [
  ['거래일시', `${item.value.dateLabel} ${item.value.time}`], ['카테고리 설정', item.value.category],
  ['거래구분', item.value.amount > 0 ? '입금' : '지출'], ['결제수단', item.value.method],
  ['사용처', item.value.user], ['거래 후 잔액', `${item.value.balanceAfter.toLocaleString('ko-KR')}원`],
] : [])
</script>

<template>
  <main class="detail-page">
    <header><button @click="router.back()">‹</button><h1>거래내역 상세보기</h1></header>
    <template v-if="item">
      <section class="hero"><div><b>{{ item.merchant }}</b><strong :class="item.amount > 0 ? 'deposit' : 'withdrawal'">{{ money(item.amount) }}</strong></div><span>{{ item.amount > 0 ? '＋' : '−' }}</span></section>
      <section class="info-card"><div v-for="row in rows" :key="row[0]"><small>{{ row[0] }}</small><b>{{ row[1] }}</b></div></section>
      <h2>메모</h2><section class="memo">{{ item.memo || '등록된 메모가 없어요.' }}</section>
    </template>
    <p v-else class="empty">거래내역을 찾을 수 없어요.</p>
  </main>
</template>

<style scoped>
.detail-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 30px;background:#f4f6fc;color:#10192d}header{display:grid;grid-template-columns:30px 1fr;align-items:center;margin-bottom:18px}header button{font-size:26px;text-align:left}h1{font-size:18px;font-weight:900}.hero{display:flex;align-items:center;justify-content:space-between;padding:18px;border:1px solid #e0e6ef;border-radius:16px;background:white}.hero b,.hero strong{display:block}.hero b{font-size:13px}.hero strong{margin-top:8px;font-size:24px}.hero span{display:grid;width:46px;height:46px;place-items:center;border-radius:50%;background:#e4fbf5;color:#14b89e;font-size:24px}.deposit{color:#13b89f}.withdrawal{color:#e32927}.info-card{margin-top:12px;padding:10px 16px;border:1px solid #dbe3ef;border-radius:17px;background:white}.info-card div{display:grid;grid-template-columns:100px 1fr;padding:13px 0;border-bottom:1px solid #edf0f5}.info-card div:last-child{border:0}.info-card small{color:#94a3b8;font-size:10px}.info-card b{text-align:right;font-size:11px;line-height:1.4}h2{margin:20px 3px 9px;font-size:12px;font-weight:900}.memo{padding:15px;border:1px solid #dbe3ef;border-radius:12px;background:white;font-size:11px}.empty{padding:80px 0;text-align:center;color:#94a3b8}
</style>
