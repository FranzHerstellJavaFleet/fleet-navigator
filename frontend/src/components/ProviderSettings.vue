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

    <!-- Provider Selection -->
    <div class="mb-6 p-4 bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-900/20 dark:to-purple-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center space-x-3">
          <div class="flex-shrink-0">
            <div class="w-3 h-3 bg-green-500 rounded-full animate-pulse"></div>
          </div>
          <div>
            <p class="text-sm font-medium text-blue-900 dark:text-blue-100">
              Aktiver Provider: <span class="font-bold">{{ getProviderDisplayName(selectedProvider) }}</span>
            </p>
            <p class="text-xs text-blue-700 dark:text-blue-300">
              {{ getProviderDescription(selectedProvider) }}
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

      <!-- Provider Toggle -->
      <div class="grid grid-cols-2 gap-3">
        <button
          @click="switchProvider('java-llama-cpp')"
          class="p-4 rounded-lg border-2 transition-all duration-200"
          :class="selectedProvider === 'java-llama-cpp'
            ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/40 shadow-lg'
            : 'border-gray-300 dark:border-gray-600 hover:border-blue-400'"
        >
          <div class="flex items-center justify-center gap-2 mb-2">
            <span class="text-2xl">🦙</span>
            <span class="font-semibold text-gray-900 dark:text-white">llama.cpp</span>
          </div>
          <p class="text-xs text-gray-600 dark:text-gray-400">Embedded GGUF Server</p>
        </button>

        <button
          @click="switchProvider('ollama')"
          class="p-4 rounded-lg border-2 transition-all duration-200"
          :class="selectedProvider === 'ollama'
            ? 'border-purple-500 bg-purple-50 dark:bg-purple-900/40 shadow-lg'
            : 'border-gray-300 dark:border-gray-600 hover:border-purple-400'"
        >
          <div class="flex items-center justify-center gap-2 mb-2">
            <span class="text-2xl">🔮</span>
            <span class="font-semibold text-gray-900 dark:text-white">Ollama</span>
          </div>
          <p class="text-xs text-gray-600 dark:text-gray-400">Lokaler Ollama Server</p>
        </button>
      </div>
    </div>

    <!-- llama.cpp Provider Card -->
    <div v-if="selectedProvider === 'java-llama-cpp'" class="mb-6 p-4 border-2 border-blue-500 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          🦙 llama.cpp
          <span class="text-xs px-2 py-0.5 bg-blue-500 text-white rounded-full">Aktiv</span>
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

    <!-- Ollama Provider Card -->
    <div v-if="selectedProvider === 'ollama'" class="mb-6 p-4 border-2 border-purple-500 bg-purple-50 dark:bg-purple-900/20 rounded-lg">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          🔮 Ollama
          <span class="text-xs px-2 py-0.5 bg-purple-500 text-white rounded-full">Aktiv</span>
        </h3>
        <div class="w-2 h-2 bg-green-500 rounded-full"></div>
      </div>
      <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
        Lokaler Ollama-Server mit Unterstützung für Llama, Mistral, Qwen und viele weitere Modelle.
      </p>

      <!-- Ollama Configuration -->
      <div class="space-y-4 mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
        <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300">
          Konfiguration
        </h4>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- Ollama Host -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              🌐 Server URL
            </label>
            <input
              v-model="config.ollama.host"
              type="text"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              placeholder="http://localhost:11434"
              disabled
              readonly
            />
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Standard Ollama Server Adresse
            </p>
          </div>

          <!-- Status -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              📊 Status
            </label>
            <div class="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-800">
              <span class="text-sm text-green-600 dark:text-green-400 font-medium">
                ✓ Verbunden
              </span>
            </div>
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Ollama Server ist erreichbar
            </p>
          </div>
        </div>

        <!-- Info Box -->
        <div class="mt-4 p-3 bg-gray-50 dark:bg-gray-900/50 border border-gray-200 dark:border-gray-700 rounded-lg">
          <p class="text-xs text-gray-600 dark:text-gray-400">
            ℹ️ <strong>Hinweis:</strong> Stelle sicher, dass Ollama auf deinem System läuft: <code>ollama serve</code>
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
const selectedProvider = ref('java-llama-cpp') // Default to llama.cpp
const providerStatus = ref({
  'java-llama-cpp': false,
  llamacpp: false,
  ollama: false
})

const config = ref({
  llamacpp: {
    binaryPath: './bin/llama-server',
    port: 2024,
    modelsDir: '/opt/fleet-navigator/models',
    autoStart: true,
    contextSize: 8192,
    gpuLayers: 999,
    threads: 8,
    enabled: true
  },
  ollama: {
    host: 'http://localhost:11434',
    enabled: false
  }
})

const loading = ref(false)

// Methods
async function loadProviders() {
  loading.value = true
  try {
    const response = await api.getProviderStatus()
    activeProvider.value = response.activeProvider
    selectedProvider.value = response.activeProvider || 'java-llama-cpp'
    providerStatus.value = response.providerStatus
  } catch (error) {
    console.error('Failed to load providers:', error)
  } finally {
    loading.value = false
  }
}

async function loadConfig() {
  try {
    const response = await api.getProviderConfig()
    if (response.llamacpp) {
      config.value.llamacpp = response.llamacpp
    }
    if (response.ollama) {
      config.value.ollama = response.ollama
    }
  } catch (error) {
    console.error('Failed to load config:', error)
  }
}

async function switchProvider(provider) {
  try {
    const response = await api.switchProvider(provider)
    selectedProvider.value = provider
    activeProvider.value = provider
    console.log(`Switched to provider: ${provider}`)

    // Log selected model
    if (response.selectedModel) {
      console.log(`Auto-selected model: ${response.selectedModel}`)
    }

    // Emit custom event to notify ModelManager
    window.dispatchEvent(new CustomEvent('provider-changed', {
      detail: {
        provider,
        selectedModel: response.selectedModel
      }
    }))

    // Reload page after short delay to apply all changes
    setTimeout(() => {
      window.location.reload()
    }, 500)
  } catch (error) {
    console.error('Failed to switch provider:', error)
  }
}

function getProviderDisplayName(provider) {
  if (provider === 'java-llama-cpp') return 'llama.cpp'
  if (provider === 'llamacpp') return 'llama.cpp'
  if (provider === 'ollama') return 'Ollama'
  return provider
}

function getProviderDescription(provider) {
  if (provider === 'java-llama-cpp' || provider === 'llamacpp') {
    return 'GGUF-Modelle mit embedded llama.cpp Server'
  }
  if (provider === 'ollama') {
    return 'Ollama-Modelle mit lokalem Server'
  }
  return ''
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
