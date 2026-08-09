<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();
const emit = defineEmits(['add-alert']);
const props = defineProps({
  eyebrow: { type: String, default: 'TRIPASS EXCHANGE' },
  name: String,
  rate: { type: [String, Number] },
  unit: { type: Number, default: 1 },
  subtitle: String,
  selectedCode: String,
  flagClass: { type: String, default: 'fi fi-un' },
  alertButtonLabel: { type: String, default: '+ 환율 알림 추가' },
});
</script>

<template>
  <section class="ticket">
    <div class="header">
      <span>{{ eyebrow }}</span>
    </div>
    <h2>
      <span :class="flagClass" style="margin-right: 8px"></span>
      {{ rate }}원
      <small
        v-if="unit > 1"
        style="margin-left: 4px; font-size: 10px; opacity: 0.8"
        >({{ unit }}{{ selectedCode }}당)</small
      >
    </h2>
    <p>{{ subtitle }}</p>
    <span class="decor">|||||||</span>
  </section>
</template>

<style scoped>
.ticket {
  position: relative;
  padding: 15px;
  border-radius: 15px;
  background: linear-gradient(135deg, #17387f, #2468c8);
  color: #fff;
  box-shadow: 0 8px 18px #17387f29;
  min-height: 120px; /* 장식 공간 확보 */
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ticket:before,
.ticket:after {
  position: absolute;
  top: 50%;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #f7f5ef;
  content: '';
}
.ticket:before {
  left: -6px;
}
.ticket:after {
  right: -6px;
}
.ticket small {
  color: #b6cefb;
  font-size: 7px;
}
.ticket button {
  background: none;
  border: none;
  color: #fff;
  font-size: 7px;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
}
.ticket h2 {
  margin-top: 10px;
  font-size: 16px;
  display: flex;
  align-items: center;
}
.ticket p {
  margin-top: 4px;
  color: #d2e1ff;
  font-size: 8px;
}
/* 장식 요소 위치 고정 */
.ticket .decor {
  position: absolute;
  right: 15px;
  bottom: 12px;
  color: #aac7fa;
  font-size: 7px;
  letter-spacing: 1px;
}
/* flag-icons가 제대로 보이도록 강제 스타일 추가 */
.ticket h2 span.fi {
  display: inline-block;
  width: 1.33em;
  height: 1em;
  background-size: cover;
  border-radius: 2px;
  vertical-align: middle;
}
</style>
