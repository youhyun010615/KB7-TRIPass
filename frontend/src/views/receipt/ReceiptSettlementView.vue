<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Check, ChevronRight, HandCoins, ReceiptText, Users } from '@lucide/vue'
import { getParticipantSettlement, getReceiptSettlements, toggleReceiptSettlement } from '@/api/receipt'

const props = defineProps({ tripId: { type: Number, required: true } })
const router = useRouter()
const summaryLoading = ref(false)
const detailLoading = ref(false)
const toggleLoadingName = ref('')
const errorMessage = ref('')
const detailErrorMessage = ref('')
const selectedParticipantName = ref('')
const summary = ref({ totalAmounts: [], participantCount: 0, participants: [] })
const detail = ref({ participantName: '', receipts: [] })

const dataOf = response => response.data?.data ?? response.data ?? {}
const formatAmount = value => Number(value || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 })
const normalizeAmounts = amounts => Array.isArray(amounts)
  ? amounts.map(amount => ({
      currencyCode: amount.currencyCode || '',
      currencySymbol: amount.currencySymbol || '',
      amount: Number(amount.amount) || 0,
    }))
  : []
const amountLabel = amount => `${amount.currencySymbol || amount.currencyCode || '통화'} ${formatAmount(amount.amount)}`
const receiptMerchant = receipt => receipt.merchantTranslatedName || receipt.merchantOriginalName || '상호명 미인식'
const receiptDate = receipt => {
  const value = String(receipt.paymentDateTime || '')
  return value ? value.replace('T', ' ').slice(0, 16) : '결제일 미확인'
}
const receiptAmount = receipt => `${receipt.currencySymbol || receipt.currencyCode || ''} ${formatAmount(receipt.totalAmount)}`.trim()

async function loadParticipant(name) {
  if (!props.tripId || !name) return
  if (selectedParticipantName.value === name) {
    selectedParticipantName.value = ''
    detail.value = { participantName: '', receipts: [] }
    detailErrorMessage.value = ''
    return
  }

  selectedParticipantName.value = name
  detailLoading.value = true
  detailErrorMessage.value = ''
  detail.value = { participantName: name, receipts: [] }
  try {
    const data = dataOf(await getParticipantSettlement(props.tripId, name))
    if (selectedParticipantName.value !== name) return
    detail.value = {
      participantName: data.participantName || name,
      receipts: Array.isArray(data.receipts) ? data.receipts : [],
    }
  } catch (error) {
    if (selectedParticipantName.value === name) {
      detailErrorMessage.value = error.response?.data?.message || '참여자의 영수증을 불러오지 못했습니다.'
    }
  } finally {
    if (selectedParticipantName.value === name) detailLoading.value = false
  }
}

async function retryParticipant(name) {
  selectedParticipantName.value = ''
  await loadParticipant(name)
}

async function loadSettlement() {
  if (!props.tripId) return
  summaryLoading.value = true
  errorMessage.value = ''
  selectedParticipantName.value = ''
  detail.value = { participantName: '', receipts: [] }
  try {
    const data = dataOf(await getReceiptSettlements(props.tripId))
    summary.value = {
      totalAmounts: normalizeAmounts(data.totalAmounts),
      participantCount: Number(data.participantCount) || 0,
      participants: Array.isArray(data.participants)
        ? data.participants.map(participant => ({
            ...participant,
            receiptCount: Number(participant.receiptCount) || 0,
            settled: Boolean(participant.settled),
            amounts: normalizeAmounts(participant.amounts),
          }))
        : [],
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '정산 내역을 불러오지 못했습니다.'
  } finally {
    summaryLoading.value = false
  }
}

async function toggleSettlement(participant) {
  if (!props.tripId || toggleLoadingName.value) return
  const nextSettled = !participant.settled
  toggleLoadingName.value = participant.participantName
  try {
    await toggleReceiptSettlement(props.tripId, participant.participantName, nextSettled)
    participant.settled = nextSettled
  } catch (error) {
    window.alert(error.response?.data?.message || '정산 상태를 변경하지 못했습니다.')
  } finally {
    toggleLoadingName.value = ''
  }
}

function openReceipt(receiptId) {
  router.push({ name: 'ReceiptDetail', params: { tripId: props.tripId, receiptId } })
}

watch(() => props.tripId, loadSettlement)
onMounted(loadSettlement)
</script>

<template>
  <div class="settlement-panel">
    <section class="settlement-hero">
      <div class="hero-label"><HandCoins :size="17" /> SETTLEMENT SUMMARY</div>
      <p>공동경비를 확인해보세요.</p>
      <div class="hero-total">
        <span>예정 정산 금액</span>
        <div class="amount-stack hero-amounts">
          <strong v-for="amount in summary.totalAmounts" :key="amount.currencyCode">{{ amountLabel(amount) }}</strong>
          <strong v-if="!summary.totalAmounts.length">0</strong>
        </div>
      </div>
    </section>

    <section class="participant-section">
      <div class="content-title">
        <div><small>SETTLEMENT LIST</small><h2>정산 필요 내역</h2></div>
        <span>{{ summary.participants.length }}명</span>
      </div>
      <p class="section-guide">참여자를 누르면 해당 영수증이 바로 펼쳐져요.</p>

      <div v-if="summaryLoading" class="state-box compact"><span class="loading-mark">···</span><b>정산 내역을 불러오고 있어요</b></div>
      <div v-else-if="errorMessage" class="state-box compact error">
        <span>!</span><b>{{ errorMessage }}</b><button type="button" @click="loadSettlement">다시 시도</button>
      </div>

      <div v-else class="participant-list">
        <article
            v-for="participant in summary.participants"
            :key="participant.participantName"
            class="participant-row"
            :class="{ expanded: selectedParticipantName === participant.participantName }"
        >
          <div class="participant-summary">
            <button
                type="button"
                class="participant-select"
                :aria-expanded="selectedParticipantName === participant.participantName"
                @click="loadParticipant(participant.participantName)"
            >
              <span class="row-avatar">{{ String(participant.participantName || '?').slice(0, 1).toUpperCase() }}</span>
              <span class="participant-copy">
                <span class="participant-name-line">
                  <b>{{ participant.participantName }}</b>
                  <em v-if="participant.settled"><Check :size="11" /> 완료</em>
                </span>
                <small>함께 결제한 영수증 {{ participant.receiptCount }}장</small>
              </span>
              <span class="amount-stack participant-amounts">
                <strong v-for="amount in participant.amounts" :key="amount.currencyCode">{{ amountLabel(amount) }}</strong>
              </span>
              <ChevronRight class="participant-chevron" :class="{ expanded: selectedParticipantName === participant.participantName }" :size="18" />
            </button>
            <button
                type="button"
                class="settlement-toggle"
                :class="{ active: participant.settled }"
                :disabled="toggleLoadingName === participant.participantName"
                @click="toggleSettlement(participant)"
            ><span><i /></span>{{ participant.settled ? '정산 완료' : '미정산' }}</button>
          </div>

          <div v-if="selectedParticipantName === participant.participantName" class="participant-details">
            <div class="details-heading">
              <div><small>RECEIPTS</small><h3>{{ participant.participantName }}님의 영수증</h3></div>
              <span :class="{ complete: participant.settled }">{{ participant.settled ? '정산 완료' : '정산 필요' }}</span>
            </div>
            <div class="details-total">
              <span>받을 금액</span>
              <div class="amount-stack">
                <strong v-for="amount in participant.amounts" :key="amount.currencyCode">{{ amountLabel(amount) }}</strong>
              </div>
            </div>

            <div v-if="detailLoading" class="state-box details-state"><span class="loading-mark">···</span><b>영수증을 불러오고 있어요</b></div>
            <div v-else-if="detailErrorMessage" class="state-box details-state error">
              <span>!</span><b>{{ detailErrorMessage }}</b><button type="button" @click="retryParticipant(participant.participantName)">다시 시도</button>
            </div>
            <template v-else>
              <button v-for="receipt in detail.receipts" :key="receipt.id" type="button" class="receipt-row" @click="openReceipt(receipt.id)">
                <span><ReceiptText :size="18" /></span>
                <div><b>{{ receiptMerchant(receipt) }}</b><small>{{ receipt.countryName || '국가 미지정' }} · {{ receiptDate(receipt) }}</small></div>
                <strong>{{ receiptAmount(receipt) }}</strong><ChevronRight :size="17" />
              </button>
              <div v-if="!detail.receipts.length" class="state-box details-state"><span><Check :size="22" /></span><b>연결된 영수증이 없어요</b></div>
            </template>
          </div>
        </article>

        <div v-if="!summary.participants.length" class="state-box compact empty-settlement">
          <span class="empty-settlement-icon"><Users :size="24" /></span><b>정산할 공동결제가 없어요</b><small>공동결제 영수증을 등록하면 참여자별로 모아드려요.</small>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.settlement-panel{color:#10192b}.settlement-hero{position:relative;overflow:hidden;margin-top:14px;padding:21px;border-radius:23px;background:linear-gradient(145deg,#0c2d72,#174ca7 62%,#2f70d9);color:#fff;box-shadow:0 17px 35px rgba(23,73,156,.22)}.settlement-hero:after{position:absolute;right:-42px;bottom:-68px;width:170px;height:170px;border-radius:50%;background:#ffffff10;content:''}.hero-label{display:flex;align-items:center;gap:6px;color:#ffd466;font-size:8px;font-weight:900;letter-spacing:.12em}.settlement-hero>p{margin-top:13px;font-size:15px;font-weight:900;line-height:1.45}.hero-total{display:flex;align-items:end;justify-content:space-between;gap:16px;margin-top:22px;padding-top:15px;border-top:1px dashed #ffffff66}.hero-total>span{color:#c7d9f7;font-size:9px}.settlement-hero>small{display:block;margin-top:6px;color:#abc8f5;font-size:8px}.amount-stack{display:grid;gap:3px;text-align:right}.amount-stack strong{color:#173f8d;font-size:11px;font-weight:950;white-space:nowrap}.hero-amounts strong{color:#fff;font-size:19px}.participant-section{margin-top:14px;padding:18px 16px 10px;border:1px solid #e2e9f4;border-radius:23px;background:#fff;box-shadow:0 10px 28px rgba(30,64,125,.07)}.content-title{display:flex;align-items:center;justify-content:space-between}.content-title small,.details-heading small{color:#2f6fed;font-size:8px;font-weight:900;letter-spacing:.16em}.content-title h2{margin-top:3px;font-size:15px;font-weight:950}.content-title>span{padding:6px 9px;border-radius:999px;background:#eef4ff;color:#2f6fed;font-size:8px;font-weight:900}.section-guide{margin-top:5px;color:#8a98ac;font-size:8px}.participant-list{margin-top:10px}.participant-row{overflow:hidden;border-bottom:1px solid #edf1f6;transition:.2s}.participant-row.expanded{margin:7px -7px 12px;border:1px solid #cfe0ff;border-radius:18px;background:#f6f9ff;box-shadow:0 8px 20px rgba(31,76,151,.08)}.participant-summary{padding:7px 0 11px}.participant-row.expanded .participant-summary{padding:8px 7px 11px}.participant-select{display:grid;width:100%;grid-template-columns:42px minmax(0,1fr) auto 16px;align-items:center;gap:10px;padding:7px 1px;text-align:left}.row-avatar{display:grid;width:40px;height:40px;place-items:center;border-radius:14px;background:linear-gradient(135deg,#dce9ff,#edf4ff);color:#245fb9;font-size:13px;font-weight:950}.participant-copy{min-width:0}.participant-copy b,.participant-copy small{display:block}.participant-copy b{overflow:hidden;font-size:11px;font-weight:900;text-overflow:ellipsis;white-space:nowrap}.participant-copy small{margin-top:4px;color:#8a98ac;font-size:8px}.participant-name-line{display:flex;align-items:center;gap:5px}.participant-name-line em{display:flex;align-items:center;gap:2px;padding:3px 5px;border-radius:999px;background:#e7f8f0;color:#16825c;font-size:7px;font-style:normal;font-weight:900}.participant-chevron{color:#a7b4c7;transition:transform .2s}.participant-chevron.expanded{transform:rotate(90deg)}.settlement-toggle{display:flex;align-items:center;gap:5px;margin:2px 22px 0 auto;color:#8795a9;font-size:8px;font-weight:900}.settlement-toggle>span{position:relative;width:26px;height:15px;border-radius:999px;background:#dce3ed}.settlement-toggle i{position:absolute;top:2px;left:2px;width:11px;height:11px;border-radius:50%;background:#fff;transition:.2s}.settlement-toggle.active{color:#16825c}.settlement-toggle.active>span{background:#27aa79}.settlement-toggle.active i{transform:translateX(11px)}.participant-details{padding:14px;border-top:1px dashed #c8d7ed;background:#fff}.details-heading{display:flex;align-items:center;justify-content:space-between}.details-heading h3{margin-top:3px;font-size:13px;font-weight:950}.details-heading>span{padding:6px 8px;border-radius:999px;background:#fff0ee;color:#e25d52;font-size:8px;font-weight:900}.details-heading>span.complete{background:#e8f8f1;color:#16825c}.details-total{display:flex;align-items:center;justify-content:space-between;margin:12px 0 2px;padding:11px 12px;border-radius:13px;background:#f2f6fc}.details-total>span{color:#718099;font-size:8px;font-weight:900}.receipt-row{display:grid;width:100%;grid-template-columns:38px minmax(0,1fr) auto 15px;align-items:center;gap:9px;padding:13px 1px;border-bottom:1px solid #edf1f6;text-align:left}.receipt-row div{min-width:0}.receipt-row b,.receipt-row small{display:block}.receipt-row b{overflow:hidden;font-size:10px;font-weight:900;text-overflow:ellipsis;white-space:nowrap}.receipt-row small{margin-top:4px;color:#8a98ac;font-size:7px}.receipt-row>strong{color:#173f8d;font-size:9px;font-weight:950;white-space:nowrap}.receipt-row>svg{color:#a7b4c7}.receipt-row>span{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#eef3fb;color:#5f7598}.state-box{display:flex;min-height:150px;flex-direction:column;align-items:center;justify-content:center;text-align:center}.state-box.compact,.state-box.details-state{min-height:120px}.state-box>span{display:grid;width:50px;height:50px;place-items:center;border-radius:17px;background:#eef4ff;color:#2f6fed;font-size:20px;font-weight:900}.state-box b{margin-top:12px;font-size:12px}.state-box small{margin-top:6px;color:#8997aa;font-size:8px}.state-box button{margin-top:11px;padding:8px 13px;border-radius:9px;background:#eaf2ff;color:#2464c4;font-size:9px;font-weight:900}.state-box.error>span{background:#fff0f0;color:#e15a5a}.loading-mark{letter-spacing:2px}@media(max-width:350px){.participant-select{grid-template-columns:38px minmax(0,1fr) auto 14px;gap:7px}.participant-amounts strong{font-size:9px}.receipt-row{grid-template-columns:34px minmax(0,1fr) auto 14px;gap:6px}}

.hero-total > span {
  font-size: 15px;
  font-weight: 900;
}
.empty-settlement-icon {
  position: relative;
  animation: empty-settlement-float 2.1s ease-in-out infinite;
}
.empty-settlement-icon::after {
  position: absolute;
  inset: -1px;
  border: 1px solid rgba(47, 111, 237, 0.34);
  border-radius: 18px;
  content: '';
  animation: empty-settlement-ring 2.1s ease-out infinite;
}
@keyframes empty-settlement-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
@keyframes empty-settlement-ring {
  0% { opacity: .65; transform: scale(.85); }
  70%, 100% { opacity: 0; transform: scale(1.35); }
}
@media (prefers-reduced-motion: reduce) {
  .empty-settlement-icon,
  .empty-settlement-icon::after { animation: none; }
}
</style>
