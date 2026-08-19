<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { deleteAccount, getAccounts } from '@/api/asset'
import { deleteCard } from '@/api/card'
import { useCardStore } from '@/stores/cardStore'
import { bankPresentationByCode, bankPresentationByName } from '@/stores/asset'

const router = useRouter()
const cardStore = useCardStore()
const accounts = ref([])
const loading = ref(true)
const deletingKeys = ref(new Set())
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
  if (account.accountNumber) parts.push(maskAccountNumber(account.accountNumber))
  return parts.filter(Boolean).join(' · ') || '연동 계좌'
}

function maskAccountNumber(number) {
  const value = String(number).replace(/\s/g, '')
  if (value.length <= 4) return `•••• ${value}`
  return `•••• •••• ${value.slice(-4)}`
}

function cardTypeLabel(type) {
  return type === 'CHECK' ? '체크카드' : '신용카드'
}

// 계좌의 organizationCode(CODEF 연동 기관 코드)를 우선으로, 없으면 계좌명으로 은행을 추정해
// 은행 고유 컬러/심볼을 찾는다.
function resolveBankMeta(account) {
  const byCode = bankPresentationByCode(account.organizationCode)
  if (byCode.code) return byCode

  const name = account.accountName ?? ''
  if (name.includes('국민') || name.includes('KB')) return bankPresentationByName('KB국민은행')
  if (name.includes('신한')) return bankPresentationByName('신한은행')
  if (name.includes('우리')) return bankPresentationByName('우리은행')
  if (name.includes('하나')) return bankPresentationByName('하나은행')
  if (name.includes('농협')) return bankPresentationByName('NH농협은행')
  if (name.includes('기업')) return bankPresentationByName('IBK기업은행')
  if (name.includes('K뱅크') || name.includes('케이뱅크')) return bankPresentationByName('K뱅크')
  if (name.includes('대구')) return bankPresentationByName('대구은행')
  if (name.includes('카카오')) return bankPresentationByName('카카오뱅크')
  return byCode
}

function isDeleting(key) {
  return deletingKeys.value.has(key)
}

function setDeleting(key, pending) {
  const next = new Set(deletingKeys.value)
  if (pending) next.add(key)
  else next.delete(key)
  deletingKeys.value = next
}

async function removeAccount(account) {
  const key = `account-${account.id}`
  if (isDeleting(key)) return
  if (!window.confirm(`${account.accountName} 연동을 해제할까요?\n해당 계좌의 거래내역은 소비 분석에서 제외됩니다.`)) return
  setDeleting(key, true)
  try {
    await deleteAccount(account.id)
    accounts.value = accounts.value.filter((item) => item.id !== account.id)
  } catch (error) {
    console.error('계좌 연동 해제 실패', error)
    errorMessage.value = error.response?.data?.message ?? '계좌 연동 해제에 실패했습니다.'
  } finally {
    setDeleting(key, false)
  }
}

async function removeCard(card) {
  const key = `card-${card.id}`
  if (isDeleting(key)) return
  if (!window.confirm(`${card.cardName} 연동을 해제할까요?\n해당 카드의 거래내역은 소비 분석에서 제외됩니다.`)) return
  setDeleting(key, true)
  try {
    await deleteCard(card.id)
    cardStore.cards = cardStore.cards.filter((item) => item.id !== card.id)
  } catch (error) {
    console.error('카드 연동 해제 실패', error)
    errorMessage.value = error.response?.data?.message ?? '카드 연동 해제에 실패했습니다.'
  } finally {
    setDeleting(key, false)
  }
}
</script>

<template>
  <main class="asset-link-page">
    <header class="page-header">
      <button type="button" aria-label="마이페이지로 이동" @click="router.push('/mypage')">‹</button>
      <h1>자산관리</h1>
    </header>

    <div v-if="loading" class="state-message">연동 자산을 불러오는 중...</div>

    <div v-else class="content">
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <section class="summary-card">
        <div class="summary-heading">
          <div>
            <span class="summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="1.8" />
                <path d="m8.5 8.5 2 7 1.5-4 1.5 4 2-7M8.1 11.2h7.8M8.5 13.3h7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </span>
            <small>총 연동 자산</small>
          </div>
        </div>
        <strong>{{ formatWon(totalBalance) }}</strong>
        <div class="summary-counts">
          <span><small>연결 통장</small><b>{{ accounts.length }}개</b></span>
          <i></i>
          <span><small>연결 카드</small><b>{{ cardStore.cards.length }}장</b></span>
        </div>
      </section>

      <section class="asset-section">
        <div class="section-title"><div><h2>내 통장</h2><small>좌우로 밀어 계좌를 확인하세요</small></div><span>{{ accounts.length }}개</span></div>
        <div class="account-carousel">
          <article
            v-for="account in accounts"
            :key="account.id"
            class="account-slide"
            :style="{ '--bank-color': resolveBankMeta(account).color }"
            @click="router.push(`/asset/accounts/${account.id}?isReal=true`)"
          >
            <div class="account-slide-top">
              <div class="account-identity">
                <span class="account-logo" :style="{ color: resolveBankMeta(account).text }">{{ resolveBankMeta(account).symbol }}</span>
                <div><strong>{{ account.accountName }}</strong><small>{{ accountCaption(account) }}</small></div>
              </div>
              <button type="button" class="delete-button" :disabled="isDeleting(`account-${account.id}`)" @click.stop="removeAccount(account)">{{ isDeleting(`account-${account.id}`) ? '처리 중' : '연동 해제' }}</button>
            </div>
            <div class="account-slide-balance"><small>현재 잔액</small><b>{{ formatWon(account.balance) }}</b></div>
            <div class="account-slide-bank"><i></i>{{ resolveBankMeta(account).name }}</div>
          </article>
          <button type="button" class="account-slide account-add-slide" @click="router.push('/profile/financial?step=2&from=asset')">
            <span>＋</span>
            <strong>통장 추가하기</strong>
            <small>다른 금융기관 계좌를 연결해 보세요</small>
          </button>
        </div>
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
          <button type="button" class="delete-button" :disabled="isDeleting(`card-${card.id}`)" @click.stop="removeCard(card)">
            {{ isDeleting(`card-${card.id}`) ? '처리 중' : '삭제' }}
          </button>
        </article>
        <button type="button" class="add-button" @click="router.push('/profile/financial?step=9&from=asset')"><span>＋</span><div><b>카드 추가하기</b><small>소비 내역을 함께 관리해요</small></div></button>
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
/* 자산관리 리뉴얼 */
.asset-link-page{background:#f2f5fa}.page-header{height:100px;padding:38px 20px 0;background:#f2f5fa;border-bottom:0}.page-header>button{color:#13213b;font-size:32px}.page-header small{display:block;color:#2f6fea;font-family:'Space Mono',monospace;font-size:8px;font-weight:800;letter-spacing:.12em}.page-header h1{margin-top:2px;font-size:18px;font-weight:800}.content{padding:12px 18px 30px}.summary-card{position:relative;min-height:138px;padding:21px;border-radius:24px;overflow:hidden;background:linear-gradient(135deg,#102e70 0%,#2459be 100%);box-shadow:0 14px 30px rgba(22,56,128,.2)}.summary-card::after{content:'';position:absolute;right:-38px;top:-55px;width:145px;height:145px;border-radius:50%;background:rgba(255,255,255,.08)}.summary-heading{display:flex;align-items:center;justify-content:space-between}.summary-heading small{color:#dbe7ff;font-size:10px;font-weight:700}.summary-heading span{padding:5px 8px;border-radius:99px;background:rgba(255,255,255,.12);color:#dbe7ff;font-size:8px}.summary-card>strong{margin-top:18px;font-size:27px;font-weight:800}.summary-card>p{margin-top:9px;color:#bfceed;font-size:10px;font-weight:600}.asset-section{margin-top:24px}.section-title{align-items:flex-end;margin:0 2px 11px}.section-title h2{font-size:15px;font-weight:800}.section-title div>small{display:block;margin-top:3px;color:#97a4b7;font-size:8.5px}.section-title>span{color:#8190a7;font-size:10px;font-weight:700}.account-carousel{display:flex;gap:12px;margin:0 -18px;padding:0 18px 8px;overflow-x:auto;scroll-snap-type:x mandatory;scroll-padding:18px;scrollbar-width:none}.account-carousel::-webkit-scrollbar{display:none}.account-slide{position:relative;flex:0 0 285px;min-height:166px;padding:17px;border-radius:21px;color:#fff;overflow:hidden;scroll-snap-align:start;cursor:pointer;box-shadow:0 10px 22px rgba(20,45,99,.15);background:linear-gradient(145deg,color-mix(in srgb,var(--bank-color,#17387f) 88%,black 4%),color-mix(in srgb,var(--bank-color,#17387f) 40%,black 60%))}.account-slide::after{content:'';position:absolute;right:-38px;bottom:-64px;width:145px;height:145px;border:1px solid rgba(255,255,255,.13);border-radius:50%}.account-slide-top{display:flex;align-items:center;justify-content:space-between}.account-slide .account-logo{display:grid;width:34px;height:34px;place-items:center;border-radius:11px;background:rgba(255,255,255,.16);color:#fff;font-size:13px;font-weight:900}.account-slide .delete-button{position:relative;right:auto;bottom:auto;z-index:2;padding:5px 8px;border:1px solid rgba(255,255,255,.2);border-radius:99px;background:rgba(255,255,255,.09);color:#dbe5f8;font-size:8px;font-weight:700}.account-slide-info{margin-top:16px}.account-slide-info small,.account-slide-info strong{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.account-slide-info small{color:rgba(255,255,255,.6);font-size:8.5px}.account-slide-info strong{margin-top:4px;font-size:13px}.account-slide-balance{margin-top:15px}.account-slide-balance small{display:block;color:rgba(255,255,255,.62);font-size:8px}.account-slide-balance b{display:block;margin-top:3px;font-size:19px;letter-spacing:-.04em}.add-button{display:flex;align-items:center;gap:10px;width:100%;min-height:52px;height:auto;margin-top:5px;padding:9px 13px;border:1px solid #dce5f2;border-radius:16px;background:#fff;color:#1c5cca;text-align:left;box-shadow:0 5px 14px rgba(25,49,86,.035)}.add-button>span{display:grid;width:30px;height:30px;place-items:center;border-radius:10px;background:#eaf2ff;font-size:16px}.add-button b,.add-button small{display:block}.add-button b{font-size:11px}.add-button small{margin-top:2px;color:#99a5b7;font-size:8px}.empty-card{border:1px dashed #d4deec;font-size:10px}.card-section{margin-top:24px}.card-section .asset-card{min-height:70px;margin-bottom:9px;padding:12px 66px 12px 12px;border:1px solid #e8edf4;border-radius:17px;box-shadow:0 6px 18px rgba(24,42,75,.04)}.card-section .asset-info strong{font-size:12px;font-weight:800}.card-section .asset-info small{font-size:8.5px}.card-visual{width:52px;height:36px;border-radius:8px}.card-row .delete-button{right:12px;top:24px;bottom:auto}.unlink-note{padding:13px}.unlink-note p{font-size:8.5px}
.account-slide-top{gap:9px}
.account-slide-bank{display:flex;align-items:center;gap:5px;margin-top:10px;color:rgba(255,255,255,.78);font-size:8px;font-weight:800}
.account-slide-bank i{display:block;width:5px;height:5px;border-radius:50%;background:#fff}
.account-identity{display:flex;align-items:center;gap:9px;min-width:0}
.account-identity>div{min-width:0}
.account-identity strong,.account-identity small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.account-identity strong{font-size:11px;font-weight:800}
.account-identity small{margin-top:3px;color:rgba(255,255,255,.68);font-size:7.5px;letter-spacing:.02em}
.account-slide .account-logo{flex:0 0 34px;background:rgba(255,255,255,.18)}
.account-slide .delete-button{flex:0 0 auto;color:#eef4ff;background:rgba(255,255,255,.11)}
.account-slide-balance{margin-top:35px}
.account-add-slide{display:flex;flex-direction:column;align-items:center;justify-content:center;border:1.5px dashed #b9c9df;background:#f8fbff!important;color:#1d4f9f;box-shadow:none;text-align:center}
.account-add-slide::after{display:none}
.account-add-slide>span{display:grid;width:38px;height:38px;place-items:center;border-radius:13px;background:#e5efff;color:#2865ca;font-size:20px}
.account-add-slide>strong{margin-top:11px;font-size:12px;font-weight:800}
.account-add-slide>small{margin-top:4px;color:#8c9bb0;font-size:8.5px}
.page-header{height:92px;padding-top:34px}
.page-header h1{margin:0;font-size:20px;font-weight:800;letter-spacing:-.03em}
.summary-card{min-height:auto;padding:20px;border:1px solid #bfd2f1;border-radius:22px;background:linear-gradient(135deg,#dce9fb 0%,#c8daf6 100%);color:#10192d;box-shadow:0 11px 25px rgba(35,73,136,.12)}
.summary-card::before{content:'';position:absolute;left:-34px;bottom:-62px;width:132px;height:132px;border:1px solid rgba(36,91,179,.14);border-radius:50%}
.summary-card::after{right:-48px;top:-68px;width:160px;height:160px;background:rgba(73,126,211,.13)}
.summary-heading{position:relative;z-index:1;align-items:center}
.summary-heading>div{display:flex;align-items:center;gap:8px}
.summary-icon{display:grid;width:30px;height:30px;place-items:center;border-radius:10px;background:#d3e0f4;color:#111827;font-size:14px;font-weight:800;box-shadow:none}
.summary-icon svg{display:block;width:18px;height:18px;color:#111827}
.summary-heading small{color:#29466f;font-size:12px;font-weight:800}
.summary-heading>span{padding:5px 8px;background:rgba(255,255,255,.48);color:#4c6385;font-size:8px;font-weight:700}
.summary-card>strong{position:relative;z-index:1;margin-top:18px;color:#10192d;font-size:27px;font-weight:800;letter-spacing:-.04em}
.summary-counts{position:relative;z-index:1;display:grid;grid-template-columns:1fr 1px 1fr;align-items:center;gap:15px;margin-top:20px;padding:13px 15px;border:1px solid rgba(255,255,255,.5);border-radius:15px;background:rgba(255,255,255,.48);backdrop-filter:blur(5px)}
.summary-counts>span{display:flex;align-items:center;justify-content:space-between;gap:8px}
.summary-counts small{color:#64758e;font-size:9px;font-weight:600}
.summary-counts b{color:#174a99;font-size:11px;font-weight:800;white-space:nowrap}
.summary-counts i{width:1px;height:20px;background:#b8c9e2}
</style>
