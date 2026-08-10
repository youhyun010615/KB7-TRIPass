<script setup>
import { ref, watch, nextTick } from 'vue';

const props = defineProps({
  modelValue: String,
  currencies: Array,
});

const emit = defineEmits(['update:modelValue']);
const navRef = ref(null);

const selectTab = (code) => {
  emit('update:modelValue', code);
  nextTick(() => {
    const el = navRef.value.querySelector(`[data-code="${code}"]`);
    if (el) {
      el.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
        inline: 'center',
      });
    }
  });
};

const scroll = (direction) => {
  if (navRef.value) {
    navRef.value.scrollBy({ left: direction * 150, behavior: 'smooth' });
  }
};
</script>

<template>
  <div class="nav-wrapper">
    <button class="arrow left" @click="scroll(-1)">‹</button>
    <div class="nav-container" ref="navRef">
      <button
        v-for="item in currencies"
        :key="item.code"
        :data-code="item.code"
        :class="{ active: modelValue === item.code }"
        @click="selectTab(item.code)"
      >
        <span class="fi" :class="item.flagClass"></span>
        {{ item.code }}
      </button>
    </div>
    <button class="arrow right" @click="scroll(1)">›</button>
  </div>
</template>

<style scoped>
.nav-wrapper {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 0;
  background: transparent;
  position: sticky;
  top: 0;
  z-index: 10;
}
.nav-container {
  display: flex;
  overflow-x: auto;
  scroll-behavior: smooth;
  gap: 8px;
  flex: 1;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.nav-container::-webkit-scrollbar {
  display: none;
}
.nav-container button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e2e7ed;
  font-size: 11px;
  white-space: nowrap;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}
.nav-container button span.fi {
  width: 14px;
  height: 10px;
  background-size: cover;
  border-radius: 1px;
  vertical-align: middle;
}
.nav-container button.active {
  background: #17387f;
  color: #fff;
  border-color: #17387f;
}
.arrow {
  background: #fff;
  border: 1px solid #e2e7ed;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
