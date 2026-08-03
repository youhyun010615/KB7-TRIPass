<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTravelStore } from '@/stores/travel'
import { useSavingsPlanStore } from '@/stores/savingsPlan'

const router = useRouter()
const travel = useTravelStore()
const plan = useSavingsPlanStore()
const money = (v) => `${Number(v).toLocaleString('ko-KR')}원`

const linkedAccounts = computed(() =>
  travel.accounts.filter((acc) => Number(travel.allocations[acc.id] ?? 0) > 0)
)

const cfg = computed(() => {
  const s = plan.status
  const base = {
    unset: {
      lineColor: '#cbd5e1',
      badgeText: '기간 계산 불가',
      badgeColor: '#e5484d',
      midLabel: '월 저축 미설정',
      midVal: '0원',
      rightLabel: '예상 출국',
      rightVal: '계산 불가',
      cardBg: '#fff2f2', cardBorder: '#ffc9cc',
      iconBg: '#e5484d', icon: '⚠',
      cardTitle: '현재 저축 계획으로는 목표 달성이 부족해요',
      amtLabel: '추천 월 저축액', amt: plan.recommendedMonthlySavings, amtColor: '#e5484d',
      pill: '계산 불가', pillBg: '#f1f5f9', pillColor: '#64748b',
      detailLabel: '예상 출국',
      detail: '지금은 월 저축 계획이 없어 지연 출국 날짜를 계산할 수 없어요.',
      planStatus: '미설정', planStatusColor: '#94a3b8',
      planDate: '다음 저축일 없음',
      cta: '월 저축 방식 선택',
    },
    success: {
      lineColor: '#3B5BDB',
      badgeText: `목표까지 ${plan.expectedMonths}개월 · ${plan.expectedMonths}회 저축`,
      badgeColor: '#3B5BDB',
      midLabel: `월 저축×${plan.expectedMonths}`,
      midVal: money(plan.monthlySavings),
      rightLabel: '예상 출국',
      rightVal: plan.expectedDeparture,
      cardBg: '#eaf9f4', cardBorder: '#bdeadd',
      iconBg: '#10aa82', icon: '✓',
      cardTitle: '현재 계획으로 목표 달성 가능',
      amtLabel: '추천 월 저축액', amt: 0, amtColor: '#07875f',
      pill: `예상 출국 ${plan.expectedDeparture}`, pillBg: '#d1f5eb', pillColor: '#07875f',
      detailLabel: '예상 출국',
      detail: '목표로 설정한 날짜에 출국할 수 있어요.',
      planStatus: '정상 진행', planStatusColor: '#07875f',
      planDate: '다음 저축일 8월 25일',
      cta: '월 저축 방식 선택하기',
    },
    warning: {
      lineColor: '#d97706',
      badgeText: `총 ${plan.expectedMonths}개월 · ${plan.expectedMonths}회 저축`,
      badgeColor: '#d97706',
      midLabel: `월 저축×${plan.expectedMonths}`,
      midVal: money(plan.monthlySavings),
      rightLabel: '예상 출국',
      rightVal: plan.expectedDeparture,
      cardBg: '#fff6e9', cardBorder: '#ffd59b',
      iconBg: '#ee921f', icon: '!',
      cardTitle: '월 저축액을 조금 더 늘려볼까요?',
      amtLabel: '추천 월 저축액', amt: plan.additionalRecommendedAmount, amtColor: '#d97706',
      pill: `예상 출국 ${plan.expectedDeparture}`, pillBg: '#fff3d6', pillColor: '#b96300',
      detailLabel: '예상 출국',
      detail: `현재 계획은 목표보다 약 ${plan.delayMonths}개월 지연될 수 있어요.`,
      planStatus: '추가 진행 필요', planStatusColor: '#d97706',
      planDate: '다음 저축일 8월 25일',
      cta: '월 저축 방식 선택하기',
    },
    error: {
      lineColor: '#cbd5e1',
      badgeText: '기간 계산 불가',
      badgeColor: '#e5484d',
      midLabel: '설정 오류',
      midVal: '',
      rightLabel: '예상 출국',
      rightVal: '계산 불가',
      cardBg: '#fff2f2', cardBorder: '#ffc9cc',
      iconBg: '#e5484d', icon: '⚠',
      cardTitle: '현재 여유 자금보다 저축 금액이 커요',
      amtLabel: '부족 금액', amt: plan.insufficientFunds, amtColor: '#e5484d',
      pill: '설정 불가', pillBg: '#f1f5f9', pillColor: '#64748b',
      detailLabel: '예상 출국',
      detail: `${money(plan.availableFunds)} 이하로 다시 입력해 주세요.`,
      planStatus: '미설정', planStatusColor: '#94a3b8',
      planDate: '다음 저축일 없음',
      cta: '월 저축 방식 선택하기',
    },
  }
  return base[s]
})

const ringDash = 213.6
const ringOffset = computed(() => ringDash - ringDash * plan.securedPercent / 100)
</script>

<template>
  <main class="page">
    <!-- 헤더 -->
    <header class="header">
      <button class="back" @click="router.back()">‹</button>
      <h1>여행 목표 자금 관리</h1>
      <button class="edit" @click="router.push('/travel/register')">계획 수정</button>
    </header>

    <!-- 보딩패스 티켓 -->
    <section class="ticket">
      <div class="ticket-top">
        <span>TRIPASS · BOARDING PASS</span>
        <span class="dday">D-230</span>
      </div>
      <div class="ticket-route">
        <strong>FR PAR</strong>
        <span class="plane">✈</span>
        <strong>GOAL</strong>
      </div>
      <div class="ticket-dash" />
      <div class="ticket-body">
        <!-- 원형 그래프 -->
        <div class="ring-wrap">
          <svg viewBox="0 0 80 80" width="84" height="84">
            <circle cx="40" cy="40" r="34" fill="#1e3a8a" />
            <circle
              cx="40" cy="40" r="34"
              fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="8"
            />
            <circle
              cx="40" cy="40" r="34"
              fill="none" stroke="#ef4444" stroke-width="8"
              stroke-linecap="round"
              :stroke-dasharray="ringDash"
              :stroke-dashoffset="ringOffset"
              transform="rotate(-90 40 40)"
            />
          </svg>
          <div class="ring-text">
            <strong>{{ plan.securedPercent }}%</strong>
            <small>현재 확보율</small>
          </div>
        </div>
        <!-- 금액 정보 -->
        <dl class="amounts">
          <div><dt>총 목표 금액</dt><dd>{{ money(plan.totalTargetAmount) }}</dd></div>
          <div><dt>확보한 여행 자금</dt><dd class="secured">{{ money(plan.securedAmount) }}</dd></div>
          <div><dt>부족 금액</dt><dd class="shortage">{{ money(plan.shortageAmount) }}</dd></div>
        </dl>
      </div>
      <div class="ticket-dash" />
      <div class="ticket-foot">
        <span>PASS NO. TRP-260815</span>
        <span class="barcode">▌▌▌▌▌▌▌▌▌▌</span>
      </div>
    </section>

    <!-- 반영 중인 계좌 -->
    <button class="account-btn" @click="router.push('/savings/accounts')">
      <span class="account-icon">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <rect x="2" y="6" width="20" height="13" rx="2" stroke="#d97706" stroke-width="2"/>
          <path d="M2 10H22" stroke="#d97706" stroke-width="2"/>
        </svg>
      </span>
      <div class="account-info">
        <strong>반영 중인 계좌</strong>
        <small>{{ linkedAccounts.map(a => a.name).join(' · ') || 'KB 여행통장 · 신한은행 통장' }}</small>
      </div>
      <div class="account-right">
        <b>{{ linkedAccounts.length || 2 }}개</b>
        <span>{{ money(plan.securedAmount) }}</span>
      </div>
      <span class="chev">›</span>
    </button>

    <!-- 저축 가능 기간 -->
    <section class="timeline-card">
      <div class="tl-head">
        <strong>저축 가능 기간</strong>
        <span :style="{ color: cfg.badgeColor, fontSize:'11px', fontWeight:'600' }">{{ cfg.badgeText }}</span>
      </div>
      <div class="tl-track">
        <div class="tl-line" :style="{ background: cfg.lineColor }" />
        <span class="tl-dot" :style="{ background: cfg.lineColor }" />
        <span class="tl-dot mid" :style="{ background: cfg.lineColor }" />
        <span class="tl-dot end" :style="{ background: cfg.lineColor }" />
      </div>
      <div class="tl-labels">
        <div class="tl-lbl"><small>현재</small><b>2026.07</b></div>
        <div class="tl-lbl center"><small>{{ cfg.midLabel }}</small><b>{{ cfg.midVal }}</b></div>
        <div class="tl-lbl right"><small>{{ cfg.rightLabel }}</small><b>{{ cfg.rightVal }}</b></div>
      </div>
    </section>

    <!-- 달성 전망 카드 -->
    <section class="forecast" :style="{ background: cfg.cardBg, borderColor: cfg.cardBorder }">
      <div class="fc-head">
        <span class="fc-icon" :style="{ background: cfg.iconBg }">{{ cfg.icon }}</span>
        <strong>{{ cfg.cardTitle }}</strong>
      </div>
      <hr class="fc-hr">
      <small class="fc-label">{{ cfg.amtLabel }}</small>
      <div class="fc-row">
        <strong class="fc-amount" :style="{ color: cfg.amtColor }">{{ money(cfg.amt) }}</strong>
        <span class="fc-pill" :style="{ background: cfg.pillBg, color: cfg.pillColor }">{{ cfg.pill }}</span>
      </div>
      <div class="fc-detail-wrap">
        <small class="fc-detail-eyebrow">{{ cfg.detailLabel }}</small>
        <p class="fc-detail">{{ cfg.detail }}</p>
      </div>
    </section>

    <!-- 월 저축 플랜 -->
    <section class="plan-card">
      <span class="plan-icon">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <rect x="3" y="4" width="18" height="18" rx="2" stroke="#3B5BDB" stroke-width="2"/>
          <path d="M16 2V6M8 2V6M3 10H21" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </span>
      <div class="plan-info">
        <small>월 저축 플랜</small>
        <strong>{{ money(plan.monthlySavings) }}</strong>
      </div>
      <div class="plan-right">
        <b :style="{ color: cfg.planStatusColor }">{{ cfg.planStatus }}</b>
        <small>{{ cfg.planDate }}</small>
      </div>
    </section>

    <!-- CTA -->
    <button class="cta" @click="router.push('/savings/plan')">{{ cfg.cta }}</button>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 0 16px 100px; background: #f7f4ee; color: #111827; }

.header { display: grid; grid-template-columns: 40px 1fr 56px; align-items: end; height: 76px; padding-bottom: 14px; }
.back { border: 0; background: none; font-size: 26px; line-height: 1; }
.header h1 { font-size: 17px; font-weight: 800; text-align: center; }
.edit { border: 0; background: none; color: #3B5BDB; font-size: 12px; font-weight: 700; text-align: right; }

/* 티켓 */
.ticket { position: relative; padding: 14px 18px 12px; border-radius: 18px; color: #fff; background: linear-gradient(145deg, #2949a3 0%, #173b86 60%, #102d6a 100%); box-shadow: 0 10px 24px rgba(23,55,127,.22); }
.ticket::before, .ticket::after { content: ''; position: absolute; top: 50%; width: 16px; height: 16px; border-radius: 50%; background: #f7f4ee; transform: translateY(-50%); }
.ticket::before { left: -8px; } .ticket::after { right: -8px; }
.ticket-top { display: flex; justify-content: space-between; align-items: center; font-size: 8px; color: rgba(255,255,255,.5); letter-spacing: .06em; }
.dday { color: #ff9b52; font-size: 12px; font-weight: 700; }
.ticket-route { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; font-size: 15px; font-weight: 800; }
.plane { color: #ff9b52; }
.ticket-dash { margin: 10px -18px; border-top: 1px dashed rgba(255,255,255,.25); }
.ticket-body { display: flex; align-items: center; gap: 20px; padding: 4px 0; }
.ring-wrap { position: relative; flex: none; }
.ring-text { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.ring-text strong { font-size: 20px; font-weight: 800; color: #ef4444; }
.ring-text small { font-size: 7px; color: rgba(255,255,255,.6); margin-top: 2px; }
.amounts { flex: 1; display: grid; gap: 8px; }
.amounts dt { color: rgba(255,255,255,.55); font-size: 9px; }
.amounts dd { font-size: 15px; font-weight: 800; letter-spacing: -.03em; margin-top: 2px; }
.amounts .secured { color: #ffd56a; }
.amounts .shortage { color: #ffa07a; }
.ticket-foot { display: flex; justify-content: space-between; align-items: center; font-size: 7px; color: rgba(255,255,255,.4); }
.barcode { color: rgba(255,255,255,.6); font-size: 11px; letter-spacing: -1px; }

/* 계좌 버튼 */
.account-btn { width: 100%; display: flex; align-items: center; gap: 10px; margin-top: 12px; padding: 13px 14px; border: 1px solid #dde5f1; border-radius: 16px; background: #fff; text-align: left; box-shadow: 0 4px 12px rgba(20,35,70,.05); }
.account-icon { display: grid; place-items: center; width: 34px; height: 34px; flex: none; border-radius: 50%; background: #fff3d6; }
.account-info strong { display: block; font-size: 13px; }
.account-info small { display: block; margin-top: 3px; color: #94a3b8; font-size: 10px; max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.account-right { margin-left: auto; text-align: right; }
.account-right b { display: block; color: #3B5BDB; font-size: 12px; }
.account-right span { color: #64748b; font-size: 11px; }
.chev { color: #cbd5e1; font-size: 18px; }

/* 타임라인 카드 */
.timeline-card { margin-top: 12px; padding: 15px 16px 14px; border: 1px solid #e2e8f0; border-radius: 16px; background: #fff; box-shadow: 0 4px 12px rgba(20,35,70,.05); }
.tl-head { display: flex; justify-content: space-between; align-items: center; }
.tl-head strong { font-size: 13px; }
.tl-track { position: relative; margin: 20px 6px 6px; height: 3px; }
.tl-line { position: absolute; inset: 0; border-radius: 99px; }
.tl-dot { position: absolute; top: 50%; width: 12px; height: 12px; border-radius: 50%; transform: translateY(-50%); }
.tl-dot:first-of-type { left: -1px; }
.tl-dot.mid { left: 50%; transform: translate(-50%, -50%); }
.tl-dot.end { right: -1px; }
.tl-labels { display: flex; justify-content: space-between; margin-top: 8px; }
.tl-lbl { display: grid; gap: 2px; }
.tl-lbl.center { text-align: center; }
.tl-lbl.right { text-align: right; }
.tl-lbl small { color: #94a3b8; font-size: 9px; }
.tl-lbl b { font-size: 10px; color: #111827; }

/* 달성 전망 카드 */
.forecast { margin-top: 12px; padding: 15px 14px 14px; border: 1px solid; border-radius: 16px; }
.fc-head { display: flex; align-items: center; gap: 9px; }
.fc-icon { display: grid; place-items: center; width: 26px; height: 26px; flex: none; border-radius: 50%; color: #fff; font-size: 13px; font-weight: 700; }
.fc-head strong { font-size: 12px; color: #111827; line-height: 1.35; }
.fc-hr { margin: 12px 0; border: 0; border-top: 1px solid rgba(0,0,0,.07); }
.fc-label { color: #94a3b8; font-size: 10px; }
.fc-row { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.fc-amount { font-size: 22px; font-weight: 800; }
.fc-pill { padding: 5px 14px; border-radius: 99px; font-size: 10px; font-weight: 600; }
.fc-detail-wrap { margin-top: 10px; padding: 10px 12px; border-radius: 10px; background: rgba(255,255,255,.7); }
.fc-detail-eyebrow { display: block; color: #94a3b8; font-size: 9px; margin-bottom: 3px; }
.fc-detail { font-size: 11px; color: #64748b; line-height: 1.5; margin: 0; }

/* 월 저축 플랜 카드 */
.plan-card { display: flex; align-items: center; gap: 12px; margin-top: 12px; padding: 14px; border: 1px solid #e2e8f0; border-radius: 16px; background: #fff; box-shadow: 0 4px 12px rgba(20,35,70,.05); }
.plan-icon { display: grid; place-items: center; width: 36px; height: 36px; flex: none; border-radius: 10px; background: #eef2ff; }
.plan-info small { display: block; color: #94a3b8; font-size: 10px; }
.plan-info strong { display: block; margin-top: 4px; color: #3B5BDB; font-size: 17px; font-weight: 800; }
.plan-right { margin-left: auto; text-align: right; }
.plan-right b { display: block; font-size: 11px; }
.plan-right small { display: block; margin-top: 3px; color: #94a3b8; font-size: 10px; }

/* CTA */
.cta { position: fixed; z-index: 5; left: 50%; bottom: 22px; width: min(358px, calc(100% - 32px)); height: 54px; transform: translateX(-50%); border: 0; border-radius: 14px; color: #fff; background: #173b86; font-size: 15px; font-weight: 800; box-shadow: 0 8px 20px rgba(23,59,134,.2); }
</style>
