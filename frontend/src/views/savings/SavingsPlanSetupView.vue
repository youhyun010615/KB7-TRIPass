<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSavingsPlanStore } from '@/stores/savingsPlan'

const router = useRouter()
const plan = useSavingsPlanStore()
const money = (v) => `${Number(v).toLocaleString('ko-KR')}원`

const isCash = computed(() => plan.savingMethod === 'cash')
const isProduct = computed(() => plan.savingMethod === 'product')

// 현금 입력 상태
const cashStatus = computed(() => {
  if (!plan.monthlySavings) return 'empty'
  if (plan.insufficientFunds) return 'error'
  if (plan.monthlySavings >= plan.recommendedMonthlySavings) return 'success'
  return 'warning'
})

function onAmountInput(e) {
  plan.setMonthlySavings(e.target.value)
}

function onSelectCash() {
  plan.selectMethod('cash')
}

function onSelectProduct() {
  plan.selectMethod('product')
}

function confirm() {
  if (!plan.canSave) return
  if (isProduct.value) router.push('/financial')
  else router.push('/savings')
}

const ctaLabel = computed(() => {
  if (isProduct.value) return '맞춤 금융상품 추천받기'
  if (cashStatus.value === 'error') return '금액 다시 입력'
  return '확인'
})
</script>

<template>
  <main class="page">
    <!-- 헤더 -->
    <header class="header">
      <button class="back" @click="router.back()">‹</button>
      <h1>목표 저축 설정</h1>
      <span />
    </header>

    <!-- 상단 미니 티켓 요약 -->
    <section class="summary-ticket">
      <div class="st-col">
        <small>총 목표 금액</small>
        <strong>{{ money(plan.totalTargetAmount) }}</strong>
      </div>
      <div class="st-divider" />
      <div class="st-col">
        <small>확보한 여행 자금</small>
        <strong class="yellow">{{ money(plan.securedAmount) }}</strong>
      </div>
      <div class="st-divider" />
      <div class="st-col">
        <small>월 저축 목표 금액</small>
        <strong class="blue">{{ money(plan.recommendedMonthlySavings) }}</strong>
      </div>
    </section>

    <!-- 저축 방식 선택 -->
    <h2>저축 방식 선택</h2>

    <!-- 여행 저축 금액 카드 -->
    <div class="method-card" :class="{ selected: isCash }" @click="onSelectCash">
      <div class="method-top">
        <span class="checkbox" :class="{ on: isCash }">
          <svg v-if="isCash" width="12" height="12" viewBox="0 0 24 24" fill="none">
            <path d="M5 13L9 17L19 7" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <div>
          <strong>여행 저축 금액</strong>
          <small>여행 자금에서 여행을 위한 금액을 저축해요.</small>
        </div>
      </div>

      <!-- 입력 패널 (선택 시) -->
      <div v-if="isCash" class="input-panel" @click.stop>
        <div class="input-label-row">
          <span>추가 저축 금액 입력란</span>
          <span class="remain-label" :class="{ danger: cashStatus === 'error' }">
            {{ cashStatus === 'error'
              ? `여유 자금보다 ${money(plan.insufficientFunds)} 부족`
              : `남은 여유 자금: ${money(plan.remainingAvailableFunds)}` }}
          </span>
        </div>
        <div class="amount-input-wrap" :class="cashStatus">
          <input
            :value="plan.monthlySavings ? money(plan.monthlySavings) : ''"
            inputmode="numeric"
            placeholder="0원"
            @input="onAmountInput"
          />
        </div>

        <!-- 성공 피드백 -->
        <div v-if="cashStatus === 'success'" class="feedback success">
          <div class="feedback-row">
            <span class="fb-icon success">✓</span>
            <span>저축 금액으로 목표 달성 가능</span>
          </div>
          <div class="feedback-detail">
            <div><small>추천 월 저축액</small><b class="green">{{ money(0) }}</b></div>
            <div><small>예상 출국</small><b class="green">{{ plan.expectedDeparture }}</b></div>
          </div>
        </div>

        <!-- 경고 피드백 -->
        <div v-else-if="cashStatus === 'warning'" class="feedback warning">
          <div class="feedback-row">
            <span class="fb-icon warning">!</span>
            <span>월 저축액을 조금 더 늘려볼까요?</span>
          </div>
          <div class="feedback-detail">
            <div><small>추천 월 저축액</small><b class="amber">{{ money(plan.additionalRecommendedAmount) }}</b></div>
            <div><small>예상 출국</small><b class="amber">{{ plan.expectedDeparture }}</b></div>
          </div>
        </div>

        <!-- 오류 피드백 -->
        <div v-else-if="cashStatus === 'error'" class="feedback error">
          <div class="feedback-row">
            <span class="fb-icon error">!</span>
            <span>여유 자금이 {{ money(plan.insufficientFunds) }} 부족해요</span>
          </div>
          <p class="error-formula">
            현재 금액 {{ money(plan.availableFunds) }} - {{ money(plan.monthlySavings) }} = {{ money(plan.insufficientFunds) }} 부족<br>
            <span>저축 금액을 {{ money(plan.availableFunds) }} 이하로 입력해 주세요.</span>
          </p>
        </div>
      </div>

      <!-- 미선택 시 입력 금액 미리보기 -->
      <div v-else class="unselected-preview">
        <small>추가 저축 금액 · 금액입력 없음</small>
        <span>0원</span>
      </div>
    </div>

    <!-- 금융상품 추천 카드 -->
    <div class="method-card" :class="{ selected: isProduct }" @click="onSelectProduct" style="margin-top: 12px">
      <div class="method-top">
        <span class="checkbox" :class="{ on: isProduct }">
          <svg v-if="isProduct" width="12" height="12" viewBox="0 0 24 24" fill="none">
            <path d="M5 13L9 17L19 7" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <div>
          <strong>금융상품 추천</strong>
          <small>목표 기간에 맞는 금융상품도 비교할 수 있어요.</small>
        </div>
      </div>

      <!-- 금융상품 선택 시 상세 -->
      <div v-if="isProduct" class="product-detail" @click.stop>
        <p class="product-desc">
          목표까지 <b>{{ money(plan.shortageAmount) }}</b>이 더 필요해요<br>
          <span>부족 금액과 남은 기간에 맞는 상품을 추천해 드려요.</span>
        </p>
        <div class="badges">
          <span class="badge">예상 수익 비교</span>
          <span class="badge">기간 맞춤 추천</span>
        </div>
      </div>
    </div>

    <!-- CTA -->
    <button
      class="cta"
      :disabled="!plan.canSave || cashStatus === 'error'"
      @click="confirm"
    >{{ ctaLabel }}</button>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 0 16px 120px; background: #f7f4ee; color: #111827; }

.header { display: grid; grid-template-columns: 40px 1fr 40px; align-items: end; height: 76px; padding-bottom: 14px; }
.back { border: 0; background: none; font-size: 26px; line-height: 1; }
.header h1 { font-size: 17px; font-weight: 800; text-align: center; }

/* 미니 티켓 */
.summary-ticket { display: flex; align-items: center; padding: 16px 18px; border-radius: 16px; color: #fff; background: linear-gradient(135deg, #2949a3, #173b86); margin-bottom: 22px; }
.st-col { flex: 1; display: grid; gap: 5px; }
.st-col small { color: rgba(255,255,255,.55); font-size: 8px; }
.st-col strong { font-size: 12px; font-weight: 800; }
.st-col .yellow { color: #ffd56a; }
.st-col .blue { color: #82d3ff; }
.st-divider { width: 1px; height: 30px; background: rgba(255,255,255,.2); margin: 0 12px; }

h2 { font-size: 15px; font-weight: 700; margin-bottom: 12px; }

/* 방식 카드 */
.method-card { border: 1.5px solid #e2e8f0; border-radius: 16px; background: #fff; overflow: hidden; cursor: pointer; }
.method-card.selected { border-color: #3B5BDB; }
.method-top { display: flex; align-items: flex-start; gap: 12px; padding: 16px; }
.checkbox { display: grid; place-items: center; width: 22px; height: 22px; flex: none; margin-top: 1px; border: 1.5px solid #cbd5e1; border-radius: 6px; background: #fff; transition: background .15s, border-color .15s; }
.checkbox.on { border-color: #3B5BDB; background: #3B5BDB; }
.method-top strong { display: block; font-size: 13px; font-weight: 700; }
.method-top small { display: block; margin-top: 3px; color: #64748b; font-size: 10px; }

/* 입력 패널 */
.input-panel { padding: 0 16px 16px; }
.input-label-row { display: flex; justify-content: space-between; font-size: 10px; color: #94a3b8; margin-bottom: 8px; }
.remain-label.danger { color: #e5484d; }
.amount-input-wrap { display: flex; align-items: center; padding: 12px 14px; border: 1.5px solid #f59e0b; border-radius: 12px; background: #fffbeb; }
.amount-input-wrap.error { border-color: #e5484d; background: #fff5f5; }
.amount-input-wrap.success { border-color: #10aa82; background: #f0fdf9; }
.amount-input-wrap input { width: 100%; border: 0; outline: 0; background: transparent; font-size: 20px; font-weight: 800; color: #3B5BDB; }
.amount-input-wrap.error input { color: #e5484d; }
.amount-input-wrap.success input { color: #07875f; }

/* 피드백 */
.feedback { margin-top: 10px; padding: 12px; border-radius: 12px; }
.feedback.success { background: #eaf9f4; }
.feedback.warning { background: #fff6e9; }
.feedback.error { background: #fff2f2; }
.feedback-row { display: flex; align-items: center; gap: 7px; font-size: 12px; font-weight: 600; }
.feedback.success .feedback-row { color: #07875f; }
.feedback.warning .feedback-row { color: #b96300; }
.feedback.error .feedback-row { color: #d9363e; }
.fb-icon { display: grid; place-items: center; width: 20px; height: 20px; border-radius: 50%; color: #fff; font-size: 11px; font-weight: 800; flex: none; }
.fb-icon.success { background: #10aa82; }
.fb-icon.warning { background: #ee921f; }
.fb-icon.error { background: #e5484d; }
.feedback-detail { display: flex; gap: 20px; margin-top: 10px; }
.feedback-detail div { display: grid; gap: 3px; }
.feedback-detail small { color: #94a3b8; font-size: 9px; }
.feedback-detail b { font-size: 13px; font-weight: 700; }
.green { color: #07875f; }
.amber { color: #d97706; }
.error-formula { margin-top: 8px; padding: 8px 10px; border-radius: 8px; background: rgba(229,72,77,.08); font-size: 10px; line-height: 1.6; color: #d9363e; }
.error-formula span { color: #94a3b8; }

/* 미선택 미리보기 */
.unselected-preview { display: flex; justify-content: space-between; align-items: center; padding: 0 16px 14px; color: #94a3b8; font-size: 10px; }
.unselected-preview span { font-size: 14px; font-weight: 600; }

/* 금융상품 상세 */
.product-detail { padding: 0 16px 16px; }
.product-desc { font-size: 12px; color: #64748b; line-height: 1.6; margin: 0 0 10px; }
.product-desc b { color: #3B5BDB; }
.product-desc span { font-size: 11px; }
.badges { display: flex; gap: 8px; }
.badge { padding: 4px 10px; border-radius: 99px; background: #eef9f3; color: #07875f; font-size: 10px; font-weight: 600; }

/* CTA */
.cta { position: fixed; z-index: 5; left: 50%; bottom: 22px; width: min(358px, calc(100% - 32px)); height: 54px; transform: translateX(-50%); border: 0; border-radius: 14px; color: #fff; background: #173b86; font-size: 15px; font-weight: 800; box-shadow: 0 8px 20px rgba(23,59,134,.2); }
.cta:disabled { background: #aeb9cc; box-shadow: none; }
</style>
