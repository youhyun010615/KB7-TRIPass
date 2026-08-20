<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTravelStore } from '@/stores/travel'

const router = useRouter()
const travel = useTravelStore()
const money = (v) => `${Number(v).toLocaleString('ko-KR')}원`

// 처음 진입 시 기본 배분 설정 (KB: 1,500,000 / 신한: 500,000)
onMounted(() => {
  if (Object.keys(travel.allocations).length === 0) {
    travel.setAllocation(1, 1_500_000)
    travel.setAllocation(2, 500_000)
  }
})

const rows = computed(() =>
  travel.accounts.map((acc) => ({
    ...acc,
    allocated: Number(travel.allocations[acc.id] ?? 0),
    selected: Number(travel.allocations[acc.id] ?? 0) > 0,
  }))
)

const selectedRows = computed(() => rows.value.filter((r) => r.selected))
const totalAllocated = computed(() => rows.value.reduce((s, r) => s + r.allocated, 0))

const editingId = ref(null)
const editingRaw = ref('')

function toggle(acc) {
  if (acc.selected) {
    travel.setAllocation(acc.id, 0)
  } else {
    travel.setAllocation(acc.id, Math.min(acc.balance, 1_000_000))
  }
}

function startEdit(acc) {
  editingId.value = acc.id
  editingRaw.value = String(acc.allocated)
}

function onInput(e) {
  editingRaw.value = e.target.value.replace(/[^0-9]/g, '')
}

function commitEdit(acc) {
  const v = Math.max(0, Math.min(Number(editingRaw.value) || 0, acc.balance))
  travel.setAllocation(acc.id, v)
  editingId.value = null
}
</script>

<template>
  <main class="page">
    <!-- 헤더 -->
    <header class="header">
      <button class="back" @click="router.back()">‹</button>
      <h1>여행 자금 반영 계좌</h1>
      <span />
    </header>

    <!-- 파란 요약 바 -->
    <section class="summary-bar">
      <div class="sb-left">
        <small>여행 자금으로 전환할 금액 합계</small>
        <strong>{{ money(totalAllocated) }}</strong>
      </div>
      <span class="sb-badge">{{ selectedRows.length }}개 계좌 반영</span>
    </section>

    <!-- 연동 계좌 선택 라벨 -->
    <h2>연동 계좌 선택</h2>

    <!-- 계좌 카드 목록 -->
    <div class="account-list">
      <div
        v-for="acc in rows"
        :key="acc.id"
        class="account-card"
        :class="{ selected: acc.selected }"
      >
        <!-- 상단: 체크박스 + 은행 정보 + 잔액 -->
        <div class="card-top">
          <button
            class="checkbox"
            :class="{ on: acc.selected }"
            @click="toggle(acc)"
          >
            <svg v-if="acc.selected" width="12" height="12" viewBox="0 0 24 24" fill="none">
              <path d="M5 13L9 17L19 7" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <div class="bank-info">
            <strong>{{ acc.name }}</strong>
            <small>현재 잔액 {{ money(acc.balance) }}</small>
          </div>
        </div>

        <!-- 하단: 인정 금액 (선택 시) -->
        <div v-if="acc.selected" class="alloc-row">
          <span>여행 자금으로 사용할 금액</span>
          <div class="alloc-right" @click="startEdit(acc)">
            <template v-if="editingId === acc.id">
              <input
                :value="editingRaw"
                inputmode="numeric"
                class="alloc-input"
                @input="onInput"
                @blur="commitEdit(acc)"
                @keydown.enter="commitEdit(acc)"
                autofocus
                @click.stop
              />
              <span class="won">원</span>
            </template>
            <template v-else>
              <span class="alloc-value">{{ money(acc.allocated) }}</span>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                <path d="M11 4H4C3.44772 4 3 4.44772 3 5V20C3 20.5523 3.44772 21 4 21H19C19.5523 21 20 20.5523 20 19V12" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
                <path d="M18.5 2.5L21.5 5.5L12 15H9V12L18.5 2.5Z" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </template>
          </div>
        </div>
        <p v-if="acc.selected && acc.allocated > acc.balance" class="over-error">잔액({{ money(acc.balance) }})을 초과했어요.</p>
      </div>
    </div>

    <!-- 계좌 추가 버튼 -->
    <button class="add-btn">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="9" stroke="#3B5BDB" stroke-width="2"/>
        <path d="M12 8V16M8 12H16" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </button>

    <!-- CTA -->
    <button class="cta" @click="router.back()">확인</button>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 0 16px 100px; background: #f7f4ee; color: #111827; }

.header { display: grid; grid-template-columns: 40px 1fr 40px; align-items: end; height: 76px; padding-bottom: 14px; }
.back { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 12px; background: #fff; color: #193d82; font-size: 24px; font-weight: 700; box-shadow: 0 5px 16px rgba(36, 72, 117, 0.07); }
.header h1 { font-size: 17px; font-weight: 800; text-align: center; }

/* 파란 요약 바 */
.summary-bar { display: flex; justify-content: space-between; align-items: center; padding: 16px 18px; border-radius: 16px; background: linear-gradient(135deg, #2949a3, #173b86); color: #fff; margin-bottom: 20px; }
.sb-left small { display: block; color: rgba(255,255,255,.55); font-size: 9px; margin-bottom: 5px; }
.sb-left strong { font-size: 22px; font-weight: 800; }
.sb-badge { padding: 6px 14px; border-radius: 99px; background: rgba(255,255,255,.18); font-size: 11px; font-weight: 600; white-space: nowrap; }

h2 { font-size: 14px; font-weight: 700; margin-bottom: 12px; color: #64748b; }

/* 계좌 카드 */
.account-list { display: grid; gap: 10px; }
.account-card { border: 1.5px solid #e2e8f0; border-radius: 16px; background: #fff; overflow: hidden; transition: border-color .2s; }
.account-card.selected { border-color: #3B5BDB; }

.card-top { display: flex; align-items: center; gap: 12px; padding: 16px; }
.checkbox { display: grid; place-items: center; width: 24px; height: 24px; flex: none; border: 1.5px solid #cbd5e1; border-radius: 7px; background: #fff; cursor: pointer; transition: background .15s, border-color .15s; }
.checkbox.on { border-color: #3B5BDB; background: #3B5BDB; }
.bank-info strong { display: block; font-size: 14px; }
.bank-info small { display: block; margin-top: 3px; color: #94a3b8; font-size: 11px; }

/* 인정 금액 행 */
.alloc-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-top: 1px solid #f1f5f9; font-size: 12px; color: #64748b; }
.alloc-right { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.alloc-value { font-size: 16px; font-weight: 700; color: #3B5BDB; }
.alloc-input { width: 90px; border: 0; border-bottom: 2px solid #3B5BDB; outline: 0; background: transparent; font-size: 16px; font-weight: 700; color: #3B5BDB; text-align: right; }
.won { font-size: 13px; color: #3B5BDB; }
.over-error { padding: 6px 16px 12px; color: #e5484d; font-size: 10px; }

/* 계좌 추가 */
.add-btn { display: grid; place-items: center; width: 44px; height: 44px; margin: 14px auto 0; border: 1.5px dashed #3B5BDB; border-radius: 50%; background: transparent; cursor: pointer; }

/* CTA */
.cta { position: fixed; z-index: 5; left: 50%; bottom: 22px; width: min(358px, calc(100% - 32px)); height: 54px; transform: translateX(-50%); border: 0; border-radius: 14px; color: #fff; background: #173b86; font-size: 15px; font-weight: 800; box-shadow: 0 8px 20px rgba(23,59,134,.2); }
</style>
