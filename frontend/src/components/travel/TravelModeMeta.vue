<script setup>
import { flagIconClass } from '@/stores/travel'

defineProps({
  tripName: { type: String, default: '나의 여행' },
  day: { type: Number, default: 0 },
  countryName: { type: String, default: '' },
  countryCode: { type: String, default: '' },
})
</script>

<template>
  <div class="travel-mode-meta" aria-label="현재 여행 정보">
    <strong>{{ tripName || '나의 여행' }}</strong>
    <span class="meta-divider" aria-hidden="true" />
    <span class="meta-day">DAY {{ day }}</span>
    <template v-if="countryName">
      <span class="meta-divider" aria-hidden="true" />
      <span class="meta-now">
        <b>NOW</b>
        <span>{{ countryName }}</span>
        <em v-if="countryCode" :class="flagIconClass(countryCode)" />
      </span>
    </template>
  </div>
</template>

<style scoped>
.travel-mode-meta{display:flex;height:38px;align-items:center;gap:8px;margin-top:10px;padding:0 11px;border:1px solid #d5e1f2;border-radius:12px;background:rgba(255,255,255,.9);box-shadow:0 5px 14px rgba(31,63,116,.07);color:#5f6f87;white-space:nowrap}.travel-mode-meta>strong{min-width:0;overflow:hidden;flex:1;color:#173f8d;font-size:11px;font-weight:900;text-overflow:ellipsis}.meta-divider{width:1px;height:13px;flex:0 0 auto;background:#d7e0ec}.meta-day{flex:0 0 auto;padding:4px 8px;border-radius:999px;background:#ffd45e;color:#173f8d;font-family:'Space Mono',monospace;font-size:9px;font-weight:950;box-shadow:0 2px 7px rgba(207,152,0,.2)}.meta-now{display:flex;flex:0 0 auto;align-items:center;gap:4px;padding:4px 7px;border:1px solid rgba(38,98,234,.2);border-radius:999px;background:#edf4ff;font-size:9px;font-weight:800;animation:meta-now-pulse 2s ease-in-out infinite}.meta-now b{color:#2662ea;font-family:'Space Mono',monospace;font-size:8px;font-weight:950;letter-spacing:.04em}.meta-now em{display:block;width:20px;height:14px;border-radius:3px;background-size:cover;box-shadow:0 1px 3px rgba(0,0,0,.14)}@keyframes meta-now-pulse{0%,100%{box-shadow:0 0 0 0 rgba(38,98,234,.12)}50%{box-shadow:0 0 0 4px rgba(38,98,234,0)}}@media(prefers-reduced-motion:reduce){.meta-now{animation:none}}
</style>
