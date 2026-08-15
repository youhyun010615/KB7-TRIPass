<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { deleteAccount, getAccounts } from '@/api/asset'
import { deleteCard } from '@/api/card'
import { useCardStore } from '@/stores/cardStore'

const router = useRouter()
const cardStore = useCardStore()
const accounts = ref([])
const loading = ref(true)
const deletingKey = ref('')
const errorMessage = ref('')

const totalBalance = computed(() => accounts.value.reduce(
  (total, account) => total + Number(account.balance ?? 0),
  0,
))

async function loadAssets() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [accountResponse] = await Promise.all([getAccounts(), cardStore.loadCards()])
    accounts.value = accountResponse.data?.data ?? []
  } catch (error) {
    console.error('연동 자산 조회 실패', error)
    errorMessage.value = '연동 자산을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

onMounted(loadAssets)

function formatWon(amount) {
  return `${Number(amount ?? 0).toLocaleString('ko-KR')}원`
}

function accountCaption(account) {
  const parts = [account.accountType]
  if (account.accountNumber) parts.push(account.accountNumber)
  return parts.filter(Boolean).join(' · ') || '연동 계좌'
}

function cardTypeLabel(type) {
  return type === 'CHECK' ? '체크카드' : '신용카드'
}

function accountMark(account) {
  const name = account.accountName ?? ''
  if (name.includes('국민') || name.includes('KB')) return '국'
  if (name.includes('신한')) return '신'
  if (name.includes('카카오')) return '카'
  return name.charAt(0) || '통'
}

function accountTone(account) {
  const name = account.accountName ?? ''
  if (name.includes('신한')) return 'mint'
  if (name.includes('카카오')) return 'yellow'
  return 'navy'
}

async function removeAccount(account) {
  if (!window.confirm(`${account.accountName} 연동을 해제할까요?\n해당 계좌의 거래내역은 소비 분석에서 제외됩니다.`)) return
  deletingKey.value = `account-${account.id}`
  try {
    await deleteAccount(account.id)
    accounts.value = accounts.value.filter((item) => item.id !== account.id)
  } catch (error) {
    console.error('계좌 연동 해제 실패', error)
    errorMessage.value = error.response?.data?.message ?? '계좌 연동 해제에 실패했습니다.'
  } finally {
    deletingKey.value = ''
  }
}

async function removeCard(card) {
  if (!window.confirm(`${card.cardName} 연동을 해제할까요?\n해당 카드의 거래내역은 소비 분석에서 제외됩니다.`)) return
  deletingKey.value = `card-${card.id}`
  try {
    await deleteCard(card.id)
    cardStore.cards = cardStore.cards.filter((item) => item.id !== card.id)
  } catch (error) {
    console.error('카드 연동 해제 실패', error)
    errorMessage.value = error.response?.data?.message ?? '카드 연동 해제에 실패했습니다.'
  } finally {
    deletingKey.value = ''
  }
}
</script>

<template>
  <main class="asset-link-page">
    <header class="page-header">
      <button type="button" aria-label="마이페이지로 이동" @click="router.push('/mypage')">‹</button>
      <h1>연동 계좌</h1>
    </header>

    <div v-if="loading" class="state-message">연동 자산을 불러오는 중...</div>

    <div v-else class="content">
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <section class="summary-card">
        <small>연동된 자산 합계</small>
        <strong>{{ formatWon(totalBalance) }}</strong>
        <p>통장 {{ accounts.length }}개 · 카드 {{ cardStore.cards.length }}장</p>
      </section>

      <section class="asset-section">
        <div class="section-title"><h2>통장</h2><span>{{ accounts.length }}개</span></div>
        <p v-if="accounts.length === 0" class="empty-card">연동된 통장이 없어요.</p>
        <article v-for="account in accounts" :key="account.id" class="asset-card" @click="router.push(`/asset/accounts/${account.id}?isReal=true`)">
          <span class="account-logo" :class="accountTone(account)">{{ accountMark(account) }}</span>
          <div class="asset-info">
            <strong>{{ account.accountName }}</strong>
            <small>{{ accountCaption(account) }}</small>
          </div>
          <div class="asset-value"><strong>{{ formatWon(account.balance) }}</strong></div>
          <button type="button" class="delete-button" :disabled="deletingKey === `account-${account.id}`" @click.stop="removeAccount(account)">
            {{ deletingKey === `account-${account.id}` ? '처리 중' : '삭제' }}
          </button>
        </article>
        <button type="button" class="add-button" @click="router.push('/profile/financial?step=2&from=asset')">＋ 통장 추가하기</button>
      </section>

      <section class="asset-section card-section">
        <div class="section-title"><h2>카드</h2><span>{{ cardStore.cards.length }}장</span></div>
        <p v-if="cardStore.cards.length === 0" class="empty-card">연동된 카드가 없어요.</p>
        <article v-for="card in cardStore.cards" :key="card.id" class="asset-card card-row" @click="router.push(`/mypage/cards/${card.id}/transactions`)">
          <span class="card-visual"><i></i></span>
          <div class="asset-info">
            <strong>{{ card.cardName }}</strong>
            <small>{{ cardTypeLabel(card.cardType) }} · {{ card.maskedCardNumber || '카드번호 비공개' }}</small>
          </div>
          <button type="button" class="delete-button" :disabled="deletingKey === `card-${card.id}`" @click.stop="removeCard(card)">
            {{ deletingKey === `card-${card.id}` ? '처리 중' : '삭제' }}
          </button>
        </article>
        <button type="button" class="add-button" @click="router.push('/profile/financial?step=9&from=asset')">＋ 카드 추가하기</button>
      </section>

      <aside class="unlink-note">
        <span>!</span>
        <p><strong>연동을 해제하면</strong> 해당 자산의 거래내역은 자산 합계와 지출 통계에서 제외돼요.</p>
      </aside>
    </div>

    <BottomNav />
  </main>
</template>

<style scoped>
.asset-link-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:0 0 105px;background:#f4f6fc;color:#10192d}.page-header{display:grid;grid-template-columns:38px 1fr;align-items:center;height:97px;padding:35px 25px 0;background:#fff;border-bottom:1px solid #e5e8ef}.page-header button{border:0;background:transparent;color:#10192d;font-size:34px;line-height:1;text-align:left}.page-header h1{margin:0;font-size:18px;font-weight:900}.content{padding:15px 20px 30px}.state-message{padding:120px 20px;text-align:center;color:#94a3b8;font-size:13px}.error-message{margin:0 0 12px;padding:11px 13px;border-radius:10px;background:#ffebee;color:#c62828;font-size:10px;text-align:center}.summary-card{min-height:126px;padding:22px;border-radius:17px;background:linear-gradient(120deg,#173681,#245ac6);color:#fff;box-shadow:0 10px 22px rgba(23,54,129,.14)}.summary-card small{display:block;color:#e8b24b;font-size:11px;font-weight:900}.summary-card strong{display:block;margin-top:15px;font-size:29px;font-weight:900;letter-spacing:-1px}.summary-card p{margin:10px 0 0;color:#c1cce4;font-size:11px;font-weight:700}.asset-section{margin-top:20px}.section-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:11px}.section-title h2{margin:0;font-size:15px;font-weight:900}.section-title span{color:#9ba8bd;font-size:12px;font-weight:800}.asset-card{position:relative;display:grid;grid-template-columns:48px minmax(0,1fr) auto;align-items:center;gap:12px;min-height:74px;margin-bottom:10px;padding:13px 14px;border-radius:16px;background:#fff;box-shadow:0 7px 20px rgba(24,42,75,.055);cursor:pointer}.account-logo{display:grid;width:42px;height:42px;place-items:center;border-radius:12px;color:#fff;font-size:15px;font-weight:900}.account-logo.navy{background:#17377e}.account-logo.mint{background:#18b89e}.account-logo.yellow{background:#ffb715;color:#10192d}.asset-info{min-width:0;padding-right:4px}.asset-info strong,.asset-info small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.asset-info strong{font-size:13px;font-weight:900}.asset-info small{margin-top:5px;color:#9aa7bb;font-size:10px}.asset-value{align-self:start;padding-top:4px;padding-right:0}.asset-value strong{font-size:13px;white-space:nowrap}.delete-button{position:absolute;right:14px;bottom:12px;padding:4px 9px;border:1px solid #dce3ed;border-radius:14px;background:#fff;color:#9aa7bb;font-size:9px;font-weight:800}.delete-button:disabled{opacity:.55}.add-button{width:100%;height:49px;border:1.5px dashed #c7d7f3;border-radius:15px;background:#fff;color:#2f75ef;font-size:12px;font-weight:900}.empty-card{margin:0 0 10px;padding:25px 15px;border-radius:16px;background:#fff;color:#9aa7bb;text-align:center;font-size:11px}.card-section{margin-top:22px}.card-row{grid-template-columns:58px minmax(0,1fr);min-height:74px;padding-right:70px}.card-visual{position:relative;display:block;width:58px;height:42px;border-radius:9px;background:linear-gradient(135deg,#153477,#0f2862)}.card-visual i{position:absolute;left:9px;bottom:7px;width:16px;height:11px;border-radius:3px;background:linear-gradient(135deg,#ffd84a,#eaa814)}.card-row .delete-button{top:26px;bottom:auto}.unlink-note{display:flex;gap:9px;margin-top:22px;padding:14px;border-radius:14px;background:#fff4df;color:#a66c20}.unlink-note>span{display:grid;width:18px;height:18px;flex:0 0 auto;place-items:center;border-radius:50%;background:#eab44d;color:#fff;font-size:10px;font-weight:900}.unlink-note p{margin:0;font-size:9px;line-height:1.55}.unlink-note strong{font-weight:900}
</style>
