<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelScheduleStore } from '@/stores/travelSchedule'

const route = useRoute()
const router = useRouter()
const store = useTravelScheduleStore()
const editing = computed(() => Boolean(route.params.scheduleId))
const original = editing.value ? store.getSchedule(route.params.scheduleId) : null
const initialCountryCode = original?.countryCode || store.countries[0]?.code || ''
const form = reactive(original ? { ...original } : {
  title: '', countryCode: initialCountryCode, date: store.period(initialCountryCode).startDate,
  time: '10:00', currency: store.countries.find(item => item.code === initialCountryCode)?.currency || 'EUR',
  amount: 0, paymentStatus: 'undecided', placeName: '', placeAddress: '', memo: '',
})
const addressInput = ref(null)
const country = computed(() => store.countries.find(item => item.code === form.countryCode))
const selectedPeriod = computed(() => country.value ? store.period(country.value.code) : null)
const dateInCountry = computed(() => selectedPeriod.value && form.date >= selectedPeriod.value.startDate && form.date <= selectedPeriod.value.endDate)
const valid = computed(() => form.title.trim() && country.value && dateInCountry.value && form.time && form.currency && Number(form.amount) >= 0 && form.placeName.trim() && form.placeAddress.trim())
const error = computed(() => form.date && !dateInCountry.value ? `${country.value?.name || '선택 국가'}의 여행 기간 안에서 날짜를 선택해 주세요.` : '')
const currencies = ['EUR', 'USD', 'CHF', 'JPY', 'HKD']
const wonRates = { EUR: 1486.2, USD: 1380, CHF: 1704.6, JPY: 9.23, HKD: 184.2 }
const wonAmount = computed(() => Math.round(Number(form.amount || 0) * (wonRates[form.currency] || 1)))
let autocomplete

function applyCountry() {
  if (!country.value) return
  form.currency = country.value.currency
  if (!dateInCountry.value) form.date = selectedPeriod.value.startDate
  setupPlaces()
}

function setupPlaces() {
  nextTick(() => {
    if (!addressInput.value || !window.google?.maps?.places?.Autocomplete) return
    autocomplete = new window.google.maps.places.Autocomplete(addressInput.value, {
      fields: ['formatted_address', 'name'],
      componentRestrictions: { country: form.countryCode.toLowerCase() },
    })
    autocomplete.addListener('place_changed', () => {
      const place = autocomplete.getPlace()
      if (place.name && !form.placeName) form.placeName = place.name
      if (place.formatted_address) form.placeAddress = place.formatted_address
    })
  })
}

function loadGooglePlaces() {
  if (window.google?.maps?.places) return setupPlaces()
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY
  if (!apiKey || document.querySelector('script[data-tripass-google-places]')) return
  const script = document.createElement('script')
  script.dataset.tripassGooglePlaces = 'true'
  script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&libraries=places&loading=async`
  script.async = true
  script.onload = setupPlaces
  document.head.appendChild(script)
}

function searchAddress() {
  const query = [form.placeName, form.placeAddress, country.value?.name].filter(Boolean).join(' ')
  window.open(`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`, '_blank', 'noopener,noreferrer')
}

const isSubmitting = ref(false)

async function submit() {
  if (!valid.value || isSubmitting.value) return
  isSubmitting.value = true
  const payload = {
    ...form, amount: Number(form.amount), title: form.title.trim(),
    placeName: form.placeName.trim(), placeAddress: form.placeAddress.trim(), memo: form.memo.trim(),
  }
  try {
    const success = editing.value ? await store.update(route.params.scheduleId, payload) : await store.save(payload)
    if (success) router.push(editing.value ? `/schedule/${route.params.scheduleId}` : { path: '/schedule', query: route.query })
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  loadGooglePlaces()
  store.ensureTripLoaded().catch(() => {})
})
onBeforeUnmount(() => { autocomplete = null })
</script>

<template>
  <main class="form-page">
    <header><button type="button" @click="router.back()">‹</button><h1>여행일정 {{ editing ? '수정' : '추가' }}</h1><span /></header>
    <section class="form-card">
      <label><span>▱ 일정명</span><input v-model="form.title" placeholder="예: 루브르 박물관 가이드 투어"></label>
      <label><span>🌐 국가</span><select v-model="form.countryCode" @change="applyCountry"><option v-for="item in store.countries" :key="item.code" :value="item.code">{{ item.flag }} {{ item.name }}</option></select></label>
      <label><span>▣ 일시</span><div class="split"><input v-model="form.date" type="date" :min="selectedPeriod?.startDate" :max="selectedPeriod?.endDate"><input v-model="form.time" type="time"></div><small v-if="error" class="error">{{ error }}</small><small v-else-if="country">{{ country.flag }} {{ country.name }} 여행 일정</small></label>
      <label><span>₩ 금액</span><div class="money"><select v-model="form.currency"><option v-for="item in currencies" :key="item">{{ item }}</option></select><input v-model.number="form.amount" type="number" min="0"><em>{{ form.currency }}</em></div><div class="conversion"><small>원화 환산 금액</small><output>약 {{ wonAmount.toLocaleString() }}원</output></div></label>
      <div class="payment"><button type="button" :class="{ active: form.paymentStatus === 'prepaid' }" @click="form.paymentStatus = 'prepaid'">💳 사전결제 완료</button><button type="button" :class="{ active: form.paymentStatus === 'onsite' }" @click="form.paymentStatus = 'onsite'">💵 현장결제 필요</button><button type="button" :class="{ active: form.paymentStatus === 'undecided' }" @click="form.paymentStatus = 'undecided'">❔ 미정</button></div>
      <label><span>📍 장소명</span><input v-model="form.placeName" placeholder="예: 루브르 박물관"></label>
      <label><span>🗺 주소</span><div class="address"><input ref="addressInput" v-model="form.placeAddress" placeholder="주소를 입력하거나 검색해 주세요"><button type="button" aria-label="Google 지도에서 주소 검색" @click="searchAddress">⌕</button></div></label>
      <label><span>📝 메모</span><textarea v-model="form.memo" maxlength="100" placeholder="일정에 필요한 내용을 메모해 주세요."/><small>{{ form.memo.length }}/100</small></label>
    </section>
    <p v-if="store.errorMessage" style="padding:0 14px;color:#e5484d;font-size:10px">{{ store.errorMessage }}</p>
    <button class="submit" :disabled="!valid || isSubmitting" type="button" @click="submit">{{ isSubmitting ? '처리 중...' : (editing ? '수정 완료' : '등록하기') }}</button>
    <BottomNav />
  </main>
</template>

<style scoped>
.form-page{min-height:100vh;padding:0 16px 150px;background:#f8f6f1;color:#10192d}.form-page>header{display:grid;grid-template-columns:32px 1fr 32px;align-items:end;height:78px;padding-bottom:13px}.form-page>header button{font-size:28px;text-align:left}.form-page h1{text-align:center;font-size:17px;font-weight:900}.form-card{overflow:hidden;border:1px solid #dce4ee;border-radius:16px;background:#fff}.form-card>label{display:block;padding:14px;border-bottom:1px solid #edf0f4}.form-card label>span{display:block;margin-bottom:8px;font-size:10px;font-weight:900}.form-card input,.form-card select,.form-card textarea{width:100%;padding:11px;border:1px solid #e0e6ef;border-radius:9px;background:#fff;font-size:10px;outline:none}.form-card input:focus,.form-card select:focus,.form-card textarea:focus{border-color:#3477e9}.split{display:grid;grid-template-columns:1fr 90px;gap:7px}.money{display:grid;grid-template-columns:75px 1fr 40px;align-items:center;gap:6px}.money em{color:#64748b;font-size:9px;font-style:normal}.form-card textarea{height:78px;resize:none}.form-card label>small{display:block;margin-top:6px;color:#6480a7;font-size:8px;text-align:right}.form-card label>.error{color:#e5484d;text-align:left}.payment{display:grid;grid-template-columns:repeat(3,1fr);gap:7px;padding:13px;border-bottom:1px solid #edf0f4}.payment button{padding:10px 4px;border:1px solid #dfe5ed;border-radius:9px;color:#64748b;font-size:8px}.payment button.active{border-color:#3477e9;background:#eef4ff;color:#246dd7;font-weight:900}.address{display:grid;grid-template-columns:1fr 42px;gap:7px}.address button{border-radius:9px;background:#19489c;color:#fff;font-size:22px}.conversion{display:flex;align-items:center;justify-content:space-between;margin-top:8px}.conversion small{color:#246dd7;font-size:9px;font-weight:800}.conversion output{color:#19489c;font-size:11px;font-weight:900}.submit{position:fixed;right:max(calc((100vw - 390px)/2 + 16px),16px);bottom:78px;left:max(calc((100vw - 390px)/2 + 16px),16px);z-index:40;height:52px;border-radius:13px;background:#19489c;color:#fff;font-size:13px;font-weight:900}.submit:disabled{background:#a7b2c6}
</style>
