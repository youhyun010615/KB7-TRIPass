<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import TransactionEditModal from '@/components/asset/TransactionEditModal.vue'
import { useAssetStore } from '@/stores/asset'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const asset = useAssetStore()
const isRealTransaction = history.state?.item?.isReal === true
const mockItem = computed(() => isRealTransaction ? null : asset.getTransaction(route.params.transactionId))
const realItem = ref(null)
const item = computed(() => mockItem.value ?? realItem.value ?? null)
const editMode = ref(null)
const DAYS = ['일', '월', '화', '수', '목', '금', '토']

onMounted(async () => {
  if (mockItem.value) return
  try {
    const res = await api.get(`/transactions/${route.params.transactionId}`)
    const t = res.data.data
    const [y, mo, d] = Array.isArray(t.transactionDate) ? t.transactionDate : t.transactionDate.split('-').map(Number)
    const date = `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const jsDate = new Date(y, mo - 1, d)
    const [h = 0, m = 0] = Array.isArray(t.transactionTime) ? t.transactionTime : (t.transactionTime ?? '00:00').split(':').map(Number)
    realItem.value = {
      id: t.id,
      merchant: t.merchantName ?? '(내용없음)',
      amount: t.transactionType === 'DEPOSIT' ? Number(t.amount) : -Number(t.amount),
      dateLabel: `${date.replaceAll('-', '.')} (${DAYS[jsDate.getDay()]})`,
      time: `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`,
      balanceAfter: Number(t.balanceAfter ?? 0),
      memo: t.memo ?? '',
      categoryId: t.categoryId ?? null,
      category: t.categoryName ?? '기타',
      method: history.state?.item?.method ?? t.paymentMethodName ?? '',
      merchantType: t.merchantType ?? '',
      sourceType: t.cardId ? 'CARD' : 'ACCOUNT',
    }
  } catch (e) {
    console.error('거래내역 조회 실패', e)
  }
})

async function saveMemo(value) {
  if (realItem.value) {
    try {
      await api.patch(`/transactions/${route.params.transactionId}`, { memo: value })
      realItem.value.memo = value
    } catch (e) {
      console.error('메모 수정 실패', e)
    }
  } else {
    asset.updateTransaction(item.value.id, { memo: value })
  }
}

// spending_categories 테이블(id 1~9)과 동일한 카테고리/ID를 써야 BE에 categoryId로 저장된다.
const categories = [
  { id: 1, name: '식비', icon: '🍴', color: '#7547d8', description: '식사, 배달, 식료품' },
  { id: 7, name: '카페', icon: '☕', color: '#c46b19', description: '커피와 디저트' },
  { id: 8, name: '생활비', icon: '🛒', color: '#1c9a67', description: '마트, 편의점, 생활용품' },
  { id: 4, name: '쇼핑', icon: '🛍️', color: '#d84a76', description: '의류, 화장품, 온라인 쇼핑' },
  { id: 2, name: '교통', icon: '🚌', color: '#2563eb', description: '대중교통, 택시, 주유' },
  { id: 3, name: '숙박', icon: '🏨', color: '#0d9488', description: '호텔, 숙소' },
  { id: 5, name: '관광', icon: '🎟️', color: '#f59e0b', description: '입장료, 투어, 액티비티' },
  { id: 9, name: '취미여가', icon: '🎨', color: '#d97706', description: '영화, 공연, 운동' },
  { id: 6, name: '기타', icon: '•••', color: '#64748b', description: '그 외 지출' },
]

// 실제 거래는 categoryId, 목업 거래는 category(이름)만 있어 이름으로 역매칭한다.
const selectedCategoryId = computed(() => {
  if (!item.value) return null
  if (item.value.categoryId != null) return item.value.categoryId
  return categories.find((c) => c.name === item.value.category)?.id ?? null
})

async function saveCategory(categoryId) {
  const category = categories.find((c) => c.id === categoryId)
  if (realItem.value) {
    try {
      await api.patch(`/transactions/${route.params.transactionId}`, { categoryId })
      realItem.value.categoryId = categoryId
      realItem.value.category = category?.name ?? realItem.value.category
    } catch (e) {
      console.error('카테고리 수정 실패', e)
    }
  } else {
    asset.updateTransaction(item.value.id, { category: category?.name ?? item.value.category })
  }
}

const isDeposit = computed(() => Number(item.value?.amount || 0) > 0)
const displayAmount = computed(() => {
  if (!item.value) return ''
  if (item.value.currency && item.value.localAmount != null) {
    return `${item.value.localAmount > 0 ? '+' : '-'}${item.value.currency} ${Math.abs(item.value.localAmount).toLocaleString('ko-KR', { maximumFractionDigits: 2 })}`
  }
  return `${item.value.amount > 0 ? '+' : '-'}${Math.abs(item.value.amount).toLocaleString('ko-KR')}원`
})
const rows = computed(() => {
  if (!item.value) return []
  const result = [
    { label: '거래일시', value: `${item.value.dateLabel} ${item.value.time}` },
    { label: '여행지', value: item.value.country ? `${item.value.city} · ${item.value.country}` : '국내' },
    { label: '카테고리', value: item.value.category, accent: true, editable: true },
    { label: '거래구분', value: isDeposit.value ? '입금' : '지출' },
    { label: isDeposit.value ? '입금 계좌' : '결제수단', value: item.value.method },
    { label: isDeposit.value ? '입금처' : '사용처', value: item.value.user || item.value.merchant },
  ]
  if (item.value.merchantType) result.push({ label: '가맹점 업종', value: item.value.merchantType })
  if (item.value.sourceType !== 'CARD') result.push({ label: '거래 후 잔액', value: `${Number(item.value.balanceAfter ?? 0).toLocaleString('ko-KR')}원` })
  return result
})
</script>

<template>
  <main class="detail-page">
    <header>
      <button type="button" aria-label="이전 화면" @click="router.back()">‹</button>
      <h1>거래내역 상세보기</h1>
      <span></span>
    </header>

    <template v-if="item">
      <section class="hero">
        <div>
          <small v-if="item.country">{{ item.flag }} {{ item.country }} 여행</small>
          <b>{{ item.merchant }}</b>
          <strong :class="isDeposit ? 'deposit' : 'withdrawal'">{{ displayAmount }}</strong>
          <em v-if="item.currency">약 {{ Math.abs(item.amount).toLocaleString('ko-KR') }}원</em>
        </div>
      </section>

      <section class="info-card">
        <div v-for="row in rows" :key="row.label">
          <small>{{ row.label }}</small>
          <p>
            <b :class="{ accent: row.accent }">{{ row.value }}</b>
            <button v-if="row.editable" type="button" @click="editMode = 'category'">수정</button>
          </p>
        </div>
      </section>

      <div class="section-heading">
        <h2>메모</h2>
        <button type="button" @click="editMode = 'memo'">수정</button>
      </div>
      <section class="memo">{{ item.memo || '등록된 메모가 없어요.' }}</section>

      <p v-if="item.country" class="trip-note">이 거래는 등록한 {{ item.country }} 여행 기간에 포함된 내역이에요.</p>
    </template>
    <p v-else class="empty">거래내역을 찾을 수 없어요.</p>

    <BottomNav />

    <TransactionEditModal
      :model-value="Boolean(editMode)"
      :mode="editMode || 'category'"
      :categories="categories"
      :selected-category="selectedCategoryId"
      :memo="item?.memo"
      @update:model-value="value => { if (!value) editMode = null }"
      @save-category="saveCategory"
      @save-memo="saveMemo"
    />
  </main>
</template>

<style scoped>
.detail-page {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: 0 auto;
  padding: 14px 20px 105px;
  background: #eef2f8;
  color: #10192d;
}
.detail-page > header {
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: center;
  margin-bottom: 18px;
}
.detail-page > header button {
  width: 36px;
  height: 36px;
  color: #193d82;
  font-size: 24px;
  font-weight: 700;
  text-align: left;
}
.detail-page > header h1 {
  text-align: center;
  font-size: 17px;
  font-weight: 900;
  letter-spacing: -0.03em;
}
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(16, 25, 43, 0.05);
}
.hero small,
.hero b,
.hero strong,
.hero em {
  display: block;
}
.hero small {
  margin-bottom: 10px;
  color: #7186aa;
  font-size: 9px;
  font-weight: 700;
}
.hero b {
  color: #10192d;
  font-size: 14px;
  font-weight: 800;
}
.hero strong {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 900;
}
.hero em {
  margin-top: 5px;
  color: #94a3b8;
  font-size: 9px;
  font-style: normal;
}
.deposit {
  color: #173f8d;
}
.withdrawal {
  color: #e8484f;
}
.info-card {
  margin-top: 12px;
  padding: 6px 16px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(16, 25, 43, 0.05);
}
.info-card > div {
  display: grid;
  grid-template-columns: 100px 1fr;
  padding: 13px 0;
  border-bottom: 1px solid #eef1f6;
}
.info-card > div:last-child {
  border: 0;
}
.info-card small {
  color: #94a3b8;
  font-size: 10px;
}
.info-card p {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  text-align: right;
}
.info-card b {
  color: #10192d;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.4;
}
.info-card .accent {
  color: #173f8d;
  font-weight: 800;
}
.info-card button,
.section-heading button {
  padding: 4px 7px;
  border-radius: 7px;
  background: #eaf2ff;
  color: #173f8d;
  font-size: 8px;
  font-weight: 900;
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20px 3px 9px;
}
.section-heading h2 {
  color: #10192d;
  font-size: 12px;
  font-weight: 900;
}
.memo {
  min-height: 54px;
  padding: 15px;
  border-radius: 14px;
  background: #fff;
  color: #10192d;
  font-size: 11px;
  box-shadow: 0 8px 22px rgba(16, 25, 43, 0.05);
}
.trip-note {
  margin-top: 12px;
  padding: 13px;
  border-radius: 12px;
  background: #eaf2ff;
  color: #4a6390;
  text-align: center;
  font-size: 9px;
}
.empty {
  padding: 80px 0;
  text-align: center;
  color: #94a3b8;
}
</style>
