<script setup>
defineProps({
  message: { type: String, default: '데이터가 없습니다' },
  subMessage: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
})

const emit = defineEmits(['action'])
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-3 py-16 px-6 text-center">
    <slot name="icon">
      <svg width="56" height="56" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="10" stroke="#D1D5DB" stroke-width="1.5"/>
        <path d="M12 8V12M12 16H12.01" stroke="#D1D5DB" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
    </slot>

    <p class="text-base font-semibold text-gray-500">{{ message }}</p>
    <p v-if="subMessage" class="text-sm text-gray-400">{{ subMessage }}</p>

    <button
      v-if="actionLabel"
      class="mt-2 h-10 px-5 rounded-xl bg-blue-600 text-white text-sm font-semibold"
      @click="emit('action')"
    >
      {{ actionLabel }}
    </button>
  </div>
</template>
