<script setup>
import { computed } from 'vue'
import { useTravelScheduleStore } from '@/stores/travelSchedule'
import { flagIconClass } from '@/stores/travel'

const props = defineProps({
  schedule: { type: Object, required: true },
  compact: { type: Boolean, default: false },
  completed: { type: Boolean, default: false },
  today: { type: Boolean, default: false },
})
const emit = defineEmits(['detail'])
const store = useTravelScheduleStore()
const item = computed(() => store.normalizeSchedule(props.schedule))
const country = computed(() => store.countries.find(entry => entry.code === item.value.countryCode))
const paymentLabel = computed(() => ({ prepaid: '사전결제 완료', onsite: '현장결제 필요', undecided: '미정' })[item.value.paymentStatus] || '미정')
const hasAmount = computed(() => Number(item.value.amount) > 0)
</script>

<template>
  <article
    class="schedule-card"
    :class="[`status-${item.paymentStatus}`, { compact, 'is-completed': completed, 'is-today': today }]"
    role="button"
    tabindex="0"
    @click="emit('detail', item.id)"
    @keydown.enter="emit('detail', item.id)"
  >
    <span class="schedule-flag-wrap" aria-hidden="true">
      <span v-if="country" :class="flagIconClass(country.code)" class="fi-inline schedule-flag" />
      <span v-else class="schedule-flag-fallback">🌐</span>
    </span>
    <div class="schedule-copy">
      <div class="schedule-title-row">
        <h4>{{ item.title }}</h4>
        <em v-if="!completed">{{ paymentLabel }}</em>
      </div>
      <p class="schedule-meta">
        {{ item.time }}<template v-if="hasAmount"><span class="dot">·</span>{{ item.currency }} {{ Number(item.amount).toLocaleString() }}</template>
      </p>
      <p class="schedule-place">{{ item.placeName || '장소 미정' }}</p>
    </div>
  </article>
</template>

<style scoped>
.schedule-card {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  align-items: center;
  gap: 11px;
  width: 100%;
  min-height: 82px;
  padding: 12px 14px;
  border: 1px solid #edf0f5;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 7px 20px rgba(16, 25, 43, .07);
  color: #10192d;
  text-align: left;
  touch-action: pan-y;
}
.schedule-flag-wrap{display:grid;width:32px;height:32px;place-items:center;align-self:start;overflow:hidden;border-radius:50%;background:#eaf1ff;box-shadow:0 2px 7px rgba(11,42,107,.12)}.schedule-flag{width:32px;height:32px;border-radius:50%;background-size:cover}.schedule-flag-fallback{font-size:16px}
.schedule-copy {
  min-width: 0;
}
.schedule-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}
.schedule-copy h4 {
  min-width: 0;
  color: #10192d;
  font-size: 13.5px;
  font-weight: 850;
  line-height: 1.35;
}
.schedule-title-row em {
  flex: none;
  margin-top: 1px;
  padding: 3px 7px;
  border-radius: 8px;
  background: #eef2ff;
  color: #173f8d;
  font-size: 9px;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}
.schedule-place {
  overflow: hidden;
  margin-top: 4px;
  color: #98a2b3;
  font-size: 10px;
  font-weight: 500;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.schedule-meta {
  overflow: hidden;
  margin-top: 5px;
  color: #7e8b9e;
  font-family:'Space Mono',ui-monospace,monospace;
  font-size: 10.5px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.schedule-meta .dot {
  margin: 0 5px;
}
.schedule-chevron {
  align-self: center;
  color: #c2cadb;
  font-size: 20px;
  font-weight: 700;
}
.status-onsite { background: #fff; }
.status-onsite .schedule-title-row em { background: #fff0ee; color: #db6258; }
.status-undecided { background: #fff; }
.status-undecided .schedule-title-row em { background: #eceff3; color: #657184; }
.is-completed {
  border-color: #d6dbe3;
  background: #eef1f5;
  box-shadow: none;
}
.is-completed .schedule-flag-wrap{
  background:#dfe3e9;
  filter:grayscale(1);
  opacity:.7;
}
.is-completed .schedule-copy h4{color:#667085}
.is-completed .schedule-meta{color:#8a94a3}
.is-completed .schedule-place{color:#98a1ae}
.is-today{border-color:#ffd978;background:#fffaf0;box-shadow:0 7px 20px rgba(198,139,0,.09)}
.is-today.is-completed{border-color:#d6dbe3;background:#eef1f5;box-shadow:none}
.compact {
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 9px 22px rgba(6, 30, 76, 0.18);
  color: #10192d;
}
.compact .schedule-flag-wrap{background:#e8f1ff}
.compact .schedule-copy h4 { color: #10192d; }
.compact .schedule-place { color: #3d4a63; }
.compact .schedule-meta { color: #8a97ab; }
.compact .schedule-title-row em { background: #edf4ff; color: #2868cf; }
.compact.status-onsite .schedule-title-row em { background: #fff0ee; color: #db6258; }
.compact.status-undecided .schedule-title-row em { background: #eceff3; color: #657184; }
.compact .schedule-chevron { color: #91a0b7; }
</style>
