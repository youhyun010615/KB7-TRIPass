<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from '@lucide/vue'
import BottomNav from '@/components/common/BottomNav.vue'
import TravelCardVisual from '@/components/common/TravelCardVisual.vue'
import { useTripWalletStore } from '@/stores/tripWallet'

const router = useRouter()
const wallet = useTripWalletStore()
const selectedCardId = ref(wallet.userTravelCards[0]?.id)
const notice = ref('')
const selectedCard = computed(() => wallet.userTravelCards.find(card => card.id === selectedCardId.value) ?? wallet.userTravelCards[0])
const hasLinkableCards = computed(() => wallet.userTravelCards.length > 0)

watch(() => wallet.userTravelCards, (cards) => {
  if (!selectedCardId.value && cards.length) selectedCardId.value = cards[0].id
}, { deep: true, immediate: true })

onMounted(async () => {
  try {
    await wallet.loadTravelCardOptions()
  } catch {
    notice.value = wallet.errorMessage || '보유 트래블카드를 불러오지 못했어요.'
  }
})

async function submitLink() {
  if (!selectedCard.value) return
  try {
    await wallet.linkTravelCard(selectedCard.value)
    router.push('/wallet')
  } catch {
    notice.value = wallet.errorMessage || '트래블카드 연결에 실패했어요.'
  }
}

function cardNumberLines(number) {
  const groups = String(number || '').split(' · ')
  const mid = Math.ceil(groups.length / 2)
  return [groups.slice(0, mid).join(' · '), groups.slice(mid).join(' · ')]
}
</script>

<template>
  <main class="card-link-page">
    <header class="page-header">
      <button type="button" class="back-button" @click="router.back()"><ChevronLeft :size="22" /></button>
      <div>
        <h1>트래블카드 연결</h1>
      </div>
      <span aria-hidden="true"></span>
    </header>

    <section class="guide-card">
      <div>
        <h2>월렛에 연결할 카드를 선택해요</h2>
        <p>카드를 연결하면 월렛 자금으로 외화를 환전하고, <br> 보유 외화를 확인할 수 있어요.</p>
      </div>
    </section>

    <p v-if="notice" class="notice-text">{{ notice }}</p>

    <h2 class="list-title">카드 선택</h2>

    <section v-if="!hasLinkableCards && !wallet.loading" class="empty-card-state" aria-live="polite">
      <div class="empty-card-animation" aria-hidden="true">
        <span class="ghost-card ghost-card-back" />
        <span class="ghost-card ghost-card-front"><i>＋</i></span>
        <span class="empty-spark spark-one">✦</span>
        <span class="empty-spark spark-two">✦</span>
      </div>
      <strong>연결할 카드가 없어요</strong>
      <p>먼저 자산관리에서 사용할 카드를 등록해 주세요.</p>
    </section>

    <section class="card-grid">
      <button
        v-for="card in wallet.userTravelCards"
        :key="card.id"
        type="button"
        class="travel-card-option"
        :class="{ selected: selectedCardId === card.id }"
        @click="selectedCardId = card.id"
      >
        <div class="mini-card">
          <TravelCardVisual
            v-model:frozen-index="wallet.cardFreezeIndex[card.id]"
            :images="card.images"
            :color="card.color"
            :issuer="card.issuer"
            :brand="card.brand"
          />
          <span class="radio" />
        </div>
        <div class="card-text">
          <strong>{{ card.name }}</strong>
          <dl>
            <div>
              <dt>카드사</dt>
              <dd>{{ card.issuer }}</dd>
            </div>
            <div>
              <dt>카드번호</dt>
              <dd>{{ cardNumberLines(card.number)[0] }}<br>{{ cardNumberLines(card.number)[1] }}</dd>
            </div>
          </dl>
        </div>
      </button>
    </section>

    <button type="button" class="submit-button" :disabled="!selectedCard" @click="submitLink">선택한 카드 연결하기</button>

    <BottomNav />
  </main>
</template>

<style scoped>
.card-link-page{max-width:430px;min-height:100vh;margin:0 auto;padding:44px 16px 96px;background:#eef2f8;color:#111827}.page-header{display:grid;grid-template-columns:36px 1fr 36px;align-items:center}.page-header button{display:grid;width:36px;height:36px;place-items:center;border-radius:12px;background:#fff;color:#193d82;box-shadow:0 5px 16px rgba(36,72,117,.07)}.page-header p{color:#1f5ab9;font-size:11px;font-weight:800;letter-spacing:.04em;text-align:center}.page-header h1{margin-top:2px;font-size:26px;font-weight:800;letter-spacing:-.04em;text-align:center}.guide-card{display:flex;gap:14px;margin-top:24px;padding:18px;border-radius:22px;background:#14357f;color:#fff;box-shadow:0 14px 28px rgba(24,51,99,.18)}.guide-card>span{display:grid;width:44px;height:44px;flex:0 0 44px;place-items:center;border-radius:15px;background:rgba(255,255,255,.16);font-size:22px}.guide-card h2{font-size:18px;font-weight:800;letter-spacing:-.04em}.guide-card p{margin-top:7px;color:#c8d6f4;font-size:13px;font-weight:500;line-height:1.45}.list-title{margin:22px 2px 12px;font-size:18px;font-weight:800;letter-spacing:-.04em}.card-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:14px}.travel-card-option{padding:10px;border:1.5px solid #dbe5f2;border-radius:20px;background:#fff;text-align:left;box-shadow:0 8px 20px rgba(20,42,75,.06)}.travel-card-option.selected{border-color:#2f70e9;background:#f8fbff}.mini-card{position:relative;width:100%;aspect-ratio:.63;border-radius:14px;overflow:hidden;color:#fff}.card-text{min-width:0;margin-top:12px}.card-text strong{display:block;font-size:14px;font-weight:800;letter-spacing:-.04em;line-height:1.3}.card-text dl{display:grid;gap:5px;margin-top:9px}.card-text div{display:grid;grid-template-columns:44px minmax(0,1fr);gap:6px;align-items:center}.card-text dt{color:#94a3b8;font-size:10px;font-weight:700}.card-text dd{min-width:0;color:#64748b;font-size:11px;font-weight:600;line-height:1.25}.radio{position:absolute;top:10px;right:10px;z-index:2;width:22px;height:22px;border:2px solid rgba(255,255,255,.85);border-radius:50%;background:rgba(15,23,42,.18)}.travel-card-option.selected .radio{border:6px solid #2f70e9;background:#fff}.submit-button{width:100%;height:58px;margin-top:22px;border-radius:16px;background:#2f70e9;color:#fff;font-size:18px;font-weight:800;box-shadow:0 12px 24px rgba(47,112,233,.22)}.notice-text{margin-top:14px;padding:12px 14px;border-radius:14px;background:#fff5df;color:#a36c07;font-size:12px;font-weight:700}
.card-link-page{word-break:keep-all}.card-link-page h1,.card-link-page h2,.card-link-page h3{text-wrap:balance}.card-link-page p{text-wrap:pretty}
.card-link-page{padding:14px 16px 88px;background:#f2f5fa}.page-header button{width:36px;height:36px}.page-header h1{color:#29466f;font-size:20px;font-weight:700;letter-spacing:-.025em}.page-header p{font-size:9.5px}.guide-card{gap:11px;margin-top:18px;padding:14px;border-radius:18px}.guide-card>span{width:36px;height:36px;flex-basis:36px;border-radius:11px;font-size:17px}.guide-card h2{font-size:15px}.guide-card p{margin-top:5px;font-size:10.5px}.list-title{margin:18px 2px 10px;font-size:15px}.card-grid{gap:10px}.travel-card-option{padding:8px;border-radius:16px}.card-text{margin-top:9px}.card-text strong{font-size:12px}.card-text dl{margin-top:7px}.card-text dt{font-size:9px}.card-text dd{font-size:9.5px}.submit-button{height:44px;margin-top:17px;border-radius:13px;font-size:13px;font-weight:700}
.submit-button:disabled{background:#d8deea;color:#98a3b5;box-shadow:none;cursor:not-allowed}.empty-card-state{display:flex;min-height:260px;flex-direction:column;align-items:center;justify-content:center;padding:26px 18px;border:1px solid #dbe5f3;border-radius:22px;background:#fff;text-align:center;box-shadow:0 10px 28px rgba(23,63,141,.06)}.empty-card-state strong{margin-top:18px;color:#173f8d;font-size:16px;font-weight:800}.empty-card-state p{margin-top:7px;color:#8c9ab0;font-size:11px;line-height:1.5}.empty-card-animation{position:relative;width:112px;height:90px}.ghost-card{position:absolute;display:block;width:76px;height:48px;border-radius:12px}.ghost-card-back{top:10px;left:11px;background:#dce8ff;transform:rotate(-9deg);animation:card-float-back 2.6s ease-in-out infinite}.ghost-card-front{right:8px;bottom:7px;background:linear-gradient(135deg,#2662ea,#173f8d);box-shadow:0 12px 22px rgba(38,98,234,.25);animation:card-float 2.6s ease-in-out infinite}.ghost-card-front i{display:grid;width:100%;height:100%;place-items:center;color:#ffd45e;font-size:24px;font-style:normal}.empty-spark{position:absolute;color:#ffd45e;font-size:14px;animation:spark-pulse 1.7s ease-in-out infinite}.spark-one{top:0;right:5px}.spark-two{bottom:2px;left:3px;animation-delay:.6s}@keyframes card-float{0%,100%{transform:translateY(0) rotate(3deg)}50%{transform:translateY(-7px) rotate(1deg)}}@keyframes card-float-back{0%,100%{transform:translateY(0) rotate(-9deg)}50%{transform:translateY(5px) rotate(-6deg)}}@keyframes spark-pulse{0%,100%{opacity:.25;transform:scale(.7)}50%{opacity:1;transform:scale(1.15)}}
</style>
