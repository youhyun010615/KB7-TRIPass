<script setup>
defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  type: { type: String, default: 'text' },
  error: { type: String, default: '' },
  hint: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <label v-if="label" class="text-sm font-medium text-gray-700">{{ label }}</label>

    <div class="relative">
      <input
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :class="[
          'w-full h-12 px-4 rounded-xl text-sm bg-gray-50 border outline-none transition',
          error
            ? 'border-red-400 focus:border-red-500 focus:ring-2 focus:ring-red-100'
            : 'border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100',
          disabled ? 'opacity-50 cursor-not-allowed' : '',
          readonly ? 'cursor-default' : '',
        ]"
        @input="emit('update:modelValue', $event.target.value)"
      />
      <slot name="suffix" />
    </div>

    <p v-if="error" class="text-xs text-red-500">{{ error }}</p>
    <p v-else-if="hint" class="text-xs text-gray-400">{{ hint }}</p>
  </div>
</template>
