<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTripWalletStore } from '@/stores/tripWallet'

const router = useRouter()
const wallet = useTripWalletStore()
const amount = ref('')
const selectedAccount = ref('KB국민은행 여행통장 · ****5320')
const showTransfer = ref(false)
const notice = ref('')
const accounts = ['KB국민은행 여행통장 · ****5320', '신한은행 통장 · ****8421', '카카오뱅크 입출금통장 · ****1108']

const money = value => `${Math.abs(Number(value || 0)).toLocaleString('ko-KR')}원`
const maxChart = computed(() => Math.max(...wallet.monthlyDeposits, 1))

function submitTransfer() {
  if (!wallet.deposit(amount.value, '8월 여행 저축', selectedAccount.value)) {
    notice.value = '송금할 금액을 입력해 주세요.'
    return
  }
  notice.value = `${money(amount.value)}을 TRIP 월렛에 담았어요.`
  amount.value = ''
  showTransfer.value = false
}
</script>

<template>
  <main class="wallet-page">
    <header class="wallet-header">
      <p>TRIPASS</p>
      <div>
        <h1>TRIP 월렛</h1>
        <span>여행을 위한 나만의 가상 저축 공간</span>
      </div>
    </header>

    <section class="wallet-ticket">
      <div class="ticket-topline"><span>TRIP WALLET</span><span>TRIPASS AIR</span><span>WALLET 01</span></div>
      <div class="wallet-cutline"><i/><b/><i/></div>
      <p class="wallet-label">현재 모은 여행 자금</p>
      <strong>{{ money(wallet.balance) }}</strong>
      <div class="wallet-goal">
        <div><span>여행 저축 목표</span><b>5,000,000원</b></div>
        <div class="wallet-progress"><i :style="{ width: `${Math.min(100, wallet.balance / 5000000 * 100)}%` }"/></div>
        <small>{{ Math.round(wallet.balance / 5000000 * 100) }}% 달성</small>
      </div>
      <button @click="showTransfer = true"><span>송금하기</span><b>|||||||</b><em>›</em></button>
    </section>

    <section class="monthly-card">
      <div class="section-heading"><div><small>8월</small><h2>이번 달 저축</h2></div><strong>{{ money(wallet.monthDeposit) }}</strong></div>
      <div class="chart">
        <div v-for="(value, index) in wallet.monthlyDeposits" :key="index" class="chart-col">
          <i :style="{ height: `${Math.max(16, value / maxChart * 88)}px` }"/>
          <span>{{ 5 + index }}월</span>
        </div>
      </div>
      <p>월마다 모은 금액을 확인할 수 있어요.</p>
    </section>

    <section class="history-card">
      <div class="section-heading"><h2>송금 내역</h2><button @click="router.push('/wallet')">전체 보기 ›</button></div>
      <article v-for="transaction in wallet.transactions.slice(0, 4)" :key="transaction.id">
        <span class="history-icon" :class="transaction.type === '입금' ? 'deposit' : 'withdraw'">{{ transaction.type === '입금' ? '↓' : '↑' }}</span>
        <div><b>{{ transaction.title }}</b><small>{{ transaction.date }} · {{ transaction.note }}</small></div>
        <strong :class="transaction.amount > 0 ? 'deposit-text' : 'withdraw-text'">{{ transaction.amount > 0 ? '+' : '-' }}{{ money(transaction.amount) }}</strong>
      </article>
    </section>

    <Transition name="sheet">
      <div v-if="showTransfer" class="wallet-sheet-backdrop" @click.self="showTransfer = false">
        <section class="wallet-sheet">
          <i class="sheet-handle"/>
          <h2>TRIP 월렛에 송금하기</h2>
          <p>연결된 계좌에서 여행 저축금을 옮겨요.</p>
          <label>출금 계좌<select v-model="selectedAccount"><option v-for="account in accounts" :key="account">{{ account }}</option></select></label>
          <label>송금 금액<div class="amount-field"><input v-model="amount" inputmode="numeric" placeholder="0"/><b>원</b></div></label>
          <div class="quick-amounts"><button v-for="value in [100000,300000,500000]" :key="value" @click="amount = value.toString()">+{{ value / 10000 }}만</button></div>
          <button class="confirm-transfer" @click="submitTransfer">송금하기</button>
        </section>
      </div>
    </Transition>
    <p v-if="notice" class="wallet-toast">{{ notice }}</p>
    <BottomNav />
  </main>
</template>

<style scoped>
.wallet-page{min-height:100vh;padding:42px 20px 94px;background:#f5f7fe;color:#14254b}.wallet-header>p{margin-bottom:9px;color:#2e68e8;font-size:10px;font-weight:900;letter-spacing:.16em}.wallet-header h1{font-size:28px;font-weight:900;letter-spacing:-.06em}.wallet-header span{display:block;margin-top:6px;color:#8290aa;font-size:12px}.wallet-ticket{position:relative;margin-top:22px;overflow:hidden;border-radius:22px;padding:18px 18px 16px;background:linear-gradient(135deg,#123071,#235ab9);box-shadow:0 14px 28px #254f9d38;color:white}.ticket-topline{display:flex;justify-content:space-between;color:#b7cbf5;font-size:8px;font-weight:700;letter-spacing:.08em}.wallet-cutline{display:flex;align-items:center;gap:8px;margin:15px -31px 18px}.wallet-cutline i{width:20px;height:20px;border-radius:50%;background:#f5f7fe}.wallet-cutline b{flex:1;border-top:1px dashed #d8e4ff99}.wallet-label{color:#b7cbf5;font-size:12px}.wallet-ticket>strong{display:block;margin-top:5px;font-size:34px;font-weight:900;letter-spacing:-.05em}.wallet-goal{margin-top:20px;padding:13px;border-radius:14px;background:#ffffff18}.wallet-goal div:first-child{display:flex;justify-content:space-between;font-size:10px}.wallet-goal b{font-size:11px}.wallet-progress{height:7px;margin-top:11px;overflow:hidden;border-radius:99px;background:#ffffff44}.wallet-progress i{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#63ddcd,#f2e6b1)}.wallet-goal small{display:block;margin-top:7px;color:#c7d6fa;font-size:10px}.wallet-ticket>button{display:flex;width:100%;align-items:center;gap:14px;justify-content:space-between;margin-top:16px;padding:10px 13px;border-radius:12px;background:#fff;color:#f0544f;font-size:13px;font-weight:900}.wallet-ticket>button b{margin-left:auto;color:#183872;letter-spacing:1px}.wallet-ticket>button em{font-size:20px;font-style:normal;color:#183872}.monthly-card,.history-card{margin-top:16px;padding:18px;border:1px solid #dfe6f4;border-radius:20px;background:white;box-shadow:0 8px 18px #1537750a}.section-heading{display:flex;align-items:center;justify-content:space-between}.section-heading h2{font-size:17px;font-weight:900}.section-heading small{color:#0b9d83;font-size:10px;font-weight:800}.section-heading strong{color:#0b9d83;font-size:19px;font-weight:900}.section-heading button{color:#6f7e98;font-size:11px}.chart{display:flex;height:118px;align-items:end;justify-content:space-around;margin-top:16px;padding:12px 4px 0;border-bottom:1px solid #e8edf5}.chart-col{display:flex;align-items:center;flex-direction:column;justify-content:end;gap:8px;height:100%;color:#93a0b5;font-size:10px}.chart-col i{width:27px;min-height:16px;border-radius:8px 8px 3px 3px;background:linear-gradient(#2a73ee,#9ec6ff)}.chart-col:last-child i{background:linear-gradient(#09a98e,#81e0cf)}.monthly-card>p{margin-top:13px;color:#90a0b7;font-size:11px}.history-card article{display:flex;align-items:center;gap:11px;padding:13px 0;border-bottom:1px solid #eff2f7}.history-card article:last-child{border:0;padding-bottom:0}.history-icon{display:grid;width:31px;height:31px;place-items:center;border-radius:11px;font-size:16px;font-weight:900}.history-icon.deposit{background:#e2f9f2;color:#009d82}.history-icon.withdraw{background:#fff0f0;color:#ef5350}.history-card article div{flex:1;min-width:0}.history-card article b{display:block;font-size:12px}.history-card article small{display:block;overflow:hidden;margin-top:4px;color:#8d9bb1;font-size:9px;text-overflow:ellipsis;white-space:nowrap}.history-card article strong{font-size:12px}.deposit-text{color:#009d82}.withdraw-text{color:#ef5350}.wallet-sheet-backdrop{position:fixed;z-index:100;inset:0;display:flex;align-items:end;background:#17233c66}.wallet-sheet{width:min(100%,390px);padding:12px 20px 28px;border-radius:24px 24px 0 0;background:#fff}.sheet-handle{display:block;width:36px;height:4px;margin:0 auto 17px;border-radius:9px;background:#d5ddeb}.wallet-sheet h2{font-size:20px;font-weight:900}.wallet-sheet>p{margin-top:7px;color:#8290a6;font-size:12px}.wallet-sheet label{display:block;margin-top:21px;color:#63718a;font-size:11px;font-weight:800}.wallet-sheet select,.amount-field{width:100%;height:52px;margin-top:8px;padding:0 14px;border:1px solid #dce4f1;border-radius:13px;background:#fff;color:#182849;font-size:14px;font-weight:700}.amount-field{display:flex;align-items:center}.amount-field input{width:100%;font-size:20px;font-weight:900;outline:0}.amount-field b{color:#7d8ba2}.quick-amounts{display:flex;gap:8px;margin-top:9px}.quick-amounts button{padding:7px 12px;border-radius:99px;background:#edf3ff;color:#2764d7;font-size:11px;font-weight:800}.confirm-transfer{width:100%;height:54px;margin-top:27px;border-radius:14px;background:#193d8c;color:#fff;font-size:15px;font-weight:900}.wallet-toast{position:fixed;bottom:78px;left:50%;z-index:120;width:max-content;max-width:calc(100% - 40px);transform:translateX(-50%);padding:11px 15px;border-radius:99px;background:#172644;color:#fff;font-size:11px;box-shadow:0 5px 18px #11182733}.sheet-enter-active,.sheet-leave-active{transition:opacity .2s}.sheet-enter-active .wallet-sheet,.sheet-leave-active .wallet-sheet{transition:transform .25s}.sheet-enter-from,.sheet-leave-to{opacity:0}.sheet-enter-from .wallet-sheet,.sheet-leave-to .wallet-sheet{transform:translateY(100%)}
</style>
