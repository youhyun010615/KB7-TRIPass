<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useReceiptStore } from '@/stores/receipt'

const route = useRoute()
const router = useRouter()
const store = useReceiptStore()
const tripId = computed(() => Number(route.query.tripId || 1))
const trip = computed(() => store.trip(tripId.value))
const step = ref('upload')
const fileName = ref('')
const progress = ref(0)

function selectFile(event) { fileName.value = event.target.files?.[0]?.name || ''; if (fileName.value) step.value = 'preview' }
function usePhoto() {
  step.value = 'analyzing'; progress.value = 12
  const timer = setInterval(() => {
    progress.value += 22
    if (progress.value >= 100) {
      clearInterval(timer); progress.value = 100
      store.draft = { tripId:tripId.value, country:trip.value.countries[0], flag:'🇫🇷', merchant:'RISTORANTE PIZZERIA da RITA', date:'2025.07.12', time:'19:42', currency:'EUR', amount:17, wonAmount:25300, confidence:96, memo:'', items:[['COPERTO','테이블 요금',2],['PIZZA DIAVOLA','디아볼라 피자',8.5],['ACQUA MINERALE','생수',2.5],['BIRRA PERONI','페로니 맥주',4]] }
      router.push(`/receipt/result?tripId=${tripId.value}`)
    }
  }, 280)
}
</script>

<template>
  <main :class="['capture-page', { camera:step==='camera' || step==='preview' }]">
    <header><button type="button" @click="step==='upload' ? router.back() : step='upload'">×</button><h1>{{ step==='camera' ? '영수증 촬영' : step==='preview' ? '촬영 결과 확인' : step==='analyzing' ? '영수증 분석' : '해외 영수증 등록' }}</h1><span>{{ step==='camera' ? '⚡ 자동' : '' }}</span></header>

    <template v-if="step==='upload'">
      <section class="trip-card"><small>저장할 여행</small><b>{{ trip.title }}</b><span>{{ trip.dateRange }}</span></section>
      <section class="upload-card"><div class="receipt-icon">▤</div><h2>영수증을 촬영하거나 업로드해 주세요</h2><p>해외 결제 영수증의 항목과 금액을<br>자동으로 인식하고 번역해 드려요.</p><label><input type="file" accept="image/jpeg,image/png,application/pdf" @change="selectFile"><span>파일 선택 / 영수증 촬영</span></label><small>JPG, JPEG, PNG, PDF · 최대 10MB</small></section>
      <button class="camera-button" type="button" @click="step='camera'">📷 카메라로 촬영하기</button>
    </template>

    <template v-else-if="step==='camera'">
      <section class="camera-frame"><div class="corner tl"/><div class="corner tr"/><div class="mock-receipt"><b>RISTORANTE DA RITA</b><i v-for="n in 7" :key="n"/><strong>TOTALE € 17.00</strong></div><div class="corner bl"/><div class="corner br"/></section>
      <p class="guide">영수증 전체가 프레임 안에 들어오게 해주세요.</p>
      <div class="shutter-row"><button>▧</button><button class="shutter" @click="step='preview'"/><button>A</button></div>
    </template>

    <template v-else-if="step==='preview'">
      <section class="preview"><div class="paper"><b>RISTORANTE PIZZERIA<br>da RITA</b><i v-for="n in 8" :key="n"/><strong>TOTALE&nbsp;&nbsp;€ 17.00</strong></div></section>
      <div class="preview-actions"><button @click="step='camera'">다시 촬영</button><button @click="usePhoto">이 사진 사용</button></div>
    </template>

    <template v-else>
      <section class="analysis"><div class="paper"><b>RISTORANTE DA RITA</b><i v-for="n in 9" :key="n"/><span class="scan-line" :style="{ top:`${progress}%` }"/></div><h2>영수증을 읽고 있어요</h2><p>상품명과 결제 금액을 번역하는 중이에요.</p><div class="progress"><i :style="{ width:`${progress}%` }"/></div><b>{{ progress }}%</b></section>
    </template>
  </main>
</template>

<style scoped>
.capture-page{min-height:100vh;padding:0 18px 40px;background:#f8f6f1;color:#111a2d}.capture-page>header{display:grid;height:88px;grid-template-columns:42px 1fr 50px;align-items:end;padding-bottom:16px}.capture-page>header button{font-size:25px;text-align:left}.capture-page>header h1{text-align:center;font-size:18px;font-weight:900}.capture-page>header span{text-align:right;color:#f8b400;font-size:10px}.trip-card{display:flex;flex-direction:column;padding:17px;border-radius:16px;background:#173f8c;color:#fff}.trip-card small{color:#b9cff1;font-size:8px}.trip-card b{margin-top:6px;font-size:15px}.trip-card span{margin-top:4px;color:#cfddf3;font-size:9px}.upload-card{display:flex;min-height:330px;flex-direction:column;align-items:center;justify-content:center;margin-top:14px;border:1px solid #d9e3ef;border-radius:18px;background:#fff;text-align:center}.receipt-icon{display:grid;width:62px;height:62px;place-items:center;border-radius:18px;background:#edf3ff;color:#256bd8;font-size:29px}.upload-card h2{margin-top:19px;font-size:15px}.upload-card p{margin-top:9px;color:#7d8a9c;font-size:10px;line-height:1.55}.upload-card label{margin-top:24px}.upload-card input{display:none}.upload-card label span{display:block;padding:12px 24px;border:1px dashed #2e73df;border-radius:12px;background:#f4f8ff;color:#2368d5;font-size:11px;font-weight:900}.upload-card>small{margin-top:10px;color:#a0aaba;font-size:8px}.camera-button{width:100%;height:54px;margin-top:14px;border-radius:14px;background:#19489c;color:#fff;font-size:13px;font-weight:900}.camera{background:#08172e;color:#fff}.camera-frame{position:relative;display:grid;height:510px;place-items:center;border-radius:20px;background:#0e223e}.corner{position:absolute;width:34px;height:34px;border-color:#27d9ef}.tl{top:20px;left:20px;border-top:3px solid;border-left:3px solid}.tr{top:20px;right:20px;border-top:3px solid;border-right:3px solid}.bl{bottom:20px;left:20px;border-bottom:3px solid;border-left:3px solid}.br{right:20px;bottom:20px;border-right:3px solid;border-bottom:3px solid}.mock-receipt,.paper{display:flex;width:220px;min-height:350px;flex-direction:column;padding:28px 22px;background:#fff7e7;color:#172033;text-align:center}.mock-receipt b,.paper b{font-size:10px}.mock-receipt i,.paper i{height:8px;margin-top:13px;border-bottom:1px solid #c7c2b5}.mock-receipt strong,.paper strong{margin-top:auto;font-size:10px}.guide{text-align:center;margin:17px 0;color:#b7c5da;font-size:10px}.shutter-row{display:grid;grid-template-columns:1fr 1fr 1fr;align-items:center;text-align:center}.shutter-row button{color:#fff}.shutter-row .shutter{width:68px;height:68px;justify-self:center;border:5px solid #fff;border-radius:50%;background:#1972ed;box-shadow:inset 0 0 0 4px #1972ed}.preview{display:grid;height:610px;place-items:center;border-radius:18px;background:#0f213c}.preview-actions{display:grid;grid-template-columns:1fr 1.4fr;gap:10px;margin-top:14px}.preview-actions button{height:54px;border:1px solid #53657e;border-radius:13px;color:#fff;font-size:12px;font-weight:900}.preview-actions button:last-child{border-color:#1475ed;background:#1475ed}.analysis{display:flex;flex-direction:column;align-items:center;padding-top:20px}.analysis .paper{position:relative;overflow:hidden}.scan-line{position:absolute;right:0;left:0;height:3px;background:#21c8ff;box-shadow:0 0 16px #21c8ff;transition:top .25s}.analysis h2{margin-top:28px;font-size:17px}.analysis p{margin-top:7px;color:#78879b;font-size:10px}.progress{width:250px;height:6px;margin-top:25px;overflow:hidden;border-radius:3px;background:#dce5ef}.progress i{display:block;height:100%;background:#2673e8}.analysis>b{margin-top:9px;color:#2673e8;font-size:11px}
</style>
