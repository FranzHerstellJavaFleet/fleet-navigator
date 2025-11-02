<template>
  <Transition name="modal">
    <div v-if="isOpen" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4" @click.self="close">
      <div class="bg-white/95 dark:bg-gray-800/95 backdrop-blur-xl rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden border border-gray-200/50 dark:border-gray-700/50">
        <!-- Header with Gradient -->
        <div class="sticky top-0 bg-gradient-to-r from-fleet-orange-500/10 to-orange-500/10 dark:from-fleet-orange-500/20 dark:to-orange-500/20 backdrop-blur-sm border-b border-gray-200/50 dark:border-gray-700/50 px-6 py-4 flex justify-between items-center z-10">
          <div class="flex items-center gap-3">
            <div class="p-2 rounded-xl bg-gradient-to-br from-fleet-orange-500 to-orange-600 shadow-lg">
              <Cog6ToothIcon class="w-6 h-6 text-white" />
            </div>
            <h2 class="text-2xl font-bold text-gray-900 dark:text-white">Einstellungen</h2>
          </div>
          <button
            @click="close"
            class="p-2 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all transform hover:scale-110"
          >
            <XMarkIcon class="w-6 h-6" />
          </button>
        </div>

        <!-- Tab Navigation -->
        <div class="flex border-b border-gray-200 dark:border-gray-700 px-6 bg-gray-50/50 dark:bg-gray-900/50">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            class="flex items-center gap-2 px-4 py-3 text-sm font-medium transition-all relative"
            :class="activeTab === tab.id
              ? 'text-fleet-orange-600 dark:text-fleet-orange-400'
              : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'"
          >
            <component :is="tab.icon" class="w-4 h-4" />
            {{ tab.label }}
            <div
              v-if="activeTab === tab.id"
              class="absolute bottom-0 left-0 right-0 h-0.5 bg-fleet-orange-500"
            />
          </button>
        </div>

        <!-- Content with Custom Scrollbar -->
        <div class="overflow-y-auto p-6 space-y-6 custom-scrollbar" style="max-height: calc(90vh - 220px);">

          <!-- TAB: General Settings -->
          <div v-if="activeTab === 'general'">
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <GlobeAltIcon class="w-5 h-5 text-blue-500" />
              Allgemein
            </h3>

            <!-- Language -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
                <LanguageIcon class="w-4 h-4" />
                Sprache
              </label>
              <select
                v-model="settings.language"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-fleet-orange-500 focus:border-transparent"
              >
                <option value="de">🇩🇪 Deutsch</option>
                <option value="en">🇬🇧 English</option>
              </select>
            </div>

            <!-- Theme -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
                <SunIcon class="w-4 h-4" />
                Theme
              </label>
              <select
                v-model="settings.theme"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-fleet-orange-500 focus:border-transparent"
              >
                <option value="light">☀️ Hell</option>
                <option value="dark">🌙 Dunkel</option>
                <option value="auto">🔄 System</option>
              </select>
            </div>
          </section>
          </div>

          <!-- TAB: Model Selection -->
          <div v-if="activeTab === 'models'">
          <!-- Smart Model Selection -->
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <CpuChipIcon class="w-5 h-5 text-purple-500" />
              Intelligente Modellauswahl
            </h3>

            <!-- Enable/Disable Toggle -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <SparklesIcon class="w-4 h-4 text-purple-500" />
                    Automatisches Modell-Routing
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Wählt automatisch das beste Modell basierend auf der Aufgabe
                  </p>
                </div>
                <ToggleSwitch v-model="modelSelectionSettings.enabled" color="purple" />
              </div>
            </div>

            <div v-if="!modelSelectionSettings.enabled" class="mb-4 p-3 rounded-xl bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700/50">
              <div class="flex items-start gap-2">
                <ExclamationTriangleIcon class="w-5 h-5 text-yellow-600 dark:text-yellow-400 flex-shrink-0 mt-0.5" />
                <p class="text-xs text-yellow-800 dark:text-yellow-200">
                  Intelligente Modellauswahl ist deaktiviert
                </p>
              </div>
            </div>

            <div class="space-y-3" :class="{ 'opacity-50 pointer-events-none': !modelSelectionSettings.enabled }">
              <!-- Code Model -->
              <div>
                <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1.5">
                  <CodeBracketIcon class="w-4 h-4 text-blue-500" />
                  Code-Modell
                  <span class="text-xs text-gray-500 dark:text-gray-400">(für Code, Debugging)</span>
                </label>
                <select
                  v-model="modelSelectionSettings.codeModel"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-blue-500"
                >
                  <option v-for="model in codeModels" :key="model" :value="model">
                    {{ model }}
                  </option>
                </select>
              </div>

              <!-- Fast Model -->
              <div>
                <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1.5">
                  <BoltIcon class="w-4 h-4 text-green-500" />
                  Schnelles Modell
                  <span class="text-xs text-gray-500 dark:text-gray-400">(für einfache Fragen)</span>
                </label>
                <select
                  v-model="modelSelectionSettings.fastModel"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500"
                >
                  <option v-for="model in availableModels" :key="model.name" :value="model.name">
                    {{ model.name }}
                  </option>
                </select>
              </div>

              <!-- Default Model -->
              <div>
                <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1.5">
                  <CubeIcon class="w-4 h-4 text-purple-500" />
                  Standard-Modell
                  <span class="text-xs text-gray-500 dark:text-gray-400">(Fallback)</span>
                </label>
                <select
                  v-model="modelSelectionSettings.defaultModel"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-purple-500"
                >
                  <option v-for="model in availableModels" :key="model.name" :value="model.name">
                    {{ model.name }}
                  </option>
                </select>
              </div>
            </div>

            <div class="mt-3 p-3 rounded-xl bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700/50">
              <div class="flex items-start gap-2">
                <InformationCircleIcon class="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
                <p class="text-xs text-blue-800 dark:text-blue-200">
                  <strong>Hinweis:</strong> Wenn aktiviert, wird das Modell automatisch basierend auf deiner Frage ausgewählt.
                </p>
              </div>
            </div>
          </section>
          </div>

          <!-- TAB: Model Parameters -->
          <div v-if="activeTab === 'parameters'">
          <!-- Model Parameters -->
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <AdjustmentsHorizontalIcon class="w-5 h-5 text-orange-500" />
              Model-Parameter
            </h3>

            <!-- Markdown Rendering -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <DocumentTextIcon class="w-4 h-4" />
                    Markdown-Formatierung
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Rendert Antworten mit Markdown (fett, kursiv, Code, Listen)
                  </p>
                </div>
                <ToggleSwitch v-model="settings.markdownEnabled" />
              </div>
            </div>

            <!-- Streaming -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <BoltIcon class="w-4 h-4" />
                    Streaming aktiviert
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Antworten werden in Echtzeit gestreamt
                  </p>
                </div>
                <ToggleSwitch v-model="settings.streamingEnabled" />
              </div>
            </div>

            <!-- Temperature -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center justify-between">
                <span class="flex items-center gap-2">
                  <FireIcon class="w-4 h-4 text-orange-500" />
                  Temperature
                </span>
                <span class="text-fleet-orange-500 font-semibold">{{ settings.temperature }}</span>
              </label>
              <input
                type="range"
                v-model.number="settings.temperature"
                min="0"
                max="2"
                step="0.1"
                class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer dark:bg-gray-700 accent-fleet-orange-500"
              >
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Niedrig = präzise, Hoch = kreativ
              </p>
            </div>

            <!-- Top P -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center justify-between">
                <span>Top P</span>
                <span class="text-fleet-orange-500 font-semibold">{{ settings.topP }}</span>
              </label>
              <input
                type="range"
                v-model.number="settings.topP"
                min="0"
                max="1"
                step="0.05"
                class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer dark:bg-gray-700 accent-fleet-orange-500"
              >
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Nucleus Sampling (empfohlen: 0.9)
              </p>
            </div>

            <!-- Top K -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center justify-between">
                <span>Top K</span>
                <span class="text-fleet-orange-500 font-semibold">{{ settings.topK }}</span>
              </label>
              <input
                type="range"
                v-model.number="settings.topK"
                min="1"
                max="100"
                step="1"
                class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer dark:bg-gray-700 accent-fleet-orange-500"
              >
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Anzahl berücksichtigter Tokens (empfohlen: 40)
              </p>
            </div>

            <!-- Context Length -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center justify-between">
                <span class="flex items-center gap-2">
                  <DocumentDuplicateIcon class="w-4 h-4" />
                  Context Length
                </span>
                <span class="text-fleet-orange-500 font-semibold">{{ settings.contextLength.toLocaleString() }}</span>
              </label>
              <input
                type="range"
                v-model.number="settings.contextLength"
                min="2048"
                max="131072"
                step="2048"
                class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer dark:bg-gray-700 accent-fleet-orange-500"
              >
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Maximaler Kontext in Tokens
              </p>
            </div>
          </section>

          <!-- Advanced Settings -->
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <WrenchScrewdriverIcon class="w-5 h-5 text-gray-500" />
              Erweitert
            </h3>

            <!-- Max Tokens -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
                <HashtagIcon class="w-4 h-4" />
                Max Tokens: {{ settings.maxTokens.toLocaleString() }}
              </label>
              <input
                type="number"
                v-model.number="settings.maxTokens"
                min="100"
                max="131072"
                step="1024"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-fleet-orange-500"
              >
            </div>

            <!-- Debug Mode -->
            <div class="p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <BugAntIcon class="w-4 h-4" />
                    Debug-Modus
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Zeigt detaillierte Logs in der Konsole
                  </p>
                </div>
                <ToggleSwitch v-model="settings.debugMode" color="red" />
              </div>
            </div>
          </section>
          </div>

          <!-- TAB: Personal Info -->
          <div v-if="activeTab === 'personal'">
            <PersonalInfoTab ref="personalInfoTabRef" />
          </div>

          <!-- TAB: Agents -->
          <div v-if="activeTab === 'agents'">
          <!-- Document Agent Settings -->
          <DocumentAgentSettings />

          <!-- Vision Settings -->
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <PhotoIcon class="w-5 h-5 text-indigo-500" />
              Vision Model
            </h3>

            <!-- Auto-select Vision Model -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <EyeIcon class="w-4 h-4" />
                    Auto Vision Model
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Automatisch Vision Model bei Bild-Upload wählen
                  </p>
                </div>
                <ToggleSwitch v-model="settings.autoSelectVisionModel" color="indigo" />
              </div>
            </div>

            <!-- Preferred Vision Model -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Bevorzugtes Vision Model
              </label>
              <select
                v-model="modelSelectionSettings.visionModel"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-indigo-500"
              >
                <option v-for="model in visionModels" :key="model" :value="model">
                  {{ model }}
                </option>
              </select>
              <p v-if="visionModels.length > 0" class="mt-2 text-xs text-gray-500 dark:text-gray-400">
                {{ visionModels.length }} Vision-Modelle verfügbar
              </p>
              <p v-else class="mt-2 text-xs text-yellow-600 dark:text-yellow-400">
                ⚠️ Keine Vision-Modelle gefunden. Installiere llava oder moondream mit Ollama.
              </p>
            </div>

            <!-- Vision Chaining -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <LinkIcon class="w-4 h-4" />
                    Vision-Chaining
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Vision Model Output → Haupt-Model
                  </p>
                </div>
                <ToggleSwitch v-model="modelSelectionSettings.visionChainingEnabled" color="indigo" />
              </div>
            </div>
          </section>
          </div>

        </div>

        <!-- Footer with Gradient -->
        <div class="sticky bottom-0 bg-gray-50/90 dark:bg-gray-900/90 backdrop-blur-sm border-t border-gray-200/50 dark:border-gray-700/50 px-6 py-4 flex justify-between">
          <button
            @click="resetToDefaults"
            class="px-4 py-2 rounded-xl text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white hover:bg-gray-200 dark:hover:bg-gray-700 transition-all flex items-center gap-2"
          >
            <ArrowPathIcon class="w-4 h-4" />
            Zurücksetzen
          </button>
          <div class="flex gap-3">
            <button
              @click="close"
              class="px-5 py-2 rounded-xl border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all transform hover:scale-105"
            >
              Abbrechen
            </button>
            <button
              @click="save"
              :disabled="saving"
              class="px-6 py-2 rounded-xl bg-gradient-to-r from-fleet-orange-500 to-orange-600 hover:from-fleet-orange-400 hover:to-orange-500 text-white font-semibold shadow-lg hover:shadow-xl transition-all transform hover:scale-105 disabled:opacity-50 flex items-center gap-2"
            >
              <CheckIcon v-if="!saving" class="w-5 h-5" />
              <ArrowPathIcon v-else class="w-5 h-5 animate-spin" />
              {{ saving ? 'Speichere...' : 'Speichern' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import {
  Cog6ToothIcon,
  XMarkIcon,
  GlobeAltIcon,
  LanguageIcon,
  SunIcon,
  CpuChipIcon,
  SparklesIcon,
  ExclamationTriangleIcon,
  CodeBracketIcon,
  BoltIcon,
  CubeIcon,
  InformationCircleIcon,
  AdjustmentsHorizontalIcon,
  DocumentTextIcon,
  FireIcon,
  DocumentDuplicateIcon,
  PhotoIcon,
  EyeIcon,
  LinkIcon,
  WrenchScrewdriverIcon,
  HashtagIcon,
  BugAntIcon,
  ArrowPathIcon,
  CheckIcon,
  UserIcon
} from '@heroicons/vue/24/outline'
import { useSettingsStore } from '../stores/settingsStore'
import { useChatStore } from '../stores/chatStore'
import DocumentAgentSettings from './DocumentAgentSettings.vue'
import PersonalInfoTab from './PersonalInfoTab.vue'
import { useToast } from '../composables/useToast'
import api from '../services/api'
import ToggleSwitch from './ToggleSwitch.vue'
import { filterVisionModels, filterCodeModels } from '../utils/modelFilters'

const { success, error: errorToast } = useToast()

const props = defineProps({
  isOpen: Boolean
})

const emit = defineEmits(['close', 'save'])

const settingsStore = useSettingsStore()
const chatStore = useChatStore()

// Local copy of settings for editing
const settings = ref({ ...settingsStore.settings })
const saving = ref(false)

// Active tab
const activeTab = ref('general')

// Ref to PersonalInfoTab
const personalInfoTabRef = ref(null)

// Tab configuration
const tabs = [
  { id: 'general', label: 'Allgemein', icon: GlobeAltIcon },
  { id: 'models', label: 'Modelle', icon: CpuChipIcon },
  { id: 'parameters', label: 'Parameter', icon: AdjustmentsHorizontalIcon },
  { id: 'personal', label: 'Persönliche Daten', icon: UserIcon },
  { id: 'agents', label: 'Agents', icon: SparklesIcon }
]

// Model selection settings
const modelSelectionSettings = ref({
  enabled: true,
  codeModel: 'qwen2.5-coder:7b',
  fastModel: 'llama3.2:3b',
  visionModel: 'llava:13b',
  defaultModel: 'qwen2.5-coder:7b',
  visionChainingEnabled: true,
  visionChainingSmartSelection: true
})

// Available models
const availableModels = ref([])

// Filtered models for specific use cases
const visionModels = computed(() => {
  const allModelNames = availableModels.value.map(m => m.name)
  const filtered = filterVisionModels(allModelNames)
  console.log('Vision Models:', filtered) // Debug
  return filtered
})
const codeModels = computed(() => {
  const allModelNames = availableModels.value.map(m => m.name)
  const filtered = filterCodeModels(allModelNames)
  console.log('Code Models:', filtered) // Debug
  return filtered
})

// Load model selection settings and available models on mount
onMounted(async () => {
  await loadModelSelectionSettings()
  await loadAvailableModels()
})

async function loadModelSelectionSettings() {
  try {
    const loadedSettings = await api.getModelSelectionSettings()
    modelSelectionSettings.value = loadedSettings
  } catch (error) {
    console.error('Failed to load model selection settings:', error)
  }
}

async function loadAvailableModels() {
  try {
    const models = await api.getAvailableModels()
    availableModels.value = models
  } catch (error) {
    console.error('Failed to load available models:', error)
  }
}

// Watch for changes from store
watch(() => settingsStore.settings, (newSettings) => {
  settings.value = { ...newSettings }
}, { deep: true })

function close() {
  emit('close')
}

async function save() {
  saving.value = true
  try {
    // Save general settings
    settingsStore.updateSettings(settings.value)

    // Apply streaming setting to chatStore
    if (chatStore.streamingEnabled !== settings.value.streamingEnabled) {
      chatStore.toggleStreaming()
    }

    // Save model selection settings to backend
    await api.updateModelSelectionSettings(modelSelectionSettings.value)

    // Save personal info if on that tab
    if (personalInfoTabRef.value && activeTab.value === 'personal') {
      await personalInfoTabRef.value.save()
    }

    success('Einstellungen gespeichert')
    emit('save')
    close()
  } catch (error) {
    console.error('Failed to save settings:', error)
    errorToast('Fehler beim Speichern der Einstellungen')
  } finally {
    saving.value = false
  }
}

function resetToDefaults() {
  if (confirm('Alle Einstellungen auf Standard zurücksetzen?')) {
    settingsStore.resetToDefaults()
    settings.value = { ...settingsStore.settings }
    success('Einstellungen zurückgesetzt')
  }
}
</script>

<style scoped>
/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.3);
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.5);
}

/* Modal Transitions */
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
