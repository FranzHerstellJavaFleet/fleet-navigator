<template>
  <div class="fleet-mate-monitor space-y-4">
    <!-- Fleet Mates List -->
    <div v-if="mates.length === 0" class="
      bg-gradient-to-br from-gray-800/30 to-gray-900/30
      backdrop-blur-sm
      p-6 rounded-xl
      border border-gray-700/30 border-dashed
      text-center
    ">
      <ServerIcon class="w-12 h-12 text-gray-600 mx-auto mb-3" />
      <div class="text-sm font-medium text-gray-400 mb-1">Keine Fleet Maate verbunden</div>
      <div class="text-xs text-gray-500">
        Fleet Maate sammeln Hardware-Daten von Remote-Systemen
      </div>
    </div>

    <!-- Mate Cards -->
    <div v-for="mate in mates" :key="mate.mateId" class="space-y-4">
      <!-- Mate Header -->
      <div class="
        bg-gradient-to-br from-fleet-orange-500/20 to-orange-600/20
        backdrop-blur-sm
        p-4 rounded-xl
        border border-fleet-orange-500/30
        shadow-lg
      ">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="p-2 rounded-lg bg-gradient-to-br from-fleet-orange-500 to-orange-600">
              <ServerIcon class="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 class="text-lg font-bold text-white">{{ mate.name }}</h3>
              <p class="text-xs text-gray-400">{{ mate.description }}</p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <span class="
              px-3 py-1 rounded-full text-xs font-semibold
              flex items-center gap-1.5
            " :class="mate.status === 'ONLINE'
              ? 'bg-green-500/20 text-green-400 border border-green-500/30'
              : 'bg-red-500/20 text-red-400 border border-red-500/30'">
              <component :is="mate.status === 'ONLINE' ? CheckCircleIcon : XCircleIcon" class="w-3 h-3" />
              {{ mate.status }}
            </span>
          </div>
        </div>
      </div>

      <!-- Hardware Stats -->
      <div v-if="mateStats[mate.mateId]" class="space-y-4">
        <MateHardwareCard :stats="mateStats[mate.mateId]" />
      </div>

      <div v-else class="
        bg-gradient-to-br from-gray-800/50 to-gray-900/50
        backdrop-blur-sm
        p-4 rounded-xl
        border border-gray-700/50
        text-center text-gray-400 text-sm
      ">
        <ArrowPathIcon class="w-6 h-6 mx-auto mb-2 animate-spin" />
        Lade Hardware-Daten...
      </div>
    </div>

    <!-- Refresh Button -->
    <button
      @click="refreshAllData"
      :disabled="isRefreshing"
      class="
        w-full px-4 py-3 rounded-xl
        bg-gradient-to-r from-fleet-orange-500 to-orange-600
        hover:from-fleet-orange-400 hover:to-orange-500
        text-white font-semibold
        shadow-lg hover:shadow-xl
        disabled:opacity-50 disabled:cursor-not-allowed
        transition-all duration-200
        transform hover:scale-105 active:scale-95
        flex items-center justify-center gap-2
      "
    >
      <ArrowPathIcon class="w-5 h-5" :class="{ 'animate-spin': isRefreshing }" />
      <span>{{ isRefreshing ? 'Aktualisiere...' : 'Aktualisieren' }}</span>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ServerIcon, CheckCircleIcon, XCircleIcon, ArrowPathIcon } from '@heroicons/vue/24/outline'
import MateHardwareCard from './MateHardwareCard.vue'
import axios from 'axios'

const mates = ref([])
const mateStats = ref({})
const isRefreshing = ref(false)
let intervalId = null

onMounted(async () => {
  await loadMates()
  await loadAllStats()
  // Auto-refresh every 5 seconds
  intervalId = setInterval(async () => {
    await loadMates()
    await loadAllStats()
  }, 5000)
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})

async function loadMates() {
  try {
    const response = await axios.get('/api/fleet-mate/mates')
    mates.value = response.data
  } catch (error) {
    console.error('Failed to load mates:', error)
  }
}

async function loadAllStats() {
  for (const mate of mates.value) {
    try {
      const response = await axios.get(`/api/fleet-mate/mates/${mate.mateId}/stats`)
      mateStats.value[mate.mateId] = response.data
    } catch (error) {
      console.error(`Failed to load stats for ${mate.mateId}:`, error)
    }
  }
}

async function refreshAllData() {
  isRefreshing.value = true
  try {
    await loadMates()
    await loadAllStats()
  } finally {
    setTimeout(() => {
      isRefreshing.value = false
    }, 500)
  }
}
</script>
