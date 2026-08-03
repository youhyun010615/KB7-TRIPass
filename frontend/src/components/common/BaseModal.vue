<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  closeOnBackdrop: { type: Boolean, default: true },
})

const emit = defineEmits(['update:modelValue'])

function close() {
  emit('update:modelValue', false)
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-end justify-center"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black/40"
          @click="closeOnBackdrop && close()"
        />

        <!-- Sheet -->
        <div class="relative w-full max-w-md bg-white rounded-t-2xl pb-safe">
          <!-- Handle -->
          <div class="flex justify-center pt-3 pb-1">
            <div class="w-10 h-1 rounded-full bg-gray-200" />
          </div>

          <!-- Title -->
          <div v-if="title || $slots.header" class="px-5 py-4 border-b border-gray-100">
            <slot name="header">
              <h2 class="text-base font-semibold text-gray-900">{{ title }}</h2>
            </slot>
          </div>

          <!-- Content -->
          <div class="px-5 py-4">
            <slot />
          </div>

          <!-- Footer -->
          <div v-if="$slots.footer" class="px-5 pb-4">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-active .relative,
.modal-leave-active .relative {
  transition: transform 0.25s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .relative {
  transform: translateY(100%);
}
</style>
