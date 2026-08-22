<template>
  <section class="add-box">
    <textarea
      v-model="inputText"
      maxlength="50"
      :placeholder="placeholder"
      @keyup.enter.exact.prevent="handleSubmit"
    />
    <div>
      <button type="button" @click="handleSubmit">추가</button>
      <button type="button" @click="handleCancel">취소</button>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue';

const props = defineProps({
  placeholder: {
    type: String,
    default: '추가할 항목을 입력하세요',
  },
});

const emit = defineEmits(['submit', 'cancel']);

const inputText = ref('');

function handleSubmit() {
  if (!inputText.value.trim()) return;
  emit('submit', inputText.value.trim());
  inputText.value = ''; // 제출 후 입력창 초기화
}

function handleCancel() {
  inputText.value = '';
  emit('cancel');
}
</script>

<style scoped>
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
</style>