<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useReceiptStore } from '@/stores/receipt'

const route = useRoute(); const router = useRouter(); const store = useReceiptStore()
const tripId = computed(() => Number(route.query.tripId || 1))
const existing = route.params.receiptId ? store.get(route.params.receiptId) : null
const base = existing || store.draft || { tripId:tripId.value, country:'프랑스', flag:'🇫🇷', merchant:'RISTORANTE PIZZERIA da RITA', date:'2025-07-12', time:'19:42', currency:'EUR', amount:17, wonAmount:25300, category:'식비', confidence:96, memo:'', items:[['COPERTO','테이블 요금',2],['PIZZA DIAVOLA','디아볼라 피자',8.5],['ACQUA MINERALE','생수',2.5],['BIRRA PERONI','페로니 맥주',4]] }
const form = reactive(JSON.parse(JSON.stringify(base)))
const editing = ref(false); const translated = ref(true); const showOriginal = ref(false)
const save = () => {
  store.save({ ...form, tripId:tripId.value, amount:Number(form.amount), wonAmount:Math.round(Number(form.amount)*1488) })
  store.draft = null
  editing.value = false
  router.push(`/receipt?tripId=${tripId.value}`)
}
const discard = () => { store.draft = null; router.push(`/receipt?tripId=${tripId.value}`) }
const remove = () => { if (window.confirm('이 영수증을 보관함에서 삭제할까요?')) { store.remove(existing.id); router.push(`/receipt?tripId=${tripId.value}`) } }
</script>

<template><main class="result-page"><header><button @click="router.back()">‹</button><h1>영수증 인식 결과</h1><button @click="editing=!editing">{{ editing?'취소':'수정' }}</button></header>
  <div class="toggle"><button :class="{active:!translated}" @click="translated=false">원문</button><button :class="{active:translated}" @click="translated=true">번역</button></div>
  <section class="receipt-paper"><input v-if="editing" v-model="form.merchant"><h2 v-else>{{ form.merchant }}</h2><small>{{ form.country }} · {{ form.date }} · {{ form.time || '시간 미확인' }}</small><div class="dash"/>
    <div v-for="(item,index) in form.items" :key="index" class="item"><span><input v-if="editing" v-model="item[translated?1:0]"><b v-else>{{ item[translated?1:0] }}</b><small v-if="translated">{{ item[0] }}</small></span><input v-if="editing" v-model.number="item[2]" type="number"><strong v-else>€ {{ Number(item[2]).toFixed(2) }}</strong></div>
    <div class="dash"/><div class="total"><span>TOTALE · 최종 결제 금액</span><strong>{{ form.currency }} <input v-if="editing" v-model.number="form.amount" type="number"><template v-else>{{ Number(form.amount).toFixed(2) }}</template></strong></div><p>약 {{ Number(form.wonAmount).toLocaleString() }}원</p><em>OCR 신뢰도 {{ form.confidence }}%</em>
  </section>
  <button class="original-button" type="button" @click="showOriginal=true">▧ 실제 영수증 원본 사진 보기</button>
  <section class="meta" v-if="editing"><label>카테고리<select v-model="form.category"><option>식비</option><option>숙박</option><option>교통</option><option>쇼핑</option><option>관광</option></select></label><label>메모<input v-model="form.memo"></label></section>
  <div v-if="!existing" class="actions"><button @click="discard">추가하지 않기</button><button @click="save">보관함에 추가</button></div>
  <div v-else class="actions"><button @click="remove">삭제</button><button @click="editing ? save() : editing=true">{{ editing?'수정 저장':'인식 결과 수정' }}</button></div>
  <div v-if="showOriginal" class="original-modal" @click.self="showOriginal=false"><section><header><b>실제 영수증 원본</b><button @click="showOriginal=false">×</button></header><div class="raw-paper"><b>RISTORANTE PIZZERIA<br>da RITA</b><small>FIRENZE · ITALIA</small><i v-for="n in 8" :key="n"/><strong>TOTALE&nbsp;&nbsp;€ {{ Number(form.amount).toFixed(2) }}</strong></div></section></div>
</main></template>

<style scoped>
.result-page{min-height:100vh;padding:0 18px 50px;background:#f8f6f1;color:#111a2d}.result-page>header{display:grid;height:88px;grid-template-columns:50px 1fr 50px;align-items:end;padding-bottom:16px}.result-page>header h1{text-align:center;font-size:18px;font-weight:900}.result-page>header button:first-child{font-size:29px;text-align:left}.result-page>header button:last-child{text-align:right;color:#2670dd;font-size:11px}.toggle{display:flex;justify-content:center;margin-bottom:14px}.toggle button{padding:8px 18px;border:1px solid #dce3ed;color:#8491a3;font-size:10px}.toggle button:first-child{border-radius:10px 0 0 10px}.toggle button:last-child{border-radius:0 10px 10px 0}.toggle .active{border-color:#246dd7;background:#246dd7;color:#fff}.receipt-paper{position:relative;margin:0 auto;padding:26px 22px;border-radius:3px;background:#fffdf6;box-shadow:0 7px 24px #263a5e18;text-align:center}.receipt-paper:after{position:absolute;right:0;bottom:-7px;left:0;height:14px;background:linear-gradient(135deg,transparent 7px,#fffdf6 0) 0 0/14px 14px repeat-x;content:''}.receipt-paper h2{font-size:13px}.receipt-paper>input{width:100%;padding:9px;border:1px solid #cfdbea;text-align:center}.receipt-paper>small{display:block;margin-top:6px;color:#8793a4;font-size:8px}.dash{margin:20px 0;border-top:1px dashed #bfc5cc}.item{display:grid;grid-template-columns:1fr auto;align-items:center;margin-top:17px;text-align:left}.item span b,.item span small{display:block}.item b{font-size:10px}.item small{margin-top:4px;color:#8995a6;font-size:7px}.item strong{font-size:9px}.item input{max-width:145px;padding:6px;border:1px solid #d8e1ec}.item>input{width:62px}.total{display:flex;align-items:center;justify-content:space-between}.total span{font-size:8px}.total strong{color:#e67a22;font-size:15px}.total input{width:70px;padding:5px;border:1px solid #d8e1ec}.receipt-paper>p{margin-top:6px;text-align:right;color:#8290a3;font-size:8px}.receipt-paper>em{display:block;margin-top:24px;color:#18a879;font-size:8px;font-style:normal}.original-button{width:100%;margin-top:20px;padding:13px;border:1px solid #cfdbea;border-radius:12px;background:#fff;color:#235fac;font-size:10px;font-weight:900}.meta{display:grid;gap:10px;margin-top:14px;padding:16px;border-radius:16px;background:#fff}.meta label{display:grid;grid-template-columns:70px 1fr;align-items:center;font-size:10px;font-weight:800}.meta input,.meta select{padding:10px;border:1px solid #dae2ec;border-radius:9px}.actions{display:grid;grid-template-columns:1fr 2fr;gap:10px;margin-top:18px}.actions button{height:52px;border:1px solid #dce3ed;border-radius:13px;color:#e5484d;font-size:12px;font-weight:900}.actions button:last-child{border:0;background:#19489c;color:#fff}.original-modal{position:fixed;z-index:100;display:grid;inset:0;place-items:center;padding:25px;background:#09172cbb}.original-modal>section{width:100%;max-width:340px;padding:16px;border-radius:18px;background:#f8f6f1}.original-modal header{display:flex;align-items:center;justify-content:space-between}.original-modal header b{font-size:14px}.original-modal header button{font-size:24px}.raw-paper{display:flex;width:230px;min-height:390px;flex-direction:column;margin:18px auto 8px;padding:30px 24px;background:#fff8e8;text-align:center}.raw-paper>b{font-size:10px}.raw-paper>small{margin-top:7px;font-size:7px}.raw-paper i{height:10px;margin-top:12px;border-bottom:1px solid #c8c2b4}.raw-paper strong{margin-top:auto;font-size:10px}
</style>
