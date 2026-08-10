<script setup>
defineProps({ groups: { type: Array, required: true }, showIcons: { type: Boolean, default: true }, loading: { type: Boolean, default: false } })
defineEmits(['select'])
const money = (value) => `${value > 0 ? '+' : '-'}${Math.abs(value).toLocaleString('ko-KR')}원`
const categoryIcons = { 급여: '₩', 카페: '☕', 식비: '🍴', 생활비: '🛒', 교통비: '🚌', 자동이체: '↻' }
</script>

<template>
  <div class="transaction-groups">
    <section v-for="group in groups" :key="group.date" :class="{ 'without-icons': !showIcons }">
      <h3>{{ group.label }}</h3>
      <button v-for="item in group.items" :key="item.id" @click="$emit('select', item)">
        <span v-if="showIcons" class="category-icon">{{ categoryIcons[item.category] || '▦' }}</span>
        <span v-else class="dot" :class="item.amount > 0 ? 'deposit-dot' : 'withdrawal-dot'"></span>
        <span><b>{{ item.merchant }}</b><small>{{ item.category }} | {{ item.method }}</small></span>
        <span class="amount"><strong :class="item.amount > 0 ? 'deposit' : 'withdrawal'">{{ money(item.amount) }}</strong><time>{{ item.time }}</time></span>
      </button>
    </section>
    <p v-if="loading" class="empty">거래내역을 불러오는 중...</p>
    <p v-else-if="!groups.length" class="empty">조회된 거래내역이 없어요.</p>
  </div>
</template>

<style scoped>
.transaction-groups section{margin-bottom:18px}.transaction-groups h3{margin:0 4px 9px;color:#94a3b8;font-size:11px;font-weight:800}.transaction-groups button{display:grid;grid-template-columns:36px 1fr auto;align-items:center;gap:8px;width:100%;margin-bottom:8px;padding:11px 12px;border:1px solid #e2e7ef;border-radius:14px;background:white;box-shadow:0 3px 9px #1e34620b;text-align:left}.category-icon{display:grid;width:32px;height:32px;place-items:center;border-radius:50%;background:#eef4ff;font-size:13px}.transaction-groups span b,.transaction-groups span small{display:block}.transaction-groups span b{font-size:12px}.transaction-groups span small{max-width:150px;margin-top:4px;overflow:hidden;color:#94a3b8;font-size:8px;text-overflow:ellipsis;white-space:nowrap}.amount{text-align:right}.transaction-groups strong,.transaction-groups time{display:block}.transaction-groups strong{font-size:12px;white-space:nowrap}.transaction-groups time{margin-top:4px;color:#94a3b8;font-size:8px}.deposit{color:#0758d6}.withdrawal{color:#e32927}.empty{padding:50px 0;text-align:center;color:#94a3b8;font-size:12px}
.transaction-groups section.without-icons button{grid-template-columns:10px 1fr auto;padding:14px 16px}.dot{width:8px;height:8px;border-radius:50%;flex-shrink:0;margin-top:2px}.deposit-dot{background:#0758d6}.withdrawal-dot{background:#e32927}
</style>
