<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useReceiptStore } from '@/stores/receipt'

const route = useRoute()
const router = useRouter()
const store = useReceiptStore()
const tripId = computed(() => Number(route.query.tripId || 1))
const trip = computed(() => store.trip(tripId.value))
const selectedCountry = ref('전체')
const tripReceipts = store.byTrip(tripId.value)
const countries = computed(() => ['전체', ...new Set(tripReceipts.value.map(item => item.country))])
const filtered = computed(() => selectedCountry.value === '전체' ? tripReceipts.value : tripReceipts.value.filter(item => item.country === selectedCountry.value))
const groups = computed(() => {
  const map = new Map()
  filtered.value.forEach(item => { if (!map.has(item.date)) map.set(item.date, []); map.get(item.date).push(item) })
  return [...map.entries()].map(([date, items]) => ({ date, items }))
})
</script>

<template>
  <main class="receipt-page">
    <header><button type="button" @click="router.back()">‹</button><h1>영수증 보관함</h1><span /></header>
    <section class="vault-ticket"><small>TRIPASS RECEIPT VAULT</small><h2>{{ trip.title }}</h2><p>{{ trip.dateRange }}</p><div><b>{{ tripReceipts.length }}건</b><span>번역 완료</span></div><i>||||||||||||||||||||</i></section>
    <section class="filter-row"><b>국가 선택</b><select v-model="selectedCountry"><option v-for="country in countries" :key="country">{{ country }}</option></select></section>
    <section class="receipt-list">
      <div class="title"><h2>영수증 목록</h2><span>최근 스캔 순</span></div>
      <div v-for="group in groups" :key="group.date" class="date-group"><h3>{{ group.date.replaceAll('.', '-') }}</h3><button v-for="item in group.items" :key="item.id" type="button" @click="router.push(`/receipt/${item.id}?tripId=${tripId}`)"><span>{{ item.flag }}</span><div><b>{{ item.merchant }}</b><small>{{ item.time || '시간 미확인' }} · {{ item.country }} · {{ item.category }}</small><i>약 {{ Number(item.wonAmount).toLocaleString() }}원</i></div><strong>{{ item.currency }} {{ item.amount.toLocaleString() }}</strong><em>›</em></button></div>
      <div v-if="!filtered.length" class="empty"><span>▤</span><b>보관된 영수증이 없어요</b><small>이 여행에서 촬영한 영수증을 추가해 주세요.</small></div>
    </section>
    <button class="scan" type="button" @click="router.push(`/receipt/capture?tripId=${tripId}`)">＋ 해외 영수증 OCR 스캔하기</button>
    <BottomNav />
  </main>
</template>

<style scoped>
.receipt-page{min-height:100vh;padding:0 18px 150px;background:#f8f6f1;color:#111a2d}.receipt-page>header{display:grid;height:88px;grid-template-columns:36px 1fr 36px;align-items:end;padding-bottom:17px}.receipt-page>header button{font-size:30px;text-align:left}.receipt-page>header h1{text-align:center;font-size:20px;font-weight:900}.vault-ticket{position:relative;overflow:hidden;padding:19px 20px;border-radius:20px;background:linear-gradient(135deg,#12347b,#1466cb);color:#fff;box-shadow:0 8px 20px #173f8d24}.vault-ticket:before,.vault-ticket:after{position:absolute;top:62px;width:22px;height:22px;border-radius:50%;background:#f8f6f1;content:''}.vault-ticket:before{left:-11px}.vault-ticket:after{right:-11px}.vault-ticket small{color:#afcaf7;font-size:8px;letter-spacing:1px}.vault-ticket h2{margin-top:12px;font-size:18px}.vault-ticket p{margin-top:5px;color:#c9daf7;font-size:10px}.vault-ticket div{display:flex;align-items:end;gap:8px;margin-top:22px;padding-top:14px;border-top:1px dashed #ffffff77}.vault-ticket div b{font-size:26px}.vault-ticket div span{padding-bottom:4px;color:#c9daf7;font-size:10px}.vault-ticket i{position:absolute;right:18px;bottom:18px;font-style:normal;letter-spacing:-1px}.filter-row{display:flex;align-items:center;justify-content:space-between;margin:16px 2px}.filter-row b{font-size:13px}.filter-row select{min-width:108px;padding:9px 12px;border:1px solid #d7e0ed;border-radius:12px;background:#fff;font-size:11px}.receipt-list{padding:18px;border:1px solid #d8e1ec;border-radius:20px;background:#fff}.title{display:flex;align-items:center;justify-content:space-between}.title h2{font-size:17px;font-weight:900}.title span{color:#94a3b8;font-size:9px}.receipt-list>button{display:grid;width:100%;grid-template-columns:38px minmax(0,1fr) auto 10px;align-items:center;gap:9px;padding:15px 2px;border-bottom:1px solid #edf0f4;text-align:left}.receipt-list>button>span{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;background:#f1f5ff;font-size:18px}.receipt-list button b,.receipt-list button small{display:block}.receipt-list button b{overflow:hidden;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.receipt-list button small{margin-top:5px;color:#8a97aa;font-size:8px}.receipt-list button strong{font-size:10px;white-space:nowrap}.receipt-list button em{color:#94a3b8;font-size:20px;font-style:normal}.empty{display:flex;min-height:190px;flex-direction:column;align-items:center;justify-content:center}.empty span{font-size:30px;color:#2e70dc}.empty b{margin-top:12px;font-size:13px}.empty small{margin-top:6px;color:#8b98a9;font-size:9px}.scan{position:fixed;right:max(calc((100vw - 390px)/2 + 18px),18px);bottom:78px;left:max(calc((100vw - 390px)/2 + 18px),18px);z-index:40;height:55px;border-radius:14px;background:#19489c;color:#fff;font-size:13px;font-weight:900;box-shadow:0 8px 18px #173f8d2e}
.date-group h3{margin:20px 2px 4px;color:#246dd7;font-size:11px}.date-group>button{display:grid;width:100%;grid-template-columns:38px minmax(0,1fr) auto 10px;align-items:center;gap:9px;padding:15px 2px;border-bottom:1px solid #edf0f4;text-align:left}.date-group>button>span{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;background:#f1f5ff;font-size:18px}.date-group button b,.date-group button small,.date-group button i{display:block}.date-group button b{overflow:hidden;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.date-group button small{margin-top:5px;color:#8a97aa;font-size:8px}.date-group button i{margin-top:3px;color:#7a8aa0;font-size:7px;font-style:normal}.date-group button strong{font-size:10px;white-space:nowrap}.date-group button em{color:#94a3b8;font-size:20px;font-style:normal}
</style>
