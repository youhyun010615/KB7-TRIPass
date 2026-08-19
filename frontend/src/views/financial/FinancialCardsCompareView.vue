<script setup>
import { computed, onMounted } from 'vue'
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

const comparisonRows = [
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
    label: '적용 환율',
    value: (card) => card.appliedRateInfo,
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
    label: '해외 결제 수수료',
    value: (card) => card.paymentFee,
  },
  {
    label: '해외 ATM 수수료',
    value: (card) => card.withdrawalFee,
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

const tableGridStyle = computed(() => ({
  gridTemplateColumns:
      `84px repeat(${Math.max(comparisonCards.value.length, 1)}, minmax(98px, 1fr))`,
}))

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
              선택한 카드를 좌우로 비교해 보세요.
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

        <div class="comparison-scroll">
          <div class="comparison-table">
            <div
                class="table-row table-header"
                :style="tableGridStyle"
            >
              <strong>비교 항목</strong>

              <button
                  v-for="card in comparisonCards"
                  :key="card.id"
                  type="button"
                  @click="openCardDetail(card.id)"
              >
                {{ card.cardName }}
              </button>
            </div>

            <div
                v-for="row in comparisonRows"
                :key="row.label"
                class="table-row"
                :style="tableGridStyle"
            >
              <strong>{{ row.label }}</strong>

              <span
                  v-for="card in comparisonCards"
                  :key="card.id"
              >
                {{ displayValue(row.value(card)) }}
              </span>
            </div>
          </div>
        </div>

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
  padding: 42px 18px 118px;
  background: linear-gradient(180deg, #f4f7ff 0%, #edf3fc 100%);
}

button {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.page-header {
  display: grid;
  grid-template-columns: 42px 1fr 42px;
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
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 14px;
  background: #fff;
  color: #16366f;
  font-size: 28px;
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

.comparison-scroll {
  position: relative;
  overflow-x: auto;
  border: 1px solid #e1e6ed;
  border-radius: 19px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(38, 62, 101, 0.05);
}

.comparison-table {
  min-width: 100%;
  width: max-content;
}

.table-row {
  display: grid;
  border-top: 1px solid #f1f3f8;
}

.table-row:first-child {
  border-top: 0;
}

.table-row > * {
  display: flex;
  min-height: 50px;
  padding: 9px 7px;
  align-items: center;
  border-left: 1px solid #f1f3f8;
  justify-content: center;
  font-size: 9.5px;
  line-height: 1.4;
  text-align: center;
  white-space: normal;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.table-row > *:first-child {
  border-left: 0;
}

.table-row > strong {
  align-items: flex-start;
  justify-content: flex-start;
  color: #5a6478;
  font-weight: 700;
  text-align: left;
}

.table-header {
  background: #f4f7ff;
}

.table-header > * {
  min-height: 56px;
  color: #10192b;
  font-weight: 900;
}

.table-header button {
  display: -webkit-box;
  overflow: hidden;
  background: transparent;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.table-header button:hover {
  color: #2f6fe9;
  text-decoration: underline;
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
/* 가로 스크롤 중 비교 항목 열 고정 */
.table-row > :first-child {
  position: sticky;
  left: 0;
  z-index: 2;
  background: #fff;
  box-shadow: 5px 0 8px -7px rgba(16, 26, 46, 0.15);
}

/* 표 헤더의 비교 항목 셀 */
.table-header > :first-child {
  z-index: 3;
  background: #f4f7ff;
}
</style>