<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft, CreditCard, RefreshCw, WalletCards } from '@lucide/vue'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTripWalletStore } from '@/stores/tripWallet'

const router = useRouter()
const wallet = useTripWalletStore()
const activeFilter = ref('ALL')
const loading = ref(true)
const notice = ref('')

const filters = [
  { key: 'ALL', label: '전체' },
  { key: 'PAYMENT', label: '현지 결제' },
  { key: 'EXCHANGE', label: '충전·빼기' },
  { key: 'REFUND', label: '환불' },
]

const filteredTransactions = computed(() => {
  const items = wallet.travelCardTransactions || []
  if (activeFilter.value === 'PAYMENT') {
    return items.filter(item => item.sourceType === 'CARD_PAYMENT' && item.transactionType !== 'PAYMENT_REFUND')
  }
  if (activeFilter.value === 'EXCHANGE') {
    return items.filter(item => item.sourceType === 'CARD_LEDGER' && !isRefund(item))
  }
  if (activeFilter.value === 'REFUND') return items.filter(isRefund)
  return items
})

const groupedTransactions = computed(() => {
  const groups = []
  const byDate = new Map()
  filteredTransactions.value.forEach(item => {
    const dateKey = String(item.occurredAt || '').slice(0, 10)
    if (!byDate.has(dateKey)) {
      const group = { dateKey, label: dateLabel(dateKey), items: [] }
      byDate.set(dateKey, group)
      groups.push(group)
    }
    byDate.get(dateKey).items.push(item)
  })
  return groups
})

function isRefund(item) {
  return item.transactionType === 'PAYMENT_REFUND' || item.transactionType === 'REFUND'
}

function dateLabel(value) {
  if (!value) return '날짜 미상'
  const date = new Date(`${value}T00:00:00`)
  return new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' }).format(date)
}

function timeLabel(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(11, 16)
  return new Intl.DateTimeFormat('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false }).format(date)
}

function titleOf(item) {
  if (item.sourceType === 'CARD_PAYMENT') return item.merchantName || '트래블카드 결제'
  if (item.transactionType === 'REFUND') return '외화 환불'
  if (item.direction === 'IN') return '외화 충전'
  return '외화 빼기'
}

function typeLabel(item) {
  if (item.sourceType === 'CARD_PAYMENT') return isRefund(item) ? '결제 환불' : '현지 결제'
  if (item.transactionType === 'REFUND') return '환불'
  return item.direction === 'IN' ? '외화 충전' : '외화 빼기'
}

function foreignMoney(item) {
  const amount = Number(item.foreignAmount || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 })
  return `${item.currencyCode || 'KRW'} ${amount}`
}

function krwMoney(value) {
  return `${Number(value || 0).toLocaleString('ko-KR')}원`
}

onMounted(async () => {
  try {
    await Promise.all([wallet.loadWalletMain(), wallet.loadTravelCardTransactions()])
  } catch {
    notice.value = wallet.errorMessage || '트래블카드 거래내역을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="transactions-page">
    <header class="page-header">
      <button type="button" @click="router.back()"><ChevronLeft :size="22" /></button>
      <div>
        <small>TRAVEL CARD</small>
        <h1>트래블카드 거래내역</h1>
      </div>
      <span />
    </header>

    <section v-if="wallet.isTravelCardLinked" class="card-summary">
      <div class="card-icon"><WalletCards :size="25" /></div>
      <div>
        <small>{{ wallet.travelCard.issuer }}</small>
        <h2>{{ wallet.travelCard.name }}</h2>
        <p>{{ wallet.travelCard.number }}</p>
      </div>
    </section>

    <nav class="filter-tabs" aria-label="거래 유형 필터">
      <button
        v-for="filter in filters"
        :key="filter.key"
        type="button"
        :class="{ active: activeFilter === filter.key }"
        @click="activeFilter = filter.key"
      >
        {{ filter.label }}
      </button>
    </nav>

    <div v-if="loading" class="state-card">
      <RefreshCw class="spin" :size="24" />
      <p>트래블카드 내역을 불러오고 있어요.</p>
    </div>

    <div v-else-if="notice" class="state-card error">{{ notice }}</div>

    <section v-else-if="groupedTransactions.length" class="transaction-groups">
      <div v-for="group in groupedTransactions" :key="group.dateKey" class="transaction-day">
        <h2>{{ group.label }}</h2>
        <div class="transaction-list">
          <article v-for="item in group.items" :key="item.transactionId" class="transaction-item">
            <div class="transaction-icon" :class="item.sourceType === 'CARD_PAYMENT' ? 'payment' : 'exchange'">
              <CreditCard v-if="item.sourceType === 'CARD_PAYMENT'" :size="21" />
              <RefreshCw v-else :size="20" />
            </div>
            <div class="transaction-copy">
              <div class="title-line">
                <strong>{{ titleOf(item) }}</strong>
                <span :class="{ refund: isRefund(item) }">{{ typeLabel(item) }}</span>
              </div>
              <p>
                {{ timeLabel(item.occurredAt) }}
                <template v-if="item.countryName"> · {{ item.countryName }}</template>
                <template v-if="item.categoryName"> · {{ item.categoryName }}</template>
              </p>
              <small v-if="item.memo && item.memo !== titleOf(item)">{{ item.memo }}</small>
            </div>
            <div class="transaction-amount" :class="item.direction === 'IN' ? 'incoming' : 'outgoing'">
              <strong>{{ item.direction === 'IN' ? '+' : '-' }}{{ foreignMoney(item) }}</strong>
              <small v-if="item.krwAmount">{{ krwMoney(item.krwAmount) }}</small>
              <small v-else-if="item.balanceAfter != null">잔액 {{ item.currencyCode }} {{ Number(item.balanceAfter).toLocaleString('ko-KR', { maximumFractionDigits: 2 }) }}</small>
            </div>
          </article>
        </div>
      </div>
    </section>

    <div v-else class="state-card">
      <CreditCard :size="28" />
      <h2>아직 거래내역이 없어요</h2>
      <p>외화 충전과 현지 카드 결제 내역이 여기에 함께 표시돼요.</p>
    </div>

    <BottomNav />
  </main>
</template>

<style scoped>
.transactions-page{max-width:430px;min-height:100vh;margin:0 auto;padding:14px 16px 104px;background:#f2f5fa;color:#111827;word-break:keep-all}.page-header{display:grid;grid-template-columns:38px 1fr 38px;align-items:center}.page-header>button{display:grid;width:38px;height:38px;place-items:center;border-radius:13px;background:#fff;color:#173f8c;box-shadow:0 5px 16px rgba(36,72,117,.08)}.page-header div{text-align:center}.page-header small{color:#2f70e9;font-size:9px;font-weight:900;letter-spacing:.17em}.page-header h1{margin-top:2px;color:#243c63;font-size:20px;font-weight:800;letter-spacing:-.04em}.card-summary{position:relative;display:grid;grid-template-columns:48px 1fr;gap:13px;align-items:center;margin-top:20px;overflow:hidden;padding:20px;border-radius:22px;background:linear-gradient(135deg,#12377f,#2d72d8);color:#fff;box-shadow:0 14px 28px rgba(24,67,145,.2)}.card-summary:after{content:"";position:absolute;right:-34px;top:-54px;width:140px;height:140px;border-radius:50%;background:rgba(255,255,255,.1)}.card-icon{display:grid;width:48px;height:48px;place-items:center;border:1px solid rgba(255,255,255,.24);border-radius:16px;background:rgba(255,255,255,.12)}.card-summary div{position:relative;z-index:1}.card-summary small{color:#cbdaf7;font-size:11px;font-weight:700}.card-summary h2{margin-top:3px;font-size:17px;font-weight:800}.card-summary p{margin-top:5px;color:#dce7fa;font-size:11px}.filter-tabs{display:grid;grid-template-columns:repeat(4,1fr);gap:4px;margin-top:16px;padding:4px;border-radius:15px;background:#e4eaf3}.filter-tabs button{height:37px;border-radius:12px;color:#8492a7;font-size:11px;font-weight:700}.filter-tabs button.active{background:#fff;color:#1951ad;box-shadow:0 3px 10px rgba(24,52,96,.08)}.transaction-groups{margin-top:22px}.transaction-day+.transaction-day{margin-top:22px}.transaction-day>h2{padding:0 4px;color:#74839a;font-size:12px;font-weight:800}.transaction-list{margin-top:9px;overflow:hidden;border-radius:20px;background:#fff;box-shadow:0 9px 24px rgba(18,43,82,.06)}.transaction-item{display:grid;grid-template-columns:40px minmax(0,1fr) auto;gap:11px;align-items:center;padding:15px}.transaction-item+.transaction-item{border-top:1px solid #e8edf4}.transaction-icon{display:grid;width:40px;height:40px;place-items:center;border-radius:13px}.transaction-icon.payment{background:#eaf2ff;color:#2767d7}.transaction-icon.exchange{background:#fff3cf;color:#b77b00}.transaction-copy{min-width:0}.title-line{display:flex;min-width:0;align-items:center;gap:6px}.title-line strong{overflow:hidden;font-size:14px;font-weight:800;text-overflow:ellipsis;white-space:nowrap}.title-line span{flex:none;padding:4px 6px;border-radius:99px;background:#eaf2ff;color:#2563c9;font-size:8px;font-weight:800}.title-line span.refund{background:#e8f8ef;color:#12875d}.transaction-copy p{margin-top:5px;color:#8a98ac;font-size:10px;font-weight:600}.transaction-copy>small{display:block;margin-top:4px;overflow:hidden;color:#a2adbd;font-size:9px;text-overflow:ellipsis;white-space:nowrap}.transaction-amount{text-align:right}.transaction-amount strong{display:block;font-size:13px;font-weight:800;white-space:nowrap}.transaction-amount small{display:block;margin-top:5px;color:#96a3b5;font-size:9px;font-weight:600}.transaction-amount.outgoing strong{color:#17233a}.transaction-amount.incoming strong{color:#168363}.state-card{display:grid;min-height:180px;margin-top:22px;place-items:center;align-content:center;gap:8px;padding:24px;border-radius:22px;background:#fff;color:#8492a7;text-align:center;box-shadow:0 9px 24px rgba(18,43,82,.05)}.state-card h2{color:#334155;font-size:16px;font-weight:800}.state-card p{font-size:11px;line-height:1.5}.state-card.error{color:#dc4b4b}.spin{animation:spin 1s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:380px){.transaction-item{grid-template-columns:36px minmax(0,1fr)}.transaction-amount{grid-column:2;text-align:left}.filter-tabs button{font-size:10px}}@media(prefers-reduced-motion:reduce){.spin{animation:none}}
</style>
