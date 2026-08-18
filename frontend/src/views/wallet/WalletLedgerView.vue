<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from '@lucide/vue'
import BottomNav from '@/components/common/BottomNav.vue'
import { toDateKey, toDayLabel, toTimeLabel, transactionTitleMap, useTripWalletStore } from '@/stores/tripWallet'

const router = useRouter()
const wallet = useTripWalletStore()
const activeTab = ref('all')
const sortOption = ref('newest')
const dateFrom = ref('')
const dateTo = ref('')
const notice = ref('')

const tabs = [
  { key: 'all', label: '전체' },
  { key: 'deposit', label: '채우기' },
  { key: 'withdraw', label: '빼기' },
]

const sortOptions = [
  { key: 'newest', label: '최신순' },
  { key: 'oldest', label: '오래된순' },
  { key: 'amount-desc', label: '금액 높은순' },
  { key: 'amount-asc', label: '금액 낮은순' },
]

function ledgerTitle(item) {
  return item.memo || transactionTitleMap[item.transactionType] || '거래'
}

const filteredLedgers = computed(() => {
  let items = wallet.ledgers
  if (activeTab.value === 'deposit') items = items.filter(item => item.direction === 'IN')
  else if (activeTab.value === 'withdraw') items = items.filter(item => item.direction === 'OUT')
  if (dateFrom.value) items = items.filter(item => toDateKey(item.createdAt) >= dateFrom.value)
  if (dateTo.value) items = items.filter(item => toDateKey(item.createdAt) <= dateTo.value)
  return items
})

const sortedLedgers = computed(() => {
  const items = filteredLedgers.value.slice()
  if (sortOption.value === 'oldest') items.sort((a, b) => a.createdAt.localeCompare(b.createdAt))
  else if (sortOption.value === 'amount-desc') items.sort((a, b) => b.amount - a.amount)
  else if (sortOption.value === 'amount-asc') items.sort((a, b) => a.amount - b.amount)
  else items.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  return items
})

// 날짜별로 묶어서 그날 합산 금액을 한 번만 보여주고, 각 거래는 자기 금액만 따로 보여준다.
const groupedByDate = computed(() => {
  const groups = []
  const byDate = new Map()
  sortedLedgers.value.forEach(item => {
    const dateKey = toDateKey(item.createdAt)
    let group = byDate.get(dateKey)
    if (!group) {
      group = { dateKey, dateLabel: toDayLabel(item.createdAt), total: 0, items: [] }
      byDate.set(dateKey, group)
      groups.push(group)
    }
    group.items.push(item)
    group.total += item.direction === 'OUT' ? -item.amount : item.amount
  })
  return groups
})

const money = value => `${Math.abs(Number(value || 0)).toLocaleString('ko-KR')}원`

onMounted(async () => {
  try {
    await wallet.loadLedgers()
  } catch {
    notice.value = wallet.errorMessage || '내역을 불러오지 못했어요.'
  }
})
</script>

<template>
  <main class="ledger-page">
    <header class="page-header">
      <button type="button" class="back-button" @click="router.back()"><ChevronLeft :size="22" /></button>
      <h1>월렛 내역</h1>
    </header>

    <nav class="segmented-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </nav>

    <div class="filter-row">
      <div class="date-filter">
        <input v-model="dateFrom" type="date" :max="dateTo || undefined">
        <span>~</span>
        <input v-model="dateTo" type="date" :min="dateFrom || undefined">
      </div>
      <select v-model="sortOption" class="sort-select">
        <option v-for="option in sortOptions" :key="option.key" :value="option.key">{{ option.label }}</option>
      </select>
    </div>

    <section v-for="group in groupedByDate" :key="group.dateKey" class="ledger-day">
      <div class="ledger-day-head">
        <h2>{{ group.dateLabel }}</h2>
        <strong>{{ group.total >= 0 ? '+' : '-' }}{{ money(group.total) }}</strong>
      </div>
      <div class="ledger-card">
        <div v-for="item in group.items" :key="item.ledgerId" class="ledger-line">
          <span :class="item.direction === 'IN' ? 'deposit' : 'withdraw'">{{ item.direction === 'IN' ? '↓' : '↑' }}</span>
          <div>
            <b>{{ ledgerTitle(item) }}</b>
            <small>{{ toTimeLabel(item.createdAt) }}<template v-if="item.accountName"> · {{ item.accountName }}</template></small>
          </div>
          <strong :class="item.direction === 'IN' ? 'deposit' : 'withdraw'">{{ item.direction === 'IN' ? '+' : '-' }}{{ money(item.amount) }}</strong>
        </div>
      </div>
    </section>
    <p v-if="notice" class="list-empty">{{ notice }}</p>
    <p v-else-if="!groupedByDate.length" class="list-empty">내역이 없어요.</p>

    <BottomNav />
  </main>
</template>

<style scoped>
.ledger-page{max-width:430px;min-height:100vh;margin:0 auto;padding:46px 18px 92px;background:#eef2f8;color:#111827}.page-header{display:flex;align-items:center;gap:14px}.page-header button{display:grid;width:36px;height:36px;place-items:center;color:#111827}.page-header h1{font-size:27px;font-weight:800;letter-spacing:-.04em}.segmented-tabs{display:grid;grid-template-columns:repeat(3,1fr);gap:4px;margin-top:24px;padding:5px;border-radius:999px;background:#e2e8f2}.segmented-tabs button{height:48px;border-radius:999px;color:#9aa6b8;font-size:15px;font-weight:700}.segmented-tabs button.active{background:#fff;color:#111827;box-shadow:0 3px 10px rgba(21,34,66,.05)}.filter-row{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-top:14px}.date-filter{display:flex;align-items:center;gap:6px;min-width:0}.date-filter input{width:0;flex:1;min-width:98px;padding:9px 8px;border:1px solid #dbe3ef;border-radius:11px;background:#fff;color:#374151;font-size:12px;font-weight:600}.date-filter span{color:#9aa6b8;font-size:12px}.sort-select{flex:none;padding:9px 10px;border:1px solid #dbe3ef;border-radius:11px;background:#fff;color:#374151;font-size:12px;font-weight:700}.ledger-day{margin-top:26px}.ledger-day-head{display:flex;align-items:baseline;justify-content:space-between}.ledger-day-head h2{color:#9aa6b8;font-size:15px;font-weight:700}.ledger-day-head strong{font-size:17px;font-weight:800;color:#111827}.ledger-card{margin-top:12px;padding:0 20px;border-radius:23px;background:#fff;box-shadow:0 10px 24px rgba(18,43,82,.06)}.ledger-line{display:grid;grid-template-columns:42px 1fr auto;align-items:center;gap:13px;padding:16px 0}.ledger-line+.ledger-line{border-top:1px solid #e3e9f3}.ledger-line span{display:grid;width:40px;height:40px;place-items:center;border-radius:13px;font-size:26px;font-weight:700}.ledger-line span.deposit{background:#eaf3ff;color:#2c6ef2}.ledger-line span.withdraw{background:#ffece9;color:#ef4444}.ledger-line b{display:block;font-size:16px;font-weight:800}.ledger-line small{display:block;margin-top:6px;color:#92a0b4;font-size:12px;font-weight:500}.ledger-line strong{font-size:16px;font-weight:800;white-space:nowrap}.deposit{color:#246bf2}.withdraw{color:#ef4444}.list-empty{margin-top:30px;padding:20px;border-radius:14px;background:#f7f9fd;color:#9aa8bd;font-size:12px;font-weight:600;text-align:center}@media(max-width:380px){.ledger-line{grid-template-columns:38px 1fr}.ledger-line strong{grid-column:2}.filter-row{flex-direction:column;align-items:stretch}.date-filter{flex-wrap:wrap}}
.ledger-page{word-break:keep-all}.ledger-page h1,.ledger-page h2,.ledger-page h3{text-wrap:balance}.ledger-page p{text-wrap:pretty}
.ledger-page{padding:42px 16px 88px;background:#f2f5fa}.page-header{gap:9px}.page-header button{width:32px;height:32px}.page-header h1{color:#29466f;font-size:20px;font-weight:700;letter-spacing:-.025em}
.segmented-tabs{margin-top:18px;padding:4px}.segmented-tabs button{height:36px;font-size:12px;font-weight:600}
.filter-row{gap:7px;margin-top:11px}.date-filter{gap:4px}.date-filter input{min-width:86px;padding:7px 6px;border-radius:9px;font-size:10px}.sort-select{padding:7px 8px;border-radius:9px;font-size:10px;font-weight:600}
.ledger-day{margin-top:20px}.ledger-day-head h2{font-size:12px;font-weight:600}.ledger-day-head strong{font-size:14px;font-weight:700}
.ledger-card{margin-top:9px;padding:0 14px;border-radius:18px}.ledger-line{grid-template-columns:34px 1fr auto;gap:10px;padding:12px 0}
.ledger-line span{width:32px;height:32px;border-radius:10px;font-size:18px}.ledger-line b{font-size:13px}.ledger-line small{margin-top:4px;font-size:10px}.ledger-line strong{font-size:13px}
</style>
