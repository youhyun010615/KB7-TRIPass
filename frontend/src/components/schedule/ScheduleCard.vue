<script setup>
import { computed } from 'vue'
import { useTravelScheduleStore } from '@/stores/travelSchedule'

const props = defineProps({ schedule: { type: Object, required: true }, compact: { type: Boolean, default: false } })
const emit = defineEmits(['detail'])
const store = useTravelScheduleStore()
const item = computed(() => store.normalizeSchedule(props.schedule))
const country = computed(() => store.countries.find(entry => entry.code === item.value.countryCode))
const paymentLabel = computed(() => ({ prepaid: '사전결제 완료', onsite: '현장결제 필요', undecided: '미정' })[item.value.paymentStatus] || '미정')
</script>

<template>
  <article class="schedule-card" :class="[`status-${item.paymentStatus}`, { compact }]">
    <div class="schedule-time"><time>{{ item.time }}</time><span>{{ country?.flag }}</span></div>
    <div class="schedule-copy">
      <h4>{{ item.title }}</h4>
      <strong>{{ item.currency }} {{ Number(item.amount || 0).toLocaleString() }}</strong>
      <p><b>{{ item.placeName || '장소 미정' }}</b><small>{{ item.placeAddress || '주소 미정' }}</small></p>
    </div>
    <div class="schedule-side"><em>{{ paymentLabel }}</em><button type="button" @click.stop="emit('detail', item.id)">상세 정보 <span>›</span></button></div>
  </article>
</template>

<style scoped>
.schedule-card{display:grid;grid-template-columns:54px minmax(0,1fr) auto;gap:12px;width:100%;padding:14px;border:1px solid #dce7f4;border-radius:16px;background:#f8fbff;color:#10192d;text-align:left}.schedule-time{display:flex;flex-direction:column;align-items:center;gap:7px}.schedule-time time{width:52px;padding:9px 3px;border-radius:10px;background:#e7f1ff;color:#195fc7;text-align:center;font-size:12px;font-weight:900}.schedule-time span{font-size:18px}.schedule-copy{min-width:0}.schedule-copy h4{overflow:hidden;font-size:13px;line-height:1.35;text-overflow:ellipsis;white-space:nowrap}.schedule-copy>strong{display:block;margin-top:5px;color:#1d64ca;font-size:11px}.schedule-copy p{margin-top:8px}.schedule-copy p>*{display:block}.schedule-copy p b{font-size:10px}.schedule-copy p small{overflow:hidden;margin-top:3px;color:#7e8b9e;font-size:9px;line-height:1.35;text-overflow:ellipsis;white-space:nowrap}.schedule-side{display:flex;min-width:74px;flex-direction:column;align-items:flex-end;justify-content:space-between}.schedule-side em{padding:6px 8px;border-radius:10px;background:#e6f1ff;color:#2472dd;font-size:8px;font-style:normal;font-weight:800;white-space:nowrap}.schedule-side button{color:#335f9e;font-size:9px;font-weight:800}.schedule-side button span{font-size:15px}.status-onsite{border-color:#f5c8c2;background:#fff6f4}.status-onsite .schedule-side em{background:#fff0ee;color:#db6258}.status-undecided{border-color:#e0e4ea;background:#fafafa}.status-undecided .schedule-side em{background:#eceff3;color:#657184}.compact{border-color:#ffffff2b;background:#ffffff18;color:#fff}.compact .schedule-time time{background:#ffffff20;color:#fff}.compact .schedule-copy>strong,.compact .schedule-side button{color:#dceaff}.compact .schedule-copy p small{color:#c6d8f4}.compact .schedule-side em{background:#fff;color:#2167ca}
</style>
