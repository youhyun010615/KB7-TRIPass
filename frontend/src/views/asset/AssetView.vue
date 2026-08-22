<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import AssetTicket from '@/components/asset/AssetTicket.vue'
import { useAssetStore, bankPresentationByName, bankPresentationByCode } from '@/stores/asset'
import api from '@/api'

const router = useRouter()
const asset = useAssetStore()
const editingAccounts = ref(false)
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`

const realAccounts = ref([])
const selectedId = ref(null)

const ACCOUNT_TYPE_LABEL = { CHECKING: '입출금', DEPOSIT: '예금', SAVING: '적금' }

// 은행마다 고유 색상 + 하단 은행명 뱃지를 보여주기 위해 계좌 데이터에 브랜드 정보를 붙인다.
const mockAccountCards = computed(() =>
  asset.accounts.map((account) => ({
    ...account,
    bankName: account.bank,
    ...bankPresentationByName(account.bank),
  })),
)
const realAccountCards = computed(() =>
  realAccounts.value.map((acc) => ({
    ...acc,
    bankName: bankPresentationByCode(acc.organizationCode).name,
    ...bankPresentationByCode(acc.organizationCode),
  })),
)

const totalBalance = computed(() =>
  asset.totalAssets + realAccounts.value.reduce((sum, acc) => sum + Number(acc.balance ?? 0), 0)
)
const totalAccountCount = computed(() => asset.accounts.length + realAccounts.value.length)

onMounted(async () => {
  try {
    const res = await api.get('/accounts')
    realAccounts.value = res.data.data ?? []
  } catch (e) {
    console.error('계좌 목록 조회 실패', e)
  }
})

const menus = [
  { icon: '◔', title: '소비 현황 분석', desc: '카테고리별 지출 보기', path: '/savings/monthly' },
  { icon: '▦', title: '전체 거래내역', desc: '연동 계좌 내역 보기', path: '/asset/transactions' },
]

function removeAccount(account) {
  if (!window.confirm(`${account.name} 계좌 연결을 삭제할까요?`)) return
  asset.removeAccount(account.id)
}

async function removeRealAccount(acc) {
  if (!window.confirm(`${acc.accountName} 계좌 연결을 해제할까요?`)) return
  try {
    await api.delete(`/accounts/${acc.id}`)
    realAccounts.value = realAccounts.value.filter(a => a.id !== acc.id)
  } catch (e) {
    alert('계좌 연결 해제에 실패했어요.')
  }
}
</script>

<template>
  <main class="asset-page">
    <div class="shell">
      <h1>자산관리</h1>
      <AssetTicket label="전체 보유금액" :amount="totalBalance" :caption="`${totalAccountCount}개 계좌 연동　·　출국까지 D-186`" action="거래내역 상세보기" @action="router.push('/asset/transactions')" />

      <div class="section-title"><h2>연동 계좌</h2><div><button type="button" @click="editingAccounts = !editingAccounts">{{ editingAccounts ? '완료' : '삭제하기' }}</button><button type="button" @click="router.push({ path: '/profile/financial', query: { step: 2, from: 'asset' } })">＋ 계좌 추가</button></div></div>
      <section class="accounts">
        <article v-for="account in mockAccountCards" :key="account.id" :class="{ primary: selectedId === account.id }" :style="{ '--bank-color': account.color }">
          <button v-if="editingAccounts" class="delete-account" type="button" :aria-label="`${account.name} 계좌 삭제`" @click="removeAccount(account)">−</button>
          <button class="account-main" type="button" :class="{ editing: editingAccounts }" :disabled="editingAccounts" @click="selectedId = account.id; router.push(`/asset/accounts/${account.id}`)">
            <span class="bank" :style="{ background: account.color, color: account.text }">{{ account.symbol }}</span>
            <span><b>{{ account.name }}</b><small>{{ account.type }} · {{ account.number }}</small></span>
            <strong>{{ money(account.balance) }}</strong>
          </button>
          <div class="bank-footer" :style="{ background: `color-mix(in srgb, ${account.color} 12%, white)`, color: account.color }">
            <i :style="{ background: account.color }"></i>{{ account.bankName }}
          </div>
        </article>
        <p v-if="!asset.accounts.length" class="empty-account">연동된 계좌가 없어요.<br>계좌를 추가해 자산을 한눈에 확인해 보세요.</p>
      </section>

      <template v-if="realAccountCards.length > 0">
        <div class="section-title" style="margin-top:18px"><h2>연동된 실제 계좌</h2></div>
        <section class="accounts">
          <article v-for="acc in realAccountCards" :key="acc.id" :class="{ primary: selectedId === acc.id }" :style="{ '--bank-color': acc.color }">
            <button v-if="editingAccounts" class="delete-account" type="button" @click="removeRealAccount(acc)">−</button>
            <button class="account-main" type="button" :class="{ editing: editingAccounts }" :disabled="editingAccounts" @click="selectedId = acc.id; router.push({ path: `/asset/accounts/${acc.id}`, query: { isReal: 'true', name: acc.accountName, number: acc.accountNumber, type: acc.accountType } })">
              <span class="bank" :style="{ background: acc.color, color: acc.text }">{{ acc.symbol }}</span>
              <span><b>{{ acc.accountName }}</b><small>{{ ACCOUNT_TYPE_LABEL[acc.accountType] ?? acc.accountType }} · {{ acc.accountNumber }}</small></span>
              <strong>{{ money(acc.balance) }}</strong>
            </button>
            <div class="bank-footer" :style="{ background: `color-mix(in srgb, ${acc.color} 12%, white)`, color: acc.color }">
              <i :style="{ background: acc.color }"></i>{{ acc.bankName }}
            </div>
          </article>
        </section>
      </template>

      <h2 class="menu-title">소비 분석</h2>
      <section class="menus">
        <button v-for="menu in menus" :key="menu.title" @click="router.push(menu.path)">
          <span>{{ menu.icon }}</span><div><b>{{ menu.title }}</b><small>{{ menu.desc }}</small></div><i>›</i>
        </button>
      </section>
    </div>
    <BottomNav />
  </main>
</template>

<style scoped>
.asset-page{min-height:100vh;padding-bottom:80px;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:14px 18px 28px;background:#f4f6fc}.shell>h1{text-align:center;font-size:18px;font-weight:900;margin-bottom:18px}.section-title{display:flex;align-items:center;justify-content:space-between;margin:18px 2px 9px}.section-title h2,.menu-title{font-size:14px;font-weight:900}.section-title>div{display:flex;gap:10px}.section-title button{color:#a56b00;font-size:10px;font-weight:900}.accounts article{position:relative;margin-bottom:10px;border:1px solid #e2e7ef;border-top:3px solid var(--bank-color,#e2e7ef);border-radius:14px;background:white;overflow:hidden}.accounts article.primary{border:1.5px solid #ffb800;border-top:3px solid var(--bank-color,#ffb800)}.account-main{display:grid;grid-template-columns:40px 1fr auto;align-items:center;gap:10px;width:100%;padding:12px;text-align:left}.account-main.editing{padding-left:48px;opacity:.78}.delete-account{position:absolute;left:12px;top:50%;z-index:2;width:25px;height:25px;transform:translateY(-50%);border-radius:50%;background:#e8484f;color:#fff;font-size:20px;font-weight:900;line-height:1}.bank{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;font-size:10.5px;font-weight:900}.accounts b,.accounts small{display:block}.accounts b{font-size:12px}.accounts small{margin-top:4px;color:#94a3b8;font-size:8px}.accounts strong{font-size:12px;white-space:nowrap}.bank-footer{display:flex;align-items:center;gap:6px;padding:7px 12px;border-top:1px solid #eef1f6;font-size:9.5px;font-weight:800}.bank-footer i{display:block;width:6px;height:6px;border-radius:50%}.empty-account{padding:22px;border:1px dashed #cbd5e1;border-radius:14px;background:#fff;color:#94a3b8;text-align:center;font-size:10px;line-height:1.7}.menu-title{margin:19px 2px 10px}.menus{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:9px}.menus button{display:flex;min-height:112px;padding:12px;border:1px solid #e2e7ef;border-radius:14px;background:white;text-align:left;flex-direction:column;align-items:flex-start}.menus>button>span{display:grid;width:34px;height:34px;place-items:center;border-radius:10px;background:#fff5d8;color:#a56b00}.menus div{margin-top:10px}.menus b,.menus small{display:block}.menus b{font-size:11px;line-height:1.35}.menus small{margin-top:4px;color:#94a3b8;font-size:8px}.menus i{display:none}
</style>
