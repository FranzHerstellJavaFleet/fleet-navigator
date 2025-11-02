<template>
  <div class="relative group">
    <button
      @click="$emit('click')"
      class="
        relative
        p-2.5
        rounded-xl
        border
        shadow-sm
        transition-all duration-200
        transform
        hover:scale-105
        active:scale-95
        hover:shadow-md
      "
      :class="buttonClasses"
    >
      <!-- Icon Slot -->
      <div class="relative z-10 transition-colors duration-200" :class="iconClasses">
        <slot />
      </div>

      <!-- Coming Soon Badge -->
      <span
        v-if="hasBadge"
        class="
          absolute -top-1 -right-1
          w-2 h-2
          bg-yellow-400
          rounded-full
          ring-2 ring-white dark:ring-gray-800
          animate-pulse
        "
      />
    </button>

    <!-- Tooltip -->
    <div
      class="
        absolute top-full mt-2 left-1/2 -translate-x-1/2
        px-3 py-1.5
        bg-gray-900 dark:bg-gray-700
        text-white text-xs font-medium
        rounded-lg shadow-xl
        opacity-0 group-hover:opacity-100
        pointer-events-none
        transition-opacity duration-200
        whitespace-nowrap
        z-50
      "
    >
      {{ title }}
      <div class="
        absolute -top-1 left-1/2 -translate-x-1/2
        w-2 h-2
        bg-gray-900 dark:bg-gray-700
        rotate-45
      "></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: String,
  color: {
    type: String,
    default: 'orange'
  },
  active: {
    type: Boolean,
    default: false
  },
  hasBadge: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const colorConfig = {
  orange: {
    border: 'border-gray-200 dark:border-gray-700',
    hoverBorder: 'hover:border-fleet-orange-400 dark:hover:border-fleet-orange-500',
    bg: 'bg-gradient-to-br from-white to-gray-50 dark:from-gray-800 dark:to-gray-900',
    hoverBg: 'hover:bg-gradient-to-br hover:from-fleet-orange-50 hover:to-orange-50 dark:hover:from-fleet-orange-900/20 dark:hover:to-orange-900/20',
    icon: 'text-gray-600 dark:text-gray-400',
    hoverIcon: 'group-hover:text-fleet-orange-500 dark:group-hover:text-fleet-orange-400',
    activeBg: 'bg-gradient-to-br from-fleet-orange-100 to-orange-100 dark:from-fleet-orange-900/40 dark:to-orange-900/40',
    activeBorder: 'border-fleet-orange-400 dark:border-fleet-orange-500'
  },
  blue: {
    border: 'border-gray-200 dark:border-gray-700',
    hoverBorder: 'hover:border-blue-400 dark:hover:border-blue-500',
    bg: 'bg-gradient-to-br from-white to-gray-50 dark:from-gray-800 dark:to-gray-900',
    hoverBg: 'hover:bg-gradient-to-br hover:from-blue-50 hover:to-blue-50 dark:hover:from-blue-900/20 dark:hover:to-blue-900/20',
    icon: 'text-gray-600 dark:text-gray-400',
    hoverIcon: 'group-hover:text-blue-500 dark:group-hover:text-blue-400'
  },
  green: {
    border: 'border-gray-200 dark:border-gray-700',
    hoverBorder: 'hover:border-green-400 dark:hover:border-green-500',
    bg: 'bg-gradient-to-br from-white to-gray-50 dark:from-gray-800 dark:to-gray-900',
    hoverBg: 'hover:bg-gradient-to-br hover:from-green-50 hover:to-green-50 dark:hover:from-green-900/20 dark:hover:to-green-900/20',
    icon: 'text-gray-600 dark:text-gray-400',
    hoverIcon: 'group-hover:text-green-500 dark:group-hover:text-green-400'
  },
  purple: {
    border: 'border-gray-200 dark:border-gray-700',
    hoverBorder: 'hover:border-purple-400 dark:hover:border-purple-500',
    bg: 'bg-gradient-to-br from-white to-gray-50 dark:from-gray-800 dark:to-gray-900',
    hoverBg: 'hover:bg-gradient-to-br hover:from-purple-50 hover:to-purple-50 dark:hover:from-purple-900/20 dark:hover:to-purple-900/20',
    icon: 'text-gray-600 dark:text-gray-400',
    hoverIcon: 'group-hover:text-purple-500 dark:group-hover:text-purple-400'
  }
}

const config = computed(() => colorConfig[props.color] || colorConfig.orange)

const buttonClasses = computed(() => {
  const classes = [
    config.value.border,
    config.value.hoverBorder,
    config.value.bg,
    config.value.hoverBg
  ]

  if (props.active && config.value.activeBg) {
    classes.push(config.value.activeBg)
    classes.push(config.value.activeBorder)
  }

  return classes.join(' ')
})

const iconClasses = computed(() => {
  return [
    config.value.icon,
    config.value.hoverIcon
  ].join(' ')
})
</script>
