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
    <!-- Header with Glassmorphism -->
    <div class="sticky top-0 z-10 bg-gray-900/80 backdrop-blur-xl border-b border-gray-700/50 p-4 mb-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <div class="p-2 rounded-lg bg-gradient-to-br from-fleet-orange-500 to-orange-600 shadow-lg">
            <ChartBarIcon class="w-5 h-5 text-white" />
          </div>
          <h3 class="text-lg font-bold bg-gradient-to-r from-white to-gray-300 bg-clip-text text-transparent">
            System Monitor
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
      <!-- Ollama Status -->
      <Transition name="fade" mode="out-in">
        <div class="
          bg-gradient-to-br from-gray-800/50 to-gray-900/50
          backdrop-blur-sm
          p-4 rounded-xl
          border border-gray-700/50
          shadow-lg
          hover:shadow-xl
          transition-all duration-200
        ">
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-2">
              <ServerIcon class="w-5 h-5 text-fleet-orange-400" />
              <span class="text-sm font-medium text-gray-300">Ollama Status</span>
            </div>
            <Transition name="badge" mode="out-in">
              <span
                :key="status.ollamaAvailable"
                class="
                  px-3 py-1 rounded-full text-xs font-semibold
                  shadow-lg
                  transition-all duration-300
                  flex items-center gap-1.5
                "
                :class="status.ollamaAvailable
                  ? 'bg-gradient-to-r from-green-500 to-emerald-500 text-white'
                  : 'bg-gradient-to-r from-red-500 to-rose-500 text-white'"
              >
                <component
                  :is="status.ollamaAvailable ? CheckCircleIcon : XCircleIcon"
                  class="w-3 h-3"
                />
                {{ status.ollamaAvailable ? 'Online' : 'Offline' }}
              </span>
            </Transition>
          </div>
          <Transition name="fade">
            <div v-if="status.ollamaVersion" class="flex items-center gap-2 text-xs text-gray-400">
              <InformationCircleIcon class="w-4 h-4" />
              <span>Version: {{ status.ollamaVersion }}</span>
            </div>
          </Transition>
        </div>
      </Transition>

      <!-- Memory Usage -->
      <div class="
        bg-gradient-to-br from-gray-800/50 to-gray-900/50
        backdrop-blur-sm
        p-4 rounded-xl
        border border-gray-700/50
        shadow-lg
        hover:shadow-xl
        transition-all duration-200
      ">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <CircleStackIcon class="w-5 h-5 text-fleet-orange-400" />
            <span class="text-sm font-medium text-gray-300">Memory Usage</span>
          </div>
          <span class="text-sm font-bold text-fleet-orange-500 bg-fleet-orange-500/10 px-2.5 py-1 rounded-lg">
            {{ chatStore.memoryUsagePercent }}%
          </span>
        </div>
        <div class="w-full bg-gray-700/50 rounded-full h-2.5 shadow-inner overflow-hidden">
          <div
            class="
              bg-gradient-to-r from-fleet-orange-500 to-orange-600
              h-2.5 rounded-full
              transition-all duration-500 ease-out
              relative
            "
            :style="{ width: chatStore.memoryUsagePercent + '%' }"
          >
            <div class="absolute inset-0 bg-white/20 animate-pulse"></div>
          </div>
        </div>
        <div class="mt-2 flex items-center gap-1.5 text-xs text-gray-400">
          <InformationCircleIcon class="w-3.5 h-3.5" />
          <span>{{ formatBytes(status.usedMemory) }} / {{ formatBytes(status.totalMemory) }}</span>
        </div>
      </div>

      <!-- Hardware Info -->
      <div class="
        bg-gradient-to-br from-gray-800/50 to-gray-900/50
        backdrop-blur-sm
        p-4 rounded-xl
        border border-gray-700/50
        shadow-lg
        hover:shadow-xl
        transition-all duration-200
      ">
        <div class="flex items-center gap-2 mb-3">
          <CpuChipIcon class="w-5 h-5 text-fleet-orange-400" />
          <span class="text-sm font-medium text-gray-300">Hardware</span>
        </div>
        <div class="space-y-2.5 text-xs">
          <div class="flex justify-between items-center p-2 rounded-lg bg-gray-900/30 hover:bg-gray-900/50 transition-colors">
            <span class="text-gray-400">CPU</span>
            <span class="text-gray-200 font-medium">{{ status.cpuModel || 'Unknown' }}</span>
          </div>
          <div class="flex justify-between items-center p-2 rounded-lg bg-gray-900/30 hover:bg-gray-900/50 transition-colors">
            <span class="text-gray-400">Frequenz</span>
            <span class="text-gray-200 font-medium">{{ status.cpuFrequency || 'Unknown' }}</span>
          </div>
          <div class="flex justify-between items-center p-2 rounded-lg bg-gray-900/30 hover:bg-gray-900/50 transition-colors">
            <span class="text-gray-400">Kerne</span>
            <span class="text-gray-200 font-medium">{{ status.cpuCores || 'Unknown' }}</span>
          </div>
        </div>
      </div>

      <!-- CPU Usage -->
      <div class="
        bg-gradient-to-br from-gray-800/50 to-gray-900/50
        backdrop-blur-sm
        p-4 rounded-xl
        border border-gray-700/50
        shadow-lg
        hover:shadow-xl
        transition-all duration-200
      ">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <BoltIcon class="w-5 h-5 text-fleet-orange-400" />
            <span class="text-sm font-medium text-gray-300">CPU Load (System)</span>
          </div>
          <span class="text-sm font-bold text-fleet-orange-500 bg-fleet-orange-500/10 px-2.5 py-1 rounded-lg">
            {{ status.cpuUsage ? status.cpuUsage.toFixed(1) : '0.0' }}%
          </span>
        </div>
        <div class="w-full bg-gray-700/50 rounded-full h-2.5 shadow-inner overflow-hidden">
          <div
            class="
              bg-gradient-to-r from-fleet-orange-500 to-orange-600
              h-2.5 rounded-full
              transition-all duration-500 ease-out
              relative
            "
            :style="{ width: Math.min(status.cpuUsage, 100) + '%' }"
          >
            <div class="absolute inset-0 bg-white/20 animate-pulse"></div>
          </div>
        </div>

        <!-- Process CPU -->
        <div class="mt-4 pt-3 border-t border-gray-700/50">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs text-gray-400 flex items-center gap-1.5">
              <RocketLaunchIcon class="w-3.5 h-3.5" />
              Fleet Navigator Prozess
            </span>
            <span class="text-xs font-bold text-fleet-orange-400 bg-fleet-orange-400/10 px-2 py-0.5 rounded">
              {{ status.processCpuUsage ? status.processCpuUsage.toFixed(1) : '0.0' }}%
            </span>
          </div>
          <div class="w-full bg-gray-700/50 rounded-full h-2 shadow-inner overflow-hidden">
            <div
              class="
                bg-gradient-to-r from-fleet-orange-400 to-orange-500
                h-2 rounded-full
                transition-all duration-500 ease-out
              "
              :style="{ width: Math.min(status.processCpuUsage, 100) + '%' }"
            ></div>
          </div>
        </div>
      </div>

      <!-- System Memory (Physical RAM) -->
      <Transition name="fade">
        <div v-if="status.systemTotalMemory" class="
          bg-gradient-to-br from-gray-800/50 to-gray-900/50
          backdrop-blur-sm
          p-4 rounded-xl
          border border-gray-700/50
          shadow-lg
          hover:shadow-xl
          transition-all duration-200
        ">
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-2">
              <CircleStackIcon class="w-5 h-5 text-blue-400" />
              <span class="text-sm font-medium text-gray-300">System RAM</span>
            </div>
            <span class="text-sm font-bold text-blue-500 bg-blue-500/10 px-2.5 py-1 rounded-lg">
              {{ systemMemoryUsagePercent }}%
            </span>
          </div>
          <div class="w-full bg-gray-700/50 rounded-full h-2.5 shadow-inner overflow-hidden">
            <div
              class="
                bg-gradient-to-r from-blue-500 to-cyan-500
                h-2.5 rounded-full
                transition-all duration-500 ease-out
              "
              :style="{ width: systemMemoryUsagePercent + '%' }"
            ></div>
          </div>
          <div class="mt-2 flex items-center gap-1.5 text-xs text-gray-400">
            <InformationCircleIcon class="w-3.5 h-3.5" />
            <span>{{ formatBytes(status.systemTotalMemory - status.systemFreeMemory) }} / {{ formatBytes(status.systemTotalMemory) }}</span>
          </div>
        </div>
      </Transition>

      <!-- GPU Info -->
      <Transition name="fade" mode="out-in">
        <div v-if="status.gpuName && status.gpuName !== 'No GPU detected' && status.gpuName !== 'Unknown'" class="
          bg-gradient-to-br from-purple-900/20 to-pink-900/20
          backdrop-blur-sm
          p-4 rounded-xl
          border border-purple-700/30
          shadow-lg
          hover:shadow-xl hover:shadow-purple-500/10
          transition-all duration-200
        ">
          <div class="flex items-center gap-2 mb-3">
            <div class="p-2 rounded-lg bg-gradient-to-br from-purple-500 to-pink-500">
              <CpuChipIcon class="w-4 h-4 text-white" />
            </div>
            <div>
              <div class="text-sm font-medium text-gray-200">GPU</div>
              <div class="text-xs text-gray-400 truncate max-w-[220px]">
                {{ status.gpuName }}
              </div>
            </div>
          </div>

          <!-- GPU Utilization -->
          <div class="space-y-3">
            <div>
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs text-gray-400 flex items-center gap-1.5">
                  <BoltIcon class="w-3.5 h-3.5" />
                  GPU Auslastung
                </span>
                <span class="text-xs font-bold text-purple-400 bg-purple-400/10 px-2 py-0.5 rounded">
                  {{ status.gpuUtilization ? status.gpuUtilization.toFixed(1) : '0.0' }}%
                </span>
              </div>
              <div class="w-full bg-gray-700/50 rounded-full h-2 shadow-inner overflow-hidden">
                <div
                  class="
                    bg-gradient-to-r from-purple-500 to-pink-500
                    h-2 rounded-full
                    transition-all duration-500 ease-out
                  "
                  :style="{ width: Math.min(status.gpuUtilization || 0, 100) + '%' }"
                ></div>
              </div>
            </div>

            <!-- VRAM -->
            <div>
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs text-gray-400 flex items-center gap-1.5">
                  <CircleStackIcon class="w-3.5 h-3.5" />
                  VRAM
                </span>
                <span class="text-xs font-bold text-purple-400 bg-purple-400/10 px-2 py-0.5 rounded">
                  {{ gpuMemoryUsagePercent }}%
                </span>
              </div>
              <div class="w-full bg-gray-700/50 rounded-full h-2 shadow-inner overflow-hidden">
                <div
                  class="
                    bg-gradient-to-r from-purple-500 to-pink-500
                    h-2 rounded-full
                    transition-all duration-500 ease-out
                  "
                  :style="{ width: gpuMemoryUsagePercent + '%' }"
                ></div>
              </div>
              <div class="mt-1 flex items-center gap-1.5 text-xs text-gray-400">
                <InformationCircleIcon class="w-3.5 h-3.5" />
                <span>{{ formatBytes(status.gpuMemoryUsed) }} / {{ formatBytes(status.gpuMemoryTotal) }}</span>
              </div>
            </div>

            <!-- GPU Temperature -->
            <Transition name="fade">
              <div v-if="status.gpuTemperature && status.gpuTemperature > 0" class="
                flex items-center justify-between p-2 rounded-lg
                bg-gray-900/30 border border-gray-700/30
              ">
                <span class="text-xs text-gray-400 flex items-center gap-1.5">
                  <FireIcon class="w-3.5 h-3.5" />
                  Temperatur
                </span>
                <span class="text-xs font-bold px-2 py-0.5 rounded" :class="[
                  getTemperatureColor(status.gpuTemperature),
                  'bg-opacity-10'
                ]">
                  {{ status.gpuTemperature.toFixed(0) }}°C
                </span>
              </div>
            </Transition>
          </div>
        </div>

        <!-- No GPU detected -->
        <div v-else class="
          bg-gradient-to-br from-gray-800/30 to-gray-900/30
          backdrop-blur-sm
          p-4 rounded-xl
          border border-gray-700/30 border-dashed
          text-center
        ">
          <CpuChipIcon class="w-8 h-8 text-gray-600 mx-auto mb-2" />
          <div class="text-sm font-medium text-gray-400 mb-1">GPU / VRAM</div>
          <div class="text-xs text-gray-500 italic">
            Keine GPU erkannt
          </div>
        </div>
      </Transition>

      <!-- OS Info -->
      <Transition name="fade">
        <div v-if="status.osName" class="
          bg-gradient-to-br from-gray-800/50 to-gray-900/50
          backdrop-blur-sm
          p-4 rounded-xl
          border border-gray-700/50
          shadow-lg
          hover:shadow-xl
          transition-all duration-200
        ">
          <div class="flex items-center gap-2 mb-2">
            <ComputerDesktopIcon class="w-5 h-5 text-fleet-orange-400" />
            <span class="text-sm font-medium text-gray-300">Betriebssystem</span>
          </div>
          <div class="text-xs text-gray-200 font-medium p-2 rounded-lg bg-gray-900/30">
            {{ status.osName }} {{ status.osVersion }}
          </div>
        </div>
      </Transition>

      <!-- Stats -->
      <div class="
        bg-gradient-to-br from-gray-800/50 to-gray-900/50
        backdrop-blur-sm
        p-4 rounded-xl
        border border-gray-700/50
        shadow-lg
        hover:shadow-xl
        transition-all duration-200
      ">
        <div class="flex items-center gap-2 mb-4">
          <ChartBarSquareIcon class="w-5 h-5 text-fleet-orange-400" />
          <h4 class="text-sm font-semibold text-gray-200">Session Stats</h4>
        </div>
        <div class="space-y-3 text-sm">
          <div class="flex justify-between items-center p-2 rounded-lg bg-gray-900/30 hover:bg-gray-900/50 transition-colors">
            <span class="text-gray-400 flex items-center gap-2">
              <CpuChipIcon class="w-4 h-4" />
              Total Tokens
            </span>
            <span class="text-fleet-orange-500 font-bold bg-fleet-orange-500/10 px-2.5 py-1 rounded-lg">
              {{ formatNumber(chatStore.globalStats.totalTokens) }}
            </span>
          </div>
          <div class="flex justify-between items-center p-2 rounded-lg bg-gray-900/30 hover:bg-gray-900/50 transition-colors">
            <span class="text-gray-400 flex items-center gap-2">
              <ChatBubbleLeftRightIcon class="w-4 h-4" />
              Messages
            </span>
            <span class="text-fleet-orange-500 font-bold bg-fleet-orange-500/10 px-2.5 py-1 rounded-lg">
              {{ chatStore.globalStats.totalMessages }}
            </span>
          </div>
          <div class="flex justify-between items-center p-2 rounded-lg bg-gray-900/30 hover:bg-gray-900/50 transition-colors">
            <span class="text-gray-400 flex items-center gap-2">
              <FolderIcon class="w-4 h-4" />
              Chats
            </span>
            <span class="text-fleet-orange-500 font-bold bg-fleet-orange-500/10 px-2.5 py-1 rounded-lg">
              {{ chatStore.globalStats.chatCount }}
            </span>
          </div>
        </div>
      </div>

      <!-- Refresh Button -->
      <button
        @click="refreshStatus"
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
        <ArrowPathIcon
          class="w-5 h-5"
          :class="{ 'animate-spin': isRefreshing }"
        />
        <span>{{ isRefreshing ? 'Aktualisiere...' : 'Aktualisieren' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  XMarkIcon,
  ChartBarIcon,
  ServerIcon,
  CircleStackIcon,
  CpuChipIcon,
  BoltIcon,
  InformationCircleIcon,
  CheckCircleIcon,
  XCircleIcon,
  RocketLaunchIcon,
  FireIcon,
  ComputerDesktopIcon,
  ChartBarSquareIcon,
  ChatBubbleLeftRightIcon,
  FolderIcon,
  ArrowPathIcon
} from '@heroicons/vue/24/outline'
import { useChatStore } from '../stores/chatStore'

defineEmits(['close'])

const chatStore = useChatStore()
const status = ref(chatStore.systemStatus)
const isRefreshing = ref(false)

let intervalId = null

// Calculate system memory usage percentage
const systemMemoryUsagePercent = computed(() => {
  if (!status.value.systemTotalMemory || !status.value.systemFreeMemory) {
    return 0
  }
  const used = status.value.systemTotalMemory - status.value.systemFreeMemory
  return Math.round((used / status.value.systemTotalMemory) * 100)
})

// Calculate GPU memory usage percentage
const gpuMemoryUsagePercent = computed(() => {
  if (!status.value.gpuMemoryTotal || !status.value.gpuMemoryUsed) {
    return 0
  }
  return Math.round((status.value.gpuMemoryUsed / status.value.gpuMemoryTotal) * 100)
})

// Get temperature color based on value
function getTemperatureColor(temp) {
  if (temp < 60) return 'text-green-400 bg-green-500/10'
  if (temp < 75) return 'text-yellow-400 bg-yellow-500/10'
  if (temp < 85) return 'text-orange-400 bg-orange-500/10'
  return 'text-red-400 bg-red-500/10'
}

onMounted(async () => {
  await refreshStatus()
  // Auto-refresh every 10 seconds
  intervalId = setInterval(refreshStatus, 10000)
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})

async function refreshStatus() {
  isRefreshing.value = true
  try {
    await chatStore.loadSystemStatus()
    status.value = chatStore.systemStatus
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

function formatNumber(num) {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}
</script>

<style scoped>
/* Custom Scrollbar */
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

/* Fade Transition */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Badge Transition */
.badge-enter-active,
.badge-leave-active {
  transition: all 0.2s ease;
}

.badge-enter-from {
  opacity: 0;
  transform: scale(0.8);
}

.badge-leave-to {
  opacity: 0;
  transform: scale(1.2);
}
</style>
