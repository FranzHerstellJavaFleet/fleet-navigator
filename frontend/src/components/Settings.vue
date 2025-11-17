<template>
  <div class="h-full flex flex-col bg-gray-50 dark:bg-gray-900">
    <!-- Header -->
    <div class="border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">⚙️ Einstellungen</h1>
      <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
        Konfiguriere die automatische Modellauswahl und andere Einstellungen
      </p>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto p-6">
      <div class="max-w-4xl mx-auto space-y-6">

        <!-- Provider Settings -->
        <ProviderSettings />

        <!-- Smart Model Selection -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <div class="flex items-start justify-between mb-4">
            <div>
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
                🤖 Intelligente Modellauswahl
              </h2>
              <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
                Automatisches Routing zu spezialisierten Modellen basierend auf der Aufgabe
              </p>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                v-model="settings.enabled"
                class="sr-only peer"
                @change="saveSettings"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600"></div>
            </label>
          </div>

          <div v-if="!settings.enabled" class="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4 mb-4">
            <p class="text-sm text-yellow-800 dark:text-yellow-200">
              ⚠️ Intelligente Modellauswahl ist deaktiviert. Es wird immer das Standard-Modell verwendet.
            </p>
          </div>

          <div class="space-y-4" :class="{ 'opacity-50 pointer-events-none': !settings.enabled }">
            <!-- Code Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                💻 Code-Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für Code-Generierung, Debugging, technische Fragen)
                </span>
              </label>
              <select
                v-model="settings.codeModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option v-for="model in codeModels.length > 0 ? codeModels : availableModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
              <p v-if="codeModels.length === 0" class="text-xs text-yellow-600 dark:text-yellow-400 mt-2">
                ⚠️ Keine Coder-Modelle gefunden - zeige alle Modelle
              </p>
            </div>

            <!-- Fast Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                ⚡ Schnelles Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für einfache Fragen, Definitionen)
                </span>
              </label>
              <select
                v-model="settings.fastModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option v-for="model in availableModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
            </div>

            <!-- Vision Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                👁️ Vision-Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für Bildanalyse PDF/JPEG/PNG)
                </span>
              </label>
              <select
                v-model="settings.visionModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option v-for="model in visionModels.length > 0 ? visionModels : availableModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
              <p v-if="visionModels.length === 0" class="text-xs text-yellow-600 dark:text-yellow-400 mt-2">
                ⚠️ Keine Vision-Modelle gefunden - zeige alle Modelle
              </p>
              <p v-else class="text-xs text-green-600 dark:text-green-400 mt-2">
                ✅ Wird automatisch bei Bild-Upload aktiviert
              </p>
            </div>

            <!-- Email Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                📧 Email-Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für Email-Klassifizierung & Antwort-Generierung)
                </span>
              </label>
              <select
                v-model="settings.emailModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">-- Standard-Modell verwenden --</option>
                <option v-for="model in fastModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                💡 Tipp: Verwende ein schnelles, kleines Modell (1B-3B) für Email-Klassifizierung!
              </p>
            </div>

            <!-- Log Analysis Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                📊 Log-Analyse-Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für Log-Datei-Analyse & Fehlersuche)
                </span>
              </label>
              <select
                v-model="settings.logAnalysisModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">-- Standard-Modell verwenden --</option>
                <option v-for="model in codeModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                💡 Tipp: Coder-Modelle (DeepSeek-Coder, Qwen2.5-Coder) sind ideal für Log-Analyse!
              </p>
            </div>

            <!-- Document Generation Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                📝 Brief-/Dokumenten-Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für Brief-Generierung & formale Texte)
                </span>
              </label>
              <select
                v-model="settings.documentModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">-- Standard-Modell verwenden --</option>
                <option v-for="model in germanModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                💡 Tipp: Qwen-Modelle haben hervorragende Deutsch-Kenntnisse für Briefe!
              </p>
            </div>
          </div>
        </div>

        <!-- Default Model -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            🎯 Standard-Modell
          </h2>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">
            Dieses Modell wird für neue Chats verwendet und als Fallback, wenn keine automatische Auswahl möglich ist.
          </p>

          <div v-if="settings.enabled" class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4 mb-4">
            <p class="text-sm text-blue-800 dark:text-blue-200">
              ℹ️ <strong>Hinweis:</strong> Wenn die intelligente Modellauswahl aktiviert ist, wird das Standard-Modell
              automatisch durch das passende spezialisierte Modell ersetzt (Code-, Fast- oder Vision-Modell).
            </p>
          </div>

          <select
            v-model="settings.defaultModel"
            @change="saveSettings"
            class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option v-for="model in availableModels" :key="model.name" :value="model.name">
              {{ model.name }} ({{ formatSize(model.size) }})
            </option>
          </select>
        </div>

        <!-- Info Panel -->
        <div class="bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-900/20 dark:to-purple-900/20 rounded-lg border border-blue-200 dark:border-blue-800 p-6">
          <h3 class="text-md font-semibold text-gray-900 dark:text-white mb-3">
            📚 Wie funktioniert die intelligente Modellauswahl?
          </h3>
          <ul class="space-y-2 text-sm text-gray-700 dark:text-gray-300">
            <li class="flex items-start">
              <span class="mr-2">💻</span>
              <span><strong>Code-Aufgaben:</strong> Automatische Erkennung von Code-Keywords (function, class, bug, etc.) und technischen Mustern → Code-Modell wird verwendet</span>
            </li>
            <li class="flex items-start">
              <span class="mr-2">⚡</span>
              <span><strong>Einfache Fragen:</strong> Kurze Fragen mit "Was ist", "Erkläre", etc. → Schnelles Modell für effiziente Antworten</span>
            </li>
            <li class="flex items-start">
              <span class="mr-2">🎯</span>
              <span><strong>Komplexe Aufgaben:</strong> Alle anderen Anfragen → Standard-Modell wird verwendet</span>
            </li>
            <li class="flex items-start">
              <span class="mr-2">👁️</span>
              <span><strong>Bilder:</strong> Wenn Bilder hochgeladen werden → Vision-Modell wird automatisch verwendet</span>
            </li>
          </ul>
        </div>

        <!-- Sampling Parameters -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <div class="mb-4">
            <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
              🎛️ LLM Sampling Parameter
            </h2>
            <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
              Feinsteuerung aller Parameter für text- und vision-basierte Modelle
            </p>
          </div>

          <div class="p-4 bg-red-100 border-2 border-red-500 text-red-900 font-bold text-xl">
            TEST: Wenn du das hier siehst, wird die Sektion geladen!
          </div>

          <SamplingParametersPanel
            v-model="defaultSamplingParams"
            :model-name="settings.defaultModel"
          />

          <div class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
            <button
              @click="saveSamplingParams"
              class="w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
            >
              <i class="fas fa-save mr-2"></i>
              Parameter als Standard speichern
            </button>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-2 text-center">
              Diese Parameter werden für alle neuen Chats als Standard verwendet
            </p>
          </div>
        </div>

        <!-- Save Status -->
        <div v-if="saveStatus" class="text-center">
          <div class="inline-flex items-center px-4 py-2 rounded-lg"
               :class="saveStatus.success ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200' : 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200'">
            <span class="mr-2">{{ saveStatus.success ? '✅' : '❌' }}</span>
            <span>{{ saveStatus.message }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import * as api from '../services/api'
import ProviderSettings from './ProviderSettings.vue'
import SamplingParametersPanel from './SamplingParametersPanel.vue'

const settings = ref({
  enabled: true,
  codeModel: 'qwen2.5-coder:7b',
  fastModel: 'llama3.2:3b',
  visionModel: 'llava:13b',
  emailModel: '',
  logAnalysisModel: '',
  documentModel: '',
  defaultModel: 'qwen2.5-coder:7b'
})

const availableModels = ref([])
const saveStatus = ref(null)
const defaultSamplingParams = ref({})

// Gefilterte Model-Listen basierend auf Capabilities
const visionModels = computed(() => {
  return availableModels.value.filter(m =>
    m.name.toLowerCase().includes('llava') ||
    m.name.toLowerCase().includes('bakllava') ||
    m.name.toLowerCase().includes('minicpm-v') ||
    m.name.toLowerCase().includes('vision')
  )
})

const codeModels = computed(() => {
  return availableModels.value.filter(m =>
    m.name.toLowerCase().includes('coder') ||
    m.name.toLowerCase().includes('code') ||
    m.name.toLowerCase().includes('deepseek') ||
    m.name.toLowerCase().includes('starcoder')
  )
})

const fastModels = computed(() => {
  return availableModels.value.filter(m =>
    m.name.toLowerCase().includes('1b') ||
    m.name.toLowerCase().includes('3b') ||
    m.name.toLowerCase().includes('tiny') ||
    m.name.toLowerCase().includes('mini')
  )
})

const germanModels = computed(() => {
  return availableModels.value.filter(m =>
    m.name.toLowerCase().includes('qwen') ||
    m.name.toLowerCase().includes('german') ||
    m.name.toLowerCase().includes('leo')
  )
})

onMounted(async () => {
  await loadSettings()
  await loadAvailableModels()
})

async function loadSettings() {
  try {
    const response = await api.getModelSelectionSettings()
    settings.value = response

    // Load task-specific models separately
    const emailModel = await api.getEmailModel()
    settings.value.emailModel = emailModel || ''

    const logAnalysisModel = await api.getLogAnalysisModel()
    settings.value.logAnalysisModel = logAnalysisModel || ''

    const documentModel = await api.getDocumentModel()
    settings.value.documentModel = documentModel || ''
  } catch (error) {
    console.error('Failed to load settings:', error)
  }
}

async function loadAvailableModels() {
  try {
    const response = await api.getAvailableModels()
    availableModels.value = response
  } catch (error) {
    console.error('Failed to load models:', error)
  }
}

async function saveSettings() {
  try {
    // Save model selection settings (without task-specific models)
    const { emailModel, logAnalysisModel, documentModel, ...modelSelectionSettings } = settings.value
    await api.updateModelSelectionSettings(modelSelectionSettings)

    // Save task-specific models separately
    await api.updateEmailModel(emailModel)
    await api.updateLogAnalysisModel(logAnalysisModel)
    await api.updateDocumentModel(documentModel)

    saveStatus.value = { success: true, message: 'Einstellungen erfolgreich gespeichert!' }

    // Clear status after 3 seconds
    setTimeout(() => {
      saveStatus.value = null
    }, 3000)
  } catch (error) {
    console.error('Failed to save settings:', error)
    saveStatus.value = { success: false, message: 'Fehler beim Speichern der Einstellungen' }
  }
}

async function saveSamplingParams() {
  try {
    // Speichere Sampling Parameters im localStorage für neue Chats
    localStorage.setItem('defaultSamplingParams', JSON.stringify(defaultSamplingParams.value))

    saveStatus.value = { success: true, message: 'Sampling Parameter erfolgreich gespeichert!' }

    // Clear status after 3 seconds
    setTimeout(() => {
      saveStatus.value = null
    }, 3000)
  } catch (error) {
    console.error('Failed to save sampling params:', error)
    saveStatus.value = { success: false, message: 'Fehler beim Speichern der Parameter' }
  }
}

function formatSize(bytes) {
  if (!bytes) return 'N/A'
  const gb = bytes / (1024 * 1024 * 1024)
  return `${gb.toFixed(1)} GB`
}
</script>
