<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import { useMypageStore } from '@/stores/mypage';

const router = useRouter();
const store = useMypageStore();

const rows = [
  { key: 'travelScheduleEnabled', label: '여행 일정 알림', sub: '일정 1시간 전 자동 안내' },
  { key: 'exchangeRateEnabled', label: '환율 및 환전 알림', sub: '목표 환율 도달 및 환전 안내' },
  { key: 'checklistEnabled', label: '체크리스트 알림', sub: '단계별 준비 항목 안내' },
  { key: 'travelReportEnabled', label: '여행 리포트 알림', sub: '여행 전·후 리포트 생성 안내' },
];

onMounted(() => {
  store.fetchSettings();
});
</script>

<template>
  <main class="page">
    <header>
      <button @click="router.back()">‹</button>
      <h1>알림 설정</h1>
      <span />
    </header>
    
    <section class="master">
      <span>
        <b>전체 알림</b>
        <small>TRIPass의 모든 알림을 받아보세요.</small>
      </span>
      <button
        :class="{ on: store.settings.allEnabled }"
        @click="store.toggleSetting('allEnabled')"
      >
        <i />
      </button>
    </section>

    <h2>알림 유형</h2>
    <section class="list">
      <div v-for="row in rows" :key="row.key">
        <span>
          <b>{{ row.label }}</b>
          <small>{{ row.sub }}</small>
        </span>
        <button
          :class="{ on: store.settings.allEnabled || store.settings[row.key] }"
          :disabled="store.settings.allEnabled"
          @click="store.toggleSetting(row.key)"
        >
          <i />
        </button>
      </div>
    </section>
    
    <aside>
      필수 보안 및 서비스 운영 알림은 설정과 관계없이 제공될 수 있어요.
    </aside>
    
    <BottomNav />
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 0 20px 105px;
  background: #f8f6f1;
  color: #10192d;
}
.page > header {
  display: grid;
  height: 90px;
  grid-template-columns: 40px 1fr 40px;
  align-items: end;
  padding-bottom: 16px;
}
.page > header button {
  font-size: 28px;
  text-align: left;
}
.page h1 {
  text-align: center;
  font-size: 21px;
  font-weight: 900;
}
.master,
.list {
  border: 1px solid #e0e5ec;
  border-radius: 20px;
  background: #fff;
}
.master {
  display: flex;
  justify-content: space-between;
  padding: 19px;
}
.master span > *,
.list span > * {
  display: block;
}
.master b,
.list b {
  font-size: 13px;
}
.master small,
.list small {
  margin-top: 5px;
  color: #8592a5;
  font-size: 9px;
}
.page > h2 {
  margin: 20px 3px 10px;
  font-size: 15px;
  font-weight: 900;
}
.list {
  overflow: hidden;
}
.list > div {
  display: flex;
  justify-content: space-between;
  padding: 17px;
  border-bottom: 1px solid #edf0f3;
}
.list > div:last-child {
  border: 0;
}
.master button,
.list button {
  position: relative;
  width: 44px;
  height: 25px;
  border-radius: 20px;
  background: #d4dae3;
  transition: 0.2s;
}
.master button i,
.list button i {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  background: #fff;
  transition: 0.2s;
}
.master button.on,
.list button.on {
  background: #176ff2;
}
.master button.on i,
.list button.on i {
  left: 23px;
}
.list button:disabled {
  opacity: 0.45;
}
aside {
  margin-top: 14px;
  padding: 15px;
  border-radius: 14px;
  background: #eef4ff;
  color: #657793;
  font-size: 9px;
  line-height: 1.5;
}
</style>
