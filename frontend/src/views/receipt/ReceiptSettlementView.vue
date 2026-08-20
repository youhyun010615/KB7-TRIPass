<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Check,
  ChevronLeft,
  ChevronRight,
  HandCoins,
  ReceiptText,
  Users,
} from '@lucide/vue'
import BottomNav from '@/components/common/BottomNav.vue'
import {
  getParticipantSettlement,
  getReceiptSettlements,
} from '@/api/receipt'

const route = useRoute()
const router = useRouter()

const tripId = computed(() => {
  const value = Number(route.params.tripId)
  return Number.isInteger(value) && value > 0 ? value : null
})

const participantName = computed(() => String(route.params.participantName || ''))
const isDetail = computed(() => Boolean(participantName.value))
const loading = ref(false)
const errorMessage = ref('')
const summary = ref({ totalOwedAmount: 0, participants: [] })
const detail = ref({ participantName: '', totalOwedAmount: 0, receipts: [] })

const participantInitial = computed(() =>
  (detail.value.participantName || participantName.value || '?').trim().slice(0, 1).toUpperCase(),
)

function dataOf(response) {
  return response.data?.data ?? response.data ?? {}
}

function formatAmount(value) {
  return Number(value || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 })
}

function receiptMerchant(item) {
  return item.merchantTranslatedName || item.merchantOriginalName || '상호명 미인식'
}

function receiptDate(item) {
  const value = String(item.paymentDateTime || '')
  return value ? value.replace('T', ' ').slice(0, 16) : '결제일 미확인'
}

function receiptAmount(item) {
  const label = item.currencySymbol || item.currencyCode || ''
  return `${label} ${formatAmount(item.totalAmount)}`.trim()
}

async function loadSettlement() {
  if (!tripId.value) {
    errorMessage.value = '여행 정보를 확인해 주세요.'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    if (isDetail.value) {
      const data = dataOf(await getParticipantSettlement(tripId.value, participantName.value))
      detail.value = {
        participantName: data.participantName || participantName.value,
        totalOwedAmount: Number(data.totalOwedAmount) || 0,
        receipts: Array.isArray(data.receipts) ? data.receipts : [],
      }
    } else {
      const data = dataOf(await getReceiptSettlements(tripId.value))
      summary.value = {
        totalOwedAmount: Number(data.totalOwedAmount) || 0,
        participants: Array.isArray(data.participants) ? data.participants : [],
      }
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '정산 내역을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function goBack() {
  if (isDetail.value) {
    router.push({ name: 'ReceiptSettlements', params: { tripId: tripId.value } })
    return
  }
  router.back()
}

function openReceipts() {
  router.push({ name: 'Receipt', params: { tripId: tripId.value } })
}

function openParticipant(name) {
  router.push({
    name: 'ReceiptParticipantSettlement',
    params: { tripId: tripId.value, participantName: name },
  })
}

function openReceipt(receiptId) {
  router.push({ name: 'ReceiptDetail', params: { tripId: tripId.value, receiptId } })
}

watch(participantName, loadSettlement)
onMounted(loadSettlement)
</script>

<template>
  <main class="settlement-page">
    <header class="page-header">
      <button type="button" aria-label="뒤로 가기" @click="goBack">
        <ChevronLeft :size="24" :stroke-width="2.4" />
      </button>
      <div>
        <small>TRIP SETTLEMENT</small>
        <h1>{{ isDetail ? '참여자 정산' : '여행 정산' }}</h1>
      </div>
      <span />
    </header>

    <nav class="vault-tabs" aria-label="영수증 보관함 메뉴">
      <button type="button" @click="openReceipts">
        <ReceiptText :size="16" /> 영수증
      </button>
      <button type="button" class="active">
        <HandCoins :size="17" /> 정산
      </button>
    </nav>

    <section v-if="!isDetail" class="settlement-hero">
      <div class="hero-label"><HandCoins :size="17" /> SETTLEMENT SUMMARY</div>
      <p>함께 결제한 여행 경비를<br>참여자별로 확인해 보세요.</p>
      <div class="hero-total">
        <span>받을 총 정산 금액</span>
        <strong>{{ formatAmount(summary.totalOwedAmount) }}</strong>
      </div>
      <small>{{ summary.participants.length }}명의 정산이 기다리고 있어요</small>
    </section>

    <section v-else class="participant-hero">
      <span class="participant-avatar">{{ participantInitial }}</span>
      <div>
        <small>SETTLEMENT WITH</small>
        <h2>{{ detail.participantName || participantName }}</h2>
      </div>
      <span class="detail-total"><small>총 정산 금액</small><strong>{{ formatAmount(detail.totalOwedAmount) }}</strong></span>
    </section>

    <section class="settlement-content">
      <div class="content-title">
        <div>
          <small>{{ isDetail ? 'SHARED RECEIPTS' : 'PARTICIPANTS' }}</small>
          <h2>{{ isDetail ? '함께 결제한 영수증' : '참여자별 정산' }}</h2>
        </div>
        <span>{{ isDetail ? detail.receipts.length : summary.participants.length }}건</span>
      </div>

      <div v-if="loading" class="state-box">
        <span class="loading-mark">···</span>
        <b>정산 내역을 불러오고 있어요</b>
      </div>

      <div v-else-if="errorMessage" class="state-box error">
        <span>!</span>
        <b>{{ errorMessage }}</b>
        <button type="button" @click="loadSettlement">다시 시도</button>
      </div>

      <template v-else-if="!isDetail">
        <button
            v-for="participant in summary.participants"
            :key="participant.participantName"
            type="button"
            class="participant-row"
            @click="openParticipant(participant.participantName)"
        >
          <span class="row-avatar">{{ String(participant.participantName || '?').slice(0, 1).toUpperCase() }}</span>
          <div>
            <b>{{ participant.participantName }}</b>
            <small>함께 결제한 영수증 {{ participant.receiptCount }}장</small>
          </div>
          <strong>{{ formatAmount(participant.totalOwedAmount) }}</strong>
          <ChevronRight :size="18" />
        </button>

        <div v-if="!summary.participants.length" class="state-box">
          <span><Users :size="24" /></span>
          <b>정산할 공동결제가 없어요</b>
          <small>공동결제 영수증을 등록하면 참여자별로 모아드려요.</small>
        </div>
      </template>

      <template v-else>
        <button
            v-for="receipt in detail.receipts"
            :key="receipt.id"
            type="button"
            class="receipt-row"
            @click="openReceipt(receipt.id)"
        >
          <span><ReceiptText :size="19" /></span>
          <div>
            <b>{{ receiptMerchant(receipt) }}</b>
            <small>{{ receipt.countryName || '국가 미지정' }} · {{ receiptDate(receipt) }}</small>
          </div>
          <strong>{{ receiptAmount(receipt) }}</strong>
          <ChevronRight :size="18" />
        </button>

        <div v-if="!detail.receipts.length" class="state-box">
          <span><Check :size="24" /></span>
          <b>연결된 영수증이 없어요</b>
        </div>
      </template>
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.settlement-page{min-height:100vh;padding:0 18px 105px;background:radial-gradient(circle at 100% 0,rgba(47,111,237,.08),transparent 260px),#f4f7fc;color:#10192b}.page-header{display:grid;height:82px;grid-template-columns:42px 1fr 42px;align-items:center;padding-top:10px}.page-header>button{display:grid;width:40px;height:40px;place-items:center;border-radius:14px;background:#fff;color:#173f8d;box-shadow:0 7px 20px rgba(26,63,132,.09)}.page-header>div{text-align:center}.page-header small,.content-title small,.participant-hero>div small{color:#2f6fed;font-size:8px;font-weight:900;letter-spacing:.16em}.page-header h1{margin-top:2px;font-size:20px;font-weight:950;letter-spacing:-.04em}.vault-tabs{display:grid;grid-template-columns:1fr 1fr;gap:5px;padding:5px;border:1px solid #dfe7f3;border-radius:16px;background:#eaf0f9}.vault-tabs button{display:flex;height:38px;align-items:center;justify-content:center;gap:6px;border-radius:12px;color:#78879d;font-size:10px;font-weight:900}.vault-tabs button.active{background:#fff;color:#17499c;box-shadow:0 4px 12px rgba(28,67,137,.1)}.settlement-hero{position:relative;overflow:hidden;margin-top:14px;padding:21px;border-radius:23px;background:linear-gradient(145deg,#0c2d72,#174ca7 62%,#2f70d9);color:#fff;box-shadow:0 17px 35px rgba(23,73,156,.22)}.settlement-hero:after{position:absolute;right:-42px;bottom:-68px;width:170px;height:170px;border-radius:50%;background:#ffffff10;content:''}.hero-label{display:flex;align-items:center;gap:6px;color:#ffd466;font-size:8px;font-weight:900;letter-spacing:.12em}.settlement-hero>p{margin-top:13px;font-size:15px;font-weight:900;line-height:1.45}.hero-total{display:flex;align-items:end;justify-content:space-between;margin-top:22px;padding-top:15px;border-top:1px dashed #ffffff66}.hero-total span{color:#c7d9f7;font-size:9px}.hero-total strong{font-size:27px;font-weight:950}.settlement-hero>small{display:block;margin-top:6px;color:#abc8f5;font-size:8px}.participant-hero{display:grid;grid-template-columns:52px 1fr auto;align-items:center;gap:12px;margin-top:14px;padding:18px;border:1px solid #dce6f4;border-radius:21px;background:#fff;box-shadow:0 10px 28px rgba(30,64,125,.08)}.participant-avatar,.row-avatar{display:grid;place-items:center;border-radius:17px;background:linear-gradient(135deg,#dce9ff,#edf4ff);color:#245fb9;font-weight:950}.participant-avatar{width:50px;height:50px;font-size:18px}.participant-hero h2{margin-top:3px;font-size:17px;font-weight:950}.detail-total{display:grid;justify-items:end}.detail-total small{color:#8a98ad;font-size:7px}.detail-total strong{margin-top:3px;color:#17499c;font-size:17px}.settlement-content{margin-top:14px;padding:18px 16px 10px;border:1px solid #e2e9f4;border-radius:23px;background:#fff;box-shadow:0 10px 28px rgba(30,64,125,.07)}.content-title{display:flex;align-items:center;justify-content:space-between}.content-title h2{margin-top:3px;font-size:15px;font-weight:950}.content-title>span{padding:6px 9px;border-radius:999px;background:#eef4ff;color:#2f6fed;font-size:8px;font-weight:900}.participant-row,.receipt-row{display:grid;width:100%;grid-template-columns:42px minmax(0,1fr) auto 16px;align-items:center;gap:10px;padding:14px 1px;border-bottom:1px solid #edf1f6;text-align:left}.row-avatar{width:40px;height:40px;border-radius:14px;font-size:13px}.participant-row div,.receipt-row div{min-width:0}.participant-row b,.participant-row small,.receipt-row b,.receipt-row small{display:block}.participant-row b,.receipt-row b{overflow:hidden;font-size:11px;font-weight:900;text-overflow:ellipsis;white-space:nowrap}.participant-row small,.receipt-row small{margin-top:4px;color:#8a98ac;font-size:8px}.participant-row>strong,.receipt-row>strong{color:#173f8d;font-size:10px;font-weight:950;white-space:nowrap}.participant-row>svg,.receipt-row>svg{color:#a7b4c7}.receipt-row>span{display:grid;width:40px;height:40px;place-items:center;border-radius:14px;background:#eef3fb;color:#5f7598}.state-box{display:flex;min-height:190px;flex-direction:column;align-items:center;justify-content:center;text-align:center}.state-box>span{display:grid;width:50px;height:50px;place-items:center;border-radius:17px;background:#eef4ff;color:#2f6fed;font-size:20px;font-weight:900}.state-box b{margin-top:12px;font-size:12px}.state-box small{margin-top:6px;color:#8997aa;font-size:8px}.state-box button{margin-top:11px;padding:8px 13px;border-radius:9px;background:#eaf2ff;color:#2464c4;font-size:9px;font-weight:900}.state-box.error>span{background:#fff0f0;color:#e15a5a}.loading-mark{letter-spacing:2px}@media(max-width:350px){.participant-hero{grid-template-columns:46px 1fr}.participant-avatar{width:44px;height:44px}.detail-total{grid-column:1/-1;grid-template-columns:1fr auto;justify-items:initial}.participant-row,.receipt-row{grid-template-columns:38px minmax(0,1fr) auto 14px;gap:7px}}
</style>
