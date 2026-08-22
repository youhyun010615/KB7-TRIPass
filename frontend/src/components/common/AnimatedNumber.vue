<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'

// 데이터는 라우터에서 미리 불러와 두므로(정확한 값), 여기서는 표시할 때 0에서
// 실제 값까지 롤링되며 올라가는(혹은 내려가는) 연출만 담당한다.
const props = defineProps({
  value: { type: Number, default: 0 },
  duration: { type: Number, default: 700 },
  decimals: { type: Number, default: 0 },
  formatter: { type: Function, default: null },
})

const displayValue = ref(0)
let rafId = null
let lastTarget = null

function easeOutCubic(t) {
  return 1 - Math.pow(1 - t, 3)
}

function animateTo(target) {
  // 소수점 반올림 시 화면에 표시되는 숫자가 바뀌지 않는 재요청(예: 디바운스된 환율 재조회)이면
  // 애니메이션을 다시 실행하지 않는다. 그렇지 않으면 값이 거의 그대로인데도 매번 롤링돼
  // "무한 카운팅"처럼 보인다. 실제로 표시값이 달라질 때만, 직전 값 대비 한 번만 움직인다.
  const epsilon = props.decimals > 0 ? 0.5 / 10 ** props.decimals : 0.5
  if (lastTarget !== null && Math.abs(target - lastTarget) < epsilon) {
    lastTarget = target
    return
  }
  lastTarget = target

  cancelAnimationFrame(rafId)

  const start = displayValue.value
  const diff = target - start
  if (diff === 0) {
    displayValue.value = target
    return
  }

  const startTime = performance.now()

  function tick(now) {
    const progress = Math.min(1, (now - startTime) / props.duration)
    displayValue.value = start + diff * easeOutCubic(progress)

    if (progress < 1) {
      rafId = requestAnimationFrame(tick)
    } else {
      displayValue.value = target
    }
  }

  rafId = requestAnimationFrame(tick)
}

watch(() => props.value, next => animateTo(Number(next) || 0), { immediate: true })
onBeforeUnmount(() => cancelAnimationFrame(rafId))

function formatted() {
  const rounded = props.decimals > 0
    ? Number(displayValue.value.toFixed(props.decimals))
    : Math.round(displayValue.value)

  if (props.formatter) return props.formatter(rounded)
  return rounded.toLocaleString('ko-KR', { minimumFractionDigits: props.decimals, maximumFractionDigits: props.decimals })
}
</script>

<template>
  <span>{{ formatted() }}</span>
</template>
