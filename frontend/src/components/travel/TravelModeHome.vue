<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTravelModeStore } from '@/stores/travelMode'

defineProps({ userName: { type: String, default: '권유현' } })

const router = useRouter()
const travelMode = useTravelModeStore()

const destinations = [
  {
    code: 'all', name: '전체 여행', flag: '✈️', currency: 'EUR', rate: 1486.2,
    theme: '#173f8d', image: '', day: 2, totalDays: 15, remain: 1590000,
    foreign: 'EUR 1,027.74', daily: '약 106,000원', goal: 5000000, prepaid: 1800000, progress: 32,
    categories: [
      { icon: '🍽️', name: '식비', amount: 600000, percent: 54, color: '#356de8' },
      { icon: '☕', name: '카페', amount: 320000, percent: 42, color: '#bf3b4f' },
      { icon: '🧴', name: '생활비', amount: 430000, percent: 61, color: '#28aa76' },
      { icon: '🛍️', name: '쇼핑', amount: 130000, percent: 34, color: '#f09b00' },
      { icon: '🎨', name: '취미·여가', amount: 110000, percent: 28, color: '#7359d9' },
    ],
  },
  {
    code: 'FR', name: '파리', flag: '🇫🇷', currency: 'EUR', rate: 1486.2,
    theme: '#173f8d', image: '/images/france.png', day: 2, totalDays: 7, remain: 590000,
    foreign: 'EUR 357.63', daily: 'EUR 144.27 · 약 238,000원', goal: 2000000, prepaid: 810000, progress: 30,
    categories: [
      { icon: '🍽️', name: '식비', amount: 150000, percent: 50, color: '#356de8' },
      { icon: '☕', name: '카페', amount: 120000, percent: 42, color: '#628dea' },
      { icon: '🧴', name: '생활비', amount: 180000, percent: 60, color: '#28aa76' },
      { icon: '🛍️', name: '쇼핑', amount: 80000, percent: 40, color: '#7359d9' },
      { icon: '🎨', name: '취미·여가', amount: 60000, percent: 30, color: '#e39428' },
    ],
  },
  {
    code: 'CH', name: '인터라켄', flag: '🇨🇭', currency: 'CHF', rate: 1704.6,
    theme: '#9c1533', image: '/images/switzerland.webp', day: 2, totalDays: 7, remain: 1000000,
    foreign: 'CHF 565.11', daily: 'CHF 113.04 · 약 200,000원', goal: 3000000, prepaid: 990000, progress: 33,
    categories: [
      { icon: '🍽️', name: '식비', amount: 450000, percent: 56, color: '#7b63d9' },
      { icon: '☕', name: '카페', amount: 200000, percent: 48, color: '#628dea' },
      { icon: '🧴', name: '생활비', amount: 250000, percent: 63, color: '#28aa76' },
      { icon: '🛍️', name: '쇼핑', amount: 50000, percent: 25, color: '#7359d9' },
      { icon: '🎨', name: '취미·여가', amount: 50000, percent: 25, color: '#e39428' },
    ],
  },
]

const schedules = {
  all: [
    { date: '2026.08.15 (수)', flag: '🇫🇷', title: '루브르 박물관 가이드 투어', time: '10:30 · EUR 85.00', status: '사전결제 완료' },
    { date: '2026.08.21 (금)', flag: '🇨🇭', title: '인터라켄 TGV 열차', time: '14:00 · EUR 65.00', status: '현장결제 필요', warning: true },
  ],
  FR: [
    { date: '2026.08.15 (수)', flag: '🇫🇷', title: '루브르 박물관 가이드 투어', time: '10:30 · EUR 85.00', status: '사전결제 완료' },
    { date: '2026.08.17 (금)', flag: '🇫🇷', title: '파리 → 인터라켄 TGV 열차', time: '14:00 · EUR 65.00', status: '사전결제 완료' },
  ],
  CH: [
    { date: '2026.08.21 (금)', flag: '🇨🇭', title: '융프라우 전망대', time: '09:30 · CHF 72.00', status: '사전결제 완료' },
    { date: '2026.08.22 (토)', flag: '🇨🇭', title: '패러글라이딩 체험', time: '14:30 · CHF 110.00', status: '현장결제 필요', warning: true },
  ],
}

const recent = {
  all: [
    { icon: '☕', place: 'Café de Flore', country: '프랑스', amount: 18360, foreign: 'EUR 12.30' },
    { icon: '🫕', place: 'Swiss Fondue House', country: '스위스', amount: 68400, foreign: 'CHF 40.00' },
  ],
  FR: [
    { icon: '☕', place: 'Café de Flore', country: '식비 · 오늘 12:30', amount: 18360, foreign: 'EUR 12.30' },
    { icon: '🛒', place: 'Monoprix', country: '생활비 · 오늘 15:10', amount: 9945, foreign: 'EUR 6.69' },
  ],
  CH: [
    { icon: '🫕', place: 'Swiss Fondue House', country: '식비 · 오늘 12:30', amount: 68400, foreign: 'CHF 40.00' },
    { icon: '🚞', place: 'Interlaken Ost', country: '교통 · 오늘 16:20', amount: 42615, foreign: 'CHF 25.00' },
  ],
}

const selected = computed(() => destinations.find((item) => item.code === travelMode.selectedDestination) ?? destinations[0])
const selectedSchedules = computed(() => schedules[selected.value.code])
const selectedRecent = computed(() => recent[selected.value.code])
const calculatorDestination = computed(() => {
  if (selected.value.code !== 'all') return selected.value
  return destinations.find((item) => item.currency === travelMode.calculatorCurrency) ?? destinations[1]
})
const convertedAmount = computed(() => Math.round((Number(travelMode.calculatorAmount) || 0) * calculatorDestination.value.rate))

function selectDestination(item) {
  travelMode.selectDestination(item.code)
  if (item.code !== 'all') travelMode.setCalculatorCurrency(item.currency)
}

function formatWon(value) {
  return `${Number(value || 0).toLocaleString('ko-KR')}원`
}
</script>

<template>
  <section class="travel-home">
    <header class="travel-header">
      <div class="mode-switch" aria-label="서비스 모드 전환">
        <button class="active">여행</button>
        <button @click="travelMode.setMode('savings')">저축</button>
      </div>
      <p>안녕하세요, <b>{{ userName }}님</b></p>
      <span class="demo-badge">DEMO · 여행 2일차</span>
    </header>

    <div class="destination-scroll" aria-label="여행 국가 선택">
      <button
        v-for="item in destinations"
        :key="item.code"
        :class="{ active: selected.code === item.code }"
        @click="selectDestination(item)"
      >
        <span>{{ item.flag }}</span>{{ item.name }}
      </button>
    </div>

    <article
      class="travel-ticket"
      :class="{ 'ticket-all': selected.code === 'all' }"
      :style="{ '--ticket-color': selected.theme, '--ticket-image': `url(${selected.image})` }"
    >
      <div class="ticket-strip"><span>BOARDING PASS</span><span>TRIPASS AIR</span><span>NO. {{ selected.code }}-230</span></div>
      <div class="cut top"><i/><span/><i/></div>
      <div class="ticket-body">
        <div class="ticket-title">
          <div><small>{{ selected.code === 'all' ? 'TRIP SUMMARY' : 'DESTINATION' }}</small><h2>{{ selected.flag }} {{ selected.name }}</h2></div>
          <div class="day"><small>TRIP DAY</small><b>{{ selected.day }}일차 / {{ selected.totalDays }}일</b></div>
        </div>
        <div class="day-progress"><i :style="{ width: `${selected.day / selected.totalDays * 100}%` }"/></div>
        <p class="asset-label">{{ selected.code === 'all' ? '전체 남은 여행 자산 (합산)' : `${selected.name} 남은 여행 자산` }}</p>
        <strong class="foreign">{{ selected.code === 'all' ? formatWon(selected.remain) : selected.foreign }}</strong>
        <span class="won">{{ selected.code === 'all' ? '프랑스 590,000원 · 스위스 1,000,000원' : `(약 ${formatWon(selected.remain)})` }}</span>
        <div class="daily"><small>남은 여행 동안 하루에 쓸 수 있는 금액</small><b>{{ selected.daily }}</b></div>
        <div class="fund-row"><span>여행 자금 진행률</span><b>{{ selected.progress }}%</b></div>
        <div class="fund-progress"><i :style="{ width: `${selected.progress}%` }"/></div>
        <div class="fund-meta"><span>목표 {{ formatWon(selected.goal) }}</span><span>사전 지출 {{ formatWon(selected.prepaid) }}</span></div>
      </div>
      <div class="cut bottom"><i/><span/><i/></div>
      <div class="ticket-stub">{{ selected.code === 'all' ? '전체 여행 자금 현황' : `${selected.name} 여행 자금 현황` }}<b>|||||||||</b></div>
    </article>

    <article class="content-card budget-card" @click="router.push('/savings/monthly')">
      <div class="card-heading"><h2>여행 자금 체크</h2><span>상세 보기 ›</span></div>
      <div v-for="category in selected.categories" :key="category.name" class="budget-row">
        <span class="category">{{ category.icon }} {{ category.name }}</span>
        <div class="bar"><i :style="{ width: `${category.percent}%`, background: category.color }"/></div>
        <b>{{ formatWon(category.amount) }}</b>
      </div>
      <div class="budget-total"><span>지출 총합</span><strong>{{ formatWon(selected.categories.reduce((sum, item) => sum + item.amount, 0)) }}</strong></div>
    </article>

    <article class="content-card">
      <div class="card-heading"><h2>다가오는 여행 일정</h2><button @click="router.push('/schedule')">전체 보기 ›</button></div>
      <button v-for="item in selectedSchedules" :key="item.title" class="schedule-row" @click="router.push('/schedule')">
        <span class="schedule-date">{{ item.date }}</span>
        <span class="schedule-title">{{ item.flag }} {{ item.title }}<small>{{ item.time }}</small></span>
        <em :class="{ warning: item.warning }">{{ item.status }}</em>
      </button>
    </article>

    <article class="content-card recent-card">
      <div class="card-heading"><h2>최근 지출 내역</h2><button @click="router.push('/asset/transactions')">전체 보기 ›</button></div>
      <button v-for="item in selectedRecent" :key="item.place" class="recent-row" @click="router.push('/asset/transactions')">
        <span class="recent-icon">{{ item.icon }}</span>
        <span><b>{{ item.place }}</b><small>{{ item.country }} · {{ item.foreign }}</small></span>
        <strong>-{{ formatWon(item.amount) }}</strong>
      </button>
    </article>

    <button class="calculator-fab" aria-label="외화 간편 계산기 열기" @click="travelMode.openCalculator(selected.code === 'all' ? 'EUR' : selected.currency)">⌨</button>

    <div v-if="travelMode.calculatorOpen" class="calculator-backdrop" @click.self="travelMode.closeCalculator">
      <section class="calculator-sheet">
        <div class="sheet-handle"/>
        <div class="sheet-heading"><div><small>QUICK EXCHANGE</small><h2>외화 간편 계산기</h2></div><button @click="travelMode.closeCalculator">×</button></div>
        <div v-if="selected.code === 'all'" class="currency-selector">
          <button
            v-for="item in destinations.slice(1)"
            :key="item.code"
            :class="{ active: travelMode.calculatorCurrency === item.currency && calculatorDestination.code === item.code }"
            @click="travelMode.setCalculatorCurrency(item.currency)"
          >{{ item.flag }} {{ item.currency }}</button>
        </div>
        <label class="calculator-input">
          <span>{{ calculatorDestination.flag }} {{ calculatorDestination.name }}</span>
          <div><input v-model.number="travelMode.calculatorAmount" type="number" min="0" step="0.01"><b>{{ calculatorDestination.currency }}</b></div>
        </label>
        <div class="exchange-arrow">⇅</div>
        <div class="calculator-result"><span>대한민국 🇰🇷</span><strong>{{ formatWon(convertedAmount) }}</strong><small>1 {{ calculatorDestination.currency }} = {{ calculatorDestination.rate.toLocaleString('ko-KR') }}원</small></div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.travel-home{width:min(100%,390px);margin:0 auto;padding-bottom:84px;color:#10192d}.travel-header{position:relative;padding:48px 18px 14px;background:rgba(255,255,255,.82)}.travel-header p{margin-top:12px;font-size:16px}.mode-switch{display:inline-flex;padding:3px;border:1px solid #dbe3f0;border-radius:999px;background:#fff}.mode-switch button{padding:6px 11px;border-radius:999px;color:#8190a4;font-size:10px;font-weight:800}.mode-switch .active{background:#173f8d;color:#fff}.demo-badge{position:absolute;right:18px;top:54px;color:#8491a3;font-size:8px;font-weight:700}.destination-scroll{display:flex;gap:7px;overflow-x:auto;padding:11px 18px 4px;scrollbar-width:none}.destination-scroll::-webkit-scrollbar{display:none}.destination-scroll button{display:flex;flex:none;align-items:center;gap:5px;padding:8px 12px;border:1px solid #dce3ec;border-radius:999px;background:#fff;color:#708096;font-size:10px;font-weight:800}.destination-scroll button.active{border-color:#173f8d;background:#173f8d;color:#fff}.travel-ticket{position:relative;margin:10px 16px 0;overflow:hidden;border-radius:18px;background:var(--ticket-color);box-shadow:0 12px 28px rgba(20,38,78,.18);color:#fff}.ticket-strip{display:flex;justify-content:space-between;padding:14px 17px 12px;background:var(--ticket-color);color:rgba(255,255,255,.58);font-size:7px;font-weight:800;letter-spacing:.08em}.cut{position:relative;z-index:3;display:grid;grid-template-columns:18px 1fr 18px;align-items:center;height:0}.cut i{width:22px;height:22px;border-radius:50%;background:#f7f4ee}.cut i:first-child{transform:translateX(-11px)}.cut i:last-child{transform:translateX(7px)}.cut span{border-top:1px dashed rgba(255,255,255,.48)}.ticket-body{position:relative;padding:18px;background:linear-gradient(180deg,rgba(6,17,48,.2),rgba(5,15,45,.72)),var(--ticket-image) center/cover}.ticket-all .ticket-body{background:linear-gradient(135deg,#244eaa,#122e70)}.ticket-title{display:flex;justify-content:space-between;align-items:flex-end}.ticket-title small{color:rgba(255,255,255,.55);font-size:7px}.ticket-title h2{margin-top:3px;font-size:17px}.day{text-align:right}.day b{display:block;margin-top:3px;font-size:11px}.day-progress,.fund-progress{height:4px;margin-top:10px;overflow:hidden;border-radius:99px;background:rgba(255,255,255,.22)}.day-progress i{display:block;height:100%;background:#ffca3a}.asset-label{margin-top:16px;color:rgba(255,255,255,.65);font-size:8px}.foreign{display:block;margin-top:3px;font-size:24px}.won{display:block;margin-top:2px;color:rgba(255,255,255,.62);font-size:8px}.daily{display:flex;justify-content:space-between;align-items:center;margin-top:12px;padding:10px;border-radius:10px;background:rgba(255,255,255,.12)}.daily small{font-size:7px}.daily b{font-size:9px}.fund-row{display:flex;justify-content:space-between;margin-top:13px;font-size:9px}.fund-progress i{display:block;height:100%;background:linear-gradient(90deg,#2d75ff,#ffcf34)}.fund-meta{display:flex;justify-content:space-between;margin-top:6px;color:rgba(255,255,255,.58);font-size:7px}.ticket-stub{display:flex;justify-content:space-between;padding:12px 17px;background:rgba(4,15,48,.38);font-size:9px;font-weight:800}.ticket-stub b{letter-spacing:-2px}.content-card{margin:12px 16px 0;padding:15px;border:1px solid #e1e6ed;border-radius:16px;background:#fff;box-shadow:0 5px 14px rgba(26,48,83,.05)}.card-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}.card-heading h2{font-size:12px}.card-heading span,.card-heading button{color:#8290a3;font-size:8px}.budget-row{display:grid;grid-template-columns:72px 1fr 68px;align-items:center;gap:8px;margin-top:10px}.category{font-size:9px}.bar{height:5px;overflow:hidden;border-radius:99px;background:#edf0f4}.bar i{display:block;height:100%;border-radius:99px}.budget-row b{text-align:right;font-size:8px}.budget-total{display:flex;justify-content:space-between;margin-top:13px;padding-top:11px;border-top:1px dashed #dce3ed;font-size:9px}.budget-total strong{color:#176be8}.schedule-row{display:grid;width:100%;grid-template-columns:82px 1fr auto;align-items:center;gap:6px;padding:10px 0;border-top:1px solid #eef1f5;text-align:left}.schedule-date{color:#1d60cb;font-size:7px;font-weight:800}.schedule-title{font-size:8px;font-weight:800}.schedule-title small{display:block;margin-top:3px;color:#8e9aab;font-size:7px;font-weight:400}.schedule-row em{padding:4px 6px;border-radius:6px;background:#edf5ff;color:#2472da;font-size:6px;font-style:normal}.schedule-row em.warning{background:#fff0ef;color:#db6258}.recent-card{margin-bottom:18px}.recent-row{display:grid;width:100%;grid-template-columns:30px 1fr auto;align-items:center;gap:8px;padding:9px 0;border-top:1px solid #eef1f5;text-align:left}.recent-icon{display:grid;width:28px;height:28px;place-items:center;border-radius:50%;background:#f4f6f9}.recent-row b,.recent-row small{display:block}.recent-row b{font-size:8px}.recent-row small{margin-top:3px;color:#8c98a9;font-size:7px}.recent-row strong{font-size:8px}.calculator-fab{position:fixed;right:max(calc((100vw - 390px)/2 + 18px),18px);bottom:76px;z-index:40;display:grid;width:48px;height:48px;place-items:center;border:5px solid #dce5f3;border-radius:50%;background:#173f8d;color:#fff;font-size:20px;box-shadow:0 7px 18px rgba(19,55,128,.28)}.calculator-backdrop{position:fixed;inset:0;z-index:60;display:flex;align-items:flex-end;justify-content:center;background:rgba(15,23,42,.42)}.calculator-sheet{width:min(100%,390px);padding:10px 18px 28px;border-radius:24px 24px 0 0;background:#f8f6f1;box-shadow:0 -12px 35px rgba(15,23,42,.18)}.sheet-handle{width:38px;height:4px;margin:0 auto 16px;border-radius:99px;background:#ccd3dc}.sheet-heading{display:flex;justify-content:space-between}.sheet-heading small{color:#7d8ba0;font-size:7px}.sheet-heading h2{margin-top:3px;font-size:16px}.sheet-heading button{font-size:25px;color:#8995a6}.currency-selector{display:flex;gap:6px;overflow-x:auto;margin-top:14px}.currency-selector button{flex:none;padding:7px 9px;border-radius:9px;background:#fff;color:#758196;font-size:8px}.currency-selector button.active{background:#173f8d;color:#fff}.calculator-input,.calculator-result{display:block;margin-top:13px;padding:13px;border:1px solid #e1e6ed;border-radius:13px;background:#fff}.calculator-input>span,.calculator-result>span{color:#7d899a;font-size:8px}.calculator-input>div{display:flex;align-items:end;margin-top:7px}.calculator-input input{min-width:0;flex:1;font-size:25px;font-weight:900;outline:none}.calculator-input b{color:#748197;font-size:11px}.exchange-arrow{text-align:center;color:#174494;font-size:20px}.calculator-result strong{display:block;margin-top:5px;color:#174494;font-size:25px}.calculator-result small{display:block;margin-top:5px;color:#8b97a8;font-size:8px}
</style>
