<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import TransactionEditModal from '@/components/asset/TransactionEditModal.vue'
import { useAssetStore } from '@/stores/asset'
import api from '@/api'
const route=useRoute(), router=useRouter(), asset=useAssetStore()
const isRealTransaction = history.state?.item?.isReal === true
const mockItem=computed(()=>isRealTransaction ? null : asset.getTransaction(route.params.transactionId))
const realItem=ref(null)
const item=computed(()=>mockItem.value ?? realItem.value ?? null)
const editMode=ref(null)
const DAYS=['일','월','화','수','목','금','토']
onMounted(async()=>{
  if(mockItem.value) return
  try {
    const res=await api.get(`/transactions/${route.params.transactionId}`)
    const t=res.data.data
    const [y,mo,d]=Array.isArray(t.transactionDate)?t.transactionDate:t.transactionDate.split('-').map(Number)
    const date=`${y}-${String(mo).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    const jsDate=new Date(y,mo-1,d)
    const [h=0,m=0]=Array.isArray(t.transactionTime)?t.transactionTime:(t.transactionTime??'00:00').split(':').map(Number)
    realItem.value={
      id:t.id,
      merchant:t.merchantName??'(내용없음)',
      amount:t.transactionType==='DEPOSIT'?Number(t.amount):-Number(t.amount),
      dateLabel:`${date.replaceAll('-','.')} (${DAYS[jsDate.getDay()]})`,
      time:`${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`,
      balanceAfter:Number(t.balanceAfter??0),
      memo:t.memo??'',
      category:t.categoryName??'기타',
      method:history.state?.item?.method??t.paymentMethodName??'',
      merchantType:t.merchantType??'',
      sourceType:t.cardId?'CARD':'ACCOUNT',
    }
  } catch(e){ console.error('거래내역 조회 실패',e) }
})
async function saveMemo(value) {
  if (realItem.value) {
    try {
      await api.patch(`/transactions/${route.params.transactionId}`, { memo: value })
      realItem.value.memo = value
    } catch(e) { console.error('메모 수정 실패', e) }
  } else {
    asset.updateTransaction(item.value.id, { memo: value })
  }
}

const categories=[
  {id:'식비',name:'식비',icon:'🍴',color:'#7547d8',description:'식사, 배달, 식료품'}, {id:'카페',name:'카페',icon:'☕',color:'#c46b19',description:'커피와 디저트'},
  {id:'생활비',name:'생활비',icon:'🛒',color:'#1c9a67',description:'마트, 편의점, 생활용품'}, {id:'쇼핑',name:'쇼핑',icon:'🛍️',color:'#d84a76',description:'의류, 화장품, 온라인 쇼핑'},
  {id:'취미·여가',name:'취미·여가',icon:'🎨',color:'#d97706',description:'영화, 공연, 운동'}, {id:'기타',name:'기타',icon:'•••',color:'#64748b',description:'그 외 지출'},
]
const isDeposit=computed(()=>Number(item.value?.amount||0)>0)
const displayAmount=computed(()=>{if(!item.value)return'';if(item.value.currency&&item.value.localAmount!=null)return`${item.value.localAmount>0?'+':'-'}${item.value.currency} ${Math.abs(item.value.localAmount).toLocaleString('ko-KR',{maximumFractionDigits:2})}`;return`${item.value.amount>0?'+':'-'}${Math.abs(item.value.amount).toLocaleString('ko-KR')}원`})
const rows=computed(()=>{
  if(!item.value)return[]
  const result=[
    {label:'거래일시',value:`${item.value.dateLabel} ${item.value.time}`},
    {label:'여행지',value:item.value.country?`${item.value.city} · ${item.value.country}`:'국내'},
    {label:'카테고리',value:item.value.category,accent:true,editable:true},
    {label:'거래구분',value:isDeposit.value?'입금':'지출'},
    {label:isDeposit.value?'입금 계좌':'결제수단',value:item.value.method},
    {label:isDeposit.value?'입금처':'사용처',value:item.value.user||item.value.merchant},
  ]
  if(item.value.merchantType) result.push({label:'가맹점 업종',value:item.value.merchantType})
  if(item.value.sourceType!=='CARD') result.push({label:'거래 후 잔액',value:`${Number(item.value.balanceAfter??0).toLocaleString('ko-KR')}원`})
  return result
})
</script>
<template><main class="detail-page"><header><button type="button" @click="router.back()">‹</button><h1>거래내역 상세보기</h1><span/></header><template v-if="item"><section class="hero"><div><small v-if="item.country">{{item.flag}} {{item.country}} 여행</small><b>{{item.merchant}}</b><strong :class="isDeposit?'deposit':'withdrawal'">{{displayAmount}}</strong><em v-if="item.currency">약 {{Math.abs(item.amount).toLocaleString('ko-KR')}}원</em></div></section><section class="info-card"><div v-for="row in rows" :key="row.label"><small>{{row.label}}</small><p><b :class="{accent:row.accent}">{{row.value}}</b><button v-if="row.editable" type="button" @click="editMode='category'">수정</button></p></div></section><div class="section-heading"><h2>메모</h2><button type="button" @click="editMode='memo'">수정</button></div><section class="memo">{{item.memo||'등록된 메모가 없어요.'}}</section><p v-if="item.country" class="trip-note">이 거래는 등록한 {{item.country}} 여행 기간에 포함된 내역이에요.</p></template><p v-else class="empty">거래내역을 찾을 수 없어요.</p><BottomNav/><TransactionEditModal :model-value="Boolean(editMode)" :mode="editMode||'category'" :categories="categories" :selected-category="item?.category" :memo="item?.memo" @update:model-value="value=>{if(!value)editMode=null}" @save-category="value=>asset.updateTransaction(item.id,{category:value})" @save-memo="saveMemo"/></main></template>
<style scoped>
.detail-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 105px;background:#f8f6f1;color:#10192d}.detail-page>header{display:grid;grid-template-columns:30px 1fr 30px;align-items:center;margin-bottom:18px}.detail-page>header button{font-size:26px;text-align:left}.detail-page>header h1{text-align:center;font-size:18px;font-weight:900}.hero{display:flex;align-items:center;justify-content:space-between;padding:18px;border:1px solid #d8e2f0;border-radius:16px;background:#fff}.hero small,.hero b,.hero strong,.hero em{display:block}.hero small{margin-bottom:10px;color:#64748b;font-size:9px}.hero b{font-size:14px}.hero strong{margin-top:8px;font-size:24px}.hero em{margin-top:5px;color:#94a3b8;font-size:9px;font-style:normal}.hero>span{display:grid;width:50px;height:50px;place-items:center;border-radius:50%;font-size:21px;font-weight:900}.deposit{color:#0758d6}.withdrawal{color:#e8484f}.info-card{margin-top:12px;padding:10px 16px;border:1px solid #dbe3ef;border-radius:17px;background:#fff}.info-card>div{display:grid;grid-template-columns:100px 1fr;padding:13px 0;border-bottom:1px solid #edf0f5}.info-card>div:last-child{border:0}.info-card small{color:#94a3b8;font-size:10px}.info-card p{display:flex;align-items:center;justify-content:flex-end;gap:8px;text-align:right}.info-card b{font-size:11px;line-height:1.4}.info-card .accent{color:#3475f4}.info-card button,.section-heading button{padding:4px 7px;border-radius:7px;background:#edf4ff;color:#286dd8;font-size:8px;font-weight:900}.section-heading{display:flex;align-items:center;justify-content:space-between;margin:20px 3px 9px}.section-heading h2{font-size:12px;font-weight:900}.memo{min-height:54px;padding:15px;border:1px solid #dbe3ef;border-radius:12px;background:#fff;font-size:11px}.trip-note{margin-top:12px;padding:13px;border-radius:12px;background:#eaf2ff;color:#5274a8;text-align:center;font-size:9px}.empty{padding:80px 0;text-align:center;color:#94a3b8}
</style>
