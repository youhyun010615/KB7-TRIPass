<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from '@lucide/vue'
import BottomNav from '@/components/common/BottomNav.vue'
import AnimatedNumber from '@/components/common/AnimatedNumber.vue'
import { useTripWalletStore } from '@/stores/tripWallet'
import { flagClassMap } from '@/stores/exchange'

const router = useRouter()
const wallet = useTripWalletStore()
const notice = ref('')

const currencyMeta = {
  EUR: { name: '유로', flag: '🇪🇺', rate: 1486 },
  JPY: { name: '일본 엔', flag: '🇯🇵', rate: 9.42 },
  CHF: { name: '스위스 프랑', flag: '🇨🇭', rate: 1606 },
  USD: { name: '미국 달러', flag: '🇺🇸', rate: 1375 },
}
const normalizeCurrencyCode = value => String(value || '')
  .replace(/\(100\)/g, '')
  .trim()
  .toUpperCase()

const foreignBalances = computed(() => wallet.foreignBalances.map(item => {
  const code = normalizeCurrencyCode(item.code || item.currencyCode)
  const amount = item.amount ?? item.balanceAmount
  const krwAmount = item.krwAmount ?? item.krwEstimatedAmount

  return {
    ...item,
    code,
    amount,
    krwAmount,
    name: item.name || item.currencyName || currencyMeta[code]?.name || code,
    flag: item.flag || currencyMeta[code]?.flag || '🌐',
    rate: item.rate || currencyMeta[code]?.rate || Math.round((krwAmount || 0) / Math.max(Number(amount || 1), 1)),
  }
}))

const totalKrwAmount = computed(() => foreignBalances.value.reduce((sum, item) => sum + Number(item.krwAmount || 0), 0))

const money = value => `${Number(value || 0).toLocaleString('ko-KR')}원`
const moneyFormatter = value => `${value.toLocaleString('ko-KR')}원`
const formatAmount = item => Number(item.amount || 0).toLocaleString('ko-KR', {
  minimumFractionDigits: item.code === 'EUR' || item.code === 'CHF' || item.code === 'USD' ? 2 : 0,
  maximumFractionDigits: item.code === 'EUR' || item.code === 'CHF' || item.code === 'USD' ? 2 : 0,
})
const hasDecimals = code => code === 'EUR' || code === 'CHF' || code === 'USD'

onMounted(async () => {
  try {
    await wallet.loadForeignBalances()
  } catch {
    notice.value = wallet.errorMessage || '외화 머니를 불러오지 못했어요.'
  }
})
</script>

<template>
  <main class="foreign-page">
    <header class="page-header">
      <button type="button" class="back-button" @click="router.back()"><ChevronLeft :size="22" /></button>
      <div>
        <h1>보유 외화</h1>
      </div>
      <span aria-hidden="true"></span>
    </header>

    <section class="summary-card">
      <span>전체 원화 환산</span>
      <strong><small>약</small> <AnimatedNumber :value="totalKrwAmount" :formatter="moneyFormatter" /></strong>
    </section>

    <p v-if="notice" class="notice-text">{{ notice }}</p>

    <section class="currency-card">
      <div class="section-head">
        <h2>전체 외화 머니</h2>
        <span>{{ foreignBalances.length }}개 통화</span>
      </div>

      <div v-if="foreignBalances.length" class="currency-list">
        <article v-for="item in foreignBalances" :key="item.code" class="currency-row">
          <div class="flag">
            <span v-if="flagClassMap[item.code]" :class="flagClassMap[item.code]" class="fi-inline wallet-currency-flag"></span>
            <span v-else>{{ item.flag }}</span>
          </div>
          <div class="currency-info">
            <h3>{{ item.code }} <small>{{ item.name }}</small></h3>
            <p>현재 환율 <AnimatedNumber :value="Number(item.rate || 0)" :formatter="v => `${v.toLocaleString('ko-KR')}원`" /></p>
          </div>
          <div class="amount-info">
            <strong><AnimatedNumber :value="Number(item.amount || 0)" :decimals="hasDecimals(item.code) ? 2 : 0" /></strong>
            <span>약 <AnimatedNumber :value="Number(item.krwAmount || 0)" :formatter="moneyFormatter" /></span>
          </div>
        </article>
      </div>
      <p v-else class="list-empty">보유한 외화가 없어요.</p>
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.foreign-page{max-width:430px;min-height:100vh;margin:0 auto;padding:44px 16px 96px;background:#eef2f8;color:#111827}.page-header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center}.page-header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;box-shadow:0 5px 16px rgba(36,72,117,.07)}.page-header p{color:#1f5ab9;font-size:11px;font-weight:800;letter-spacing:.04em;text-align:center}.page-header h1{margin-top:2px;font-size:26px;font-weight:800;letter-spacing:-.04em;text-align:center}.summary-card{margin-top:24px;padding:22px;border-radius:24px;background:#14357f;color:#fff;box-shadow:0 14px 28px rgba(24,51,99,.18)}.summary-card>span{display:block;color:#c8d6f4;font-size:13px;font-weight:600}.summary-card strong{display:block;margin-top:8px;font-size:32px;font-weight:800;letter-spacing:-.05em}.summary-card strong small{margin-right:2px;color:#c8d6f4;font-size:14px;font-weight:600}.summary-card p{margin-top:10px;color:#dbe7ff;font-size:13px;font-weight:500}.currency-card{margin-top:16px;padding:20px;border-radius:24px;background:#fff;box-shadow:0 10px 24px rgba(18,43,82,.07)}.section-head{display:flex;align-items:center;justify-content:space-between}.section-head h2{font-size:20px;font-weight:800;letter-spacing:-.04em}.section-head span{color:#64748b;font-size:12px;font-weight:700}.currency-list{display:grid;gap:10px;margin-top:16px}.currency-row{display:grid;grid-template-columns:38px minmax(0,1fr) auto;gap:11px;align-items:center;padding:13px;border:1px solid #edf2f8;border-radius:17px;background:#f8fbff}.flag{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;background:#fff;font-size:20px;box-shadow:0 0 0 1px #e4ebf5}.currency-info{min-width:0}.currency-info h3{display:flex;align-items:baseline;gap:6px;font-size:16px;font-weight:800}.currency-info small{color:#8b98ad;font-size:12px;font-weight:600}.currency-info p{overflow:hidden;margin-top:5px;color:#94a3b8;font-size:11px;font-weight:600;text-overflow:ellipsis;white-space:nowrap}.amount-info{text-align:right}.amount-info strong{display:block;font-size:17px;font-weight:800}.amount-info>span{display:block;margin-top:5px;color:#9aa8bd;font-size:11px;font-weight:600}.list-empty{margin-top:16px;padding:20px;border-radius:14px;background:#f7f9fd;color:#9aa8bd;font-size:12px;font-weight:600;text-align:center}.notice-text{margin-top:14px;padding:12px 14px;border-radius:14px;background:#fff5df;color:#a36c07;font-size:12px;font-weight:700}@media(max-width:380px){.currency-row{grid-template-columns:34px minmax(0,1fr)}.amount-info{grid-column:2;text-align:left}.summary-card strong{font-size:28px}}
.foreign-page{word-break:keep-all}.foreign-page h1,.foreign-page h2,.foreign-page h3{text-wrap:balance}.foreign-page p{text-wrap:pretty}
.foreign-page{padding:14px 16px 88px;background:#f2f5fa}.page-header button{width:36px;height:36px}.page-header h1{color:#29466f;font-size:20px;font-weight:700;letter-spacing:-.025em}.page-header p{font-size:9.5px}.summary-card{margin-top:18px;padding:18px;border-radius:20px;background:linear-gradient(135deg,#102d6d,#2874dc)}.summary-card>span{font-size:11px}.summary-card strong{margin-top:6px;font-size:26px}.summary-card p{margin-top:7px;font-size:11px}.currency-card{margin-top:13px;padding:16px;border-radius:19px}.section-head h2{font-size:16px}.section-head span{font-size:10.5px}.currency-list{gap:8px;margin-top:13px}.currency-row{grid-template-columns:34px minmax(0,1fr) auto;gap:9px;padding:10px;border-radius:14px}.flag{width:32px;height:32px;font-size:17px}.currency-info h3{font-size:13px}.currency-info small{font-size:10px}.currency-info p{margin-top:3px;font-size:9.5px}.amount-info strong{font-size:14px}.amount-info>span{margin-top:3px;font-size:9.5px}
.wallet-currency-flag{display:block;width:25px;height:17px;border-radius:3px;background-size:cover;box-shadow:0 1px 3px rgba(15,23,42,.18)}
.currency-row .flag{width:32px;height:24px;border-radius:5px;background:#fff;box-shadow:0 1px 4px rgba(15,23,42,.18)}.wallet-currency-flag{width:27px;height:18px}
</style>
