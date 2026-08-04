<script setup>
defineProps({
  country: { type: Object, required: true },
  income: { type: Number, required: true },
  expense: { type: Number, required: true },
})

const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
</script>

<template>
  <section class="fund-ticket" :style="{ background: country.ticketGradient }">
    <span class="ticket-notch ticket-notch-left" />
    <span class="ticket-notch ticket-notch-right" />
    <header>
      <b>{{ country.flag }} {{ country.city }} 여행</b>
      <span>D-230</span>
    </header>
    <div class="perforation" />
    <div class="ticket-values">
      <div>
        <small>＋ 월 수입</small>
        <strong>{{ money(income) }}</strong>
      </div>
      <div>
        <small>－ 고정지출</small>
        <strong>{{ money(expense) }}</strong>
      </div>
    </div>
    <div class="ticket-barcode" aria-hidden="true">
      <i v-for="(height, index) in [8,5,10,6,9,5,11,7,10,5,8,11]" :key="index" :style="{ height: `${height}px` }" />
    </div>
  </section>
</template>

<style scoped>
.fund-ticket { position: relative; overflow: visible; min-height: 116px; padding: 14px 17px 12px; border-radius: 15px; color: #fff; box-shadow: 0 9px 18px rgba(20, 42, 92, .18); }
.fund-ticket header { display: flex; justify-content: space-between; align-items: center; font-size: 12px; }
.fund-ticket header span { font-size: 9px; font-weight: 800; color: rgba(255,255,255,.78); }
.perforation { margin: 12px -5px 10px; border-top: 1px dashed rgba(255,255,255,.55); }
.ticket-values { display: grid; grid-template-columns: 1fr 1fr; }
.ticket-values > div { display: flex; flex-direction: column; gap: 4px; }
.ticket-values > div + div { padding-left: 15px; border-left: 1px dashed rgba(255,255,255,.45); }
.ticket-values small { font-size: 10px; font-weight: 700; color: rgba(255,255,255,.82); }
.ticket-values strong { font-size: 21px; line-height: 1; letter-spacing: -.6px; }
.ticket-notch { position: absolute; top: 35px; width: 14px; height: 14px; border-radius: 50%; background: #f7f4ee; }
.ticket-notch-left { left: -7px; }
.ticket-notch-right { right: -7px; }
.ticket-barcode { position: absolute; right: 15px; bottom: 6px; display: flex; align-items: end; gap: 2px; opacity: .72; }
.ticket-barcode i { display: block; width: 1.5px; background: white; }
</style>
