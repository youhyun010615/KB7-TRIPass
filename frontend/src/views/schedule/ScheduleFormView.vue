<script setup>
import {
  computed,
  onMounted,
  reactive,
  ref,
} from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import { useTravelScheduleStore } from '@/stores/travelSchedule';
import { useTravelStore } from '@/stores/travel';

const route = useRoute();
const router = useRouter();
const store = useTravelScheduleStore();
const travel = useTravelStore();
const editing = computed(() => Boolean(route.params.scheduleId));
const original = editing.value
  ? store.getSchedule(route.params.scheduleId)
  : null;
const initialCountryCode =
  original?.countryCode || store.countries[0]?.code || '';
const form = reactive(
  original
    ? { ...original }
    : {
        title: '',
        countryCode: initialCountryCode,
        date: store.period(initialCountryCode).startDate,
        time: '10:00',
        currency:
          store.countries.find((item) => item.code === initialCountryCode)
            ?.currency || 'EUR',
        amount: 0,
        paymentStatus: 'undecided',
        placeName: '',
        placeAddress: '',
        memo: '',
      },
);
const country = computed(() =>
  store.countries.find((item) => item.code === form.countryCode),
);
const selectedPeriod = computed(() =>
  country.value ? store.period(country.value.code) : null,
);
const dateInCountry = computed(
  () =>
    selectedPeriod.value &&
    form.date >= selectedPeriod.value.startDate &&
    form.date <= selectedPeriod.value.endDate,
);
const valid = computed(
  () =>
    form.title.trim() &&
    country.value &&
    dateInCountry.value &&
    form.time &&
    form.currency &&
    Number(form.amount) >= 0 &&
    form.placeName.trim(),
);
const error = computed(() =>
  form.date && !dateInCountry.value
    ? `${country.value?.name || '선택 국가'}의 여행 기간 안에서 날짜를 선택해 주세요.`
    : '',
);
const currencies = ['EUR', 'USD', 'CHF', 'JPY', 'HKD'];
const wonRates = { EUR: 1486.2, USD: 1380, CHF: 1704.6, JPY: 9.23, HKD: 184.2 };
const wonAmount = computed(() =>
  Math.round(Number(form.amount || 0) * (wonRates[form.currency] || 1)),
);
const tripDateRange = computed(() => {
  const plans = travel.selectedPlans || [];
  const starts = plans.map((item) => item.startDate).filter(Boolean).sort();
  const ends = plans.map((item) => item.endDate).filter(Boolean).sort();
  return starts.length && ends.length ? `${starts[0].replaceAll('-', '.')} ~ ${ends.at(-1).replaceAll('-', '.')}` : '';
});
function applyCountry() {
  if (!country.value) return;
  form.currency = country.value.currency;
  if (!dateInCountry.value) form.date = selectedPeriod.value.startDate;
}

const isSubmitting = ref(false);

async function submit() {
  if (!valid.value || isSubmitting.value) return;
  isSubmitting.value = true;
  const payload = {
    ...form,
    amount: Number(form.amount),
    title: form.title.trim(),
    placeName: form.placeName.trim(),
    placeAddress: '',
    memo: form.memo.trim(),
  };
  try {
    const success = editing.value
      ? await store.update(route.params.scheduleId, payload)
      : await store.save(payload);
    if (success)
      router.push(
        editing.value
          ? `/schedule/${route.params.scheduleId}`
          : { path: '/schedule', query: route.query },
      );
  } finally {
    isSubmitting.value = false;
  }
}

onMounted(() => {
  store.ensureTripLoaded().catch(() => {});
});
</script>

<template>
  <main class="form-page">
    <header>
      <button type="button" @click="router.back()">‹</button>
      <h1>여행일정 {{ editing ? '수정' : '추가' }}</h1>
      <span />
    </header>
    <section class="trip-summary">
      <div><b>{{ travel.tripName || '여행 일정' }}</b><small>{{ tripDateRange }}</small></div>
      <span>여행 중</span>
    </section>
    <section class="form-card">
      <div class="section-title"><h2>일정 정보</h2><p>여행 중 방문할 일정 정보를 입력해요.</p></div>
      <label
        ><span>일정명</span
        ><input
          v-model="form.title"
          placeholder="예: 루브르 박물관 가이드 투어"
      /></label>
      <label
        ><span>국가</span
        ><select v-model="form.countryCode" @change="applyCountry">
          <option
            v-for="item in store.countries"
            :key="item.code"
            :value="item.code"
          >
            {{ item.flag }} {{ item.name }}
          </option>
        </select></label
      >
      <label
        ><span>일시</span>
        <div class="split">
          <input
            v-model="form.date"
            type="date"
            :min="selectedPeriod?.startDate"
            :max="selectedPeriod?.endDate"
          /><input v-model="form.time" type="time" />
        </div>
        <small v-if="error" class="error">{{ error }}</small></label
      >
      <label
        ><span>장소명</span
        ><input v-model="form.placeName" placeholder="예: 루브르 박물관"
      /></label>
      <label
        ><span>금액</span>
        <div class="money">
          <select v-model="form.currency">
            <option v-for="item in currencies" :key="item">
              {{ item }}
            </option></select
          ><input v-model.number="form.amount" type="number" min="0" /><em>{{
            form.currency
          }}</em>
        </div>
        <div class="conversion">
          <output>원화 환산 금액 · 약 {{ wonAmount.toLocaleString() }}원</output>
        </div></label
      >
      <div class="payment-wrap">
        <strong>결제 상태</strong>
        <div class="payment">
        <button
          type="button"
          :class="{ active: form.paymentStatus === 'prepaid' }"
          @click="form.paymentStatus = 'prepaid'"
        >
          사전결제 완료</button
        ><button
          type="button"
          :class="{ active: form.paymentStatus === 'onsite' }"
          @click="form.paymentStatus = 'onsite'"
        >
          현장결제 필요</button
        ><button
          type="button"
          :class="{ active: form.paymentStatus === 'undecided' }"
          @click="form.paymentStatus = 'undecided'"
        >
          미정
        </button>
        </div>
      </div>
    </section>
    <section class="memo-card">
      <label><span>메모</span><textarea v-model="form.memo" maxlength="100" placeholder="일정에 필요한 내용을 메모해 주세요."/><small>{{ form.memo.length }}/100</small></label>
    </section>
    <p
      v-if="store.errorMessage"
      style="padding: 0 14px; color: #e5484d; font-size: 10px"
    >
      {{ store.errorMessage }}
    </p>
    <button
      class="submit"
      :disabled="!valid || isSubmitting"
      type="button"
      @click="submit"
    >
      {{ isSubmitting ? '처리 중...' : editing ? '수정 완료' : '등록하기' }}
    </button>
    <BottomNav />
  </main>
</template>

<style scoped>
.form-page {
  min-height: 100vh;
  padding: 0 16px 150px;
  background: #f3f6fc;
  color: #10192d;
}
.form-page > header {
  display: grid;
  grid-template-columns: 32px 1fr 32px;
  align-items: end;
  height: 59px;
  padding-bottom: 13px;
}
.form-page > header button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  background: #fff;
  color: #193d82;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 5px 16px rgba(36, 72, 117, 0.07);
}
.form-page h1 {
  text-align: center;
  font-size: 17px;
  font-weight: 900;
}
.form-card {
  overflow: hidden;
  border: 1px solid #dce4ee;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(23, 63, 141, 0.07);
}
.trip-summary{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px;padding:17px 18px;border:1px solid #d5e1f3;border-radius:18px;background:#fff;box-shadow:0 8px 20px rgba(23,63,141,.07)}
.trip-summary div{min-width:0}.trip-summary b,.trip-summary small{display:block}.trip-summary b{overflow:hidden;font-size:15px;font-weight:900;text-overflow:ellipsis;white-space:nowrap}.trip-summary small{margin-top:6px;color:#8493a9;font-size:10px}.trip-summary>span{flex:none;padding:7px 12px;border-radius:999px;background:#214d97;color:#fff;font-size:9px;font-weight:900}
.section-title{padding:18px 14px 4px}.section-title h2{font-size:17px;font-weight:900}.section-title p{margin-top:6px;color:#8493a9;font-size:10px}
.form-card > label {
  display: block;
  padding: 14px;
  border-bottom: 1px solid #edf0f4;
}
.form-card label > span {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 900;
}
.form-card input,
.form-card select,
.form-card textarea {
  width: 100%;
  padding: 13px;
  border: 1px solid #e0e6ef;
  border-radius: 9px;
  background: #fff;
  font-size: 11px;
  outline: none;
}
.form-card input:focus,
.form-card select:focus,
.form-card textarea:focus {
  border-color: #3477e9;
}
.split {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
}
.money {
  display: grid;
  grid-template-columns: 75px 1fr 40px;
  align-items: center;
  gap: 6px;
}
.money em {
  color: #64748b;
  font-size: 9px;
  font-style: normal;
}
.form-card textarea {
  height: 78px;
  resize: none;
}
.form-card label > small {
  display: block;
  margin-top: 6px;
  color: #6480a7;
  font-size: 10px;
  font-weight: 800;
  text-align: right;
}
.form-card label > .error {
  color: #e5484d;
  text-align: left;
}
.payment-wrap {
  padding: 13px;
  border-bottom: 1px solid #edf0f4;
}
.payment-wrap > strong {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 900;
}
.payment {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 7px;
}
.payment button {
  padding: 10px 4px;
  border: 1px solid #dfe5ed;
  border-radius: 9px;
  color: #64748b;
  font-size: 10px;
  font-weight: 800;
}
.payment button.active {
  border-color: #3477e9;
  background: #eef4ff;
  color: #246dd7;
  font-weight: 900;
}
.memo-card{margin-top:14px;padding:18px 14px;border:1px solid #dce4ee;border-radius:18px;background:#fff;box-shadow:0 8px 22px rgba(23,63,141,.06)}.memo-card label>span{display:block;margin-bottom:10px;font-size:14px;font-weight:900}.memo-card textarea{width:100%;height:112px;padding:14px;border:0;border-radius:14px;background:#f6f8fc;font-size:11px;line-height:1.6;resize:none;outline:none}.memo-card small{display:block;margin-top:6px;color:#8a97aa;font-size:8px;text-align:right}
.conversion {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 8px;
}
.conversion output {
  color: #6480a7;
  font-size: 10px;
  font-weight: 800;
}
.submit {
  position: fixed;
  right: max(calc((100vw - 390px) / 2 + 16px), 16px);
  bottom: 78px;
  left: max(calc((100vw - 390px) / 2 + 16px), 16px);
  z-index: 40;
  height: 52px;
  border-radius: 13px;
  background: #19489c;
  color: #fff;
  font-size: 13px;
  font-weight: 900;
}
.submit:disabled {
  background: #a7b2c6;
}
</style>
