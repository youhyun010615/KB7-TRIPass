<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import TransactionFilterSheet from '@/components/asset/TransactionFilterSheet.vue'
import { useCardStore } from '@/stores/cardStore'
import { bankPresentationByCode, bankPresentationByName } from '@/stores/asset'
import { getTravelCardImage } from '@/utils/travelCard'
import kbTravelersImage from '@/assets/travel-cards/kb-travelers.png'
import kbTravelersTosimiImage from '@/assets/cards/kb-travelers-tosimi.png'
import kbCheckGenericImage from '@/assets/cards/kb-check-generic.png'

const route = useRoute()
const router = useRouter()
const cardStore = useCardStore()
const cardId = Number(route.params.cardId)

const loading = ref(false)
const syncing = ref(false)
const syncMessage = ref('')

const card = computed(() => cardStore.cards.find((item) => item.id === cardId))

function resolveCardMeta(value = {}) {
  const byCode = bankPresentationByCode(value.organizationCode)
  if (byCode.code) return byCode

  const name = value.cardName ?? ''
  if (name.includes('국민') || name.includes('KB')) return bankPresentationByName('KB국민은행')
  if (name.includes('신한')) return bankPresentationByName('신한은행')
  if (name.includes('우리')) return bankPresentationByName('우리은행')
  if (name.includes('하나')) return bankPresentationByName('하나은행')
  if (name.includes('농협')) return bankPresentationByName('NH농협은행')
  if (name.includes('기업')) return bankPresentationByName('IBK기업은행')
  if (name.includes('카카오')) return bankPresentationByName('카카오뱅크')
  return byCode
}

const cardImage = computed(() => {
  const value = card.value
  if (!value) return null
  const name = value.cardName ?? ''
  const isKb = resolveCardMeta(value).name === 'KB국민은행' || name.includes('KB') || name.includes('국민')
  if (isKb && name.includes('트래블')) return kbTravelersTosimiImage
  if (name.includes('트래블')) return getTravelCardImage(name) || getTravelCardImage(resolveCardMeta(value).name) || kbTravelersImage
  if (isKb && value.cardType === 'CHECK') return kbCheckGenericImage
  return value.cardImageUrl || value.imageUrl || null
})

const isKbCard = computed(() => {
  const name = card.value?.cardName ?? ''
  return resolveCardMeta(card.value).name === 'KB국민은행' || name.includes('KB') || name.includes('국민')
})

const cardIssuerName = computed(() => isKbCard.value ? '국민은행' : (resolveCardMeta(card.value).name || '연동 카드'))

function maskedCardNumber(number) {
  const value = String(number ?? '').trim()
  if (!value) return '카드번호 비공개'
  if (/[•*]/.test(value)) return value
  const digits = value.replace(/\D/g, '')
  return digits ? `•••• •••• •••• ${digits.slice(-4)}` : '카드번호 비공개'
}

const transactionCount = computed(() => groups.value.reduce((sum, group) => sum + group.items.length, 0))

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
const period = ref('3개월')
const sort = ref('latest')
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

const rawGroups = computed(() => cardStore.cardTransactions.reduce((result, transaction) => {
  const [year, month, day] = parseDate(transaction.transactionDate)
  if (!year || !month || !day) return result

  const date = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  const [hour = 0, minute = 0, second = 0] = parseTime(transaction.transactionTime)
  const time = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
  const item = {
    id: transaction.id,
    merchant: transaction.merchantName ?? '(가맹점명 없음)',
    category: transaction.categoryName ?? '기타',
    method: card.value?.cardName ?? '',
    amount: -Math.abs(Number(transaction.amount)),
    time,
    transactionDate: date,
    transactionDateText: time,
    balanceAfter: null,
    memo: transaction.memo ?? '',
    merchantType: transaction.merchantType ?? '',
    isReal: true,
    sourceType: 'CARD',
  }

  const group = result.find((entry) => entry.date === date)
  if (group) group.items.push(item)
  else result.push({ date, label: `${year}.${String(month).padStart(2, '0')}.${String(day).padStart(2, '0')} ${DAYS[new Date(year, month - 1, day).getDay()]}요일`, items: [item] })
  return result
}, []))

const groups = computed(() => rawGroups.value
  .map((group) => ({
    ...group,
    items: [...group.items].sort((a, b) => {
      const aKey = `${a.transactionDate} ${a.time}`
      const bKey = `${b.transactionDate} ${b.time}`
      return sort.value === 'latest' ? bKey.localeCompare(aKey) : aKey.localeCompare(bKey)
    }),
  }))
  .sort((a, b) => sort.value === 'latest' ? b.date.localeCompare(a.date) : a.date.localeCompare(b.date)))

function applyFilters(value) {
  startDate.value = value.startDate
  endDate.value = value.endDate
  period.value = value.period
  sort.value = value.sort
}

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
    <header class="card-header">
      <button type="button" aria-label="뒤로 가기" @click="router.back()">‹</button>
      <h1>{{ card?.cardName ?? '카드 거래내역' }}</h1>
    </header>

    <section class="card-overview">
      <div class="card-preview" :class="{ 'has-photo': cardImage }" :style="{ '--card-color': resolveCardMeta(card).color, '--card-text': resolveCardMeta(card).text }">
        <img v-if="cardImage" :src="cardImage" :alt="`${card?.cardName ?? '연동 카드'} 카드 이미지`" />
        <template v-else>
          <span>{{ resolveCardMeta(card).name || 'TRIPASS CARD' }}</span>
          <i></i>
          <b>••••　{{ String(card?.maskedCardNumber ?? '').replace(/\D/g, '').slice(-4) || '0000' }}</b>
        </template>
      </div>
      <div class="card-identity">
        <small :class="{ 'kb-issuer': isKbCard }">{{ cardIssuerName }}</small>
        <strong>{{ card?.cardName ?? '연동 카드' }}</strong>
        <p>{{ maskedCardNumber(card?.maskedCardNumber) }} · {{ cardTypeLabel(card?.cardType) }}</p>
      </div>
    </section>

    <p v-if="syncMessage" class="sync-message" :class="{ error: syncMessage.includes('실패') || syncMessage.includes('오류') }">{{ syncMessage }}</p>

    <section class="history-panel">
      <div class="history-title">
        <div><small>CARD HISTORY</small><h2>카드내역</h2></div>
        <div class="history-tools">
          <button type="button" :disabled="syncing" @click="syncTransactions()"><span>↻</span>{{ syncing ? '동기화 중' : '최신 내역' }}</button>
        </div>
      </div>
      <TransactionFilterSheet :start-date="startDate" :end-date="endDate" :period="period" :sort="sort" :count="transactionCount" @apply="applyFilters" />
      <TransactionGroups :groups="groups" :show-icons="false" :bank-layout="true" :loading="loading || syncing" @select="openTransaction" />
    </section>
  </main>
</template>

<style scoped>
.card-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:0 0 34px;background:#eef2f8;color:#10192d}
.card-header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center;height:68px;padding:14px 20px 0}
.card-header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}
.card-header h1{overflow:hidden;margin:0;font-size:17px;font-weight:900;letter-spacing:-.03em;text-align:center;text-overflow:ellipsis;white-space:nowrap}
.card-overview{display:grid;grid-template-columns:86px minmax(0,1fr);align-items:center;gap:18px;margin:0 20px;padding:18px 20px;border-radius:20px;background:#fff;box-shadow:0 8px 22px rgba(16,25,43,.05)}
.card-preview{position:relative;width:86px;aspect-ratio:360/570;overflow:hidden;padding:10px;border-radius:11px;background:linear-gradient(155deg,var(--card-color,#173f8d),#102b70);color:var(--card-text,#fff);box-shadow:0 10px 20px rgba(16,43,112,.2)}
.card-preview.has-photo{padding:0;background:#eef2f8}
.card-preview img{width:100%;height:100%;object-fit:cover}
.card-preview span{display:block;overflow:hidden;font-size:7px;font-weight:800;text-overflow:ellipsis;white-space:nowrap}
.card-preview i{display:block;width:18px;height:13px;margin-top:20px;border-radius:4px;background:linear-gradient(135deg,#f6db8a,#c6a651)}
.card-preview b{position:absolute;left:10px;bottom:10px;font-size:5.5px;letter-spacing:.02em;writing-mode:vertical-rl}
.card-identity{min-width:0}
.card-identity small,.card-identity strong,.card-identity p{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.card-identity small{width:max-content;color:#286ce0;font-size:8px;font-weight:800}
.card-identity small.kb-issuer{padding:5px 8px;border-radius:999px;background:#ffcf33;color:#2f2600;font-size:9px;font-weight:900}
.card-identity strong{margin-top:5px;color:#10192d;font-size:14px;font-weight:800}
.card-identity p{margin:6px 0 0;color:#94a3b8;font-size:8px}
.sync-message{margin:10px 20px 0;padding:9px 12px;border-radius:9px;background:#eaf2ff;color:#173f8d;font-size:10px;text-align:center}.sync-message.error{background:#ffebee;color:#c62828}
.history-panel{margin:16px 20px 0;padding:20px 18px 4px;border-radius:20px;background:#fff;box-shadow:0 8px 22px rgba(16,25,43,.05)}
.history-title{display:flex;align-items:flex-end;justify-content:space-between}.history-title small{display:block;color:#286ce0;font-family:'Space Mono',monospace;font-size:7.5px;font-weight:800;letter-spacing:.11em}.history-title h2{margin:3px 0 0;color:#10192d;font-size:16px;font-weight:900}.history-tools{display:flex;align-items:flex-end;flex-direction:column;gap:5px}.history-tools button{display:flex;align-items:center;gap:4px;padding:6px 9px;border-radius:9px;background:#eaf2ff;color:#173f8d;font-size:8.5px;font-weight:900}.history-tools button:disabled{opacity:.55}.history-tools button span{color:#286ce0;font-size:12px}.history-tools strong{color:#94a3b8;font-size:10px;font-weight:700}
.history-panel :deep(.transaction-groups section){margin:0 0 20px}.history-panel :deep(.transaction-groups h3){margin:0;padding:12px 2px 9px;border-bottom:1px solid #eef1f6;color:#7186aa;font-size:11px;font-weight:800}.history-panel :deep(.transaction-groups section.without-icons button){grid-template-columns:minmax(0,1fr) auto;gap:10px;margin:0;padding:14px 2px;border:0;border-bottom:1px solid #eef1f6;border-radius:0;box-shadow:none}.history-panel :deep(.transaction-groups .dot){display:none}.history-panel :deep(.transaction-groups span b){color:#10192d;font-size:12px;font-weight:700}.history-panel :deep(.transaction-groups span small){max-width:190px;margin-top:5px;color:#94a3b8;font-size:8.5px}.history-panel :deep(.transaction-groups strong){font-size:12px;font-weight:800}.history-panel :deep(.transaction-groups time){margin-top:5px;color:#94a3b8;font-size:8.5px}.history-panel :deep(.transaction-groups .withdrawal){color:#e8484f}
</style>
