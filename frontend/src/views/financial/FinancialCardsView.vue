<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

import BottomNav from '@/components/common/BottomNav.vue'
import ProductTicket from '@/components/financial/ProductTicket.vue'
import { useTravelCardsStore } from '@/stores/travelCards'

const router = useRouter()
const travelCardsStore = useTravelCardsStore()

const currencyOptions = [
  { code: '', name: '전체 통화' },
  { code: 'USD', name: '미국 달러' },
  { code: 'JPY', name: '일본 엔' },
  { code: 'EUR', name: '유로' },
  { code: 'GBP', name: '영국 파운드' },
  { code: 'CNH', name: '중국 위안' },
  { code: 'HKD', name: '홍콩 달러' },
  { code: 'SGD', name: '싱가포르 달러' },
  { code: 'THB', name: '태국 바트' },
  { code: 'VND', name: '베트남 동' },
  { code: 'CHF', name: '스위스 프랑' },
  { code: 'AUD', name: '호주 달러' },
  { code: 'CAD', name: '캐나다 달러' },
  { code: 'NZD', name: '뉴질랜드 달러' },
  { code: 'TWD', name: '대만 달러' },
  { code: 'IDR', name: '인도네시아 루피아' },
  { code: 'MYR', name: '말레이시아 링깃' },
  { code: 'PHP', name: '필리핀 페소' },
  { code: 'AED', name: '아랍에미리트 디르함' },
  { code: 'SAR', name: '사우디아라비아 리얄' },
  { code: 'SEK', name: '스웨덴 크로나' },
  { code: 'DKK', name: '덴마크 크로네' },
  { code: 'NOK', name: '노르웨이 크로네' },
]

const companyColors = {
  KB국민카드: '#e4ad00',
  하나카드: '#12aa92',
  우리카드: '#2472c8',
  신한카드: '#2865e8',
}

function getCompanyColor(cardCompany) {
  return companyColors[cardCompany] || '#315b9f'
}

function getCompanyCode(cardCompany) {
  if (cardCompany?.includes('KB')) {
    return 'KB'
  }

  if (cardCompany?.includes('하나')) {
    return 'H'
  }

  if (cardCompany?.includes('우리')) {
    return 'W'
  }

  if (cardCompany?.includes('신한')) {
    return 'S'
  }

  return cardCompany?.charAt(0) || 'C'
}

function getErrorMessage() {
  return (
      travelCardsStore.errorMessage ||
      '트래블카드 목록을 불러오지 못했습니다.'
  )
}

async function loadCards() {
  try {
    await travelCardsStore.loadCards()
  } catch {
    // 오류 내용은 Store의 errorMessage로 표시합니다.
  }
}

async function searchCards() {
  await loadCards()
}

async function applyFilters() {
  await loadCards()
}

async function resetFilters() {
  travelCardsStore.resetFilters()
  await loadCards()
}

function openCardDetail(cardId) {
  router.push(`/financial/cards/${cardId}`)
}

function openComparison() {
  router.push('/financial/cards/compare')
}

function toggleComparison(cardId) {
  const wasCompared =
      travelCardsStore.isCompared(cardId)

  const changed =
      travelCardsStore.toggleComparisonCard(cardId)

  if (!changed && !wasCompared) {
    window.alert(
        '비교할 트래블카드는 최대 3개까지 선택할 수 있습니다.',
    )
  }
}

onMounted(loadCards)
</script>

<template>
  <main class="page">
    <div class="shell">
      <header class="page-header">
        <button
            type="button"
            class="back-button"
            aria-label="뒤로 가기"
            @click="router.back()"
        >
          ‹
        </button>

        <h1>트래블카드 찾기</h1>

        <span />
      </header>

      <ProductTicket
          title="여행에 맞는 트래블카드"
          subtitle="결제 수수료와 환전 혜택을 비교해 보세요"
      />

      <button
          type="button"
          class="comparison-link"
          @click="openComparison"
      >
        <span class="comparison-icon">▣</span>

        <span class="comparison-copy">
          <b>카드 비교</b>
          <small>최대 3개까지 한눈에 비교</small>
        </span>

        <em>
          {{ travelCardsStore.comparedCardCount }}개 선택 ›
        </em>
      </button>

      <form
          class="search-form"
          @submit.prevent="searchCards"
      >
        <span aria-hidden="true">⌕</span>

        <input
            v-model="travelCardsStore.keyword"
            type="search"
            placeholder="카드명·카드사·은행명으로 검색"
            aria-label="트래블카드 검색어"
        />

        <button
            type="submit"
            :disabled="travelCardsStore.listLoading"
        >
          검색
        </button>
      </form>

      <section class="filters">
        <label>
          <span>지원 통화</span>

          <select
              v-model="travelCardsStore.currencyCode"
              @change="applyFilters"
          >
            <option
                v-for="currency in currencyOptions"
                :key="currency.code || 'ALL'"
                :value="currency.code"
            >
              {{ currency.name }}
              {{ currency.code ? `(${currency.code})` : '' }}
            </option>
          </select>
        </label>

        <label>
          <span>즉시 사용</span>

          <select
              v-model="travelCardsStore.instantUse"
              @change="applyFilters"
          >
            <option :value="null">전체</option>
            <option :value="true">가능</option>
            <option :value="false">별도 계좌 필요</option>
          </select>
        </label>

        <label>
          <span>교통카드</span>

          <select
              v-model="travelCardsStore.transitCard"
              @change="applyFilters"
          >
            <option :value="null">전체</option>
            <option :value="true">지원</option>
            <option :value="false">미지원</option>
          </select>
        </label>
      </section>

      <div class="list-heading">
        <h2>트래블카드</h2>

        <button
            type="button"
            @click="resetFilters"
        >
          필터 초기화
        </button>
      </div>

      <section
          v-if="travelCardsStore.listLoading"
          class="state-card"
      >
        <b>트래블카드를 불러오고 있어요.</b>
        <p>잠시만 기다려 주세요.</p>
      </section>

      <section
          v-else-if="travelCardsStore.errorMessage"
          class="state-card error"
      >
        <b>목록을 불러오지 못했습니다.</b>
        <p>{{ getErrorMessage() }}</p>

        <button
            type="button"
            @click="loadCards"
        >
          다시 시도
        </button>
      </section>

      <section
          v-else-if="!travelCardsStore.cards.length"
          class="state-card"
      >
        <b>조건에 맞는 카드가 없습니다.</b>
        <p>검색어나 필터 조건을 변경해 주세요.</p>

        <button
            type="button"
            @click="resetFilters"
        >
          전체 카드 보기
        </button>
      </section>

      <section
          v-else
          class="card-list"
      >
        <article
            v-for="card in travelCardsStore.cards"
            :key="card.id"
            class="card-item"
        >
          <button
              type="button"
              class="card-main"
              @click="openCardDetail(card.id)"
          >
            <i
                :style="{
                background: getCompanyColor(
                  card.cardCompany,
                ),
              }"
            >
              {{ getCompanyCode(card.cardCompany) }}
            </i>

            <span class="card-copy">
              <b>{{ card.cardName }}</b>

              <small>
                {{ card.cardCompany }}
                <template v-if="card.bankName">
                  · {{ card.bankName }}
                </template>
              </small>

              <small>
                지원 통화
                {{ card.supportedCurrencyCount }}개
                ·
                {{
                  card.transitCard
                      ? '교통카드 지원'
                      : '교통카드 미지원'
                }}
              </small>
            </span>

            <strong>
              {{ card.foreignCurrencyHoldingLimit }}
            </strong>

            <em>›</em>
          </button>

          <div class="card-footer">
            <span
                :class="{
                available: card.instantUse,
              }"
            >
              {{
                card.instantUse
                    ? '별도 계좌 개설 없이 이용 가능'
                    : '연계 외화계좌 필요'
              }}
            </span>

            <button
                type="button"
                class="comparison-toggle"
                :class="{
                selected:
                  travelCardsStore.isCompared(card.id),
              }"
                @click="toggleComparison(card.id)"
            >
              {{
                travelCardsStore.isCompared(card.id)
                    ? '비교 선택됨'
                    : '비교 추가'
              }}
            </button>
          </div>
        </article>
      </section>

      <BottomNav />
    </div>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #e7ecf4;
  color: #10192d;
}

.shell {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: auto;
  padding: 52px 18px 100px;
  background: #f7f5ef;
}

.page-header {
  display: grid;
  grid-template-columns: 32px 1fr 32px;
  align-items: center;
  margin-bottom: 16px;
}

.back-button {
  border: 0;
  background: none;
  font-size: 25px;
  text-align: left;
}

.page-header h1 {
  font-size: 18px;
  font-weight: 900;
  text-align: center;
}

.comparison-link {
  display: grid;
  width: 100%;
  grid-template-columns: 36px 1fr auto;
  align-items: center;
  gap: 9px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #e0e6ed;
  border-radius: 13px;
  background: #fff;
  text-align: left;
}

.comparison-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  background: #edf3ff;
  color: #174494;
}

.comparison-copy b,
.comparison-copy small {
  display: block;
}

.comparison-copy b {
  font-size: 11px;
}

.comparison-copy small {
  margin-top: 3px;
  color: #8792a2;
  font-size: 8px;
}

.comparison-link em {
  color: #2870dc;
  font-size: 9px;
  font-style: normal;
  font-weight: 900;
}

.search-form {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 9px 8px 12px;
  border: 1px solid #e1e6ed;
  border-radius: 11px;
  background: #fff;
  color: #9aa5b5;
}

.search-form input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: none;
  background: transparent;
  font-size: 10px;
}

.search-form button {
  padding: 7px 11px;
  border-radius: 8px;
  background: #1c58ae;
  color: #fff;
  font-size: 9px;
  font-weight: 800;
}

.search-form button:disabled {
  background: #aeb9ca;
}

.filters {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 10px;
}

.filters label:first-child {
  grid-column: 1 / 3;
}

.filters label {
  display: grid;
  gap: 5px;
}

.filters label > span {
  color: #738095;
  font-size: 8px;
  font-weight: 700;
}

.filters select {
  width: 100%;
  padding: 9px 10px;
  border: 1px solid #dfe5ed;
  border-radius: 10px;
  outline: none;
  background: #fff;
  color: #26354b;
  font-size: 9px;
}

.list-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 2px 8px;
}

.list-heading h2 {
  font-size: 13px;
}

.list-heading button {
  color: #2d6bc8;
  font-size: 8px;
  font-weight: 700;
}

.card-list {
  display: grid;
  gap: 9px;
}

.card-item {
  overflow: hidden;
  border: 1px solid #e1e6ed;
  border-radius: 14px;
  background: #fff;
}

.card-main {
  display: grid;
  width: 100%;
  grid-template-columns: 40px minmax(0, 1fr) auto 8px;
  align-items: center;
  gap: 9px;
  padding: 13px 12px;
  text-align: left;
}

.card-main i {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 10px;
  color: #fff;
  font-size: 9px;
  font-style: normal;
  font-weight: 900;
}

.card-copy {
  min-width: 0;
}

.card-copy b,
.card-copy small {
  display: block;
}

.card-copy b {
  overflow: hidden;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-copy small {
  margin-top: 4px;
  color: #8995a5;
  font-size: 7px;
  line-height: 1.4;
}

.card-main strong {
  max-width: 92px;
  color: #1768d3;
  font-size: 8px;
  line-height: 1.4;
  text-align: right;
}

.card-main em {
  color: #9ba6b5;
  font-size: 18px;
  font-style: normal;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-top: 1px solid #edf0f4;
  background: #fbfcfe;
}

.card-footer > span {
  color: #8d98a7;
  font-size: 7px;
}

.card-footer > span.available {
  color: #07966c;
  font-weight: 700;
}

.comparison-toggle {
  padding: 5px 9px;
  border-radius: 9px;
  background: #edf2fa;
  color: #68788e;
  font-size: 7px;
}

.comparison-toggle.selected {
  background: #dceaff;
  color: #176cdd;
  font-weight: 900;
}

.state-card {
  margin-top: 10px;
  padding: 48px 20px;
  border: 1px solid #e1e6ed;
  border-radius: 14px;
  background: #fff;
  text-align: center;
}

.state-card b {
  display: block;
  font-size: 11px;
}

.state-card p {
  margin-top: 7px;
  color: #8592a4;
  font-size: 9px;
  line-height: 1.5;
}

.state-card button {
  margin-top: 14px;
  padding: 9px 14px;
  border-radius: 9px;
  background: #173f8d;
  color: #fff;
  font-size: 8px;
  font-weight: 800;
}

.state-card.error {
  border-color: #f2caca;
  background: #fffafa;
}

.state-card.error b {
  color: #d44747;
}
</style>