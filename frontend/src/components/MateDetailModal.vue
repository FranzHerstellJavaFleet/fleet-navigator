<template>
  <Transition name="modal">
    <div v-if="mate" class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div class="
        bg-gray-900
        rounded-2xl shadow-2xl
        w-full max-w-6xl max-h-[90vh]
        border border-gray-700/50
        flex flex-col
        transform transition-all duration-300
      ">
        <!-- Header -->
        <div class="
          flex items-center justify-between p-6
          bg-gradient-to-r from-fleet-orange-500/20 to-orange-600/20
          border-b border-gray-700/50
          rounded-t-2xl
        ">
          <div class="flex items-center gap-4">
            <div class="p-3 rounded-xl bg-gradient-to-br from-fleet-orange-500 to-orange-600 shadow-lg">
              <ServerIcon class="w-7 h-7 text-white" />
            </div>
            <div>
              <h2 class="text-2xl font-bold text-white">{{ mate.name }}</h2>
              <div class="flex items-center gap-3 mt-1">
                <p class="text-sm text-gray-400">{{ mate.description }}</p>
                <span
                  class="px-2 py-1 rounded-full text-xs font-semibold flex items-center gap-1"
                  :class="mate.status === 'ONLINE'
                    ? 'bg-green-500/20 text-green-400 border border-green-500/30'
                    : 'bg-red-500/20 text-red-400 border border-red-500/30'"
                >
                  <component :is="mate.status === 'ONLINE' ? CheckCircleIcon : XCircleIcon" class="w-3 h-3" />
                  {{ mate.status }}
                </span>
              </div>
            </div>
          </div>
          <button
            @click="$emit('close')"
            class="
              p-2 rounded-lg
              text-gray-400 hover:text-gray-300
              hover:bg-gray-800
              transition-all duration-200
              transform hover:scale-110 active:scale-95
            "
          >
            <XMarkIcon class="w-6 h-6" />
          </button>
        </div>

        <!-- Tabs -->
        <div class="flex gap-2 px-6 pt-4 border-b border-gray-700/50 bg-gray-900/50">
          <button
            @click="activeTab = 'hardware'"
            class="
              px-4 py-3 rounded-t-lg font-semibold text-sm
              transition-all duration-200
              flex items-center gap-2
            "
            :class="activeTab === 'hardware'
              ? 'bg-gray-800 text-white border-t-2 border-fleet-orange-500'
              : 'text-gray-400 hover:text-gray-300 hover:bg-gray-800/50'"
          >
            <CpuChipIcon class="w-5 h-5" />
            Hardware Monitor
          </button>
          <button
            @click="activeTab = 'terminal'"
            class="
              px-4 py-3 rounded-t-lg font-semibold text-sm
              transition-all duration-200
              flex items-center gap-2
            "
            :class="activeTab === 'terminal'
              ? 'bg-gray-800 text-white border-t-2 border-fleet-orange-500'
              : 'text-gray-400 hover:text-gray-300 hover:bg-gray-800/50'"
          >
            <SparklesIcon class="w-5 h-5" />
            AI Log-Analyse
          </button>
          <button
            @click="activeTab = 'remote'"
            class="
              px-4 py-3 rounded-t-lg font-semibold text-sm
              transition-all duration-200
              flex items-center gap-2
            "
            :class="activeTab === 'remote'
              ? 'bg-gray-800 text-white border-t-2 border-fleet-orange-500'
              : 'text-gray-400 hover:text-gray-300 hover:bg-gray-800/50'"
          >
            <CommandLineIcon class="w-5 h-5" />
            Remote Terminal
          </button>
        </div>

        <!-- Content Area -->
        <div class="flex-1 overflow-y-auto p-6">
          <!-- Hardware Tab -->
          <div v-if="activeTab === 'hardware'">
            <div v-if="stats && mate.status === 'ONLINE'" class="space-y-4">
              <!-- System Info -->
              <div class="bg-gradient-to-br from-gray-800/50 to-gray-900/50 p-5 rounded-xl border border-gray-700/50">
                <h3 class="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                  <ComputerDesktopIcon class="w-5 h-5 text-blue-400" />
                  System-Information
                </h3>
                <div class="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span class="text-gray-400">Hostname:</span>
                    <span class="text-white ml-2 font-medium">{{ stats.system?.hostname || 'N/A' }}</span>
                  </div>
                  <div>
                    <span class="text-gray-400">OS:</span>
                    <span class="text-white ml-2 font-medium">
                      {{ (stats.system?.platform || 'Linux').charAt(0).toUpperCase() + (stats.system?.platform || 'Linux').slice(1) }}
                      {{ stats.system?.platform_version || '' }}
                    </span>
                  </div>
                  <div>
                    <span class="text-gray-400">Kernel:</span>
                    <span class="text-white ml-2 font-medium">{{ stats.system?.kernel_version || 'N/A' }}</span>
                  </div>
                  <div>
                    <span class="text-gray-400">Uptime:</span>
                    <span class="text-white ml-2 font-medium">{{ formatUptime(stats.system?.uptime) }}</span>
                  </div>
                </div>
              </div>

              <!-- CPU -->
              <div class="bg-gradient-to-br from-gray-800/50 to-gray-900/50 p-5 rounded-xl border border-gray-700/50">
                <h3 class="text-lg font-semibold text-white mb-4">CPU</h3>
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4 text-sm">
                  <div>
                    <span class="text-gray-400">Modell:</span>
                    <span class="text-white ml-2 font-medium">{{ shortenCPUModel(stats.cpu?.model) }}</span>
                  </div>
                  <div>
                    <span class="text-gray-400">Kerne:</span>
                    <span class="text-white ml-2 font-medium">{{ stats.cpu?.cores || 0 }}</span>
                  </div>
                  <div>
                    <span class="text-gray-400">Takt:</span>
                    <span class="text-white ml-2 font-medium">{{ stats.cpu?.mhz?.toFixed(0) || 0 }} MHz</span>
                  </div>
                  <div>
                    <span class="text-gray-400">Auslastung:</span>
                    <span class="text-white ml-2 font-bold" :class="getCPUColor(stats.cpu?.usage_percent)">
                      {{ stats.cpu?.usage_percent?.toFixed(1) || 0 }}%
                    </span>
                  </div>
                </div>

                <!-- Per-Core CPU Usage -->
                <div v-if="stats.cpu?.per_core && stats.cpu.per_core.length > 0" class="space-y-2">
                  <h4 class="text-sm font-semibold text-gray-300 mb-2">Pro-Kern Auslastung</h4>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
                    <div v-for="(usage, index) in stats.cpu.per_core" :key="index" class="flex items-center gap-2">
                      <span class="text-xs text-gray-400 w-16">Core {{ index }}</span>
                      <div class="flex-1 h-6 bg-gray-700 rounded-lg overflow-hidden">
                        <div
                          class="h-full transition-all duration-300"
                          :class="getCPUBarColor(usage)"
                          :style="{ width: usage + '%' }"
                        />
                      </div>
                      <span class="text-xs font-semibold w-12 text-right" :class="getCPUColor(usage)">
                        {{ usage?.toFixed(1) }}%
                      </span>
                      <span
                        v-if="getCoreTemp(index)"
                        class="text-xs font-semibold px-2 py-0.5 rounded-full w-12 text-center"
                        :class="getTempBadgeColor(getCoreTemp(index))"
                      >
                        {{ getCoreTemp(index)?.toFixed(0) }}°C
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Memory -->
              <div class="bg-gradient-to-br from-gray-800/50 to-gray-900/50 p-5 rounded-xl border border-gray-700/50">
                <h3 class="text-lg font-semibold text-white mb-4">RAM</h3>
                <div class="space-y-3">
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-400">
                      Belegt: {{ (stats.memory?.used / 1024 / 1024 / 1024).toFixed(1) }} GB /
                      {{ (stats.memory?.total / 1024 / 1024 / 1024).toFixed(1) }} GB
                    </span>
                    <span class="font-bold" :class="getRAMColor(stats.memory?.used_percent)">
                      {{ stats.memory?.used_percent?.toFixed(1) }}%
                    </span>
                  </div>
                  <div class="h-6 bg-gray-700 rounded-lg overflow-hidden">
                    <div
                      class="h-full transition-all duration-300"
                      :class="getRAMBarColor(stats.memory?.used_percent)"
                      :style="{ width: stats.memory?.used_percent + '%' }"
                    />
                  </div>
                </div>
              </div>

              <!-- GPU -->
              <div v-if="stats.gpu && stats.gpu.length > 0" v-for="gpu in stats.gpu" :key="gpu.index"
                   class="bg-gradient-to-br from-purple-900/30 to-pink-900/30 p-5 rounded-xl border border-purple-500/30">
                <div class="flex items-center justify-between mb-4">
                  <h3 class="text-lg font-semibold text-white flex items-center gap-2">
                    <svg class="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
                    </svg>
                    GPU {{ gpu.index }}
                  </h3>
                  <span class="text-xs px-2 py-1 rounded-full bg-purple-500/20 text-purple-300 border border-purple-500/30">
                    {{ gpu.temperature?.toFixed(0) }}°C
                  </span>
                </div>

                <!-- GPU Model -->
                <div class="text-sm text-gray-400 mb-4">
                  {{ gpu.name }}
                </div>

                <!-- GPU Utilization -->
                <div class="space-y-3 mb-4">
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-400">GPU Auslastung</span>
                    <span class="font-bold" :class="getGPUColor(gpu.utilization_gpu)">
                      {{ gpu.utilization_gpu?.toFixed(1) }}%
                    </span>
                  </div>
                  <div class="h-6 bg-gray-700 rounded-lg overflow-hidden">
                    <div
                      class="h-full transition-all duration-300"
                      :class="getGPUBarColor(gpu.utilization_gpu)"
                      :style="{ width: gpu.utilization_gpu + '%' }"
                    />
                  </div>
                </div>

                <!-- VRAM -->
                <div class="space-y-3">
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-400">
                      VRAM: {{ (gpu.memory_used / 1024).toFixed(1) }} GB / {{ (gpu.memory_total / 1024).toFixed(1) }} GB
                    </span>
                    <span class="font-bold" :class="getVRAMColor(gpu.memory_used_percent)">
                      {{ gpu.memory_used_percent?.toFixed(1) }}%
                    </span>
                  </div>
                  <div class="h-6 bg-gray-700 rounded-lg overflow-hidden">
                    <div
                      class="h-full transition-all duration-300"
                      :class="getVRAMBarColor(gpu.memory_used_percent)"
                      :style="{ width: gpu.memory_used_percent + '%' }"
                    />
                  </div>
                </div>
              </div>

              <!-- Temperature -->
              <div v-if="stats.temperature?.cpu_package" class="bg-gradient-to-br from-gray-800/50 to-gray-900/50 p-5 rounded-xl border border-gray-700/50">
                <h3 class="text-lg font-semibold text-white mb-4">CPU Package Temperatur</h3>
                <div class="flex items-center gap-3">
                  <FireIcon class="w-6 h-6" :class="getTempIconColor(stats.temperature.cpu_package)" />
                  <span
                    class="text-3xl font-bold px-4 py-2 rounded-lg"
                    :class="getTempBadgeColor(stats.temperature.cpu_package)"
                  >
                    {{ stats.temperature.cpu_package?.toFixed(1) }}°C
                  </span>
                </div>
              </div>
            </div>
            <div v-else-if="mate.status === 'OFFLINE'" class="
              bg-gradient-to-br from-red-500/10 to-rose-500/10
              border border-red-500/30
              p-12 rounded-xl
              text-center
            ">
              <XCircleIcon class="w-16 h-16 text-red-400 mx-auto mb-4" />
              <h3 class="text-xl font-semibold text-red-400 mb-2">Maat ist offline</h3>
              <p class="text-sm text-gray-400">
                Keine Hardware-Daten verfügbar. Starte den Maat, um Daten zu sehen.
              </p>
            </div>
            <div v-else class="
              bg-gradient-to-br from-gray-800/50 to-gray-900/50
              p-12 rounded-xl
              text-center
            ">
              <ArrowPathIcon class="w-12 h-12 text-gray-500 mx-auto mb-4 animate-spin" />
              <p class="text-sm text-gray-400">Lade Hardware-Daten...</p>
            </div>
          </div>

          <!-- Terminal Tab (AI Log Analysis) -->
          <div v-if="activeTab === 'terminal'" class="space-y-4">
            <!-- Log Analysis Form -->
            <div class="bg-gray-800/50 p-4 rounded-xl border border-gray-700/50">
              <h4 class="text-sm font-semibold text-gray-300 mb-3 flex items-center gap-2">
                <SparklesIcon class="w-4 h-4 text-yellow-400" />
                AI-gestützte Log-Analyse
              </h4>

              <div class="space-y-3">
                <!-- Log Path -->
                <div>
                  <label class="text-xs text-gray-400 block mb-1">Log-Datei</label>
                  <select v-model="logAnalysis.path" class="w-full px-3 py-2 bg-gray-700 text-white rounded-lg text-sm border border-gray-600 focus:border-fleet-orange-500 focus:outline-none">
                    <option value="/var/log/syslog">System Log (/var/log/syslog)</option>
                    <option value="/var/log/auth.log">Auth Log</option>
                    <option value="/var/log/kern.log">Kernel Log</option>
                  </select>
                </div>

                <!-- Mode -->
                <div>
                  <label class="text-xs text-gray-400 block mb-1">Analyse-Modus</label>
                  <select v-model="logAnalysis.mode" class="w-full px-3 py-2 bg-gray-700 text-white rounded-lg text-sm border border-gray-600 focus:border-fleet-orange-500 focus:outline-none">
                    <option value="smart">Smart (nur relevante Events)</option>
                    <option value="full">Vollständig (alle Zeilen)</option>
                    <option value="errors-only">Nur Errors</option>
                  </select>
                </div>

                <!-- Model -->
                <div>
                  <label class="text-xs text-gray-400 block mb-1">AI Modell</label>
                  <select
                    v-model="logAnalysis.model"
                    :disabled="loadingModels"
                    class="w-full px-3 py-2 bg-gray-700 text-white rounded-lg text-sm border border-gray-600 focus:border-fleet-orange-500 focus:outline-none disabled:opacity-50">
                    <option v-if="loadingModels" value="">Lade Modelle...</option>
                    <option v-for="model in availableModels" :key="model.name" :value="model.name">
                      {{ model.name }} {{ model.size ? `(${model.size})` : '' }}
                    </option>
                    <option v-if="!loadingModels && availableModels.length === 0" value="">Keine Modelle verfügbar</option>
                  </select>
                </div>

                <!-- Enhanced Dual-Phase Progress Display -->
                <div v-if="analyzing && readingProgress < 100" class="space-y-3 p-4 bg-gray-800 rounded-lg border-2"
                     :class="progressPhase === 'reading' ? 'border-blue-500' : 'border-orange-500'">

                  <!-- Phase Indicator with Animation -->
                  <div class="flex items-center gap-3">
                    <div class="text-3xl animate-bounce">
                      {{ progressPhase === 'reading' ? '📖' : '🤖' }}
                    </div>
                    <div class="flex-1">
                      <div class="text-sm font-bold"
                           :class="progressPhase === 'reading' ? 'text-blue-400' : 'text-orange-400'">
                        {{ progressPhase === 'reading' ? 'PHASE 1: LOG-DATEI LESEN' : 'PHASE 2: KI-ANALYSE' }}
                      </div>
                      <div class="text-xs text-gray-400 mt-1">
                        {{ progressPhase === 'reading'
                          ? 'Fleet Mate überträgt Log-Daten in Chunks...'
                          : 'KI analysiert Log mit GPU-Beschleunigung...' }}
                      </div>
                    </div>
                    <div class="text-2xl font-bold font-mono"
                         :class="progressPhase === 'reading' ? 'text-blue-400' : 'text-orange-400'">
                      {{ readingProgress }}%
                    </div>
                  </div>

                  <!-- Enhanced Progress Bar -->
                  <div class="w-full bg-gray-900 rounded-full h-4 overflow-hidden shadow-inner">
                    <div
                      class="h-full transition-all duration-300 relative"
                      :class="progressPhase === 'reading'
                        ? 'bg-gradient-to-r from-blue-500 via-blue-400 to-blue-600'
                        : 'bg-gradient-to-r from-fleet-orange-500 via-orange-400 to-orange-600'"
                      :style="{ width: readingProgress + '%' }">
                      <!-- Animated shine effect -->
                      <div class="absolute inset-0 bg-gradient-to-r from-transparent via-white to-transparent opacity-30 animate-pulse"></div>
                    </div>
                  </div>

                  <!-- Activity Indicator -->
                  <div class="flex items-center gap-2 text-xs text-gray-500">
                    <div class="flex gap-1">
                      <div class="w-2 h-2 rounded-full bg-current animate-pulse" style="animation-delay: 0ms"></div>
                      <div class="w-2 h-2 rounded-full bg-current animate-pulse" style="animation-delay: 150ms"></div>
                      <div class="w-2 h-2 rounded-full bg-current animate-pulse" style="animation-delay: 300ms"></div>
                    </div>
                    <span>System arbeitet...</span>
                  </div>
                </div>

                <!-- Start Button -->
                <button
                  @click="startLogAnalysis"
                  :disabled="analyzing || mate.status !== 'ONLINE'"
                  class="w-full px-4 py-2 rounded-lg bg-gradient-to-r from-fleet-orange-500 to-orange-600
                         hover:from-fleet-orange-400 hover:to-orange-500
                         text-white font-semibold text-sm
                         disabled:opacity-50 disabled:cursor-not-allowed
                         transition-all duration-200 transform hover:scale-105 active:scale-95
                         flex items-center justify-center gap-2"
                >
                  <SparklesIcon class="w-4 h-4" :class="{ 'animate-spin': analyzing }" />
                  {{ analyzing ? 'Analysiere...' : 'Analyse starten' }}
                </button>
              </div>
            </div>

            <!-- Terminal Output -->
            <div class="bg-black/70 p-4 rounded-xl border border-gray-700/50 font-mono text-xs min-h-[300px] max-h-[500px] overflow-y-auto">
              <div class="flex items-center justify-between mb-3 pb-2 border-b border-gray-700">
                <span class="text-green-400">fleet-mate@{{ mate.mateId }}</span>
                <span class="text-gray-500 text-xs">{{ new Date().toLocaleTimeString('de-DE') }}</span>
              </div>

              <!-- Output -->
              <div class="text-gray-300 whitespace-pre-wrap" v-html="analysisOutput"></div>

              <!-- Typing Cursor -->
              <span v-if="analyzing" class="inline-block w-2 h-4 bg-green-400 animate-pulse ml-1"></span>
            </div>

            <!-- PDF Export Button -->
            <button
              v-if="!analyzing && analysisOutput !== '$ Warte auf Analyse...\\n'"
              @click="exportAsPdf"
              class="mt-4 w-full px-4 py-2 rounded-lg bg-gradient-to-r from-red-600 to-red-700
                     hover:from-red-500 hover:to-red-600
                     text-white font-semibold text-sm
                     transition-all duration-200 transform hover:scale-105 active:scale-95
                     flex items-center justify-center gap-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              Als PDF exportieren
            </button>
          </div>

          <!-- Remote Terminal Tab -->
          <div v-if="activeTab === 'remote'">
            <MateTerminal :mateId="mate.mateId" />
          </div>
        </div>

        <!-- Footer -->
        <div class="
          flex items-center justify-between gap-3 p-6
          bg-gray-900/50
          border-t border-gray-700/50
          rounded-b-2xl
        ">
          <div class="flex items-center gap-2 text-xs text-gray-500">
            <ClockIcon class="w-4 h-4" />
            <span v-if="mate.lastHeartbeat">
              Letztes Heartbeat: {{ formatTime(mate.lastHeartbeat) }}
            </span>
          </div>
          <div class="flex gap-3">
            <button
              @click="pingMate"
              :disabled="mate.status !== 'ONLINE' || pinging"
              class="
                px-4 py-2 rounded-lg
                bg-gray-800 hover:bg-gray-700
                text-gray-300 hover:text-white
                font-medium text-sm
                border border-gray-700
                disabled:opacity-50 disabled:cursor-not-allowed
                transition-all duration-200
                flex items-center gap-2
              "
            >
              <ArrowPathIcon class="w-4 h-4" :class="{ 'animate-spin': pinging }" />
              Ping
            </button>
            <button
              @click="$emit('close')"
              class="
                px-6 py-2 rounded-lg
                bg-fleet-orange-500 hover:bg-fleet-orange-600
                text-white font-semibold text-sm
                shadow-lg
                transition-all duration-200
                transform hover:scale-105 active:scale-95
              "
            >
              Schließen
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  ServerIcon,
  XMarkIcon,
  CheckCircleIcon,
  XCircleIcon,
  CpuChipIcon,
  CommandLineIcon,
  ArrowPathIcon,
  ClockIcon,
  SparklesIcon,
  ComputerDesktopIcon,
  FireIcon
} from '@heroicons/vue/24/outline'
import axios from 'axios'
import { useToast } from '../composables/useToast'
import MateTerminal from './MateTerminal.vue'

const props = defineProps({
  mate: Object,
  stats: Object
})

const emit = defineEmits(['close'])

const activeTab = ref('hardware')
const pinging = ref(false)
const analyzing = ref(false)
const readingProgress = ref(0)
const progressPhase = ref('reading') // 'reading' or 'analyzing'
const analysisOutput = ref('$ Warte auf Analyse...\n')
const availableModels = ref([])
const loadingModels = ref(false)
const logAnalysis = ref({
  path: '/var/log/syslog',
  mode: 'smart',
  model: 'mistral:latest'
})

const { success, error } = useToast()

// Load available models (GGUF) when modal opens
async function loadAvailableModels() {
  loadingModels.value = true
  try {
    const response = await axios.get('/api/models')
    console.log('📥 Raw models response:', response.data)

    availableModels.value = response.data.map(model => ({
      name: model.name,
      size: model.size || 'Unknown'
    }))
    console.log('✅ Loaded models for mate detail:', availableModels.value)

    // Set first model as default if current model is not available
    if (availableModels.value.length > 0 && !availableModels.value.find(m => m.name === logAnalysis.value.model)) {
      logAnalysis.value.model = availableModels.value[0].name
    }
  } catch (err) {
    console.error('Failed to load models:', err)
    // Fallback to default GGUF model
    availableModels.value = [
      { name: 'qwen2.5-3b-instruct-q4_k_m.gguf', size: 'Unknown' }
    ]
  } finally {
    loadingModels.value = false
  }
}

// Load models on mount
onMounted(() => {
  loadAvailableModels()
})

async function pingMate() {
  pinging.value = true
  try {
    await axios.post(`/api/fleet-mate/mates/${props.mate.mateId}/ping`)
    success('Ping erfolgreich gesendet')
  } catch (err) {
    console.error('Failed to ping mate:', err)
    error('Ping fehlgeschlagen')
  } finally {
    setTimeout(() => {
      pinging.value = false
    }, 500)
  }
}

let currentSessionId = ''

async function exportAsPdf() {
  try {
    // Clean up the analysis output (remove ANSI codes, terminal prompts, etc.)
    const cleanContent = analysisOutput.value
      .replace(/\$[^\n]*\n/g, '') // Remove terminal prompts
      .replace(/✓[^\n]*\n/g, '')  // Remove status messages
      .replace(/🤖[^\n]*\n/g, '') // Remove AI status messages
      .trim()

    if (!cleanContent) {
      error('Keine Analyse-Daten zum Exportieren vorhanden')
      return
    }

    const response = await axios.post('/api/fleet-mate/export-pdf', {
      content: cleanContent,
      mateId: props.mate.mateId,
      logPath: logAnalysis.value.path,
      sessionId: currentSessionId
    }, {
      responseType: 'blob'
    })

    // Create download link
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url

    // Extract filename from Content-Disposition header or create default
    const contentDisposition = response.headers['content-disposition']
    let filename = 'log-analysis.pdf'
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename="?(.+)"?/i)
      if (filenameMatch) {
        filename = filenameMatch[1]
      }
    }

    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)

    success('PDF erfolgreich heruntergeladen!')
  } catch (err) {
    console.error('Failed to export PDF:', err)
    error('PDF-Export fehlgeschlagen')
  }
}

async function startLogAnalysis() {
  analyzing.value = true
  readingProgress.value = 0
  progressPhase.value = 'reading'
  analysisOutput.value = '$ Starte Log-Analyse...\n\n'

  try {
    // Send analysis request
    const response = await axios.post(
      `/api/fleet-mate/mates/${props.mate.mateId}/analyze-log`,
      {
        logPath: logAnalysis.value.path,
        mode: logAnalysis.value.mode,
        model: logAnalysis.value.model,
        prompt: 'Analysiere dieses System-Log nach Fehlern, Warnungen und Auffälligkeiten.'
      }
    )

    const sessionId = response.data.sessionId
    currentSessionId = sessionId // Store for PDF export
    analysisOutput.value += `✓ Session erstellt: ${sessionId}\n`
    analysisOutput.value += `✓ Maat liest Log-Datei...\n\n`

    // Connect to SSE stream
    const eventSource = new EventSource(`/api/fleet-mate/stream/${sessionId}`)

    eventSource.addEventListener('progress', (event) => {
      const data = JSON.parse(event.data)
      readingProgress.value = Math.round(data.progress)
      progressPhase.value = data.phase || (data.progress < 50 ? 'reading' : 'analyzing')
      console.log('[SSE] progress:', readingProgress.value + '%', 'phase:', progressPhase.value)
    })

    eventSource.addEventListener('start', (event) => {
      console.log('[SSE] start event received:', event.data)
      const data = JSON.parse(event.data)
      progressPhase.value = 'analyzing'
      analysisOutput.value += `🤖 AI-Analyse gestartet mit ${data.model}...\n\n`
    })

    eventSource.addEventListener('chunk', (event) => {
      const data = JSON.parse(event.data)
      analysisOutput.value += data.chunk
    })

    let analysisCompleted = false

    eventSource.addEventListener('done', (event) => {
      console.log('[SSE] done event received:', event.data)
      analysisCompleted = true
      analysisOutput.value += '\n\n✓ Analyse abgeschlossen!\n'
      eventSource.close()
      analyzing.value = false
      success('Log-Analyse abgeschlossen')
    })

    eventSource.addEventListener('error', (event) => {
      console.log('[SSE] error event received, analysisCompleted:', analysisCompleted)

      // Ignore error if analysis was completed successfully (SSE auto-closes with error event)
      if (analysisCompleted) {
        console.log('[SSE] Ignoring error event - analysis was already completed')
        return
      }

      // Only show error for real failures (before completion)
      console.error('[SSE] Real error - stream interrupted before completion:', event)
      analysisOutput.value += '\n\n✗ Verbindungsfehler: Stream wurde unterbrochen\n'
      eventSource.close()
      analyzing.value = false
      error('Fehler bei der Log-Analyse')
    })

  } catch (err) {
    console.error('Failed to start analysis:', err)
    analysisOutput.value += `\n✗ Fehler: ${err.message}\n`
    analyzing.value = false
    error('Fehler beim Starten der Analyse')
  }
}

function formatTime(timestamp) {
  if (!timestamp) return 'Nie'
  const date = new Date(timestamp)
  const now = new Date()
  const diffMs = now - date
  const diffSecs = Math.floor(diffMs / 1000)
  const diffMins = Math.floor(diffSecs / 60)

  if (diffSecs < 60) return `vor ${diffSecs} Sekunden`
  if (diffMins < 60) return `vor ${diffMins} Minuten`

  return date.toLocaleTimeString('de-DE')
}

function formatUptime(seconds) {
  if (!seconds) return 'N/A'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  if (days > 0) return `${days}d ${hours}h`
  return `${hours}h`
}

function shortenCPUModel(model) {
  if (!model) return 'N/A'
  return model.replace(/\(R\)|\(TM\)|Processor|CPU/gi, '').trim().substring(0, 30)
}

function getCoreTemp(coreIndex) {
  if (!props.stats?.temperature?.per_core) return null
  return props.stats.temperature.per_core[coreIndex]
}

function getCPUColor(percent) {
  if (!percent) return 'text-gray-400'
  if (percent < 50) return 'text-green-400'
  if (percent < 75) return 'text-yellow-400'
  return 'text-red-400'
}

function getCPUBarColor(percent) {
  if (!percent) return 'bg-gray-600'
  if (percent < 50) return 'bg-gradient-to-r from-green-500 to-emerald-500'
  if (percent < 75) return 'bg-gradient-to-r from-yellow-500 to-amber-500'
  return 'bg-gradient-to-r from-red-500 to-rose-500'
}

function getRAMColor(percent) {
  if (!percent) return 'text-gray-400'
  if (percent < 70) return 'text-blue-400'
  if (percent < 85) return 'text-yellow-400'
  return 'text-red-400'
}

function getRAMBarColor(percent) {
  if (!percent) return 'bg-gray-600'
  if (percent < 70) return 'bg-gradient-to-r from-blue-500 to-cyan-500'
  if (percent < 85) return 'bg-gradient-to-r from-yellow-500 to-amber-500'
  return 'bg-gradient-to-r from-red-500 to-rose-500'
}

function getTempBadgeColor(temp) {
  if (!temp) return 'bg-gray-600 text-gray-300'
  if (temp < 60) return 'bg-green-500/20 text-green-400 border border-green-500/30'
  if (temp < 80) return 'bg-yellow-500/20 text-yellow-400 border border-yellow-500/30'
  return 'bg-red-500/20 text-red-400 border border-red-500/30'
}

function getTempIconColor(temp) {
  if (!temp) return 'text-gray-400'
  if (temp < 60) return 'text-green-400'
  if (temp < 80) return 'text-yellow-400'
  return 'text-red-400'
}

function getGPUColor(percent) {
  if (!percent) return 'text-gray-400'
  if (percent < 50) return 'text-green-400'
  if (percent < 75) return 'text-yellow-400'
  return 'text-red-400'
}

function getGPUBarColor(percent) {
  if (!percent) return 'bg-gray-600'
  if (percent < 50) return 'bg-gradient-to-r from-green-500 to-emerald-500'
  if (percent < 75) return 'bg-gradient-to-r from-yellow-500 to-amber-500'
  return 'bg-gradient-to-r from-red-500 to-rose-500'
}

function getVRAMColor(percent) {
  if (!percent) return 'text-gray-400'
  if (percent < 70) return 'text-purple-400'
  if (percent < 85) return 'text-yellow-400'
  return 'text-red-400'
}

function getVRAMBarColor(percent) {
  if (!percent) return 'bg-gray-600'
  if (percent < 70) return 'bg-gradient-to-r from-purple-500 to-pink-500'
  if (percent < 85) return 'bg-gradient-to-r from-yellow-500 to-amber-500'
  return 'bg-gradient-to-r from-red-500 to-rose-500'
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active > div,
.modal-leave-active > div {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-enter-from > div {
  transform: scale(0.9) translateY(-20px);
}

.modal-leave-to > div {
  transform: scale(0.9) translateY(20px);
}
</style>
