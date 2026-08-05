<script setup>
import { useRouter } from 'vue-router'; import { useMypageStore } from '@/stores/mypage'
const router=useRouter(), store=useMypageStore()
</script>
<template><button class="bell" type="button" aria-label="알림함" @click="router.push('/notifications')"><svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M10 21h4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg><i v-if="store.unreadCount">{{ store.unreadCount > 9 ? '9+' : store.unreadCount }}</i></button></template>
<style scoped>.bell{position:relative;display:grid;width:34px;height:34px;flex:none;place-items:center;border:1px solid #e0e6ef;border-radius:50%;background:#fff;color:#173d87;box-shadow:0 3px 9px #172d5012}.bell i{position:absolute;top:-4px;right:-4px;display:grid;min-width:16px;height:16px;padding:0 3px;place-items:center;border:2px solid #fff;border-radius:9px;background:#ff6b35;color:#fff;font-size:7px;font-style:normal;font-weight:900}</style>
