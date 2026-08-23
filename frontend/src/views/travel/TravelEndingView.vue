<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTravelStore } from '@/stores/travel'
import { useTravelModeStore } from '@/stores/travelMode'
import { useTripWalletStore } from '@/stores/tripWallet'

const router = useRouter()
const travel = useTravelStore()
const travelMode = useTravelModeStore()
const wallet = useTripWalletStore()
const busy = ref(false)
const walletLoading = ref(true)
const fundChoice = ref('transfer')
const actionError = ref('')
const trip = computed(() => travel.lifecycle || {})
const remainingFunds = computed(() => Number(wallet.balance || 0))
const primaryAccount = computed(() => (
  wallet.linkedAccounts.find(account => account.isPrimary)
  || wallet.linkedAccounts[0]
  || null
))

function formatWon(value) {
  return `${Math.round(Number(value || 0)).toLocaleString('ko-KR')}원`
}

async function archiveAndNavigate(destination) {
  await travel.archiveCurrentTrip()
  travelMode.setMode('savings')
  await router.replace(destination === 'register' ? '/travel/register' : '/')
}

async function finish(destination) {
  if (busy.value) return
  busy.value = true
  actionError.value = ''
  try {
    await archiveAndNavigate(destination)
  } catch (error) {
    actionError.value = error.response?.data?.message || '여행을 마무리하지 못했어요. 다시 시도해 주세요.'
  } finally {
    busy.value = false
  }
}

async function settleRemainingFunds() {
  if (busy.value) return
  busy.value = true
  actionError.value = ''
  try {
    if (fundChoice.value === 'transfer' && remainingFunds.value > 0) {
      if (!primaryAccount.value) {
        actionError.value = '송금할 주계좌를 먼저 연결해 주세요.'
        return
      }
      const transferred = await wallet.withdraw({
        mode: 'registered',
        amount: remainingFunds.value,
        targetAccountId: primaryAccount.value.accountId,
      })
      if (!transferred) {
        actionError.value = '남은 여행 자금을 송금하지 못했어요.'
        return
      }
    }
    await archiveAndNavigate('home')
  } catch (error) {
    actionError.value = error.response?.data?.message || '남은 여행 자금을 처리하지 못했어요. 다시 시도해 주세요.'
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  try {
    await Promise.all([wallet.loadWalletMain(), wallet.loadAccounts()])
  } catch (error) {
    actionError.value = error.response?.data?.message || '남은 여행 자금을 불러오지 못했어요.'
  } finally {
    walletLoading.value = false
  }
})
</script>

<template>
  <main class="ending-page">
    <section class="ending-hero">
      <span class="eyebrow">TRIPASS · JOURNEY COMPLETE</span>
      <div class="plane" aria-hidden="true">✈</div>
      <h1>여행은 잘 보내셨나요?</h1>
      <p>{{ trip.tripName || '소중한 여행' }}의 기록이 모두 준비됐어요.<br>저축부터 여행의 순간까지 천천히 돌아보세요.</p>
    </section>

    <section class="report-stack">
      <button type="button" @click="router.push(`/mypage/reports/pre-trip?tripId=${trip.tripId}`)">
        <span class="report-icon">₩</span>
        <span><small>SAVING STORY</small><strong>여행 저축 리포트</strong><em>여행을 위해 모은 과정과 달성 기록</em></span>
        <b>›</b>
      </button>
      <button type="button" @click="router.push(`/mypage/reports/post-trip?tripId=${trip.tripId}`)">
        <span class="report-icon yellow">✦</span>
        <span><small>TRAVEL STORY</small><strong>여행 리포트</strong><em>예산과 실제 지출, 여행 결과 분석</em></span>
        <b>›</b>
      </button>
      <button type="button" @click="router.push(`/mypage/travel/${trip.tripId}`)">
        <span class="report-icon pale">▤</span>
        <span><small>MY TRIP ARCHIVE</small><strong>여행 기록 전체 보기</strong><em>일정·체크리스트·영수증·완료 미션</em></span>
        <b>›</b>
      </button>
    </section>

    <section class="fund-settlement">
      <div class="fund-section-heading">
        <small>TRIP FUND</small>
        <h2>남은 여행 자금 정리</h2>
        <p>여행 후 남은 자금을 어떻게 관리할까요?</p>
      </div>

      <div class="remaining-balance">
        <span>남은 여행 자금</span>
        <strong v-if="!walletLoading">{{ formatWon(remainingFunds) }}</strong>
        <strong v-else class="balance-loading">불러오는 중</strong>
      </div>

      <div class="fund-options">
        <button type="button" :class="{ selected: fundChoice === 'transfer' }" @click="fundChoice = 'transfer'">
          <i aria-hidden="true" />
          <span>
            <b>내 계좌로 보내기</b>
            <small>연결된 주계좌로 남은 원화를 안전하게 보내요.</small>
          </span>
        </button>

        <div v-if="fundChoice === 'transfer' && primaryAccount" class="receiving-account">
          <span>₩</span>
          <div>
            <b>{{ primaryAccount.bankName }} {{ primaryAccount.name }}</b>
            <small>{{ primaryAccount.number }}</small>
          </div>
          <em>주계좌</em>
        </div>

        <button type="button" :class="{ selected: fundChoice === 'keep' }" @click="fundChoice = 'keep'">
          <i aria-hidden="true" />
          <span>
            <b>다음 여행을 위해 보관</b>
            <small>트립월렛에 보관하고 다음 여행 자금으로 이어가요.</small>
          </span>
        </button>
      </div>

      <p v-if="actionError" class="fund-error">{{ actionError }}</p>
      <button type="button" class="settlement-primary" :disabled="busy || walletLoading" @click="settleRemainingFunds">
        <template v-if="fundChoice === 'transfer' && remainingFunds > 0">{{ formatWon(remainingFunds) }} 보내고 여행 마무리</template>
        <template v-else>남은 자금 보관하고 여행 마무리</template>
      </button>
      <button type="button" class="settlement-later" :disabled="busy" @click="finish('home')">나중에 정리할게요</button>
    </section>

    <section class="ending-actions">
      <button type="button" class="primary" :disabled="busy" @click="finish('register')">새 여행 목표 설정하기</button>
      <button type="button" class="secondary" :disabled="busy" @click="finish('home')">홈으로 가기</button>
    </section>
  </main>
</template>

<style scoped>
.ending-page{min-height:100vh;padding:58px 20px 36px;background:linear-gradient(180deg,#0b2a6b 0,#174ca7 43%,#eef3fb 43%);color:#10192b}.ending-hero{text-align:center;color:#fff}.eyebrow{font-size:10px;font-weight:900;letter-spacing:.18em;color:#ffd466}.plane{width:74px;height:74px;margin:20px auto 14px;display:grid;place-items:center;border-radius:50%;font-size:32px;background:#ffffff18;border:1px solid #ffffff35;box-shadow:0 14px 36px #061c4c66}.ending-hero h1{font-size:27px;font-weight:950}.ending-hero p{margin-top:10px;font-size:13px;line-height:1.75;color:#dce8ff}.report-stack{margin-top:34px;display:grid;gap:12px}.report-stack button{width:100%;display:grid;grid-template-columns:50px 1fr 18px;align-items:center;gap:13px;padding:17px;border:0;border-radius:20px;background:#fff;text-align:left;box-shadow:0 9px 25px #18386b18}.report-stack button>span:nth-child(2){display:grid;gap:2px}.report-stack small{font-size:9px;font-weight:900;letter-spacing:.12em;color:#2f6fed}.report-stack strong{font-size:15px}.report-stack em{font-size:11px;font-style:normal;color:#8a96aa}.report-stack b{font-size:24px;color:#9aa7bb}.report-icon{width:48px;height:48px;display:grid!important;place-items:center!important;border-radius:15px;background:#eaf1ff;color:#174ca7;font-size:21px;font-weight:900}.report-icon.yellow{background:#fff5d8;color:#d89b00}.report-icon.pale{background:#eef2f7;color:#607086}
.fund-settlement{margin-top:24px;padding:21px 16px 18px;border:1px solid #d8e4f5;border-radius:23px;background:#fff;box-shadow:0 12px 30px #18386b14}.fund-section-heading small{color:#2f6fed;font-size:8px;font-weight:950;letter-spacing:.15em}.fund-section-heading h2{margin-top:4px;font-size:18px;font-weight:950}.fund-section-heading p{margin-top:5px;color:#7c899e;font-size:10px}.remaining-balance{margin-top:15px;padding:17px;border:1px solid #d9e3f1;border-radius:17px;background:linear-gradient(145deg,#f8fbff,#fff)}.remaining-balance span{display:block;color:#8593a8;font-size:9px;font-weight:800}.remaining-balance strong{display:block;margin-top:7px;color:#10192b;font-family:'Space Mono',ui-monospace,monospace;font-size:26px;font-weight:950}.remaining-balance .balance-loading{font-family:inherit;font-size:15px;color:#8a96aa}.fund-options{display:grid;gap:9px;margin-top:12px}.fund-options>button{display:grid;grid-template-columns:22px 1fr;align-items:start;gap:10px;width:100%;padding:14px;border:1px solid #dde5f1;border-radius:16px;background:#f7f9fc;text-align:left}.fund-options>button.selected{border:2px solid #6da4f1;background:#eef5ff;box-shadow:0 5px 15px #2d6fc51a}.fund-options>button>i{width:19px;height:19px;border:2px solid #9db3d7;border-radius:50%}.fund-options>button.selected>i{border:6px solid #17499c}.fund-options>button span{display:grid;gap:4px}.fund-options>button b{font-size:12px}.fund-options>button small{color:#7c899d;font-size:9px;line-height:1.45}.receiving-account{display:grid;grid-template-columns:34px 1fr auto;align-items:center;gap:10px;margin:-2px 8px 2px;padding:12px;border:1px solid #dce5f2;border-radius:14px;background:#fff}.receiving-account>span{display:grid;width:32px;height:32px;place-items:center;border-radius:10px;background:#eaf2ff;color:#17499c;font-size:14px;font-weight:950}.receiving-account b{font-size:10px}.receiving-account small{display:block;margin-top:3px;color:#91a0b5;font-size:8px}.receiving-account em{padding:5px 7px;border-radius:999px;background:#fff0bd;color:#173b86;font-size:8px;font-style:normal;font-weight:900}.fund-error{margin-top:10px;padding:9px 11px;border-radius:10px;background:#fff0f1;color:#c5353b;font-size:9px;line-height:1.5}.settlement-primary{width:100%;height:50px;margin-top:14px;border:0;border-radius:15px;background:#17499c;color:#fff;font-size:12px;font-weight:950}.settlement-primary:disabled{opacity:.55}.settlement-later{display:block;margin:10px auto 0;padding:5px 9px;color:#55709c;background:transparent;font-size:10px;font-weight:850}.ending-actions{margin-top:22px}.ending-actions button{width:100%;height:52px;border-radius:16px;font-size:14px;font-weight:900}.primary{border:0;background:#17499c;color:#fff}.secondary{margin-top:9px;border:1px solid #cfdaea;background:#fff;color:#24426f}
</style>
