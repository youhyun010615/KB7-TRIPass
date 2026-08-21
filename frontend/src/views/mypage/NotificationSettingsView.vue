<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useMypageStore } from '@/stores/mypage'
import notificationIcon from '@/assets/icons/mingcute_notification-fill.svg'

const router = useRouter()
const mypageStore = useMypageStore()

const notificationStartTime = ref(localStorage.getItem('tripass-notification-start') || '09:00')
const notificationEndTime = ref(localStorage.getItem('tripass-notification-end') || '22:00')

const notificationRows = [
  { key: 'travelScheduleEnabled', label: '여행 일정', sub: '출국 D-day와 예약 일정' },
  { key: 'exchangeRateEnabled', label: '환율 및 환전', sub: '목표 환율 도달 시' },
  { key: 'checklistEnabled', label: '체크리스트', sub: '준비물·서류 리마인드' },
  { key: 'travelReportEnabled', label: '여행 리포트', sub: '월간 지출 요약' },
]

watch([notificationStartTime, notificationEndTime], ([start, end]) => {
  localStorage.setItem('tripass-notification-start', start)
  localStorage.setItem('tripass-notification-end', end)
})

onMounted(() => mypageStore.fetchSettings())
</script>

<template>
  <main class="settings-page">
    <header class="page-header">
      <button type="button" aria-label="뒤로 가기" @click="router.back()">‹</button>
      <div>
        <small>MY NOTIFICATION</small>
        <h1>알림 설정</h1>
      </div>
      <span aria-hidden="true"></span>
    </header>

    <section class="intro-card">
      <span class="bell-icon" aria-hidden="true">
        <span class="bell-wave bell-wave-one"></span>
        <span class="bell-wave bell-wave-two"></span>
        <img :src="notificationIcon" alt="" />
      </span>
      <div><b>필요한 알림만 받아보세요</b><p>여행 준비부터 환율과 리포트까지 원하는 알림을 관리할 수 있어요.</p></div>
    </section>

    <section class="setting-section">
      <div class="section-heading">
        <div><h2>알림 수신</h2><p>받고 싶은 알림을 선택해 주세요.</p></div>
        <div class="all-toggle">
          <span>전체 알림</span>
          <button
            type="button"
            class="toggle"
            :class="{ active: mypageStore.settings.allEnabled }"
            :aria-pressed="mypageStore.settings.allEnabled"
            @click="mypageStore.toggleSetting('allEnabled')"
          ><i /></button>
        </div>
      </div>

      <div class="setting-list">
        <div v-for="row in notificationRows" :key="row.key" class="setting-row">
          <div><b>{{ row.label }}</b><small>{{ row.sub }}</small></div>
          <button
            type="button"
            class="toggle"
            :class="{ active: mypageStore.settings.allEnabled || mypageStore.settings[row.key] }"
            :disabled="mypageStore.settings.allEnabled"
            :aria-pressed="mypageStore.settings.allEnabled || mypageStore.settings[row.key]"
            :aria-label="row.label"
            @click="mypageStore.toggleSetting(row.key)"
          ><i /></button>
        </div>
      </div>
    </section>

    <section class="time-card">
      <div class="time-heading">
        <div><h2>알림 수신 시간</h2><p>설정한 시간 안에서 알림을 받아요.</p></div>
        <span>TIME</span>
      </div>
      <div class="time-fields">
        <label><span>시작</span><input v-model="notificationStartTime" type="time" aria-label="알림 시작 시간"></label>
        <i>–</i>
        <label><span>종료</span><input v-model="notificationEndTime" type="time" aria-label="알림 종료 시간"></label>
      </div>
      <p class="time-note">수신 시간 외에 발생한 알림은 설정한 시작 시간 이후에 확인할 수 있어요.</p>
    </section>
  </main>
</template>

<style scoped>
.settings-page{width:min(100%,390px);min-height:100dvh;margin:0 auto;padding:0 16px 36px;background:#eef2f8;color:#111a2d;word-break:keep-all}.page-header{display:grid;height:76px;grid-template-columns:42px 1fr 42px;align-items:center}.page-header>button{display:grid;width:38px;height:38px;place-items:center;border-radius:13px;background:#fff;color:#173f8d;font-size:26px;font-weight:700;box-shadow:0 5px 16px rgba(36,72,117,.07)}.page-header div{text-align:center}.page-header small{display:block;color:#2f6fed;font-family:'Space Mono',monospace;font-size:8px;font-weight:900;letter-spacing:.14em}.page-header h1{margin-top:3px;font-size:19px;font-weight:950}.intro-card{display:flex;align-items:center;gap:13px;padding:17px;border:1px solid #cadcf8;border-radius:19px;background:linear-gradient(135deg,#f9fbff,#e7f0ff);box-shadow:0 8px 22px rgba(31,72,137,.07)}.bell-icon{position:relative;display:grid;flex:0 0 42px;width:42px;height:42px;place-items:center}.bell-icon img{position:relative;z-index:2;width:25px;height:25px;animation:settings-bell-ring 2.4s ease-in-out infinite;transform-origin:50% 12%}.bell-wave{position:absolute;inset:2px;border:1px solid rgba(47,112,240,.28);border-radius:50%;animation:settings-bell-wave 2.4s ease-out infinite}.bell-wave-two{animation-delay:1.2s}.intro-card b{font-size:13px;font-weight:900}.intro-card p{margin-top:4px;color:#78869b;font-size:10px;line-height:1.5}.setting-section{margin-top:18px}.section-heading,.time-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;padding:0 2px 10px}.section-heading h2,.time-heading h2{font-size:15px;font-weight:950}.section-heading p,.time-heading p{margin-top:3px;color:#929eb0;font-size:9.5px}.all-toggle{display:flex;align-items:center;gap:7px;padding-top:3px}.all-toggle>span{color:#738097;font-size:10px;font-weight:800}.setting-list{overflow:hidden;border-radius:20px;background:#fff;box-shadow:0 4px 14px rgba(16,25,43,.07)}.setting-row{display:flex;align-items:center;gap:12px;padding:15px 17px;border-bottom:1px solid #f1f3f8}.setting-row:last-child{border-bottom:0}.setting-row>div{flex:1}.setting-row b,.setting-row small{display:block}.setting-row b{font-size:13px;font-weight:900}.setting-row small{margin-top:3px;color:#98a2b3;font-size:10px}.toggle{display:flex;flex:0 0 42px;width:42px;height:24px;align-items:center;padding:3px;border-radius:999px;background:#dde2ec;transition:.2s}.toggle i{width:18px;height:18px;border-radius:50%;background:#fff;box-shadow:0 1px 4px rgba(15,23,42,.18);transition:.2s}.toggle.active{justify-content:flex-end;background:#2f6fed}.toggle:disabled{cursor:default}.time-card{margin-top:18px;padding:17px;border:1px solid #dce6f3;border-radius:20px;background:#fff;box-shadow:0 4px 14px rgba(16,25,43,.07)}.time-heading{padding:0}.time-heading>span{padding:4px 7px;border-radius:99px;background:#edf3ff;color:#3970d0;font-family:'Space Mono',monospace;font-size:7px;font-weight:900;letter-spacing:.08em}.time-fields{display:grid;grid-template-columns:minmax(0,1fr) 12px minmax(0,1fr);align-items:end;gap:7px;margin-top:14px}.time-fields label{padding:10px 11px;border:1px solid #e1e8f2;border-radius:13px;background:#f7f9fc}.time-fields label>span{display:block;margin-bottom:4px;color:#8b98ab;font-size:8px;font-weight:800}.time-fields input{width:100%;border:0;outline:0;background:transparent;color:#173f8d;font-size:13px;font-weight:900}.time-fields>i{padding-bottom:13px;color:#a5afbd;font-style:normal;text-align:center}.time-note{margin-top:12px;color:#9aa5b6;font-size:9px;line-height:1.5}
@keyframes settings-bell-ring{0%,42%,100%{transform:rotate(0)}48%{transform:rotate(11deg)}54%{transform:rotate(-9deg)}60%{transform:rotate(6deg)}66%{transform:rotate(-3deg)}}
@keyframes settings-bell-wave{0%{opacity:0;transform:scale(.62)}28%{opacity:.7}100%{opacity:0;transform:scale(1.18)}}
@media (prefers-reduced-motion:reduce){.bell-icon img,.bell-wave{animation:none}}
</style>
