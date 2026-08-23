<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useExchangeStore } from '@/stores/exchange'

const route = useRoute()
const router = useRouter()
const exchange = useExchangeStore()
const alert = computed(() => exchange.alerts.find(item => String(item.id) === String(route.params.alertId)))
const currency = computed(() => exchange.getCurrencyByCountryId(alert.value?.countryId))
const format = value => Number(value || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 })

onMounted(async () => {
  if (!exchange.currencies.length) await exchange.updateExchangeRates()
  await exchange.fetchAlerts()
})

async function remove() {
  if (!alert.value || !window.confirm('이 환율 알림을 삭제할까요?')) return
  await exchange.removeAlert(alert.value.id)
  router.replace('/exchange/alerts')
}
</script>

<template>
  <main class="page">
    <header><button type="button" @click="router.back()">‹</button><h1>환율 알림 상세</h1><span /></header>
    <section v-if="alert" class="ticket">
      <div class="ticket-head"><small>TRIPASS RATE ALERT</small><b>ACTIVE</b></div>
      <div class="identity">
        <span :class="currency?.flagClass" class="flag" />
        <div><small>{{ currency?.countryName }}</small><h2>{{ currency?.name }} · {{ alert.currencyCode }}</h2></div>
      </div>
      <div class="rate-row"><span>목표 환율</span><strong>{{ format(alert.targetRate) }}원</strong></div>
      <div class="rate-row current"><span>현재 환율</span><strong>{{ format(currency?.rate) }}원</strong></div>
      <p>목표 환율에 도달하면 알림으로 알려드려요.</p>
    </section>
    <p v-else class="loading">알림 정보를 불러오고 있어요.</p>
    <div v-if="alert" class="actions">
      <button type="button" class="delete" @click="remove">삭제</button>
      <button type="button" class="edit" @click="router.push(`/exchange/alerts/${alert.id}/edit`)">수정하기</button>
    </div>
  </main>
</template>

<style scoped>
.page{min-height:100vh;padding:14px 18px 30px;background:#eef2f8;color:#10192d}.page>header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center;margin-bottom:22px}.page>header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:800;box-shadow:0 5px 16px rgba(36,72,117,.08)}.page>header h1{text-align:center;font-size:18px;font-weight:900}.ticket{position:relative;overflow:hidden;padding:22px;border-radius:24px;background:linear-gradient(145deg,#0d2f76,#2469e8);color:#fff;box-shadow:0 18px 38px rgba(23,63,141,.22)}.ticket:after{position:absolute;right:-55px;top:-65px;width:170px;height:170px;border-radius:50%;background:rgba(255,255,255,.08);content:''}.ticket-head,.rate-row{position:relative;z-index:1;display:flex;align-items:center;justify-content:space-between}.ticket-head small{color:#ffd45e;font-size:9px;font-weight:900;letter-spacing:.14em}.ticket-head b{padding:5px 8px;border-radius:99px;background:#dff8ef;color:#087e5b;font-size:8px}.identity{position:relative;z-index:1;display:flex;align-items:center;gap:12px;margin:27px 0 23px}.flag{width:42px;height:29px;border-radius:5px;background-size:cover;box-shadow:0 3px 10px rgba(0,0,0,.2)}.identity small{color:rgba(255,255,255,.65);font-size:9px}.identity h2{margin-top:4px;font-size:17px}.rate-row{padding:15px 0;border-top:1px dashed rgba(255,255,255,.3)}.rate-row span{color:rgba(255,255,255,.66);font-size:10px}.rate-row strong{font-size:20px}.rate-row.current strong{color:#ffd45e}.ticket>p{position:relative;z-index:1;margin-top:5px;color:rgba(255,255,255,.62);font-size:9px}.loading{padding:45px;text-align:center;color:#8290a3;font-size:11px}.actions{display:grid;grid-template-columns:1fr 2fr;gap:9px;margin-top:18px}.actions button{height:50px;border-radius:14px;font-size:12px;font-weight:900}.delete{border:1px solid #f1a8aa;color:#d9484f;background:#fff}.edit{color:#fff;background:#173f8d}
</style>
