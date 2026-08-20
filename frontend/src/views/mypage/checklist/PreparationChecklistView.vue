<template>
  <main class="page">
    <!-- 📌 1. 상단 고정 영역 (스크롤되지 않음) -->
    <div class="fixed-header">
      <header>
        <button type="button" @click="router.back()">‹</button>
        <h1>여행 준비 체크리스트</h1>
        <span />
      </header>

      <!-- D-Day 탭 네비게이션 -->
      <nav>
        <button
          v-for="key in ['d30', 'd7', 'd1']"
          :key="key"
          :class="{ active: stage === key }"
          @click="stage = key"
        >
          {{ dynamicStageMeta[key].label }}
        </button>
      </nav>

      <!-- 상단 패스/진행률 카드 -->
      <section class="pass">
        <small>TRIP PREP BOARDING PASS</small><i />
        <div>
          <h2>{{ dynamicStageMeta[stage].label }} 체크리스트</h2>
          <b>{{ progress.done }} / {{ progress.total }} 완료</b>
        </div>
        <div class="bar">
          <span :style="{ width: `${progress.percent}%` }" />
        </div>
        <footer>
          <strong>{{ progress.percent }}% 완료</strong>
          <em>{{ progress.total - progress.done }}개 항목 남음</em>
        </footer>
      </section>
    </div>

    <!-- 🎯 2. 하단 전체 통합 스크롤 영역 -->
    <div class="scroll-content">
      <!-- 📋 이번 단계 본래 항목 섹션 -->
      <h3>{{ dynamicStageMeta[stage].title }}</h3>
      <section class="items">
        <div v-if="store.loading" class="loading">불러오는 중...</div>
        <template v-else>
          <div
            v-for="item in regularItems"
            :key="item.id"
            class="item-card"
            :class="{ done: item.isCompleted }"
            @click="handleToggle(item)"
          >
            <span class="check-icon">{{ item.isCompleted ? '✓' : '' }}</span>

            <div class="content">
              <b class="item-title">
                <span
                  class="type-badge"
                  :class="item.isCustom ? 'custom' : 'default'"
                >
                  {{ item.isCustom ? '사용자' : '기본' }}
                </span>
                <span class="name">{{ item.itemName }}</span>
              </b>
            </div>

            <div class="actions">
              <button
                v-if="item.isCustom"
                type="button"
                class="del-btn"
                @click="(e) => handleDelete(e, item.id)"
              >
                삭제
              </button>
              <em class="status-tag">{{
                item.isCompleted ? '완료' : '미완료'
              }}</em>
            </div>
          </div>
        </template>
      </section>

      <!-- 📌 지난 단계 이월 항목 섹션 -->
      <template v-if="rolledItems.length">
        <h3 class="rolled-title">이전 단계에서 이월</h3>
        <section class="items rolled">
          <div
            v-for="item in rolledItems"
            :key="item.id"
            class="item-card"
            :class="{ done: item.isCompleted }"
            @click="handleToggle(item)"
          >
            <span class="check-icon">{{ item.isCompleted ? '✓' : '' }}</span>

            <div class="content">
              <b class="item-title">
                <span class="name">{{ item.itemName }}</span>
                <span class="type-badge rolled-badge">
                  {{ item.ddayStage }} 이월
                </span>
              </b>
            </div>

            <div class="actions">
              <button
                v-if="item.isCustom"
                type="button"
                class="del-btn"
                @click="(e) => handleDelete(e, item.id)"
              >
                삭제
              </button>
              <em class="status-tag">이월</em>
            </div>
          </div>
        </section>
      </template>

      <!-- ➕ 추가 박스 및 버튼 영역 -->
      <ChecklistAddBox
        v-if="adding"
        placeholder="추가할 여행 점검 항목을 입력하세요"
        @submit="handleAddSubmit"
        @cancel="adding = false"
      />
      <button v-else class="add-button" @click="adding = true">
        ＋ 체크리스트 추가
      </button>
    </div>
  </main>
  <BottomNav />
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import { useChecklistStore } from '@/stores/checklist';
import ChecklistAddBox from '@/components/checklist/ChecklistAddBox.vue';

const route = useRoute();
const router = useRouter();
const store = useChecklistStore();

// 1. Query 파라미터에서 tripId 및 탭(stage) 설정
const tripId = computed(() => Number(route.query.tripId || 1));
const stage = ref('d30'); // 'd30', 'd7', 'd1'
const adding = ref(false);
const newItem = ref('');

// 기준 D-Day 메타 데이터
const stageMeta = {
  d30: { label: 'D-30', title: 'D-30 준비 항목' },
  d7: { label: 'D-7', title: 'D-7 준비 항목' },
  d1: { label: 'D-1', title: '출국 직전 항목' },
};

/**
 * 여행 출발일과 현재 날짜를 비교하여 D-Day 단계 메타 데이터를 반환하는 함수
 */
function getDynamicStageMeta(departureDateStr) {
  if (!departureDateStr) return stageMeta;

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const formattedDate = String(departureDateStr).replace(/\./g, '-');
  const departureDate = new Date(formattedDate);
  departureDate.setHours(0, 0, 0, 0);

  const diffTime = departureDate.getTime() - today.getTime();
  const dDay = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

  const dynamicMeta = JSON.parse(JSON.stringify(stageMeta));

  if (dDay >= 8 && dDay <= 30) {
    dynamicMeta.d30.title = '이번 준비 항목';
  } else if (dDay >= 2 && dDay <= 7) {
    dynamicMeta.d7.title = '이번 준비 항목';
  } else if (dDay >= 0 && dDay <= 1) {
    dynamicMeta.d1.title = '이번 준비 항목';
  }

  return dynamicMeta;
}

const departureDate = ref('2026.08.15');

const dynamicStageMeta = computed(() =>
  getDynamicStageMeta(departureDate.value),
);

// 2. API 호출 함수
const fetchApiData = async () => {
  await store.loadChecklists(
    tripId.value,
    'PRE_TRAVEL',
    stage.value.toUpperCase(),
  );
};

onMounted(() => {
  fetchApiData();
});

watch(stage, () => {
  fetchApiData();
});

// 3. 백엔드 데이터 바인딩
const regularItems = computed(() => store.currentChecklists);
const rolledItems = computed(() => store.carriedOverChecklists);

// 4. 진행률 계산
const allItems = computed(() => [...regularItems.value, ...rolledItems.value]);
const progress = computed(() => {
  const total = allItems.value.length;
  const done = allItems.value.filter((item) => item.isCompleted).length;
  const percent = total ? Math.round((done / total) * 100) : 0;
  return { done, total, percent };
});

// 5. 토글 핸들러
const handleToggle = (item) => {
  store.toggleItem(tripId.value, item.id, item.isCompleted);
};

// 6. 새 항목 추가 핸들러
async function handleAddSubmit(text) {
  if (!text) return;

  const success = await store.addItem(tripId.value, {
    itemName: text,
    checklistType: 'PRE_TRAVEL',
    ddayStage: stage.value.toUpperCase(),
  });
  if (success) {
    adding.value = false;
  }
}
// 7. 항목 삭제 핸들러
const handleDelete = async (event, itemId) => {
  event.stopPropagation();

  if (confirm('해당 체크리스트 항목을 삭제하시겠습니까?')) {
    await store.removeItem(
      tripId.value,
      itemId,
      'PRE_TRAVEL',
      stage.value.toUpperCase(),
    );
  }
};
</script>

<style scoped>
.page {
  position: relative; /* 바텀네비 위치 기준점 */
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 0 18px;
  background: #f8f6f1;
  color: #111a2d;
  box-sizing: border-box;
  overflow: hidden; /* 페이지 전체 스크롤 차단 */
}

.fixed-header {
  flex-shrink: 0; /* 헤더 영역 높이 고정 */
}

/* 🎯 통으로 넘겨지는 스크롤 컨테이너 */
.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 95px; /* 바텀네비게이션 레이어 안 겹치게 여백 */

  /* 스크롤바 감추기 */
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.scroll-content::-webkit-scrollbar {
  display: none;
}

/* Header & Nav */
.page header {
  display: grid;
  height: 67px;
  grid-template-columns: 38px 1fr 38px;
  align-items: end;
  padding-bottom: 17px;
}
.page header button {
  font-size: 31px;
  text-align: left;
}
.page header h1 {
  text-align: center;
  font-size: 17px;
  font-weight: 900;
}
.page nav {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-bottom: 14px;
  border: 1px solid #e0e6ef;
  border-radius: 10px;
  background: #fff;
}
.page nav button {
  padding: 10px;
  color: #8a96a8;
  font-size: 10px;
  font-weight: 800;
}
.page nav .active {
  box-shadow: inset 0 -2px #0871ff;
  color: #0871ff;
}

/* Boarding Pass */
.pass {
  position: relative;
  padding: 19px;
  border-radius: 16px;
  background: linear-gradient(135deg, #15367d, #075fc6);
  color: #fff;
}
.pass:before,
.pass:after {
  position: absolute;
  top: 50%;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #f8f6f1;
  content: '';
}
.pass:before {
  left: -7px;
}
.pass:after {
  right: -7px;
}
.pass small {
  color: #bdd1f1;
  font-size: 8px;
  font-weight: 800;
}
.pass i {
  display: block;
  margin: 12px 0;
  border-top: 1px dashed #7fa7dc;
}
.pass > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pass h2 {
  font-size: 17px;
}
.pass > div b {
  font-size: 10px;
}
.pass .bar {
  height: 6px;
  margin-top: 17px;
  border-radius: 8px;
  background: #ffffff30;
}
.pass .bar span {
  display: block;
  height: 100%;
  border-radius: 8px;
  background: #ff9300;
  transition: width 0.3s ease;
}
.pass footer {
  display: flex;
  justify-content: space-between;
  margin-top: 9px;
}
.pass footer strong,
.pass footer em {
  font-size: 9px;
  font-style: normal;
}

/* List Titles */
.scroll-content h3 {
  margin: 20px 2px 10px;
  font-size: 14px;
}
.rolled-title {
  color: #d97900;
}

/* Items & Cards Layout */
.items {
  display: grid;
  gap: 9px;
}
.item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid #e2e7ef;
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 700;
  color: #1a202c;
  word-break: break-all;
}

.check-icon {
  display: grid;
  width: 24px;
  height: 24px;
  border: 1px solid #dce3ec;
  border-radius: 50%;
  place-items: center;
  color: #fff;
  font-size: 15px;
}

.content {
  flex: 1;
  min-width: 0;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 🗑️ 깔끔한 '삭제' 텍스트 버튼 */
.del-btn {
  background: none;
  border: none;
  color: #a0aec0;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
  transition: all 0.2s ease;
}
.del-btn:hover {
  color: #e53e3e;
  background-color: #fff5f5;
  text-decoration: underline;
}

.status-tag {
  padding: 5px 8px;
  border-radius: 10px;
  background: #fff0f1;
  color: #ef5e66;
  font-size: 7px;
  font-style: normal;
}

/* 🏷️ 작고 은은한 구분 뱃지 */
.type-badge {
  display: inline-block;
  padding: 1px 4px;
  margin-right: 2px;
  font-size: 9px;
  font-weight: 500;
  border-radius: 4px;
  white-space: nowrap;
  line-height: 1.2;
  opacity: 0.85;
}

.type-badge.default {
  background: #f1f5f9;
  color: #94a3b8;
}

.type-badge.custom {
  background: #fff7ed;
  color: #f97316;
}

.type-badge.rolled-badge {
  background: #fef3c7;
  color: #d97706;
}

/* Completed & Rolled Styles */
.item-card.done .check-icon {
  border-color: #16ae87;
  background: #16ae87;
}
.item-card.done .status-tag {
  background: #e8f8f3;
  color: #16a37f;
}

.items.rolled .item-card {
  border-color: #ffd99f;
  background: #fffaf0;
}
.items.rolled .status-tag {
  background: #fff0d8;
  color: #db820d;
}

/* Add Box & Button */
.add-button {
  width: 100%;
  margin-top: 14px;
  padding: 13px;
  border: 1px solid #1769d7;
  border-radius: 11px;
  background: #fff;
  color: #1769d7;
  font-size: 10px;
  font-weight: 900;
  cursor: pointer;
}
.add-box {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #2b78ed;
  border-radius: 13px;
  background: #fff;
}
.add-box textarea {
  width: 100%;
  height: 70px;
  padding: 10px;
  border: 0;
  resize: none;
  font-size: 10px;
  box-sizing: border-box;
  outline: none;
}
.add-box div {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.add-box button {
  padding: 11px;
  border: 0;
  border-radius: 9px;
  background: #0871ff;
  color: #fff;
  font-size: 10px;
  font-weight: 900;
  cursor: pointer;
}
.add-box button:last-child {
  background: #eef1f5;
  color: #657288;
}

.loading {
  padding: 20px;
  text-align: center;
  color: #8a96a8;
  font-size: 12px;
}
</style>
