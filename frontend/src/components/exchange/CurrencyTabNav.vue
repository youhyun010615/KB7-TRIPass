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
  gap: 7px;
  padding: 8px 0 11px;
  background: transparent;
  position: sticky;
  top: 0;
  z-index: 10;
}
.nav-container {
  display: flex;
  overflow-x: auto;
  scroll-behavior: smooth;
  gap: 7px;
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
  flex: 0 0 auto;
  gap: 6px;
  min-height: 38px;
  padding: 7px 13px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid #dce3ee;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}
.nav-container button span.fi {
  width: 15px;
  height: 11px;
  background-size: cover;
  border-radius: 1px;
  vertical-align: middle;
}
.nav-container button.active {
  background: #123478;
  color: #fff;
  border-color: #123478;
  box-shadow: 0 6px 12px rgba(18, 52, 120, .16);
}
.arrow {
  background: #fff;
  border: 1px solid #dce3ee;
  border-radius: 50%;
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  color: #6f7d94;
  font-size: 19px;
  box-shadow: 0 3px 8px rgba(30, 49, 79, .04);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
