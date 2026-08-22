<script setup>
defineProps({
  items: { type: Array, default: () => [] },
  compact: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])
</script>

<template>
  <div class="management-grid" :class="{ compact }">
    <button
      v-for="(item, index) in items"
      :key="item.label"
      type="button"
      class="management-tile"
      :class="{ wide: index === items.length - 1 && items.length % 2 === 1 }"
      :style="{ animationDelay: `${index * 70}ms` }"
      @click="emit('select', item)"
    >
      <div class="tile-top">
        <span class="tile-icon" aria-hidden="true">
          <svg v-if="item.icon === 'report'" viewBox="0 0 24 24" fill="none">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M14 2v6h6M8 13h8M8 17h4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <svg v-else-if="item.icon === 'checklist'" viewBox="0 0 24 24" fill="none">
            <path d="m9 11 3 3L22 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <svg v-else-if="item.icon === 'schedule'" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
            <path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <svg v-else-if="item.icon === 'receipt'" viewBox="0 0 24 24" fill="none">
            <path d="M5 3h14v18l-2.5-1.5L14 21l-2.5-1.5L9 21l-4-2V3Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
            <path d="M9 8h6M9 12h6M9 16h4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/>
            <path d="m9 12 2 2 4.5-4.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <span v-if="item.badge" class="tile-badge" :class="{ preparing: item.badge === '준비 중' }">{{ item.badge }}</span>
      </div>
      <span class="tile-copy">
        <b>{{ item.label }}</b>
        <small>{{ item.desc }}</small>
      </span>
    </button>
  </div>
</template>

<style scoped>
.management-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}
.management-tile{min-width:0;min-height:138px;padding:16px;border:0;border-radius:18px;background:#fff;color:#111827;text-align:left;box-shadow:0 4px 14px rgba(16,25,43,.07);opacity:0;transform:translateY(14px) scale(.96);animation:tile-reveal .44s cubic-bezier(.2,.82,.28,1.12) forwards;transition:transform .18s ease,box-shadow .18s ease}
.management-tile:active{transform:scale(.97);box-shadow:0 2px 8px rgba(16,25,43,.06)}
.management-tile.wide{grid-column:1/-1;min-height:122px}
.tile-top{display:flex;align-items:flex-start;justify-content:space-between;gap:8px}
.tile-icon{display:grid;width:40px;height:40px;flex:0 0 auto;place-items:center;border-radius:12px;background:#eef2ff;color:#3b5bdb}
.tile-icon svg{width:18px;height:18px}
.tile-badge{flex:0 0 auto;padding:3px 8px;border-radius:999px;background:#f4f5f9;color:#111827;font-size:10.5px;font-weight:700;line-height:1.45}
.tile-badge.preparing{background:#eaf1ff;color:#0b2a6b}
.tile-copy{display:block;margin-top:12px}
.tile-copy b{display:block;font-size:14px;font-weight:600;line-height:1.4}
.tile-copy small{display:block;margin-top:3px;color:#9aa4b5;font-size:12px;line-height:1.45}
.management-grid.compact{gap:9px}
.compact .management-tile{min-height:112px;padding:13px;border-radius:16px;box-shadow:0 4px 12px rgba(16,25,43,.06)}
.compact .management-tile.wide{min-height:100px}
.compact .tile-icon{width:36px;height:36px;border-radius:12px}
.compact .tile-copy{margin-top:10px}
.compact .tile-copy b{font-size:11px;font-weight:700}
.compact .tile-copy small{margin-top:4px;font-size:8.5px}
.compact .tile-badge{padding:2px 7px;font-size:8.5px}
@keyframes tile-reveal{0%{opacity:0;transform:translateY(14px) scale(.96)}68%{opacity:1;transform:translateY(-2px) scale(1.01)}100%{opacity:1;transform:none}}
@media (prefers-reduced-motion:reduce){.management-tile{opacity:1;transform:none;animation:none}}
</style>
