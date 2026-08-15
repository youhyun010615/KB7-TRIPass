<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  images: { type: Array, default: () => [] },
  color: { type: String, default: '#153783' },
  issuer: { type: String, default: '' },
  brand: { type: String, default: '' },
  interval: { type: Number, default: 2000 },
  // null이면 자동 스와이프 중, 숫자면 해당 인덱스로 고정. 상위(store)에서 관리해서
  // 페이지 이동 후 돌아와도 고정 상태가 유지되도록 합니다.
  frozenIndex: { type: Number, default: null },
})

const emit = defineEmits(['update:frozenIndex'])

const TRANSITION_MS = 600

const activeIndex = ref(0)
const withTransition = ref(true)
let tickTimer = null
let resetTimer = null

const isPaused = computed(() => props.frozenIndex !== null && props.frozenIndex !== undefined)

// 마지막에 첫 이미지를 한 장 더 붙여서, 끝에 도달하면 그 복제본으로 자연스럽게
// 슬라이드한 뒤 transition 없이 index 0으로 순간 복귀시켜 한 방향으로 계속 도는 것처럼 보이게 합니다.
const trackImages = computed(() => (
  props.images.length > 1 ? [...props.images, props.images[0]] : props.images
))

function clearTimers() {
  if (tickTimer) clearInterval(tickTimer)
  if (resetTimer) clearTimeout(resetTimer)
  tickTimer = null
  resetTimer = null
}

function snapTo(index) {
  withTransition.value = false
  activeIndex.value = index
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      withTransition.value = true
    })
  })
}

function startCycle() {
  clearTimers()
  if (isPaused.value || props.images.length < 2) return
  tickTimer = setInterval(() => {
    activeIndex.value += 1
    if (activeIndex.value === props.images.length) {
      resetTimer = setTimeout(() => snapTo(0), TRANSITION_MS + 50)
    }
  }, props.interval)
}

function clampIndex(index) {
  return Math.min(Math.max(index, 0), Math.max(props.images.length - 1, 0))
}

function toggleFreeze(event) {
  if (props.images.length < 2) return
  event.stopPropagation()
  if (isPaused.value) {
    emit('update:frozenIndex', null)
  } else {
    const current = activeIndex.value >= props.images.length ? 0 : activeIndex.value
    emit('update:frozenIndex', current)
  }
}

watch(() => props.frozenIndex, (value) => {
  if (value !== null && value !== undefined) {
    clearTimers()
    snapTo(clampIndex(value))
  } else {
    startCycle()
  }
})

watch(() => props.images, () => {
  if (isPaused.value) emit('update:frozenIndex', null)
  snapTo(0)
  startCycle()
})

onMounted(() => {
  if (isPaused.value) {
    snapTo(clampIndex(props.frozenIndex))
  } else {
    startCycle()
  }
})
onBeforeUnmount(clearTimers)
</script>

<template>
  <div
    class="travel-card-visual"
    :class="{ clickable: images.length > 1 }"
    :style="!images.length ? { background: color } : null"
    @click="toggleFreeze"
  >
    <div
      v-if="images.length"
      class="card-track"
      :class="{ 'no-transition': !withTransition }"
      :style="{ transform: `translateX(-${activeIndex * 100}%)` }"
    >
      <img v-for="(src, index) in trackImages" :key="index" :src="src" class="card-frame" alt="">
    </div>
    <template v-else>
      <b>{{ issuer }}</b>
      <span>{{ brand }}</span>
      <i />
    </template>
  </div>
</template>

<style scoped>
.travel-card-visual{position:relative;width:100%;height:100%;overflow:hidden;border-radius:inherit;color:#fff}
.travel-card-visual.clickable{cursor:pointer}
.travel-card-visual b{position:absolute;left:16px;top:14px;font-size:12px;font-weight:700}
.travel-card-visual span{position:absolute;top:14px;right:16px;color:rgba(255,255,255,.78);font-size:10px;font-weight:800}
.travel-card-visual i{position:absolute;left:16px;bottom:14px;width:22px;height:16px;border-radius:5px;background:#ffd35d}
.card-track{display:flex;width:100%;height:100%;transition:transform .6s ease}
.card-track.no-transition{transition:none}
.card-frame{flex:0 0 100%;width:100%;height:100%;object-fit:cover}
.travel-card-visual{word-break:keep-all}.travel-card-visual b{text-wrap:balance}
</style>
