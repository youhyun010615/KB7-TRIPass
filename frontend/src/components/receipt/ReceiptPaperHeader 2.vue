<script setup>
import { ReceiptText } from '@lucide/vue'

defineProps({
  modeLabel: {
    type: String,
    default: 'TRIP RECEIPT',
  },
  reference: {
    type: [String, Number],
    default: 'NEW',
  },
})
</script>

<template>
  <div class="receipt-document-header">
    <div class="receipt-document-topline">
      <b>TRIPASS</b>
      <span>RECEIPT NO. {{ reference }}</span>
    </div>
    <div class="receipt-document-brand">
      <span><ReceiptText :size="23" :stroke-width="2.2" /></span>
      <div>
        <small>{{ modeLabel }}</small>
        <strong>TRIPASS RECEIPT</strong>
      </div>
    </div>
  </div>
</template>

<style scoped>
.receipt-document-header{margin:-22px -20px 19px}.receipt-document-topline{display:flex;align-items:center;justify-content:space-between;padding:12px 17px;border-radius:20px 20px 0 0;background:linear-gradient(135deg,#103579,#205bb9);color:#d5e4ff;font-size:7px;font-weight:900;letter-spacing:.13em}.receipt-document-topline b{color:#ffd466;font-size:9px}.receipt-document-brand{display:flex;align-items:center;gap:11px;padding:15px 18px;border-bottom:1px dashed #cbd5e3;background:#f8faff}.receipt-document-brand>span{display:grid;width:43px;height:43px;place-items:center;border-radius:14px;background:#e5efff;color:#245fb9}.receipt-document-brand>div{display:grid}.receipt-document-brand small{color:#8797ae;font-size:7px;font-weight:900;letter-spacing:.12em}.receipt-document-brand strong{margin-top:3px;color:#173f8d;font-size:13px;font-weight:950;letter-spacing:.02em}
</style>
