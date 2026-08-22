<script setup>
import { onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'

import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelCardsStore } from '@/stores/travelCards'
import { getSettlementTypeLabel, getTravelCardImage } from '@/utils/travelCard'

const router = useRouter()
const travelCardsStore = useTravelCardsStore()

const {
  comparisonCards,
  comparisonLoading,
  errorMessage,
} = storeToRefs(travelCardsStore)

const primaryComparisonGroups = [
  {
    label: '적용 환율',
    value: (card) => card.appliedRateInfo,
    preference: 'max',
    criteria: '우대율이 높을수록 최고',
    comparableValue: (value) => {
      const percentageMatch = String(value ?? '').match(
          /(\d+(?:\.\d+)?)\s*%/,
      )

      return percentageMatch
          ? Number(percentageMatch[1])
          : null
    },
  },
  {
    label: '해외 결제 수수료',
    value: (card) => card.paymentFee,
    preference: 'min',
    criteria: '수수료가 낮을수록 최고',
  },
  {
    label: '해외 ATM 수수료',
    value: (card) => card.withdrawalFee,
    preference: 'min',
    criteria: '수수료가 낮을수록 최고',
  },
]

const secondaryComparisonGroups = [
  {
    label: '발급·연계 은행',
    value: (card) => card.bankName,
  },
  {
    label: '필요 계좌',
    value: (card) => card.requiredAccount,
  },
  {
    label: '즉시 사용',
    value: (card) =>
        card.instantUse ? '가능' : '별도 계좌 필요',
  },
  {
    label: '결제 방식',
    value: (card) =>
        getSettlementTypeLabel(card.settlementType),
  },
  {
    label: '외화 보유한도',
    value: (card) => card.foreignCurrencyHoldingLimit,
  },
  {
    label: '환전 수수료',
    value: (card) => card.exchangeFee,
  },
  {
    label: '재환전 수수료',
    value: (card) => card.reExchangeFee,
  },
  {
    label: '자동 충전',
    value: (card) =>
        card.autoChargeSupported ? '지원' : '미지원',
  },
  {
    label: '교통카드',
    value: (card) =>
        card.transitCard ? '지원' : '미지원',
  },
  {
    label: '지원 통화',
    value: (card) =>
        `${card.supportedCurrencyCount ?? 0}개`,
  },
]

const showSecondaryGroups = ref(false)

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

function getCardNameParts(cardName) {
  return String(cardName ?? '').split(/\s*\+\s*/)
}

function getBenefitValueParts(value) {
  return String(displayValue(value)).split(/\s*\+\s*/)
}

function isLongBenefitValue(value) {
  return String(displayValue(value)).length >= 18
}

function getComparableNumber(value) {
  const normalizedValue = displayValue(value)
  const normalizedText = String(normalizedValue)
      .replaceAll(',', '')
      .trim()

  if (/면제|무료/.test(normalizedText)) {
    return 0
  }

  const numericMatch = normalizedText.match(
      /-?\d+(?:\.\d+)?/,
  )

  return numericMatch ? Number(numericMatch[0]) : null
}

function isBestValue(group, card) {
  if (!group.preference) {
    return false
  }

  const comparableValues = comparisonCards.value
      .map((comparisonCard) => ({
        cardId: comparisonCard.id,
        value: group.comparableValue
            ? group.comparableValue(group.value(comparisonCard))
            : getComparableNumber(group.value(comparisonCard)),
      }))
      .filter(({ value }) => Number.isFinite(value))

  if (comparableValues.length < 2) {
    return false
  }

  const bestValue = group.preference === 'max'
      ? Math.max(...comparableValues.map(({ value }) => value))
      : Math.min(...comparableValues.map(({ value }) => value))

  const currentValue = comparableValues.find(
      ({ cardId }) => cardId === card.id,
  )?.value

  return currentValue === bestValue
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

          <div class="selection-actions">
            <button
                v-if="comparisonCards.length"
                type="button"
                class="clear-button"
                @click="clearComparison"
            >
              전체 해제
            </button>

            <span class="selection-count">
              {{ comparisonCards.length }}/3 선택
            </span>
          </div>
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

        </div>

        <div class="benefit-card-list">
          <article
              v-for="group in primaryComparisonGroups"
              :key="group.label"
              class="benefit-card"
              :class="{
                'rate-benefit-card': group.label === '적용 환율',
              }"
          >
            <div class="benefit-group-heading">
              <h3>{{ group.label }}</h3>
              <small>{{ group.criteria }}</small>
            </div>

            <button
                v-for="card in comparisonCards"
                :key="card.id"
                type="button"
                class="benefit-row"
                @click="openCardDetail(card.id)"
            >
              <span class="benefit-card-name">
                <template
                    v-for="(namePart, index) in getCardNameParts(card.cardName)"
                    :key="`${card.id}-${index}`"
                >
                  <span>{{ index ? `+ ${namePart}` : namePart }}</span>
                </template>
                <small>{{ card.cardCompany }}</small>
              </span>

              <span
                  class="benefit-value-wrap"
                  :class="{
                    'long-value': isLongBenefitValue(group.value(card)),
                  }"
              >
                <strong>
                  <template
                      v-for="(valuePart, index) in getBenefitValueParts(group.value(card))"
                      :key="`${card.id}-${group.label}-${index}`"
                  >
                    <span>
                      {{ index ? `+ ${valuePart}` : valuePart }}
                    </span>
                  </template>
                </strong>
                <em v-if="isBestValue(group, card)">
                  최고
                </em>
              </span>
            </button>
          </article>

          <button
              type="button"
              class="more-benefit-button"
              :aria-expanded="showSecondaryGroups"
              @click="showSecondaryGroups = !showSecondaryGroups"
          >
            <span class="more-benefit-label">
              {{
                showSecondaryGroups
                    ? '비교 항목 접기'
                    : `나머지 ${secondaryComparisonGroups.length}개 항목 비교`
              }}
            </span>
            <svg
                class="more-benefit-chevron"
                :class="{ opened: showSecondaryGroups }"
                viewBox="0 0 20 20"
                aria-hidden="true"
            >
              <path
                  d="M5 7.5 10 12.5 15 7.5"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
              />
            </svg>
          </button>

          <div
              v-if="showSecondaryGroups"
              class="secondary-benefit-list"
          >
            <article
                v-for="group in secondaryComparisonGroups"
                :key="group.label"
                class="benefit-card compact-benefit-card"
            >
              <h3>{{ group.label }}</h3>

              <button
                  v-for="card in comparisonCards"
                  :key="card.id"
                  type="button"
                  class="benefit-row"
                  @click="openCardDetail(card.id)"
              >
                <span class="benefit-card-name">
                  <template
                      v-for="(namePart, index) in getCardNameParts(card.cardName)"
                      :key="`${card.id}-${group.label}-name-${index}`"
                  >
                    <span>{{ index ? `+ ${namePart}` : namePart }}</span>
                  </template>
                  <small>{{ card.cardCompany }}</small>
                </span>
                <span
                    class="benefit-value-wrap"
                    :class="{
                      'long-value': isLongBenefitValue(group.value(card)),
                    }"
                >
                  <strong>
                    <template
                        v-for="(valuePart, index) in getBenefitValueParts(group.value(card))"
                        :key="`${card.id}-${group.label}-${index}`"
                    >
                      <span>
                        {{ index ? `+ ${valuePart}` : valuePart }}
                      </span>
                    </template>
                  </strong>
                </span>
              </button>
            </article>
          </div>

        </div>
      </section>

      <BottomNav />
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
  padding: 14px 18px 118px;
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

.selection-actions {
  display: flex;
  align-items: center;
  gap: 7px;
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
  font-size: 9.5px;
  font-weight: 600;
  line-height: 1.5;
}

.clear-button {
  padding: 4px;
  background: transparent;
  color: #5a6478;
  font-size: 10.5px;
  font-weight: 700;
}

.benefit-card-list,
.secondary-benefit-list {
  display: grid;
  gap: 12px;
}

.benefit-card {
  padding: 17px 16px 5px;
  border: 1px solid #e5eaf2;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(29, 55, 92, 0.055);
}

.benefit-card h3 {
  margin: 0 0 10px;
  color: #10192b;
  font-size: 14px;
  font-weight: 900;
}

.benefit-group-heading {
  display: flex;
  margin-bottom: 10px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.benefit-group-heading h3 {
  margin-bottom: 0;
}

.benefit-group-heading small {
  color: #8a98ab;
  font-size: 8.5px;
  font-weight: 600;
  white-space: nowrap;
}

.benefit-row {
  display: flex;
  width: 100%;
  min-height: 61px;
  padding: 11px 0;
  align-items: center;
  border-top: 1px solid #edf0f5;
  justify-content: space-between;
  gap: 14px;
  background: transparent;
  text-align: left;
}

.benefit-row:first-of-type {
  border-top: 0;
}

.benefit-card-name {
  min-width: 112px;
  max-width: 44%;
  flex: 0 1 44%;
  color: #162138;
  font-size: 12.5px;
  font-weight: 500;
  line-height: 1.35;
  word-break: keep-all;
}

.rate-benefit-card {
  position: relative;
  margin-bottom: 6px;
}

.rate-benefit-card::after {
  position: absolute;
  right: 12px;
  bottom: -10px;
  left: 12px;
  height: 1px;
  background: repeating-linear-gradient(
    90deg,
    #bdc9dc 0 7px,
    transparent 7px 13px
  );
  content: '';
}

.benefit-card-name > span {
  display: block;
  white-space: nowrap;
}

.benefit-card-name small {
  display: block;
  margin-top: 4px;
  color: #96a3b6;
  font-size: 9.5px;
  font-weight: 600;
}

.benefit-value-wrap {
  display: flex;
  min-width: 0;
  max-width: 56%;
  flex: 1 1 56%;
  align-items: center;
  justify-content: flex-end;
  gap: 7px;
}

.benefit-value-wrap strong {
  color: #173f8d;
  font-size: 10.5px;
  font-weight: 500;
  line-height: 1.35;
  text-align: right;
  word-break: keep-all;
}

.benefit-value-wrap strong > span {
  display: block;
  white-space: nowrap;
}

.benefit-value-wrap.long-value strong {
  font-size: 8.5px;
  line-height: 1.45;
  word-break: keep-all;
}

.benefit-value-wrap em {
  flex: 0 0 auto;
  padding: 4px 7px;
  border-radius: 999px;
  background: #e8f8ef;
  color: #12a36d;
  font-size: 8.5px;
  font-style: normal;
  font-weight: 900;
}

.secondary-benefit-list {
  margin-top: 1px;
}

.compact-benefit-card {
  padding-top: 15px;
}

.compact-benefit-card .benefit-row {
  min-height: 53px;
}

.more-benefit-button {
  display: flex;
  width: 100%;
  min-height: 55px;
  margin-top: 1px;
  align-items: center;
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  justify-content: center;
  gap: 7px;
  background: #fff;
  color: #173f8d;
  font-size: 12.5px;
  font-weight: 900;
  box-shadow: 0 8px 24px rgba(29, 55, 92, 0.045);
}

.more-benefit-label {
  line-height: 1;
}

.more-benefit-chevron {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  color: #173f8d;
  transition: transform 0.2s ease;
}

.more-benefit-chevron.opened {
  transform: rotate(180deg);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
