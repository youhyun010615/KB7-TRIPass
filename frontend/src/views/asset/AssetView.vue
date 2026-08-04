<script setup>
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import AssetTicket from '@/components/asset/AssetTicket.vue'
import { useAssetStore } from '@/stores/asset'

const router = useRouter()
const asset = useAssetStore()
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`

const menus = [
  { icon: '◎', title: '여행 목표 자금 관리', desc: '목표 설정 및 현황', path: '/savings' },
  { icon: '▤', title: '고정지출 관리', desc: '월 고정비 등록', path: '/asset/fixed-expenses' },
  { icon: '✈', title: '사전 지출 금액 관리', desc: '여행 전 지출 확인', path: '/asset/prepaid' },
  { icon: '◎', title: '이달의 자금 체크 바로가기', desc: '이달의 자금 체크', path: '/savings/monthly' },
  { icon: '□', title: '다가오는 금융일정', desc: '다가오는 월급과 고정지출 확인', path: '/financial-schedule' },
]
</script>

<template>
  <main class="asset-page">
    <div class="shell">
      <h1>자산관리</h1>
      <AssetTicket label="전체 보유금액" :amount="asset.totalAssets" :caption="`${asset.accounts.length}개 계좌 연동　·　출국까지 D-186`" action="거래내역 상세보기" @action="router.push('/asset/transactions')" />

      <div class="section-title"><h2>연동 계좌</h2><button>＋ 계좌 추가</button></div>
      <section class="accounts">
        <button v-for="account in asset.accounts" :key="account.id" :class="{ primary: account.primary }" @click="router.push(`/asset/accounts/${account.id}`)">
          <span class="bank" :style="{ background: account.tone, color: account.accent }">{{ account.symbol }}</span>
          <span><b>{{ account.name }}</b><small>{{ account.type }} · {{ account.number }}</small></span>
          <strong>{{ money(account.balance) }}</strong>
        </button>
      </section>

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
.asset-page{min-height:100vh;padding-bottom:80px;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:52px 18px 28px;background:#f4f6fc}.shell>h1{text-align:center;font-size:18px;font-weight:900;margin-bottom:18px}.section-title{display:flex;align-items:center;justify-content:space-between;margin:18px 2px 9px}.section-title h2,.menu-title{font-size:14px;font-weight:900}.section-title button{color:#a56b00;font-size:10px;font-weight:900}.accounts button{display:grid;grid-template-columns:40px 1fr auto;align-items:center;gap:10px;width:100%;margin-bottom:10px;padding:12px;border:1px solid #e2e7ef;border-radius:14px;background:white;text-align:left}.accounts button.primary{border:1.5px solid #ffb800}.bank{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;font-size:12px;font-weight:900}.accounts b,.accounts small{display:block}.accounts b{font-size:12px}.accounts small{margin-top:4px;color:#94a3b8;font-size:8px}.accounts strong{font-size:12px;white-space:nowrap}.menu-title{margin:19px 2px 10px}.menus button{display:grid;grid-template-columns:38px 1fr 18px;align-items:center;width:100%;margin-bottom:9px;padding:12px;border:1px solid #e2e7ef;border-radius:14px;background:white;text-align:left}.menus>button>span{display:grid;width:34px;height:34px;place-items:center;border-radius:10px;background:#fff5d8;color:#a56b00}.menus b,.menus small{display:block}.menus b{font-size:12px}.menus small{margin-top:4px;color:#94a3b8;font-size:8px}.menus i{text-align:right;color:#94a3b8;font-size:22px}
</style>
