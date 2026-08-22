<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'

import { useTravelCardsStore } from '@/stores/travelCards'
import { getTravelCardImage } from '@/utils/travelCard'

const router = useRouter()
const travelCardsStore = useTravelCardsStore()

const {
  comparisonCards,
  comparisonLoading,
  errorMessage,
} = storeToRefs(travelCardsStore)

// 처음부터 노출되는 핵심 3항목
const benefitGroups = [
  {
    key: 'appliedRateInfo',
    title: '적용 환율',
    value: (card) => card.appliedRateInfo,
    preference: 'high',
  },
  {
    key: 'paymentFee',
    title: '해외 결제 수수료',
    value: (card) => card.paymentFee,
    preference: 'low',
  },
  {
    key: 'withdrawalFee',
    title: '해외 ATM 수수료',
    value: (card) => card.withdrawalFee,
    preference: 'low',
  },
]

// "나머지 항목 비교" 버튼을 눌러야 보이는 추가 항목
const extraBenefitGroups = [
  {
    key: 'exchangeFee',
    title: '환전 수수료',
    value: (card) => card.exchangeFee,
    preference: 'high',
  },
  {
    key: 'reExchangeFee',
    title: '재환전 수수료',
    value: (card) => card.reExchangeFee,
    preference: 'high',
  },
  {
    key: 'foreignCurrencyHoldingLimit',
    title: '외화 보유 한도',
    value: (card) => card.foreignCurrencyHoldingLimit,
    preference: 'high',
  },
  {
    key: 'supportedCurrencyCount',
    title: '지원 통화 개수',
    value: (card) =>
        card.supportedCurrencyCount ? `${card.supportedCurrencyCount}종` : null,
    preference: 'high',
  },
  {
    key: 'instantUse',
    title: '즉시 사용 가능 여부',
    value: (card) => (card.instantUse ? '계좌 개설 없이 즉시 사용' : '계좌 개설 필요'),
    score: (card) => (card.instantUse ? 1 : 0),
  },
  {
    key: 'settlementType',
    title: '해외 결제 처리 방식',
    value: (card) =>
        card.settlementType === 'DIRECT' ? '현지통화 직접 결제' : '달러 환산 후 결제',
    score: (card) => (card.settlementType === 'DIRECT' ? 1 : 0),
  },
  {
    key: 'autoChargeSupported',
    title: '자동 충전 지원',
    value: (card) => (card.autoChargeSupported ? '지원' : '미지원'),
    score: (card) => (card.autoChargeSupported ? 1 : 0),
  },
  {
    key: 'transitCard',
    title: '교통카드 지원',
    value: (card) => (card.transitCard ? '지원' : '미지원'),
    score: (card) => (card.transitCard ? 1 : 0),
  },
  {
    key: 'requiredAccount',
    title: '필요 계좌·서비스',
    value: (card) => card.requiredAccount,
  },
]

const isExpanded = ref(false)

const visibleGroups = computed(() =>
    isExpanded.value
        ? [...benefitGroups, ...extraBenefitGroups]
        : benefitGroups,
)

function toggleExpanded() {
  isExpanded.value = !isExpanded.value
}

const canOpenFirstCard = computed(
    () => comparisonCards.value.length > 0,
)

function displayValue(value) {
  if (
      value === null ||
      value === undefined ||
      String(value).trim() === ''
  ) {
    return '-'
  }

  return value
}

function comparisonScore(value, preference) {
  const text = String(displayValue(value)).replace(/,/g, '')

  if (/무료|전액\s*면제|수수료\s*없음/.test(text)) {
    return 0
  }

  const numbers = text.match(/\d+(?:\.\d+)?/g)?.map(Number) ?? []

  if (numbers.length === 0) {
    return null
  }

  return preference === 'high'
      ? Math.max(...numbers)
      : numbers[0]
}

function isBestBenefit(card, group) {
  const scoreOf = (item) =>
      group.score
          ? group.score(item)
          : comparisonScore(group.value(item), group.preference)

  const scores = comparisonCards.value
      .map(scoreOf)
      .filter((score) => score !== null && score !== undefined)
  const cardScore = scoreOf(card)

  if (
      cardScore === null ||
      cardScore === undefined ||
      scores.length === 0
  ) {
    return false
  }

  const bestScore = (group.score || group.preference === 'high')
      ? Math.max(...scores)
      : Math.min(...scores)

  return cardScore === bestScore
}


function getCardCode(card) {
  const source =
      card.cardCompany ||
      card.bankName ||
      card.cardName ||
      'T'

  const normalizedSource = source
      .replace(/카드|은행|체크|트래블/gi, '')
      .trim()

  if (!normalizedSource) {
    return 'T'
  }

  return normalizedSource
      .slice(0, 2)
      .toUpperCase()
}

function getCardColor(cardId) {
  const colors = [
    '#0db89f',
    '#2868f5',
    '#f2ad00',
    '#e54a45',
    '#794bd8',
    '#1477aa',
  ]

  const normalizedId = Number(cardId)

  if (!Number.isInteger(normalizedId)) {
    return colors[0]
  }

  return colors[
  Math.abs(normalizedId - 1) % colors.length
      ]
}

async function loadComparison() {
  try {
    await travelCardsStore.loadComparison()
  } catch {
    // 오류 문구는 Store의 errorMessage를 사용합니다.
  }
}

function removeCard(cardId) {
  travelCardsStore.removeComparisonCard(cardId)
}

function openCardDetail(cardId) {
  router.push(`/financial/cards/${cardId}`)
}

function openFirstCardDetail() {
  const firstCard = comparisonCards.value[0]

  if (!firstCard) {
    return
  }

  openCardDetail(firstCard.id)
}

function clearComparison() {
  travelCardsStore.clearComparison()
}

onMounted(loadComparison)
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

        <div>
          <small>TRIPASS CARD FINDER</small>
          <h1>카드 비교</h1>
        </div>

        <span />
      </header>

      <section class="selection-section">
        <div class="selection-heading">
          <strong>비교할 카드 선택</strong>

          <span class="selection-count">
            {{ comparisonCards.length }}/3 선택
          </span>
        </div>

        <div class="selected-card-list">
          <article
              v-for="card in comparisonCards"
              :key="card.id"
              class="selected-card"
          >
            <button
                type="button"
                class="remove-button"
                :aria-label="`${card.cardName} 비교에서 제거`"
                @click="removeCard(card.id)"
            >
              ×
            </button>

            <button
                type="button"
                class="selected-card-content"
                @click="openCardDetail(card.id)"
            >
              <img
                  v-if="getTravelCardImage(card.cardCompany)"
                  class="card-thumb"
                  :src="getTravelCardImage(card.cardCompany)"
                  :alt="`${card.cardCompany} ${card.cardName}`"
              />
              <span
                  v-else
                  class="card-symbol"
                  :style="{
                  backgroundColor: getCardColor(card.id),
                }"
              >
                {{ getCardCode(card) }}
              </span>

              <strong>{{ card.cardName }}</strong>
              <small>{{ card.cardCompany }}</small>
            </button>
          </article>

          <button
              v-if="comparisonCards.length < 3"
              type="button"
              class="add-card-button"
              @click="router.push('/financial/cards')"
          >
            <strong>＋</strong>
            <span>카드 추가</span>
          </button>
        </div>
      </section>

      <section
          v-if="comparisonLoading"
          class="state-panel"
      >
        <span class="loader" />
        <strong>카드 정보를 비교하고 있어요.</strong>
      </section>

      <section
          v-else-if="errorMessage"
          class="state-panel error-panel"
      >
        <strong>
          비교 정보를 불러오지 못했습니다.
        </strong>

        <p>{{ errorMessage }}</p>

        <div class="state-actions">
          <button
              type="button"
              @click="loadComparison"
          >
            다시 시도
          </button>

          <button
              type="button"
              class="secondary-button"
              @click="clearComparison"
          >
            선택 초기화
          </button>
        </div>
      </section>

      <section
          v-else-if="comparisonCards.length === 0"
          class="state-panel empty-panel"
      >
        <strong>비교할 카드를 선택해 주세요.</strong>

        <p>
          최대 3개의 트래블카드를 선택하여
          혜택과 이용 조건을 비교할 수 있습니다.
        </p>

        <button
            type="button"
            @click="router.push('/financial/cards')"
        >
          카드 선택하기
        </button>
      </section>

      <section
          v-else
          class="comparison-section"
      >
        <div class="comparison-heading">
          <div>
            <h2>핵심 혜택 비교</h2>
            <p>
              항목마다 어느 카드가 유리한지 바로 보여드려요.
            </p>
          </div>

          <button
              type="button"
              class="clear-button"
              @click="clearComparison"
          >
            전체 해제
          </button>
        </div>

        <div class="benefit-card-list">
          <article
              v-for="group in visibleGroups"
              :key="group.key"
              class="benefit-card"
          >
            <h3>{{ group.title }}</h3>

            <button
                v-for="card in comparisonCards"
                :key="card.id"
                type="button"
                class="benefit-row"
                @click="openCardDetail(card.id)"
            >
              <span class="benefit-identity">
                <b class="benefit-name">{{ card.cardName }}</b>
                <small class="benefit-company">{{ card.cardCompany }}</small>
              </span>

              <span class="benefit-result">
                {{ displayValue(group.value(card)) }}
              </span>

              <span
                  v-if="isBestBenefit(card, group)"
                  class="best-badge"
              >
                최고
              </span>
            </button>
          </article>
        </div>

        <button
            type="button"
            class="expand-button"
            @click="toggleExpanded"
        >
          {{
            isExpanded
                ? '접기'
                : `나머지 ${extraBenefitGroups.length}개 항목 비교`
          }}
          <i :class="{ open: isExpanded }">⌄</i>
        </button>

        <p class="comparison-guide">
          카드명을 누르면 해당 카드의 상세 정보를
          확인할 수 있습니다.
        </p>
      </section>

      <button
          type="button"
          class="detail-button"
          :disabled="!canOpenFirstCard"
          @click="openFirstCardDetail"
      >
        {{
          comparisonCards.length === 1
              ? '선택한 카드 상세 보기'
              : '첫 번째 카드 상세 보기'
        }}
      </button>

    </div>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #e9eef7;
  color: #101b33;
}

.shell {
  width: min(100%, 430px);
  min-height: 100vh;
  margin: 0 auto;
  padding: 14px 18px 32px;
  background: linear-gradient(180deg, #f4f7ff 0%, #edf3fc 100%);
}

button {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.page-header {
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: center;
  margin-bottom: 17px;
}

.page-header > div {
  text-align: center;
}

.page-header small {
  color: #2f6fd8;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.13em;
}

.page-header h1 {
  margin-top: 2px;
  font-size: 18px;
  font-weight: 900;
}

.back-button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  background: #fff;
  color: #16366f;
  font-size: 24px;
  box-shadow: 0 5px 16px rgba(36, 72, 117, 0.07);
}

.selection-section {
  margin-top: 4px;
}

.selection-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.selection-heading strong {
  font-size: 14px;
  font-weight: 800;
}

.selection-count {
  padding: 6px 11px;
  border-radius: 999px;
  background: #173f8d;
  color: #fff;
  font-size: 10.5px;
  font-weight: 800;
}

.selected-card-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 9px;
  margin-top: 13px;
}

.selected-card,
.add-card-button {
  position: relative;
  min-height: 122px;
  overflow: hidden;
  border: 1px solid #e1e6ed;
  border-radius: 17px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(38, 62, 101, 0.05);
}

.selected-card-content {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 120px;
  padding: 14px 8px 10px;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  background: transparent;
}

.card-thumb {
  width: 40px;
  height: 40px;
  border: 1px solid #e7ecf3;
  border-radius: 12px;
  background: #f5f7fa;
  object-fit: cover;
}

.card-symbol {
  display: grid;
  min-width: 38px;
  height: 28px;
  padding: 0 7px;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  font-size: 9px;
  font-weight: 900;
}

.selected-card strong {
  display: -webkit-box;
  margin-top: 9px;
  overflow: hidden;
  font-size: 10px;
  font-weight: 800;
  line-height: 1.35;
  text-align: center;
  word-break: keep-all;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.selected-card small {
  margin-top: 4px;
  color: #8a97aa;
  font-size: 8.5px;
}

.remove-button {
  position: absolute;
  z-index: 1;
  top: 6px;
  right: 8px;
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border-radius: 50%;
  background: #f1f3f6;
  color: #78869a;
  font-size: 15px;
  line-height: 1;
}

.add-card-button {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  border: 1.5px dashed #b9c9df;
  background: #f8fbff;
  color: #2f6fe9;
}

.add-card-button strong {
  font-size: 22px;
  font-weight: 400;
}

.add-card-button span {
  margin-top: 6px;
  font-size: 9.5px;
  font-weight: 700;
}

.state-panel {
  display: flex;
  min-height: 245px;
  margin-top: 16px;
  padding: 30px 20px;
  align-items: center;
  border: 1px solid #e1e6ed;
  border-radius: 22px;
  flex-direction: column;
  justify-content: center;
  background: #fff;
  box-shadow: 0 8px 22px rgba(38, 62, 101, 0.05);
  text-align: center;
}

.state-panel strong {
  font-size: 15px;
  font-weight: 800;
}

.state-panel p {
  margin: 9px 0 18px;
  color: #718096;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-line;
}

.state-panel button {
  padding: 12px 18px;
  border-radius: 12px;
  background: #173f8d;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.state-actions {
  display: flex;
  gap: 8px;
}

.state-panel .secondary-button {
  border: 1px solid #d2def0;
  background: #fff;
  color: #52657f;
}

.loader {
  width: 29px;
  height: 29px;
  margin-bottom: 15px;
  border: 3px solid #dae5f3;
  border-top-color: #1855ad;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.error-panel strong {
  color: #d64747;
}

.comparison-section {
  margin-top: 16px;
}

.comparison-heading {
  display: flex;
  margin: 0 2px 10px;
  align-items: flex-end;
  justify-content: space-between;
}

.comparison-heading h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 900;
}

.comparison-heading p {
  margin: 4px 0 0;
  color: #8190a5;
  font-size: 10px;
}

.clear-button {
  padding: 4px;
  background: transparent;
  color: #5a6478;
  font-size: 10.5px;
  font-weight: 700;
}

.benefit-card-list {
  display: grid;
  gap: 12px;
}

.benefit-card {
  padding: 17px 16px 7px;
  border: 1px solid #e8edf5;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 9px 24px rgba(38, 62, 101, 0.05);
}

.benefit-card h3 {
  margin: 0 0 8px;
  color: #111c34;
  font-size: 14px;
  font-weight: 900;
}

.benefit-row {
  display: grid;
  width: 100%;
  min-height: 59px;
  padding: 12px 0;
  grid-template-columns: minmax(76px, 108px) minmax(0, 1fr) 35px;
  gap: 8px;
  align-items: center;
  border-top: 1px solid #edf1f6;
  background: transparent;
  color: inherit;
  text-align: left;
}

.benefit-card h3 + .benefit-row {
  border-top: 0;
}

.benefit-identity {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.benefit-name {
  overflow: hidden;
  color: #1c2940;
  font-size: 12.5px;
  font-weight: 750;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.benefit-company {
  overflow: hidden;
  color: #8a97aa;
  font-size: 8px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.benefit-result {
  color: #173f8d;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.45;
  text-align: right;
  word-break: keep-all;
}

.best-badge {
  display: inline-flex;
  min-width: 35px;
  min-height: 25px;
  align-items: center;
  border-radius: 999px;
  background: #e8f8ef;
  color: #0a9b61;
  font-size: 9.5px;
  font-weight: 900;
  justify-content: center;
}

.benefit-row:not(:has(.best-badge))::after {
  width: 35px;
  content: '';
}

.expand-button {
  display: flex;
  width: 100%;
  min-height: 48px;
  margin-top: 12px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid #e8edf5;
  border-radius: 14px;
  background: #fff;
  color: #52657f;
  font-size: 12px;
  font-weight: 800;
  box-shadow: 0 9px 24px rgba(38, 62, 101, 0.05);
}

.expand-button i {
  font-style: normal;
  transition: transform 0.2s;
}

.expand-button i.open {
  transform: rotate(180deg);
}

.comparison-guide {
  margin: 10px 3px 0;
  color: #8090a5;
  font-size: 10px;
}

.detail-button {
  width: 100%;
  min-height: 52px;
  margin-top: 20px;
  border-radius: 14px;
  background: #173f8d;
  color: #fff;
  font-size: 14px;
  font-weight: 900;
  box-shadow: 0 10px 22px rgba(23, 63, 141, 0.16);
}

.detail-button:disabled {
  background: #adb8c8;
  box-shadow: none;
  cursor: default;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
