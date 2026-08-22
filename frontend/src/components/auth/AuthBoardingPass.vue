<script setup>
import tripassTransparentSymbol from '@/assets/brand/tripass-symbol-transparent-v2.png'

defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
  ticketCode: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <main class="auth-shell">
    <div class="ambient ambient-main"></div>
    <div class="ambient ambient-soft"></div>

    <header class="auth-hero">
      <div class="brand">
        <img :src="tripassTransparentSymbol" alt="" />
        <span>TRIPASS</span>
      </div>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
    </header>

    <section class="boarding-pass">
      <div class="ticket-band">
        <span>
          <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M21.7 11.2 14 7.1V3.6a2 2 0 0 0-4 0v3.5l-7.7 4.1a1.5 1.5 0 0 0-.8 1.3v1.2l8.5-2.2v4.2l-2.3 1.8v1l4.3-1 4.3 1v-1L14 15.7v-4.2l8.5 2.2v-1.2a1.5 1.5 0 0 0-.8-1.3Z" />
          </svg>
          TRIPASS BOARDING PASS
        </span>
        <strong>{{ ticketCode }}</strong>
      </div>

      <div class="ticket-body">
        <slot></slot>
      </div>
    </section>

    <footer v-if="$slots.footer" class="auth-footer">
      <slot name="footer"></slot>
    </footer>
  </main>
</template>

<style scoped>
.auth-shell {
  position: relative;
  display: flex;
  min-height: 100dvh;
  flex-direction: column;
  overflow: hidden;
  color: white;
  background:
    radial-gradient(circle at 96% 4%, rgba(108, 159, 255, 0.36) 0 82px, transparent 83px),
    linear-gradient(160deg, #2861c9 0%, #153f91 43%, #0b2865 100%);
  isolation: isolate;
}

.ambient {
  position: absolute;
  z-index: -1;
  border-radius: 999px;
  pointer-events: none;
}

.ambient-main {
  top: -64px;
  right: -58px;
  width: 210px;
  height: 210px;
  background: rgba(255, 255, 255, 0.06);
}

.ambient-soft {
  bottom: -105px;
  left: -82px;
  width: 240px;
  height: 240px;
  background: rgba(42, 91, 181, 0.32);
}

.auth-hero {
  padding: max(40px, env(safe-area-inset-top)) 22px 26px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: white;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.16em;
}

.brand img {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  object-fit: contain;
}

.ticket-band svg {
  width: 15px;
  height: 15px;
  color: #ffd45e;
  transform: rotate(45deg);
}

.auth-hero h1 {
  max-width: 310px;
  margin: 19px 0 0;
  font-size: 25px;
  line-height: 1.34;
  font-weight: 850;
  letter-spacing: -0.04em;
  white-space: pre-line;
}

.auth-hero p {
  margin-top: 7px;
  color: rgba(211, 226, 255, 0.76);
  font-size: 12px;
  line-height: 1.6;
}

.boarding-pass {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 480px;
  flex: 1;
  flex-direction: column;
  margin: 0 18px;
  overflow: hidden;
  border-radius: 20px;
  background: white;
  box-shadow: 0 22px 45px rgba(2, 20, 58, 0.28);
}

.ticket-band {
  display: flex;
  min-height: 52px;
  align-items: center;
  justify-content: space-between;
  padding: 0 19px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.5);
  color: rgba(255, 255, 255, 0.88);
  background: #0d327e;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.ticket-band span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ticket-band strong {
  color: rgba(255, 255, 255, 0.58);
  font-size: 8px;
  letter-spacing: 0.12em;
}

.ticket-body {
  position: relative;
  flex: 1;
  padding: 24px 20px 28px;
  color: #111b32;
}

.ticket-body::before,
.ticket-body::after {
  position: absolute;
  top: -12px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #17479e;
  content: '';
}

.ticket-body::before { left: -12px; }
.ticket-body::after { right: -12px; }

.auth-footer {
  position: relative;
  z-index: 1;
  padding: 17px 20px max(22px, env(safe-area-inset-bottom));
  text-align: center;
}

@media (max-height: 760px) {
  .auth-hero { padding-top: 28px; padding-bottom: 18px; }
  .auth-hero h1 { margin-top: 13px; font-size: 22px; }
  .boarding-pass { min-height: 430px; }
  .ticket-body { padding-top: 20px; padding-bottom: 22px; }
  .auth-footer { padding-top: 12px; padding-bottom: 15px; }
}
</style>
