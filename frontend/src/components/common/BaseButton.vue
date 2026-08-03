<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary',
    // 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
  },
  size: {
    type: String,
    default: 'md',
    // 'sm' | 'md' | 'lg'
  },
  disabled: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  full: { type: Boolean, default: false },
})
</script>

<template>
  <button
    :disabled="disabled || loading"
    :class="[
      'inline-flex items-center justify-center font-semibold rounded-xl transition-all',
      full ? 'w-full' : '',
      size === 'sm' ? 'h-9 px-4 text-sm' : size === 'lg' ? 'h-14 px-6 text-base' : 'h-12 px-5 text-sm',
      variant === 'primary' ? 'bg-blue-600 text-white active:bg-blue-700 disabled:bg-gray-200 disabled:text-gray-400' :
      variant === 'secondary' ? 'bg-blue-50 text-blue-600 active:bg-blue-100 disabled:opacity-40' :
      variant === 'outline' ? 'border border-gray-300 text-gray-700 bg-white active:bg-gray-50 disabled:opacity-40' :
      variant === 'ghost' ? 'text-gray-600 bg-transparent active:bg-gray-100 disabled:opacity-40' :
      variant === 'danger' ? 'bg-red-500 text-white active:bg-red-600 disabled:opacity-40' : '',
    ]"
  >
    <svg v-if="loading" class="animate-spin mr-2 h-4 w-4" viewBox="0 0 24 24" fill="none">
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
    </svg>
    <slot />
  </button>
</template>
