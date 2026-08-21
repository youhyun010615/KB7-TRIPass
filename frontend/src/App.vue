<script setup>
import AppLoadingOverlay from '@/components/common/AppLoadingOverlay.vue';
</script>

<template>
  <div class="min-h-screen bg-gray-100 flex justify-center">
    <div class="w-full max-w-[390px] min-h-screen bg-white relative overflow-hidden shadow-xl">
      <RouterView />
      <AppLoadingOverlay />
    </div>
  </div>
</template>
