<template>
  <label class="relative inline-flex items-center cursor-pointer">
    <input
      type="checkbox"
      :checked="modelValue"
      @change="$emit('update:modelValue', $event.target.checked)"
      class="sr-only peer"
    >
    <div
      class="w-11 h-6 rounded-full peer transition-all duration-300 peer-focus:outline-none peer-focus:ring-4"
      :class="switchClasses"
    >
      <div
        class="
          absolute top-[2px] left-[2px]
          bg-white
          border border-gray-300
          rounded-full h-5 w-5
          transition-all duration-300
          shadow-md
          peer-checked:translate-x-full
          peer-checked:border-white
        "
      ></div>
    </div>
  </label>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  color: {
    type: String,
    default: 'orange'
  }
})

defineEmits(['update:modelValue'])

const switchClasses = computed(() => {
  const colors = {
    orange: {
      bg: 'bg-gray-200 dark:bg-gray-700',
      checked: 'peer-checked:bg-fleet-orange-500',
      ring: 'peer-focus:ring-fleet-orange-300 dark:peer-focus:ring-fleet-orange-800'
    },
    purple: {
      bg: 'bg-gray-200 dark:bg-gray-700',
      checked: 'peer-checked:bg-purple-600',
      ring: 'peer-focus:ring-purple-300 dark:peer-focus:ring-purple-800'
    },
    blue: {
      bg: 'bg-gray-200 dark:bg-gray-700',
      checked: 'peer-checked:bg-blue-600',
      ring: 'peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800'
    },
    indigo: {
      bg: 'bg-gray-200 dark:bg-gray-700',
      checked: 'peer-checked:bg-indigo-600',
      ring: 'peer-focus:ring-indigo-300 dark:peer-focus:ring-indigo-800'
    },
    green: {
      bg: 'bg-gray-200 dark:bg-gray-700',
      checked: 'peer-checked:bg-green-600',
      ring: 'peer-focus:ring-green-300 dark:peer-focus:ring-green-800'
    },
    red: {
      bg: 'bg-gray-200 dark:bg-gray-700',
      checked: 'peer-checked:bg-red-600',
      ring: 'peer-focus:ring-red-300 dark:peer-focus:ring-red-800'
    }
  }

  const config = colors[props.color] || colors.orange
  return `${config.bg} ${config.checked} ${config.ring}`
})
</script>

<style scoped>
/* Smooth toggle animation */
.peer:checked ~ div > div {
  transform: translateX(20px);
}
</style>
