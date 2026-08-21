<template>
  <main class="page">
    <!-- 📌 1. 상단 고정 영역 -->
    <div class="fixed-header">
      <header>
        <button type="button" @click="router.back()">‹</button>
        <h1>귀국 체크리스트</h1>
        <span />
      </header>

      <section class="pass">
        <small>RETURN CHECK BOARDING PASS</small><i />
        <div>
          <h2>귀국 체크리스트</h2>
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

    <!-- 🎯 2. 하단 스크롤 영역 -->
    <div class="scroll-content">
      <h3>귀국 점검 체크리스트</h3>

      <section class="items">
        <div v-if="store.loading" class="loading">불러오는 중...</div>
        <template v-else>
          <div
            v-for="item in returnItems"
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

            <!-- 액션 영역: 삭제 버튼 + 완료 상태 -->
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

      <!-- ➕ 추가 박스 및 버튼 영역 -->
      <ChecklistAddBox
        v-if="adding"
        placeholder="추가할 귀국 점검 항목을 입력하세요"
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
import { computed, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import ChecklistAddBox from '@/components/checklist/ChecklistAddBox.vue';
import { useChecklistStore } from '@/stores/checklist';

const route = useRoute();
const router = useRouter();
const store = useChecklistStore();

// tripId 고정 및 query 파라미터 처리
const tripId = computed(() => Number(route.query.tripId || 1));

const adding = ref(false);
const newItem = ref('');

// 1. RETURN 타입 귀국 체크리스트 목록 API 조회
const fetchApiData = async () => {
  await store.loadChecklists(tripId.value, 'RETURN');
};

onMounted(() => {
  fetchApiData();
});

// 2. 백엔드/스토어 데이터 바인딩
const returnItems = computed(() => store.currentChecklists);

// 3. 진행률 계산
const progress = computed(() => {
  const total = returnItems.value.length;
  const done = returnItems.value.filter((item) => item.isCompleted).length;
  const percent = total ? Math.round((done / total) * 100) : 0;
  return { done, total, percent };
});

// 4. 완료 상태 토글
const handleToggle = (item) => {
  store.toggleItem(tripId.value, item.id, item.isCompleted);
};

// 5. 새 귀국 항목 추가
async function handleAddSubmit(text) {
  if (!text) return;

  const success = await store.addItem(tripId.value, {
    itemName: text,
    checklistType: 'RETURN',
    ddayStage: null, 
  });

  if (success) {
    adding.value = false;
  }
}

// 6. 귀국 항목 삭제
const handleDelete = async (event, itemId) => {
  event.stopPropagation();

  if (confirm('해당 귀국 점검 항목을 삭제하시겠습니까?')) {
    await store.removeItem(tripId.value, itemId, 'RETURN');
  }
};
</script>

<style scoped>
/* 📌 Layout & Scroll 구조 설정 */
.page {
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 0 18px;
  background: #f8f6f1;
  color: #111a2d;
  box-sizing: border-box;
  overflow: hidden;
}

.fixed-header {
  flex-shrink: 0;
}

.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 95px;

  -ms-overflow-style: none;
  scrollbar-width: none;
}

.scroll-content::-webkit-scrollbar {
  display: none;
}

/* Header */
.page header {
  display: grid;
  height: 67px;
  grid-template-columns: 38px 1fr 38px;
  align-items: end;
  padding-bottom: 17px;
}
.page header button {
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
  border: none;
  cursor: pointer;
}
.page header h1 {
  text-align: center;
  font-size: 17px;
  font-weight: 900;
}

/* Boarding Pass (보라색 테마) */
.pass {
  position: relative;
  padding: 19px;
  border-radius: 16px;
  background: linear-gradient(135deg, #63308d, #934fba);
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
  color: #e1c8ee;
  font-size: 8px;
  font-weight: 800;
}
.pass i {
  display: block;
  margin: 12px 0;
  border-top: 1px dashed #c79bdd;
}
.pass > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pass h2 {
  font-size: 16px;
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
  background: #dba8f4;
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

/* Title */
.scroll-content h3 {
  margin: 20px 2px 10px;
  font-size: 14px;
}

/* Items & Card Styles */
.items {
  display: grid;
  gap: 9px;
}

.item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 13px 16px;
  border: 1px solid #e2e7ef;
  border-radius: 13px;
  background: #fff;
  cursor: pointer;
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
  flex-shrink: 0;
}

.content {
  flex: 1;
  min-width: 0;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #111a2d;
  word-break: break-all;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 🗑️ 삭제 텍스트 버튼 */
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

/* 🏷️ 작고 은은한 구분 미니 뱃지 */
.type-badge {
  display: inline-block;
  padding: 1px 4px;
  font-size: 8px;
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
  background: #f3e8ff;
  color: #9333ea;
}

/* Completed Status Styles */
.item-card.done .check-icon {
  border-color: #16ae87;
  background: #16ae87;
}

.item-card.done .status-tag {
  background: #e8f8f3;
  color: #16a37f;
}

/* Add Box & Button */
.add-button {
  width: 100%;
  margin-top: 12px;
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
  margin-top: 12px;
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
  outline: none; /* 검은 테두리 제거 */
  resize: none;
  font-size: 10px;
  box-sizing: border-box;
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
<style scoped>
.page{background:#f3f6fc}.pass{background:linear-gradient(145deg,#2662ea,#173f8d);box-shadow:0 14px 30px rgba(23,63,141,.18)}.pass:before,.pass:after{background:#f3f6fc}.pass small,.pass footer em{color:#cbd9f7}.pass i{border-color:#8ba9df}.pass .bar span{background:linear-gradient(90deg,#ffd45e,#ffbe3d)}.item-card{border-color:#dfe7f4;background:#fff;box-shadow:0 7px 18px rgba(23,63,141,.05)}.check-icon{border-color:#a9bfe8}.item-card.done .check-icon{border-color:#2662ea;background:#2662ea}.add-button{border-color:#bfd0f1;color:#2662ea;background:#f9fbff}
.pass{padding:16px;border-radius:16px;box-shadow:0 9px 22px rgba(23,63,141,.14)}.pass h2{font-size:15px}.pass .bar{margin-top:13px}.scroll-content h3{margin:16px 2px 8px;font-size:13px}.items{gap:7px}.item-card{gap:8px;padding:11px 12px;border-radius:12px;box-shadow:0 5px 14px rgba(23,63,141,.045)}.item-title{font-size:11px}.check-icon{width:22px;height:22px;font-size:13px}.actions{gap:5px}.status-tag{padding:4px 7px}.add-button{margin-top:10px;padding:11px;border-radius:10px}.add-box{margin-top:10px;padding:10px;border-radius:11px}
</style>
