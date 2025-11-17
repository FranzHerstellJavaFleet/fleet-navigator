<template>
  <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-start justify-between mb-4">
      <div>
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
          🔌 LLM Provider
        </h2>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          Fleet Navigator verwendet llama.cpp für lokale AI-Inferenz
        </p>
      </div>
    </div>

    <!-- Current Provider Status -->
    <div v-if="activeProvider" class="mb-6 p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-3">
          <div class="flex-shrink-0">
            <div class="w-3 h-3 bg-green-500 rounded-full animate-pulse"></div>
          </div>
          <div>
            <p class="text-sm font-medium text-blue-900 dark:text-blue-100">
              Aktiver Provider: <span class="font-bold">{{ getProviderDisplayName(activeProvider) }}</span>
            </p>
            <p class="text-xs text-blue-700 dark:text-blue-300">
              GGUF-Modelle mit embedded llama.cpp Server
            </p>
          </div>
        </div>
        <button
          @click="refreshProviders"
          class="px-3 py-1 text-sm bg-blue-100 dark:bg-blue-800 text-blue-700 dark:text-blue-100 rounded hover:bg-blue-200 dark:hover:bg-blue-700"
        >
          🔄 Aktualisieren
        </button>
      </div>
    </div>

    <!-- llama.cpp Provider Card (Always active) -->
    <div class="mb-6 p-4 border-2 border-blue-500 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          llama.cpp
          <span class="text-xs px-2 py-0.5 bg-blue-500 text-white rounded-full">Embedded</span>
        </h3>
        <div class="w-2 h-2 bg-green-500 rounded-full"></div>
      </div>
      <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
        Embedded AI-Server mit GGUF-Modell-Support. Über 40.000 Modelle von Hugging Face verfügbar.
      </p>

      <!-- llama.cpp Configuration -->
      <div v-if="config.llamacpp" class="space-y-4 mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
        <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300">
          Konfiguration
        </h4>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- Models Directory -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              📁 Modell-Verzeichnis
            </label>
            <input
              v-model="config.llamacpp.modelsDir"
              type="text"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              placeholder="./models"
              disabled
              readonly
            />
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Standardverzeichnis für GGUF-Modelle
            </p>
          </div>

          <!-- Context Size -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              📝 Context Size
            </label>
            <input
              v-model.number="config.llamacpp.contextSize"
              type="number"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              disabled
              readonly
            />
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Maximale Token-Anzahl im Kontext
            </p>
          </div>

          <!-- GPU Layers -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              🎮 GPU Layers
            </label>
            <input
              v-model.number="config.llamacpp.gpuLayers"
              type="number"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              disabled
              readonly
            />
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              999 = Alle Layers auf GPU (falls verfügbar)
            </p>
          </div>

          <!-- Port -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              🔌 Port
            </label>
            <input
              v-model.number="config.llamacpp.port"
              type="number"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              disabled
              readonly
            />
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Interner llama.cpp Server Port
            </p>
          </div>
        </div>

        <!-- Info Box -->
        <div class="mt-4 p-3 bg-gray-50 dark:bg-gray-900/50 border border-gray-200 dark:border-gray-700 rounded-lg">
          <p class="text-xs text-gray-600 dark:text-gray-400">
            ℹ️ <strong>Hinweis:</strong> Die Konfiguration ist schreibgeschützt und wird über die <code>application.properties</code> verwaltet.
            Ein Neustart ist erforderlich, um Änderungen zu übernehmen.
          </p>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../services/api'

// State
const activeProvider = ref('')
const providerStatus = ref({
  'java-llama-cpp': false,
  llamacpp: false
})

const config = ref({
  llamacpp: {
    binaryPath: './bin/llama-server',
    port: 2024,
    modelsDir: './models',
    autoStart: true,
    contextSize: 4096,
    gpuLayers: 999,
    threads: 0,
    enabled: true
  }
})

const loading = ref(false)

// Methods
async function loadProviders() {
  loading.value = true
  try {
    const response = await api.get('/api/llm/providers')
    activeProvider.value = response.data.activeProvider
    providerStatus.value = response.data.providerStatus
  } catch (error) {
    console.error('Failed to load providers:', error)
  } finally {
    loading.value = false
  }
}

async function loadConfig() {
  try {
    const response = await api.get('/api/llm/providers/config')
    if (response.data.llamacpp) {
      config.value.llamacpp = response.data.llamacpp
    }
  } catch (error) {
    console.error('Failed to load config:', error)
  }
}

function getProviderDisplayName(provider) {
  if (provider === 'java-llama-cpp') return 'llama.cpp (JNI)'
  if (provider === 'llamacpp') return 'llama.cpp'
  return provider
}

async function refreshProviders() {
  await loadProviders()
  await loadConfig()
}

// Initialize
onMounted(async () => {
  await loadProviders()
  await loadConfig()
})
</script>
