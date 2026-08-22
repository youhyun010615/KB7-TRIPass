<script setup>
import { ref } from 'vue'

const toasts = ref([])

function show({ message, type = 'info', duration = 3000 }) {
  const id = Date.now()
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, duration)
}

defineExpose({ show })
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-4 left-1/2 -translate-x-1/2 z-50 flex flex-col gap-2 w-[calc(100%-2rem)] max-w-sm">
      <Transition
        v-for="toast in toasts"
        :key="toast.id"
        name="toast"
        appear
      >
        <div
          :class="[
            'flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg text-sm font-medium text-white',
            toast.type === 'success' ? 'bg-green-500' :
            toast.type === 'error'   ? 'bg-red-500' :
            toast.type === 'warning' ? 'bg-orange-400' :
            'bg-gray-800',
          ]"
        >
          <!-- success -->
          <svg v-if="toast.type === 'success'" class="shrink-0 w-5 h-5" viewBox="0 0 24 24" fill="none">
            <path d="M5 13L9 17L19 7" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <!-- error -->
          <svg v-else-if="toast.type === 'error'" class="shrink-0 w-5 h-5" viewBox="0 0 24 24" fill="none">
            <path d="M12 8V12M12 16H12.01" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="12" cy="12" r="9" stroke="white" stroke-width="2"/>
          </svg>
          <!-- warning -->
          <svg v-else-if="toast.type === 'warning'" class="shrink-0 w-5 h-5" viewBox="0 0 24 24" fill="none">
            <path d="M12 9V13M12 17H12.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"
              stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <!-- info -->
          <svg v-else class="shrink-0 w-5 h-5" viewBox="0 0 24 24" fill="none">
            <path d="M12 16V12M12 8H12.01" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="12" cy="12" r="9" stroke="white" stroke-width="2"/>
          </svg>

          <span>{{ toast.message }}</span>
        </div>
      </Transition>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
