<template>
  <Transition name="modal">
    <div v-if="show" class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-[70] p-4">
      <div
        class="
          bg-white dark:bg-gray-800
          rounded-2xl shadow-2xl
          w-full max-w-3xl
          border border-gray-200 dark:border-gray-700
          flex flex-col
          overflow-hidden
        "
        style="height: 80vh; max-height: 80vh;"
      >
        <!-- Header mit Progress -->
        <div class="flex-shrink-0 bg-gradient-to-r from-indigo-500/10 to-purple-500/10 dark:from-indigo-500/20 dark:to-purple-500/20 border-b border-gray-200 dark:border-gray-700">
          <!-- Progress Bar -->
          <div class="px-6 pt-4">
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm font-medium text-gray-600 dark:text-gray-400">
                Schritt {{ currentStep }} von {{ totalSteps }}
              </span>
              <span class="text-sm text-gray-500 dark:text-gray-500">
                {{ stepTitles[currentStep - 1] }}
              </span>
            </div>
            <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
              <div
                class="bg-gradient-to-r from-indigo-500 to-purple-500 h-2 rounded-full transition-all duration-300"
                :style="{ width: `${(currentStep / totalSteps) * 100}%` }"
              ></div>
            </div>
          </div>

          <!-- Step Indicators -->
          <div class="flex items-center justify-center gap-2 px-6 py-4">
            <button
              v-for="step in totalSteps"
              :key="step"
              @click="goToStep(step)"
              :disabled="!canGoToStep(step)"
              class="
                flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium
                transition-all duration-200
              "
              :class="[
                currentStep === step
                  ? 'bg-indigo-500 text-white shadow-lg'
                  : step < currentStep
                    ? 'bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 cursor-pointer hover:bg-indigo-200 dark:hover:bg-indigo-900/50'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-400 cursor-not-allowed'
              ]"
            >
              <span>{{ stepEmojis[step - 1] }}</span>
              <span class="hidden sm:inline">{{ stepTitles[step - 1] }}</span>
            </button>
          </div>
        </div>

        <!-- Content Area -->
        <div class="flex-1 overflow-y-auto p-6 min-h-0">

          <!-- Provider nicht verfügbar Warnung -->
          <div v-if="!providerAvailable && !isCheckingProvider" class="flex flex-col items-center justify-center h-full text-center">
            <div class="p-4 rounded-full bg-amber-100 dark:bg-amber-900/30 mb-4">
              <ExclamationTriangleIcon class="w-12 h-12 text-amber-500" />
            </div>
            <h2 class="text-xl font-bold text-gray-900 dark:text-white mb-2">Kein Provider verfügbar</h2>
            <p class="text-gray-600 dark:text-gray-400 mb-4 max-w-md">
              Bitte stellen Sie sicher, dass Ollama oder java-llama-cpp als Provider verfügbar ist.
            </p>
            <div class="flex gap-3">
              <button
                @click="checkProviderStatus"
                class="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white rounded-lg transition-colors"
              >
                Erneut prüfen
              </button>
              <button
                @click="$emit('close')"
                class="px-4 py-2 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 rounded-lg transition-colors"
              >
                Schließen
              </button>
            </div>
          </div>

          <!-- Loading -->
          <div v-else-if="isCheckingProvider" class="flex flex-col items-center justify-center h-full">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500 mb-4"></div>
            <p class="text-gray-600 dark:text-gray-400">Prüfe Provider-Status...</p>
          </div>

          <!-- Step 1: Basis-Modell wählen -->
          <div v-else-if="currentStep === 1" class="space-y-6">
            <div class="text-center mb-6">
              <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">Wähle ein Basis-Modell</h2>
              <p class="text-gray-600 dark:text-gray-400">Das Basis-Modell bestimmt die Fähigkeiten deines Custom Models.</p>
              <!-- Provider-Info Badge -->
              <div class="mt-2 inline-flex items-center gap-2 px-3 py-1 rounded-full text-sm"
                   :class="activeProvider === 'ollama'
                     ? 'bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400'
                     : 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'">
                <span>{{ activeProvider === 'ollama' ? '🦙 Ollama' : '⚡ java-llama-cpp' }}</span>
                <span class="text-xs opacity-75">{{ activeProvider === 'ollama' ? '(Vollständiges Custom Model)' : '(GGUF Konfiguration)' }}</span>
              </div>
            </div>

            <div v-if="isLoadingModels" class="flex justify-center py-12">
              <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500"></div>
            </div>

            <div v-else-if="availableModels.length === 0" class="text-center py-12">
              <CpuChipIcon class="w-16 h-16 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
              <p class="text-gray-500 dark:text-gray-400">Keine Modelle in Ollama verfügbar.</p>
              <p class="text-sm text-gray-400 dark:text-gray-500 mt-1">Bitte laden Sie zuerst ein Modell mit "ollama pull" herunter.</p>
            </div>

            <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <button
                v-for="model in availableModels"
                :key="model.name"
                @click="selectModel(model)"
                class="
                  p-4 rounded-xl border-2 text-left
                  transition-all duration-200
                  hover:shadow-lg
                "
                :class="[
                  wizardData.baseModel === model.name
                    ? 'border-indigo-500 bg-indigo-50 dark:bg-indigo-900/20 shadow-lg shadow-indigo-500/20'
                    : 'border-gray-200 dark:border-gray-700 hover:border-indigo-300 dark:hover:border-indigo-600'
                ]"
              >
                <div class="flex items-start gap-3">
                  <div class="p-2 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-500 flex-shrink-0">
                    <CpuChipIcon class="w-6 h-6 text-white" />
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="font-bold text-gray-900 dark:text-white truncate">
                      {{ model.name }}
                    </h3>
                    <div class="flex flex-wrap gap-2 mt-1">
                      <span v-if="model.size" class="text-xs px-2 py-0.5 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400">
                        {{ formatSize(model.size) }}
                      </span>
                      <span v-if="model.parameterSize" class="text-xs px-2 py-0.5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400">
                        {{ model.parameterSize }}
                      </span>
                    </div>
                  </div>
                  <div v-if="wizardData.baseModel === model.name" class="flex-shrink-0">
                    <CheckCircleIcon class="w-6 h-6 text-indigo-500" />
                  </div>
                </div>
              </button>
            </div>
          </div>

          <!-- Step 2: Name und Eigenschaften -->
          <div v-else-if="currentStep === 2" class="space-y-6">
            <div class="text-center mb-6">
              <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">Name und Eigenschaften</h2>
              <p class="text-gray-600 dark:text-gray-400">Definiere die Identität und Persönlichkeit deines Modells.</p>
            </div>

            <!-- Model Name -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Modellname *
              </label>
              <input
                v-model="wizardData.name"
                type="text"
                placeholder="z.B. nova, assistant, coder"
                class="
                  w-full px-4 py-3
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-gray-100
                  rounded-xl
                  focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent
                "
              />
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Nur Kleinbuchstaben, Zahlen und Bindestriche. Tag ":latest" wird automatisch hinzugefügt.
              </p>
            </div>

            <!-- Description -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Beschreibung (optional)
              </label>
              <input
                v-model="wizardData.description"
                type="text"
                placeholder="z.B. Hilfreicher deutscher Assistent"
                class="
                  w-full px-4 py-3
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-gray-100
                  rounded-xl
                  focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent
                "
              />
            </div>

            <!-- System Prompt -->
            <div>
              <div class="flex items-center justify-between mb-2">
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  System Prompt (Persönlichkeit)
                </label>
                <button
                  @click="triggerPromptFileInput"
                  type="button"
                  class="
                    flex items-center gap-2 px-3 py-1.5 text-sm
                    text-indigo-600 dark:text-indigo-400
                    hover:bg-indigo-50 dark:hover:bg-indigo-900/20
                    rounded-lg transition-all duration-200
                    border border-indigo-300 dark:border-indigo-600
                  "
                >
                  <ArrowUpTrayIcon class="w-4 h-4" />
                  <span>Aus Datei</span>
                </button>
                <input
                  ref="promptFileInput"
                  type="file"
                  @change="handlePromptFileSelect"
                  accept=".txt,.md"
                  class="hidden"
                />
              </div>

              <!-- Prompt-Vorlagen -->
              <div class="flex flex-wrap gap-2 mb-3">
                <button
                  v-for="template in promptTemplates"
                  :key="template.name"
                  @click="applyPromptTemplate(template)"
                  class="
                    px-3 py-1.5 text-xs font-medium rounded-lg
                    bg-gray-100 dark:bg-gray-700
                    hover:bg-indigo-100 dark:hover:bg-indigo-900/30
                    text-gray-600 dark:text-gray-400
                    hover:text-indigo-600 dark:hover:text-indigo-400
                    transition-colors
                  "
                >
                  {{ template.emoji }} {{ template.name }}
                </button>
              </div>

              <textarea
                v-model="wizardData.systemPrompt"
                rows="6"
                placeholder="z.B. Du bist Nova, ein hilfreicher deutscher KI-Assistent. Du antwortest immer höflich und präzise..."
                class="
                  w-full px-4 py-3
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-gray-100
                  rounded-xl
                  focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent
                  resize-none
                "
              ></textarea>
            </div>

            <!-- Erweiterte Parameter (collapsed) -->
            <div class="border-t border-gray-200 dark:border-gray-700 pt-4">
              <button
                @click="showAdvanced = !showAdvanced"
                class="flex items-center gap-2 text-sm font-medium text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 transition-colors"
              >
                <ChevronDownIcon class="w-5 h-5 transition-transform duration-200" :class="{ 'rotate-180': showAdvanced }" />
                Erweiterte Parameter
              </button>

              <Transition name="fade">
                <div v-if="showAdvanced" class="mt-4 grid grid-cols-2 gap-4">
                  <!-- Temperature -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Temperature: {{ wizardData.temperature }}
                    </label>
                    <input
                      v-model.number="wizardData.temperature"
                      type="range"
                      min="0"
                      max="2"
                      step="0.1"
                      class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg cursor-pointer accent-indigo-500"
                    />
                  </div>

                  <!-- Top P -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Top P: {{ wizardData.topP }}
                    </label>
                    <input
                      v-model.number="wizardData.topP"
                      type="range"
                      min="0"
                      max="1"
                      step="0.05"
                      class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg cursor-pointer accent-indigo-500"
                    />
                  </div>

                  <!-- Context Length -->
                  <div class="col-span-2">
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Context Length: {{ wizardData.numCtx.toLocaleString() }} Tokens
                    </label>
                    <input
                      v-model.number="wizardData.numCtx"
                      type="range"
                      min="2048"
                      max="131072"
                      step="2048"
                      class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg cursor-pointer accent-indigo-500"
                    />
                    <p class="text-xs text-gray-500 mt-1">Mehr Context = mehr VRAM benötigt</p>
                  </div>
                </div>
              </Transition>
            </div>
          </div>

          <!-- Step 3: Zusammenfassung -->
          <div v-else-if="currentStep === 3" class="space-y-6">
            <div class="text-center mb-6">
              <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">Zusammenfassung</h2>
              <p class="text-gray-600 dark:text-gray-400">Prüfe deine Einstellungen und erstelle das Modell.</p>
            </div>

            <!-- Summary Card -->
            <div v-if="!isCreating" class="bg-gradient-to-br from-indigo-50 to-purple-50 dark:from-indigo-900/20 dark:to-purple-900/20 rounded-2xl p-6 border border-indigo-200 dark:border-indigo-800">
              <div class="flex items-start gap-4 mb-6">
                <div class="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-500 shadow-lg">
                  <SparklesIcon class="w-8 h-8 text-white" />
                </div>
                <div>
                  <h3 class="text-xl font-bold text-gray-900 dark:text-white">
                    {{ wizardData.name || 'Unbenannt' }}:latest
                  </h3>
                  <p class="text-gray-600 dark:text-gray-400">{{ wizardData.description || 'Keine Beschreibung' }}</p>
                </div>
              </div>

              <div class="space-y-3">
                <div class="flex justify-between py-2 border-b border-indigo-200 dark:border-indigo-700">
                  <span class="text-gray-600 dark:text-gray-400">Basis-Modell</span>
                  <span class="font-medium text-gray-900 dark:text-white">{{ wizardData.baseModel }}</span>
                </div>
                <div class="flex justify-between py-2 border-b border-indigo-200 dark:border-indigo-700">
                  <span class="text-gray-600 dark:text-gray-400">Temperature</span>
                  <span class="font-medium text-gray-900 dark:text-white">{{ wizardData.temperature }}</span>
                </div>
                <div class="flex justify-between py-2 border-b border-indigo-200 dark:border-indigo-700">
                  <span class="text-gray-600 dark:text-gray-400">Context Length</span>
                  <span class="font-medium text-gray-900 dark:text-white">{{ wizardData.numCtx.toLocaleString() }} Tokens</span>
                </div>
                <div class="py-2">
                  <span class="text-gray-600 dark:text-gray-400">System Prompt</span>
                  <p class="mt-1 text-sm text-gray-900 dark:text-white bg-white/50 dark:bg-gray-800/50 rounded-lg p-3 max-h-32 overflow-y-auto">
                    {{ wizardData.systemPrompt || 'Kein System Prompt definiert' }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Creating Progress -->
            <div v-else class="flex flex-col items-center justify-center py-12">
              <div class="animate-spin rounded-full h-16 w-16 border-b-2 border-indigo-500 mb-6"></div>
              <p class="text-lg font-medium text-gray-900 dark:text-white mb-2">
                {{ creationProgress }}
              </p>
              <div class="w-full max-w-md bg-gray-200 dark:bg-gray-700 rounded-full h-2 mt-4">
                <div
                  class="bg-indigo-500 h-2 rounded-full transition-all duration-300"
                  :style="{ width: creationProgressPercent + '%' }"
                ></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="flex-shrink-0 p-6 border-t border-gray-200 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-900/50">
          <div class="flex justify-between items-center">
            <!-- Back Button -->
            <button
              v-if="currentStep > 1 && providerAvailable"
              @click="previousStep"
              :disabled="isCreating"
              class="
                px-6 py-2.5 rounded-xl
                text-gray-700 dark:text-gray-300
                bg-white dark:bg-gray-800
                border border-gray-300 dark:border-gray-600
                hover:bg-gray-50 dark:hover:bg-gray-700
                font-medium
                transition-all duration-200
                disabled:opacity-50 disabled:cursor-not-allowed
              "
            >
              Zurück
            </button>
            <div v-else></div>

            <div class="flex gap-3">
              <!-- Cancel Button -->
              <button
                @click="$emit('close')"
                :disabled="isCreating"
                class="
                  px-6 py-2.5 rounded-xl
                  text-gray-700 dark:text-gray-300
                  bg-white dark:bg-gray-800
                  border border-gray-300 dark:border-gray-600
                  hover:bg-gray-50 dark:hover:bg-gray-700
                  font-medium
                  transition-all duration-200
                  disabled:opacity-50 disabled:cursor-not-allowed
                "
              >
                Abbrechen
              </button>

              <!-- Next/Create Button -->
              <button
                v-if="currentStep < totalSteps"
                @click="nextStep"
                :disabled="!canProceed"
                class="
                  px-6 py-2.5 rounded-xl
                  bg-gradient-to-r from-indigo-500 to-purple-500
                  hover:from-indigo-600 hover:to-purple-600
                  text-white font-medium
                  shadow-sm hover:shadow-md
                  transition-all duration-200
                  disabled:opacity-50 disabled:cursor-not-allowed
                  flex items-center gap-2
                "
              >
                <span>Weiter</span>
                <ArrowRightIcon class="w-5 h-5" />
              </button>
              <button
                v-else
                @click="createModel"
                :disabled="!canCreate || isCreating"
                class="
                  px-6 py-2.5 rounded-xl
                  bg-gradient-to-r from-indigo-500 to-purple-500
                  hover:from-indigo-600 hover:to-purple-600
                  text-white font-medium
                  shadow-sm hover:shadow-md
                  transition-all duration-200
                  disabled:opacity-50 disabled:cursor-not-allowed
                  flex items-center gap-2
                "
              >
                <SparklesIcon class="w-5 h-5" />
                <span>Modell erstellen</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import {
  XMarkIcon,
  SparklesIcon,
  ChevronDownIcon,
  CpuChipIcon,
  CheckCircleIcon,
  ArrowRightIcon,
  ArrowUpTrayIcon,
  ExclamationTriangleIcon
} from '@heroicons/vue/24/outline'
import api from '../services/api'
import { useToast } from '../composables/useToast'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'created'])

const { success, error: errorToast } = useToast()

// Step configuration
const totalSteps = 3
const currentStep = ref(1)
const stepTitles = ['Basis-Modell', 'Eigenschaften', 'Erstellen']
const stepEmojis = ['1️⃣', '2️⃣', '3️⃣']

// Wizard data
const wizardData = ref({
  baseModel: '',
  name: '',
  description: '',
  systemPrompt: '',
  temperature: 0.8,
  topP: 0.9,
  topK: 40,
  repeatPenalty: 1.1,
  numPredict: 2048,
  numCtx: 8192
})

// UI state
const showAdvanced = ref(false)
const isCreating = ref(false)
const creationProgress = ref('')
const creationProgressPercent = ref(0)
const availableModels = ref([])
const isLoadingModels = ref(false)
const providerAvailable = ref(false)
const activeProvider = ref('')  // 'ollama' or 'java-llama-cpp'
const isCheckingProvider = ref(true)
const promptFileInput = ref(null)

// Prompt templates
const promptTemplates = [
  {
    name: 'Assistent',
    emoji: '🤖',
    prompt: 'Du bist ein hilfreicher, freundlicher KI-Assistent. Du antwortest auf Deutsch, präzise und gut strukturiert. Bei Unsicherheiten fragst du nach.'
  },
  {
    name: 'Coder',
    emoji: '💻',
    prompt: 'Du bist ein erfahrener Software-Entwickler. Du schreibst sauberen, gut dokumentierten Code und erklärst deine Lösungen. Du verwendest Best Practices und moderne Patterns.'
  },
  {
    name: 'Kreativ',
    emoji: '🎨',
    prompt: 'Du bist ein kreativer Schreibassistent. Du hilfst beim Verfassen von Texten, Geschichten und kreativen Inhalten. Du bist inspirierend und ideenreich.'
  },
  {
    name: 'Analyst',
    emoji: '📊',
    prompt: 'Du bist ein analytischer Datenexperte. Du analysierst Informationen gründlich, erkennst Muster und präsentierst Erkenntnisse klar strukturiert.'
  }
]

// Computed
const canProceed = computed(() => {
  switch (currentStep.value) {
    case 1:
      return wizardData.value.baseModel !== ''
    case 2:
      return wizardData.value.name.trim() !== ''
    default:
      return true
  }
})

const canCreate = computed(() => {
  return wizardData.value.baseModel !== '' &&
         wizardData.value.name.trim() !== '' &&
         providerAvailable.value
})

// Watch for show changes to reset and check provider
watch(() => props.show, (newVal) => {
  if (newVal) {
    resetWizard()
    checkProviderStatus()
  }
})

onMounted(() => {
  if (props.show) {
    checkProviderStatus()
  }
})

async function checkProviderStatus() {
  isCheckingProvider.value = true
  try {
    const status = await api.getProviderStatus()
    activeProvider.value = status.activeProvider

    // Check if Ollama or java-llama-cpp is available
    const ollamaAvailable = status.activeProvider === 'ollama' ||
                            (status.providerStatus?.ollama?.available === true)
    const llamaCppAvailable = status.activeProvider === 'java-llama-cpp' ||
                              (status.providerStatus?.['java-llama-cpp']?.available === true)

    providerAvailable.value = ollamaAvailable || llamaCppAvailable

    if (providerAvailable.value) {
      await loadAvailableModels()
    }
  } catch (error) {
    console.error('Error checking provider status:', error)
    providerAvailable.value = false
  } finally {
    isCheckingProvider.value = false
  }
}

async function loadAvailableModels() {
  isLoadingModels.value = true
  try {
    if (activeProvider.value === 'ollama') {
      // Load Ollama models
      const models = await api.getOllamaModels()
      availableModels.value = models || []
    } else {
      // Load GGUF models for java-llama-cpp
      const models = await api.getAvailableModels()
      availableModels.value = models || []
    }
  } catch (error) {
    console.error('Failed to load available models:', error)
    errorToast('Fehler beim Laden der Modelle')
    availableModels.value = []
  } finally {
    isLoadingModels.value = false
  }
}

function selectModel(model) {
  wizardData.value.baseModel = model.name
}

function canGoToStep(step) {
  if (step > currentStep.value) return false
  return true
}

function goToStep(step) {
  if (canGoToStep(step)) {
    currentStep.value = step
  }
}

function nextStep() {
  if (canProceed.value && currentStep.value < totalSteps) {
    currentStep.value++
  }
}

function previousStep() {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

function applyPromptTemplate(template) {
  wizardData.value.systemPrompt = template.prompt
}

function triggerPromptFileInput() {
  promptFileInput.value?.click()
}

async function handlePromptFileSelect(event) {
  const file = event.target.files?.[0]
  if (!file) return

  const validExtensions = ['.txt', '.md']
  const isValidType = validExtensions.some(ext => file.name.toLowerCase().endsWith(ext))

  if (!isValidType) {
    errorToast('Bitte eine .txt oder .md Datei wählen')
    event.target.value = ''
    return
  }

  if (file.size > 1024 * 1024) {
    errorToast('Datei zu groß (max. 1MB)')
    event.target.value = ''
    return
  }

  try {
    const text = await file.text()
    wizardData.value.systemPrompt = text
    success(`Prompt aus "${file.name}" geladen`)
  } catch (error) {
    errorToast('Fehler beim Lesen der Datei')
  } finally {
    event.target.value = ''
  }
}

async function createModel() {
  if (!canCreate.value) return

  isCreating.value = true
  creationProgress.value = 'Erstelle Modell...'
  creationProgressPercent.value = 0

  try {
    if (activeProvider.value === 'ollama') {
      // Create Ollama Custom Model
      const request = {
        name: wizardData.value.name.trim(),
        baseModel: wizardData.value.baseModel.trim(),
        description: wizardData.value.description.trim() || null,
        systemPrompt: wizardData.value.systemPrompt.trim() || null,
        temperature: wizardData.value.temperature,
        topP: wizardData.value.topP,
        topK: wizardData.value.topK,
        repeatPenalty: wizardData.value.repeatPenalty,
        numPredict: wizardData.value.numPredict,
        numCtx: wizardData.value.numCtx
      }

      await api.createCustomModel(request, (progress) => {
        creationProgress.value = progress

        // Progress estimation
        if (progress.includes('Bereite') || progress.includes('Vorbereite')) {
          creationProgressPercent.value = 10
        } else if (progress.includes('Erstelle Modell in Ollama')) {
          creationProgressPercent.value = 30
        } else if (progress.includes('pulling') || progress.includes('download')) {
          creationProgressPercent.value = 50
        } else if (progress.includes('writing') || progress.includes('verifying')) {
          creationProgressPercent.value = 70
        } else if (progress.includes('Speichere')) {
          creationProgressPercent.value = 90
        } else if (progress.includes('erfolgreich')) {
          creationProgressPercent.value = 100
        }
      })

      success('Custom Model erfolgreich erstellt!')
    } else {
      // Create GGUF Model Config for java-llama-cpp
      creationProgress.value = 'Speichere Konfiguration...'
      creationProgressPercent.value = 50

      const config = {
        name: wizardData.value.name.trim(),
        baseModel: wizardData.value.baseModel.trim(),
        description: wizardData.value.description.trim() || null,
        systemPrompt: wizardData.value.systemPrompt.trim() || null,
        temperature: wizardData.value.temperature,
        topP: wizardData.value.topP,
        topK: wizardData.value.topK,
        repeatPenalty: wizardData.value.repeatPenalty,
        maxTokens: wizardData.value.numPredict,
        contextSize: wizardData.value.numCtx,
        gpuLayers: 999  // Default: alle Layers auf GPU
      }

      await api.createGgufModelConfig(config)
      creationProgressPercent.value = 100

      success('GGUF Konfiguration erfolgreich erstellt!')
    }

    emit('created')
    emit('close')

  } catch (error) {
    console.error('Failed to create custom model:', error)
    errorToast('Fehler beim Erstellen: ' + (error.message || 'Unbekannter Fehler'))
  } finally {
    isCreating.value = false
    creationProgress.value = ''
    creationProgressPercent.value = 0
  }
}

function resetWizard() {
  currentStep.value = 1
  wizardData.value = {
    baseModel: '',
    name: '',
    description: '',
    systemPrompt: '',
    temperature: 0.8,
    topP: 0.9,
    topK: 40,
    repeatPenalty: 1.1,
    numPredict: 2048,
    numCtx: 8192
  }
  showAdvanced.value = false
  isCreating.value = false
  creationProgress.value = ''
  creationProgressPercent.value = 0
}

function formatSize(bytes) {
  if (!bytes) return ''
  const gb = bytes / (1024 * 1024 * 1024)
  if (gb >= 1) {
    return `${gb.toFixed(1)} GB`
  }
  const mb = bytes / (1024 * 1024)
  return `${mb.toFixed(0)} MB`
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s, max-height 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
