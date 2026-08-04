<script setup>
defineProps({
  city: { type: String, default: '파리' },
  dDay: { type: String, default: 'D-230' },
  target: { type: Number, required: true },
  secured: { type: Number, required: true },
  shortage: { type: Number, required: true },
  percent: { type: Number, required: true },
})

const money = (value) => `${Number(value).toLocaleString('ko-KR')}원`
</script>

<template>
  <section class="goal-ticket">
    <div class="ticket-topline"><span>TRIPASS · BOARDING PASS</span><b>{{ dDay }}</b></div>
    <div class="ticket-route"><strong>🇫🇷 FR PAR</strong><span>✈</span><strong>GOAL</strong></div>
    <div class="ticket-body">
      <div class="progress-ring" :style="{ '--progress': `${percent * 3.6}deg` }">
        <div><strong>{{ percent }}%</strong><small>현재 확보율</small></div>
      </div>
      <dl>
        <div><dt>총 목표 금액</dt><dd>{{ money(target) }}</dd></div>
        <div><dt>확보한 여행 자금</dt><dd>{{ money(secured) }}</dd></div>
        <div><dt>부족 금액</dt><dd>{{ money(shortage) }}</dd></div>
      </dl>
    </div>
    <div class="ticket-footer"><span>PASS NO. TRP-250815</span><i>|||||||||||</i></div>
  </section>
</template>

<style scoped>
.goal-ticket { position:relative; overflow:visible; padding:14px 18px 11px; border-radius:16px; color:#fff; background:linear-gradient(145deg,#294ca3 0%,#173b86 62%,#102f70 100%); box-shadow:0 9px 20px rgba(23,59,134,.2); }
.goal-ticket::before,.goal-ticket::after { content:''; position:absolute; top:50%; width:18px; height:18px; border-radius:50%; background:#f7f4ee; transform:translateY(-50%); }
.goal-ticket::before { left:-9px; }.goal-ticket::after { right:-9px; }
.ticket-topline,.ticket-route,.ticket-footer { display:flex; align-items:center; justify-content:space-between; }
.ticket-topline { color:#c9d7ff; font-size:7px; letter-spacing:.05em; }.ticket-topline b { color:#ff9b52; font-size:10px; }
.ticket-route { margin-top:9px; padding-bottom:8px; border-bottom:1px dashed rgba(255,255,255,.32); font-size:12px; }.ticket-route span { color:#ff9b52; }
.ticket-body { display:flex; align-items:center; gap:23px; padding:14px 4px 10px; }
.progress-ring { display:grid; width:92px; height:92px; flex:none; padding:9px; border-radius:50%; background:conic-gradient(#ff4d4f var(--progress),#d8e4ff 0); }
.progress-ring > div { display:flex; flex-direction:column; align-items:center; justify-content:center; border-radius:50%; background:#24458f; }
.progress-ring strong { color:#ff4d4f; font-size:22px; }.progress-ring small { margin-top:1px; color:#d8e4ff; font-size:7px; }
.ticket-body dl { flex:1; display:grid; gap:7px; }.ticket-body dl div { display:grid; gap:1px; }
.ticket-body dt { color:#bfd0f8; font-size:8px; }.ticket-body dd { font-size:15px; font-weight:800; letter-spacing:-.04em; }
.ticket-body dl div:last-child dd { color:#ffc078; }
.ticket-footer { padding-top:8px; border-top:1px dashed rgba(255,255,255,.32); color:#aebfe8; font-size:6px; }.ticket-footer i { color:#fff; font-size:10px; font-style:normal; letter-spacing:-1px; }
</style>
