<script setup>
import { computed } from 'vue';
import { useExchangeStore } from '@/stores/exchange';

const exchange = useExchangeStore();

const format = (v, d = 2) =>
  Number(v || 0).toLocaleString('ko-KR', { maximumFractionDigits: d });

const sentAmount = computed(() => format(exchange.krwAmount, 0));
const receivedAmount = computed(() => format(exchange.foreignAmount));
const amountClass = (value) => ({
  compact: String(value).length > 10,
  tiny: String(value).length > 14,
});

function updateKrw(event) {
  const el = event.target;
  const oldValue = el.value;
  const selectionStart = el.selectionStart;

  // 1. 숫자만 추출하고 최대 15자리로 제한 (Number.MAX_SAFE_INTEGER 범위 내 안전 보장)
  const rawDigits = oldValue.replace(/[^0-9]/g, '');
  const digits = rawDigits.slice(0, 15);

  exchange.krwAmount = Number(digits) || 0;
  const newValue = format(exchange.krwAmount, 0);

  // 2. 커서 앞의 실제 숫자 개수 구하기
  const oldTextToLeft = oldValue.slice(0, selectionStart);
  const digitCountToLeft = oldTextToLeft.replace(/[^0-9]/g, '').length;

  // 3. 입력 제한(15자리)에 따른 보정
  const finalDigitCountToLeft = Math.min(digitCountToLeft, digits.length);

  // 4. DOM 값 직접 반영
  el.value = newValue;

  // 5. 새 문자열에서 해당 숫자 개수만큼 떨어진 위치로 커서 계산 및 복원
  let newCursorPos = 0;
  let digitsCount = 0;
  for (let i = 0; i <= newValue.length; i++) {
    if (digitsCount === finalDigitCountToLeft) {
      newCursorPos = i;
      break;
    }
    if (i < newValue.length && /[0-9]/.test(newValue[i])) {
      digitsCount++;
    }
  }

  el.setSelectionRange(newCursorPos, newCursorPos);
}
</script>

<template>
  <section class="calculator">
    <h2>빠른 환율 계산</h2>
    <div>
      <label
        >보내는 금액
        <input
          :value="sentAmount"
          :class="amountClass(sentAmount)"
          inputmode="numeric"
          @input="updateKrw"
        />
        <b>KRW</b>
      </label>
      <span>→</span>
      <label
        >받는 금액
        <strong :class="amountClass(receivedAmount)">{{
          receivedAmount
        }}</strong>
        <b>{{ exchange.selectedCode }}</b>
      </label>
    </div>
  </section>
</template>

<style scoped>
.calculator {
  margin-top: 20px;
  padding: 15px;
  border: 1px solid #e1e6ed;
  border-radius: 15px;
  background: #fff;
}
.calculator h2 {
  font-size: 11px;
}
.calculator > div {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 20px minmax(0, 1fr);
  align-items: end;
  gap: 4px;
  margin-top: 12px;
}
.calculator label {
  display: grid;
  padding: 10px;
  border-radius: 10px;
  background: #f7f8fa;
  color: #8c97a7;
  font-size: 7px;
}
.calculator input,
.calculator strong {
  font-size: 14px;
  font-weight: 900;
}
</style>
