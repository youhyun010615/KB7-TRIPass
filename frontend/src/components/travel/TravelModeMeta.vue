<script setup>
import { flagIconClass } from '@/stores/travel'

defineProps({
  tripName: { type: String, default: '나의 여행' },
  dateRange: { type: String, default: '' },
  day: { type: Number, default: 0 },
  countryName: { type: String, default: '' },
  countryCode: { type: String, default: '' },
  countryCodes: { type: Array, default: () => [] },
})
</script>

<template>
  <div class="travel-mode-meta" aria-label="현재 여행 정보">
    <div class="meta-trip-copy">
      <div class="meta-trip-title">
        <strong>{{ tripName || '나의 여행' }}</strong>
        <span v-if="countryCodes.length" class="meta-trip-flags" aria-label="여행 국가">
          <em v-for="code in countryCodes" :key="code" :class="flagIconClass(code)" />
        </span>
      </div>
      <small v-if="dateRange">{{ dateRange }}</small>
    </div>
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
.travel-mode-meta{display:flex;height:45px;min-height:45px;box-sizing:border-box;align-items:center;gap:9px;margin-top:10px;padding:6px 12px;border:0;border-radius:14px;background:#111;box-shadow:0 5px 14px rgba(0,0,0,.14);color:#fff;white-space:nowrap}.meta-trip-copy{display:flex;min-width:0;flex:1;flex-direction:column;gap:2px}.meta-trip-title{display:flex;min-width:0;align-items:center;gap:5px}.meta-trip-title>strong{overflow:hidden;color:#fff;font-size:12px;font-weight:900;text-overflow:ellipsis}.meta-trip-flags{display:flex;flex:0 0 auto;gap:2px}.meta-trip-flags em{display:block;width:19px;height:13px;border-radius:2px;background-size:cover;box-shadow:0 1px 3px rgba(0,0,0,.2)}.meta-trip-copy small{overflow:hidden;color:rgba(255,255,255,.68);font-family:'Space Mono',monospace;font-size:8px;font-weight:700;text-overflow:ellipsis}.meta-divider{width:1px;height:20px;flex:0 0 auto;background:rgba(255,255,255,.24)}.meta-day{flex:0 0 auto;padding:5px 9px;border-radius:999px;background:#ffd45e;color:#173f8d;font-family:'Space Mono',monospace;font-size:9px;font-weight:950;box-shadow:0 2px 7px rgba(207,152,0,.2)}.meta-now{display:flex;flex:0 0 auto;align-items:center;gap:4px;padding:5px 7px;border:1px solid rgba(255,255,255,.25);border-radius:999px;background:rgba(255,255,255,.12);color:#fff;font-size:9px;font-weight:800;animation:meta-now-pulse 2s ease-in-out infinite}.meta-now b{color:#ffd45e;font-family:'Space Mono',monospace;font-size:8px;font-weight:950;letter-spacing:.04em}.meta-now em{display:block;width:20px;height:14px;border-radius:3px;background-size:cover;box-shadow:0 1px 3px rgba(0,0,0,.2)}@keyframes meta-now-pulse{0%,100%{box-shadow:0 0 0 0 rgba(255,212,94,.12)}50%{box-shadow:0 0 0 4px rgba(255,212,94,0)}}@media(prefers-reduced-motion:reduce){.meta-now{animation:none}}
</style>
