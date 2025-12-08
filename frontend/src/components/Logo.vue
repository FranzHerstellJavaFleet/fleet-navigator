<template>
  <svg :width="size" :height="size" viewBox="0 0 40 40" class="inline-block">
    <defs>
      <linearGradient id="sailGradient" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" style="stop-color:#FF9500;stop-opacity:1" />
        <stop offset="100%" style="stop-color:#EA580C;stop-opacity:1" />
      </linearGradient>
    </defs>

    <!-- Einfaches Segelboot -->
    <!-- Segel -->
    <path
      d="M18 8 L12 24 L18 24 Z"
      fill="url(#sailGradient)"
      opacity="0.9"
    />

    <!-- Mast -->
    <line x1="18" y1="8" x2="18" y2="25" stroke="#654321" stroke-width="1.5"/>

    <!-- Rumpf -->
    <path
      d="M10 25 Q15 28 20 25"
      stroke="#654321"
      stroke-width="2"
      fill="none"
    />

    <!-- Java Tasse (kleiner) -->
    <g transform="translate(24, 14)">
      <!-- Tasse -->
      <rect x="0" y="0" width="6" height="7" rx="1" fill="#EA580C" opacity="0.9"/>

      <!-- Henkel -->
      <path
        d="M6 2 Q8 2 8 4 Q8 5 6 5"
        stroke="#EA580C"
        stroke-width="1.2"
        fill="none"
      />

      <!-- Dampf -->
      <path
        d="M2 -2 Q2 -4 3 -4"
        stroke="#FF9500"
        stroke-width="1"
        fill="none"
        opacity="0.7"
      />
      <path
        d="M4 -2 Q4 -3 5 -3"
        stroke="#FF9500"
        stroke-width="1"
        fill="none"
        opacity="0.7"
      />
    </g>

    <!-- Kleine Wellen -->
    <path
      d="M8 28 Q12 30 16 28 Q20 26 24 28"
      stroke="#4A9EFF"
      stroke-width="1.2"
      fill="none"
      opacity="0.5"
    />
  </svg>
</template>

<script setup>
defineProps({
  size: {
    type: Number,
    default: 48
  }
})
</script>
