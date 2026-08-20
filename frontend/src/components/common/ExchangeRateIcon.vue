<script setup>
defineProps({
  size: {
    type: Number,
    default: 26,
  },
  strokeWidth: {
    type: Number,
    default: 1.7,
  },
})
</script>

<template>
  <svg
      :width="size"
      :height="size"
      viewBox="0 0 26 26"
      fill="none"
      aria-hidden="true"
  >
    <path
        d="M6.278 15.5 4.358 13.577 2.438 15.5M19.722 10.539l1.92 1.924 1.92-1.924"
        stroke="currentColor"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        stroke-linejoin="round"
    />
    <path
        d="M21.641 12.463a8.64 8.64 0 0 0-16.794-2.885M4.357 13.575a8.64 8.64 0 0 0 16.66 3.223"
        stroke="currentColor"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        stroke-linejoin="round"
    />
    <path
        d="M10.379 15.144c.056.644.362 1.239.852 1.66.49.42 1.125.632 1.77.59 2.114 0 2.621-1.217 2.621-2.25 0-1.032-.874-2.126-2.621-2.126-1.748 0-2.622-.691-2.622-2.1a2.49 2.49 0 0 1 2.622-2.275 2.49 2.49 0 0 1 2.621 2.275M13 18.704v-1.138M13 7.328v1.31"
        stroke="currentColor"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        stroke-linejoin="round"
    />
  </svg>
</template>
