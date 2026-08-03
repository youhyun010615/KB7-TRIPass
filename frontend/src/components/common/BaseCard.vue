<script setup>
defineProps({
  padding: { type: String, default: 'md' }, // 'none' | 'sm' | 'md' | 'lg'
  shadow: { type: Boolean, default: false },
  border: { type: Boolean, default: true },
  clickable: { type: Boolean, default: false },
})
</script>

<template>
  <div
    :class="[
      'bg-white rounded-2xl',
      padding === 'none' ? '' : padding === 'sm' ? 'p-3' : padding === 'lg' ? 'p-5' : 'p-4',
      border ? 'border border-gray-100' : '',
      shadow ? 'shadow-sm' : '',
      clickable ? 'cursor-pointer active:scale-[0.98] transition-transform' : '',
    ]"
  >
    <slot />
  </div>
</template>
