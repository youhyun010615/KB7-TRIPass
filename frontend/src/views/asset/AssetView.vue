<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import AssetTicket from '@/components/asset/AssetTicket.vue'
import { useAssetStore } from '@/stores/asset'
import api from '@/api'

const router = useRouter()
const asset = useAssetStore()
const editingAccounts = ref(false)
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`

const realAccounts = ref([])

const ACCOUNT_TYPE_LABEL = { CHECKING: '입출금', DEPOSIT: '예금', SAVING: '적금' }

onMounted(async () => {
  try {
    const res = await api.get('/accounts')
    realAccounts.value = res.data.data ?? []
  } catch (e) {
    console.error('계좌 목록 조회 실패', e)
  }
})

const menus = [
  { icon: '◎', title: '여행 목표 자금 관리', desc: '목표 설정 및 현황', path: '/savings' },
  { icon: '▤', title: '고정지출 관리', desc: '월 고정비 등록', path: '/asset/fixed-expenses' },
  { icon: '✈', title: '사전 지출 금액 관리', desc: '여행 전 지출 확인', path: '/asset/prepaid' },
  { icon: '◎', title: '이달의 자금 체크', desc: '이번 달 자금 현황', path: '/savings/monthly' },
  { icon: '□', title: '다가오는 금융 일정', desc: '월급과 고정지출 일정 확인', path: '/financial-schedule' },
]

function removeAccount(account) {
  if (!window.confirm(`${account.name} 계좌 연결을 삭제할까요?`)) return
  asset.removeAccount(account.id)
}
</script>

<template>
  <main class="asset-page">
    <div class="shell">
      <h1>자산관리</h1>
      <AssetTicket label="전체 보유금액" :amount="asset.totalAssets" :caption="`${asset.accounts.length}개 계좌 연동　·　출국까지 D-186`" action="거래내역 상세보기" @action="router.push('/asset/transactions')" />

      <div class="section-title"><h2>연동 계좌</h2><div><button type="button" @click="editingAccounts = !editingAccounts">{{ editingAccounts ? '완료' : '삭제하기' }}</button><button type="button" @click="router.push({ path: '/profile/financial', query: { step: 2, from: 'asset' } })">＋ 계좌 추가</button></div></div>
      <section class="accounts">
        <article v-for="account in asset.accounts" :key="account.id" :class="{ primary: account.primary }">
          <button v-if="editingAccounts" class="delete-account" type="button" :aria-label="`${account.name} 계좌 삭제`" @click="removeAccount(account)">−</button>
          <button class="account-main" type="button" :class="{ editing: editingAccounts }" :disabled="editingAccounts" @click="router.push(`/asset/accounts/${account.id}`)">
            <span class="bank" :style="{ background: account.tone, color: account.accent }">{{ account.symbol }}</span>
            <span><b>{{ account.name }}</b><small>{{ account.type }} · {{ account.number }}</small></span>
            <strong>{{ money(account.balance) }}</strong>
          </button>
        </article>
        <p v-if="!asset.accounts.length" class="empty-account">연동된 계좌가 없어요.<br>계좌를 추가해 자산을 한눈에 확인해 보세요.</p>
      </section>

      <template v-if="realAccounts.length > 0">
        <div class="section-title" style="margin-top:18px"><h2>연동된 실제 계좌</h2></div>
        <section class="accounts">
          <article v-for="acc in realAccounts" :key="acc.id">
            <button class="account-main" type="button" @click="router.push({ path: `/asset/accounts/${acc.id}`, query: { isReal: 'true', name: acc.accountName, number: acc.accountNumber, type: acc.accountType } })">
              <span class="bank" style="background:#e8f0fe;color:#1a56db">{{ acc.accountName?.charAt(0) ?? '계' }}</span>
              <span><b>{{ acc.accountName }}</b><small>{{ ACCOUNT_TYPE_LABEL[acc.accountType] ?? acc.accountType }} · {{ acc.accountNumber }}</small></span>
              <strong>{{ money(acc.balance) }}</strong>
            </button>
          </article>
        </section>
      </template>

      <h2 class="menu-title">자산관리 메뉴</h2>
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
.asset-page{min-height:100vh;padding-bottom:80px;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:52px 18px 28px;background:#f4f6fc}.shell>h1{text-align:center;font-size:18px;font-weight:900;margin-bottom:18px}.section-title{display:flex;align-items:center;justify-content:space-between;margin:18px 2px 9px}.section-title h2,.menu-title{font-size:14px;font-weight:900}.section-title>div{display:flex;gap:10px}.section-title button{color:#a56b00;font-size:10px;font-weight:900}.accounts article{position:relative;margin-bottom:10px;border:1px solid #e2e7ef;border-radius:14px;background:white;overflow:hidden}.accounts article.primary{border:1.5px solid #ffb800}.account-main{display:grid;grid-template-columns:40px 1fr auto;align-items:center;gap:10px;width:100%;padding:12px;text-align:left}.account-main.editing{padding-left:48px;opacity:.78}.delete-account{position:absolute;left:12px;top:50%;z-index:2;width:25px;height:25px;transform:translateY(-50%);border-radius:50%;background:#e8484f;color:#fff;font-size:20px;font-weight:900;line-height:1}.bank{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;font-size:12px;font-weight:900}.accounts b,.accounts small{display:block}.accounts b{font-size:12px}.accounts small{margin-top:4px;color:#94a3b8;font-size:8px}.accounts strong{font-size:12px;white-space:nowrap}.empty-account{padding:22px;border:1px dashed #cbd5e1;border-radius:14px;background:#fff;color:#94a3b8;text-align:center;font-size:10px;line-height:1.7}.menu-title{margin:19px 2px 10px}.menus{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:9px}.menus button{display:flex;min-height:112px;padding:12px;border:1px solid #e2e7ef;border-radius:14px;background:white;text-align:left;flex-direction:column;align-items:flex-start}.menus>button>span{display:grid;width:34px;height:34px;place-items:center;border-radius:10px;background:#fff5d8;color:#a56b00}.menus div{margin-top:10px}.menus b,.menus small{display:block}.menus b{font-size:11px;line-height:1.35}.menus small{margin-top:4px;color:#94a3b8;font-size:8px}.menus i{display:none}
</style>
