<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAssetStore } from '@/stores/asset'

const route = useRoute(); const router = useRouter(); const asset = useAssetStore()
const item = asset.getFixedExpense(route.params.fixedExpenseId)
const editing = ref(false); const confirmDelete = ref(false)
const form = reactive(item ? { ...item } : {})
const account = computed(() => asset.getAccount(form.accountId))
const money = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`
function save(){ asset.updateFixedExpense(item.id, form); editing.value=false }
function remove(){ asset.removeFixedExpense(item.id); router.replace('/asset/fixed-expenses') }
</script>

<template><main class="page"><div class="shell">
  <header><button @click="router.back()">‹</button><h1>고정지출 상세</h1></header>
  <template v-if="item">
    <section class="hero"><span :style="{color:item.color,background:`${item.color}18`}">{{ item.icon }}</span><div><small>매월 {{ form.day }}일 예정</small><h2>{{ form.name }}</h2><strong>{{ money(form.amount) }}</strong></div></section>
    <section v-if="!editing" class="details"><dl><div><dt>반복 예정일</dt><dd>매월 {{ form.day }}일</dd></div><div><dt>연결 계좌</dt><dd>{{ account?.name }} {{ account?.number }}</dd></div><div><dt>알림 시점</dt><dd>{{ form.alert }}</dd></div><div><dt>상태</dt><dd class="active">{{ form.active ? '사용 중' : '일시 정지' }}</dd></div><div><dt>메모</dt><dd>{{ form.memo || '-' }}</dd></div></dl><p>수정된 내용은 다음 금융 일정부터 반영돼요.</p></section>
    <section v-else class="details edit"><label>항목명<input v-model="form.name"></label><div class="row"><label>예정일<input v-model.number="form.day" type="number" min="1" max="31"></label><label>금액<input v-model="form.amount" type="number"></label></div><label>연결 계좌<select v-model="form.accountId"><option v-for="a in asset.accounts" :key="a.id" :value="a.id">{{ a.name }}</option></select></label><label>알림<select v-model="form.alert"><option>당일</option><option>1일 전</option><option>3일 전</option><option>7일 전</option></select></label><label>메모<textarea v-model="form.memo"></textarea></label></section>
    <div class="actions"><button v-if="!editing" @click="editing=true">내용 수정</button><button v-else @click="save">수정 완료</button><button class="danger" @click="confirmDelete=true">삭제</button></div>
  </template><p v-else class="missing">고정지출 정보를 찾을 수 없어요.</p>
  <div v-if="confirmDelete" class="dim" @click.self="confirmDelete=false"><section class="modal"><h3>고정지출을 삭제할까요?</h3><p>이미 발생한 거래내역은 그대로 유지돼요.</p><div><button @click="confirmDelete=false">취소</button><button @click="remove">삭제</button></div></section></div>
</div></main></template>

<style scoped>
.page{min-height:100vh;background:#e7ecf4;color:#10192d}.shell{width:min(100%,390px);min-height:100vh;margin:auto;padding:52px 18px 28px;background:#f7f5ef}header{display:flex;align-items:center;margin-bottom:18px}header button{font-size:25px}h1{flex:1;text-align:center;font-size:18px;font-weight:900;padding-right:20px}.hero{display:flex;align-items:center;gap:14px;padding:18px;border:1px solid #e1e6ee;border-radius:17px;background:#fff}.hero>span{display:grid;width:52px;height:52px;place-items:center;border-radius:50%;font-size:22px}.hero small{color:#9aa5b4;font-size:9px}.hero h2{margin:3px 0;font-size:16px}.hero strong{color:#173f8d;font-size:22px}.details{margin-top:14px;padding:8px 16px 14px;border:1px solid #e1e6ee;border-radius:17px;background:#fff}.details dl>div{display:flex;justify-content:space-between;gap:20px;padding:14px 0;border-bottom:1px solid #edf0f4;font-size:11px}.details dt{color:#94a3b8}.details dd{text-align:right;font-weight:800}.active{color:#0fa874}.details>p{margin-top:12px;padding:10px;border-radius:9px;background:#fff7df;color:#a56b00;font-size:9px}.actions{display:grid;grid-template-columns:2fr 1fr;gap:9px;margin-top:18px}.actions button{padding:14px;border-radius:11px;background:#173f8d;color:#fff;font-weight:900}.actions .danger{background:#fff1f1;color:#e5484d}.edit{padding-top:16px}.edit label{display:block;margin-bottom:12px;color:#68768a;font-size:9px}.edit input,.edit select,.edit textarea{width:100%;margin-top:5px;padding:11px;border:1px solid #dce3ed;border-radius:9px}.row{display:grid;grid-template-columns:1fr 1fr;gap:10px}.missing{text-align:center}.dim{position:fixed;inset:0;display:grid;place-items:end center;background:#0f172a73}.modal{width:min(100%,390px);padding:24px 20px 30px;border-radius:22px 22px 0 0;background:#fff}.modal h3{font-size:17px}.modal p{margin:8px 0 20px;color:#7c8798;font-size:11px}.modal div{display:grid;grid-template-columns:1fr 1fr;gap:8px}.modal button{padding:13px;border-radius:10px;background:#eef1f5;font-weight:900}.modal button:last-child{background:#e5484d;color:#fff}
</style>
