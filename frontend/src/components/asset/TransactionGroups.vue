<script setup>
defineProps({ groups: { type: Array, required: true } })
defineEmits(['select'])
const money = (value) => `${value > 0 ? '+' : '-'}${Math.abs(value).toLocaleString('ko-KR')}원`
</script>

<template>
  <div class="transaction-groups">
    <section v-for="group in groups" :key="group.date">
      <h3>{{ group.label }}</h3>
      <button v-for="item in group.items" :key="item.id" @click="$emit('select', item)">
        <time>{{ item.time }}</time>
        <span><b>{{ item.merchant }}</b><small>{{ item.category }} | {{ item.method }}</small></span>
        <strong :class="item.amount > 0 ? 'deposit' : 'withdrawal'">{{ money(item.amount) }}</strong>
      </button>
    </section>
    <p v-if="!groups.length" class="empty">조회된 거래내역이 없어요.</p>
  </div>
</template>

<style scoped>
.transaction-groups section{margin-bottom:18px}.transaction-groups h3{margin:0 4px 9px;color:#94a3b8;font-size:11px;font-weight:800}.transaction-groups button{display:grid;grid-template-columns:42px 1fr auto;align-items:center;width:100%;margin-bottom:8px;padding:11px 12px;border:1px solid #e2e7ef;border-radius:14px;background:white;box-shadow:0 3px 9px #1e34620b;text-align:left}.transaction-groups time{color:#94a3b8;font-size:9px}.transaction-groups span b,.transaction-groups span small{display:block}.transaction-groups span b{font-size:12px}.transaction-groups span small{max-width:160px;margin-top:4px;overflow:hidden;color:#94a3b8;font-size:8px;text-overflow:ellipsis;white-space:nowrap}.transaction-groups strong{font-size:12px;white-space:nowrap}.deposit{color:#0758d6}.withdrawal{color:#e32927}.empty{padding:50px 0;text-align:center;color:#94a3b8;font-size:12px}
</style>
