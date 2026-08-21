<script setup>
import { loadingOverlayState } from '@/utils/loadingOverlay';
import airplaneIcon from '@/assets/icons/blue_airplane.svg';
</script>

<template>
  <Transition name="loading-fade">
    <section
      v-if="loadingOverlayState.visible"
      class="app-loading-overlay"
      role="status"
      aria-live="polite"
      aria-label="화면을 불러오는 중"
    >
      <div class="radar" aria-hidden="true">
        <i class="ring ring-one" />
        <i class="ring ring-two" />
        <i class="sweep" />
        <img :src="airplaneIcon" alt="" />
      </div>
      <div class="loading-copy">
        <strong>여행 정보를 불러오는 중</strong>
        <span>연결이 지연되고 있어요. 조금만 기다려 주세요.</span>
      </div>
    </section>
  </Transition>
</template>

<style scoped>
.app-loading-overlay{position:fixed;top:0;bottom:0;left:50%;z-index:1000;display:flex;width:min(100%,390px);flex-direction:column;align-items:center;justify-content:center;gap:25px;overflow:hidden;background:radial-gradient(circle at 82% 12%,rgba(83,132,218,.24),transparent 31%),linear-gradient(155deg,#123c86 0%,#0b2a6b 64%,#082258 100%);color:#fff;transform:translateX(-50%)}.app-loading-overlay::before{position:absolute;right:-70px;top:-72px;width:220px;height:220px;border-radius:50%;background:rgba(255,255,255,.045);content:''}.radar{position:relative;display:grid;width:112px;height:112px;place-items:center;border:1.5px solid rgba(255,255,255,.22);border-radius:50%;box-shadow:0 0 32px rgba(61,132,255,.18)}.ring{position:absolute;border:1.5px solid rgba(255,255,255,.2);border-radius:50%}.ring-one{width:74px;height:74px}.ring-two{width:38px;height:38px}.sweep{position:absolute;inset:0;border-radius:50%;background:conic-gradient(from -12deg,rgba(83,145,255,.76),rgba(60,122,239,.22) 23%,transparent 40%);animation:radar-spin 1.55s linear infinite}.radar img{position:relative;z-index:2;width:27px;height:27px;filter:brightness(0) invert(1) drop-shadow(0 2px 7px rgba(0,0,0,.22));transform:rotate(-6deg)}.loading-copy{position:relative;z-index:1;display:flex;flex-direction:column;align-items:center;gap:9px;text-align:center}.loading-copy strong{font-size:16px;font-weight:900;letter-spacing:-.025em}.loading-copy span{color:rgba(255,255,255,.61);font-size:11px;font-weight:650}.loading-fade-enter-active,.loading-fade-leave-active{transition:opacity .22s ease}.loading-fade-enter-from,.loading-fade-leave-to{opacity:0}@keyframes radar-spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.sweep{animation:none}.loading-fade-enter-active,.loading-fade-leave-active{transition:none}}
</style>
