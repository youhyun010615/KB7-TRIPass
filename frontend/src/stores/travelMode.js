import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useTravelModeStore = defineStore('travelMode', () => {
  // 'savings' | 'travel'
  const mode = ref(localStorage.getItem('travelMode') ?? 'savings')

  const isTravelMode = computed(() => mode.value === 'travel')
  const isSavingsMode = computed(() => mode.value === 'savings')

  function setMode(newMode) {
    mode.value = newMode
    localStorage.setItem('travelMode', newMode)
  }

  function toggleMode() {
    setMode(mode.value === 'savings' ? 'travel' : 'savings')
  }

  return { mode, isTravelMode, isSavingsMode, setMode, toggleMode }
})
