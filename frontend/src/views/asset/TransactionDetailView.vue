<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useAssetStore } from '@/stores/asset'

const route = useRoute()
const router = useRouter()
const asset = useAssetStore()
const item = computed(() => asset.getTransaction(route.params.transactionId))
const editMode = ref(null)
const draftCategory = ref('')
const draftMemo = ref('')
const categories = ['식비', '카페', '교통비', '쇼핑', '숙박비', '관광', '생활비', '기타']
const categoryVisuals = {
  급여: { icon: '₩', tone: '#e4fbf5', color: '#12ad92' }, 카페: { icon: '☕', tone: '#fff4e5', color: '#c46b19' },
  식비: { icon: '🍴', tone: '#f2edff', color: '#7547d8' }, 생활비: { icon: '🛒', tone: '#eaf8f1', color: '#1c9a67' },
  교통비: { icon: '🚌', tone: '#eaf2ff', color: '#3475f4' }, 자동이체: { icon: '↻', tone: '#edf1f7', color: '#53657d' },
  쇼핑: { icon: '🛍️', tone: '#fff1f5', color: '#d84a76' }, 숙박비: { icon: '🏨', tone: '#f1efff', color: '#6753c8' },
  관광: { icon: '🎟️', tone: '#e9f8ff', color: '#2685ad' }, 기타: { icon: '▦', tone: '#edf4ff', color: '#3475f4' },
}
const visual = computed(() => categoryVisuals[item.value?.category] || categoryVisuals.기타)
const isDeposit = computed(() => Number(item.value?.amount || 0) > 0)
const displayAmount = computed(() => {
  if (!item.value) return ''
  if (item.value.currency && item.value.localAmount != null) {
    const sign = item.value.localAmount > 0 ? '+' : '-'
    return `${sign}${item.value.currency} ${Math.abs(item.value.localAmount).toLocaleString('ko-KR', { maximumFractionDigits: 2 })}`
  }
  const sign = item.value.amount > 0 ? '+' : '-'
  return `${sign}${Math.abs(item.value.amount).toLocaleString('ko-KR')}원`
})
const rows = computed(() => item.value ? [
  { label: '거래일시', value: `${item.value.dateLabel} ${item.value.time}` },
  { label: '여행지', value: item.value.country ? `${item.value.city} · ${item.value.country}` : '국내' },
  { label: '카테고리', value: item.value.category, accent: true, editable: 'category' },
  { label: '거래구분', value: isDeposit.value ? '입금' : '지출' },
  { label: isDeposit.value ? '입금 계좌' : '결제수단', value: item.value.method },
  { label: isDeposit.value ? '입금처' : '사용처', value: item.value.user || item.value.merchant },
  { label: '거래 후 여행 잔액', value: `${item.value.balanceAfter.toLocaleString('ko-KR')}원` },
] : [])

function openEditor(mode) {
  editMode.value = mode
  draftCategory.value = item.value?.category || ''
  draftMemo.value = item.value?.memo || ''
}
function saveEdit() {
  if (editMode.value === 'category') asset.updateTransaction(item.value.id, { category: draftCategory.value })
  if (editMode.value === 'memo') asset.updateTransaction(item.value.id, { memo: draftMemo.value.trim() })
  editMode.value = null
}
</script>

<template>
  <main class="detail-page">
    <header><button type="button" @click="router.back()">‹</button><h1>거래내역 상세보기</h1><span /></header>
    <template v-if="item">
      <section class="hero"><div><small v-if="item.country">{{ item.flag }} {{ item.country }} 여행</small><b>{{ item.merchant }}</b><strong :class="isDeposit ? 'deposit' : 'withdrawal'">{{ displayAmount }}</strong><em v-if="item.currency">약 {{ Math.abs(item.amount).toLocaleString('ko-KR') }}원</em></div><span :style="{ background: visual.tone, color: visual.color }">{{ visual.icon }}</span></section>
      <section class="info-card"><div v-for="row in rows" :key="row.label"><small>{{ row.label }}</small><p><b :class="{ accent: row.accent }">{{ row.value }}</b><button v-if="row.editable" type="button" @click="openEditor(row.editable)">수정</button></p></div></section>
      <div class="section-heading"><h2>메모</h2><button type="button" @click="openEditor('memo')">수정</button></div><section class="memo">{{ item.memo || '등록된 메모가 없어요.' }}</section>
      <p v-if="item.country" class="trip-note">이 거래는 등록한 {{ item.country }} 여행 기간에 포함된 내역이에요.</p>
    </template>
    <p v-else class="empty">거래내역을 찾을 수 없어요.</p>
    <BottomNav />

    <Teleport to="body"><div v-if="editMode" class="editor-wrap"><button class="backdrop" type="button" aria-label="수정 취소" @click="editMode = null"/><section class="editor"><header><h2>{{ editMode === 'category' ? '카테고리 수정' : '메모 수정' }}</h2><button type="button" @click="editMode = null">×</button></header><div v-if="editMode === 'category'" class="category-grid"><button v-for="category in categories" :key="category" type="button" :class="{ selected: draftCategory === category }" @click="draftCategory = category"><span>{{ categoryVisuals[category]?.icon }}</span>{{ category }}</button></div><textarea v-else v-model="draftMemo" maxlength="100" placeholder="메모를 입력해 주세요."/><button class="save" type="button" :disabled="editMode === 'category' && !draftCategory" @click="saveEdit">수정 완료</button></section></div></Teleport>
  </main>
</template>

<style scoped>
.detail-page{width:min(100%,390px);min-height:100vh;margin:0 auto;padding:48px 20px 105px;background:#f8f6f1;color:#10192d}.detail-page>header{display:grid;grid-template-columns:30px 1fr 30px;align-items:center;margin-bottom:18px}.detail-page>header button{font-size:26px;text-align:left}.detail-page>header h1{text-align:center;font-size:18px;font-weight:900}.hero{display:flex;align-items:center;justify-content:space-between;padding:18px;border:1px solid #d8e2f0;border-radius:16px;background:#fff}.hero small,.hero b,.hero strong,.hero em{display:block}.hero small{margin-bottom:10px;color:#64748b;font-size:9px}.hero b{font-size:14px}.hero strong{margin-top:8px;font-size:24px}.hero em{margin-top:5px;color:#94a3b8;font-size:9px;font-style:normal}.hero>span{display:grid;width:50px;height:50px;place-items:center;border-radius:50%;font-size:21px;font-weight:900}.deposit{color:#0758d6}.withdrawal{color:#e8484f}.info-card{margin-top:12px;padding:10px 16px;border:1px solid #dbe3ef;border-radius:17px;background:#fff}.info-card>div{display:grid;grid-template-columns:100px 1fr;padding:13px 0;border-bottom:1px solid #edf0f5}.info-card>div:last-child{border:0}.info-card small{color:#94a3b8;font-size:10px}.info-card p{display:flex;align-items:center;justify-content:flex-end;gap:8px;text-align:right}.info-card b{font-size:11px;line-height:1.4}.info-card .accent{color:#3475f4}.info-card button,.section-heading button{padding:4px 7px;border-radius:7px;background:#edf4ff;color:#286dd8;font-size:8px;font-weight:900}.section-heading{display:flex;align-items:center;justify-content:space-between;margin:20px 3px 9px}.section-heading h2{font-size:12px;font-weight:900}.memo{min-height:54px;padding:15px;border:1px solid #dbe3ef;border-radius:12px;background:#fff;font-size:11px}.trip-note{margin-top:12px;padding:13px;border-radius:12px;background:#eaf2ff;color:#5274a8;text-align:center;font-size:9px}.empty{padding:80px 0;text-align:center;color:#94a3b8}.editor-wrap{position:fixed;inset:0;z-index:100;display:flex;align-items:flex-end;justify-content:center}.backdrop{position:absolute;inset:0;background:#10182780}.editor{position:relative;width:min(100%,430px);padding:20px 20px 26px;border-radius:22px 22px 0 0;background:#fff}.editor header{display:flex;align-items:center;justify-content:space-between}.editor h2{font-size:17px}.editor header button{font-size:26px;color:#64748b}.category-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-top:18px}.category-grid button{display:flex;flex-direction:column;align-items:center;gap:5px;padding:10px 2px;border:1px solid #e1e6ee;border-radius:10px;color:#64748b;font-size:9px}.category-grid button span{font-size:18px}.category-grid button.selected{border-color:#3475f4;background:#eef4ff;color:#246bd5;font-weight:900}.editor textarea{width:100%;height:100px;margin-top:18px;padding:12px;border:1px solid #dbe3ef;border-radius:12px;font-size:11px;resize:none;outline:none}.editor textarea:focus{border-color:#3475f4}.save{width:100%;height:48px;margin-top:16px;border-radius:12px;background:#173f8d;color:#fff;font-size:13px;font-weight:900}.save:disabled{background:#a8b1c0}
</style>
