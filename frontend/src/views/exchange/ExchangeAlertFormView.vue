<script setup>
import { computed, reactive, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useExchangeStore } from '@/stores/exchange';

const route = useRoute();
const router = useRouter();
const exchange = useExchangeStore();

const isModalOpen = ref(false);

const existing = computed(() =>
  exchange.alerts.find(
    (item) => String(item.id) === String(route.params.alertId),
  ),
);

// 데이터 구조 업데이트 반영 (currencyCode, targetRate, targetAmount)
const defaultCode =
  route.query.code || existing.value?.currencyCode || exchange.selectedCode;

const form = reactive({
  id: existing.value?.id,
  currencyCode: defaultCode,
  targetRate:
    existing.value?.targetRate || exchange.getCurrency(defaultCode)?.rate || 0,
  targetAmount: existing.value?.targetAmount || 100000,
  enabled: existing.value?.enabled ?? true,
});

const currency = computed(() => exchange.getCurrency(form.currencyCode));

// 금융 숫자 포맷팅 (입력용)
const targetRateDisplay = computed({
  get: () => form.targetRate.toLocaleString('ko-KR'),
  set: (val) => {
    form.targetRate = Number(val.replace(/[^0-9]/g, ''));
  },
});

const targetAmountDisplay = computed({
  get: () => form.targetAmount.toLocaleString('ko-KR'),
  set: (val) => {
    form.targetAmount = Number(val.replace(/[^0-9]/g, ''));
  },
});

const valid = computed(
  () => form.currencyCode && form.targetRate > 0 && form.targetAmount > 0,
);
const expected = computed(() =>
  exchange.expectedForeign(
    form.targetAmount,
    form.targetRate,
    currency.value?.unit,
  ),
);
const format = (v) =>
  Number(v || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 });

function save() {
  if (valid.value) {
    exchange.saveAlert({ ...form });
    router.replace('/exchange/alerts');
  }
}
function remove() {
  exchange.removeAlert(form.id);
  router.replace('/exchange/alerts');
}

function selectCurrency(code) {
  form.currencyCode = code;
  isModalOpen.value = false;
  form.targetRate = exchange.getCurrency(code)?.rate || 0;
}

onMounted(async () => {
  if (exchange.currencies.length === 0) {
    await exchange.updateExchangeRates();
    // 만약 다이렉트 진입으로 인해 환율정보가 없어 targetRate가 0으로 세팅되어 있었다면 보정
    if (form.targetRate === 0) {
      form.targetRate = exchange.getCurrency(form.currencyCode)?.rate || 0;
    }
  }
});
</script>

<template>
  <main class="page">
    <div class="shell">
      <header>
        <button @click="router.back()">‹</button>
        <h1>환율 알림 설정</h1>
      </header>
      <section class="current">
        <small>현재 주요 환율</small
        ><strong>{{ format(currency?.rate) }}원</strong
        ><em :class="{ up: currency?.change > 0 }"
          >{{ currency?.change > 0 ? '▲' : '▼' }}
          {{ format(Math.abs(currency?.change || 0)) }}원</em
        >
      </section>

      <!-- 커스텀 통화 선택 버튼 -->
      <label
        >통화
        <button class="currency-selector" @click="isModalOpen = true">
          <span :class="currency?.flagClass" class="flag-icon"></span>
          {{ form.currencyCode }} · {{ currency?.name }}
        </button>
      </label>

      <label
        >목표 환율
        <div>
          <span>1 {{ form.currencyCode }} =</span
          ><input v-model="targetRateDisplay" type="text" /><b>원</b>
        </div></label
      ><label
        >환전 예정 금액
        <div>
          <input v-model="targetAmountDisplay" type="text" /><b>원</b>
        </div></label
      >
      <section class="result">
        <small>목표 환율에 도달하면</small
        ><strong
          >약 {{ format(expected) }} {{ form.currencyCode }} 환전 가능</strong
        >
        <p>
          현재 환율 대비
          {{ format(Math.abs((currency?.rate || 0) - form.targetRate)) }}원 차이
        </p>
      </section>
      <button class="save" :disabled="!valid" @click="save">
        환율 알림 저장</button
      ><button v-if="form.id" class="delete" @click="remove">알림 삭제</button>
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
  padding: 52px 18px 30px;
  background: #f7f5ef;
}
header {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
}
header button {
  width: 28px;
  font-size: 25px;
}
header h1 {
  flex: 1;
  padding-right: 28px;
  text-align: center;
  font-size: 18px;
  font-weight: 900;
}
.current,
label,
.result {
  display: block;
  margin-top: 12px;
  padding: 15px;
  border: 1px solid #e1e6ed;
  border-radius: 14px;
  background: #fff;
}
.current {
  position: relative;
}
.current small,
.current strong {
  display: block;
}
.current small {
  color: #8b97a8;
  font-size: 8px;
}
.current strong {
  margin-top: 7px;
  color: #174494;
  font-size: 20px;
}
.current em {
  position: absolute;
  right: 15px;
  bottom: 18px;
  color: #0a9e73;
  font-size: 8px;
  font-style: normal;
}
.current em.up {
  color: #dd675b;
}
label {
  color: #66758a;
  font-size: 9px;
}
/* 통화 선택 커스텀 버튼 */
.currency-selector {
  width: 100%;
  margin-top: 8px;
  padding: 12px;
  border: 1px solid #e2e7ed;
  border-radius: 10px;
  background: #fff;
  color: #10192d;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
}
.flag-icon {
  display: block;
  width: 20px;
  height: 15px;
}
label > div {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  margin-top: 8px;
  padding: 12px;
  border: 1px solid #e2e7ed;
  border-radius: 10px;
  background: #fff;
}
label input {
  min-width: 0;
  flex: 1;
  text-align: right;
  font-size: 14px;
  font-weight: 900;
  outline: none;
  border: none;
}
label span,
label b {
  font-size: 8px;
}
.result small,
.result strong {
  display: block;
}
.result small {
  color: #8995a6;
  font-size: 8px;
}
.result strong {
  margin-top: 7px;
  color: #1472ee;
  font-size: 14px;
}
.result p {
  margin-top: 7px;
  color: #079d72;
  font-size: 8px;
}
.save,
.delete {
  width: 100%;
  margin-top: 14px;
  padding: 14px;
  border-radius: 11px;
  background: #173f8d;
  color: #fff;
  font-weight: 900;
}
.save:disabled {
  background: #aab4c3;
}
.delete {
  margin-top: 8px;
  background: #fff0f0;
  color: #df554e;
}
</style>
