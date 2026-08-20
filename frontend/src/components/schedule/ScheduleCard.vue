<script setup>
import { computed } from 'vue'
import { useTravelScheduleStore } from '@/stores/travelSchedule'
import { flagIconClass } from '@/stores/travel'

const props = defineProps({ schedule: { type: Object, required: true }, compact: { type: Boolean, default: false } })
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
    :class="[`status-${item.paymentStatus}`, { compact }]"
    role="button"
    tabindex="0"
    @click="emit('detail', item.id)"
    @keydown.enter="emit('detail', item.id)"
  >
    <div class="schedule-time">
      <time>{{ item.time }}</time>
      <span v-if="country" class="schedule-country">
        <span :class="flagIconClass(country.code)" class="fi-inline schedule-flag" />{{ country.name }}
      </span>
    </div>
    <div class="schedule-copy">
      <div class="schedule-title-row">
        <h4>{{ item.title }}</h4>
        <em>{{ paymentLabel }}</em>
      </div>
      <p class="schedule-place">{{ item.placeName || '장소 미정' }}</p>
      <p class="schedule-meta">
        {{ item.placeAddress || '주소 미정' }}<template v-if="hasAmount">
          <span class="dot">·</span>{{ item.currency }} {{ Number(item.amount).toLocaleString() }}</template>
      </p>
    </div>
    <span class="schedule-chevron" aria-hidden="true">›</span>
  </article>
</template>

<style scoped>
.schedule-card {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr) 18px;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px;
  border-radius: 16px;
  background: #f8f9fc;
  box-shadow: 0 2px 7px rgba(16, 25, 43, .04);
  color: #10192d;
  text-align: left;
}
.schedule-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  align-self: start;
}
.schedule-time time {
  padding: 9px 4px;
  width: 100%;
  border-radius: 11px;
  background: #eef2ff;
  color: #173f8d;
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.schedule-country {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  color: #7e8b9e;
  font-size: 10px;
  font-weight: 700;
  line-height: 1.2;
  text-align: center;
  white-space: nowrap;
}
.schedule-flag {
  font-size: 14px;
}
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
  font-size: 16px;
  font-weight: 700;
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
  margin-top: 6px;
  color: #3d4a63;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.schedule-meta {
  overflow: hidden;
  margin-top: 3px;
  color: #8a97ab;
  font-size: 12px;
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
.status-onsite { background: #fff6f4; }
.status-onsite .schedule-title-row em { background: #fff0ee; color: #db6258; }
.status-undecided { background: #fafafa; }
.status-undecided .schedule-title-row em { background: #eceff3; color: #657184; }
.compact {
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 9px 22px rgba(6, 30, 76, 0.18);
  color: #10192d;
}
.compact .schedule-time time { background: #e8f1ff; color: #174b9f; }
.compact .schedule-country { color: #718096; }
.compact .schedule-copy h4 { color: #10192d; }
.compact .schedule-place { color: #3d4a63; }
.compact .schedule-meta { color: #8a97ab; }
.compact .schedule-title-row em { background: #edf4ff; color: #2868cf; }
.compact.status-onsite .schedule-title-row em { background: #fff0ee; color: #db6258; }
.compact.status-undecided .schedule-title-row em { background: #eceff3; color: #657184; }
.compact .schedule-chevron { color: #91a0b7; }
</style>
