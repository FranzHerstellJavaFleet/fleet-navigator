<template>
  <div class="
    bg-white/80 dark:bg-gray-800/80
    backdrop-blur-xl backdrop-saturate-150
    border-b border-gray-200/50 dark:border-gray-700/50
    px-6 py-3
    shadow-sm
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

        <!-- Current Chat Title -->
        <div class="flex-1 flex items-center gap-4">
          <h2 v-if="chatStore.currentChat" class="text-lg font-semibold text-gray-800 dark:text-gray-100">
            {{ chatStore.currentChat.title }}
          </h2>
          <h2 v-else class="text-lg font-semibold text-gray-400 dark:text-gray-500">
            Wähle oder erstelle einen neuen Chat
          </h2>

          <!-- Chat Stats (Tokens & Streaming) -->
          <div v-if="chatStore.currentChat" class="flex items-center gap-3 text-xs">
            <!-- Token Counter -->
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
          <!-- System Prompt Title (Clickable) - Opens Settings Modal -->
          <button
            @click="openSystemPromptsSettings"
            class="
              flex items-center space-x-2 px-3 py-2
              rounded-lg shadow-sm
              hover:shadow-md
              transition-all duration-200
              cursor-pointer
              transform hover:scale-105 active:scale-95
            "
            :class="isUsingCustomModelPrompt
              ? 'bg-gradient-to-br from-green-100 to-green-50 dark:from-green-900/30 dark:to-green-800/20 border border-green-200 dark:border-green-700/50 hover:from-green-200 hover:to-green-100 dark:hover:from-green-800/40 dark:hover:to-green-700/30 hover:border-green-300 dark:hover:border-green-600/50'
              : 'bg-gradient-to-br from-purple-100 to-purple-50 dark:from-purple-900/30 dark:to-purple-800/20 border border-purple-200 dark:border-purple-700/50 hover:from-purple-200 hover:to-purple-100 dark:hover:from-purple-800/40 dark:hover:to-purple-700/30 hover:border-purple-300 dark:hover:border-purple-600/50'"
            :title="isUsingCustomModelPrompt ? 'Custom Model verwendet eigenen System-Prompt' : 'System Prompts verwalten (öffnet Einstellungen)'"
          >
            <ChatBubbleLeftRightIcon class="w-4 h-4" :class="isUsingCustomModelPrompt ? 'text-green-600 dark:text-green-400' : 'text-purple-600 dark:text-purple-400'" />
            <span class="text-sm font-medium" :class="isUsingCustomModelPrompt ? 'text-green-900 dark:text-green-100' : 'text-purple-900 dark:text-purple-100'">
              {{ systemPromptDisplayText }}
            </span>
            <Cog6ToothIcon v-if="!isUsingCustomModelPrompt" class="w-3 h-3 text-purple-500 dark:text-purple-400 opacity-60" />
          </button>

          <!-- Provider Switcher -->
          <div class="relative">
            <button
              @click="showProviderDropdown = !showProviderDropdown"
              class="
                flex items-center space-x-2 px-3 py-2
                bg-gradient-to-br from-blue-100 to-blue-50
                dark:from-blue-900/30 dark:to-blue-800/20
                rounded-lg border border-blue-200 dark:border-blue-700/50
                shadow-sm
                hover:from-blue-200 hover:to-blue-100
                dark:hover:from-blue-800/40 dark:hover:to-blue-700/30
                hover:border-blue-300 dark:hover:border-blue-600/50
                hover:shadow-md
                transition-all duration-200
                cursor-pointer
                transform hover:scale-105 active:scale-95
              "
              title="LLM Provider wechseln"
            >
              <ServerIcon class="w-4 h-4 text-blue-600 dark:text-blue-400" />
              <span class="text-sm font-medium text-blue-900 dark:text-blue-100">
                {{ activeProvider ? (activeProvider.charAt(0).toUpperCase() + activeProvider.slice(1)) : 'Loading...' }}
              </span>
              <ChevronDownIcon class="w-3 h-3 text-blue-500 dark:text-blue-400" />
            </button>

            <!-- Provider Dropdown -->
            <Transition name="dropdown">
              <div
                v-if="showProviderDropdown"
                @click.stop
                class="absolute right-0 mt-2 w-64 bg-white dark:bg-gray-800 rounded-lg shadow-xl border border-gray-200 dark:border-gray-700 z-[9999]"
              >
                <div class="p-2">
                  <div
                    v-for="provider in availableProviders"
                    :key="provider.id"
                    @click="switchProvider(provider.id)"
                    class="
                      flex items-center justify-between px-3 py-2 rounded-lg
                      hover:bg-gray-100 dark:hover:bg-gray-700
                      cursor-pointer transition-colors
                    "
                    :class="{ 'bg-blue-50 dark:bg-blue-900/20': provider.id === activeProvider }"
                  >
                    <div class="flex items-center gap-2">
                      <ServerIcon class="w-4 h-4 text-gray-600 dark:text-gray-400" />
                      <span class="text-sm font-medium text-gray-900 dark:text-white">
                        {{ provider.name }}
                      </span>
                    </div>
                    <CheckIcon
                      v-if="provider.id === activeProvider"
                      class="w-4 h-4 text-blue-600 dark:text-blue-400"
                    />
                  </div>
                </div>
              </div>
            </Transition>
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
            <CpuChipIcon class="w-4 h-4 text-gray-600 dark:text-gray-400" />
            <span class="text-sm font-medium text-gray-900 dark:text-white">
              {{ chatStore.selectedModel || 'Modell wird geladen...' }}
            </span>
            <span class="text-xs">⭐</span>
          </button>
        </div>

        <!-- Theme Toggle -->
        <ActionButton
          @click="$emit('toggle-theme')"
          :title="darkMode ? 'Zum hellen Modus wechseln' : 'Zum dunklen Modus wechseln'"
          color="orange"
        >
          <SunIcon v-if="darkMode" class="w-5 h-5" />
          <MoonIcon v-else class="w-5 h-5" />
        </ActionButton>

        <!-- DISTRIBUTED AGENTS -->
        <!-- Fleet Mates (Linux/OS Mates) -->
        <ActionButton
          @click="openInNewTab('/agents/fleet-mates')"
          title="Fleet Mates Dashboard"
          color="orange"
        >
          <ServerIcon class="w-5 h-5" />
        </ActionButton>

        <!-- Settings -->
        <ActionButton
          @click="$emit('toggle-settings')"
          title="Einstellungen öffnen"
          color="orange"
        >
          <Cog6ToothIcon class="w-5 h-5" />
        </ActionButton>

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

    <!-- System Prompt Editor -->
    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0 max-h-0"
      enter-to-class="opacity-100 max-h-96"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100 max-h-96"
      leave-to-class="opacity-0 max-h-0"
    >
      <div v-if="showSystemPrompt" class="mt-3 overflow-hidden">
        <!-- System Prompt Selection ListBox -->
        <div v-if="promptTemplates.length > 0" class="mb-4">
          <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">System Prompt auswählen:</div>
          <div class="space-y-2 max-h-[500px] overflow-y-auto pr-2 custom-scrollbar">
            <!-- "Kein System Prompt" Option -->
            <button
              @click="clearSystemPrompt"
              class="
                w-full text-left px-4 py-3 rounded-xl
                transition-all duration-200
                flex items-center gap-3
                border-2
              "
              :class="!chatStore.systemPrompt
                ? 'bg-purple-900/40 border-purple-500/50 shadow-lg shadow-purple-500/20'
                : 'bg-gray-700/30 dark:bg-gray-700/30 bg-gray-100 border-gray-300 dark:border-gray-600/30 hover:bg-gray-200 dark:hover:bg-gray-700/50 hover:border-gray-400 dark:hover:border-gray-500/50'"
            >
              <div class="flex-shrink-0 p-2 rounded-lg bg-gray-600/50 dark:bg-gray-600/50">
                <XMarkIcon class="w-5 h-5 text-gray-400" />
              </div>
              <div class="flex-1">
                <div class="font-medium text-gray-900 dark:text-white">Kein System Prompt</div>
                <div class="text-xs text-gray-600 dark:text-gray-400">Standard-Verhalten ohne spezielle Anweisungen</div>
              </div>
              <div v-if="!chatStore.systemPrompt" class="flex-shrink-0">
                <div class="w-5 h-5 rounded-full bg-purple-500 flex items-center justify-center">
                  <svg class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                  </svg>
                </div>
              </div>
            </button>

            <!-- Prompt Template Options -->
            <button
              v-for="template in promptTemplates"
              :key="template.id"
              @click="loadTemplate(template)"
              class="
                w-full text-left px-4 py-3 rounded-xl
                transition-all duration-200
                flex items-center gap-3
                border-2 group
                relative
              "
              :class="chatStore.systemPromptTitle === template.name
                ? 'bg-purple-900/40 border-purple-500/50 shadow-lg shadow-purple-500/20'
                : 'bg-gray-700/30 dark:bg-gray-700/30 bg-gray-100 border-gray-300 dark:border-gray-600/30 hover:bg-gray-200 dark:hover:bg-gray-700/50 hover:border-gray-400 dark:hover:border-gray-500/50'"
            >
              <div class="flex-shrink-0 p-2 rounded-lg bg-gradient-to-br from-purple-500/20 to-indigo-500/20">
                <ChatBubbleLeftRightIcon class="w-5 h-5 text-purple-400" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="font-medium text-gray-900 dark:text-white truncate">{{ template.name }}</div>
                <div class="text-xs text-gray-600 dark:text-gray-400 line-clamp-2 mt-1">
                  {{ template.content.substring(0, 100) }}{{ template.content.length > 100 ? '...' : '' }}
                </div>
              </div>
              <div class="flex items-center gap-2">
                <button
                  v-if="!template.isDefault"
                  @click.stop="deleteTemplate(template.id)"
                  class="
                    opacity-0 group-hover:opacity-100
                    p-2 rounded-lg
                    text-red-500 hover:text-red-600 hover:bg-red-100 dark:hover:bg-red-900/30
                    transition-all duration-200
                  "
                  title="Vorlage löschen"
                >
                  <TrashIcon class="w-4 h-4" />
                </button>
                <div v-if="chatStore.systemPromptTitle === template.name" class="flex-shrink-0">
                  <div class="w-5 h-5 rounded-full bg-purple-500 flex items-center justify-center">
                    <svg class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                  </div>
                </div>
              </div>
            </button>
          </div>
        </div>

        <!-- Custom Prompt Textarea (Optional) -->
        <div class="mb-4">
          <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Oder eigenen Prompt eingeben:
          </div>
          <textarea
            v-model="chatStore.systemPrompt"
            placeholder="System-Prompt eingeben (z.B. 'Du bist ein hilfreicher Java-Experte...')"
            rows="4"
            class="
              w-full px-4 py-3
              bg-white dark:bg-gray-900
              border border-gray-300 dark:border-gray-600
              text-gray-900 dark:text-gray-100
              rounded-xl
              focus:outline-none focus:ring-2 focus:ring-fleet-orange-500 focus:border-transparent
              text-sm resize-none
              transition-all duration-200
            "
            @input="chatStore.systemPromptTitle = null"
          ></textarea>
          <button
            @click="showSaveTemplateModal = true"
            v-if="chatStore.systemPrompt.trim() && !chatStore.systemPromptTitle"
            class="
              mt-2 flex items-center gap-2
              px-3 py-1.5 text-sm
              text-fleet-orange-600 dark:text-fleet-orange-400
              hover:text-fleet-orange-700 dark:hover:text-fleet-orange-300
              hover:bg-fleet-orange-50 dark:hover:bg-fleet-orange-900/20
              rounded-lg
              transition-all duration-200
            "
          >
            <BookmarkIcon class="w-4 h-4" />
            Als Vorlage speichern
          </button>
        </div>

        <!-- Action Buttons -->
        <div class="flex justify-end gap-2 pt-4 border-t border-gray-300 dark:border-gray-700/50">
          <button
            @click="showSystemPrompt = false"
            class="
              px-4 py-2 text-sm
              bg-fleet-orange-500 hover:bg-fleet-orange-600
              text-white
              rounded-xl shadow-sm
              hover:shadow-md
              transition-all duration-200
              transform hover:scale-105 active:scale-95
            "
          >
            Fertig
          </button>
        </div>
      </div>
    </Transition>

    <!-- Save Template Modal -->
    <Transition name="fade">
      <div v-if="showSaveTemplateModal" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50">
        <div class="
          bg-white dark:bg-gray-800
          rounded-2xl shadow-2xl
          p-6 w-full max-w-md
          border border-gray-200 dark:border-gray-700
          transform transition-all duration-300
        ">
          <h3 class="text-lg font-bold mb-4 text-gray-900 dark:text-white">Vorlage speichern</h3>
          <input
            v-model="newTemplateName"
            @keyup.enter="saveTemplate"
            type="text"
            placeholder="Name der Vorlage"
            class="
              w-full px-4 py-2 mb-4
              border border-gray-300 dark:border-gray-600
              bg-white dark:bg-gray-900
              text-gray-900 dark:text-white
              rounded-lg
              focus:outline-none focus:ring-2 focus:ring-fleet-orange-500
            "
          />
          <div class="flex justify-end gap-2">
            <button
              @click="showSaveTemplateModal = false"
              class="px-4 py-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
            >
              Abbrechen
            </button>
            <button
              @click="saveTemplate"
              class="px-4 py-2 bg-fleet-orange-500 hover:bg-fleet-orange-600 text-white rounded-lg transition-colors"
            >
              Speichern
            </button>
          </div>
        </div>
      </div>
    </Transition>
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
  TrashIcon,
  BookmarkIcon,
  Bars3Icon,
  XMarkIcon,
  BoltIcon,
  FireIcon,
  ServerIcon,
  ChevronDownIcon,
  CheckIcon
} from '@heroicons/vue/24/outline'
import { BoltIcon as BoltIconSolid } from '@heroicons/vue/24/solid'
import { useChatStore } from '../stores/chatStore'
import { useSettingsStore } from '../stores/settingsStore'
import api from '../services/api'
import axios from 'axios'

// Components
import ActionButton from './ActionButton.vue'
import Logo from './Logo.vue'

defineProps({
  darkMode: Boolean
})

const chatStore = useChatStore()
const settingsStore = useSettingsStore()
const showSystemPrompt = ref(false)
const showSaveTemplateModal = ref(false)
const newTemplateName = ref('')
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

// Provider Management
const showProviderDropdown = ref(false)
const activeProvider = ref('')
const availableProviders = ref([])

// System monitoring - Fleet Mate Stats laden
const mates = ref([])
const mateStats = ref({})

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

let mateDataInterval = null

onMounted(async () => {
  await loadTemplates()
  await loadProviders()

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
})

onUnmounted(() => {
  if (mateDataInterval) {
    clearInterval(mateDataInterval)
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

const loadTemplate = async (template) => {
  chatStore.systemPrompt = template.content
  chatStore.systemPromptTitle = template.name
  // Auto-close after selection
  showSystemPrompt.value = false
}

// Provider Management Functions
async function loadProviders() {
  try {
    const response = await api.getProviderStatus()
    // Response structure: { activeProvider: "ollama", availableProviders: [...], providerStatus: {...} }

    if (response.availableProviders && Array.isArray(response.availableProviders)) {
      availableProviders.value = response.availableProviders.map(p => ({
        id: p,
        name: formatProviderName(p)
      }))
    }

    if (response.activeProvider) {
      activeProvider.value = response.activeProvider
    }

    console.log('✅ Loaded providers:', availableProviders.value, 'Active:', activeProvider.value)
  } catch (error) {
    console.error('Failed to load providers:', error)
    // Fallback: set some defaults
    availableProviders.value = [
      { id: 'ollama', name: 'Ollama' },
      { id: 'llamacpp', name: 'Llama.cpp' },
      { id: 'java-llama-cpp', name: 'Java Llama.cpp' }
    ]
    activeProvider.value = 'ollama'
  }
}

function formatProviderName(provider) {
  // Format provider names nicely with clear distinction
  const nameMap = {
    'ollama': 'Ollama',
    'llamacpp': 'Llama.cpp (Native)',
    'java-llama-cpp': 'Llama.cpp (JNI)'
  }
  return nameMap[provider] || provider.charAt(0).toUpperCase() + provider.slice(1)
}

async function switchProvider(providerId) {
  try {
    await api.switchProvider(providerId)
    activeProvider.value = providerId
    showProviderDropdown.value = false
    console.log(`✅ Switched to provider: ${providerId}`)
  } catch (error) {
    console.error('Failed to switch provider:', error)
  }
}

// Open Settings Modal on "templates" tab
const emit = defineEmits(['toggle-theme', 'toggle-settings', 'toggle-model-manager', 'open-settings-tab'])

function openSystemPromptsSettings() {
  // Emit event to open settings on specific tab
  emit('open-settings-tab', 'templates')
}

const deleteTemplate = async (id) => {
  if (confirm('Vorlage wirklich löschen?')) {
    try {
      await api.deleteSystemPrompt(id)
      await loadTemplates()
    } catch (error) {
      console.error('Failed to delete template:', error)
    }
  }
}

const saveTemplate = async () => {
  if (!newTemplateName.value.trim()) return

  try {
    await api.createSystemPrompt({
      name: newTemplateName.value,
      content: chatStore.systemPrompt
    })
    await loadTemplates()
    newTemplateName.value = ''
    showSaveTemplateModal.value = false
  } catch (error) {
    console.error('Failed to save template:', error)
  }
}

const clearSystemPrompt = () => {
  chatStore.systemPrompt = ''
  chatStore.systemPromptTitle = 'Kein System-Prompt'
  // Auto-close after selection
  showSystemPrompt.value = false
}

const openInNewTab = (url) => {
  window.open(url, '_blank')
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
