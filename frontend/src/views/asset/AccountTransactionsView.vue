<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore, bankPresentationByName } from '@/stores/asset'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const asset = useAssetStore()

// 계좌명에서 은행을 추정해 브랜드 컬러/심볼을 찾는다(목업/실계좌 공통).
function resolveBankMeta(name) {
  const value = name ?? ''
  if (value.includes('국민') || value.includes('KB')) return bankPresentationByName('KB국민은행')
  if (value.includes('신한')) return bankPresentationByName('신한은행')
  if (value.includes('우리')) return bankPresentationByName('우리은행')
  if (value.includes('하나')) return bankPresentationByName('하나은행')
  if (value.includes('농협')) return bankPresentationByName('NH농협은행')
  if (value.includes('기업')) return bankPresentationByName('IBK기업은행')
  if (value.includes('K뱅크') || value.includes('케이뱅크')) return bankPresentationByName('K뱅크')
  if (value.includes('대구')) return bankPresentationByName('대구은행')
  if (value.includes('카카오')) return bankPresentationByName('카카오뱅크')
  return bankPresentationByName(null)
}
const isReal = route.query.isReal === 'true'
const filter = ref('all')

function toLocalDateStr(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function threeMonthsAgoFrom(base) {
  // setMonth()는 대상 월에 없는 일자(예: 5월 31일 - 3개월)를 만나면 다음 달로 넘어가므로,
  // 1일로 이동한 뒤 대상 월의 마지막 날짜를 넘지 않게 보정한다.
  const d = new Date(base.getFullYear(), base.getMonth() - 3, 1)
  const lastDayOfTargetMonth = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate()
  d.setDate(Math.min(base.getDate(), lastDayOfTargetMonth))
  return d
}

const today = toLocalDateStr(new Date())
const threeMonthsAgo = toLocalDateStr(threeMonthsAgoFrom(new Date()))
const startDate = ref(isReal ? threeMonthsAgo : '2026-06-20')
const endDate = ref(isReal ? today : '2026-07-19')
const tabs = [{ id: 'all', label: '전체' }, { id: 'deposit', label: '입금' }, { id: 'withdrawal', label: '출금' }]
const realAccount = ref({ name: '', number: '', type: '', bank: '', balance: 0 })
const realTransactions = ref([])

const DAYS = ['일', '월', '화', '수', '목', '금', '토']

async function fetchRealTransactions() {
  try {
    const params = { startDate: startDate.value, endDate: endDate.value }
    if (filter.value !== 'all') params.type = filter.value.toUpperCase()
    const res = await api.get(`/accounts/${route.params.accountId}/transactions`, { params })
    const data = res.data.data
    realAccount.value = {
      name: data.accountName ?? '',
      number: data.accountNumber ?? '',
      type: data.accountType ?? '',
      bank: (data.accountName ?? '').split(' ')[0],
      balance: data.balance,
    }
    realTransactions.value = data.transactions ?? []
  } catch (e) {
    console.error('거래내역 조회 실패', e)
  }
}

const loading = ref(false)
const syncing = ref(false)
const syncMessage = ref('')

async function fetchRealTransactionsWithLoading() {
  loading.value = true
  await fetchRealTransactions()
  loading.value = false
}

async function syncTransactions() {
  if (!isReal) return
  syncing.value = true
  syncMessage.value = ''
  try {
    await api.post('/accounts/transactions', {
      accountId: Number(route.params.accountId),
      startDate: startDate.value,
      endDate: endDate.value,
    })
    await fetchRealTransactions()
    syncMessage.value = '최신 거래내역을 불러왔습니다.'
  } catch (e) {
    console.error('거래내역 동기화 실패', e)
    syncMessage.value = e.response?.data?.message ?? '거래내역 동기화에 실패했습니다.'
  } finally {
    syncing.value = false
  }
}

onMounted(async () => {
  if (!isReal) return
  loading.value = true
  await fetchRealTransactions()
  if (realTransactions.value.length === 0) await syncTransactions()
  loading.value = false
})

watch([startDate, endDate, filter], () => {
  if (isReal) fetchRealTransactions()
})

const account = computed(() => isReal ? realAccount.value : (asset.getAccount(route.params.accountId) || asset.accounts[0]))

function maskedAccountNumber(number) {
  const value = String(number ?? '').replace(/\s/g, '')
  if (!value) return '계좌번호 정보 없음'
  if (value.length <= 4) return `•••• ${value}`
  return `•••• •••• ${value.slice(-4)}`
}

const accountTransactions = computed(() => isReal ? [] : asset.transactionsByAccount(account.value?.id))
const travelRecognizedAmount = computed(() => accountTransactions.value
  .filter((item) => item.country || item.category === '여행비')
  .reduce((sum, item) => sum + Math.abs(Math.min(0, item.amount)), 0))

const groups = computed(() => {
  if (isReal) {
    return realTransactions.value.reduce((result, t) => {
      const [y, mo, d] = Array.isArray(t.transactionDate) ? t.transactionDate : t.transactionDate.split('-').map(Number)
      const date = `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`
      const jsDate = new Date(y, mo - 1, d)
      const label = `${date.replaceAll('-', '.')} (${DAYS[jsDate.getDay()]})`
      const [h = 0, m = 0] = Array.isArray(t.transactionTime) ? t.transactionTime : (t.transactionTime ?? '00:00').split(':').map(Number)
      const time = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
      const amount = t.transactionType === 'DEPOSIT' ? Number(t.amount) : -Number(t.amount)
      const isCardPayment = Boolean(t.cardId)
      const item = {
        id: t.id,
        merchant: t.merchantName ?? '(내용없음)',
        category: t.categoryName ?? '기타',
        method: isCardPayment
          ? (t.paymentMethodName || t.sourceCardType || '카드 결제')
          : (realAccount.value.name || route.query.name || ''),
        amount,
        time,
        dateLabel: label,
        balanceAfter: Number(t.balanceAfter ?? 0),
        memo: t.memo ?? '',
        isReal: true,
        sourceType: isCardPayment ? 'CARD' : 'ACCOUNT',
        isCardPayment,
      }
      const group = result.find((g) => g.date === date)
      if (group) group.items.push(item)
      else result.push({ date, label, items: [item] })
      return result
    }, [])
  }
  return accountTransactions.value
    .filter((item) => {
      if (item.date < startDate.value || item.date > endDate.value) return false
      if (filter.value === 'deposit') return item.amount > 0
      if (filter.value === 'withdrawal') return item.amount < 0
      return true
    })
    .reduce((result, item) => {
      const group = result.find((entry) => entry.date === item.date)
      if (group) group.items.push(item)
      else result.push({ date: item.date, label: item.dateLabel, items: [item] })
      return result
    }, [])
})
</script>

<template>
  <main class="account-page">
    <header class="account-header"><button @click="router.back()">‹</button><h1>거래내역 조회</h1></header>

    <section class="account-overview">
      <div class="account-identity">
        <span class="bank-mark" :style="{ background: resolveBankMeta(account.name).color, color: resolveBankMeta(account.name).text }">{{ resolveBankMeta(account.name).symbol }}</span>
        <div><strong>{{ account.name }}</strong><small>{{ maskedAccountNumber(account.number) }} · {{ account.type }}</small></div>
      </div>
      <div class="balance-block"><small>현재 잔액</small><strong>{{ Number(account.balance ?? 0).toLocaleString('ko-KR') }}<em>원</em></strong></div>
      <div class="overview-meta"><span>여행 자금 인정 금액</span><b>{{ travelRecognizedAmount.toLocaleString('ko-KR') }}원</b></div>
    </section>

    <div class="quick-actions">
      <button v-if="isReal" type="button" :disabled="syncing" @click="syncTransactions"><span>↻</span>{{ syncing ? '동기화 중' : '최신 내역' }}</button>
      <button type="button" @click="router.push('/asset/transactions/calendar')"><span>▦</span>달력 보기</button>
    </div>

    <p v-if="syncMessage" class="sync-message" :class="{ error: syncMessage.includes('실패') || syncMessage.includes('오류') }">{{ syncMessage }}</p>

    <section class="history-panel">
      <div class="history-title"><div><small>ACCOUNT HISTORY</small><h2>거래내역</h2></div><span>{{ groups.reduce((sum, group) => sum + group.items.length, 0) }}건</span></div>
      <section class="date-filter"><label><span>시작일</span><input v-model="startDate" type="date" :max="endDate"></label><i>–</i><label><span>종료일</span><input v-model="endDate" type="date" :min="startDate"></label></section>
    <div class="tabs"><button v-for="tab in tabs" :key="tab.id" :class="{ active: filter === tab.id }" type="button" @click="filter = tab.id">{{ tab.label }}</button></div>
    <TransactionGroups :groups="groups" :show-icons="false" :loading="loading" @select="isReal ? router.push({ path: `/asset/transactions/${$event.id}`, state: { item: $event } }) : router.push(`/asset/transactions/${$event.id}`)" />
    </section>
  </main>
</template>

<style scoped>
.account-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:0 0 34px;background:#eef2f8;color:#10192d}
.account-header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center;height:92px;margin:0;padding:38px 20px 0;background:#eef2f8}
.account-header button{width:36px;height:36px;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}
.account-header h1{font-size:17px;font-weight:900;letter-spacing:-.03em;text-align:center}
.account-overview{margin:0 20px;padding:20px;border-radius:20px;background:#fff;box-shadow:0 8px 22px rgba(16,25,43,.05)}
.account-identity{display:flex;align-items:center;gap:10px}
.bank-mark{display:grid;width:38px;height:38px;place-items:center;border-radius:12px;font-size:14px;font-weight:900}
.account-identity>div{min-width:0}
.account-identity strong,.account-identity small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.account-identity strong{color:#10192d;font-size:14px;font-weight:800}
.account-identity small{margin-top:4px;color:#94a3b8;font-size:9px}
.balance-block{margin-top:22px;text-align:right}
.balance-block small{display:block;color:#94a3b8;font-size:10px}
.balance-block strong{display:block;margin-top:5px;color:#10192d;font-size:27px;font-weight:800;letter-spacing:-.045em}
.balance-block em{margin-left:2px;font-size:14px;font-style:normal;font-weight:700}
.overview-meta{display:flex;align-items:center;justify-content:flex-end;gap:8px;margin-top:10px;padding-top:10px;border-top:1px dashed #e7edf9;color:#7186aa;font-size:10px}
.overview-meta b{color:#173f8d;font-size:10px;font-weight:800}
.quick-actions{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;margin:12px 20px 0}
.quick-actions button{display:flex;align-items:center;justify-content:center;gap:6px;min-height:42px;border-radius:13px;background:#fff;color:#173f8d;font-size:11px;font-weight:800;box-shadow:0 5px 14px rgba(16,25,43,.04)}
.quick-actions button:disabled{opacity:.55}
.quick-actions span{color:#286ce0;font-size:15px}
.sync-message{margin:10px 20px 0;padding:9px 12px;border-radius:9px;background:#eaf2ff;color:#173f8d;font-size:10px;text-align:center}
.sync-message.error{background:#ffebee;color:#c62828}
.history-panel{margin:16px 20px 0;padding:20px 18px 4px;border-radius:20px;background:#fff;box-shadow:0 8px 22px rgba(16,25,43,.05)}
.history-title{display:flex;align-items:flex-end;justify-content:space-between}
.history-title small{display:block;color:#286ce0;font-family:'Space Mono',monospace;font-size:7.5px;font-weight:800;letter-spacing:.11em}
.history-title h2{margin-top:3px;color:#10192d;font-size:16px;font-weight:900}
.history-title>span{color:#94a3b8;font-size:10px;font-weight:700}
.date-filter{display:grid;grid-template-columns:1fr auto 1fr;align-items:end;gap:7px;margin-top:15px;padding:10px 12px;border:1px solid #e7edf9;border-radius:12px;background:#f7f9fd}
.date-filter label span{display:block;margin-bottom:5px;color:#94a3b8;font-size:8px}
.date-filter input{width:100%;color:#10192d;font-size:9px;font-weight:700}
.date-filter i{padding-bottom:2px;color:#94a3b8;font-size:9px;font-style:normal}
.tabs{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin:12px 0 15px;padding:4px;border-radius:12px;background:#f4f7fb;text-align:center}
.tabs button{padding:8px 0;border-radius:9px;color:#7186aa;font-size:10px;font-weight:700}
.tabs button.active{background:#fff;color:#173f8d;box-shadow:0 3px 8px rgba(29,50,82,.08)}
.history-panel :deep(.transaction-groups section){margin:0 0 20px}
.history-panel :deep(.transaction-groups h3){margin:0;padding:12px 2px 9px;border-bottom:1px solid #eef1f6;color:#7186aa;font-size:11px;font-weight:800}
.history-panel :deep(.transaction-groups section.without-icons button){grid-template-columns:minmax(0,1fr) auto;gap:10px;margin:0;padding:14px 2px;border:0;border-bottom:1px solid #eef1f6;border-radius:0;box-shadow:none}
.history-panel :deep(.transaction-groups .dot){display:none}
.history-panel :deep(.transaction-groups span b){color:#10192d;font-size:12px;font-weight:700}
.history-panel :deep(.transaction-groups span small){max-width:190px;margin-top:5px;color:#94a3b8;font-size:8.5px}
.history-panel :deep(.transaction-groups strong){font-size:12px;font-weight:800}
.history-panel :deep(.transaction-groups time){margin-top:5px;color:#94a3b8;font-size:8.5px}
.history-panel :deep(.transaction-groups .deposit){color:#173f8d}
.history-panel :deep(.transaction-groups .withdrawal){color:#e8484f}
</style>
