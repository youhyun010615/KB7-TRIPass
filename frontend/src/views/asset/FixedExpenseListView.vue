<script setup>
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import AssetTicket from '@/components/asset/AssetTicket.vue'
import { useAssetStore } from '@/stores/asset'

const router = useRouter()
const asset = useAssetStore()
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
</script>

<template>
  <main class="page"><div class="shell">
    <header><button @click="router.back()">‹</button><h1>고정지출 관리</h1><span aria-hidden="true"></span></header>
    <AssetTicket label="이번 달 고정지출" :amount="asset.activeFixedTotal" :caption="`${asset.fixedExpenses.length}개 항목 · 매월 반복 예정`" />
    <section class="list">
      <button v-for="item in asset.fixedExpenses" :key="item.id" @click="router.push(`/asset/fixed-expenses/${item.id}`)">
        <span class="icon" :style="{ color:item.color, background:`${item.color}18` }">{{ item.icon }}</span>
        <span><b>{{ item.name }}</b><small>매월 {{ item.day }}일 · {{ item.active ? '사용 중' : '일시 정지' }}</small></span>
        <strong>{{ money(item.amount) }} <i>›</i></strong>
      </button>
    </section>
    <button class="add" @click="router.push('/asset/fixed-expenses/new')">＋ 고정지출 항목 추가하기</button>
  </div><BottomNav /></main>
</template>

<style scoped>
.page{min-height:100vh;padding-bottom:80px;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:auto;padding:14px 18px 28px;background:#f7f5ef}header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center;margin-bottom:18px}header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}h1{text-align:center;font-size:18px;font-weight:900}.list{margin-top:16px}.list button{display:grid;grid-template-columns:40px 1fr auto;align-items:center;gap:10px;width:100%;margin-bottom:9px;padding:12px;border:1px solid #e3e7ee;border-radius:14px;background:#fff;text-align:left;box-shadow:0 4px 12px #1b35650a}.icon{display:grid;width:36px;height:36px;place-items:center;border-radius:11px;font-weight:900}.list b,.list small{display:block}.list b{font-size:12px}.list small{margin-top:4px;color:#9aa4b4;font-size:9px}.list strong{font-size:11px;white-space:nowrap}.list i{color:#94a3b8;font-size:18px}.add{width:100%;padding:14px;border:1px dashed #9cb6ec;border-radius:13px;background:#fff;color:#2457b8;font-size:12px;font-weight:900}
</style>
