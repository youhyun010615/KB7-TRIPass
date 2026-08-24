<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import {createSchedule} from '@/api/schedule';
import {fetchTripGoal} from '@/api/travel';
import {useTravelScheduleStore} from '@/stores/travelSchedule';
import {flagIconClass, useTravelStore} from '@/stores/travel';

const route = useRoute();
const router = useRouter();
const store = useTravelScheduleStore();
const travel = useTravelStore();
const editing = computed(() => Boolean(route.params.scheduleId));
const archiveMode = computed(() => Boolean(route.meta.scheduleArchive));
const requestedDate = computed(() => String(route.query.date || ''));
const archiveCountries = ref([]);
const countrySelectEl = ref(null);
const countryDropdownOpen = ref(false);
const currencySelectEl = ref(null);
const currencyDropdownOpen = ref(false);
const availableCountries = computed(() =>
    (archiveMode.value ? archiveCountries.value : travel.selectedPlans).map((plan) => ({
      ...plan,
      code: String(plan.code || '').toUpperCase(),
      currency: plan.currencyCode || plan.currency || '',
    })),
);
const original = editing.value
    ? store.getSchedule(route.params.scheduleId)
    : null;
const initialCountryCode = original?.countryCode || '';
const form = reactive(
    original
        ? {...original}
        : {
          title: '',
          countryCode: initialCountryCode,
          date: requestedDate.value || '',
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
    availableCountries.value.find((item) => item.code === form.countryCode),
);
const selectedPeriod = computed(() => country.value
    ? {startDate: country.value.startDate, endDate: country.value.endDate}
    : null);
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
// 원화와 사용자가 이번 여행에 등록한 모든 국가의 통화를 노출한다.
// 같은 통화를 사용하는 국가가 여럿이어도 선택 목록에는 한 번만 표시한다.
const currencies = computed(() =>
    [...new Set([
      'KRW',
      ...availableCountries.value
          .map((item) => String(item.currency || '').toUpperCase())
          .filter(Boolean),
    ])],
);
const currencyNames = {
  KRW: '대한민국 원', AED: '아랍에미리트 디르함', AUD: '호주 달러', BHD: '바레인 디나르',
  BND: '브루나이 달러', CAD: '캐나다 달러', CHF: '스위스 프랑', CNH: '중국 위안화(역외)',
  CNY: '중국 위안화', DKK: '덴마크 크로네', EUR: '유로', GBP: '영국 파운드',
  HKD: '홍콩 달러', IDR: '인도네시아 루피아', JPY: '일본 엔', KWD: '쿠웨이트 디나르',
  MYR: '말레이시아 링깃', NOK: '노르웨이 크로네', NZD: '뉴질랜드 달러', SAR: '사우디아라비아 리얄',
  SEK: '스웨덴 크로나', SGD: '싱가포르 달러', THB: '태국 바트', USD: '미국 달러',
};
const wonRates = {
  EUR: 1486.2, USD: 1380, CHF: 1704.6, JPY: 9.23, HKD: 184.2, GBP: 1750,
  AED: 375.8, AUD: 900, BHD: 3670, BND: 1022, CAD: 1010, CNH: 190, CNY: 190,
  DKK: 199, IDR: 0.087, KWD: 4480, MYR: 295, NOK: 130, NZD: 830, SAR: 368,
  SEK: 130, SGD: 1020, THB: 38.9,
};
const wonAmount = computed(() =>
    Math.round(Number(form.amount || 0) * (wonRates[form.currency] || 1)),
);
const tripDateRange = computed(() => {
  const plans = availableCountries.value;
  const starts = plans.map((item) => item.startDate).filter(Boolean).sort();
  const ends = plans.map((item) => item.endDate).filter(Boolean).sort();
  return starts.length && ends.length ? `${starts[0].replaceAll('-', '.')} ~ ${ends.at(-1).replaceAll('-', '.')}` : '';
});

function applyCountry() {
  if (!country.value) return;
  form.currency = country.value.currency;
  if (!dateInCountry.value) form.date = selectedPeriod.value.startDate;
}

function selectCountry(item) {
  form.countryCode = item.code;
  applyCountry();
  countryDropdownOpen.value = false;
}

function closeDropdowns(event) {
  if (!countrySelectEl.value?.contains(event.target)) countryDropdownOpen.value = false;
  if (!currencySelectEl.value?.contains(event.target)) currencyDropdownOpen.value = false;
}

function selectCurrency(item) {
  form.currency = item;
  currencyDropdownOpen.value = false;
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
    let success;
    if (!editing.value && archiveMode.value) {
      const tripId = Number(route.params.id || route.query.tripId);
      const tripCountryId = country.value?.tripCountryId;
      if (!tripId || !tripCountryId) {
        store.errorMessage = '선택한 여행의 국가 정보를 불러오지 못했어요.';
        success = false;
      } else {
        await createSchedule(tripId, {
          tripCountryId,
          scheduleName: payload.title,
          scheduledAt: `${payload.date}T${payload.time}:00`,
          amount: payload.amount,
          currencyCode: payload.currency || undefined,
          paymentStatus: String(payload.paymentStatus || 'undecided').toUpperCase(),
          placeName: payload.placeName,
          placeAddress: '',
          memo: payload.memo,
        });
        success = true;
      }
    } else {
      success = editing.value
          ? await store.update(route.params.scheduleId, payload)
          : await store.save(payload);
    }
    if (success) {
      if (editing.value) {
        const tripId = route.query.tripId;
        const scheduleId = route.params.scheduleId;

        const detailPath = tripId
            ? `/mypage/travel/${tripId}/schedules/${scheduleId}`
            : `/schedule/${scheduleId}`;

        const previousPath = String(
            window.history.state?.back || '',
        ).split('?')[0];

        if (previousPath === detailPath) {
          router.back();
        } else {
          router.replace(detailPath);
        }
      } else if (archiveMode.value) {
        const tripId = route.params.id || route.query.tripId;

        router.replace({
          path: `/mypage/travel/${tripId}/schedules`,
          query: {tripId},
        });
      } else {
        router.push({
          path: '/schedule',
          query: route.query,
        });
      }
    }
  } catch (error) {
    store.errorMessage = error.response?.data?.message || '여행 일정을 등록하지 못했어요.';
  } finally {
    isSubmitting.value = false;
  }
}

onMounted(async () => {
  document.addEventListener('pointerdown', closeDropdowns);
  if (archiveMode.value) {
    const tripId = Number(route.params.id || route.query.tripId);
    try {
      if (!travel.countries.length) await travel.loadCountries();
      const trip = await fetchTripGoal(tripId);
      archiveCountries.value = (trip?.countries || []).map((item) => {
        const catalog = travel.countries.find(
            (entry) => Number(entry.countryId) === Number(item.countryId),
        );
        const name = item.countryName || catalog?.name || '';
        const presentation = travel.countryFlagMap[name] || {};
        return {
          ...catalog,
          ...item,
          code: catalog?.code || presentation.code || '',
          name,
          flag: catalog?.flag || presentation.emoji || '🌍',
          currencyCode: item.currencyCode || catalog?.currencyCode || '',
          startDate: item.arrivalDate || item.startDate || '',
          endDate: item.departureDate || item.endDate || '',
        };
      });
    } catch {
      archiveCountries.value = [];
      store.errorMessage = '선택한 여행의 국가 정보를 불러오지 못했어요.';
    }
  } else {
    await store.ensureTripLoaded().catch(() => {
    });
  }
  if (!editing.value && availableCountries.value.length) {
    const isConfiguredCountry = availableCountries.value.some(
        (item) => item.code === form.countryCode,
    );
    if (!isConfiguredCountry) {
      form.countryCode = availableCountries.value[0].code;
      applyCountry();
    }
  }
  if (!editing.value && requestedDate.value) {
    const requestedCountry = availableCountries.value.find(
        (item) => requestedDate.value >= item.startDate && requestedDate.value <= item.endDate,
    );
    if (requestedCountry) {
      form.countryCode = requestedCountry.code;
      form.currency = requestedCountry.currency;
      form.date = requestedDate.value;
    }
  }
});
onBeforeUnmount(() => document.removeEventListener('pointerdown', closeDropdowns));
</script>

<template>
  <main class="form-page" :class="{ 'archive-form': archiveMode }">
    <header>
      <button type="button" @click="router.back()">‹</button>
      <h1>여행일정 {{ editing ? '수정' : '추가' }}</h1>
      <span/>
    </header>
    <section v-if="editing" class="trip-summary">
      <div><b>{{ travel.tripName || '여행 일정' }}</b><small>{{ tripDateRange }}</small></div>
      <span>여행 중</span>
    </section>
    <section class="form-card">
      <div class="section-title"><h2>일정 정보</h2>
        <p>여행 중 방문할 일정 정보를 입력해요.</p></div>
      <label
      ><span>일정명</span
      ><input
          v-model="form.title"
          placeholder="예: 루브르 박물관 가이드 투어"
      /></label>
      <label ref="countrySelectEl" class="country-field">
        <span>국가</span>
        <button
            type="button"
            class="country-trigger"
            :class="{ open: countryDropdownOpen }"
            @click="countryDropdownOpen = !countryDropdownOpen"
        >
          <span class="country-value">
            <i v-if="country" :class="flagIconClass(country.code)" class="country-flag"/>
            {{ country?.name || '국가를 선택해 주세요' }}
          </span>
          <b aria-hidden="true">⌄</b>
        </button>
        <div v-if="countryDropdownOpen" class="country-options">
          <button
              v-for="item in availableCountries"
              :key="item.code"
              type="button"
              :class="{ selected: item.code === form.countryCode }"
              @click="selectCountry(item)"
          >
            <i :class="flagIconClass(item.code)" class="country-flag"/>
            <span>{{ item.name }}</span>
            <b v-if="item.code === form.countryCode">✓</b>
          </button>
        </div>
      </label>
      <label
      ><span>일시</span>
        <div class="split">
          <input
              v-model="form.date"
              type="date"
              :min="selectedPeriod?.startDate"
              :max="selectedPeriod?.endDate"
          /><input v-model="form.time" type="time"/>
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
          <div ref="currencySelectEl" class="currency-field">
            <button
                type="button"
                class="currency-trigger"
                :class="{ open: currencyDropdownOpen }"
                @click="currencyDropdownOpen = !currencyDropdownOpen"
            >
              <span class="currency-trigger-value">
                <span class="currency-code">{{ form.currency }}</span>
                <span class="currency-name">{{ currencyNames[form.currency] || form.currency }}</span>
              </span>
              <b aria-hidden="true">⌄</b>
            </button>
            <div v-if="currencyDropdownOpen" class="currency-options">
              <button
                  v-for="item in currencies"
                  :key="item"
                  type="button"
                  :class="{ selected: item === form.currency }"
                  @click="selectCurrency(item)"
              >
                <span class="currency-code">{{ item }}</span>
                <span class="currency-name">{{ currencyNames[item] || item }}</span>
                <b v-if="item === form.currency">✓</b>
              </button>
            </div>
          </div>
          <input v-model.number="form.amount" type="number" min="0"/><em>{{
            form.currency
          }}</em>
        </div>
        <div class="conversion">
          <output>원화 환산 금액 · 약 {{ wonAmount.toLocaleString() }}원</output>
        </div>
      </label
      >
      <div class="payment-wrap">
        <strong>결제 상태</strong>
        <div class="payment">
          <button
              type="button"
              :class="{ active: form.paymentStatus === 'prepaid' }"
              @click="form.paymentStatus = 'prepaid'"
          >
            사전결제 완료
          </button
          >
          <button
              type="button"
              :class="{ active: form.paymentStatus === 'onsite' }"
              @click="form.paymentStatus = 'onsite'"
          >
            현장결제 필요
          </button
          >
          <button
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
      <label><span>메모</span><textarea v-model="form.memo" maxlength="100" placeholder="일정에 필요한 내용을 메모해 주세요."/><small>{{
          form.memo.length
        }}/100</small></label>
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
    <!-- <BottomNav v-if="!archiveMode" /> -->
  </main>
</template>

<style scoped>
.form-page {
  min-height: 100vh;
  padding: 0 16px 24px;
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
  overflow: visible;
  border: 1px solid #dce4ee;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(23, 63, 141, 0.07);
}

.country-field {
  position: relative;
  z-index: 8
}

.country-trigger {
  display: flex;
  width: 100%;
  height: 45px;
  align-items: center;
  justify-content: space-between;
  padding: 0 13px;
  border: 1px solid #e0e6ef;
  border-radius: 9px;
  background: #fff;
  color: #10192d;
  font-size: 12px;
  font-weight: 800
}

.country-trigger.open {
  border-color: #3477e9;
  box-shadow: 0 0 0 2px rgba(52, 119, 233, .1)
}

.country-value {
  display: flex !important;
  align-items: center;
  gap: 9px;
  margin: 0 !important;
  font-size: 12px !important
}

.country-trigger > b {
  color: #64748b;
  font-size: 18px
}

.country-flag {
  display: inline-block;
  width: 23px;
  height: 15px;
  flex: none;
  border-radius: 3px;
  background-position: center;
  background-size: cover;
  box-shadow: 0 1px 4px rgba(15, 35, 70, .16)
}

.country-options {
  position: absolute;
  top: 82px;
  right: 14px;
  left: 14px;
  z-index: 50;
  overflow: hidden;
  padding: 6px;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(16, 38, 78, .18)
}

.country-options button {
  display: flex;
  width: 100%;
  height: 43px;
  align-items: center;
  gap: 10px;
  padding: 0 11px;
  border-radius: 8px;
  background: #fff;
  color: #17233a;
  font-size: 12px;
  font-weight: 800;
  text-align: left
}

.country-options button:hover, .country-options button.selected {
  background: #edf4ff;
  color: #1f64d5
}

.country-options button span {
  margin: 0 !important;
  font-size: 12px !important
}

.country-options button b {
  margin-left: auto;
  color: #246dd7
}

.currency-field {
  position: relative;
  z-index: 7
}

.currency-trigger {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 44px;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 0 10px;
  border: 1px solid #e0e6ef;
  border-radius: 9px;
  background: #fff;
  color: #10192d;
  font-size: 11px;
  font-weight: 800
}

.currency-trigger.open {
  border-color: #3477e9;
  box-shadow: 0 0 0 2px rgba(52, 119, 233, .1)
}

.currency-trigger > b {
  flex: none;
  color: #64748b;
  font-size: 16px
}

.currency-trigger-value {
  display: flex;
  min-width: 0;
  align-items: baseline;
  gap: 5px;
  overflow: hidden
}

.currency-code {
  flex: none;
  font-size: 11px;
  font-weight: 800;
  color: #10192d
}

.currency-name {
  overflow: hidden;
  color: #94a3b8;
  font-size: 9px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap
}

.currency-options {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 50;
  width: 170px;
  overflow: hidden;
  padding: 6px;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(16, 38, 78, .18)
}

.currency-options button {
  display: flex;
  width: 100%;
  height: 38px;
  align-items: center;
  gap: 5px;
  padding: 0 10px;
  border-radius: 8px;
  background: #fff;
  color: #17233a;
  font-size: 11px;
  font-weight: 800;
  text-align: left
}

.currency-options button:hover, .currency-options button.selected {
  background: #edf4ff
}

.currency-options button:hover .currency-code, .currency-options button.selected .currency-code {
  color: #1f64d5
}

.currency-options button .currency-name {
  color: #94a3b8;
  font-size: 9px;
  font-weight: 600
}

.currency-options button b {
  margin-left: auto;
  flex: none;
  color: #246dd7
}

.trip-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding: 17px 18px;
  border: 1px solid #d5e1f3;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(23, 63, 141, .07)
}

.trip-summary div {
  min-width: 0
}

.trip-summary b, .trip-summary small {
  display: block
}

.trip-summary b {
  overflow: hidden;
  font-size: 15px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap
}

.trip-summary small {
  margin-top: 6px;
  color: #8493a9;
  font-size: 10px
}

.trip-summary > span {
  flex: none;
  padding: 7px 12px;
  border-radius: 999px;
  background: #214d97;
  color: #fff;
  font-size: 9px;
  font-weight: 900
}

.section-title {
  padding: 18px 14px 4px
}

.section-title h2 {
  font-size: 17px;
  font-weight: 900
}

.section-title p {
  margin-top: 6px;
  color: #8493a9;
  font-size: 10px
}

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
  width: 100%;
  min-width: 0;
  overflow: hidden;
  border-radius: 9px;
}

.split > input {
  display: block;
  inline-size: 100%;
  width: 100%;
  min-inline-size: 0;
  min-width: 0;
  max-width: 100%;
  height: 45px;
  box-sizing: border-box;
  -webkit-appearance: none;
  appearance: none;
}

.split > input::-webkit-date-and-time-value {
  min-width: 0;
  text-align: left;
}

.money {
  display: grid;
  grid-template-columns: 130px 1fr 40px;
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

.memo-card {
  margin-top: 14px;
  padding: 18px 14px;
  border: 1px solid #dce4ee;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(23, 63, 141, .06)
}

.memo-card label > span {
  display: block;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 900
}

.memo-card textarea {
  width: 100%;
  height: 112px;
  padding: 14px;
  border: 0;
  border-radius: 14px;
  background: #f6f8fc;
  font-size: 11px;
  line-height: 1.6;
  resize: none;
  outline: none
}

.memo-card small {
  display: block;
  margin-top: 6px;
  color: #8a97aa;
  font-size: 8px;
  text-align: right
}

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
  width: 100%;
  margin: 20px 0 24px;
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
