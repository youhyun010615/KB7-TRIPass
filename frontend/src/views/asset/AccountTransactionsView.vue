<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AssetTicket from '@/components/asset/AssetTicket.vue'
import TransactionGroups from '@/components/asset/TransactionGroups.vue'
import { useAssetStore } from '@/stores/asset'

const route = useRoute()
const router = useRouter()
const asset = useAssetStore()
const account = computed(() => asset.getAccount(route.params.accountId) || asset.accounts[0])
const groups = computed(() => asset.transactionsByAccount(account.value.id).reduce((result, item) => {
  const group = result.find((entry) => entry.date === item.date)
  if (group) group.items.push(item)
  else result.push({ date: item.date, label: item.dateLabel, items: [item] })
  return result
}, []))
</script>

<template>
  <main class="account-page">
    <header><button @click="router.back()">‹</button><h1>계좌내역 · {{ account.bank }}</h1></header>
    <AssetTicket :label="account.name" :amount="account.balance" :caption="`${account.number} · ${account.type}`" />
    <div class="tabs"><b>전체</b><span>입금</span><span>출금</span></div>
    <TransactionGroups :groups="groups" @select="router.push(`/asset/transactions/${$event.id}`)" />
  </main>
</template>

<style scoped>
.account-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 30px;background:#f4f6fc;color:#10192d}header{display:grid;grid-template-columns:30px 1fr;align-items:center;margin-bottom:17px}header button{font-size:26px;text-align:left}h1{font-size:17px;font-weight:900}.tabs{display:grid;grid-template-columns:repeat(3,1fr);margin:16px 0;text-align:center;color:#b0bac9;font-size:11px}.tabs b{padding-bottom:10px;border-bottom:2px solid #3475f4;color:#3475f4}
</style>
