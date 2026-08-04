<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import AssetTicket from '@/components/asset/AssetTicket.vue'
import { useAssetStore } from '@/stores/asset'
import { useMonthlyFundStore } from '@/stores/monthlyFund'

const router = useRouter(); const asset = useAssetStore(); const fund = useMonthlyFundStore()
const meta = { COMMON:{name:'공통 항목',flag:'🌍',desc:'모든 국가에 공통으로 적용되는 항목'}, CH:{name:'스위스',flag:'🇨🇭',desc:'스위스 여행 관련 사전 지출 항목'}, FR:{name:'프랑스',flag:'🇫🇷',desc:'프랑스 여행 관련 사전 지출 항목'}, DE:{name:'독일',flag:'🇩🇪',desc:'독일 여행 관련 사전 지출 항목'}, JP:{name:'일본',flag:'🇯🇵',desc:'일본 여행 관련 사전 지출 항목'}, VN:{name:'베트남',flag:'🇻🇳',desc:'베트남 여행 관련 사전 지출 항목'} }
const allItems = computed(() => [...asset.prepaidExpenses, ...fund.prepaidExpenses.map((item) => ({...item,scope:item.countryCode,icon:'▦'}))])
const groups = computed(() => Object.entries(meta).map(([code,info]) => { const items=allItems.value.filter((item)=>item.scope===code); return {code,...info,items,total:items.reduce((sum,item)=>sum+Number(item.amount||0),0)} }).filter((group)=>group.items.length))
const total = computed(() => allItems.value.reduce((sum,item)=>sum+Number(item.amount||0),0))
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
</script>

<template><main class="page"><div class="shell">
  <header><button @click="router.back()">‹</button><h1>사전 지출 금액</h1></header>
  <AssetTicket label="총 사전 지출 금액" :amount="total" :caption="`${allItems.length}개 항목 · 여행 전 결제 완료`" />
  <section v-for="group in groups" :key="group.code" class="country">
    <div class="title"><div><h2>{{ group.flag }} {{ group.name }}</h2><p>{{ group.desc }}</p></div><strong>{{ money(group.total) }}</strong></div>
    <div class="items"><article v-for="item in group.items" :key="`${group.code}-${item.id}`"><span>{{ item.icon || '▦' }}</span><div><b>{{ item.name }}</b><small>{{ item.date }}</small></div><strong>{{ money(item.amount) }}</strong></article></div>
  </section>
  <button class="add" @click="router.push('/savings/monthly/prepaid/new')">＋ 사전 지출 등록하기</button>
  <BottomNav />
</div></main></template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{position:relative;width:min(100%,390px);min-height:100vh;margin:auto;padding:52px 18px 100px;background:#f7f5ef}header{display:flex;align-items:center;margin-bottom:18px}header button{font-size:25px}h1{flex:1;text-align:center;font-size:18px;font-weight:900;padding-right:20px}.country{margin-top:14px;padding:15px;border:1px solid #e1e6ed;border-radius:16px;background:#fff;box-shadow:0 5px 14px #1a37670a}.title{display:flex;justify-content:space-between;align-items:start;padding-bottom:12px;border-bottom:1px solid #edf0f4}.title h2{font-size:13px}.title p{margin-top:4px;color:#9aa5b5;font-size:8px}.title strong{color:#1670e8;font-size:11px}.items article{display:grid;grid-template-columns:36px 1fr auto;align-items:center;gap:9px;padding:11px 0;border-bottom:1px solid #edf0f4}.items article:last-child{border:0}.items article>span{display:grid;width:32px;height:32px;place-items:center;border-radius:10px;background:#eef4ff}.items b,.items small{display:block}.items b{font-size:11px}.items small{margin-top:4px;color:#9aa5b5;font-size:8px}.items strong{font-size:10px}.add{width:100%;margin-top:14px;padding:14px;border-radius:12px;background:#173f8d;color:#fff;font-size:12px;font-weight:900}
</style>
