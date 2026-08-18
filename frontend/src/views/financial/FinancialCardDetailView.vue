<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelCardsStore } from '@/stores/travelCards'
import { getSettlementTypeLabel, getTravelCardImage, getTravelCardBackImage } from '@/utils/travelCard'

const route = useRoute()
const router = useRouter()
const travelCardsStore = useTravelCardsStore()

const cardId = computed(
    () => Number(route.params.cardId),
)

const card = computed(
    () => travelCardsStore.selectedCard,
)

const isValidCardId = computed(
    () =>
        Number.isInteger(cardId.value) &&
        cardId.value > 0,
)

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

function formatValue(value) {
  if (
      value === null ||
      value === undefined ||
      value === ''
  ) {
    return '정보 없음'
  }

  return value
}


async function loadCard() {
  if (!isValidCardId.value) {
    travelCardsStore.errorMessage =
        '올바르지 않은 트래블카드 ID입니다.'

    return
  }

  try {
    await travelCardsStore.loadCardDetail(
        cardId.value,
    )
  } catch {
    // 오류 메시지는 Store에서 처리합니다.
  }
}

function toggleComparison() {
  if (!card.value) {
    return
  }

  const wasCompared =
      travelCardsStore.isCompared(card.value.id)

  const changed =
      travelCardsStore.toggleComparisonCard(
          card.value.id,
      )

  if (!changed && !wasCompared) {
    window.alert(
        '비교할 트래블카드는 최대 3개까지 선택할 수 있습니다.',
    )
  }
}

function openComparison() {
  router.push('/financial/cards/compare')
}

watch(
    cardId,
    () => {
      loadCard()
    },
    { immediate: true },
)
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
          <h1>카드 상세</h1>
        </div>

        <span />
      </header>

      <section
          v-if="travelCardsStore.detailLoading"
          class="state-card"
      >
        <b>카드 정보를 불러오고 있어요.</b>
        <p>잠시만 기다려 주세요.</p>
      </section>

      <section
          v-else-if="travelCardsStore.errorMessage"
          class="state-card error"
      >
        <b>카드 정보를 불러오지 못했습니다.</b>
        <p>
          {{ travelCardsStore.errorMessage }}
        </p>

        <button
            v-if="isValidCardId"
            type="button"
            @click="loadCard"
        >
          다시 시도
        </button>

        <button
            v-else
            type="button"
            @click="router.replace('/financial/cards')"
        >
          카드 목록으로 이동
        </button>
      </section>

      <template v-else-if="card">
        <section class="identity-hero">
          <div class="identity-visual">
           <div class="card-entrance">
            <div
                v-if="getTravelCardImage(card.cardCompany)"
                class="card-spin-stage card-flip"
            >
              <div class="card-face card-face-front">
                <img
                    class="card-face-image"
                    :src="getTravelCardImage(card.cardCompany)"
                    :alt="`${card.cardCompany} ${card.cardName} 카드 앞면 이미지`"
                />
                <span class="card-face-sheen" aria-hidden="true"></span>
              </div>
              <div class="card-face card-face-back">
                <img
                    v-if="getTravelCardBackImage(card.cardCompany)"
                    class="card-face-image"
                    :src="getTravelCardBackImage(card.cardCompany)"
                    :alt="`${card.cardCompany} ${card.cardName} 카드 뒷면 이미지`"
                />
                <div
                    v-else
                    class="card-face-back-mock"
                    :style="{
                    background: `linear-gradient(150deg, ${getCompanyColor(card.cardCompany)} 0%, #0c2d72 130%)`,
                  }"
                >
                  <span class="mock-signature" aria-hidden="true"></span>
                  <b>{{ card.cardCompany }}</b>
                </div>
                <span class="card-face-sheen" aria-hidden="true"></span>
              </div>
            </div>

            <i
                v-else
                class="identity-fallback-badge"
                :style="{
                background: `linear-gradient(145deg, ${getCompanyColor(card.cardCompany)} 0%, #0c2d72 130%)`,
              }"
            >
              {{ getCompanyCode(card.cardCompany) }}
            </i>
           </div>
          </div>

          <div class="identity-copy">
            <small>
              {{ card.cardCompany }}
              <template v-if="card.bankName">
                · {{ card.bankName }}
              </template>
            </small>
            <b>{{ card.cardName }}</b>
            <span
                class="identity-status"
                :class="{ available: card.instantUse }"
            >
              {{ card.instantUse ? '즉시 이용 가능' : '연계 계좌 필요' }}
            </span>
          </div>
        </section>

        <section class="information-card">
          <small class="section-eyebrow">USAGE</small>
          <h2>이용 조건</h2>

          <dl>
            <div>
              <dt>필요 계좌·서비스</dt>
              <dd>
                {{ formatValue(card.requiredAccount) }}
              </dd>
            </div>

            <div>
              <dt>외화 결제 방식</dt>
              <dd>
                {{
                  getSettlementTypeLabel(
                      card.settlementType,
                  )
                }}
              </dd>
            </div>

            <div>
              <dt>적용 환율</dt>
              <dd>
                {{ formatValue(card.appliedRateInfo) }}
              </dd>
            </div>

            <div>
              <dt>외화 보유한도</dt>
              <dd class="blue">
                {{
                  formatValue(
                      card.foreignCurrencyHoldingLimit,
                  )
                }}
              </dd>
            </div>
          </dl>
        </section>

        <section class="information-card">
          <small class="section-eyebrow">FEES</small>
          <h2>수수료 정보</h2>

          <dl>
            <div>
              <dt>환전 수수료</dt>
              <dd>
                {{ formatValue(card.exchangeFee) }}
              </dd>
            </div>

            <div>
              <dt>재환전 수수료</dt>
              <dd>
                {{ formatValue(card.reExchangeFee) }}
              </dd>
            </div>

            <div>
              <dt>해외 결제 수수료</dt>
              <dd>
                {{ formatValue(card.paymentFee) }}
              </dd>
            </div>

            <div>
              <dt>해외 ATM 출금 수수료</dt>
              <dd>
                {{ formatValue(card.withdrawalFee) }}
              </dd>
            </div>
          </dl>
        </section>

        <section class="feature-card">
          <small class="section-eyebrow">FEATURES</small>
          <h2>지원 기능</h2>

          <div class="feature-grid">
            <article>
              <span>자동충전</span>
              <b
                  :class="{
                  supported:
                    card.autoChargeSupported,
                }"
              >
                {{
                  card.autoChargeSupported
                      ? '지원'
                      : '미지원'
                }}
              </b>
            </article>

            <article>
              <span>교통카드</span>
              <b
                  :class="{
                  supported: card.transitCard,
                }"
              >
                {{
                  card.transitCard
                      ? '지원'
                      : '미지원'
                }}
              </b>
            </article>
          </div>
        </section>

        <section class="currency-card">
          <small class="section-eyebrow">CURRENCY</small>
          <div class="section-heading">
            <h2>지원 통화</h2>

            <span>
              {{
                card.supportedCurrencies?.length || 0
              }}개
            </span>
          </div>

          <div
              v-if="card.supportedCurrencies?.length"
              class="currency-list"
          >
            <span
                v-for="currency in card.supportedCurrencies"
                :key="currency"
            >
              {{ currency }}
            </span>
          </div>

          <p v-else>
            직접 보유·차감하는 지원 통화 정보가 없습니다.
          </p>

          <small
              v-if="
              card.settlementType ===
              'USD_CONVERSION'
            "
              class="currency-notice"
          >
            현지 결제금액을 USD로 환산하여
            USD 외화잔액에서 차감하는 카드입니다.
          </small>
        </section>

        <button
            type="button"
            class="comparison-button"
            :class="{
            selected:
              travelCardsStore.isCompared(card.id),
          }"
            @click="toggleComparison"
        >
          {{
            travelCardsStore.isCompared(card.id)
                ? '카드 비교에서 제거'
                : '카드 비교에 추가'
          }}
        </button>

        <button
            type="button"
            class="comparison-page-button"
            @click="openComparison"
        >
          선택한 카드 비교 보기
          ({{ travelCardsStore.comparedCardCount }}/3)
        </button>
      </template>

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
  margin: auto;
  padding: 42px 18px 118px;
  background: linear-gradient(180deg, #f4f7ff 0%, #edf3fc 100%);
}

.page-header {
  display: grid;
  grid-template-columns: 42px 1fr 42px;
  align-items: center;
  margin-bottom: 17px;
}

.back-button {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 0;
  border-radius: 14px;
  background: #fff;
  color: #16366f;
  font-size: 28px;
  box-shadow: 0 5px 16px #24487512;
}

.page-header > div {
  text-align: center;
}

.page-header small {
  color: #2f6fd8;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.13em;
}

.page-header h1 {
  margin-top: 2px;
  font-size: 21px;
  font-weight: 900;
}

.identity-hero {
  position: relative;
  padding: 10px 20px 24px;
  text-align: center;
  color: #10192b;
}

.identity-visual {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: center;
  perspective: 1200px;
}

.card-entrance {
  display: flex;
  width: 100%;
  justify-content: center;
  animation: card-rise-in 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes card-rise-in {
  from {
    opacity: 0;
    transform: translateY(46px) scale(0.92);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.card-spin-stage {
  position: relative;
  width: 58%;
  max-width: 190px;
  aspect-ratio: 0.63;
  transform-style: preserve-3d;
}

.card-flip {
  animation: card-flip-anim 7s ease-in-out infinite;
}

.card-face {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border-radius: 14px;
  backface-visibility: hidden;
  box-shadow: 0 18px 32px rgba(7, 19, 48, 0.4);
}

.card-face-back {
  transform: rotateY(180deg);
}

.card-face-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
}

.card-face-sheen {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    115deg,
    rgba(255, 255, 255, 0.32) 0%,
    rgba(255, 255, 255, 0.06) 22%,
    rgba(255, 255, 255, 0) 42%,
    rgba(255, 255, 255, 0) 100%
  );
  pointer-events: none;
}

.card-face-back-mock {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 18px 15px 15px;
}

.mock-signature {
  display: block;
  width: 78%;
  height: 22px;
  margin-top: 26px;
  border-radius: 4px;
  background: repeating-linear-gradient(135deg, #ffffff5c 0 2px, transparent 2px 5px), #ffffffe6;
}

.card-face-back-mock b {
  margin-top: auto;
  color: #fff;
  font-family: 'Space Mono', monospace;
  font-size: 10px;
  font-style: normal;
  letter-spacing: 0.12em;
}

@keyframes card-flip-anim {
  0%,
  38% {
    transform: rotateY(0deg);
  }
  50%,
  88% {
    transform: rotateY(180deg);
  }
  100% {
    transform: rotateY(360deg);
  }
}

.identity-fallback-badge {
  display: grid;
  width: 96px;
  height: 96px;
  place-items: center;
  border-radius: 20px;
  color: #fff;
  font-size: 26px;
  font-style: normal;
  font-weight: 900;
  box-shadow: 0 14px 26px rgba(7, 19, 48, 0.25);
}

.identity-status {
  display: inline-block;
  margin-top: 12px;
  padding: 6px 9px;
  border-radius: 9px;
  background: #eef2f8;
  color: #5a6478;
  font-size: 10px;
  font-weight: 800;
}

.identity-status.available {
  background: #e5f7f1;
  color: #078b64;
}

.identity-copy {
  position: relative;
  margin-top: 18px;
}

.identity-copy small {
  display: block;
  color: #5a6478;
  font-size: 11px;
  font-weight: 700;
}

.identity-copy b {
  display: block;
  margin-top: 6px;
  font-size: 23px;
  font-weight: 900;
  color: #10192b;
  letter-spacing: -0.03em;
  line-height: 1.3;
}

.information-card,
.feature-card,
.currency-card {
  margin-top: 12px;
  border: 1px solid #e1e6ed;
  border-radius: 19px;
  background: #fff;
  box-shadow: 0 8px 22px #263e650b;
}

.information-card,
.feature-card,
.currency-card {
  padding: 16px;
}

.section-eyebrow {
  display: block;
  color: #2e6fd9;
  font-size: 9px;
  font-weight: 900;
  letter-spacing: 0.13em;
}

.information-card h2,
.feature-card h2,
.currency-card h2 {
  margin-top: 3px;
  font-size: 16px;
}

.information-card dl > div {
  display: grid;
  grid-template-columns: 108px 1fr;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #eef1f5;
}

.information-card dl > div:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.information-card dt {
  color: #8c97a7;
  font-size: 11px;
}

.information-card dd {
  font-size: 12px;
  font-weight: 800;
  line-height: 1.5;
  text-align: right;
  word-break: keep-all;
}

.information-card dd.blue {
  color: #1768d3;
}

.feature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
  margin-top: 12px;
}

.feature-grid article {
  padding: 12px;
  border-radius: 11px;
  background: #f5f7fa;
}

.feature-grid span,
.feature-grid b {
  display: block;
}

.feature-grid span {
  color: #8692a3;
  font-size: 11px;
}

.feature-grid b {
  margin-top: 6px;
  color: #7d8998;
  font-size: 13px;
}

.feature-grid b.supported {
  color: #078f68;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-heading > span {
  padding: 5px 8px;
  border-radius: 9px;
  background: #eaf2ff;
  color: #1768d3;
  font-size: 10px;
  font-weight: 800;
}

.currency-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.currency-list span {
  padding: 6px 9px;
  border: 1px solid #dce5f0;
  border-radius: 9px;
  background: #f8fafd;
  color: #435570;
  font-size: 11px;
  font-weight: 700;
}

.currency-card > p {
  margin-top: 12px;
  color: #8793a3;
  font-size: 11px;
}

.currency-notice {
  display: block;
  margin-top: 12px;
  padding: 10px;
  border-radius: 9px;
  background: #fff6e8;
  color: #b76a16;
  font-size: 11px;
  line-height: 1.5;
}

.comparison-button,
.comparison-page-button {
  width: 100%;
  margin-top: 12px;
  padding: 15px;
  border: 1px solid #d2def0;
  border-radius: 13px;
  background: #fff;
  color: #173f8d;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 4px 12px #253f6b0d;
}

.comparison-button.selected {
  border-color: #4b7fd0;
  background: #e8f1ff;
  color: #175fc4;
}

.comparison-page-button {
  margin-top: 9px;
  border: none;
  background: #173f8d;
  color: #fff;
  box-shadow: 0 10px 22px #123a8529;
}

.state-card {
  margin-top: 12px;
  padding: 50px 20px;
  border: 1px solid #e1e6ed;
  border-radius: 19px;
  background: #fff;
  box-shadow: 0 8px 22px #263e650b;
  text-align: center;
}

.state-card b {
  display: block;
  font-size: 15px;
}

.state-card p {
  margin-top: 7px;
  color: #8592a4;
  font-size: 12px;
  line-height: 1.5;
}

.state-card button {
  margin-top: 14px;
  padding: 10px 16px;
  border-radius: 9px;
  background: #173f8d;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}

.state-card.error {
  border-color: #f2caca;
  background: #fffafa;
}

.state-card.error b {
  color: #d44747;
}

@media (prefers-reduced-motion: reduce) {
  .card-flip,
  .card-entrance {
    animation: none;
  }
}
</style>