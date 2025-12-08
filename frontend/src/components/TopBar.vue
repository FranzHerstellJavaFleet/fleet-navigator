<template>
  <div class="
    topbar-nav
    bg-white/95 dark:bg-gray-900/95
    backdrop-blur-sm
    border-b border-gray-200 dark:border-gray-700
    px-6 py-3
    relative z-50
  ">
    <div class="flex items-center justify-between">
      <!-- Left Side: Hamburger + Logo + Title -->
      <div class="flex items-center space-x-4">
        <!-- Hamburger Menu Button -->
        <button
          @click="settingsStore.toggleSidebar()"
          class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          :title="settingsStore.settings.sidebarCollapsed ? 'Sidebar einblenden' : 'Sidebar ausblenden'"
        >
          <Bars3Icon class="w-6 h-6 text-gray-700 dark:text-gray-300" />
        </button>

        <!-- Logo + App Title (clickable link to website) -->
        <a
          href="https://www.java-developer.online"
          target="_blank"
          rel="noopener noreferrer"
          class="flex items-center space-x-3 hover:opacity-80 transition-opacity cursor-pointer"
          title="Visit java-developer.online"
        >
          <Logo :size="32" />
          <h1 class="text-xl font-bold bg-gradient-to-r from-fleet-orange-400 to-fleet-orange-600 bg-clip-text text-transparent">
            Fleet Navigator
          </h1>
        </a>

        <!-- Divider -->
        <div class="h-8 w-px bg-gray-300 dark:bg-gray-600"></div>

        <!-- Current Chat Title OR Version Info -->
        <div class="flex-1 flex items-center gap-4">
          <h2 v-if="chatStore.currentChat" class="text-lg font-semibold text-gray-800 dark:text-gray-100">
            {{ chatStore.currentChat.title }}
          </h2>
          <!-- Version Badge (wenn kein Chat aktiv, bis Version 1.x sichtbar) -->
          <div
            v-else-if="showVersionBadge"
            class="flex items-center gap-3 px-3 py-1.5 rounded-lg shadow-sm"
            :class="versionsMatch
              ? 'bg-gradient-to-r from-gray-100 to-gray-50 dark:from-gray-800 dark:to-gray-900 border border-gray-200 dark:border-gray-700'
              : 'bg-gradient-to-r from-red-100 to-red-50 dark:from-red-900/30 dark:to-red-800/20 border border-red-300 dark:border-red-700'"
            :title="versionsMatch
              ? 'Versionen synchron'
              : 'Version Mismatch! Browser-Cache leeren (Ctrl+Shift+R)'"
          >
            <!-- Frontend Version + Build Date -->
            <div class="flex items-center gap-1.5">
              <span class="text-[10px] text-gray-400 dark:text-gray-500">FE</span>
              <span class="text-sm font-medium" :class="versionsMatch ? 'text-gray-600 dark:text-gray-300' : 'text-red-600 dark:text-red-400'">v{{ frontendVersion }}</span>
              <span class="text-[10px] text-gray-400 dark:text-gray-500">({{ frontendBuildDate }})</span>
            </div>
            <!-- Divider -->
            <div class="h-4 w-px bg-gray-300 dark:bg-gray-600"></div>
            <!-- Backend Version + Build Date -->
            <div class="flex items-center gap-1.5">
              <span class="text-[10px] text-gray-400 dark:text-gray-500">BE</span>
              <span class="text-sm font-medium" :class="versionsMatch ? 'text-gray-600 dark:text-gray-300' : 'text-red-600 dark:text-red-400'">v{{ backendVersion }}</span>
              <span class="text-[10px] text-gray-400 dark:text-gray-500">({{ backendBuildTime }})</span>
            </div>
            <!-- Warning Icon if mismatch -->
            <span v-if="!versionsMatch" class="text-red-500" title="Versionen stimmen nicht überein!">⚠️</span>
          </div>

          <!-- Chat Stats (Tokens & Streaming) -->
          <div v-if="chatStore.currentChat" class="flex items-center gap-3 text-xs">
            <!-- Projekt-Kontext Anzeige (wenn Chat zu Projekt gehört) -->
            <div
              v-if="chatStore.currentChat.projectId"
              class="flex items-center gap-2 px-2.5 py-1.5 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800"
              :title="`Projekt '${chatStore.currentChat.projectName}': ${chatStore.currentChat.projectChatCount || 1} Chat(s), ${(chatStore.currentChat.projectTotalChatTokens || 0).toLocaleString()} Chat-Tokens + ${(chatStore.currentChat.projectTokens || 0).toLocaleString()} Kontext-Tokens`"
            >
              <span class="text-blue-600 dark:text-blue-400 text-[10px] font-medium">📁 {{ chatStore.currentChat.projectName }}</span>
              <span class="text-blue-500 dark:text-blue-400">|</span>
              <span class="text-blue-600 dark:text-blue-400 text-[10px]">{{ chatStore.currentChat.projectChatCount || 1 }} Chats</span>
              <span class="text-blue-500 dark:text-blue-400">|</span>
              <span class="text-blue-600 dark:text-blue-400 text-[10px] font-mono">{{ ((chatStore.currentChat.projectTotalChatTokens || 0) + (chatStore.currentChat.projectTokens || 0)).toLocaleString() }} Tokens</span>
            </div>

            <!-- Context Usage Progressbar (nur wenn Experte mit numCtx ausgewählt) -->
            <div
              v-if="chatStore.contextUsage.maxContextTokens"
              class="flex items-center gap-2 px-2.5 py-1.5 rounded-lg bg-gray-100 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 min-w-[180px]"
              :title="projectContextTitle"
            >
              <span class="text-gray-500 dark:text-gray-400 text-[10px]">CTX</span>
              <div class="flex-1 h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                <div
                  class="h-full transition-all duration-300 rounded-full"
                  :class="contextUsageColor"
                  :style="{ width: contextUsagePercent + '%' }"
                ></div>
              </div>
              <span class="text-gray-600 dark:text-gray-400 font-mono text-[10px]">{{ contextUsagePercent }}%</span>
            </div>

            <!-- Token Counter (aktueller Chat) -->
            <div class="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-gray-100 dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
              <CpuChipIcon class="w-3.5 h-3.5 text-gray-500 dark:text-gray-400" />
              <span class="text-gray-600 dark:text-gray-400">Tokens:</span>
              <span class="text-fleet-orange-500 font-semibold">{{ chatStore.currentChatTokens }}</span>
            </div>

            <!-- Streaming Status -->
            <div class="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-gray-100 dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
              <BoltIcon v-if="chatStore.streamingEnabled" class="w-3.5 h-3.5 text-green-500" />
              <DocumentTextIcon v-else class="w-3.5 h-3.5 text-gray-500" />
              <span class="text-gray-600 dark:text-gray-400">{{ chatStore.streamingEnabled ? 'Streaming' : 'Normal' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Side Controls -->
      <div class="flex items-center space-x-2">
        <!-- Current Model Display -->
        <div class="flex items-center space-x-3 mr-2">
          <!-- Expert Avatar Display (nur bei Experten) -->
          <div
            v-if="chatStore.selectedExpertId"
            class="flex-shrink-0"
            :title="displayModelName"
          >
            <!-- Avatar Image or Silhouette - quadratisch, dünner Rand -->
            <div class="w-10 h-10 rounded-md overflow-hidden border border-gray-300 dark:border-gray-600 shadow-sm bg-gray-100 dark:bg-gray-800">
              <img
                v-if="currentExpertAvatar"
                :src="currentExpertAvatar"
                :alt="displayModelName"
                class="w-10 h-10 object-cover object-center"
                style="min-width: 40px; min-height: 40px;"
                @error="handleExpertAvatarError"
              />
              <div
                v-else
                class="w-full h-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center"
              >
                <UserIcon class="w-6 h-6 text-gray-400 dark:text-gray-500" />
              </div>
            </div>
          </div>

          <!-- System Prompt Title - Klickbar für normale Modelle, öffnet Settings -->
          <!-- Variante 1: Normale Modelle (nicht Experte, nicht Custom) - Klickbar -->
          <button
            v-if="!chatStore.selectedExpertId && !isUsingCustomModelPrompt"
            @click="openSystemPromptsSettings"
            class="
              flex items-center space-x-2 px-3 py-2
              rounded-lg shadow-sm
              hover:shadow-md
              transition-all duration-200
              cursor-pointer
              transform hover:scale-105 active:scale-95
              bg-gradient-to-br from-purple-100 to-purple-50 dark:from-purple-900/30 dark:to-purple-800/20
              border border-purple-200 dark:border-purple-700/50
              hover:from-purple-200 hover:to-purple-100 dark:hover:from-purple-800/40 dark:hover:to-purple-700/30
              hover:border-purple-300 dark:hover:border-purple-600/50
            "
            title="System Prompt in Einstellungen auswählen"
          >
            <ChatBubbleLeftRightIcon class="w-4 h-4 text-purple-600 dark:text-purple-400" />
            <span class="text-sm font-medium text-purple-900 dark:text-purple-100">
              {{ systemPromptDisplayText }}
            </span>
            <Cog6ToothIcon class="w-3 h-3 text-purple-500 dark:text-purple-400 opacity-60" />
          </button>

          <!-- Variante 2: Custom Model - Nicht klickbar, nur Anzeige -->
          <div
            v-if="!chatStore.selectedExpertId && isUsingCustomModelPrompt"
            class="
              flex items-center space-x-2 px-3 py-2
              rounded-lg shadow-sm
              bg-gradient-to-br from-green-100 to-green-50 dark:from-green-900/30 dark:to-green-800/20
              border border-green-200 dark:border-green-700/50
            "
            title="Custom Model verwendet eigenen System-Prompt (nicht änderbar)"
          >
            <ChatBubbleLeftRightIcon class="w-4 h-4 text-green-600 dark:text-green-400" />
            <span class="text-sm font-medium text-green-900 dark:text-green-100">
              Eigener Modell-Prompt
            </span>
          </div>

          <!-- Model (Clickable - Opens Model Manager) -->
          <button
            @click="$emit('toggle-model-manager')"
            class="
              flex items-center space-x-2 px-3 py-2
              bg-gradient-to-br from-gray-100 to-gray-50
              dark:from-gray-700/50 dark:to-gray-800/50
              rounded-lg border border-gray-200 dark:border-gray-700
              shadow-sm
              hover:from-gray-200 hover:to-gray-100
              dark:hover:from-gray-600/50 dark:hover:to-gray-700/50
              hover:border-gray-300 dark:hover:border-gray-600
              hover:shadow-md
              transition-all duration-200
              cursor-pointer
              transform hover:scale-105 active:scale-95
            "
            title="Modellverwaltung öffnen"
          >
            <CpuChipIcon v-if="!chatStore.selectedExpertId" class="w-4 h-4 text-gray-600 dark:text-gray-400" />
            <span v-else class="text-sm">🎓</span>
            <span class="text-sm font-medium text-gray-900 dark:text-white">
              {{ displayModelName }}
            </span>
            <span v-if="!chatStore.selectedExpertId" class="text-xs">⭐</span>
          </button>

          <!-- Active Expert Mode Badge (nur wenn Experte und Modus aktiv) -->
          <div
            v-if="chatStore.selectedExpertId && chatStore.currentChat?.activeExpertModeName"
            class="
              flex items-center space-x-1.5 px-2.5 py-1.5
              bg-gradient-to-br from-amber-100 to-amber-50
              dark:from-amber-900/30 dark:to-amber-800/20
              rounded-lg border border-amber-200 dark:border-amber-700/50
              shadow-sm
            "
            :title="'Aktiver Modus: ' + chatStore.currentChat.activeExpertModeName"
          >
            <span class="text-sm">📋</span>
            <span class="text-xs font-medium text-amber-900 dark:text-amber-100">
              {{ chatStore.currentChat.activeExpertModeName }}
            </span>
          </div>
        </div>

        <!-- Theme Dropdown (extracted component) -->
        <ThemeSelector :darkMode="darkMode" @toggle-theme="$emit('toggle-theme')" />

        <!-- Fleet Mates Status (Clickable - Opens Dashboard) -->
        <button
          @click="openInNewTab('/agents/fleet-mates')"
          class="
            flex items-center space-x-2 px-3 py-2
            rounded-lg border shadow-sm
            transition-all duration-200 cursor-pointer
            transform hover:scale-105 active:scale-95
            hover:shadow-md
          "
          :class="connectedMatesCount > 0
            ? 'bg-gradient-to-br from-green-100 to-green-50 dark:from-green-900/30 dark:to-green-800/20 border-green-200 dark:border-green-700/50 hover:from-green-200 hover:to-green-100 dark:hover:from-green-800/40 dark:hover:to-green-700/30'
            : trustedMates.length > 0
              ? 'bg-gradient-to-br from-yellow-100 to-yellow-50 dark:from-yellow-900/30 dark:to-yellow-800/20 border-yellow-200 dark:border-yellow-700/50 hover:from-yellow-200 hover:to-yellow-100 dark:hover:from-yellow-800/40 dark:hover:to-yellow-700/30'
              : 'bg-gradient-to-br from-gray-100 to-gray-50 dark:from-gray-700/50 dark:to-gray-800/50 border-gray-200 dark:border-gray-700 hover:from-gray-200 hover:to-gray-100 dark:hover:from-gray-600/50 dark:hover:to-gray-700/50'"
          :title="trustedMates.length > 0
            ? `${connectedMatesCount} von ${trustedMates.length} Mates online - Klick für Dashboard`
            : 'Keine Mates gepairt - Klick für Dashboard'"
        >
          <UserGroupIcon class="w-4 h-4" :class="connectedMatesCount > 0 ? 'text-green-600 dark:text-green-400' : trustedMates.length > 0 ? 'text-yellow-600 dark:text-yellow-400' : 'text-gray-500 dark:text-gray-400'" />
          <span class="text-sm font-medium text-gray-900 dark:text-white">
            <template v-if="trustedMates.length > 0">
              {{ connectedMatesCount }}/{{ trustedMates.length }} Mates
            </template>
            <template v-else>
              Keine Mates
            </template>
          </span>
          <span
            class="w-2 h-2 rounded-full"
            :class="connectedMatesCount > 0 ? 'bg-green-500 animate-pulse' : trustedMates.length > 0 ? 'bg-yellow-500' : 'bg-gray-400'"
          ></span>
        </button>

        <!-- System Stats (CPU & Temp) - Clickable - GANZ RECHTS -->
        <button
          @click="$emit('toggle-monitor')"
          class="
            flex items-center space-x-2 px-3 py-2
            bg-gradient-to-br from-gray-100 to-gray-50
            dark:from-gray-700/50 dark:to-gray-800/50
            rounded-lg border border-gray-200 dark:border-gray-700
            shadow-sm
            hover:from-gray-200 hover:to-gray-100
            dark:hover:from-gray-600/50 dark:hover:to-gray-700/50
            hover:border-gray-300 dark:hover:border-gray-600
            hover:shadow-md
            transition-all duration-200
            cursor-pointer
            transform hover:scale-105 active:scale-95
          "
          title="System Monitor öffnen"
        >
          <BoltIcon class="w-4 h-4 text-amber-500 dark:text-amber-400" />
          <span class="text-sm font-medium text-gray-900 dark:text-white">
            CPU: {{ cpuUsage }}%
          </span>
          <div class="h-4 w-px bg-gray-300 dark:bg-gray-600"></div>
          <FireIcon
            class="w-4 h-4"
            :class="temperatureColor"
          />
          <span class="text-sm font-medium text-gray-900 dark:text-white">
            {{ temperature }}°C
          </span>
        </button>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import {
  SunIcon,
  MoonIcon,
  ChatBubbleLeftRightIcon,
  Cog6ToothIcon,
  CpuChipIcon,
  Bars3Icon,
  BoltIcon,
  FireIcon,
  UserGroupIcon,
  UserIcon
} from '@heroicons/vue/24/outline'
import { BoltIcon as BoltIconSolid } from '@heroicons/vue/24/solid'
import { useChatStore } from '../stores/chatStore'
import { useSettingsStore } from '../stores/settingsStore'
import { useAuthStore } from '../stores/authStore'
import api from '../services/api'
import axios from 'axios'

// Components
import ActionButton from './ActionButton.vue'
import Logo from './Logo.vue'
import ThemeSelector from './topbar/ThemeSelector.vue'

const props = defineProps({
  darkMode: Boolean
})

const chatStore = useChatStore()
const settingsStore = useSettingsStore()
const authStore = useAuthStore()

// Version info from Vite build (Frontend)
const frontendVersion = __APP_VERSION__ || '0.0.0'
const frontendBuildDate = __BUILD_DATE__ || 'Unknown'

// Backend version (loaded from API)
const backendVersion = ref('...')
const backendBuildTime = ref('')

// Show version badge until version 1.x
const showVersionBadge = computed(() => {
  const majorVersion = parseInt(frontendVersion.split('.')[0], 10)
  return majorVersion < 1
})

// Check if versions match (for warning display)
const versionsMatch = computed(() => {
  return frontendVersion === backendVersion.value
})

// Load backend version from system status
const loadBackendVersion = async () => {
  try {
    const response = await axios.get('/api/system/status')
    if (response.data) {
      backendVersion.value = response.data.backendVersion || 'unknown'
      backendBuildTime.value = response.data.backendBuildTime || ''
    }
  } catch (error) {
    console.warn('Could not load backend version:', error)
    backendVersion.value = 'error'
  }
}

// Computed: Context usage percentage for progressbar
const contextUsagePercent = computed(() => {
  if (!chatStore.contextUsage.maxContextTokens || chatStore.contextUsage.maxContextTokens === 0) {
    return 0
  }
  // Bei Projekten: Gesamt-Projekt-Tokens berücksichtigen
  let totalTokens = chatStore.contextUsage.totalChatTokens
  if (chatStore.currentChat?.projectId) {
    totalTokens = (chatStore.currentChat.projectTotalChatTokens || 0) + (chatStore.currentChat.projectTokens || 0)
  }
  const percent = (totalTokens / chatStore.contextUsage.maxContextTokens) * 100
  return Math.min(100, Math.round(percent))
})

// Computed: Tooltip for context progressbar (includes project info if applicable)
const projectContextTitle = computed(() => {
  const max = chatStore.contextUsage.maxContextTokens
  if (!max) return ''

  if (chatStore.currentChat?.projectId) {
    const chatTokens = chatStore.currentChat.projectTotalChatTokens || 0
    const contextTokens = chatStore.currentChat.projectTokens || 0
    const total = chatTokens + contextTokens
    return `Projekt-Kontext: ${total.toLocaleString()} / ${max.toLocaleString()} Tokens (${chatTokens.toLocaleString()} Chat + ${contextTokens.toLocaleString()} Dateien)`
  }

  return `${chatStore.contextUsage.totalChatTokens.toLocaleString()} / ${max.toLocaleString()} Tokens (${contextUsagePercent.value}%)`
})

// Computed: Color for context usage progressbar
const contextUsageColor = computed(() => {
  const percent = contextUsagePercent.value
  if (percent >= 90) return 'bg-red-500'
  if (percent >= 75) return 'bg-orange-500'
  if (percent >= 50) return 'bg-yellow-500'
  return 'bg-green-500'
})

const emit = defineEmits(['toggle-theme', 'toggle-settings', 'toggle-model-manager', 'toggle-monitor', 'open-settings-tab'])
const promptTemplates = ref([])

// Computed: Display text for system prompt button
const systemPromptDisplayText = computed(() => {
  // Check if current model is a custom model
  if (chatStore.isCustomModel(chatStore.selectedModel)) {
    return 'Eigener Modell-Prompt'
  }
  return chatStore.systemPromptTitle || 'Kein System-Prompt'
})

// Computed: Check if custom model is active (for styling)
const isUsingCustomModelPrompt = computed(() => {
  return chatStore.isCustomModel(chatStore.selectedModel)
})

// Computed: Display name for model/expert
const displayModelName = computed(() => {
  if (chatStore.selectedExpertId) {
    const expert = chatStore.getExpertById(chatStore.selectedExpertId)
    if (expert) {
      return expert.name
    }
  }
  return chatStore.selectedModel || 'Modell wird geladen...'
})

// Computed: Current expert avatar URL
const currentExpertAvatar = computed(() => {
  if (chatStore.selectedExpertId) {
    const expert = chatStore.getExpertById(chatStore.selectedExpertId)
    return expert?.avatarUrl || null
  }
  return null
})

// Handle expert avatar loading errors
const handleExpertAvatarError = (event) => {
  console.warn('Experten-Avatar konnte nicht geladen werden')
  // The fallback silhouette is shown automatically via v-else
}

// System monitoring - Fleet Mate Stats laden
const mates = ref([])
const mateStats = ref({})

// Trusted Mates (pairing) status
const trustedMates = ref([])
const connectedMatesCount = ref(0)

// Lade Fleet Mate Daten regelmäßig
const loadMateData = async () => {
  try {
    // Lade Mates Liste
    const response = await axios.get('/api/fleet-mate/mates')
    mates.value = response.data || []

    // Lade Stats nur für Hardware-Monitoring-Mates (nicht Email-Mates!)
    // Priorität: ubuntu-desktop-01 oder erster Mate mit lastStatsUpdate
    const hardwareMates = mates.value.filter(m =>
      m.status === 'ONLINE' && m.lastStatsUpdate != null
    )

    // 1. Versuche ubuntu-desktop-01 zu finden
    let hardwareMate = hardwareMates.find(m => m.mateId === 'ubuntu-desktop-01')

    // 2. Fallback: Erster Hardware-Mate
    if (!hardwareMate) {
      hardwareMate = hardwareMates[0]
    }

    if (hardwareMate) {
      try {
        const statsResponse = await axios.get(`/api/fleet-mate/mates/${hardwareMate.mateId}/stats`)
        mateStats.value = statsResponse.data || {}
      } catch (statsError) {
        console.debug(`No stats for mate ${hardwareMate.mateId}`)
      }
    }
  } catch (error) {
    console.error('Failed to load mate data:', error)
  }
}

// CPU Usage vom ersten Fleet Mate
const cpuUsage = computed(() => {
  if (!mateStats.value.cpu || !mateStats.value.cpu.usage_percent) return 0
  return Math.round(mateStats.value.cpu.usage_percent)
})

// Temperatur vom ersten Fleet Mate (Package Temp bevorzugt)
const temperature = computed(() => {
  if (!mateStats.value.temperature || !mateStats.value.temperature.sensors) return 0

  const sensors = mateStats.value.temperature.sensors
  // Suche CPU Package Temperature
  const packageSensor = sensors.find(s => s.name && s.name.includes('coretemp_package'))
  if (packageSensor && packageSensor.temperature) {
    return Math.round(packageSensor.temperature)
  }

  // Fallback: erster Sensor mit Temperatur
  const firstSensor = sensors.find(s => s.temperature && s.temperature > 0)
  if (firstSensor) {
    return Math.round(firstSensor.temperature)
  }

  return 0
})

// Temperature color based on value
const temperatureColor = computed(() => {
  const temp = temperature.value
  if (temp < 60) return 'text-green-500 dark:text-green-400'
  if (temp < 75) return 'text-yellow-500 dark:text-yellow-400'
  if (temp < 85) return 'text-orange-500 dark:text-orange-400'
  return 'text-red-500 dark:text-red-400'
})

// Lade Trusted Mates (Pairing) Status
const loadTrustedMatesStatus = async () => {
  try {
    // Hole alle trusted mates
    const trustedResponse = await axios.get('/api/pairing/trusted')
    trustedMates.value = trustedResponse.data || []

    // Hole WebSocket-Verbindungsstatus
    const wsResponse = await axios.get('/api/fleet-mate/mates')
    const connectedMateIds = (wsResponse.data || [])
      .filter(m => m.status === 'ONLINE')
      .map(m => m.mateId)

    // Zähle wie viele trusted mates aktuell verbunden sind
    connectedMatesCount.value = trustedMates.value.filter(tm =>
      connectedMateIds.includes(tm.mateId)
    ).length
  } catch (error) {
    console.debug('Could not load trusted mates status:', error)
  }
}

let mateDataInterval = null
let trustedMatesInterval = null

onMounted(async () => {
  await loadTemplates()

  // Load backend version
  await loadBackendVersion()

  // Load Karla prompt if only title is set but no content
  if (chatStore.systemPromptTitle === 'Karla' && !chatStore.systemPrompt) {
    const karlaTemplate = promptTemplates.value.find(t => t.name === 'Karla')
    if (karlaTemplate) {
      chatStore.systemPrompt = karlaTemplate.content
      console.log('✅ Auto-loaded Karla prompt')
    }
  }

  // Lade Fleet Mate Daten initial und dann alle 5 Sekunden
  await loadMateData()
  mateDataInterval = setInterval(loadMateData, 5000)

  // Lade Trusted Mates Status initial und dann alle 10 Sekunden
  await loadTrustedMatesStatus()
  trustedMatesInterval = setInterval(loadTrustedMatesStatus, 10000)
})

onUnmounted(() => {
  if (mateDataInterval) {
    clearInterval(mateDataInterval)
  }
  if (trustedMatesInterval) {
    clearInterval(trustedMatesInterval)
  }
})

const loadTemplates = async () => {
  try {
    const response = await api.getSystemPrompts()
    promptTemplates.value = response.data
  } catch (error) {
    console.error('Failed to load templates:', error)
  }
}

// Open Settings Modal on "templates" tab
function openSystemPromptsSettings() {
  emit('open-settings-tab', 'templates')
}

const openInNewTab = (url) => {
  window.open(url, '_blank')
}

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
