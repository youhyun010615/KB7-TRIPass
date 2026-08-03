<script setup>
defineProps({
  title: { type: String, default: '' },
  showBack: { type: Boolean, default: true },
})

const emit = defineEmits(['back'])
</script>

<template>
  <header class="flex items-center justify-between px-4 h-14 bg-white border-b border-gray-100">
    <button
      v-if="showBack"
      class="w-10 h-10 flex items-center justify-center"
      @click="emit('back')"
    >
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
        <path d="M15 18L9 12L15 6" stroke="#222" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>
    <div v-else class="w-10" />

    <h1 class="text-base font-semibold text-gray-900">{{ title }}</h1>

    <slot name="actions">
      <div class="w-10" />
    </slot>
  </header>
</template>
