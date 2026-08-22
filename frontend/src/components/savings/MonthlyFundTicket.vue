<script setup>
defineProps({
  country: { type: Object, required: true },
  income: { type: Number, required: true },
  expense: { type: Number, required: true },
})

const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
</script>

<template>
  <section class="fund-ticket" :class="{ overall: country.code === 'all' }" :style="{ '--theme': country.theme, '--photo': `url(${country.image})` }">
    <header><b>{{ country.flag }} {{ country.city }} · 이달 자금</b><span>TRIPASS MONTHLY</span></header>
    <div class="perforation"><i /><span /><i /></div>
    <div class="ticket-values">
      <div><small>＋ 월 수입</small><strong>{{ money(income) }}</strong></div>
      <div><small>－ 고정지출</small><strong>{{ money(expense) }}</strong></div>
    </div>
    <div class="perforation bottom"><i /><span /><i /></div>
    <div class="ticket-barcode" aria-hidden="true"><i v-for="index in 22" :key="index" :class="{ wide: index % 4 === 0 }" /></div>
  </section>
</template>

<style scoped>
.fund-ticket{position:relative;overflow:hidden;min-height:146px;border-radius:16px;background:linear-gradient(100deg,#071a44c4,#0c2b62c4),var(--photo) center/cover;color:#fff;box-shadow:0 9px 18px #142a5c2e}.fund-ticket.overall{background:linear-gradient(125deg,#123f52,#1d6173)}
.fund-ticket header{display:flex;align-items:center;justify-content:space-between;padding:14px 17px 12px;font-size:11px}.fund-ticket header span{color:#ffffffa0;font-size:7px;font-weight:800;letter-spacing:.08em}
.perforation{display:grid;grid-template-columns:15px 1fr 15px;align-items:center;height:0}.perforation i{width:20px;height:20px;border-radius:50%;background:#f7f4ee}.perforation i:first-child{transform:translateX(-10px)}.perforation i:last-child{transform:translateX(5px)}.perforation span{border-top:1px dashed #ffffff80}.perforation.bottom{position:absolute;right:0;bottom:22px;left:0}
.ticket-values{display:grid;grid-template-columns:1fr 1fr;padding:24px 16px 22px}.ticket-values>div{display:flex;flex-direction:column;gap:8px}.ticket-values>div+div{padding-left:16px;border-left:1px solid #ffffff40}.ticket-values small{color:#dce8fa;font-size:10px;font-weight:700}.ticket-values strong{font-size:21px;line-height:1;letter-spacing:-.6px}
.ticket-barcode{display:flex;height:25px;align-items:center;justify-content:flex-end;gap:2px;padding:8px 17px 7px;background:#ffffff0c}.ticket-barcode i{width:1px;height:11px;background:#fff}.ticket-barcode i.wide{width:3px}
</style>
