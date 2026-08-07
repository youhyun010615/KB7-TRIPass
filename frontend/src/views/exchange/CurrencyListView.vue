<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useExchangeStore } from '@/stores/exchange'

const router = useRouter()
const exchange = useExchangeStore()
const query = ref('')
const format = (value) => Number(value || 0).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const filteredCurrencies = computed(() => {
  const keyword = query.value.trim().toLocaleLowerCase('ko-KR')
  if (!keyword) return exchange.currencies
  return exchange.currencies.filter((item) => [item.code, item.name, item.country]
    .some((value) => value.toLocaleLowerCase('ko-KR').includes(keyword)))
})

function openCurrency(item) {
  exchange.selectedCode = item.code
  router.push(`/exchange/currencies/${item.code}`)
}
</script>

<template>
  <main class="page">
    <div class="shell">
      <header><button type="button" aria-label="이전 화면" @click="router.back()">‹</button><h1>주요 통화</h1><span /></header>
      <label class="search"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.8"/><path d="M16 16L21 21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg><input v-model="query" placeholder="국가명 또는 통화 코드를 검색해 주세요" aria-label="통화 검색"><button v-if="query" type="button" aria-label="검색어 지우기" @click="query = ''">×</button></label>
      <p class="result-count">{{ query ? `'${query}' 검색 결과` : '전체 통화' }} · {{ filteredCurrencies.length }}개</p>
      <section v-if="filteredCurrencies.length" class="currency-list">
        <button v-for="item in filteredCurrencies" :key="item.code" type="button" @click="openCurrency(item)">
          <span class="flag">{{ item.flag }}</span>
          <span class="identity"><b>{{ item.code }}<small v-if="item.unit > 1"> {{ item.unit }}</small></b><em>{{ item.name }}</em></span>
          <span class="rate"><strong>{{ format(item.rate) }}원</strong><small :class="{ up: item.change > 0 }">{{ item.change > 0 ? '▲' : '▼' }} {{ format(Math.abs(item.change)) }}</small></span>
          <i>›</i>
        </button>
      </section>
      <section v-else class="empty"><b>검색 결과가 없어요.</b><p>국가명, 통화명 또는 통화 코드를 다시 확인해 주세요.</p><button type="button" @click="query = ''">전체 통화 보기</button></section>
      <BottomNav />
    </div>
  </main>
</template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:auto;padding:52px 18px 100px;background:#f7f5ef}header{display:grid;grid-template-columns:30px 1fr 30px;align-items:center}header button{font-size:26px;text-align:left}header h1{text-align:center;font-size:18px;font-weight:900}.search{display:grid;grid-template-columns:20px 1fr 24px;align-items:center;gap:7px;margin-top:20px;padding:11px 12px;border:1px solid #dce3ed;border-radius:13px;background:#fff;color:#8290a3}.search input{min-width:0;background:transparent;font-size:10px;outline:none}.search button{font-size:17px;color:#94a3b8}.result-count{margin:14px 3px 8px;color:#8290a3;font-size:9px;font-weight:800}.currency-list button{display:grid;width:100%;grid-template-columns:32px 1fr auto 10px;align-items:center;gap:10px;margin-bottom:9px;padding:13px 14px;border:1px solid #dfe5ed;border-radius:14px;background:#fff;box-shadow:0 4px 11px #172d550a;text-align:left}.flag{font-size:19px}.identity b,.identity em,.rate strong,.rate small{display:block}.identity b{font-size:11px}.identity b small{font-size:8px}.identity em{margin-top:4px;color:#64748b;font-size:8px;font-style:normal}.rate{text-align:right}.rate strong{font-size:11px}.rate small{margin-top:4px;color:#0b9e74;font-size:8px}.rate small.up{color:#e45b55}.currency-list i{color:#9aa7b7;font-size:18px;font-style:normal}.empty{margin-top:45px;padding:35px 15px;border:1px dashed #cbd5e1;border-radius:15px;background:#fff;text-align:center}.empty b{font-size:12px}.empty p{margin-top:7px;color:#94a3b8;font-size:9px}.empty button{margin-top:14px;padding:9px 12px;border-radius:8px;background:#173f8d;color:#fff;font-size:9px;font-weight:900}.shell :deep(.fixed){display:flex;gap:0;margin:0}
</style>
