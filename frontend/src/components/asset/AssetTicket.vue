<script setup>
defineProps({
  label: { type: String, required: true },
  amount: { type: Number, required: true },
  caption: { type: String, default: '' },
  action: { type: String, default: '' },
})
defineEmits(['action'])
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
</script>

<template>
  <section class="asset-ticket">
    <i class="notch left" /><i class="notch right" />
    <small>▣　{{ label }}</small>
    <strong>{{ money(amount) }}</strong>
    <button v-if="action" @click="$emit('action')">{{ action }} ›</button>
    <p v-if="caption">{{ caption }}</p>
    <span class="barcode">||||||||</span>
  </section>
</template>

<style scoped>
.asset-ticket{position:relative;min-height:130px;padding:20px;border-radius:17px;background:linear-gradient(115deg,#102b70,#0e62b9);color:white;box-shadow:0 10px 20px #173d8925}.asset-ticket small{display:block;color:#ffcf28;font-size:11px;font-weight:900}.asset-ticket strong{display:block;margin-top:12px;font-size:27px;letter-spacing:-1px}.asset-ticket button{position:absolute;right:17px;top:48px;padding:9px 12px;border-radius:18px;background:#ffffff28;font-size:10px;font-weight:800}.asset-ticket p{position:absolute;left:20px;bottom:16px;color:#ffffffa6;font-size:10px}.barcode{position:absolute;right:16px;bottom:10px;font-size:12px;letter-spacing:-2px;color:#ffffffbf}.notch{position:absolute;top:76px;width:14px;height:14px;border-radius:50%;background:#f4f6fc}.notch.left{left:-7px}.notch.right{right:-7px}
</style>
