<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelCardsStore } from '@/stores/travelCards'
import { getTravelCardImage } from '@/utils/travelCard'

const router = useRouter()
const travelCardsStore = useTravelCardsStore()
const openFilter = ref('')
const resultCount = computed(() => travelCardsStore.cards.length)
const hasActiveFilters = computed(() => Boolean(
    travelCardsStore.keyword.trim() ||
    travelCardsStore.currencyCode ||
    travelCardsStore.instantUse !== null ||
    travelCardsStore.transitCard !== null,
))

const PAGE_SIZE = 6
const currentPage = ref(1)
const totalPages = computed(() =>
    Math.max(1, Math.ceil(travelCardsStore.cards.length / PAGE_SIZE)),
)
const pagedCards = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return travelCardsStore.cards.slice(start, start + PAGE_SIZE)
})

function goToPage(page) {
  currentPage.value = Math.min(Math.max(1, page), totalPages.value)
}

const currencyOptions = [
  { code: '', name: '전체 통화' },
  { code: 'USD', name: '미국 달러' },
  { code: 'JPY', name: '일본 엔' },
  { code: 'EUR', name: '유로' },
  { code: 'GBP', name: '영국 파운드' },
  { code: 'CNH', name: '중국 위안' },
  { code: 'HKD', name: '홍콩 달러' },
  { code: 'SGD', name: '싱가포르 달러' },
  { code: 'THB', name: '태국 바트' },
  { code: 'VND', name: '베트남 동' },
  { code: 'CHF', name: '스위스 프랑' },
  { code: 'AUD', name: '호주 달러' },
  { code: 'CAD', name: '캐나다 달러' },
  { code: 'NZD', name: '뉴질랜드 달러' },
  { code: 'TWD', name: '대만 달러' },
  { code: 'IDR', name: '인도네시아 루피아' },
  { code: 'MYR', name: '말레이시아 링깃' },
  { code: 'PHP', name: '필리핀 페소' },
  { code: 'AED', name: '아랍에미리트 디르함' },
  { code: 'SAR', name: '사우디아라비아 리얄' },
  { code: 'SEK', name: '스웨덴 크로나' },
  { code: 'DKK', name: '덴마크 크로네' },
  { code: 'NOK', name: '노르웨이 크로네' },
]

const companyColors = {
  KB국민카드: '#e4ad00',
  하나카드: '#12aa92',
  우리카드: '#2472c8',
  신한카드: '#2865e8',
}

const instantUseOptions = [
  { value: null, label: '전체' },
  { value: true, label: '바로 이용 가능' },
  { value: false, label: '연계계좌 필요' },
]

const transitCardOptions = [
  { value: null, label: '전체' },
  { value: true, label: '지원' },
  { value: false, label: '미지원' },
]

const selectedCurrencyLabel = computed(() => {
  const selected = currencyOptions.find((item) => item.code === travelCardsStore.currencyCode)
  return selected?.name || '전체 통화'
})
const selectedInstantUseLabel = computed(() =>
  instantUseOptions.find((item) => item.value === travelCardsStore.instantUse)?.label || '전체',
)
const selectedTransitCardLabel = computed(() =>
  transitCardOptions.find((item) => item.value === travelCardsStore.transitCard)?.label || '전체',
)

function toggleFilter(filterName) {
  openFilter.value = openFilter.value === filterName ? '' : filterName
}

async function selectFilter(filterName, value) {
  if (filterName === 'currency') travelCardsStore.currencyCode = value
  if (filterName === 'instantUse') travelCardsStore.instantUse = value
  if (filterName === 'transitCard') travelCardsStore.transitCard = value
  openFilter.value = ''
  await applyFilters()
}

function closeFilters() {
  openFilter.value = ''
}

function getCompanyColor(cardCompany) {
  return companyColors[cardCompany] || '#315b9f'
}

function getCompanyCode(cardCompany) {
  if (cardCompany?.includes('KB')) {
    return 'KB'
  }

  if (cardCompany?.includes('하나')) {
    return 'H'
  }

  if (cardCompany?.includes('우리')) {
    return 'W'
  }

  if (cardCompany?.includes('신한')) {
    return 'S'
  }

  return cardCompany?.charAt(0) || 'C'
}

function getErrorMessage() {
  return (
      travelCardsStore.errorMessage ||
      '트래블카드 목록을 불러오지 못했습니다.'
  )
}

async function loadCards() {
  try {
    await travelCardsStore.loadCards()
    currentPage.value = 1
  } catch {
    // 오류 내용은 Store의 errorMessage로 표시합니다.
  }
}

async function searchCards() {
  await loadCards()
}

async function applyFilters() {
  await loadCards()
}

async function resetFilters() {
  travelCardsStore.resetFilters()
  await loadCards()
}

function openCardDetail(cardId) {
  router.push(`/financial/cards/${cardId}`)
}

function openComparison() {
  router.push('/financial/cards/compare')
}

function toggleComparison(cardId) {
  const wasCompared =
      travelCardsStore.isCompared(cardId)

  const changed =
      travelCardsStore.toggleComparisonCard(cardId)

  if (!changed && !wasCompared) {
    window.alert(
        '비교할 트래블카드는 최대 3개까지 선택할 수 있습니다.',
    )
  }
}

onMounted(() => {
  loadCards()
  document.addEventListener('click', closeFilters)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeFilters)
})
</script>

<template>
  <main class="page">
    <div class="shell">
      <header class="page-header">
        <button
            type="button"
            class="back-button"
            aria-label="뒤로 가기"
            @click="router.back()"
        >
          ‹
        </button>

        <div>
          <small>TRIPASS CARD FINDER</small>
          <h1>트래블카드 찾기</h1>
        </div>

        <span />
      </header>

      <section class="finder-hero">
        <span class="hero-orbit" aria-hidden="true"></span>
        <div class="hero-copy">
          <small>SMART TRAVEL CARD</small>
          <h2>내 여행에 맞는<br />카드를 찾아보세요</h2>
          <p>환전·해외결제·교통 혜택을<br />한곳에서 비교할 수 있어요.</p>
        </div>
        <div class="hero-card" aria-hidden="true">
          <i></i><b>TRIPASS</b><span>GLOBAL PASS</span>
        </div>
      </section>

      <form
          class="search-form"
          @submit.prevent="searchCards"
      >
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none"><circle cx="11" cy="11" r="6.5" stroke="currentColor" stroke-width="2"/><path d="m16 16 4 4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>

        <input
            v-model="travelCardsStore.keyword"
            type="search"
            placeholder="카드명 또는 카드사를 검색하세요"
            aria-label="트래블카드 검색어"
        />

        <button
            type="submit"
            :disabled="travelCardsStore.listLoading"
        >
          찾기
        </button>
      </form>

      <section class="filters">
        <div class="filter-field" @click.stop>
          <span>여행 통화</span>
          <button type="button" :aria-expanded="openFilter === 'currency'" @click="toggleFilter('currency')">
            {{ selectedCurrencyLabel }} <i>⌄</i>
          </button>
          <div v-if="openFilter === 'currency'" class="filter-menu wide">
            <button
                v-for="currency in currencyOptions"
                :key="currency.code || 'ALL'"
                type="button"
                :class="{ selected: travelCardsStore.currencyCode === currency.code }"
                @click="selectFilter('currency', currency.code)"
            >
              <span>{{ currency.name }}</span><small>{{ currency.code || 'ALL' }}</small>
            </button>
          </div>
        </div>

        <div class="filter-field" @click.stop>
          <span>이용 방식</span>
          <button type="button" :aria-expanded="openFilter === 'instantUse'" @click="toggleFilter('instantUse')">
            {{ selectedInstantUseLabel }} <i>⌄</i>
          </button>
          <div v-if="openFilter === 'instantUse'" class="filter-menu">
            <button v-for="option in instantUseOptions" :key="String(option.value)" type="button" :class="{ selected: travelCardsStore.instantUse === option.value }" @click="selectFilter('instantUse', option.value)">
              <span>{{ option.label }}</span>
            </button>
          </div>
        </div>

        <div class="filter-field" @click.stop>
          <span>교통 기능</span>
          <button type="button" :aria-expanded="openFilter === 'transitCard'" @click="toggleFilter('transitCard')">
            {{ selectedTransitCardLabel }} <i>⌄</i>
          </button>
          <div v-if="openFilter === 'transitCard'" class="filter-menu">
            <button v-for="option in transitCardOptions" :key="String(option.value)" type="button" :class="{ selected: travelCardsStore.transitCard === option.value }" @click="selectFilter('transitCard', option.value)">
              <span>{{ option.label }}</span>
            </button>
          </div>
        </div>
      </section>

      <div class="list-heading">
        <div>
          <small>TRIP PICK</small>
          <h2>추천 카드 <b>{{ resultCount }}</b></h2>
        </div>

        <div class="list-actions">
          <button
              v-if="hasActiveFilters"
              type="button"
              class="reset-button"
              @click="resetFilters"
          >
            초기화 ↻
          </button>
          <button type="button" class="list-compare-button" @click="openComparison">
            카드 비교
            <b>{{ travelCardsStore.comparedCardCount }}</b>
          </button>
        </div>
      </div>

      <section
          v-if="travelCardsStore.listLoading"
          class="state-card"
      >
        <b>트래블카드를 불러오고 있어요.</b>
        <p>잠시만 기다려 주세요.</p>
      </section>

      <section
          v-else-if="travelCardsStore.errorMessage"
          class="state-card error"
      >
        <b>목록을 불러오지 못했습니다.</b>
        <p>{{ getErrorMessage() }}</p>

        <button
            type="button"
            @click="loadCards"
        >
          다시 시도
        </button>
      </section>

      <section
          v-else-if="!travelCardsStore.cards.length"
          class="state-card"
      >
        <b>조건에 맞는 카드가 없습니다.</b>
        <p>검색어나 필터 조건을 변경해 주세요.</p>

        <button
            type="button"
            @click="resetFilters"
        >
          전체 카드 보기
        </button>
      </section>

      <section
          v-else
          class="card-grid"
      >
        <article
            v-for="card in pagedCards"
            :key="card.id"
            class="card-tile"
        >
          <button
              type="button"
              class="card-tile-visual"
              @click="openCardDetail(card.id)"
          >
            <img
                v-if="getTravelCardImage(card.cardCompany)"
                :src="getTravelCardImage(card.cardCompany)"
                :alt="`${card.cardCompany} ${card.cardName}`"
            />
            <i
                v-else
                :style="{
                background: getCompanyColor(
                  card.cardCompany,
                ),
              }"
            >
              {{ getCompanyCode(card.cardCompany) }}
            </i>
            <small
                class="card-tile-status"
                :class="{ available: card.instantUse }"
            >
              {{ card.instantUse ? '바로 이용 가능' : '연계계좌 필요' }}
            </small>
          </button>

          <button
              type="button"
              class="card-tile-copy"
              @click="openCardDetail(card.id)"
          >
            <small class="company-name">{{ card.cardCompany }}</small>
            <b>{{ card.cardName }}</b>

            <span class="card-tags">
              <small>통화 {{ card.supportedCurrencyCount }}개</small>
              <small :class="{ muted: !card.transitCard }">{{ card.transitCard ? '교통카드' : '교통 미지원' }}</small>
            </span>
          </button>

          <div class="card-tile-footer">
            <div class="fee-info">
              <div>
                <small>환전 수수료</small>
                <strong>{{ card.exchangeFee || '카드별 확인' }}</strong>
              </div>
              <div>
                <small>해외 결제 수수료</small>
                <strong>{{ card.paymentFee || '카드별 확인' }}</strong>
              </div>
            </div>

            <button
                type="button"
                class="comparison-toggle"
                :class="{
                selected:
                  travelCardsStore.isCompared(card.id),
              }"
                @click="toggleComparison(card.id)"
            >
              {{
                travelCardsStore.isCompared(card.id)
                    ? '✓ 비교 담기 완료'
                    : '+ 비교 담기'
              }}
            </button>
          </div>
        </article>
      </section>

      <nav v-if="!travelCardsStore.listLoading && !travelCardsStore.errorMessage && totalPages > 1" class="pagination">
        <button
            type="button"
            class="pagination-arrow"
            aria-label="이전 페이지"
            :disabled="currentPage === 1"
            @click="goToPage(currentPage - 1)"
        >
          ‹
        </button>
        <button
            v-for="page in totalPages"
            :key="page"
            type="button"
            class="pagination-page"
            :class="{ active: currentPage === page }"
            @click="goToPage(page)"
        >
          {{ page }}
        </button>
        <button
            type="button"
            class="pagination-arrow"
            aria-label="다음 페이지"
            :disabled="currentPage === totalPages"
            @click="goToPage(currentPage + 1)"
        >
          ›
        </button>
      </nav>

      <button
          v-if="travelCardsStore.comparedCardCount"
          type="button"
          class="floating-compare"
          @click="openComparison"
      >
        <span><b>{{ travelCardsStore.comparedCardCount }}</b>개의 카드가 담겼어요</span>
        <strong>비교하기 ›</strong>
      </button>

      <BottomNav />
    </div>
  </main>
</template>

<style scoped>
.page{min-height:100vh;background:#e9eef7;color:#101b33}.shell{width:min(100%,430px);min-height:100vh;margin:auto;padding:42px 18px 118px;background:linear-gradient(180deg,#f4f7ff 0%,#edf3fc 100%)}
.page-header{display:grid;grid-template-columns:42px 1fr 42px;align-items:center;margin-bottom:17px}.back-button{display:grid;width:42px;height:42px;place-items:center;border:0;border-radius:14px;background:#fff;color:#16366f;font-size:28px;box-shadow:0 5px 16px #24487512}.page-header>div{text-align:center}.page-header small{color:#2f6fd8;font-size:8px;font-weight:900;letter-spacing:.13em}.page-header h1{margin-top:2px;font-size:18px;font-weight:900}
.finder-hero{position:relative;display:flex;min-height:184px;overflow:hidden;padding:23px 20px;border-radius:25px;background:linear-gradient(145deg,#0c2d72 0%,#174ca7 68%,#2f70d9 100%);color:#fff;box-shadow:0 16px 34px #123a8529}.hero-copy{position:relative;z-index:2}.hero-copy small{color:#ffd268;font-size:8px;font-weight:900;letter-spacing:.14em}.hero-copy h2{margin-top:10px;font-size:21px;font-weight:900;line-height:1.3;letter-spacing:-.04em}.hero-copy p{margin-top:9px;color:#c8d9f8;font-size:10px;line-height:1.55}.hero-orbit{position:absolute;top:-70px;right:-60px;width:190px;height:190px;border-radius:50%;background:#ffffff12}.hero-card{position:absolute;right:-21px;bottom:-18px;width:142px;height:91px;padding:17px;border:1px solid #ffffff3a;border-radius:18px;background:linear-gradient(145deg,#ffffff29,#8fb9ff25);box-shadow:0 14px 28px #071c4c50;transform:rotate(-8deg);backdrop-filter:blur(5px)}.hero-card i{display:block;width:24px;height:17px;border-radius:5px;background:linear-gradient(135deg,#ffd76c,#eca82d)}.hero-card b,.hero-card span{display:block}.hero-card b{margin-top:11px;font-family:'Space Mono',monospace;font-size:10px;letter-spacing:.12em}.hero-card span{margin-top:3px;color:#cbdcff;font-size:6px;letter-spacing:.16em}

.search-form {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 15px;
  padding: 7px 8px 7px 14px;
  border: 1px solid #d8e2f1;
  border-radius: 15px;
  background: #fff;
  color: #71819b;
  box-shadow:0 7px 20px #253f6b0b;
}
.search-form svg{width:19px;height:19px;flex:none}

.search-form input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: none;
  background: transparent;
  font-size: 11px;
}

.search-form button {
  padding: 9px 14px;
  border-radius: 10px;
  background: #173f8d;
  color: #fff;
  font-size: 10px;
  font-weight: 800;
}

.search-form button:disabled {
  background: #aeb9ca;
}

.filters{position:relative;z-index:20;display:grid;grid-template-columns:1fr 1fr;gap:9px;margin-top:12px}.filter-field{position:relative;display:grid;gap:5px}.filter-field:first-child{grid-column:1/3}.filter-field>span{padding-left:2px;color:#738095;font-size:10px;font-weight:700}.filter-field>button{display:flex;width:100%;min-height:40px;align-items:center;justify-content:space-between;padding:9px 11px;border:1px solid #d9e3f1;border-radius:11px;background:#fff;color:#26354b;font-size:10px;font-weight:700;text-align:left}.filter-field>button[aria-expanded=true]{border-color:#4b7fd0;box-shadow:0 0 0 3px #3478e516}.filter-field>button i{color:#6680a7;font-size:13px;font-style:normal}.filter-menu{position:absolute;top:calc(100% + 6px);left:0;z-index:50;width:100%;overflow:hidden;padding:5px;border:1px solid #d8e2f0;border-radius:12px;background:#fff;box-shadow:0 13px 30px #1737612b}.filter-menu.wide{display:grid;max-height:244px;grid-template-columns:1fr 1fr;overflow-y:auto}.filter-menu>button{display:flex;width:100%;align-items:center;justify-content:space-between;padding:10px;border-radius:8px;background:#fff;color:#34425a;font-size:10px;text-align:left}.filter-menu>button:hover,.filter-menu>button.selected{background:#edf4ff;color:#1e5fbd;font-weight:900}.filter-menu>button small{color:#8998ad;font-size:8px}.filter-menu>button.selected small{color:#4e7fc5}

.list-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 22px 2px 10px;
}
.list-heading small{color:#2e6fd9;font-size:10px;font-weight:900;letter-spacing:.13em}.list-heading h2{margin-top:3px;font-size:16px}.list-heading h2 b{color:#2d6dd5;font-size:11px}

.list-actions{display:flex;align-items:center;gap:7px}.list-actions .reset-button{color:#2d6bc8;font-size:9px;font-weight:700}.list-compare-button{display:flex;align-items:center;gap:6px;padding:8px 10px;border:1px solid #d2def0;border-radius:10px;background:#fff;color:#173f8d;font-size:9px;font-weight:900;box-shadow:0 4px 12px #253f6b0d}.list-compare-button b{display:grid;width:18px;height:18px;place-items:center;border-radius:6px;background:#ffce61;color:#143674;font-size:8px}

.card-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:2px;padding:4px 2px 10px}

.pagination{display:flex;align-items:center;justify-content:center;gap:6px;margin-top:6px}
.pagination-arrow{display:grid;width:30px;height:30px;place-items:center;border:1px solid #d9e3f1;border-radius:10px;background:#fff;color:#173f8d;font-size:16px}
.pagination-arrow:disabled{opacity:.4}
.pagination-page{display:grid;width:30px;height:30px;place-items:center;border-radius:10px;background:transparent;color:#66748c;font-size:12px;font-weight:700}
.pagination-page.active{background:#173f8d;color:#fff;font-weight:900}

.card-tile {
  overflow: hidden;
  border: 1px solid #e1e6ed;
  border-radius: 22px;
  background: #fff;
  box-shadow:0 8px 22px #263e650b;
}

.card-tile-visual {
  position: relative;
  display: flex;
  width: 100%;
  aspect-ratio: 0.66;
  align-items: center;
  justify-content: center;
  padding: 16px;
  border: none;
  background: linear-gradient(165deg, #eef3fd, #e3ebfa);
}

.card-tile-visual img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 12px 22px rgba(20, 42, 94, 0.18);
}

.card-tile-visual > i {
  display: grid;
  width: 64px;
  height: 64px;
  place-items: center;
  border-radius: 16px;
  color: #fff;
  font-size: 16px;
  font-style: normal;
  font-weight: 900;
}

.card-tile-status {
  position: absolute;
  left: 10px;
  bottom: 10px;
  padding: 5px 8px;
  border-radius: 8px;
  background: #ffffffe6;
  color: #78869a;
  font-size: 7.5px;
  font-weight: 800;
  white-space: nowrap;
  box-shadow: 0 4px 10px rgba(20, 42, 94, 0.12);
}

.card-tile-status.available {
  background: #e5f7f1;
  color: #078a68;
}

.card-tile-copy {
  display: block;
  width: 100%;
  padding: 13px 14px 0;
  text-align: left;
}

.card-tile-copy b,.card-tile-copy small{display:block}.card-tile-copy .company-name{margin:0;color:#3972c6;font-size:9px;font-weight:800}

.card-tile-copy b {
  overflow: hidden;
  margin-top:4px;font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-tile-copy .card-tags{display:flex;flex-wrap:wrap;gap:5px;margin-top:8px}.card-tile-copy .card-tags small{margin:0;padding:5px 7px;border-radius:99px;background:#edf4ff;color:#376aaa;font-size:8px}.card-tile-copy .card-tags small.muted{background:#f2f4f7;color:#8995a7}

.card-tile-footer {
  display: flex;
  flex-direction: column;
  gap: 9px;
  margin-top: 12px;
  padding: 12px 14px 14px;
  border-top: 1px solid #edf0f4;
  background: #fbfcfe;
}

.fee-info{display:flex;flex-direction:column;gap:8px}.fee-info>div{display:flex;flex-direction:column;gap:3px}.fee-info>div small{color:#8a97aa;font-size:8px;white-space:nowrap}.fee-info>div strong{color:#173c82;font-size:10px;line-height:1.3;word-break:keep-all}

.comparison-toggle {
  width: 100%;
  padding: 9px 10px;
  border-radius: 10px;
  background: #edf2fa;
  color: #68788e;
  font-size: 9px;
  text-align: center;
}

.comparison-toggle.selected {
  background: #dceaff;
  color: #176cdd;
  font-weight: 900;
}

.state-card {
  margin-top: 10px;
  padding: 48px 20px;
  border: 1px solid #e1e6ed;
  border-radius: 14px;
  background: #fff;
  text-align: center;
}

.state-card b {
  display: block;
  font-size: 11px;
}

.state-card p {
  margin-top: 7px;
  color: #8592a4;
  font-size: 9px;
  line-height: 1.5;
}

.state-card button {
  margin-top: 14px;
  padding: 9px 14px;
  border-radius: 9px;
  background: #173f8d;
  color: #fff;
  font-size: 8px;
  font-weight: 800;
}

.state-card.error {
  border-color: #f2caca;
  background: #fffafa;
}

.state-card.error b {
  color: #d44747;
}
.floating-compare{position:fixed;bottom:82px;left:50%;z-index:45;display:flex;width:min(calc(100% - 36px),394px);align-items:center;justify-content:space-between;padding:14px 16px;border:1px solid #ffffff30;border-radius:16px;background:#102f72;color:#fff;box-shadow:0 14px 30px #0d2d7150;transform:translateX(-50%)}.floating-compare span{color:#cbd9f4;font-size:10px}.floating-compare span b{display:inline-grid;width:21px;height:21px;margin-right:5px;place-items:center;border-radius:7px;background:#ffd36b;color:#133575}.floating-compare strong{font-size:11px}
@media(max-width:360px){.hero-card{right:-38px}.hero-copy h2{font-size:19px}}
</style>
