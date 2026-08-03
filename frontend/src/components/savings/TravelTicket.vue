<script setup>
defineProps({
  eyebrow: { type: String, default: 'TRIPASS · BOARDING PASS' },
  title: { type: String, default: '' },
  meta: { type: String, default: '' },
})
</script>

<template>
  <section class="travel-ticket">
    <div class="ticket-topline">
      <span>{{ eyebrow }}</span>
      <span>✈ GOAL</span>
    </div>
    <div v-if="title || meta" class="ticket-heading">
      <strong>{{ title }}</strong>
      <span>{{ meta }}</span>
    </div>
    <div class="ticket-rule" />
    <slot />
    <div class="ticket-footer">
      <span>PASS NO. TRP-260815</span>
      <span class="barcode">▥▥▥▥▥▥</span>
    </div>
  </section>
</template>

<style scoped>
.travel-ticket {
  position: relative;
  overflow: visible;
  border-radius: 16px;
  padding: 15px 17px 12px;
  color: #fff;
  background: linear-gradient(135deg, #2949a3 0%, #17377f 58%, #102d6a 100%);
  box-shadow: 0 10px 24px rgba(23, 55, 127, 0.2);
}
.travel-ticket::before,
.travel-ticket::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #f5f3ee;
  transform: translateY(-50%);
}
.travel-ticket::before { left: -7px; }
.travel-ticket::after { right: -7px; }
.ticket-topline,
.ticket-heading,
.ticket-footer { display: flex; align-items: center; justify-content: space-between; }
.ticket-topline { font-size: 8px; letter-spacing: .07em; color: #cbd9ff; }
.ticket-heading { margin-top: 10px; }
.ticket-heading strong { font-size: 14px; }
.ticket-heading span { font-size: 11px; color: #ffb25f; font-weight: 700; }
.ticket-rule { margin: 10px 0 12px; border-top: 1px dashed rgba(255,255,255,.45); }
.ticket-footer { margin-top: 12px; padding-top: 9px; border-top: 1px dashed rgba(255,255,255,.35); font-size: 7px; color: #b9c7ed; }
.barcode { color: #fff; font-size: 11px; letter-spacing: -2px; }
</style>
