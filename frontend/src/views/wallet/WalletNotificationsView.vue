<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from '@lucide/vue'
import BottomNav from '@/components/common/BottomNav.vue'
import { toDayLabel, toTimeLabel, useTripWalletStore } from '@/stores/tripWallet'

const router = useRouter()
const wallet = useTripWalletStore()
const notice = ref('')

function money(value) {
  return `${Number(value || 0).toLocaleString('ko-KR')}원`
}

onMounted(async () => {
  try {
    await wallet.loadAutoSavingLogs()
  } catch {
    notice.value = '자동 채우기 기록을 불러오지 못했어요.'
  }
})
</script>

<template>
  <main class="notifications-page">
    <header class="page-header">
      <button type="button" class="back-button" @click="router.back()"><ChevronLeft :size="22" /></button>
      <h1>자동 채우기 기록</h1>
    </header>

    <section class="notification-list">
      <article
        v-for="item in wallet.autoSavingLogs"
        :key="item.id"
        class="notification-row"
      >
        <span
          class="notification-icon"
          :style="item.isSuccess ? { background: '#e4f8f2', color: '#079b84' } : { background: '#ffece9', color: '#ef4444' }"
        >{{ item.isSuccess ? '✓' : '!' }}</span>
        <div class="notification-body">
          <b>{{ item.isSuccess ? '자동 채우기 성공' : '자동 채우기 실패' }}</b>
          <p>{{ money(item.amount) }}<template v-if="!item.isSuccess && item.reason"> · {{ item.reason }}</template></p>
          <small>{{ toDayLabel(item.executedAt) }} {{ toTimeLabel(item.executedAt) }}</small>
        </div>
      </article>
      <p v-if="notice" class="list-empty">{{ notice }}</p>
      <p v-else-if="!wallet.autoSavingLogs.length" class="list-empty">아직 자동 채우기 기록이 없어요.</p>
    </section>

    <BottomNav />
  </main>
</template>

<style scoped>
.notifications-page{max-width:430px;min-height:100vh;margin:0 auto;padding:44px 16px 96px;background:#eef2f8;color:#111827}.page-header{display:flex;align-items:center;gap:12px}.page-header button{display:grid;width:36px;height:36px;place-items:center;color:#0f172a}.page-header h1{flex:1;font-size:24px;font-weight:800;letter-spacing:-.04em}.notification-list{display:grid;gap:10px;margin-top:24px}.notification-row{display:flex;align-items:flex-start;gap:12px;padding:16px;border-radius:18px;background:#fff;border:1px solid #e4ebf5;text-align:left}.notification-icon{display:grid;flex:0 0 38px;width:38px;height:38px;place-items:center;border-radius:50%;font-size:16px;font-weight:800}.notification-body{min-width:0;flex:1}.notification-body b{display:block;font-size:14px;font-weight:800;color:#111827}.notification-body p{margin-top:5px;color:#5f6c82;font-size:12px;font-weight:500;line-height:1.45}.notification-body small{display:block;margin-top:7px;color:#9aa8bd;font-size:11px;font-weight:600}.list-empty{margin-top:18px;padding:30px 20px;border-radius:14px;background:#f7f9fd;color:#9aa8bd;font-size:12px;font-weight:600;text-align:center}
.notifications-page{word-break:keep-all}.notifications-page h1{text-wrap:balance}.notifications-page p{text-wrap:pretty}
.notifications-page{padding:42px 16px 88px;background:#f2f5fa}.page-header{gap:9px}.page-header button{width:32px;height:32px}.page-header h1{color:#29466f;font-size:20px;font-weight:700;letter-spacing:-.025em}.notification-list{gap:8px;margin-top:18px}.notification-row{gap:10px;padding:12px;border-radius:15px}.notification-icon{flex-basis:32px;width:32px;height:32px;font-size:13px}.notification-body b{font-size:12.5px}.notification-body p{margin-top:4px;font-size:10.5px}.notification-body small{margin-top:5px;font-size:9.5px}
</style>
