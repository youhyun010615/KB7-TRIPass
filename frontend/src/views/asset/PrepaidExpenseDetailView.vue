<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PREPAID_SCOPE_META, useAssetStore } from '@/stores/asset'

const route = useRoute()
const router = useRouter()
const asset = useAssetStore()
const item = computed(() => asset.getPrepaidExpense(route.params.prepaidExpenseId))
const scope = computed(() => PREPAID_SCOPE_META[item.value?.scope] || PREPAID_SCOPE_META.COMMON)
const typeMeta = computed(() => {
  const name = item.value?.name || ''
  if (/항공|비행/.test(name)) return { type:'항공권', icon:'✈️', tone:'#eaf2ff', color:'#3475f4' }
  if (/보험/.test(name)) return { type:'여행자 보험', icon:'◇', tone:'#e7fbf6', color:'#12ad92' }
  if (/호텔|숙박/.test(name)) return { type:'숙박', icon:'▦', tone:'#eef4ff', color:'#3475f4' }
  if (/투어|액티비티/.test(name)) return { type:'투어·액티비티', icon:'●', tone:'#f3edff', color:'#7547d8' }
  if (/교통|픽업/.test(name)) return { type:'교통', icon:'▣', tone:'#fff3e8', color:'#eb7b33' }
  return { type:'기타', icon:'◆', tone:'#edf1f7', color:'#53657d' }
})
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
</script>

<template><main class="page"><div class="shell">
  <header><button @click="router.back()">‹</button><h1>사전 지출 상세보기</h1></header>
  <template v-if="item">
    <section class="hero"><div><small>{{ scope.flag }} {{ scope.name }} · {{ typeMeta.type }}</small><h2>{{ item.name }}</h2><strong>{{ money(item.amount) }}</strong></div><span :style="{background:typeMeta.tone,color:typeMeta.color}">{{ typeMeta.icon }}</span></section>
    <section class="details"><dl><div><dt>항목 구분</dt><dd>{{ typeMeta.type }}</dd></div><div><dt>적용 범위</dt><dd>{{ scope.flag }} {{ scope.name }}</dd></div><div><dt>결제일</dt><dd>{{ item.date }}</dd></div><div><dt>결제 금액</dt><dd class="amount">{{ money(item.amount) }}</dd></div></dl></section>
    <h3>메모</h3><section class="memo">{{ item.memo || '등록된 메모가 없어요.' }}</section>
    <p class="notice">이 금액은 여행 자금 현황의 사전 지출 금액에 반영돼요.</p>
  </template>
  <section v-else class="empty">사전 지출 정보를 찾을 수 없어요.</section>
</div></main></template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:auto;padding:52px 18px 30px;background:#f4f6fc}header{display:flex;align-items:center;margin-bottom:18px}header button{width:26px;font-size:26px;text-align:left}header h1{flex:1;padding-right:26px;text-align:center;font-size:18px;font-weight:900}.hero{display:flex;align-items:center;justify-content:space-between;padding:18px;border:1px solid #dce4ef;border-radius:17px;background:#fff;box-shadow:0 4px 14px #1532650a}.hero small{color:#8995a7;font-size:9px}.hero h2{margin-top:5px;font-size:14px}.hero strong{display:block;margin-top:7px;color:#174494;font-size:24px}.hero>span{display:grid;width:50px;height:50px;place-items:center;border-radius:50%;font-size:21px;font-weight:900}.details{margin-top:13px;padding:6px 16px;border:1px solid #dce4ef;border-radius:17px;background:#fff}.details dl>div{display:grid;grid-template-columns:95px 1fr;padding:14px 0;border-bottom:1px solid #edf0f5;font-size:11px}.details dl>div:last-child{border:0}.details dt{color:#94a3b8}.details dd{text-align:right;font-weight:800}.details .amount{color:#174494;font-size:13px}h3{margin:20px 3px 9px;font-size:12px}.memo{min-height:58px;padding:15px;border:1px solid #dce4ef;border-radius:13px;background:#fff;font-size:11px}.notice{margin-top:12px;padding:11px;border-radius:10px;background:#fff7df;color:#a56b00;font-size:9px}.empty{padding:80px 0;text-align:center;color:#94a3b8}
</style>
