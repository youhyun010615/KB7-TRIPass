<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
import { useChecklistStore } from '@/stores/checklist';

const route = useRoute();
const router = useRouter();
const store = useChecklistStore();
const tripId = computed(() => Number(route.query.tripId || 1));
const data = computed(() => store.getTrip(tripId.value));
const progress = computed(() => store.progress(data.value.returns));
const adding = ref(false);
const newItem = ref('');
function submit() {
  if (store.add(tripId.value, 'return', null, newItem.value)) {
    newItem.value = '';
    adding.value = false;
  }
}
</script>

<template>
  <main class="page">
    <header>
      <button @click="router.back()">‹</button>
      <h1>귀국 체크리스트</h1>
      <span />
    </header>
    <section class="pass">
      <small>RETURN CHECK BOARDING PASS</small><i />
      <div>
        <h2>귀국 {{ data.trip.returnDate }}</h2>
        <b>{{ progress.done }} / {{ progress.total }} 완료</b>
      </div>
      <div class="bar"><span :style="{ width: `${progress.percent}%` }" /></div>
      <footer>
        <strong>{{ progress.percent }}% 완료</strong
        ><em>{{ data.trip.flightTime }} · {{ data.trip.flight }}</em>
      </footer>
    </section>
    <h3>귀국 점검 체크리스트</h3>
    <section class="items">
      <button
        v-for="item in data.returns"
        :key="item.id"
        :class="{ done: item.done }"
        @click="store.toggle(tripId, 'return', item.id)"
      >
        <span>{{ item.done ? '✓' : '' }}</span>
        <div>
          <b>{{ item.text }}</b
          ><small>{{ item.note }}</small>
        </div>
        <em>{{ item.done ? '완료' : '미완료' }}</em>
      </button>
    </section>
    <section v-if="adding" class="add-box">
      <textarea
        v-model="newItem"
        maxlength="60"
        placeholder="추가할 귀국 점검 항목을 입력하세요"
      />
      <div>
        <button @click="submit">추가</button
        ><button
          @click="
            adding = false;
            newItem = '';
          "
        >
          취소
        </button>
      </div>
    </section>
    <button v-else class="add-button" @click="adding = true">
      ＋ 체크리스트 추가
    </button>
    <BottomNav />
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 0 18px 95px;
  background: #f8f6f1;
  color: #111a2d;
}
.page > header {
  display: grid;
  height: 92px;
  grid-template-columns: 38px 1fr 38px;
  align-items: end;
  padding-bottom: 17px;
}
.page > header button {
  font-size: 31px;
  text-align: left;
}
.page > header h1 {
  text-align: center;
  font-size: 17px;
  font-weight: 900;
}
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
.page > h3 {
  margin: 20px 2px 10px;
  font-size: 14px;
}
.items {
  display: grid;
  gap: 9px;
}
.items button {
  display: grid;
  grid-template-columns: 34px 1fr auto;
  gap: 11px;
  align-items: center;
  padding: 13px;
  border: 1px solid #e2e7ef;
  border-radius: 13px;
  background: #fff;
  text-align: left;
}
.items button > span {
  display: grid;
  width: 32px;
  height: 32px;
  border: 1px solid #dce3ec;
  border-radius: 50%;
  place-items: center;
  color: #fff;
  font-size: 15px;
}
.items button div b,
.items button div small {
  display: block;
}
.items button div b {
  font-size: 11px;
}
.items button div small {
  margin-top: 4px;
  color: #8a96a6;
  font-size: 8px;
}
.items button em {
  padding: 5px 8px;
  border-radius: 10px;
  background: #fff0f1;
  color: #ef5e66;
  font-size: 7px;
  font-style: normal;
}
.items button.done > span {
  border-color: #16ae87;
  background: #16ae87;
}
.items button.done em {
  background: #e8f8f3;
  color: #16a37f;
}
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
  resize: none;
  font-size: 10px;
}
.add-box div {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.add-box button {
  padding: 11px;
  border-radius: 9px;
  background: #0871ff;
  color: #fff;
  font-size: 10px;
  font-weight: 900;
}
.add-box button:last-child {
  background: #eef1f5;
  color: #657288;
}
</style>
