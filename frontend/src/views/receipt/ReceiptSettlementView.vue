<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Check, ChevronLeft, ChevronRight, HandCoins, ReceiptText, Users } from '@lucide/vue'
import BottomNav from '@/components/common/BottomNav.vue'
import {
  getParticipantSettlement,
  getReceiptSettlements,
  toggleReceiptSettlement,
} from '@/api/receipt'

const route = useRoute()
const router = useRouter()
const tripId = computed(() => {
  const value = Number(route.params.tripId)
  return Number.isInteger(value) && value > 0 ? value : null
})
const routeParticipantName = computed(() => String(route.params.participantName || ''))
const summaryLoading = ref(false)
const detailLoading = ref(false)
const toggleLoadingName = ref('')
const errorMessage = ref('')
const detailErrorMessage = ref('')
const selectedParticipantName = ref('')
const summary = ref({ totalAmounts: [], participantCount: 0, participants: [] })
const detail = ref({ participantName: '', receipts: [] })
const selectedParticipant = computed(() =>
  summary.value.participants.find(item => item.participantName === selectedParticipantName.value) || null,
)

function dataOf(response) {
  return response.data?.data ?? response.data ?? {}
}

function formatAmount(value) {
  return Number(value || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 })
}

function normalizeAmounts(amounts) {
  return Array.isArray(amounts)
    ? amounts.map(item => ({
        currencyCode: item.currencyCode || '',
        currencySymbol: item.currencySymbol || '',
        amount: Number(item.amount) || 0,
      }))
    : []
}

function amountLabel(amount) {
  return `${amount.currencySymbol || amount.currencyCode || '통화'} ${formatAmount(amount.amount)}`
}

function receiptMerchant(item) {
  return item.merchantTranslatedName || item.merchantOriginalName || '상호명 미인식'
}

function receiptDate(item) {
  const value = String(item.paymentDateTime || '')
  return value ? value.replace('T', ' ').slice(0, 16) : '결제일 미확인'
}

function receiptAmount(item) {
  return `${item.currencySymbol || item.currencyCode || ''} ${formatAmount(item.totalAmount)}`.trim()
}

async function loadParticipant(name) {
  if (!tripId.value || !name) return
  selectedParticipantName.value = name
  detailLoading.value = true
  detailErrorMessage.value = ''
  detail.value = { participantName: name, receipts: [] }

  try {
    const data = dataOf(await getParticipantSettlement(tripId.value, name))
    detail.value = {
      participantName: data.participantName || name,
      receipts: Array.isArray(data.receipts) ? data.receipts : [],
    }
  } catch (error) {
    detailErrorMessage.value = error.response?.data?.message || '참여자의 영수증을 불러오지 못했습니다.'
  } finally {
    detailLoading.value = false
  }
}

async function loadSettlement() {
  if (!tripId.value) {
    errorMessage.value = '여행 정보를 확인해 주세요.'
    return
  }
  summaryLoading.value = true
  errorMessage.value = ''

  try {
    const data = dataOf(await getReceiptSettlements(tripId.value))
    summary.value = {
      totalAmounts: normalizeAmounts(data.totalAmounts),
      participantCount: Number(data.participantCount) || 0,
      participants: Array.isArray(data.participants)
        ? data.participants.map(item => ({
            ...item,
            receiptCount: Number(item.receiptCount) || 0,
            settled: Boolean(item.settled),
            amounts: normalizeAmounts(item.amounts),
          }))
        : [],
    }

    const requestedName = routeParticipantName.value || selectedParticipantName.value
    const nextParticipant = summary.value.participants.find(
      item => item.participantName === requestedName,
    ) || summary.value.participants[0]

    if (nextParticipant) await loadParticipant(nextParticipant.participantName)
    else {
      selectedParticipantName.value = ''
      detail.value = { participantName: '', receipts: [] }
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '정산 내역을 불러오지 못했습니다.'
  } finally {
    summaryLoading.value = false
  }
}

async function toggleSettlement(participant) {
  if (!tripId.value || toggleLoadingName.value) return
  const nextSettled = !participant.settled
  toggleLoadingName.value = participant.participantName
  try {
    await toggleReceiptSettlement(tripId.value, participant.participantName, nextSettled)
    participant.settled = nextSettled
  } catch (error) {
    window.alert(error.response?.data?.message || '정산 상태를 변경하지 못했습니다.')
  } finally {
    toggleLoadingName.value = ''
  }
}

function openReceipts() {
  router.push({ name: 'Receipt', params: { tripId: tripId.value } })
}

function openReceipt(receiptId) {
  router.push({ name: 'ReceiptDetail', params: { tripId: tripId.value, receiptId } })
}

watch(routeParticipantName, name => {
  if (name) loadParticipant(name)
})
onMounted(loadSettlement)
</script>

<template>
  <main class="settlement-page">
    <header class="page-header">
      <button type="button" aria-label="뒤로 가기" @click="router.back()">
        <ChevronLeft :size="24" :stroke-width="2.4" />
      </button>
      <div><small>TRIP SETTLEMENT</small><h1>여행 정산</h1></div>
      <span />
    </header>

    <section class="settlement-hero">
      <div class="hero-label"><HandCoins :size="17" /> SETTLEMENT SUMMARY</div>
      <p>함께 결제한 여행 경비를<br>참여자별로 확인해 보세요.</p>
      <div class="hero-total">
        <span>받을 총 정산 금액</span>
        <div class="amount-stack hero-amounts">
          <strong v-for="amount in summary.totalAmounts" :key="amount.currencyCode">
            {{ amountLabel(amount) }}
          </strong>
          <strong v-if="!summary.totalAmounts.length">—</strong>
        </div>
      </div>
      <small>{{ summary.participantCount }}명의 정산 내역이에요</small>
    </section>

    <nav class="vault-tabs" aria-label="영수증 보관함 메뉴">
      <button type="button" @click="openReceipts"><ReceiptText :size="16" /> 영수증</button>
      <button type="button" class="active"><HandCoins :size="17" /> 정산</button>
    </nav>

    <section class="participant-section">
      <div class="content-title">
        <div><small>PARTICIPANTS</small><h2>참여자별 정산</h2></div>
        <span>{{ summary.participants.length }}명</span>
      </div>

      <div v-if="summaryLoading" class="state-box compact">
        <span class="loading-mark">···</span><b>정산 내역을 불러오고 있어요</b>
      </div>
      <div v-else-if="errorMessage" class="state-box compact error">
        <span>!</span><b>{{ errorMessage }}</b>
        <button type="button" @click="loadSettlement">다시 시도</button>
      </div>

      <div v-else class="participant-list">
        <article
            v-for="participant in summary.participants"
            :key="participant.participantName"
            class="participant-row"
            :class="{
              selected: selectedParticipantName === participant.participantName,
              settled: participant.settled,
            }"
        >
          <button type="button" class="participant-select" @click="loadParticipant(participant.participantName)">
            <span class="row-avatar">{{ String(participant.participantName || '?').slice(0, 1).toUpperCase() }}</span>
            <span class="participant-copy">
              <span class="participant-name-line">
                <b>{{ participant.participantName }}</b>
                <em v-if="participant.settled"><Check :size="11" /> 완료</em>
              </span>
              <small>함께 결제한 영수증 {{ participant.receiptCount }}장</small>
            </span>
            <span class="amount-stack participant-amounts">
              <strong v-for="amount in participant.amounts" :key="amount.currencyCode">
                {{ amountLabel(amount) }}
              </strong>
            </span>
            <ChevronRight :size="18" />
          </button>
          <button
              type="button"
              class="settlement-toggle"
              :class="{ active: participant.settled }"
              :disabled="toggleLoadingName === participant.participantName"
              @click="toggleSettlement(participant)"
          >
            <span><i /></span>{{ participant.settled ? '정산 완료' : '미정산' }}
          </button>
        </article>

        <div v-if="!summary.participants.length" class="state-box compact">
          <span><Users :size="24" /></span><b>정산할 공동결제가 없어요</b>
          <small>공동결제 영수증을 등록하면 참여자별로 모아드려요.</small>
        </div>
      </div>
    </section>

    <section v-if="selectedParticipant" class="settlement-content">
      <div class="selected-participant-header">
        <span class="participant-avatar">{{ selectedParticipant.participantName.slice(0, 1).toUpperCase() }}</span>
        <div><small>SELECTED PARTICIPANT</small><h2>{{ selectedParticipant.participantName }}님의 영수증</h2></div>
        <span :class="['status-badge', { complete: selectedParticipant.settled }]">
          {{ selectedParticipant.settled ? '정산 완료' : '정산 필요' }}
        </span>
      </div>
      <div class="selected-total">
        <span>받을 금액</span>
        <div class="amount-stack">
          <strong v-for="amount in selectedParticipant.amounts" :key="amount.currencyCode">
            {{ amountLabel(amount) }}
          </strong>
        </div>
      </div>

      <div v-if="detailLoading" class="state-box">
        <span class="loading-mark">···</span><b>영수증을 불러오고 있어요</b>
      </div>
      <div v-else-if="detailErrorMessage" class="state-box error">
        <span>!</span><b>{{ detailErrorMessage }}</b>
        <button type="button" @click="loadParticipant(selectedParticipantName)">다시 시도</button>
      </div>
      <template v-else>
        <button
            v-for="receipt in detail.receipts"
            :key="receipt.id"
            type="button"
            class="receipt-row"
            @click="openReceipt(receipt.id)"
        >
          <span><ReceiptText :size="19" /></span>
          <div><b>{{ receiptMerchant(receipt) }}</b><small>{{ receipt.countryName || '국가 미지정' }} · {{ receiptDate(receipt) }}</small></div>
          <strong>{{ receiptAmount(receipt) }}</strong><ChevronRight :size="18" />
        </button>
        <div v-if="!detail.receipts.length" class="state-box">
          <span><Check :size="24" /></span><b>연결된 영수증이 없어요</b>
        </div>
      </template>
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.settlement-page{min-height:100vh;padding:0 18px 105px;background:radial-gradient(circle at 100% 0,rgba(47,111,237,.08),transparent 260px),#f4f7fc;color:#10192b}.page-header{display:grid;height:82px;grid-template-columns:42px 1fr 42px;align-items:center;padding-top:10px}.page-header>button{display:grid;width:40px;height:40px;place-items:center;border-radius:14px;background:#fff;color:#173f8d;box-shadow:0 7px 20px rgba(26,63,132,.09)}.page-header>div{text-align:center}.page-header small,.content-title small,.selected-participant-header small{color:#2f6fed;font-size:8px;font-weight:900;letter-spacing:.16em}.page-header h1{margin-top:2px;font-size:20px;font-weight:950;letter-spacing:-.04em}.vault-tabs{display:grid;grid-template-columns:1fr 1fr;gap:5px;padding:5px;border:1px solid #dfe7f3;border-radius:16px;background:#eaf0f9}.vault-tabs button{display:flex;height:38px;align-items:center;justify-content:center;gap:6px;border-radius:12px;color:#78879d;font-size:10px;font-weight:900}.vault-tabs button.active{background:#fff;color:#17499c;box-shadow:0 4px 12px rgba(28,67,137,.1)}.settlement-hero{position:relative;overflow:hidden;margin-top:14px;padding:21px;border-radius:23px;background:linear-gradient(145deg,#0c2d72,#174ca7 62%,#2f70d9);color:#fff;box-shadow:0 17px 35px rgba(23,73,156,.22)}.settlement-hero:after{position:absolute;right:-42px;bottom:-68px;width:170px;height:170px;border-radius:50%;background:#ffffff10;content:''}.hero-label{display:flex;align-items:center;gap:6px;color:#ffd466;font-size:8px;font-weight:900;letter-spacing:.12em}.settlement-hero>p{margin-top:13px;font-size:15px;font-weight:900;line-height:1.45}.hero-total{display:flex;align-items:end;justify-content:space-between;gap:16px;margin-top:22px;padding-top:15px;border-top:1px dashed #ffffff66}.hero-total>span{color:#c7d9f7;font-size:9px}.settlement-hero>small{display:block;margin-top:6px;color:#abc8f5;font-size:8px}.amount-stack{display:grid;gap:3px;text-align:right}.amount-stack strong{color:#173f8d;font-size:11px;font-weight:950;white-space:nowrap}.hero-amounts strong{color:#fff;font-size:19px}.participant-section,.settlement-content{margin-top:14px;padding:18px 16px 10px;border:1px solid #e2e9f4;border-radius:23px;background:#fff;box-shadow:0 10px 28px rgba(30,64,125,.07)}.content-title{display:flex;align-items:center;justify-content:space-between}.content-title h2{margin-top:3px;font-size:15px;font-weight:950}.content-title>span{padding:6px 9px;border-radius:999px;background:#eef4ff;color:#2f6fed;font-size:8px;font-weight:900}.participant-list{margin-top:8px}.participant-row{position:relative;padding:7px 0 11px;border-bottom:1px solid #edf1f6}.participant-row.selected{margin:5px -6px;padding:7px 6px 11px;border-radius:15px;background:#f5f8ff}.participant-select{display:grid;width:100%;grid-template-columns:42px minmax(0,1fr) auto 16px;align-items:center;gap:10px;padding:7px 1px;text-align:left}.row-avatar,.participant-avatar{display:grid;place-items:center;border-radius:14px;background:linear-gradient(135deg,#dce9ff,#edf4ff);color:#245fb9;font-weight:950}.row-avatar{width:40px;height:40px;font-size:13px}.participant-copy{min-width:0}.participant-copy b,.participant-copy small{display:block}.participant-copy b{overflow:hidden;font-size:11px;font-weight:900;text-overflow:ellipsis;white-space:nowrap}.participant-copy small{margin-top:4px;color:#8a98ac;font-size:8px}.participant-name-line{display:flex;align-items:center;gap:5px}.participant-name-line em{display:flex;align-items:center;gap:2px;padding:3px 5px;border-radius:999px;background:#e7f8f0;color:#16825c;font-size:7px;font-style:normal;font-weight:900}.participant-select>svg{color:#a7b4c7}.settlement-toggle{display:flex;align-items:center;gap:5px;margin:2px 22px 0 auto;color:#8795a9;font-size:8px;font-weight:900}.settlement-toggle>span{position:relative;width:26px;height:15px;border-radius:999px;background:#dce3ed}.settlement-toggle i{position:absolute;top:2px;left:2px;width:11px;height:11px;border-radius:50%;background:#fff;transition:.2s}.settlement-toggle.active{color:#16825c}.settlement-toggle.active>span{background:#27aa79}.settlement-toggle.active i{transform:translateX(11px)}.selected-participant-header{display:grid;grid-template-columns:48px minmax(0,1fr) auto;align-items:center;gap:10px;padding-bottom:14px;border-bottom:1px dashed #ccd7e6}.participant-avatar{width:46px;height:46px;font-size:16px}.selected-participant-header h2{margin-top:3px;font-size:14px;font-weight:950}.status-badge{padding:6px 8px;border-radius:999px;background:#fff0ee;color:#e25d52;font-size:8px;font-weight:900}.status-badge.complete{background:#e8f8f1;color:#16825c}.selected-total{display:flex;align-items:center;justify-content:space-between;margin:12px 0 2px;padding:11px 12px;border-radius:13px;background:#f2f6fc}.selected-total>span{color:#718099;font-size:8px;font-weight:900}.receipt-row{display:grid;width:100%;grid-template-columns:42px minmax(0,1fr) auto 16px;align-items:center;gap:10px;padding:14px 1px;border-bottom:1px solid #edf1f6;text-align:left}.receipt-row div{min-width:0}.receipt-row b,.receipt-row small{display:block}.receipt-row b{overflow:hidden;font-size:11px;font-weight:900;text-overflow:ellipsis;white-space:nowrap}.receipt-row small{margin-top:4px;color:#8a98ac;font-size:8px}.receipt-row>strong{color:#173f8d;font-size:10px;font-weight:950;white-space:nowrap}.receipt-row>svg{color:#a7b4c7}.receipt-row>span{display:grid;width:40px;height:40px;place-items:center;border-radius:14px;background:#eef3fb;color:#5f7598}.state-box{display:flex;min-height:150px;flex-direction:column;align-items:center;justify-content:center;text-align:center}.state-box.compact{min-height:120px}.state-box>span{display:grid;width:50px;height:50px;place-items:center;border-radius:17px;background:#eef4ff;color:#2f6fed;font-size:20px;font-weight:900}.state-box b{margin-top:12px;font-size:12px}.state-box small{margin-top:6px;color:#8997aa;font-size:8px}.state-box button{margin-top:11px;padding:8px 13px;border-radius:9px;background:#eaf2ff;color:#2464c4;font-size:9px;font-weight:900}.state-box.error>span{background:#fff0f0;color:#e15a5a}.loading-mark{letter-spacing:2px}@media(max-width:350px){.participant-select,.receipt-row{grid-template-columns:38px minmax(0,1fr) auto 14px;gap:7px}.selected-participant-header{grid-template-columns:43px minmax(0,1fr)}.status-badge{grid-column:1/-1;justify-self:end}}
.vault-tabs { margin-top: 19px; }
</style>
