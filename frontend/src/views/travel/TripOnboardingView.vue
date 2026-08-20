<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTravelStore } from '@/stores/travel'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const router = useRouter()
const travelStore = useTravelStore()
const errorMessage = ref('')

async function startOnboarding() {
  errorMessage.value = ''
  try {
    const lifecycle = await travelStore.loadLifecycle()
    if (!lifecycle?.onboardingPending) {
      await router.replace('/')
      return
    }
    await travelStore.acknowledgeOnboarding()
    await router.replace({ name: 'TravelRegister', query: { onboarding: '1' } })
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '온보딩을 시작하지 못했어요.'
  }
}

onMounted(startOnboarding)
</script>

<template>
  <main class="onboarding-loader">
    <LoadingSpinner v-if="!errorMessage" />
    <template v-else>
      <strong>여행 준비 화면을 열지 못했어요</strong>
      <p>{{ errorMessage }}</p>
      <button type="button" @click="startOnboarding">다시 시도</button>
    </template>
  </main>
</template>

<style scoped>
.onboarding-loader{display:flex;min-height:100dvh;max-width:390px;margin:0 auto;align-items:center;justify-content:center;flex-direction:column;padding:28px;background:#eef2f8;color:#111827;text-align:center}.onboarding-loader strong{font-size:18px}.onboarding-loader p{margin-top:8px;color:#718096;font-size:13px}.onboarding-loader button{margin-top:18px;padding:12px 22px;border-radius:12px;background:#174b9c;color:#fff;font-weight:800}
</style>
