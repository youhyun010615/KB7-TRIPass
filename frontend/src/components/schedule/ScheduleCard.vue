<script setup>
import { computed } from 'vue'
import { useTravelScheduleStore } from '@/stores/travelSchedule'

const props = defineProps({
  schedule: { type: Object, required: true },
  compact: { type: Boolean, default: false },
  completed: { type: Boolean, default: false },
})
const emit = defineEmits(['detail'])
const store = useTravelScheduleStore()
const item = computed(() => store.normalizeSchedule(props.schedule))
const paymentLabel = computed(() => ({ prepaid: '사전결제 완료', onsite: '현장결제 필요', undecided: '미정' })[item.value.paymentStatus] || '미정')
const hasAmount = computed(() => Number(item.value.amount) > 0)
const scheduleIcon = computed(() => {
  const text = `${item.value.title || ''} ${item.value.placeName || ''}`
  if (/열차|기차|역|TGV|교통/.test(text)) return '🚆'
  if (/숙소|호텔|체크인/.test(text)) return '🏨'
  if (/식사|레스토랑|카페|디너/.test(text)) return '🍽️'
  if (/박물관|미술관|투어|전시/.test(text)) return '🏛️'
  if (/공항|비행|항공/.test(text)) return '✈️'
  return '📍'
})
</script>

<template>
  <article
    class="schedule-card"
    :class="[`status-${item.paymentStatus}`, { compact, 'is-completed': completed }]"
    role="button"
    tabindex="0"
    @click="emit('detail', item.id)"
    @keydown.enter="emit('detail', item.id)"
  >
    <div class="schedule-icon" aria-hidden="true">{{ scheduleIcon }}</div>
    <div class="schedule-copy">
      <div class="schedule-title-row">
        <h4>{{ item.title }}</h4>
        <em :class="{ 'completion-badge': completed }">
          {{ completed ? '✓ 일정 완료' : paymentLabel }}
        </em>
      </div>
      <p class="schedule-meta">
        {{ item.time }}<template v-if="hasAmount"><span class="dot">·</span>{{ item.currency }} {{ Number(item.amount).toLocaleString() }}</template>
      </p>
      <p class="schedule-place">📍 {{ item.placeName || '장소 미정' }}</p>
    </div>
    <span class="schedule-chevron" aria-hidden="true">›</span>
  </article>
</template>

<style scoped>
.schedule-card {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 14px;
  align-items: center;
  gap: 14px;
  width: 100%;
  min-height: 104px;
  padding: 18px 16px;
  border: 1px solid #edf0f5;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 7px 20px rgba(16, 25, 43, .07);
  color: #10192d;
  text-align: left;
  touch-action: pan-y;
}
.schedule-icon{display:grid;width:46px;height:46px;place-items:center;align-self:center;border-radius:50%;background:#eaf1ff;font-size:20px}
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
  font-size: 15px;
  font-weight: 850;
  line-height: 1.35;
}
.schedule-title-row em {
  flex: none;
  margin-top: 1px;
  padding: 4px 9px;
  border-radius: 8px;
  background: #eef2ff;
  color: #173f8d;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}
.schedule-place {
  overflow: hidden;
  margin-top: 5px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 500;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.schedule-meta {
  overflow: hidden;
  margin-top: 8px;
  color: #7e8b9e;
  font-family:'Space Mono',ui-monospace,monospace;
  font-size: 11.5px;
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
  border-color: #dfe5ee;
  background: #fff;
}
.is-completed .schedule-icon{background:#eef1f5;filter:grayscale(.35)}
.is-completed .schedule-title-row .completion-badge,
.compact.is-completed .schedule-title-row .completion-badge {
  background: #e8f7ef;
  color: #16815d;
}
.compact {
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 9px 22px rgba(6, 30, 76, 0.18);
  color: #10192d;
}
.compact .schedule-icon{background:#e8f1ff}
.compact .schedule-copy h4 { color: #10192d; }
.compact .schedule-place { color: #3d4a63; }
.compact .schedule-meta { color: #8a97ab; }
.compact .schedule-title-row em { background: #edf4ff; color: #2868cf; }
.compact.status-onsite .schedule-title-row em { background: #fff0ee; color: #db6258; }
.compact.status-undecided .schedule-title-row em { background: #eceff3; color: #657184; }
.compact .schedule-chevron { color: #91a0b7; }
</style>
