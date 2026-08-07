<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

defineProps({
  eyebrow: { type: String, default: 'TRIPASS EXCHANGE' },
  title: String,
  subtitle: String,
  selectedCode: String,
});
</script>

<template>
  <section class="ticket">
    <div class="header">
      <small>{{ eyebrow }}</small>
      <button @click="router.push(`/exchange/currencies/${selectedCode}`)">
        환율 더보기 ›
      </button>
    </div>
    <h2>{{ title }}</h2>
    <p>{{ subtitle }}</p>
    <span>|||||||</span>
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
}
.ticket h2 {
  margin-top: 10px;
  font-size: 16px;
}
.ticket p {
  margin-top: 4px;
  color: #d2e1ff;
  font-size: 8px;
}
.ticket span {
  position: absolute;
  right: 13px;
  bottom: 8px;
  color: #aac7fa;
  font-size: 7px;
  letter-spacing: 1px;
}
</style>
