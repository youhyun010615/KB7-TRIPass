<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import AssetTicket from '@/components/asset/AssetTicket.vue'
import { PREPAID_SCOPE_META, useAssetStore } from '@/stores/asset'

const router = useRouter()
const asset = useAssetStore()
const filterOpen = ref(false)

const filterOptions = computed(() => ['ALL', 'COMMON', ...asset.prepaidCountryScopes])
const activeMeta = computed(() => PREPAID_SCOPE_META[asset.selectedPrepaidScope] || PREPAID_SCOPE_META.ALL)
const visibleGroups = computed(() => {
  if (asset.selectedPrepaidScope === 'ALL') return asset.prepaidGroups.filter((group) => group.items.length)
  return asset.prepaidGroups.filter((group) => group.scope === asset.selectedPrepaidScope)
})
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`

function chooseFilter(scope) {
  asset.selectedPrepaidScope = scope
  filterOpen.value = false
}
</script>

<template>
  <main class="page">
    <div class="shell">
      <header><button aria-label="뒤로가기" @click="router.back()">‹</button><h1>사전 지출 금액 목록</h1><span aria-hidden="true"></span></header>

      <AssetTicket label="총 사전 지출 금액" :amount="asset.prepaidTotal" :caption="`${asset.prepaidExpenses.length}개 항목 · 여행 전 결제 완료`" />

      <div class="filter-wrap">
        <button class="filter" :aria-expanded="filterOpen" @click="filterOpen = !filterOpen">
          <span>{{ activeMeta.flag }}</span><b>{{ activeMeta.name }}</b><i>⌄</i>
        </button>
        <div v-if="filterOpen" class="filter-menu">
          <button v-for="scope in filterOptions" :key="scope" :class="{ selected: asset.selectedPrepaidScope === scope }" @click="chooseFilter(scope)">
            <span>{{ PREPAID_SCOPE_META[scope].flag }}</span>{{ PREPAID_SCOPE_META[scope].name }}<i>✓</i>
          </button>
        </div>
      </div>

      <section v-for="group in visibleGroups" :key="group.scope" class="country-card">
        <div class="country-title">
          <div><h2>{{ group.flag }} {{ group.name }}</h2><p>{{ group.description }}</p></div>
          <strong>{{ money(group.directTotal) }}</strong>
        </div>

        <div class="items">
          <button v-for="item in group.items" :key="item.id" @click="router.push(`/asset/prepaid/${item.id}`)">
            <span class="item-icon">{{ item.icon || '▦' }}</span>
            <div><b>{{ item.name }}</b><small>{{ item.date }}</small></div>
            <strong>{{ money(item.amount) }}</strong><i>›</i>
          </button>
        </div>

        <div v-if="group.scope === 'COMMON' && asset.prepaidCountryScopes.length" class="allocation-note">
          각 나라에 {{ money(asset.commonAllocation) }}씩 최종 사전 지출 금액으로 반영돼요.
        </div>

        <dl v-if="group.scope !== 'COMMON'" class="summary">
          <div><dt>{{ group.name }} 직접 지출</dt><dd>{{ money(group.directTotal) }}</dd></div>
          <div><dt>공통 사전 지출 배분</dt><dd class="orange">{{ money(group.allocatedCommon) }}</dd></div>
          <div><dt>최종 사전 지출 금액</dt><dd>{{ money(group.finalTotal) }}</dd></div>
        </dl>
      </section>

      <section v-if="!visibleGroups.length" class="empty"><span>✈</span><h2>등록된 사전 지출이 없어요</h2><p>여행 전에 결제한 항목을 등록해 주세요.</p></section>

      <BottomNav />
    </div>
  </main>
</template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{position:relative;width:min(100%,390px);min-height:100vh;margin:auto;padding:14px 18px 104px;background:#f7f5ef}header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center;margin-bottom:18px}header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;font-size:24px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}h1{text-align:center;font-size:18px;font-weight:900}.filter-wrap{position:relative;z-index:4;margin:14px 2px 4px}.filter{display:flex;align-items:center;gap:6px;min-width:92px;padding:8px 11px;border:1px solid #dce3ed;border-radius:18px;background:#fff;font-size:10px}.filter i{margin-left:auto;color:#7c8797}.filter-menu{position:absolute;top:40px;left:0;width:154px;padding:7px;border:1px solid #dce3ed;border-radius:13px;background:#fff;box-shadow:0 12px 30px #132a5540}.filter-menu button{display:grid;grid-template-columns:25px 1fr 15px;align-items:center;width:100%;padding:10px;border-radius:9px;text-align:left;font-size:11px}.filter-menu button.selected{background:#eef4ff;color:#174494;font-weight:900}.filter-menu i{visibility:hidden}.filter-menu .selected i{visibility:visible}.country-card{margin-top:12px;padding:14px;border:1px solid #e0e6ef;border-radius:16px;background:#fff;box-shadow:0 5px 14px #1a37670a}.country-title{display:flex;align-items:start;justify-content:space-between;padding-bottom:11px}.country-title h2{font-size:13px}.country-title p{margin-top:4px;color:#9aa5b5;font-size:8px}.country-title>strong{padding:6px 10px;border-radius:14px;background:#edf4ff;color:#1670e8;font-size:10px}.items{border:1px solid #edf0f4;border-radius:12px;padding:0 10px}.items>button{display:grid;grid-template-columns:34px 1fr auto 8px;align-items:center;gap:8px;width:100%;padding:11px 0;border-bottom:1px solid #edf0f4;text-align:left}.items>button:last-child{border:0}.item-icon{display:grid;width:30px;height:30px;place-items:center;border-radius:50%;background:#eef4ff;font-size:14px}.items b,.items small{display:block}.items b{font-size:10px}.items small{margin-top:4px;color:#9aa5b5;font-size:7px}.items strong{font-size:9px}.items i{color:#a8b1bf}.allocation-note{margin-top:10px;padding:9px;border-radius:9px;background:#fff7df;color:#b06e00;font-size:8px;text-align:center}.summary{margin-top:10px;padding-top:9px;border-top:1px dashed #d7dfeb}.summary div{display:flex;justify-content:space-between;padding:3px 2px;font-size:9px}.summary dt{color:#7c8797}.summary dd{color:#1670e8;font-weight:900}.summary .orange{color:#f08a24}.summary div:last-child{margin:4px -6px -5px;padding:8px;border-radius:7px;background:#173f8d;color:#fff}.summary div:last-child dt,.summary div:last-child dd{color:#fff;font-weight:900}.empty{margin-top:12px;padding:40px 20px;border:1px solid #e0e6ef;border-radius:16px;background:#fff;text-align:center}.empty span{display:grid;width:50px;height:50px;margin:auto;place-items:center;border-radius:50%;background:#eef4ff;color:#2475e8}.empty h2{margin-top:12px;font-size:14px}.empty p{margin-top:6px;color:#98a3b3;font-size:9px}
</style>
