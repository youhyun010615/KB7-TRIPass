<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import MonthlyFundTicket from '@/components/savings/MonthlyFundTicket.vue'
import { useMonthlyFundStore } from '@/stores/monthlyFund'
import { useTravelStore } from '@/stores/travel'

const router = useRouter()
const fund = useMonthlyFundStore()
const travel = useTravelStore()

const countryThemes = [
  { code: 'FR', name: '프랑스', city: '파리', flag: '🇫🇷', ticketGradient: 'linear-gradient(115deg,#12377f,#0587ef)' },
  { code: 'CH', name: '스위스', city: '인터라켄', flag: '🇨🇭', ticketGradient: 'linear-gradient(115deg,#9c1034,#e32835)' },
  { code: 'DE', name: '독일', city: '베를린', flag: '🇩🇪', ticketGradient: 'linear-gradient(115deg,#151515,#db1111)' },
  { code: 'JP', name: '일본', city: '도쿄', flag: '🇯🇵', ticketGradient: 'linear-gradient(115deg,#a8155c,#f04c98)' },
  { code: 'HK', name: '홍콩', city: '홍콩', flag: '🇭🇰', ticketGradient: 'linear-gradient(115deg,#971827,#d62d3c)' },
]

const initialCode = travel.selectedCountryCodes[0] || 'FR'
const selectedCode = ref(initialCode)
const selectedCountry = computed(() => countryThemes.find((item) => item.code === selectedCode.value) || countryThemes[0])
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
</script>

<template>
  <main class="monthly-page">
    <div class="page-shell">
      <header class="app-title">
        <button aria-label="뒤로가기" @click="router.back()">‹</button>
        <h1>이달의 자금 체크</h1>
        <select v-model="selectedCode" aria-label="여행 국가 선택">
          <option v-for="country in countryThemes" :key="country.code" :value="country.code">
            {{ country.flag }} {{ country.city }}
          </option>
        </select>
      </header>

      <MonthlyFundTicket :country="selectedCountry" :income="fund.monthlyIncome" :expense="fund.fixedExpense" />

      <section class="card summary-card">
        <h2>자금 분배 요약 <span>i</span></h2>
        <div class="summary-grid">
          <div><strong>{{ money(fund.availableFunds) }}</strong><small>분배 가능 금액</small></div>
          <div><strong>{{ money(fund.categoryTargetTotal) }}</strong><small>카테고리 목표 총합</small></div>
          <div><strong>{{ money(fund.freeFunds) }}</strong><small>여유자금</small></div>
        </div>
        <p>ⓘ 월급에서 고정지출과 카테고리 목표를 뺀 값이에요.</p>
      </section>

      <section class="card category-card">
        <h2>카테고리별 자금 현황</h2>
        <button
          v-for="item in fund.categorySummaries"
          :key="item.id"
          class="category-row"
          @click="router.push(`/savings/monthly/categories/${item.id}`)"
        >
          <span class="category-icon" :style="{ background: `${item.color}18` }">{{ item.icon }}</span>
          <span class="category-info">
            <b>{{ item.name }}</b>
            <small>{{ money(item.spent) }} / {{ money(item.target) }}</small>
            <i><em :style="{ width: `${item.percent}%`, background: item.color }" /></i>
          </span>
          <strong :style="{ color: item.color }">{{ item.percent }}% ›</strong>
        </button>

        <button class="prepaid-link" @click="router.push('/asset/prepaid')">
          <span>✈️</span><b>여행비 사전 지출</b><small>목록 보기 ›</small>
        </button>
      </section>

      <button class="primary-cta" @click="router.push('/savings/monthly/categories')">카테고리 목표 수정</button>
    </div>
    <BottomNav />
  </main>
</template>

<style scoped>
.monthly-page { min-height: 100vh; padding-bottom: 88px; background: #e9eef6; color: #111827; }
.page-shell { width: min(100%, 390px); min-height: calc(100vh - 88px); margin: 0 auto; padding: 44px 16px 24px; background: #f7f4ee; }
.app-title { display: grid; grid-template-columns: 30px 1fr auto; align-items: center; margin-bottom: 18px; }
.app-title button { font-size: 28px; text-align: left; }
.app-title h1 { font-size: 20px; font-weight: 900; }
.app-title select { max-width: 92px; padding: 6px 4px; border: 1px solid #e5eaf2; border-radius: 999px; background: white; color: #475569; font-size: 10px; }
.card { margin-top: 14px; padding: 18px 16px; border: 1px solid #edf0f5; border-radius: 18px; background: #fff; box-shadow: 0 7px 16px rgba(30, 52, 98, .07); }
.card h2 { font-size: 16px; font-weight: 900; }
.summary-card h2 span { display: inline-grid; width: 14px; height: 14px; place-items: center; border-radius: 50%; background: #eef2f7; color: #94a3b8; font-size: 9px; }
.summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); margin-top: 19px; }
.summary-grid div { text-align: center; }
.summary-grid div + div { border-left: 1px solid #edf0f5; }
.summary-grid strong { display: block; color: #0066ff; font-size: 15px; font-weight: 900; white-space: nowrap; letter-spacing: -.5px; }
.summary-grid div:last-child strong, .summary-grid div:last-child small { color: #19a66d; }
.summary-grid small { display: block; margin-top: 7px; color: #397cf0; font-size: 9px; }
.summary-card > p { margin-top: 16px; padding-top: 11px; border-top: 1px dashed #dfe5ee; color: #98a3b6; font-size: 9px; }
.category-card { padding-bottom: 10px; }
.category-row { display: grid; grid-template-columns: 36px 1fr 48px; align-items: center; gap: 10px; width: 100%; padding: 11px 0; text-align: left; }
.category-icon { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 50%; font-size: 14px; }
.category-info b { display: block; font-size: 12px; }
.category-info small { display: block; margin-top: 3px; color: #475569; font-size: 8px; }
.category-info i { display: block; height: 5px; margin-top: 7px; overflow: hidden; border-radius: 9px; background: #e9edf3; }
.category-info em { display: block; height: 100%; border-radius: inherit; }
.category-row > strong { text-align: right; font-size: 11px; }
.prepaid-link { display: grid; grid-template-columns: 36px 1fr auto; align-items: center; width: 100%; margin-top: 4px; padding: 14px 0 6px; border-top: 1px dashed #dfe5ee; text-align: left; }
.prepaid-link b { font-size: 12px; }
.prepaid-link small { color: #94a3b8; font-size: 10px; }
.primary-cta { width: 100%; height: 52px; margin-top: 14px; border-radius: 13px; background: #173d89; color: white; font-size: 14px; font-weight: 900; box-shadow: 0 8px 16px rgba(23,61,137,.16); }
</style>
