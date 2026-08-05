<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import { useTravelScheduleStore } from '@/stores/travelSchedule'

const router = useRouter()
const store = useTravelScheduleStore()
const alerts = computed(() => store.sortedSchedules)
const country = code => store.countries.find(item => item.code === code)
const openDetail = item => {
  store.markNotificationRead(item.id)
  router.push(`/schedule/${item.id}`)
}
</script>

<template>
  <main class="notification-page">
    <header><button type="button" @click="router.back()">‹</button><h1>여행 일정 알림</h1><span /></header>
    <section class="intro-ticket">
      <small>TRIPASS SCHEDULE</small>
      <h2>다가오는 여행을<br>미리 준비해 드릴게요.</h2>
      <p>등록한 알림 시점에 맞춰 일정을 알려드려요.</p>
      <div><b>{{ alerts.filter(item => !item.notificationRead).length }}건</b><span>확인하지 않은 일정 알림</span></div>
    </section>

    <section class="alert-list">
      <div class="section-title"><h2>예정된 알림</h2><span>총 {{ alerts.length }}건</span></div>
      <button v-for="item in alerts" :key="item.id" type="button" :class="{ read: item.notificationRead }" @click="openDetail(item)">
        <span class="flag">{{ country(item.countryCode).flag }}</span>
        <span class="copy"><small>{{ item.date.replaceAll('-', '.') }} · {{ item.time }}</small><b>{{ item.title }}</b><em>1시간 전 자동 알림 · {{ item.paymentStatus === 'prepaid' ? '사전결제 완료' : '현장결제 필요' }}</em></span>
        <i v-if="!item.notificationRead" />
        <strong>›</strong>
      </button>
      <p v-if="!alerts.length">등록된 일정 알림이 없어요.</p>
    </section>
    <BottomNav />
  </main>
</template>

<style scoped>
.notification-page{min-height:100vh;padding:0 16px 100px;background:#f8f6f1;color:#10192d}.notification-page>header{display:grid;height:78px;grid-template-columns:32px 1fr 32px;align-items:end;padding-bottom:13px}.notification-page>header button{font-size:28px;text-align:left}.notification-page>header h1{text-align:center;font-size:18px;font-weight:900}.intro-ticket{position:relative;overflow:hidden;padding:20px;border-radius:18px;background:linear-gradient(135deg,#102f73,#1f5ec0);color:#fff;box-shadow:0 9px 20px #183f8e25}.intro-ticket:after{position:absolute;right:-28px;bottom:-36px;width:130px;height:130px;border:24px solid #ffffff0c;border-radius:50%;content:''}.intro-ticket small{color:#aecdff;font-size:8px;letter-spacing:1px}.intro-ticket h2{margin-top:13px;font-size:21px;line-height:1.35}.intro-ticket p{margin-top:7px;color:#d5e3fa;font-size:9px}.intro-ticket div{display:flex;align-items:end;gap:9px;margin-top:20px}.intro-ticket b{font-size:25px}.intro-ticket span{padding-bottom:3px;color:#d5e3fa;font-size:9px}.alert-list{margin-top:14px;padding:17px;border:1px solid #dce4ee;border-radius:18px;background:#fff}.section-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}.section-title h2{font-size:15px;font-weight:900}.section-title span{color:#94a3b8;font-size:8px}.alert-list button{position:relative;display:grid;width:100%;grid-template-columns:38px 1fr 7px 10px;align-items:center;gap:8px;margin-top:8px;padding:13px 11px;border:1px solid #cfe0f7;border-radius:13px;background:#f5f9ff;text-align:left}.alert-list button.read{border-color:#e3e8ef;background:#fafafa}.flag{display:grid;width:34px;height:34px;place-items:center;border-radius:50%;background:#fff}.copy>*{display:block}.copy small{color:#75849a;font-size:7px}.copy b{margin-top:5px;font-size:10px}.copy em{margin-top:5px;color:#2771dc;font-size:7px;font-style:normal}.read .copy em{color:#7d8999}.alert-list button>i{width:6px;height:6px;border-radius:50%;background:#ff6b35}.alert-list button>strong{color:#8796aa;font-size:18px}.alert-list>p{padding:45px 0;text-align:center;color:#94a3b8;font-size:10px}
</style>
