<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useCardStore } from '@/stores/cardStore'

const route = useRoute()
const router = useRouter()
const cardStore = useCardStore()
const cardId = Number(route.params.cardId)

const loading = ref(false)
const syncing = ref(false)
const syncMessage = ref('')

const card = computed(() => cardStore.cards.find((item) => item.id === cardId))

function toLocalDateStr(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function threeMonthsAgoFrom(base) {
  const date = new Date(base.getFullYear(), base.getMonth() - 3, 1)
  const lastDay = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()
  date.setDate(Math.min(base.getDate(), lastDay))
  return date
}

const now = new Date()
const initialSyncStartDate = toLocalDateStr(threeMonthsAgoFrom(now))
const startDate = ref(initialSyncStartDate)
const endDate = ref(toLocalDateStr(now))
const DAYS = ['일', '월', '화', '수', '목', '금', '토']

function monthlyRanges(from, to) {
  const ranges = []
  const cursor = new Date(`${from}T00:00:00`)
  const end = new Date(`${to}T00:00:00`)

  while (cursor <= end) {
    const monthEnd = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 0)
    const rangeEnd = monthEnd < end ? monthEnd : end
    ranges.push({ startDate: toLocalDateStr(cursor), endDate: toLocalDateStr(rangeEnd) })
    cursor.setFullYear(rangeEnd.getFullYear(), rangeEnd.getMonth(), rangeEnd.getDate() + 1)
  }
  return ranges
}

function parseDate(value) {
  if (Array.isArray(value)) return value.map(Number)
  return String(value ?? '').split('-').map(Number)
}

function parseTime(value) {
  if (Array.isArray(value)) return value.map(Number)
  return String(value ?? '00:00').split(':').map(Number)
}

const groups = computed(() => cardStore.cardTransactions.reduce((result, transaction) => {
  const [year, month, day] = parseDate(transaction.transactionDate)
  if (!year || !month || !day) return result

  const date = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  const jsDate = new Date(year, month - 1, day)
  const label = `${date.replaceAll('-', '.')} (${DAYS[jsDate.getDay()]})`
  const [hour = 0, minute = 0] = parseTime(transaction.transactionTime)
  const item = {
    id: transaction.id,
    merchant: transaction.merchantName ?? '(가맹점명 없음)',
    category: transaction.categoryName ?? '기타',
    method: card.value?.cardName ?? '',
    amount: -Math.abs(Number(transaction.amount)),
    time: `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`,
    dateLabel: label,
    balanceAfter: null,
    memo: transaction.memo ?? '',
    merchantType: transaction.merchantType ?? '',
    isReal: true,
    sourceType: 'CARD',
  }

  const group = result.find((entry) => entry.date === date)
  if (group) group.items.push(item)
  else result.push({ date, label, items: [item] })
  return result
}, []))

async function loadTransactions() {
  loading.value = true
  try {
    await cardStore.loadCardTransactions(cardId, startDate.value, endDate.value)
  } finally {
    loading.value = false
  }
}

async function syncTransactions({ initial = false } = {}) {
  syncing.value = true
  syncMessage.value = ''
  try {
    const syncRanges = monthlyRanges(
      initial ? initialSyncStartDate : startDate.value,
      endDate.value,
    )

    for (const range of syncRanges) {
      await cardStore.fetchAndSaveCardTransactions(cardId, range.startDate, range.endDate)
    }
    await cardStore.loadCardTransactions(cardId, startDate.value, endDate.value)
    await cardStore.loadCards()
    syncMessage.value = '최신 거래내역을 불러왔습니다.'
  } catch (error) {
    console.error('카드 거래내역 동기화 실패', error)
    syncMessage.value = error.response?.data?.message ?? '거래내역 동기화에 실패했습니다.'
  } finally {
    syncing.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    if (cardStore.cards.length === 0) await cardStore.loadCards()
    await cardStore.loadCardTransactions(cardId, startDate.value, endDate.value)

    // 신규 연동 카드는 최초 상세 진입 시에만 최근 3개월 거래를 자동 수집한다.
    if (card.value && !card.value.lastSyncedAt) await syncTransactions({ initial: true })
  } finally {
    loading.value = false
  }
})

watch([startDate, endDate], () => {
  if (startDate.value && endDate.value && startDate.value <= endDate.value) loadTransactions()
})

function cardTypeLabel(type) {
  return type === 'CHECK' ? '체크카드' : '신용카드'
}

function openTransaction(item) {
  router.push({ path: `/asset/transactions/${item.id}`, state: { item } })
}
</script>

<template>
  <main class="card-page">
    <header>
      <button type="button" aria-label="뒤로 가기" @click="router.back()">‹</button>
      <h1>{{ card?.cardName ?? '카드 거래내역' }}</h1>
    </header>

    <section class="card-ticket">
      <i class="notch left"></i><i class="notch right"></i>
      <small>▰　{{ cardTypeLabel(card?.cardType) }}</small>
      <strong>{{ card?.cardName ?? '연동 카드' }}</strong>
      <p>{{ card?.maskedCardNumber || '카드번호 비공개' }}</p>
      <span class="barcode">||||||||</span>
    </section>

    <section class="date-filter">
      <label><span>시작일</span><input v-model="startDate" type="date" :max="endDate"></label>
      <i>~</i>
      <label><span>종료일</span><input v-model="endDate" type="date" :min="startDate" :max="toLocalDateStr(now)"></label>
    </section>

    <p v-if="syncMessage" class="sync-message" :class="{ error: syncMessage.includes('실패') || syncMessage.includes('오류') }">{{ syncMessage }}</p>

    <div class="section-header">
      <span>카드내역</span>
      <button type="button" class="sync-btn" :disabled="syncing" @click="syncTransactions()">
        {{ syncing ? '동기화 중' : '↻ 최신 내역' }}
      </button>
    </div>

    <TransactionGroups :groups="groups" :show-icons="true" :loading="loading || syncing" @select="openTransaction" />
  </main>
</template>

<style scoped>
.card-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 30px;background:#f4f6fc;color:#10192d}header{display:grid;grid-template-columns:30px 1fr;align-items:center;margin-bottom:17px}header button{border:0;background:transparent;font-size:26px;text-align:left}h1{margin:0;font-size:17px;font-weight:900}.card-ticket{position:relative;min-height:130px;padding:20px;border-radius:17px;background:linear-gradient(115deg,#102b70,#0e62b9);color:white;box-shadow:0 10px 20px #173d8925}.card-ticket small{display:block;color:#ffcf28;font-size:11px;font-weight:900}.card-ticket strong{display:block;margin-top:12px;overflow:hidden;font-size:22px;letter-spacing:-1px;text-overflow:ellipsis;white-space:nowrap}.card-ticket p{position:absolute;left:20px;bottom:16px;margin:0;color:#ffffffa6;font-size:10px}.barcode{position:absolute;right:16px;bottom:10px;color:#ffffffbf;font-size:12px;letter-spacing:-2px}.notch{position:absolute;top:76px;width:14px;height:14px;border-radius:50%;background:#f4f6fc}.notch.left{left:-7px}.notch.right{right:-7px}.date-filter{display:grid;grid-template-columns:1fr auto 1fr;align-items:end;gap:7px;margin-top:10px;padding:10px 12px;border:1px solid #dbe3ef;border-radius:12px;background:#fff}.date-filter label span{display:block;margin-bottom:5px;color:#94a3b8;font-size:8px}.date-filter input{width:100%;border:0;background:transparent;font-size:9px}.date-filter i{padding-bottom:2px;color:#94a3b8;font-size:9px;font-style:normal}.section-header{display:flex;justify-content:space-between;align-items:center;margin:12px 0}.section-header>span{font-size:13px;font-weight:900}.sync-btn{padding:8px 11px;border:0;border-radius:9px;background:#172f6b;color:#fff;font-size:10px;font-weight:800}.sync-btn:disabled{opacity:.6}.sync-message{margin:10px 0 0;padding:9px 12px;border-radius:9px;background:#e8f1ff;color:#1a56db;font-size:10px;text-align:center}.sync-message.error{background:#ffebee;color:#c62828}
</style>
