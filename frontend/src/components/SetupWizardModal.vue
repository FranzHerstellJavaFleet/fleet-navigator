<template>
  <!-- Modal Overlay -->
  <Transition name="modal">
    <div
      v-if="isVisible"
      class="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-[100] p-4"
    >
      <!-- Modal Dialog -->
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-2xl border border-gray-200 dark:border-gray-700 overflow-hidden">

        <!-- Header -->
        <div class="bg-gradient-to-r from-fleet-orange-500 to-amber-500 px-6 py-6">
          <div class="flex items-center gap-4">
            <div class="p-3 bg-white/20 rounded-xl">
              <RocketLaunchIcon class="w-10 h-10 text-white" />
            </div>
            <div>
              <h2 class="text-2xl font-bold text-white">Willkommen bei Fleet Navigator!</h2>
              <p class="text-white/80 text-sm mt-1">Erste Einrichtung - Lass uns loslegen</p>
            </div>
          </div>
        </div>

        <!-- Content (scrollable) -->
        <div class="p-8 max-h-[60vh] overflow-y-auto">

          <!-- Step 1: No Model Found -->
          <div v-if="step === 1" class="space-y-6">
            <div class="text-center mb-8">
              <div class="inline-flex items-center justify-center w-20 h-20 bg-amber-100 dark:bg-amber-900/30 rounded-full mb-4">
                <ExclamationTriangleIcon class="w-10 h-10 text-amber-600 dark:text-amber-400" />
              </div>
              <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-2">
                Kein KI-Modell gefunden
              </h3>
              <p class="text-gray-600 dark:text-gray-400">
                Fleet Navigator benötigt ein KI-Modell, um zu funktionieren.<br>
                Lade jetzt ein empfohlenes Modell herunter.
              </p>
            </div>

            <!-- Recommended Models -->
            <div class="space-y-3">
              <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                Empfohlene Modelle:
              </h4>

              <div
                v-for="model in recommendedModels"
                :key="model.name"
                @click="selectedModel = model.name"
                class="p-4 rounded-xl border-2 cursor-pointer transition-all duration-200"
                :class="selectedModel === model.name
                  ? 'border-fleet-orange-500 bg-fleet-orange-50 dark:bg-fleet-orange-900/20'
                  : 'border-gray-200 dark:border-gray-700 hover:border-fleet-orange-300'"
              >
                <div class="flex items-center gap-4">
                  <div class="flex-shrink-0">
                    <input
                      type="radio"
                      :value="model.name"
                      v-model="selectedModel"
                      class="w-5 h-5 text-fleet-orange-500 focus:ring-fleet-orange-500"
                    />
                  </div>
                  <div class="flex-1">
                    <div class="flex items-center gap-2">
                      <span class="font-semibold text-gray-900 dark:text-white">{{ model.displayName }}</span>
                      <span v-if="model.recommended" class="px-2 py-0.5 text-xs bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400 rounded-full">
                        Empfohlen
                      </span>
                    </div>
                    <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">{{ model.description }}</p>
                    <div class="flex items-center gap-4 mt-2 text-xs text-gray-500 dark:text-gray-500">
                      <span>{{ model.size }}</span>
                      <span>{{ model.params }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Info Box -->
            <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
              <div class="flex items-start gap-3">
                <InformationCircleIcon class="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
                <div class="text-sm text-blue-800 dark:text-blue-200">
                  <p>Der Download erfolgt automatisch. Je nach Modellgröße und Internetverbindung kann dies einige Minuten dauern.</p>
                </div>
              </div>
            </div>

            <!-- Directory Structure Info -->
            <div class="bg-gray-100 dark:bg-gray-900/50 rounded-lg p-4 mt-4">
              <h5 class="text-xs font-semibold text-gray-600 dark:text-gray-400 mb-2 flex items-center gap-2">
                <FolderIcon class="w-4 h-4" />
                Modell-Verzeichnis ({{ osType }})
              </h5>
              <div class="font-mono text-xs text-gray-700 dark:text-gray-300 space-y-1">
                <div class="flex items-center gap-2">
                  <span class="text-gray-500">📁</span>
                  <span>{{ modelsDirectory }}</span>
                </div>
                <div class="flex items-center gap-2 pl-4">
                  <span class="text-gray-500">├─ 📁</span>
                  <span>library/</span>
                  <span class="text-gray-500 text-[10px]">(vorinstallierte Modelle)</span>
                </div>
                <div class="flex items-center gap-2 pl-4">
                  <span class="text-gray-500">└─ 📁</span>
                  <span>custom/</span>
                  <span class="text-gray-500 text-[10px]">(eigene Modelle)</span>
                </div>
              </div>
              <p class="text-[10px] text-gray-500 dark:text-gray-500 mt-2">
                {{ osType === 'Windows' ? 'Windows: %LOCALAPPDATA%\\JavaFleet\\models\\' :
                   osType === 'Mac OS X' ? 'macOS: ~/.java-fleet/models/' :
                   'Linux: ~/.java-fleet/models/' }}
              </p>
            </div>
          </div>

          <!-- Step 2: Downloading -->
          <div v-if="step === 2" class="space-y-6">
            <div class="text-center mb-4">
              <div class="inline-flex items-center justify-center w-20 h-20 bg-blue-100 dark:bg-blue-900/30 rounded-full mb-4">
                <ArrowDownTrayIcon class="w-10 h-10 text-blue-600 dark:text-blue-400 animate-bounce" />
              </div>
              <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-2">
                Modell wird heruntergeladen
              </h3>
              <p class="text-gray-600 dark:text-gray-400">{{ selectedModel }}</p>
            </div>

            <!-- Progress -->
            <div class="space-y-4">
              <div class="text-center">
                <span class="text-4xl font-bold text-gray-900 dark:text-white">{{ downloadProgress }}%</span>
              </div>

              <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-4 overflow-hidden">
                <div
                  class="bg-gradient-to-r from-blue-500 to-cyan-500 h-4 rounded-full transition-all duration-300"
                  :style="{ width: downloadProgress + '%' }"
                ></div>
              </div>

              <div class="flex justify-between text-sm text-gray-600 dark:text-gray-400">
                <span>{{ downloadedSize }} / {{ totalSize }}</span>
                <span v-if="downloadSpeed">{{ downloadSpeed }} MB/s</span>
              </div>
            </div>

            <!-- Status -->
            <div class="bg-gray-100 dark:bg-gray-900/50 rounded-lg p-3 text-sm text-gray-600 dark:text-gray-400 font-mono">
              {{ downloadStatus || 'Verbinde...' }}
            </div>
          </div>

          <!-- Step 3: Complete -->
          <div v-if="step === 3" class="space-y-6 text-center">
            <div class="inline-flex items-center justify-center w-24 h-24 bg-green-100 dark:bg-green-900/30 rounded-full mb-4">
              <CheckCircleIcon class="w-14 h-14 text-green-600 dark:text-green-400" />
            </div>
            <h3 class="text-2xl font-bold text-gray-900 dark:text-white">
              Einrichtung abgeschlossen!
            </h3>
            <p class="text-gray-600 dark:text-gray-400">
              Fleet Navigator ist jetzt einsatzbereit.<br>
              Du kannst sofort mit der KI chatten.
            </p>
          </div>

        </div>

        <!-- Footer -->
        <div class="bg-gray-50 dark:bg-gray-900/50 px-8 py-5 flex justify-between items-center border-t border-gray-200 dark:border-gray-700">
          <div class="text-sm text-gray-500 dark:text-gray-400">
            <span v-if="step === 1">Schritt 1 von 2</span>
            <span v-else-if="step === 2">Download läuft...</span>
            <span v-else>Fertig!</span>
          </div>
          <div class="flex gap-3">
            <button
              v-if="step === 1"
              @click="startDownload"
              :disabled="!selectedModel"
              class="px-6 py-2.5 bg-fleet-orange-500 text-white rounded-lg font-semibold hover:bg-fleet-orange-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <ArrowDownTrayIcon class="w-5 h-5" />
              Modell herunterladen
            </button>
            <button
              v-if="step === 3"
              @click="completeSetup"
              class="px-6 py-2.5 bg-green-500 text-white rounded-lg font-semibold hover:bg-green-600 transition-colors flex items-center gap-2"
            >
              <CheckIcon class="w-5 h-5" />
              Los geht's!
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
  RocketLaunchIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  ArrowDownTrayIcon,
  CheckCircleIcon,
  CheckIcon,
  FolderIcon
} from '@heroicons/vue/24/outline'
import axios from 'axios'

const props = defineProps({
  isVisible: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['complete', 'close'])

const step = ref(1)
const selectedModel = ref('')
const downloadProgress = ref(0)
const downloadedSize = ref('0 MB')
const totalSize = ref('0 MB')
const downloadSpeed = ref('')
const downloadStatus = ref('')
const modelsDirectory = ref('')
const osType = ref('')

const recommendedModels = ref([
  {
    name: 'Qwen/Qwen2.5-3B-Instruct-GGUF',
    filename: 'qwen2.5-3b-instruct-q4_k_m.gguf',
    displayName: 'Qwen 2.5 3B Instruct',
    description: 'Kompakt, schnell, gut für alltägliche Aufgaben. Ideal für den Einstieg.',
    size: '~2.1 GB',
    params: '3B Parameter',
    recommended: true
  },
  {
    name: 'Qwen/Qwen2.5-7B-Instruct-GGUF',
    filename: 'qwen2.5-7b-instruct-q4_k_m.gguf',
    displayName: 'Qwen 2.5 7B Instruct',
    description: 'Leistungsstärker, bessere Qualität. Empfohlen für anspruchsvolle Aufgaben.',
    size: '~4.7 GB',
    params: '7B Parameter',
    recommended: false
  },
  {
    name: 'bartowski/Llama-3.2-3B-Instruct-GGUF',
    filename: 'Llama-3.2-3B-Instruct-Q4_K_M.gguf',
    displayName: 'Llama 3.2 3B Instruct',
    description: 'Meta\'s neuestes kleines Modell. Gut ausbalanciert.',
    size: '~2.0 GB',
    params: '3B Parameter',
    recommended: false
  }
])

// Pre-select recommended model and load system info
onMounted(async () => {
  const recommended = recommendedModels.value.find(m => m.recommended)
  if (recommended) {
    selectedModel.value = recommended.name
  }

  // Load system paths info
  try {
    const response = await axios.get('/api/system/setup-status')
    modelsDirectory.value = response.data.modelsDirectory || '~/.java-fleet/models'
    osType.value = response.data.osType || 'Linux'
  } catch (err) {
    console.error('Could not load system info:', err)
    modelsDirectory.value = '~/.java-fleet/models'
    osType.value = 'Linux'
  }
})

async function startDownload() {
  if (!selectedModel.value) return

  // Find selected model details
  const model = recommendedModels.value.find(m => m.name === selectedModel.value)
  if (!model) return

  step.value = 2
  downloadProgress.value = 0
  downloadStatus.value = 'Starte Download von HuggingFace...'

  try {
    // Build HuggingFace download URL
    const params = new URLSearchParams({
      modelId: model.name,
      filename: model.filename
    })
    const eventSource = new EventSource(`/api/model-store/huggingface/download?${params}`)

    eventSource.addEventListener('progress', (event) => {
      try {
        const progressMsg = event.data
        downloadStatus.value = progressMsg

        // Parse progress message like "📥 Downloading: 45% (1.2 GB / 2.6 GB)"
        const percentMatch = progressMsg.match(/(\d+)%/)
        if (percentMatch) {
          downloadProgress.value = parseInt(percentMatch[1])
        }

        // Extract sizes if available
        const sizeMatch = progressMsg.match(/\(([^)]+)\s*\/\s*([^)]+)\)/)
        if (sizeMatch) {
          downloadedSize.value = sizeMatch[1].trim()
          totalSize.value = sizeMatch[2].trim()
        }
      } catch (e) {
        console.error('Error parsing progress:', e)
      }
    })

    eventSource.addEventListener('complete', (event) => {
      console.log('Download complete:', event.data)
      eventSource.close()
      step.value = 3
    })

    eventSource.addEventListener('error', (event) => {
      console.error('Download error event:', event.data)
      downloadStatus.value = 'Fehler: ' + (event.data || 'Unbekannter Fehler')
    })

    eventSource.onerror = (error) => {
      console.error('SSE connection error:', error)
      // Check if download completed
      if (downloadProgress.value >= 99) {
        eventSource.close()
        step.value = 3
      } else {
        downloadStatus.value = 'Verbindungsfehler - Versuche erneut...'
      }
    }

  } catch (err) {
    console.error('Download error:', err)
    downloadStatus.value = 'Fehler: ' + (err.message || 'Unbekannter Fehler')
  }
}

function completeSetup() {
  emit('complete')
  emit('close')
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
</style>
