<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTravelStore } from '@/stores/travel'
import { useSavingsPlanStore } from '@/stores/savingsPlan'

const router = useRouter()
const travel = useTravelStore()
const plan = useSavingsPlanStore()

const money = (v) => `${Number(v).toLocaleString('ko-KR')}원`

const editingId = ref(null)
const editingValue = ref('')

const linkedAccounts = computed(() =>
  travel.accounts.map((acc) => ({
    ...acc,
    allocated: Number(travel.allocations[acc.id] ?? 0),
    selected: Number(travel.allocations[acc.id] ?? 0) > 0,
  }))
)

const totalLinkedAmount = computed(() =>
  linkedAccounts.value.reduce((sum, acc) => sum + acc.allocated, 0)
)
const linkedCount = computed(() => linkedAccounts.value.filter((acc) => acc.selected).length)

function toggleAccount(acc) {
  if (acc.selected) {
    travel.setAllocation(acc.id, 0)
  } else {
    travel.setAllocation(acc.id, acc.balance)
  }
}

function startEdit(acc) {
  editingId.value = acc.id
  editingValue.value = String(acc.allocated)
}

function commitEdit(acc) {
  const raw = Number(String(editingValue.value).replace(/[^0-9]/g, '')) || 0
  travel.setAllocation(acc.id, raw)
  editingId.value = null
}

function onInput(e) {
  editingValue.value = String(e.target.value).replace(/[^0-9]/g, '')
}
</script>

<template>
  <main class="accounts-page">
    <header class="page-header">
      <button aria-label="뒤로가기" @click="router.back()">‹</button>
      <h1>여행 자금 반영 계좌</h1>
      <span></span>
    </header>

    <!-- 요약 -->
    <section class="summary-card">
      <div class="summary-row">
        <span>반영 중인 계좌</span>
        <strong>{{ linkedCount }}개</strong>
      </div>
      <div class="summary-row total">
        <span>반영 총액</span>
        <strong>{{ money(totalLinkedAmount) }}</strong>
      </div>
      <div class="summary-bar">
        <div
          class="summary-fill"
          :style="{ width: `${Math.min(100, (totalLinkedAmount / plan.totalTargetAmount) * 100)}%` }"
        />
      </div>
      <div class="summary-hint">
        목표 금액 {{ money(plan.totalTargetAmount) }} 중
        <b>{{ Math.min(100, Math.round((totalLinkedAmount / plan.totalTargetAmount) * 100)) }}%</b> 확보
      </div>
    </section>

    <!-- 계좌 목록 -->
    <h2>연결된 계좌</h2>
    <div class="account-list">
      <div
        v-for="acc in linkedAccounts"
        :key="acc.id"
        class="account-card"
        :class="{ selected: acc.selected }"
      >
        <div class="account-top">
          <!-- 선택 토글 -->
          <button class="toggle-btn" :class="{ on: acc.selected }" @click="toggleAccount(acc)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path v-if="acc.selected" d="M5 13L9 17L19 7" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <div class="account-info">
            <strong>{{ acc.name }}</strong>
            <span>{{ acc.number }}</span>
          </div>
          <div class="account-balance">
            <small>잔액</small>
            <b>{{ money(acc.balance) }}</b>
          </div>
        </div>

        <!-- 인정 금액 (선택된 경우) -->
        <div v-if="acc.selected" class="allocation-row">
          <span>여행 자금 인정 금액</span>
          <div class="alloc-input-wrap" @click="startEdit(acc)">
            <template v-if="editingId === acc.id">
              <input
                :value="editingValue"
                inputmode="numeric"
                class="alloc-input editing"
                @input="onInput"
                @blur="commitEdit(acc)"
                @keydown.enter="commitEdit(acc)"
                autofocus
              />
              <span class="won">원</span>
            </template>
            <template v-else>
              <span class="alloc-value">{{ money(acc.allocated) }}</span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                <path d="M11 4H4C3.44772 4 3 4.44772 3 5V20C3 20.5523 3.44772 21 4 21H19C19.5523 21 20 20.5523 20 19V12" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
                <path d="M18.5 2.5L21.5 5.5L12 15H9V12L18.5 2.5Z" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </template>
          </div>
        </div>
        <p v-if="acc.selected && acc.allocated > acc.balance" class="alloc-error">
          잔액({{ money(acc.balance) }})을 초과했어요.
        </p>
      </div>
    </div>

    <!-- 계좌 추가 버튼 -->
    <button class="add-account-btn">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="9" stroke="#3B5BDB" stroke-width="2"/>
        <path d="M12 8V16M8 12H16" stroke="#3B5BDB" stroke-width="2" stroke-linecap="round"/>
      </svg>
      계좌 추가
    </button>

    <button class="primary-cta" @click="router.back()">
      확인
    </button>
  </main>
</template>

<style scoped>
.accounts-page { min-height: 100vh; padding: 0 18px 100px; background: #f7f4ee; color: #111827; }

.page-header {
  height: 76px;
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: end;
  padding-bottom: 14px;
}
.page-header button { border: 0; background: none; font-size: 24px; text-align: left; }
.page-header h1 { font-size: 17px; font-weight: 800; text-align: center; }

.summary-card {
  padding: 18px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 5px 14px rgba(20, 35, 70, .06);
}
.summary-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; font-size: 13px; }
.summary-row span { color: #64748b; }
.summary-row.total strong { font-size: 20px; color: #3B5BDB; }
.summary-bar { margin: 12px 0 6px; height: 6px; border-radius: 99px; background: #e2e8f0; overflow: hidden; }
.summary-fill { height: 100%; border-radius: 99px; background: #3B5BDB; transition: width .4s; }
.summary-hint { font-size: 11px; color: #94a3b8; }
.summary-hint b { color: #3B5BDB; }

h2 { margin: 20px 0 10px; font-size: 15px; font-weight: 700; }

.account-list { display: grid; gap: 10px; }
.account-card {
  padding: 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(20, 35, 70, .05);
  transition: border-color .2s;
}
.account-card.selected { border-color: #3B5BDB; background: #f5f8ff; }

.account-top { display: flex; align-items: center; gap: 10px; }
.toggle-btn {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  flex: none;
  border: 1.5px solid #cbd5e1;
  border-radius: 7px;
  background: #fff;
  cursor: pointer;
  transition: background .15s, border-color .15s;
}
.toggle-btn.on { border-color: #3B5BDB; background: #3B5BDB; }
.account-info { flex: 1; }
.account-info strong { display: block; font-size: 13px; }
.account-info span { display: block; margin-top: 2px; color: #94a3b8; font-size: 11px; }
.account-balance { text-align: right; }
.account-balance small { display: block; color: #94a3b8; font-size: 10px; }
.account-balance b { font-size: 13px; }

.allocation-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
  font-size: 12px;
  color: #64748b;
}
.alloc-input-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}
.alloc-value { font-size: 15px; font-weight: 700; color: #3B5BDB; }
.alloc-input {
  width: 100px;
  border: 0;
  border-bottom: 1.5px solid #3B5BDB;
  outline: 0;
  background: transparent;
  font-size: 15px;
  font-weight: 700;
  color: #3B5BDB;
  text-align: right;
}
.won { font-size: 12px; color: #3B5BDB; }
.alloc-error { margin-top: 6px; color: #e5484d; font-size: 10px; }

.add-account-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 12px;
  padding: 14px;
  border: 1.5px dashed #3B5BDB;
  border-radius: 16px;
  background: transparent;
  color: #3B5BDB;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.primary-cta {
  position: fixed;
  z-index: 5;
  left: 50%;
  bottom: 22px;
  width: min(354px, calc(100% - 36px));
  height: 54px;
  transform: translateX(-50%);
  border: 0;
  border-radius: 14px;
  color: #fff;
  background: #173b86;
  font-size: 14px;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(23, 59, 134, .18);
}
</style>
