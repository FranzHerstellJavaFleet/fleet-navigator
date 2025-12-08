<template>
  <div class="
    fixed right-0 h-full w-80
    bg-gradient-to-b from-gray-900 to-gray-950
    text-white
    border-l border-gray-700/50
    overflow-y-auto custom-scrollbar
    shadow-2xl
    z-40
    pt-16
  ">
    <!-- Header -->
    <div class="sticky top-0 z-10 bg-gray-900/80 backdrop-blur-xl border-b border-gray-700/50 p-4 mb-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <div class="p-2 rounded-lg bg-gradient-to-br from-fleet-orange-500 to-orange-600 shadow-lg">
            <ServerIcon class="w-5 h-5 text-white" />
          </div>
          <h3 class="text-lg font-bold bg-gradient-to-r from-white to-gray-300 bg-clip-text text-transparent">
            Fleet Mates
          </h3>
        </div>
        <button
          @click="$emit('close')"
          class="
            p-2 rounded-lg
            text-gray-400 hover:text-white
            hover:bg-gray-800
            transition-all duration-200
            transform hover:scale-110 active:scale-95
          "
        >
          <XMarkIcon class="w-5 h-5" />
        </button>
      </div>
    </div>

    <div class="px-4 pb-4 space-y-4">
      <!-- Database Size Tile -->
      <div class="
        bg-gradient-to-br from-indigo-900/30 to-purple-900/30
        backdrop-blur-sm
        p-4 rounded-xl
        border border-indigo-500/30
        shadow-lg
      ">
        <div class="flex items-center justify-between mb-2">
          <div class="flex items-center gap-2">
            <CircleStackIcon class="w-5 h-5 text-indigo-400" />
            <span class="text-sm font-medium text-gray-300">Datenbank</span>
          </div>
          <span class="text-lg font-bold text-indigo-400">
            {{ dbSize.formatted || '...' }}
          </span>
        </div>
        <div class="text-xs text-gray-500">
          H2 Database ({{ dbSize.sizeBytes ? (dbSize.sizeBytes).toLocaleString() : 0 }} Bytes)
        </div>
        <div v-if="dbSizeHistory.length > 0" class="text-xs text-gray-600 mt-1">
          Historie: {{ dbSizeHistory.length }} Messungen
        </div>
      </div>

      <!-- No Mates Connected -->
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
          Starten Sie einen Fleet Maat um Hardware-Daten zu empfangen
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
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-2">
              <ServerIcon class="w-5 h-5 text-fleet-orange-400" />
              <span class="text-sm font-bold text-white">{{ mate.name }}</span>
            </div>
            <span class="
              px-2.5 py-1 rounded-full text-xs font-semibold
              flex items-center gap-1.5
            " :class="mate.status === 'ONLINE'
              ? 'bg-green-500/20 text-green-400 border border-green-500/30'
              : 'bg-red-500/20 text-red-400 border border-red-500/30'">
              <component :is="mate.status === 'ONLINE' ? CheckCircleIcon : XCircleIcon" class="w-3 h-3" />
              {{ mate.status }}
            </span>
          </div>
          <p class="text-xs text-gray-400">{{ mate.description }}</p>
          <p class="text-xs text-gray-500 mt-1">ID: {{ mate.mateId }}</p>
        </div>

        <!-- Hardware Stats -->
        <div v-if="mateStats[mate.mateId]" class="space-y-3">
          <!-- System Info -->
          <div class="
            bg-gradient-to-br from-gray-800/50 to-gray-900/50
            backdrop-blur-sm
            p-4 rounded-xl
            border border-gray-700/50
            shadow-lg
          ">
            <div class="flex items-center gap-2 mb-3">
              <ComputerDesktopIcon class="w-5 h-5 text-fleet-orange-400" />
              <span class="text-sm font-medium text-gray-300">System</span>
            </div>
            <div class="space-y-2 text-xs">
              <div class="flex justify-between p-2 rounded-lg bg-gray-900/30">
                <span class="text-gray-400">Hostname</span>
                <span class="text-gray-200 font-medium">{{ mateStats[mate.mateId].system?.hostname || 'N/A' }}</span>
              </div>
              <div class="flex justify-between p-2 rounded-lg bg-gray-900/30">
                <span class="text-gray-400">OS</span>
                <span class="text-gray-200 font-medium">
                  {{ mateStats[mate.mateId].system?.platform || 'N/A' }}
                  {{ mateStats[mate.mateId].system?.platform_version || '' }}
                </span>
              </div>
              <div class="flex justify-between p-2 rounded-lg bg-gray-900/30">
                <span class="text-gray-400">Kernel</span>
                <span class="text-gray-200 font-medium">{{ mateStats[mate.mateId].system?.kernel_version || 'N/A' }}</span>
              </div>
              <div class="flex justify-between p-2 rounded-lg bg-gray-900/30">
                <span class="text-gray-400">Uptime</span>
                <span class="text-gray-200 font-medium">{{ formatUptime(mateStats[mate.mateId].system?.uptime) }}</span>
              </div>
            </div>
          </div>

          <!-- CPU Overview -->
          <div class="
            bg-gradient-to-br from-gray-800/50 to-gray-900/50
            backdrop-blur-sm
            p-4 rounded-xl
            border border-gray-700/50
            shadow-lg
          ">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <CpuChipIcon class="w-5 h-5 text-fleet-orange-400" />
                <span class="text-sm font-medium text-gray-300">CPU</span>
              </div>
              <span class="text-sm font-bold text-fleet-orange-500 bg-fleet-orange-500/10 px-2.5 py-1 rounded-lg">
                {{ mateStats[mate.mateId].cpu?.usage_percent?.toFixed(1) || '0.0' }}%
              </span>
            </div>
            <div class="space-y-2 text-xs mb-3">
              <div class="flex justify-between p-2 rounded-lg bg-gray-900/30">
                <span class="text-gray-400">Modell</span>
                <span class="text-gray-200 font-medium text-right">{{ getCpuModel(mateStats[mate.mateId].cpu?.model) }}</span>
              </div>
              <div class="flex justify-between p-2 rounded-lg bg-gray-900/30">
                <span class="text-gray-400">Kerne</span>
                <span class="text-gray-200 font-medium">{{ mateStats[mate.mateId].cpu?.cores || 0 }}</span>
              </div>
              <div class="flex justify-between p-2 rounded-lg bg-gray-900/30">
                <span class="text-gray-400">Takt</span>
                <span class="text-gray-200 font-medium">{{ (mateStats[mate.mateId].cpu?.mhz || 0).toFixed(0) }} MHz</span>
              </div>
            </div>

            <!-- CPU Cores -->
            <div class="mt-3 pt-3 border-t border-gray-700/50">
              <div class="text-xs font-medium text-gray-400 mb-2 flex items-center gap-1.5">
                <BoltIcon class="w-3.5 h-3.5" />
                Kern-Auslastung
              </div>
              <div class="space-y-1.5">
                <div
                  v-for="(usage, index) in mateStats[mate.mateId].cpu?.per_core || []"
                  :key="index"
                  class="flex items-center gap-2"
                >
                  <span class="text-xs text-gray-400 w-12">Core {{ index }}</span>
                  <div class="flex-1 bg-gray-700/50 rounded-full h-2 overflow-hidden">
                    <div
                      class="h-2 rounded-full transition-all duration-300"
                      :class="getCpuBarColor(usage)"
                      :style="{ width: Math.min(usage, 100) + '%' }"
                    ></div>
                  </div>
                  <span class="text-xs font-medium w-12 text-right" :class="getCpuTextColor(usage)">
                    {{ usage.toFixed(1) }}%
                  </span>
                  <span class="text-xs w-10 text-right" :class="getTempTextColor(getCoreTemp(mate.mateId, index))">
                    {{ getCoreTemp(mate.mateId, index) }}°C
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Memory -->
          <div class="
            bg-gradient-to-br from-gray-800/50 to-gray-900/50
            backdrop-blur-sm
            p-4 rounded-xl
            border border-gray-700/50
            shadow-lg
          ">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <CircleStackIcon class="w-5 h-5 text-blue-400" />
                <span class="text-sm font-medium text-gray-300">RAM</span>
              </div>
              <span class="text-sm font-bold text-blue-500 bg-blue-500/10 px-2.5 py-1 rounded-lg">
                {{ mateStats[mate.mateId].memory?.used_percent?.toFixed(1) || '0.0' }}%
              </span>
            </div>
            <div class="w-full bg-gray-700/50 rounded-full h-2.5 shadow-inner overflow-hidden mb-2">
              <div
                class="bg-gradient-to-r from-blue-500 to-cyan-500 h-2.5 rounded-full transition-all duration-500"
                :style="{ width: Math.min(mateStats[mate.mateId].memory?.used_percent || 0, 100) + '%' }"
              ></div>
            </div>
            <div class="text-xs text-gray-400">
              {{ formatBytes(mateStats[mate.mateId].memory?.used) }} /
              {{ formatBytes(mateStats[mate.mateId].memory?.total) }}
            </div>
          </div>

          <!-- GPU -->
          <div
            v-for="gpu in mateStats[mate.mateId].gpu || []"
            :key="gpu.index"
            class="
              bg-gradient-to-br from-purple-900/30 to-pink-900/30
              backdrop-blur-sm
              p-4 rounded-xl
              border border-purple-500/30
              shadow-lg
            "
          >
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <svg class="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
                </svg>
                <span class="text-sm font-medium text-gray-300">GPU {{ gpu.index }}</span>
              </div>
              <span class="text-xs font-bold text-purple-400 bg-purple-500/10 px-2.5 py-1 rounded-lg">
                {{ gpu.temperature?.toFixed(0) || '0' }}°C
              </span>
            </div>

            <!-- GPU Model -->
            <div class="text-xs text-gray-400 mb-3 truncate">
              {{ gpu.name }}
            </div>

            <!-- GPU Utilization -->
            <div class="mb-3">
              <div class="flex justify-between text-xs mb-1.5">
                <span class="text-gray-400">GPU Auslastung</span>
                <span class="font-bold" :class="getGpuTextColor(gpu.utilization_gpu)">
                  {{ gpu.utilization_gpu?.toFixed(1) || '0.0' }}%
                </span>
              </div>
              <div class="w-full bg-gray-700/50 rounded-full h-2.5 shadow-inner overflow-hidden">
                <div
                  class="h-2.5 rounded-full transition-all duration-500"
                  :class="getGpuBarColor(gpu.utilization_gpu)"
                  :style="{ width: Math.min(gpu.utilization_gpu || 0, 100) + '%' }"
                ></div>
              </div>
            </div>

            <!-- VRAM -->
            <div>
              <div class="flex justify-between text-xs mb-1.5">
                <span class="text-gray-400">VRAM</span>
                <span class="font-bold" :class="getVramTextColor(gpu.memory_used_percent)">
                  {{ gpu.memory_used_percent?.toFixed(1) || '0.0' }}%
                </span>
              </div>
              <div class="w-full bg-gray-700/50 rounded-full h-2.5 shadow-inner overflow-hidden mb-2">
                <div
                  class="bg-gradient-to-r from-purple-500 to-pink-500 h-2.5 rounded-full transition-all duration-500"
                  :style="{ width: Math.min(gpu.memory_used_percent || 0, 100) + '%' }"
                ></div>
              </div>
              <div class="text-xs text-gray-400">
                {{ (gpu.memory_used / 1024).toFixed(1) }} GB / {{ (gpu.memory_total / 1024).toFixed(1) }} GB
              </div>
            </div>
          </div>

          <!-- Disk -->
          <div
            v-for="disk in mateStats[mate.mateId].disk || []"
            :key="disk.mount_point"
            class="
              bg-gradient-to-br from-gray-800/50 to-gray-900/50
              backdrop-blur-sm
              p-4 rounded-xl
              border border-gray-700/50
              shadow-lg
            "
          >
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <CircleStackIcon class="w-5 h-5 text-purple-400" />
                <span class="text-sm font-medium text-gray-300">Disk {{ disk.mount_point }}</span>
              </div>
              <span class="text-sm font-bold text-purple-500 bg-purple-500/10 px-2.5 py-1 rounded-lg">
                {{ disk.used_percent?.toFixed(1) || '0.0' }}%
              </span>
            </div>
            <div class="w-full bg-gray-700/50 rounded-full h-2.5 shadow-inner overflow-hidden mb-2">
              <div
                class="bg-gradient-to-r from-purple-500 to-pink-500 h-2.5 rounded-full transition-all duration-500"
                :style="{ width: Math.min(disk.used_percent || 0, 100) + '%' }"
              ></div>
            </div>
            <div class="space-y-1 text-xs text-gray-400">
              <div>{{ formatBytes(disk.used) }} / {{ formatBytes(disk.total) }}</div>
              <div class="flex justify-between">
                <span>{{ disk.device }}</span>
                <span>{{ disk.fs_type }}</span>
              </div>
            </div>
          </div>

          <!-- Temperature -->
          <div
            v-if="getCpuPackageTemp(mate.mateId)"
            class="
              bg-gradient-to-br from-gray-800/50 to-gray-900/50
              backdrop-blur-sm
              p-4 rounded-xl
              border border-gray-700/50
              shadow-lg
            "
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <FireIcon class="w-5 h-5 text-orange-400" />
                <span class="text-sm font-medium text-gray-300">CPU Package Temp</span>
              </div>
              <span
                class="text-sm font-bold px-2.5 py-1 rounded-lg"
                :class="getTempBadgeColor(getCpuPackageTemp(mate.mateId))"
              >
                {{ getCpuPackageTemp(mate.mateId) }}°C
              </span>
            </div>
          </div>
        </div>

        <!-- Loading Stats -->
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
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import {
  XMarkIcon,
  ServerIcon,
  CheckCircleIcon,
  XCircleIcon,
  ArrowPathIcon,
  CpuChipIcon,
  CircleStackIcon,
  BoltIcon,
  FireIcon,
  ComputerDesktopIcon
} from '@heroicons/vue/24/outline'
import axios from 'axios'

defineEmits(['close'])

const mates = ref([])
const mateStats = ref({})
const dbSize = ref({ sizeBytes: 0, formatted: '...' })
const dbSizeHistory = ref([])
const isRefreshing = ref(false)
let statsIntervalId = null
let dbSizeIntervalId = null

onMounted(async () => {
  await loadDbSize()
  await loadDbSizeHistory()
  await loadMates()
  await loadAllStats()
  // Auto-refresh hardware stats every 5 seconds
  statsIntervalId = setInterval(async () => {
    await loadMates()
    await loadAllStats()
  }, 5000)
  // Auto-refresh DB size every 30 minutes (matches backend scheduled job)
  dbSizeIntervalId = setInterval(async () => {
    await loadDbSize()
    await loadDbSizeHistory()
  }, 30 * 60 * 1000)
})

onUnmounted(() => {
  if (statsIntervalId) clearInterval(statsIntervalId)
  if (dbSizeIntervalId) clearInterval(dbSizeIntervalId)
})

async function loadDbSize() {
  try {
    const response = await axios.get('/api/system/db-size')
    dbSize.value = response.data
  } catch (error) {
    console.debug('Failed to load database size:', error)
  }
}

async function loadDbSizeHistory() {
  try {
    const response = await axios.get('/api/system/db-size/history')
    dbSizeHistory.value = response.data
  } catch (error) {
    console.debug('Failed to load database size history:', error)
  }
}

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
    // Nur für Hardware-Monitoring Mates (nicht Email-Mates!)
    // Email-Mates haben keine lastStatsUpdate
    if (mate.status === 'ONLINE' && mate.lastStatsUpdate != null) {
      try {
        const response = await axios.get(`/api/fleet-mate/mates/${mate.mateId}/stats`)
        mateStats.value[mate.mateId] = response.data
      } catch (error) {
        console.debug(`Failed to load stats for ${mate.mateId}:`, error)
      }
    }
  }
}

async function refreshAllData() {
  isRefreshing.value = true
  try {
    await loadDbSize()
    await loadDbSizeHistory()
    await loadMates()
    await loadAllStats()
  } finally {
    setTimeout(() => {
      isRefreshing.value = false
    }, 500)
  }
}

function formatBytes(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

function formatUptime(seconds) {
  if (!seconds) return 'N/A'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  if (days > 0) return `${days}d ${hours}h`
  return `${hours}h ${Math.floor((seconds % 3600) / 60)}m`
}

function getCpuModel(model) {
  if (!model) return 'N/A'
  // Kürze lange Intel/AMD Namen
  return model.replace(/\(R\)|\(TM\)|CPU|Processor/g, '').trim()
}

function getCpuBarColor(usage) {
  if (usage < 50) return 'bg-gradient-to-r from-green-500 to-emerald-500'
  if (usage < 75) return 'bg-gradient-to-r from-yellow-500 to-orange-500'
  return 'bg-gradient-to-r from-red-500 to-rose-500'
}

function getCpuTextColor(usage) {
  if (usage < 50) return 'text-green-400'
  if (usage < 75) return 'text-yellow-400'
  return 'text-red-400'
}

function getTempTextColor(temp) {
  if (!temp || temp === 'N/A') return 'text-gray-500'
  const tempNum = parseInt(temp)
  if (tempNum < 60) return 'text-green-400'
  if (tempNum < 80) return 'text-yellow-400'
  return 'text-red-400'
}

function getTempBadgeColor(temp) {
  if (!temp || temp === 'N/A') return 'bg-gray-500/20 text-gray-400'
  const tempNum = parseInt(temp)
  if (tempNum < 60) return 'bg-green-500/20 text-green-400'
  if (tempNum < 80) return 'bg-yellow-500/20 text-yellow-400'
  return 'bg-red-500/20 text-red-400'
}

function getCoreTemp(mateId, coreIndex) {
  const sensors = mateStats.value[mateId]?.temperature?.sensors || []
  const coreSensor = sensors.find(s => s.name === `coretemp_core_${coreIndex}`)
  return coreSensor ? coreSensor.temperature.toFixed(0) : 'N/A'
}

function getCpuPackageTemp(mateId) {
  const sensors = mateStats.value[mateId]?.temperature?.sensors || []
  const packageSensor = sensors.find(s => s.name && s.name.includes('coretemp_package'))
  return packageSensor ? packageSensor.temperature.toFixed(0) : null
}

function getGpuTextColor(usage) {
  if (!usage) return 'text-gray-400'
  if (usage < 50) return 'text-green-400'
  if (usage < 75) return 'text-yellow-400'
  return 'text-red-400'
}

function getGpuBarColor(usage) {
  if (!usage) return 'bg-gray-600'
  if (usage < 50) return 'bg-gradient-to-r from-green-500 to-emerald-500'
  if (usage < 75) return 'bg-gradient-to-r from-yellow-500 to-orange-500'
  return 'bg-gradient-to-r from-red-500 to-rose-500'
}

function getVramTextColor(usage) {
  if (!usage) return 'text-gray-400'
  if (usage < 70) return 'text-purple-400'
  if (usage < 85) return 'text-yellow-400'
  return 'text-red-400'
}
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(17, 24, 39, 0.5);
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: linear-gradient(to bottom, rgb(249, 115, 22), rgb(234, 88, 12));
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(to bottom, rgb(251, 146, 60), rgb(249, 115, 22));
}
</style>
