<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import BottomNav from '@/components/common/BottomNav.vue'
import ProductTicket from '@/components/financial/ProductTicket.vue'
import { useTravelCardsStore } from '@/stores/travelCards'

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

function getSettlementTypeLabel(settlementType) {
  if (settlementType === 'DIRECT') {
    return '지원 외화 직접 보유·차감'
  }

  if (settlementType === 'USD_CONVERSION') {
    return '현지통화를 USD로 환산 후 차감'
  }

  return formatValue(settlementType)
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

onMounted(loadCard)
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

        <h1>카드 상세</h1>

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
        <ProductTicket
            title="트래블카드 주요 조건"
            subtitle="외화 보유한도와 해외 이용 수수료를 확인해 보세요"
        />

        <section class="identity-card">
          <i
              :style="{
              background: getCompanyColor(
                card.cardCompany,
              ),
            }"
          >
            {{ getCompanyCode(card.cardCompany) }}
          </i>

          <div>
            <b>{{ card.cardName }}</b>

            <small>
              {{ card.cardCompany }}
              <template v-if="card.bankName">
                · {{ card.bankName }}
              </template>
            </small>
          </div>

          <span
              :class="{
              available: card.instantUse,
            }"
          >
            {{
              card.instantUse
                  ? '즉시 이용 가능'
                  : '연계 계좌 필요'
            }}
          </span>
        </section>

        <section class="information-card">
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

.identity-card,
.information-card,
.feature-card,
.currency-card {
  margin-top: 12px;
  border: 1px solid #e0e6ed;
  border-radius: 15px;
  background: #fff;
}

.identity-card {
  position: relative;
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 16px;
}

.identity-card i {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 12px;
  color: #fff;
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
}

.identity-card b,
.identity-card small {
  display: block;
}

.identity-card b {
  font-size: 11px;
  line-height: 1.4;
}

.identity-card small {
  margin-top: 5px;
  color: #8a96a6;
  font-size: 8px;
}

.identity-card > span {
  position: absolute;
  right: 14px;
  bottom: 11px;
  padding: 5px 8px;
  border-radius: 9px;
  background: #f0f2f5;
  color: #8490a0;
  font-size: 7px;
  font-weight: 800;
}

.identity-card > span.available {
  background: #e8f8f2;
  color: #078b64;
}

.information-card,
.feature-card,
.currency-card {
  padding: 15px;
}

.information-card h2,
.feature-card h2,
.currency-card h2 {
  font-size: 11px;
}

.information-card dl > div {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 10px;
  padding: 11px 0;
  border-bottom: 1px solid #eef1f5;
}

.information-card dl > div:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.information-card dt {
  color: #8c97a7;
  font-size: 8px;
}

.information-card dd {
  font-size: 8px;
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
  font-size: 8px;
}

.feature-grid b {
  margin-top: 6px;
  color: #7d8998;
  font-size: 10px;
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
  font-size: 8px;
  font-weight: 800;
}

.currency-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.currency-list span {
  padding: 6px 8px;
  border: 1px solid #dce5f0;
  border-radius: 9px;
  background: #f8fafd;
  color: #435570;
  font-size: 8px;
  font-weight: 700;
}

.currency-card > p {
  margin-top: 12px;
  color: #8793a3;
  font-size: 8px;
}

.currency-notice {
  display: block;
  margin-top: 12px;
  padding: 10px;
  border-radius: 9px;
  background: #fff6e8;
  color: #b76a16;
  font-size: 8px;
  line-height: 1.5;
}

.comparison-button,
.comparison-page-button {
  width: 100%;
  margin-top: 10px;
  padding: 13px;
  border: 1px solid #174494;
  border-radius: 10px;
  color: #174494;
  font-size: 9px;
  font-weight: 900;
}

.comparison-button.selected {
  background: #e8f1ff;
  color: #175fc4;
}

.comparison-page-button {
  margin-top: 8px;
  background: #173f8d;
  color: #fff;
}

.state-card {
  margin-top: 10px;
  padding: 50px 20px;
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