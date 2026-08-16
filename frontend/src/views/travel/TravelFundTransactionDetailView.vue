<script setup>
import { computed, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import api from '@/api';
import { useTravelFundStore } from '@/stores/travelFund';
import TransactionEditModal from '@/components/asset/TransactionEditModal.vue';

const route = useRoute(),
  router = useRouter(),
  fund = useTravelFundStore(),
  editMode = ref(null),
  transaction = ref(null);

// 거래 상세 데이터를 다시 가져오는 함수
const fetchTransaction = async () => {
  try {
    const res = await api.get(`/transactions/${route.params.transactionId}`);
    transaction.value = res.data.data;
  } catch (e) {
    console.error('거래 상세 조회 실패', e);
  }
};

onMounted(fetchTransaction);

const category = computed(() =>
  fund.getCategory(transaction.value?.categoryId),
);
// 국가별 기본 메타데이터 맵 (프랑스, 스위스, 독일, 일본, 홍콩)
const countryMeta = {
  FR: { flag: '🇫🇷', name: '프랑스', city: '파리', code: 'FR' },
  CH: { flag: '🇨🇭', name: '스위스', city: '취리히', code: 'CH' },
  DE: { flag: '🇩🇪', name: '독일', city: '베를린', code: 'DE' },
  JP: { flag: '🇯🇵', name: '일본', city: '도쿄', code: 'JP' },
  HK: { flag: '🇭🇰', name: '홍콩', city: '홍콩', code: 'HK' },
};

// 트랜잭션에 기록된 countryCode(또는 trip_country_id에 맞춘 기본값) 기준 매핑
const country = computed(() => {
  // 실제 데이터의 필드명(예: transaction.value?.countryCode)에 맞춰 fallback 설정
  const code = transaction.value?.countryCode || 'FR';

  return (
    countryMeta[code] || {
      flag: '✈️',
      name: '여행',
      city: '여행지',
      code: 'FR',
    }
  );
});

const currencyMeta = {
  FR: { code: 'EUR', rate: 1486.2 },
  CH: { code: 'CHF', rate: 1704.6 },
  DE: { code: 'EUR', rate: 1486.2 },
  JP: { code: 'JPY', rate: 9.23 },
  HK: { code: 'HKD', rate: 184.2 },
};

const localAmount = computed(() =>
  transaction.value && country.value
    ? Math.round(
        (transaction.value.amount /
          (currencyMeta[country.value.code]?.rate || 1)) *
          100,
      ) / 100
    : 0,
);

const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`;
const formatDateTime = (date, time) => {
  if (!date) return '-';

  const formattedDate = date.replaceAll('-', '.');
  // 'HH:mm:ss' 또는 'HH:mm'에서 'HH:mm'만 추출 (값 없으면 빈 문자열 또는 기본 시간)
  const formattedTime = time ? time.slice(0, 5) : '';

  return formattedTime ? `${formattedDate} ${formattedTime}` : formattedDate;
};

const rows = computed(() =>
  transaction.value
    ? [
        {
          label: '거래일시',
          value: formatDateTime(
            transaction.value.transactionDate,
            transaction.value.transactionTime,
          ),
        },
        {
          label: '여행지',
          value: `${country.value.city} · ${country.value.name}`,
        },
        {
          label: '카테고리',
          value: transaction.value.categoryName || '기타',
          editable: false,
        },
        {
          label: '거래구분',
          value:
            transaction.value.transactionType === 'WITHDRAWAL'
              ? '지출'
              : '입금',
        },
        {
          label: '결제수단',
          value: transaction.value.paymentMethodName || '알 수 없음',
        },
        { label: '사용처', value: transaction.value.merchantName },
      ]
    : [],
);
</script>
<template>
  <main class="detail-page">
    <header>
      <button @click="router.back()">‹</button>
      <h1>거래내역 상세보기</h1>
      <span />
    </header>

    <template v-if="transaction">
      <section class="hero">
        <div>
          <small>{{ country.flag }} {{ country.name }}</small>
          <b>{{ transaction.merchantName }}</b>
          <strong>
            -{{ currencyMeta[country.code]?.code }}
            {{
              localAmount.toLocaleString('ko-KR', { maximumFractionDigits: 2 })
            }}
          </strong>
          <em>약 {{ money(transaction.amount) }}</em>
        </div>
        <span :style="{ background: `${category?.color || '#98a7ba'}18` }">
          {{ transaction.icon || '•••' }}
        </span>
      </section>
      <section class="info-card">
        <div v-for="row in rows" :key="row.label">
          <small>{{ row.label }}</small>
          <p>
            <b
              :class="{ accent: row.editable }"
              :style="
                row.editable ? { color: category?.color || '#98a7ba' } : {}
              "
              >{{ row.value }}</b
            ><button v-if="row.editable" @click="editMode = 'category'">
              수정
            </button>
          </p>
        </div>
      </section>
      <div class="section-heading">
        <h2>메모</h2>
        <button @click="editMode = 'memo'">수정</button>
      </div>
      <section class="memo">
        {{ transaction.memo || '등록된 메모가 없어요.' }}
      </section>
      <p class="trip-note">
        이 거래는 등록한 {{ country.name }} 여행 기간에 포함된 내역이에요.
      </p></template
    >
    <p v-else class="empty">거래내역을 찾을 수 없어요.</p>
    <BottomNav></BottomNav>
    <TransactionEditModal
      :model-value="Boolean(editMode)"
      :mode="editMode || 'category'"
      :categories="fund.categories"
      :selected-category="transaction?.categoryId"
      :memo="transaction?.memo"
      @update:model-value="
        (value) => {
          if (!value) editMode = null;
        }
      "
      @save-category="
        async (value) => {
          await api.patch(`/transactions/${route.params.transactionId}`, {
            categoryId: value,
          });
          editMode = null;
          await fetchTransaction();
        }
      "
      @save-memo="
        async (value) => {
          await api.patch(`/transactions/${route.params.transactionId}`, {
            memo: value,
          });
          editMode = null;
          await fetchTransaction();
        }
      "
    />
  </main>
</template>

<style scoped>
.detail-page {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: 0 auto;
  padding: 48px 20px 105px;
  background: #f8f6f1;
  color: #10192d;
}
.detail-page > header {
  display: grid;
  grid-template-columns: 30px 1fr 30px;
  align-items: center;
  margin-bottom: 18px;
}
.detail-page > header button {
  font-size: 26px;
  text-align: left;
}
.detail-page > header h1 {
  text-align: center;
  font-size: 18px;
  font-weight: 900;
}
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px;
  border: 1px solid #d8e2f0;
  border-radius: 16px;
  background: #fff;
}
.hero small,
.hero b,
.hero strong,
.hero em {
  display: block;
}
.hero small {
  margin-bottom: 10px;
  color: #64748b;
  font-size: 9px;
}
.hero b {
  font-size: 14px;
}
.hero strong {
  margin-top: 8px;
  color: #e8484f;
  font-size: 24px;
}
.hero em {
  margin-top: 5px;
  color: #94a3b8;
  font-size: 9px;
  font-style: normal;
}
.hero > span {
  display: grid;
  width: 50px;
  height: 50px;
  place-items: center;
  border-radius: 50%;
  font-size: 21px;
}
.info-card {
  margin-top: 12px;
  padding: 10px 16px;
  border: 1px solid #dbe3ef;
  border-radius: 17px;
  background: #fff;
}
.info-card > div {
  display: grid;
  grid-template-columns: 100px 1fr;
  padding: 13px 0;
  border-bottom: 1px solid #edf0f5;
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
  font-size: 11px;
  line-height: 1.4;
}
.info-card button,
.section-heading button {
  padding: 4px 7px;
  border-radius: 7px;
  background: #edf4ff;
  color: #286dd8;
  font-size: 8px;
  font-weight: 900;
}
.section-heading {
  display: flex;
  justify-content: space-between;
  margin: 20px 3px 9px;
}
.section-heading h2 {
  font-size: 12px;
}
.memo {
  min-height: 54px;
  padding: 15px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #fff;
  font-size: 11px;
}
.trip-note {
  margin-top: 12px;
  padding: 13px;
  border-radius: 12px;
  background: #eaf2ff;
  color: #5274a8;
  text-align: center;
  font-size: 9px;
}
.empty {
  padding: 80px;
  text-align: center;
  color: #94a3b8;
}
</style>
