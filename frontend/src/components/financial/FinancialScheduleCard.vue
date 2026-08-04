<script setup>
defineProps({ event: { type:Object, required:true }, compact: Boolean })
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
</script>

<template>
  <article class="schedule-card" :class="[event.type, { compact }]">
    <span class="icon">{{ event.icon }}</span>
    <time><b>{{ event.day }}</b><small>일</small></time>
    <div class="copy"><b>{{ event.title }}</b><small>{{ money(event.amount) }} {{ event.type === 'income' ? '입금' : '출금' }} 예정</small><em>🔔 하루 전 알림</em></div>
    <span class="type">{{ event.type === 'income' ? '입금' : '지출' }}</span>
  </article>
</template>

<style scoped>
.schedule-card{display:grid;grid-template-columns:38px 44px 1fr auto;align-items:center;gap:8px;padding:12px;border:1px solid;border-radius:14px}.schedule-card+.schedule-card{margin-top:9px}.schedule-card.income{border-color:#b7d8ff;background:#eaf4ff}.schedule-card.expense{border-color:#f3c4be;background:#fff3f1}.icon{display:grid;width:36px;height:36px;place-items:center;border-radius:50%;font-size:14px}.income .icon{background:#d5eaff;color:#2f80ed}.expense .icon{background:#ffe0dc;color:#d96c5f}time{display:grid;width:40px;height:40px;place-content:center;border-radius:10px;text-align:center}.income time{background:#d5eaff;color:#246fcf}.expense time{background:#ffe0dc;color:#bd5a50}time b{font-size:13px}time small{font-size:7px}.copy b,.copy small,.copy em{display:block}.copy b{font-size:11px}.copy small{margin-top:4px;color:#657185;font-size:8px}.copy em{margin-top:4px;color:#8c96a6;font-size:7px;font-style:normal}.type{padding:4px 7px;border-radius:10px;font-size:7px;font-weight:900}.income .type{background:#fff;color:#2f80ed}.expense .type{background:#fff;color:#d96c5f}.compact{grid-template-columns:35px 1fr auto}.compact time{display:none}.compact .copy em{display:none}
</style>
