<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTravelModeStore } from '@/stores/travelMode'

const props = defineProps({
  userName: { type: String, default: '권유현' },
  onSwitchMode: { type: Function, default: null },
})

const router = useRouter()
const travelMode = useTravelModeStore()
const countryMenuOpen = ref(false)
const calculatorCountryCode = ref('FR')

const destinations = [
  {
    code: 'all', name: '전체', title: '전체 여행', flag: '🌍', image: '', theme: '#17485b', currency: 'EUR', rate: 1486.2,
    dday: 13, day: 2, totalDays: 15, remain: 3440000, localAmount: '3,440,000원', daily: '', goal: 11500000, prepaid: 4010000, progress: 30,
    categories: [
      { icon: '🍴', name: '식비', amount: 600000, france: 27, swiss: 73 },
      { icon: '☕', name: '카페', amount: 320000, france: 60, swiss: 40 },
      { icon: '📦', name: '생활비', amount: 430000, france: 52, swiss: 48 },
      { icon: '🛍️', name: '쇼핑', amount: 130000, france: 62, swiss: 38 },
      { icon: '🎨', name: '취미·여가', amount: 110000, france: 55, swiss: 45 },
      { icon: '💬', name: '기타', amount: 110000, france: 18, swiss: 82 },
    ],
  },
  {
    code: 'FR', name: '프랑스', title: '파리', flag: '🇫🇷', image: '/images/france.png', theme: '#0b3c90', currency: 'EUR', rate: 1486.2,
    dday: 5, day: 2, totalDays: 7, remain: 590000, localAmount: 'EUR 357.63', daily: 'EUR 144.27', dailyWon: '약 238,000원', goal: 2000000, prepaid: 810000, progress: 30,
    categories: [
      { icon: '🍴', name: '식비', amount: 150000, percent: 55, color: '#7962d9' },
      { icon: '☕', name: '카페', amount: 120000, percent: 48, color: '#5e91ea' },
      { icon: '📦', name: '생활비', amount: 180000, percent: 62, color: '#63c27e' },
      { icon: '🛍️', name: '쇼핑', amount: 80000, percent: 40, color: '#8668dd' },
      { icon: '🎨', name: '취미·여가', amount: 60000, percent: 30, color: '#efa43a' },
      { icon: '💬', name: '기타', amount: 10000, percent: 15, color: '#87929f' },
    ],
  },
  {
    code: 'CH', name: '스위스', title: '인터라켄', flag: '🇨🇭', image: '/images/switzerland.webp', theme: '#97112d', currency: 'CHF', rate: 1704.6,
    dday: 5, day: 2, totalDays: 7, remain: 1000000, localAmount: 'CHF 565.11', daily: 'CHF 113.04', dailyWon: '약 200,000원', goal: 3000000, prepaid: 990000, progress: 33,
    categories: [
      { icon: '🍴', name: '식비', amount: 450000, percent: 60, color: '#8065da' },
      { icon: '☕', name: '카페', amount: 200000, percent: 50, color: '#6093ea' },
      { icon: '📦', name: '생활비', amount: 250000, percent: 66, color: '#63c27e' },
      { icon: '🛍️', name: '쇼핑', amount: 50000, percent: 30, color: '#8468dc' },
      { icon: '🎨', name: '취미·여가', amount: 50000, percent: 30, color: '#efa43a' },
      { icon: '💬', name: '기타', amount: 100000, percent: 45, color: '#87929f' },
    ],
  },
  {
    code: 'DE', name: '독일', title: '베를린', flag: '🇩🇪', image: '/images/germany.png', theme: '#171717', currency: 'EUR', rate: 1486.2,
    dday: 5, day: 2, totalDays: 7, remain: 720000, localAmount: 'EUR 484.46', daily: 'EUR 96.89', dailyWon: '약 144,000원', goal: 2500000, prepaid: 830000, progress: 34,
    categories: [
      { icon: '🍴', name: '식비', amount: 240000, percent: 58, color: '#d5a300' }, { icon: '☕', name: '카페', amount: 90000, percent: 38, color: '#638fe3' },
      { icon: '📦', name: '생활비', amount: 160000, percent: 52, color: '#63c27e' }, { icon: '🛍️', name: '쇼핑', amount: 80000, percent: 35, color: '#e1a000' },
      { icon: '🎨', name: '취미·여가', amount: 70000, percent: 31, color: '#d95e49' }, { icon: '💬', name: '기타', amount: 40000, percent: 20, color: '#87929f' },
    ],
  },
  {
    code: 'JP', name: '일본', title: '도쿄', flag: '🇯🇵', image: '/images/japan.webp', theme: '#d92d7a', currency: 'JPY', rate: 9.23,
    dday: 5, day: 2, totalDays: 7, remain: 650000, localAmount: 'JPY 70,422', daily: 'JPY 14,084', dailyWon: '약 130,000원', goal: 2200000, prepaid: 760000, progress: 35,
    categories: [
      { icon: '🍴', name: '식비', amount: 220000, percent: 60, color: '#e55b9a' }, { icon: '☕', name: '카페', amount: 80000, percent: 36, color: '#638fe3' },
      { icon: '📦', name: '생활비', amount: 150000, percent: 48, color: '#63c27e' }, { icon: '🛍️', name: '쇼핑', amount: 100000, percent: 42, color: '#ec5c9d' },
      { icon: '🎨', name: '취미·여가', amount: 60000, percent: 29, color: '#efa43a' }, { icon: '💬', name: '기타', amount: 30000, percent: 17, color: '#87929f' },
    ],
  },
  {
    code: 'HK', name: '홍콩', title: '홍콩', flag: '🇭🇰', image: '/images/Hong%20Kong.png', theme: '#b8202e', currency: 'HKD', rate: 184.2,
    dday: 5, day: 2, totalDays: 7, remain: 480000, localAmount: 'HKD 2,606.95', daily: 'HKD 521.17', dailyWon: '약 96,000원', goal: 1800000, prepaid: 620000, progress: 38,
    categories: [
      { icon: '🍴', name: '식비', amount: 180000, percent: 57, color: '#e0a400' }, { icon: '☕', name: '카페', amount: 60000, percent: 32, color: '#638fe3' },
      { icon: '📦', name: '생활비', amount: 120000, percent: 44, color: '#63c27e' }, { icon: '🛍️', name: '쇼핑', amount: 70000, percent: 36, color: '#e0a400' },
      { icon: '🎨', name: '취미·여가', amount: 40000, percent: 25, color: '#efa43a' }, { icon: '💬', name: '기타', amount: 20000, percent: 15, color: '#87929f' },
    ],
  },
]

const schedules = {
  all: [
    { date: '2026.08.15 (수)', flag: '🇫🇷', title: '루브르 박물관 가이드 투어', time: '10:30 · EUR 85.00', status: '사전결제 완료' },
    { date: '2026.08.21 (금)', flag: '🇨🇭', title: '체르마트 마터호른 샬레 숙소', time: '15:00 체크인 · CHF 220.00', status: '현장결제 필요', warning: true },
  ],
  FR: [
    { date: '2026.08.15 (수)', flag: '🇫🇷', title: '루브르 박물관 가이드 투어', time: '10:30 · EUR 85.00', status: '사전결제 완료' },
    { date: '', flag: '🚆', title: '파리 → 인터라켄 TGV 열차', time: '14:00 · EUR 65.00', status: '사전결제 완료' },
  ],
  CH: [
    { date: '2026.08.15 (수)', flag: '🇨🇭', title: '융프라우 전망대', time: '09:30 · CHF 72.00', status: '사전결제 완료' },
    { date: '', flag: '🪂', title: '패러글라이딩 체험', time: '14:30 · CHF 110.00', status: '현장결제 필요', warning: true },
  ],
  DE: [{ date: '2026.08.25 (화)', flag: '🇩🇪', title: '브란덴부르크 문 투어', time: '10:00 · EUR 35.00', status: '사전결제 완료' }],
  JP: [{ date: '2026.08.25 (화)', flag: '🇯🇵', title: '시부야 전망대', time: '18:00 · JPY 2,500', status: '사전결제 완료' }],
  HK: [{ date: '2026.08.25 (화)', flag: '🇭🇰', title: '빅토리아 피크 야경 투어', time: '18:30 · HKD 320', status: '사전결제 완료' }],
}

const recent = {
  all: [
    { icon: '☕', place: 'Café de Flore (프랑스)', meta: '식비 · 오늘 12:30', amount: 18360 },
    { icon: '🫕', place: 'Swiss Fondue House (스위스)', meta: '식비 · 어제 19:10', amount: 68400 },
  ],
  FR: [
    { icon: '☕', place: 'Café de Flore', meta: '식비 · 오늘 12:30', amount: 18360 },
    { icon: '🛒', place: 'Monoprix', meta: '생활비 · 오늘 15:10', amount: 9945 },
  ],
  CH: [
    { icon: '🫕', place: 'Swiss Fondue House', meta: '식비 · 오늘 12:30', amount: 68400 },
    { icon: '🚞', place: 'Interlaken Ost', meta: '교통 · 오늘 16:20', amount: 42615 },
  ],
  DE: [{ icon: '🥨', place: 'Zeit für Brot', meta: '식비 · 오늘 10:20', amount: 14860 }],
  JP: [{ icon: '🍜', place: '이치란 라멘', meta: '식비 · 오늘 13:10', amount: 12400 }],
  HK: [{ icon: '🥟', place: 'Tim Ho Wan', meta: '식비 · 오늘 13:20', amount: 38600 }],
}

const overallAssets = [
  { code: 'FR', flag: '🇫🇷', city: '파리', amount: 590000, local: '약 €128.10', image: '/images/france.png', theme: '#124c9f' },
  { code: 'CH', flag: '🇨🇭', city: '스위스', amount: 1000000, local: '약 CHF 586.65', image: '/images/switzerland.webp', theme: '#a81436' },
  { code: 'DE', flag: '🇩🇪', city: '베를린', amount: 720000, local: '약 EUR 484.46', image: '/images/germany.png', theme: '#202020' },
  { code: 'JP', flag: '🇯🇵', city: '도쿄', amount: 650000, local: '약 JPY 70,422', image: '/images/japan.webp', theme: '#c82770' },
  { code: 'HK', flag: '🇭🇰', city: '홍콩', amount: 480000, local: '약 HKD 2,606.95', image: '/images/Hong%20Kong.png', theme: '#b8202e' },
]

const selected = computed(() => destinations.find(item => item.code === travelMode.selectedDestination) ?? destinations[0])
const selectedSchedules = computed(() => schedules[selected.value.code])
const selectedRecent = computed(() => recent[selected.value.code])
const calculatorDestination = computed(() => selected.value.code === 'all'
  ? destinations.find(item => item.code === calculatorCountryCode.value) ?? destinations[1]
  : selected.value)
const convertedAmount = computed(() => Math.round((Number(travelMode.calculatorAmount) || 0) * calculatorDestination.value.rate))
const categoryTotal = computed(() => selected.value.categories.reduce((sum, item) => sum + item.amount, 0))

function formatWon(value) { return `${Number(value || 0).toLocaleString('ko-KR')}원` }
function selectDestination(item) {
  travelMode.selectDestination(item.code)
  if (item.code !== 'all') travelMode.setCalculatorCurrency(item.currency)
  countryMenuOpen.value = false
}
function openCalculator() {
  if (selected.value.code !== 'all') calculatorCountryCode.value = selected.value.code
  travelMode.openCalculator(selected.value.code === 'all' ? travelMode.calculatorCurrency : selected.value.currency)
}
function selectCalculatorDestination(item) {
  calculatorCountryCode.value = item.code
  travelMode.setCalculatorCurrency(item.currency)
}
function switchMode(mode) {
  if (props.onSwitchMode) props.onSwitchMode(mode)
  else travelMode.setMode(mode)
}
</script>

<template>
  <section class="travel-home">
    <header class="travel-header">
      <div class="mode-toggle travel-selected" aria-label="서비스 모드 전환">
        <span class="mode-thumb" />
        <button class="active" type="button" @click="switchMode('travel')">여행</button>
        <button type="button" @click="switchMode('savings')">저축</button>
      </div>
      <h1>안녕하세요, {{ userName }}님</h1>
      <div class="country-select">
        <button type="button" :aria-expanded="countryMenuOpen" @click="countryMenuOpen = !countryMenuOpen">
          <span>{{ selected.flag }}</span>{{ selected.name }}<i>⌄</i>
        </button>
        <div v-if="countryMenuOpen" class="country-menu">
          <button v-for="item in destinations" :key="item.code" type="button" :class="{ active: item.code === selected.code }" @click="selectDestination(item)">
            <span>{{ item.flag }}</span>{{ item.name }}
          </button>
        </div>
      </div>
    </header>

    <article class="ticket" :class="[{ combined: selected.code === 'all' }, `country-${selected.code}`]" :style="{ '--theme': selected.theme, '--photo': `url(${selected.image})` }">
      <div class="ticket-top"><span>BOARDING PASS</span><span>TRIPASS AIR</span><span>NO. {{ selected.code === 'all' ? 'EUR' : selected.code }}-230</span></div>
      <div class="perforation"><i/><span/><i/></div>
      <div class="ticket-main">
        <div class="trip-line"><b>{{ selected.flag }} {{ selected.title }}</b><strong>D-{{ selected.dday }}</strong></div>
        <div class="trip-progress"><small>{{ selected.day }}일차</small><div><i :style="{ width: `${selected.day / selected.totalDays * 100}%` }"/></div><small>{{ selected.totalDays }}일차</small></div>
        <p class="trip-description">{{ selected.code === 'all' ? '등록한 모든 여행의 남은 자산을 한눈에 확인해요' : `${selected.title}에서 시작되는 설레는 여행을 즐겨보세요` }} ✨</p>
        <div class="ticket-photo-space" />

        <div class="travel-summary-content">
          <template v-if="selected.code === 'all'">
            <div class="summary-title"><span>전체 남은 여행 자산 (합산)</span><strong>{{ formatWon(selected.remain) }}</strong></div>
            <div class="country-assets" aria-label="국가별 남은 여행 자산">
              <div v-for="asset in overallAssets" :key="asset.code" class="country-asset-card" :style="{ '--asset-image': `url(${asset.image})`, '--asset-theme': asset.theme }">
                <span>{{ asset.flag }} {{ asset.city }} 남은 여행 자산</span><b>{{ formatWon(asset.amount) }}</b><small>({{ asset.local }})</small>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="summary-title asset-title"><span>{{ selected.title }} 남은 여행 자산</span><strong>{{ selected.localAmount }} <small>(약 {{ formatWon(selected.remain) }})</small></strong></div>
            <div class="daily-budget"><span>남은 5일 동안 하루에 쓸 수 있는 금액</span><b>{{ selected.daily }} <small>({{ selected.dailyWon }})</small></b></div>
          </template>
          <div class="fund-label"><span>여행 자금 진행률</span><b>{{ selected.progress }}%</b></div>
          <div class="fund-track"><i :style="{ width: `${selected.progress}%` }"/></div>
          <div class="fund-meta"><span>목표 {{ formatWon(selected.goal) }}</span><span>사전 지불 금액 {{ formatWon(selected.prepaid) }}</span></div>
        </div>
      </div>
      <div class="perforation lower"><i/><span/><i/></div>
      <button class="ticket-stub" type="button" @click="router.push('/travel/funds')"><span>여행 목표 자금 관리</span><div class="stub-action"><div class="barcode"><i v-for="(height,index) in [18,11,22,8,17,13,23,8,19,9,15,12,21,8,18]" :key="index" :style="{ height: `${height}px`, width: index % 4 === 0 ? '3px' : '2px' }" /></div><b>›</b></div></button>
    </article>

    <article class="card budget-card" role="button" tabindex="0" aria-label="여행 자금 체크 상세 보기" @click="router.push('/travel/funds')" @keydown.enter="router.push('/travel/funds')">
      <div class="card-title"><h2>여행자금 체크</h2><div v-if="selected.code === 'all'" class="legend"><span>● 프랑스</span><span>● 스위스</span></div></div>
      <div v-for="category in selected.categories" :key="category.name" class="budget-row">
        <span class="category"><i>{{ category.icon }}</i>{{ category.name }}</span>
        <div v-if="selected.code === 'all'" class="split-bar"><i :style="{ width: `${category.france}%` }"/><em :style="{ width: `${category.swiss}%` }"/></div>
        <div v-else class="single-bar"><i :style="{ width: `${category.percent}%`, background: category.color }"/></div>
        <b>{{ formatWon(category.amount) }}</b>
      </div>
      <div v-if="selected.code !== 'all'" class="budget-total"><span>지출 총합</span><strong>{{ formatWon(categoryTotal) }}</strong></div>
    </article>

    <article class="card">
      <div class="card-title"><h2>다가오는 여행 일정</h2><button type="button" @click="router.push('/schedule')">전체 보기</button></div>
      <button v-for="item in selectedSchedules" :key="item.title" class="schedule-row" type="button" @click="router.push('/schedule')">
        <span><b v-if="item.date">{{ item.date }}</b><strong>{{ item.flag }} {{ item.title }}</strong><small>{{ item.time }}</small></span>
        <em :class="{ warning: item.warning }">{{ item.status }}</em>
      </button>
    </article>

    <article class="card recent-card">
      <div class="card-title"><h2>최근 지출 내역</h2><button type="button" @click="router.push('/asset/transactions')">전체 보기</button></div>
      <button v-for="item in selectedRecent" :key="item.place" class="recent-row" type="button" @click="router.push('/asset/transactions')">
        <i>{{ item.icon }}</i><span><b>{{ item.place }}</b><small>{{ item.meta }}</small></span><strong>- {{ formatWon(item.amount) }}</strong>
      </button>
    </article>

    <section v-if="travelMode.calculatorOpen" class="quick-calculator" aria-label="외화 계산기">
      <div class="calculator-head"><b>외화 계산기</b><button type="button" aria-label="닫기" @click="travelMode.closeCalculator">×</button></div>
      <div v-if="selected.code === 'all'" class="calculator-currencies">
        <button v-for="item in destinations.slice(1)" :key="item.code" type="button" :class="{ active: calculatorDestination.code === item.code }" @click="selectCalculatorDestination(item)">{{ item.flag }} {{ item.currency }}</button>
      </div>
      <div class="calculator-fields"><label><input v-model.number="travelMode.calculatorAmount" type="number" min="0"><span>{{ calculatorDestination.currency }}</span></label><b>↔</b><output>{{ convertedAmount.toLocaleString('ko-KR') }} <small>KRW</small></output></div>
    </section>
    <button class="calculator-fab" type="button" aria-label="외화 계산기 열기" @click="openCalculator">▦</button>
  </section>
</template>

<style scoped>
.travel-home{width:min(100%,390px);margin:auto;padding-bottom:94px;background:#f8f6f1;color:#10192d}.travel-header{position:relative;display:flex;align-items:center;gap:10px;padding:46px 16px 14px;background:#fff}.travel-header h1{font-size:16px;font-weight:800;white-space:nowrap}.mode-toggle{display:flex;align-items:center;padding:2px;border:2px solid #173f8d;border-radius:999px;background:#fff}.mode-toggle button{padding:5px 7px;border-radius:999px;color:#173f8d;font-size:10px;font-weight:900}.mode-toggle button.active{background:#173f8d;color:#fff}.country-select{position:relative;margin-left:auto}.country-select>button{display:flex;align-items:center;gap:5px;padding:8px 9px;border:1px solid #d8e0eb;border-radius:11px;background:#fff;font-size:11px;font-weight:800}.country-select i{font-style:normal;color:#64748b}.country-menu{position:absolute;right:0;top:40px;z-index:80;width:138px;padding:5px;border:1px solid #dce3ed;border-radius:12px;background:#fff;box-shadow:0 10px 25px #15254724}.country-menu button{display:flex;width:100%;gap:7px;padding:10px;border-radius:8px;text-align:left;font-size:11px}.country-menu button.active{background:#eef4ff;color:#173f8d;font-weight:900}.ticket{position:relative;margin:0 16px;overflow:hidden;border-radius:18px;background:var(--theme);color:#fff;box-shadow:0 8px 18px #2037652b}.ticket-top{display:flex;justify-content:space-between;padding:15px 18px 14px;color:#ffffffa6;font-size:8px;font-weight:800;letter-spacing:.06em}.perforation{position:relative;z-index:3;display:grid;grid-template-columns:20px 1fr 20px;align-items:center;height:0}.perforation i{width:22px;height:22px;border-radius:50%;background:#f8f6f1}.perforation i:first-child{transform:translateX(-11px)}.perforation i:last-child{transform:translateX(9px)}.perforation span{border-top:1px dashed #ffffff70}.ticket-main{min-height:312px;padding:20px;background:linear-gradient(180deg,#091b4270,#071733b8),var(--photo) center/cover}.combined .ticket-main{background:linear-gradient(135deg,#103779,#1553a2)}.trip-line{display:flex;justify-content:space-between;align-items:center}.trip-line b{font-size:14px}.trip-line strong{font-size:24px}.trip-progress{display:grid;grid-template-columns:42px 1fr 42px;align-items:center;gap:6px;margin-top:12px;color:#d8e5ff;font-size:10px}.trip-progress small:last-child{text-align:right}.trip-progress div{height:3px;background:#ffffff80}.trip-progress i{display:block;height:4px;background:#ffb800}.ticket-label{margin-top:18px;color:#ffbd14;font-size:11px;font-weight:800}.ticket h2{margin-top:5px;font-size:28px}.ticket h2 small{font-size:10px;color:#8cebbf}.country-assets{display:grid;grid-template-columns:1fr 1fr;margin-top:14px;overflow:hidden;border-radius:12px;background:#ffffff12}.country-assets>div{position:relative;padding:14px 12px;background-position:center;background-size:cover}.country-assets>div::before{position:absolute;inset:0;content:"";background:linear-gradient(135deg,#0c4b9fe8,#163c8adb)}.country-assets>div.swiss-asset::before{background:linear-gradient(135deg,#8d1639e8,#c91432dc)}.country-assets .france-asset{background-image:url('/images/france.png')}.country-assets .swiss-asset{background-image:url('/images/switzerland.webp')}.country-assets span,.country-assets b,.country-assets small{position:relative;display:block;z-index:1}.country-assets span{font-size:10px;color:#e3ecfa}.country-assets b{margin-top:6px;font-size:18px}.country-assets small{text-align:right;color:#8cebbf;font-size:8px}.daily-budget{margin-top:20px;padding:13px;border-radius:12px;background:#ffffff1a}.daily-budget span,.daily-budget b{display:block}.daily-budget span{font-size:10px;color:#dde7f8}.daily-budget b{margin-top:6px;font-size:15px}.daily-budget small{font-size:9px;color:#8cebbf}.fund-label{display:flex;justify-content:space-between;margin-top:16px;font-size:10px;font-weight:800}.fund-track{height:7px;margin-top:8px;overflow:hidden;border-radius:99px;background:#ffffff30}.fund-track i{display:block;height:100%;border-radius:99px;background:linear-gradient(90deg,#73c8e7,#fff1cc 55%,#ef5b54)}.fund-meta{display:flex;justify-content:space-between;margin-top:10px;color:#d6e1f2;font-size:9px}.ticket-stub{display:flex;width:100%;align-items:center;justify-content:space-between;padding:14px 18px;background:#fff;color:var(--theme);font-size:12px;font-weight:900}.ticket-stub b{color:#263a5b;letter-spacing:-1px}.card{display:block;width:calc(100% - 32px);margin:12px 16px 0;padding:16px;border:1px solid #dfe5ee;border-radius:16px;background:#fff;box-shadow:0 4px 12px #1425480d;text-align:left}.card-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px}.card-title h2{font-size:15px}.card-title button,.legend{font-size:9px;color:#4b77ca}.legend{display:flex;gap:8px}.legend span:first-child{color:#276ce0}.legend span:last-child{color:#c12b40}.budget-row{display:grid;grid-template-columns:92px 1fr 76px;align-items:center;gap:7px;height:43px}.category{display:flex;align-items:center;gap:7px;font-size:11px;font-weight:700}.category i{display:grid;width:28px;height:28px;place-items:center;border-radius:50%;background:#f3f5f8;font-style:normal}.single-bar,.split-bar{display:flex;height:7px;overflow:hidden;border-radius:99px;background:#e8ebf2}.single-bar i,.split-bar i,.split-bar em{display:block;height:100%}.split-bar i{background:#0b3c90}.split-bar em{background:#a51530}.budget-row>b{text-align:right;font-size:10px}.budget-total{display:flex;justify-content:space-between;margin-top:8px;padding-top:10px;border-top:1px solid #26333f;font-size:11px}.budget-total strong{color:#2872e5}.schedule-row{display:flex;width:100%;justify-content:space-between;align-items:center;padding:11px 0;border-top:1px solid #edf0f4;text-align:left}.schedule-row span>*{display:block}.schedule-row b{margin-bottom:5px;color:#315fc0;font-size:10px}.schedule-row strong{font-size:11px}.schedule-row small{margin-top:4px;color:#8996a7;font-size:9px}.schedule-row em{padding:5px 7px;border-radius:6px;background:#eaf3ff;color:#2472da;font-size:8px;font-style:normal}.schedule-row em.warning{background:#fff0ef;color:#db6258}.recent-card{margin-bottom:12px}.recent-row{display:grid;width:100%;grid-template-columns:30px 1fr auto;align-items:center;gap:8px;padding:11px 0;border-top:1px solid #edf0f4;text-align:left}.recent-row>i{font-style:normal}.recent-row span>*{display:block}.recent-row b,.recent-row strong{font-size:10px}.recent-row small{margin-top:3px;color:#8c98a9;font-size:8px}.quick-calculator{position:fixed;right:max(calc((100vw - 390px)/2 + 18px),18px);bottom:77px;z-index:45;width:320px;padding:14px;border:1px solid #dfe5ee;border-radius:15px;background:#fff;box-shadow:0 10px 30px #15254733}.calculator-head{display:flex;justify-content:space-between;font-size:13px}.calculator-head button{font-size:18px;color:#7b8798}.calculator-currencies{display:flex;gap:5px;margin-top:8px;overflow-x:auto}.calculator-currencies button{flex:none;padding:6px 8px;border-radius:7px;background:#f2f4f7;font-size:9px}.calculator-currencies button.active{background:#173f8d;color:#fff}.calculator-fields{display:grid;grid-template-columns:1fr 20px 1fr;align-items:center;gap:4px;margin-top:8px}.calculator-fields label,.calculator-fields output{display:flex;align-items:center;justify-content:space-between;padding:10px;border-radius:9px;background:#f4f5f7;font-size:12px;font-weight:900}.calculator-fields input{width:70px;background:transparent;font-weight:900;outline:none}.calculator-fields span,.calculator-fields small{color:#9aa4b3;font-size:8px}.calculator-fab{position:fixed;right:max(calc((100vw - 390px)/2 + 18px),18px);bottom:74px;z-index:44;width:48px;height:48px;border:6px solid #dce5f2;border-radius:50%;background:#173f8d;color:#fff;font-size:22px;box-shadow:0 8px 18px #173f8d3d}
.travel-header{padding:40px 16px 12px}
.mode-toggle{position:relative;display:grid;grid-template-columns:1fr 1fr;width:84px;padding:2px;overflow:hidden}
.mode-toggle button{position:relative;z-index:2;height:25px;padding:0;border-radius:999px;transition:color .25s ease}
.mode-toggle button.active{background:transparent;color:#fff}
.mode-thumb{position:absolute;top:2px;left:2px;width:calc(50% - 2px);height:25px;border-radius:999px;background:#173f8d;transition:transform .3s cubic-bezier(.22,1,.36,1)}
.ticket-main{height:335px;min-height:335px;background:linear-gradient(180deg,#091b4260,#071733bf),var(--photo) center/cover}
.trip-line{display:grid;grid-template-columns:auto 1fr auto;align-items:end;gap:8px}
.trip-line>div{display:flex;flex-direction:column;gap:4px}
.trip-line small{color:#ffffff80;font-size:7px;letter-spacing:.12em}
.trip-line b{font-size:15px}.departure{text-align:right}
.flight-route{display:flex;align-items:center;color:#ffd829;font-size:16px}
.flight-route i{width:100%;border-top:1px dashed #ffffff80}
.trip-description{height:16px;margin-top:9px;overflow:hidden;color:#ffffffe0;font-size:9px;white-space:nowrap;text-overflow:ellipsis}
.ticket-main{height:320px;min-height:320px;padding:20px;overflow:hidden}
.ticket-photo-space{height:36px}
.travel-summary-panel{padding:12px 14px;border-radius:14px;background:color-mix(in srgb,var(--theme) 78%,transparent);box-shadow:inset 0 0 0 1px #ffffff0d;backdrop-filter:blur(2px)}
.travel-summary-panel .trip-progress{margin-top:0}
.summary-title{display:flex;align-items:flex-end;justify-content:space-between;gap:10px;margin-top:9px}
.summary-title>span{color:#fff;font-size:10px;font-weight:800}
.summary-title>strong{color:#fff;font-size:18px;white-space:nowrap}
.summary-title>strong small{color:#8cebbf;font-size:8px}
.travel-summary-panel .country-assets{margin-top:8px;border-radius:9px}
.travel-summary-panel .country-assets>div{padding:8px 9px}
.travel-summary-panel .country-assets span{font-size:8px}.travel-summary-panel .country-assets b{margin-top:3px;font-size:13px}.travel-summary-panel .country-assets small{font-size:7px}
.travel-summary-panel .daily-budget{margin-top:8px;padding:8px 10px;border-radius:9px;background:#ffffff12}
.travel-summary-panel .daily-budget span{font-size:8px}.travel-summary-panel .daily-budget b{margin-top:3px;font-size:12px}
.travel-summary-panel .fund-label{margin-top:9px}.travel-summary-panel .fund-track{height:6px;margin-top:5px}.travel-summary-panel .fund-meta{margin-top:6px}
.ticket-stub{height:45px;padding:0 18px;background:var(--theme);color:#fff}
.ticket-stub b{color:#fff}
.trip-line{display:flex;align-items:center;justify-content:space-between}
.trip-line b{font-size:14px}.trip-line strong{font-size:22px}
.trip-progress{grid-template-columns:42px 1fr 42px;margin-top:7px;font-size:9px}
.trip-description{margin-top:7px;font-size:9px}
.ticket-photo-space{height:34px}
.travel-summary-content{color:#fff}
.summary-title{display:block;margin-top:0}
.summary-title>span{display:block;margin-bottom:3px;color:#ffffffd9;font-size:11px}
.summary-title>strong{display:block;font-size:24px;line-height:1.15}
.summary-title>strong small{margin-left:4px;color:#8ff0bf;font-size:10px}
.asset-title{display:grid;grid-template-columns:auto 1fr;align-items:end;gap:10px}
.asset-title>span{margin:0;font-size:11px;font-weight:800}
.asset-title>strong{text-align:right;font-size:23px}
.travel-summary-content .daily-budget{margin-top:10px;padding:10px 12px;border:1px solid #ffffff0a;border-radius:11px;background:#ffffff17;backdrop-filter:blur(3px)}
.travel-summary-content .daily-budget span{font-size:9px}.travel-summary-content .daily-budget b{margin-top:4px;font-size:15px}.travel-summary-content .daily-budget small{font-size:10px}
.travel-summary-content .fund-label{margin-top:11px;font-size:11px}.travel-summary-content .fund-track{height:7px;margin-top:6px}.travel-summary-content .fund-meta{margin-top:7px;font-size:9px}
.country-FR .fund-track i{background:linear-gradient(90deg,#002395 0%,#f4f4f4 52%,#ed2939 100%)}
.country-CH .fund-track i{background:linear-gradient(90deg,#ff0000 0%,#fff 58%,#ff0000 100%)}
.country-DE .fund-track i{background:linear-gradient(90deg,#111 0%,#dd0000 52%,#ffce00 100%)}
.country-JP .fund-track i{background:linear-gradient(90deg,#fff 0%,#bc002d 48%,#fff 100%)}
.country-HK .fund-track i{background:linear-gradient(90deg,#de2910 0%,#ffde00 100%)}
.country-all .fund-track i{background:linear-gradient(90deg,#002395 0%,#f4f4f4 30%,#ed2939 48%,#ff0000 66%,#fff 82%,#ff0000 100%)}
.stub-action{display:flex;align-items:center;gap:8px}.barcode{display:flex;height:24px;align-items:flex-end;gap:2px}.barcode i{display:block;background:#fff;border-radius:1px}
.ticket-top{height:44px;box-sizing:border-box}
.perforation{position:absolute;left:0;right:0;z-index:5;height:0}
.perforation:not(.lower){top:33px}
.perforation.lower{bottom:56px}
.asset-title{display:block}
.asset-title>span{display:block;margin-bottom:5px;color:#ffbd14;font-size:12px;line-height:1.2}
.asset-title>strong{display:block;text-align:left;font-size:25px}
.combined .ticket-main{background:linear-gradient(145deg,#12354c 0%,#17606a 58%,#1b485f 100%)}
.country-assets{display:flex;gap:8px;overflow-x:auto;scroll-snap-type:x mandatory;overscroll-behavior-x:contain;border-radius:10px;background:#ffffff0d;scrollbar-width:none}
.country-assets::-webkit-scrollbar{display:none}
.country-assets>div.country-asset-card{flex:0 0 calc(50% - 4px);min-width:0;padding:9px 10px;scroll-snap-align:start;background-image:var(--asset-image);background-position:center;background-size:cover}
.country-assets>div.country-asset-card::before{background:var(--asset-theme);opacity:.84}
.country-assets>div.country-asset-card span{font-size:8px;white-space:nowrap}.country-assets>div.country-asset-card b{font-size:13px;white-space:nowrap}.country-assets>div.country-asset-card small{font-size:7px;white-space:nowrap}
.country-all .fund-track i{background:linear-gradient(90deg,#79d3d8 0%,#f8d56b 55%,#f29a55 100%)}
</style>
