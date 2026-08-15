<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from '@lucide/vue'
import BottomNav from '@/components/common/BottomNav.vue'
import AnimatedNumber from '@/components/common/AnimatedNumber.vue'
import { useTripWalletStore } from '@/stores/tripWallet'

const router = useRouter()
const wallet = useTripWalletStore()

const showAddModal = ref(false)
const selectedOptionAccountId = ref(null)
const showDeleteConfirm = ref(false)
const pendingDeleteAccount = ref(null)
const notice = ref('')
let noticeTimer

const totalBalance = computed(() => wallet.linkedAccounts.reduce((sum, account) => sum + Number(account.balance || 0), 0))

// 프로필에 등록된 계좌 중 아직 월렛에 연결되지 않은 계좌만 API로 조회한다.
const availableAccounts = computed(() => wallet.accountOptions)

const money = value => `${Number(value || 0).toLocaleString('ko-KR')}원`
const moneyFormatter = value => `${value.toLocaleString('ko-KR')}원`

function showNotice(message) {
  notice.value = message
  window.clearTimeout(noticeTimer)
  noticeTimer = window.setTimeout(() => {
    notice.value = ''
  }, 3000)
}

function requestRemoveAccount(account) {
  pendingDeleteAccount.value = account
  showDeleteConfirm.value = true
}

async function confirmRemoveAccount() {
  if (!pendingDeleteAccount.value) return
  const target = pendingDeleteAccount.value
  showDeleteConfirm.value = false
  pendingDeleteAccount.value = null

  try {
    await wallet.removeAccount(target.id)
    showNotice('계좌 연결을 해제했어요.')
  } catch {
    showNotice(wallet.errorMessage || '계좌 연결 해제에 실패했어요.')
  }
}

async function makePrimary(account) {
  if (account.isPrimary) return

  try {
    await wallet.setPrimaryAccount(account.id)
    showNotice(`${account.name}을(를) 주계좌로 설정했어요.`)
  } catch {
    showNotice(wallet.errorMessage || '주계좌 설정에 실패했어요.')
  }
}

onBeforeUnmount(() => window.clearTimeout(noticeTimer))

function openAddModal() {
  selectedOptionAccountId.value = availableAccounts.value[0]?.accountId ?? null
  showAddModal.value = true
  wallet.loadAccountOptions().then((items) => {
    selectedOptionAccountId.value = items[0]?.accountId ?? null
  }).catch(() => {
    showNotice(wallet.errorMessage || '추가 가능한 계좌를 불러오지 못했어요.')
  })
}

async function confirmAddAccount() {
  const picked = availableAccounts.value.find(account => account.accountId === selectedOptionAccountId.value)
  if (!picked) return

  try {
    await wallet.addAccount(picked.accountId, wallet.linkedAccounts.length === 0)
    showAddModal.value = false
    showNotice('계좌를 월렛에 연결했어요.')
  } catch {
    showNotice(wallet.errorMessage || '계좌 추가에 실패했어요.')
  }
}

onMounted(async () => {
  try {
    await Promise.all([wallet.loadAccounts(), wallet.loadAccountOptions()])
  } catch {
    showNotice(wallet.errorMessage || '연결계좌를 불러오지 못했어요.')
  }
})
</script>

<template>
  <main class="accounts-page">
    <header class="page-header">
      <button type="button" class="back-button" @click="router.back()"><ChevronLeft :size="22" /></button>
      <div>
        <h1>트립월렛 연결계좌</h1>
      </div>
    </header>

    <section class="summary-card">
      <span>전체 계좌 잔액</span>
      <strong><AnimatedNumber :value="totalBalance" :formatter="moneyFormatter" /></strong>
    </section>

    <p v-if="notice" class="notice-text">{{ notice }}</p>

    <section class="account-card">
      <div class="section-head">
        <h2>전체 연결계좌</h2>
        <span>{{ wallet.linkedAccounts.length }}개</span>
      </div>

      <div v-if="wallet.linkedAccounts.length" class="account-list">
        <article v-for="account in wallet.linkedAccounts" :key="account.id" class="account-row" :class="{ primary: account.isPrimary }">
          <div class="logo">{{ account.logo }}</div>
          <div class="account-info">
            <h3>{{ account.name }}</h3>
            <p>{{ account.number }}</p>
          </div>
          <div class="amount-info">
            <strong>{{ money(account.balance) }}</strong>
            <div class="row-actions">
              <button
                v-if="!account.isPrimary"
                type="button"
                class="set-primary"
                @click="makePrimary(account)"
              >
                주계좌로 설정
              </button>
              <span v-else class="primary-badge">주계좌</span>
              <button type="button" @click="requestRemoveAccount(account)">삭제</button>
            </div>
          </div>
        </article>
      </div>
      <p v-else class="list-empty">연결된 계좌가 없어요.</p>
    </section>

    <button type="button" class="add-account" @click="openAddModal">+ 계좌 추가</button>

    <Transition name="fade">
      <div v-if="showAddModal" class="add-modal-backdrop" @click.self="showAddModal = false">
        <section class="add-modal">
          <div class="add-modal-head">
            <h2>계좌 추가</h2>
            <button type="button" @click="showAddModal = false">×</button>
          </div>
          <p>프로필에 등록된 계좌 중에서 선택해요.</p>

          <div v-if="availableAccounts.length" class="asset-account-list">
            <button
              v-for="account in availableAccounts"
              :key="account.accountId"
              type="button"
              class="asset-account-option"
              :class="{ selected: selectedOptionAccountId === account.accountId }"
              @click="selectedOptionAccountId = account.accountId"
            >
              <span class="asset-logo">{{ account.logo }}</span>
              <div class="asset-account-info">
                <h3>{{ account.name }}</h3>
                <p>{{ account.number }}</p>
              </div>
              <strong>{{ money(account.balance) }}</strong>
              <span class="radio" />
            </button>
          </div>
          <p v-else class="empty-hint">추가할 수 있는 계좌가 없어요. <br> 등록된 계좌가 모두 연결돼 있어요.</p>

          <button type="button" class="modal-save" :disabled="!selectedOptionAccountId" @click="confirmAddAccount">추가하기</button>
        </section>
      </div>
    </Transition>

    <Transition name="fade">
      <div v-if="showDeleteConfirm" class="confirm-backdrop" @click.self="showDeleteConfirm = false">
        <section class="confirm-modal">
          <h2>계좌 연결을 해제할까요?</h2>
          <p>{{ pendingDeleteAccount?.name }}이(가) 연결계좌 목록에서 삭제돼요.</p>
          <div class="confirm-actions">
            <button type="button" @click="showDeleteConfirm = false">취소</button>
            <button type="button" class="danger" @click="confirmRemoveAccount">삭제하기</button>
          </div>
        </section>
      </div>
    </Transition>

    <BottomNav />
  </main>
</template>

<style scoped>
.accounts-page{max-width:430px;min-height:100vh;margin:0 auto;padding:44px 16px 96px;background:#eef2f8;color:#111827}.page-header{display:flex;align-items:center;gap:12px}.page-header button{display:grid;width:36px;height:36px;place-items:center;color:#0f172a}.page-header p{color:#1f5ab9;font-size:11px;font-weight:800;letter-spacing:.04em}.page-header h1{margin-top:2px;font-size:26px;font-weight:800;letter-spacing:-.04em}.summary-card{margin-top:24px;padding:22px;border-radius:24px;background:#14357f;color:#fff;box-shadow:0 14px 28px rgba(24,51,99,.18)}.summary-card>span{display:block;color:#c8d6f4;font-size:13px;font-weight:600}.summary-card strong{display:block;margin-top:8px;font-size:32px;font-weight:800;letter-spacing:-.05em}.account-card{margin-top:16px;padding:20px;border-radius:24px;background:#fff;box-shadow:0 10px 24px rgba(18,43,82,.07)}.section-head{display:flex;align-items:center;justify-content:space-between}.section-head h2{font-size:20px;font-weight:800;letter-spacing:-.04em}.section-head span{color:#64748b;font-size:12px;font-weight:700}.account-list{display:grid;gap:10px;margin-top:16px}.account-row{display:grid;grid-template-columns:38px minmax(0,1fr) auto;gap:11px;align-items:center;padding:13px;border:1px solid #edf2f8;border-radius:17px;background:#f8fbff}.account-row.primary{border:2px solid #2f70e9;background:#f3f8ff}.logo{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;background:#fff;font-size:16px;font-weight:800;color:#9a6500;box-shadow:0 0 0 1px #e4ebf5}.account-info{min-width:0}.account-info h3{font-size:16px;font-weight:800}.account-info p{overflow:hidden;margin-top:5px;color:#94a3b8;font-size:11px;font-weight:600;text-overflow:ellipsis;white-space:nowrap}.amount-info{text-align:right}.amount-info strong{display:block;font-size:17px;font-weight:800}.row-actions{display:flex;align-items:center;justify-content:flex-end;gap:8px;margin-top:6px}.amount-info button{color:#9aa8bd;font-size:11px;font-weight:600}.set-primary{padding:5px 10px;border:1px solid #dbe4f2;border-radius:99px;background:#fff;color:#2f70e9;font-weight:700}.primary-badge{padding:5px 10px;border-radius:99px;background:#eaf2ff;color:#2f70e9;font-size:11px;font-weight:800}.list-empty{margin-top:16px;padding:20px;border-radius:14px;background:#f7f9fd;color:#9aa8bd;font-size:12px;font-weight:600;text-align:center}.add-account{display:block;width:100%;margin-top:16px;padding:16px;border-radius:17px;background:#fff;color:#2f70e9;font-size:15px;font-weight:800;box-shadow:0 10px 24px rgba(18,43,82,.07)}@media(max-width:380px){.account-row{grid-template-columns:34px minmax(0,1fr)}.amount-info{grid-column:2;text-align:left}.summary-card strong{font-size:28px}}
.add-modal-backdrop{position:fixed;z-index:130;inset:0;display:flex;align-items:center;justify-content:center;padding:20px;background:rgba(23,35,60,.42)}.add-modal{width:min(100%,380px);max-height:min(560px,86vh);display:flex;flex-direction:column;padding:20px;border-radius:22px;background:#fff;box-shadow:0 18px 44px rgba(17,24,39,.22)}.add-modal-head{display:flex;align-items:center;justify-content:space-between}.add-modal-head h2{font-size:19px;font-weight:800}.add-modal-head button{width:30px;height:30px;border-radius:50%;background:#f1f4f9;color:#64748b;font-size:19px}.add-modal>p{margin-top:6px;color:#8290a6;font-size:12px;font-weight:600}.asset-account-list{display:grid;flex:1;gap:10px;margin-top:16px;overflow-y:auto}.asset-account-option{position:relative;display:grid;grid-template-columns:38px minmax(0,1fr) auto 24px;align-items:center;gap:11px;padding:13px;border:1.5px solid #edf2f8;border-radius:17px;background:#f8fbff;text-align:left}.asset-account-option.selected{border-color:#2f70e9;background:#f3f8ff}.asset-logo{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;font-size:14px;font-weight:800}.asset-account-info{min-width:0}.asset-account-info h3{font-size:15px;font-weight:800}.asset-account-info p{margin-top:4px;overflow:hidden;color:#94a3b8;font-size:11px;font-weight:600;text-overflow:ellipsis;white-space:nowrap}.asset-account-option>strong{font-size:14px;font-weight:800;white-space:nowrap}.asset-account-option .radio{width:20px;height:20px;border:2px solid #c7d3e4;border-radius:50%}.asset-account-option.selected .radio{border:6px solid #2f70e9}.empty-hint{margin-top:20px;padding:20px;border-radius:16px;background:#f7f9fd;color:#8290a6;font-size:13px;font-weight:600;text-align:center;line-height:1.5}.modal-save{flex:none;width:100%;height:50px;margin-top:18px;border-radius:14px;background:#2f70e9;color:#fff;font-size:15px;font-weight:800}.modal-save:disabled{background:#c7d3e4;color:#fff}.notice-text{margin-top:14px;padding:12px 14px;border-radius:14px;background:#fff5df;color:#a36c07;font-size:12px;font-weight:700}.confirm-backdrop{position:fixed;z-index:130;inset:0;display:flex;align-items:center;justify-content:center;padding:20px;background:rgba(23,35,60,.42)}.confirm-modal{width:min(100%,360px);padding:20px;border-radius:22px;background:#fff;box-shadow:0 18px 44px rgba(17,24,39,.22);text-align:center}.confirm-modal h2{font-size:20px;font-weight:800;letter-spacing:-.04em}.confirm-modal p{margin-top:10px;color:#64748b;font-size:13px;font-weight:500;line-height:1.5}.confirm-actions{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:22px}.confirm-actions button{height:48px;border-radius:14px;background:#f1f5f9;color:#64748b;font-size:15px;font-weight:800}.confirm-actions .danger{background:#ffe8e8;color:#ef4444}.fade-enter-active,.fade-leave-active{transition:opacity .2s}.fade-enter-from,.fade-leave-to{opacity:0}
.accounts-page{word-break:keep-all}.accounts-page h1,.accounts-page h2,.accounts-page h3{text-wrap:balance}.accounts-page p{text-wrap:pretty}
</style>
