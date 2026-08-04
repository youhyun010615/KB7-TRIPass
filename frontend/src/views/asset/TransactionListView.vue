<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore } from '@/stores/asset'

const router = useRouter()
const asset = useAssetStore()
const tabs = [{ id: 'all', label: '전체' }, { id: 'deposit', label: '입금' }, { id: 'withdrawal', label: '출금' }]
const counts = computed(() => ({ all: asset.transactions.length, deposit: asset.depositCount, withdrawal: asset.withdrawalCount }))
</script>

<template>
  <main class="list-page">
    <header><button @click="router.back()">‹</button><h1>전체 계좌 거래내역</h1><button aria-label="캘린더" @click="router.push('/asset/transactions/calendar')">▣</button></header>
    <div class="period">2024.06.28 ~ 2024.07.27　|　1개월</div>
    <nav><button v-for="tab in tabs" :key="tab.id" :class="{ active: asset.transactionFilter === tab.id }" @click="asset.transactionFilter = tab.id">{{ tab.label }}</button></nav>
    <section class="counts"><span v-for="tab in tabs" :key="tab.id"><small>{{ tab.label === 'all' ? '전체 거래' : tab.label }}</small><b>{{ counts[tab.id] }}건</b></span></section>
    <TransactionGroups :groups="asset.groupedTransactions" @select="router.push(`/asset/transactions/${$event.id}`)" />
  </main>
</template>

<style scoped>
.list-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 30px;background:#f4f6fc;color:#10192d}header{display:grid;grid-template-columns:30px 1fr 30px;align-items:center;margin-bottom:16px}header button{font-size:24px}header button:first-child{text-align:left}header button:last-child{text-align:right;color:#64748b}h1{text-align:center;font-size:18px;font-weight:900}.period{display:inline-block;padding:10px 14px;border-radius:12px;background:#e9edf5;color:#64748b;font-size:10px;font-weight:800}nav{display:grid;grid-template-columns:repeat(3,1fr);margin:12px -20px 0;background:white}nav button{height:46px;border-bottom:2px solid #e7eaf1;color:#b0bac9;font-size:12px;font-weight:800}nav .active{border-color:#3475f4;color:#3475f4}.counts{display:grid;grid-template-columns:repeat(3,1fr);margin:0 -20px 18px;padding:12px 20px;background:white}.counts span{text-align:center}.counts small,.counts b{display:block}.counts small{color:#94a3b8;font-size:9px}.counts b{margin-top:5px;font-size:17px}
</style>
