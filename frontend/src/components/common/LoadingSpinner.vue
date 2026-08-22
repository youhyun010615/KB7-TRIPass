<script setup>
defineProps({
  size: { type: String, default: 'md' }, // 'sm' | 'md' | 'lg'
  fullScreen: { type: Boolean, default: false },
})
</script>

<template>
  <div
    :class="[
      fullScreen
        ? 'fixed inset-0 z-50 flex items-center justify-center bg-white/80'
        : 'flex items-center justify-center',
    ]"
  >
    <svg
      :class="[
        'animate-spin text-blue-600',
        size === 'sm' ? 'w-5 h-5' : size === 'lg' ? 'w-10 h-10' : 'w-7 h-7',
      ]"
      viewBox="0 0 24 24"
      fill="none"
    >
      <circle class="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3"/>
      <path class="opacity-80" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"/>
    </svg>
  </div>
</template>
