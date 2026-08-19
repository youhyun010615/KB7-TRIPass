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
    <div class="calculator-heading">
      <div>
        <small>QUICK CONVERTER</small>
        <h2>빠른 환율 계산</h2>
      </div>
      <span>{{ exchange.selectedCode }} 환율 적용</span>
    </div>
    <div class="calculator-fields">
      <label>
        <span class="field-label">보내는 금액</span>
        <span class="amount-row">
          <input
            :value="sentAmount"
            :class="amountClass(sentAmount)"
            inputmode="numeric"
            aria-label="보내는 원화 금액"
            @input="updateKrw"
          />
          <b>KRW</b>
        </span>
      </label>
      <span class="convert-arrow" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <path d="M5 12h14m-5-5 5 5-5 5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </span>
      <label class="result-field">
        <span class="field-label">받는 금액</span>
        <span class="amount-row">
          <strong :class="amountClass(receivedAmount)">{{ receivedAmount }}</strong>
          <b>{{ exchange.selectedCode }}</b>
        </span>
      </label>
    </div>
  </section>
</template>

<style scoped>
.calculator {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid #bcd2ff;
  border-radius: 22px;
  background: linear-gradient(150deg, #eff5ff 0%, #e6f0ff 100%);
  box-shadow: 0 10px 26px rgba(23, 43, 77, .055);
}
.calculator-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}
.calculator-heading small {
  display: block;
  margin-bottom: 3px;
  color: #2f6fea;
  font-family: 'Space Mono', monospace;
  font-size: 8px;
  font-weight: 800;
  letter-spacing: .12em;
}
.calculator h2 {
  color: #173f8d;
  font-size: 15px;
  font-weight: 800;
}
.calculator-heading > span {
  padding: 5px 8px;
  border-radius: 999px;
  background: #edf3ff;
  color: #3268c5;
  font-size: 8px;
  font-weight: 700;
  white-space: nowrap;
}
.calculator-fields {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  margin-top: 14px;
}
.calculator label {
  display: block;
  min-width: 0;
  padding: 12px;
  border: 1px solid #e8edf5;
  border-radius: 14px;
  background: #f7f9fc;
  color: #8d99ad;
}
.calculator label:focus-within {
  border-color: #9bbaf1;
  background: #f3f7ff;
}
.field-label {
  display: block;
  font-size: 9px;
  font-weight: 600;
}
.amount-row {
  display: flex;
  align-items: baseline;
  gap: 5px;
  min-width: 0;
  margin-top: 7px;
}
.calculator input,
.calculator strong {
  display: block;
  width: 100%;
  min-width: 0;
  color: #17233a;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: -.025em;
  line-height: 1.2;
}
.calculator input {
  border: 0;
  outline: 0;
  background: transparent;
}
.calculator input.compact,
.calculator strong.compact { font-size: 12px; }
.calculator input.tiny,
.calculator strong.tiny { font-size: 10px; }
.result-field {
  background: #f1f5fd !important;
}
.calculator b {
  flex: 0 0 auto;
  color: #53647e;
  font-size: 8.5px;
  font-weight: 800;
}
.convert-arrow {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  align-self: center;
  border: 3px solid #fff;
  border-radius: 50%;
  background: #173b82;
  color: #fff;
  box-shadow: 0 5px 12px rgba(23, 59, 130, .22);
}
.convert-arrow svg {
  width: 15px;
  height: 15px;
}
@media (max-width: 360px) {
  .calculator { padding: 15px; }
  .calculator-fields { grid-template-columns: minmax(0, 1fr) 28px minmax(0, 1fr); gap: 3px; }
  .convert-arrow { width: 28px; height: 28px; }
  .calculator label { padding: 10px 8px; }
}
</style>
