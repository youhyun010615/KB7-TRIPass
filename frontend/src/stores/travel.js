import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useTravelStore = defineStore('travel', () => {
  const isTravelMode = ref(false)
  const selectedCountry = ref(null)
  const travelBudget = ref(null)

  function enterTravelMode(country, budget) {
    isTravelMode.value = true
    selectedCountry.value = country
    travelBudget.value = budget
  }

  function exitTravelMode() {
    isTravelMode.value = false
    selectedCountry.value = null
    travelBudget.value = null
  }

  return { isTravelMode, selectedCountry, travelBudget, enterTravelMode, exitTravelMode }
})
