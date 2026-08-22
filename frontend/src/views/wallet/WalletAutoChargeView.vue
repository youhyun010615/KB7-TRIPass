<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from '@lucide/vue'
import { useTripWalletStore } from '@/stores/tripWallet'

const router = useRouter()
const wallet = useTripWalletStore()
const transferDay = ref(wallet.autoCharge.day)
const notice = ref('')

const accounts = computed(() => wallet.linkedAccounts)
const primaryAccount = computed(() => accounts.value.find(account => account.isPrimary) ?? null)

onMounted(async () => {
  try {
    await Promise.all([wallet.loadAccounts(), wallet.loadAutoSaving()])
    transferDay.value = wallet.autoCharge.day || transferDay.value
  } catch {
    notice.value = wallet.errorMessage || '자동 충전 정보를 불러오지 못했어요.'
  }
})

async function saveAutoCharge() {
  if (!primaryAccount.value) {
    notice.value = '먼저 주계좌를 설정해 주세요.'
    return
  }

  try {
    await wallet.updateAutoCharge({ day: transferDay.value, amount: wallet.autoCharge.amount, enabled: true })
    router.push('/wallet')
  } catch {
    notice.value = wallet.errorMessage || '자동 충전 설정 저장에 실패했어요.'
  }
}
</script>

<template>
  <main class="auto-page">
    <header class="page-header">
      <button type="button" class="back-button" @click="router.back()"><ChevronLeft :size="22" /></button>
      <h1>자동 충전 설정</h1>
      <span aria-hidden="true"></span>
    </header>

    <section class="setting-panel">
      <h2>자동 충전</h2>
      <p>매달 지정한 날짜에 주계좌에서<br>목표 금액이 자동으로 송금돼요.</p>

      <label class="date-field">
        <span>송금일</span>
        <strong>매월 {{ transferDay }}일</strong>
        <button type="button" aria-label="송금일 선택">▣</button>
      </label>

      <div class="account-head">
        <h3>출금 계좌</h3>
      </div>

      <div v-if="primaryAccount" class="account-list">
        <div class="account-card primary">
          <span>{{ primaryAccount.logo }}</span>
          <div>
            <b>{{ primaryAccount.name }}</b>
            <small>{{ primaryAccount.number }}</small>
          </div>
          <em>주계좌</em>
        </div>
      </div>
      <p v-else class="notice-box error">주계좌가 설정되어 있지 않아요. 연결계좌에서 주계좌를 먼저 설정해 주세요.</p>
    </section>

    <p v-if="notice" class="notice-box error">{{ notice }}</p>
    <p v-else class="notice-box">변경 내용은 다음 송금부터 적용됩니다.</p>
    <button class="save-button" type="button" @click="saveAutoCharge">변경 내용 저장하기</button>
  </main>
</template>

<style scoped>
.auto-page{max-width:430px;min-height:100vh;margin:0 auto;padding:46px 18px 36px;background:#eef2f8;color:#111827}.page-header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center}.page-header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;box-shadow:0 5px 16px rgba(36,72,117,.07)}.page-header h1{font-size:27px;font-weight:800;letter-spacing:-.04em;text-align:center}.setting-panel{margin-top:34px;padding:25px 22px 26px;border:2px solid #2d6ef0;border-radius:22px;background:#fff}.setting-panel h2{font-size:21px;font-weight:800}.setting-panel p{margin-top:12px;color:#8b9ab0;font-size:15px;font-weight:500;line-height:1.55}.date-field{display:grid;grid-template-columns:1fr auto;gap:6px;margin-top:28px;padding:19px 18px;border-radius:18px;background:#f5f7fb}.date-field span{grid-column:1;color:#91a0b5;font-size:13px;font-weight:600}.date-field strong{grid-column:1;font-size:20px;font-weight:800}.date-field button{grid-column:2;grid-row:1/3;align-self:center;width:44px;height:44px;border-radius:14px;background:#e9f1ff;color:#286df0;font-size:18px}.account-head{display:flex;align-items:center;justify-content:space-between;margin-top:33px}.account-head h3{font-size:19px;font-weight:800}.account-list{display:grid;gap:13px;margin-top:18px}.account-card{position:relative;display:grid;grid-template-columns:42px 1fr auto;align-items:center;gap:12px;padding:18px;border-radius:17px;background:#f6f8fc;text-align:left;color:#111827}.account-card.primary{border:2px solid #2d6ef0;background:#edf4ff}.account-card span{display:grid;width:42px;height:42px;place-items:center;border-radius:12px;background:#2d6ef0;color:#fff;font-size:18px;font-weight:800}.account-card b{display:block;font-size:17px;font-weight:800}.account-card small{display:block;margin-top:6px;color:#8fa0b7;font-size:13px;font-weight:500}.account-card em{padding:4px 7px;border-radius:99px;background:#eaf3ff;color:#1463ed;font-size:11px;font-style:normal;font-weight:700}.notice-box{margin-top:24px;padding:18px;border-radius:17px;background:#fff5df;color:#a36c07;font-size:15px;font-weight:700}.notice-box.error{background:#fff1f2;color:#e11d48}.save-button{position:fixed;left:50%;bottom:32px;width:min(calc(100% - 36px),394px);height:62px;transform:translateX(-50%);border-radius:17px;background:#2e70ed;color:#fff;font-size:20px;font-weight:800}@media(max-width:380px){.setting-panel{padding:22px 18px}}
.auto-page{word-break:keep-all}.auto-page h1,.auto-page h2,.auto-page h3{text-wrap:balance}.auto-page p{text-wrap:pretty}
.auto-page{padding:14px 16px 30px;background:#f2f5fa}.page-header button{width:36px;height:36px}.page-header h1{color:#29466f;font-size:20px;font-weight:700;letter-spacing:-.025em}.setting-panel{margin-top:22px;padding:18px 16px 20px;border-width:1px;border-radius:19px}.setting-panel h2{font-size:17px}.setting-panel p{margin-top:8px;font-size:12px}.date-field{margin-top:20px;padding:13px 14px;border-radius:14px}.date-field span{font-size:10.5px}.date-field strong{font-size:16px}.date-field button{width:35px;height:35px;border-radius:11px;font-size:14px}.account-head{margin-top:23px}.account-head h3{font-size:16px}.account-list{gap:9px;margin-top:13px}.account-card{grid-template-columns:34px 1fr auto;gap:9px;padding:12px;border-radius:14px}.account-card span{width:34px;height:34px;border-radius:10px;font-size:14px}.account-card b{font-size:14px}.account-card small{margin-top:4px;font-size:10.5px}.notice-box{margin-top:17px;padding:13px;border-radius:14px;font-size:11px}.save-button{bottom:24px;width:min(calc(100% - 32px),398px);height:46px;border-radius:13px;font-size:14px;font-weight:700}
</style>
