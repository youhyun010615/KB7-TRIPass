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
  const digits = event.target.value.replace(/[^0-9]/g, '').slice(0, 16);
  exchange.krwAmount = Number(digits) || 0;
  event.target.value = format(exchange.krwAmount, 0);
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
