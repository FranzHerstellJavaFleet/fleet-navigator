<template>
  <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-start justify-between mb-4">
      <div>
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
          🔌 LLM Provider
        </h2>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          Wähle den LLM-Provider für Fleet Navigator
        </p>
      </div>
    </div>

    <!-- Provider Selection -->
    <div class="mb-6 p-4 bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-900/20 dark:to-purple-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center space-x-3">
          <div class="flex-shrink-0">
            <div class="w-3 h-3 rounded-full animate-pulse" :class="providerStatusColor"></div>
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

      <!-- Provider Toggle - 3 Options -->
      <div class="grid grid-cols-3 gap-3">
        <!-- llama-server (Default für FleetCode) -->
        <button
          @click="switchProvider('llama-server')"
          class="p-4 rounded-lg border-2 transition-all duration-200 relative"
          :class="selectedProvider === 'llama-server'
            ? 'border-green-500 bg-green-50 dark:bg-green-900/40 shadow-lg'
            : 'border-gray-300 dark:border-gray-600 hover:border-green-400'"
        >
          <div class="absolute -top-2 -right-2 px-2 py-0.5 bg-green-500 text-white text-xs rounded-full font-bold">
            Default
          </div>
          <div class="flex items-center justify-center gap-2 mb-2">
            <span class="text-2xl">🖥️</span>
            <span class="font-semibold text-gray-900 dark:text-white">llama-server</span>
          </div>
          <p class="text-xs text-gray-600 dark:text-gray-400">Embedded Server (FleetCode)</p>
        </button>

        <!-- java-llama-cpp -->
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

        <!-- Ollama -->
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

    <!-- FleetCode Info Box -->
    <div class="mb-6 p-4 bg-gradient-to-r from-cyan-50 to-blue-50 dark:from-cyan-900/20 dark:to-blue-900/20 border border-cyan-300 dark:border-cyan-700 rounded-lg">
      <div class="flex items-start gap-3">
        <span class="text-2xl">⚡</span>
        <div>
          <h4 class="font-semibold text-cyan-900 dark:text-cyan-100">FleetCode AI Coding Agent</h4>
          <p class="text-sm text-cyan-700 dark:text-cyan-300 mt-1">
            FleetCode funktioniert <strong>nur</strong> mit dem <strong>llama-server</strong> auf Port <strong>{{ llamaServerPort }}</strong>.
            Der llama-server wird von Fleet Navigator verwaltet und kann hier gestartet werden.
          </p>
          <div class="mt-2 flex items-center gap-2">
            <span class="text-xs px-2 py-1 rounded-full" :class="llamaServerOnline ? 'bg-green-500 text-white' : 'bg-red-500 text-white'">
              {{ llamaServerOnline ? '✓ llama-server online' : '✗ llama-server offline' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- llama-server Provider Card -->
    <div v-if="selectedProvider === 'llama-server'" class="mb-6 p-4 border-2 border-green-500 bg-green-50 dark:bg-green-900/20 rounded-lg">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          🖥️ llama-server
          <span class="text-xs px-2 py-0.5 bg-green-500 text-white rounded-full">Aktiv</span>
        </h3>
        <div class="flex items-center gap-2">
          <span v-if="llamaServerPid" class="text-xs text-gray-500 dark:text-gray-400">
            PID: {{ llamaServerPid }}
          </span>
          <div class="w-3 h-3 rounded-full" :class="llamaServerOnline ? 'bg-green-500 animate-pulse' : 'bg-red-500'"></div>
        </div>
      </div>
      <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
        Embedded llama-server, verwaltet von Fleet Navigator. Erforderlich für FleetCode AI Coding Agent.
      </p>

      <!-- Aktuell laufendes Modell -->
      <div v-if="currentModel" class="mb-4 p-3 bg-green-100 dark:bg-green-900/40 border border-green-300 dark:border-green-700 rounded-lg">
        <p class="text-sm text-green-800 dark:text-green-200">
          <strong>🟢 Läuft:</strong> {{ currentModel.split('/').pop() }}
        </p>
      </div>

      <!-- llama-server Configuration -->
      <div class="space-y-4 mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
        <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300">
          Server-Konfiguration
        </h4>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- Modell-Auswahl -->
          <div class="md:col-span-2">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              🦙 Modell auswählen
            </label>
            <select
              v-model="selectedModel"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              :disabled="llamaServerOnline"
            >
              <option value="" disabled>-- Modell wählen --</option>
              <option v-for="model in availableModels" :key="model" :value="model">
                {{ model }}
              </option>
            </select>
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              {{ availableModels.length }} Modelle in ~/.java-fleet/models/library/
            </p>
          </div>

          <!-- Port -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              🔌 Server Port
            </label>
            <input
              v-model.number="llamaServerPort"
              type="number"
              min="1024"
              max="65535"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              placeholder="2026"
              :disabled="llamaServerOnline"
            />
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Standard: 2026 (FleetCode erwartet diesen Port)
            </p>
          </div>

          <!-- Status -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              📊 Server Status
            </label>
            <div class="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-800">
              <span v-if="checkingLlamaServer || startingServer" class="text-sm text-gray-500">
                🔄 {{ startingServer ? 'Startet...' : 'Prüfe...' }}
              </span>
              <span v-else-if="llamaServerOnline" class="text-sm text-green-600 dark:text-green-400 font-medium">
                ✓ Online auf Port {{ llamaServerPort }}
              </span>
              <span v-else class="text-sm text-red-600 dark:text-red-400 font-medium">
                ✗ Offline
              </span>
            </div>
          </div>

          <!-- Context Size -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              📝 Context Size
            </label>
            <input
              v-model.number="contextSize"
              type="number"
              min="512"
              max="131072"
              step="512"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              :disabled="llamaServerOnline"
            />
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Tokens im Kontext (Standard: 8192)
            </p>
          </div>

          <!-- GPU Layers -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              🎮 GPU Layers
            </label>
            <input
              v-model.number="gpuLayers"
              type="number"
              min="0"
              max="999"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              :disabled="llamaServerOnline"
            />
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              0 = CPU only, 99 = alle Layers auf GPU
            </p>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex flex-wrap gap-3 mt-4">
          <!-- Start Button -->
          <button
            v-if="!llamaServerOnline"
            @click="startLlamaServer"
            :disabled="startingServer || !selectedModel"
            class="px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-lg text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ startingServer ? '🔄 Startet...' : '▶️ Server starten' }}
          </button>

          <!-- Stop Button -->
          <button
            v-if="llamaServerOnline"
            @click="stopLlamaServer"
            :disabled="stoppingServer"
            class="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg text-sm font-medium disabled:opacity-50"
          >
            {{ stoppingServer ? '🔄 Stoppt...' : '⏹️ Server stoppen' }}
          </button>

          <!-- Status prüfen -->
          <button
            @click="checkLlamaServerStatus"
            :disabled="checkingLlamaServer"
            class="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg text-sm font-medium disabled:opacity-50"
          >
            🔍 Status prüfen
          </button>

          <!-- Restart Button (nur wenn online) -->
          <button
            v-if="llamaServerOnline"
            @click="restartLlamaServer"
            :disabled="restartingServer"
            class="px-4 py-2 bg-orange-500 hover:bg-orange-600 text-white rounded-lg text-sm font-medium disabled:opacity-50"
          >
            {{ restartingServer ? '🔄 Neustart...' : '🔄 Neu starten' }}
          </button>
        </div>

        <!-- Info Box -->
        <div v-if="!llamaServerOnline && availableModels.length === 0" class="mt-4 p-3 bg-yellow-50 dark:bg-yellow-900/30 border border-yellow-300 dark:border-yellow-700 rounded-lg">
          <p class="text-xs text-yellow-800 dark:text-yellow-200">
            ⚠️ <strong>Keine Modelle gefunden!</strong> Bitte lade GGUF-Modelle in
            <code class="bg-gray-200 dark:bg-gray-700 px-1 rounded">~/.java-fleet/models/library/</code>
          </p>
        </div>

        <!-- Success Info -->
        <div v-if="llamaServerOnline" class="mt-4 p-3 bg-green-50 dark:bg-green-900/30 border border-green-300 dark:border-green-700 rounded-lg">
          <p class="text-xs text-green-800 dark:text-green-200">
            ✅ <strong>Server läuft!</strong> FleetCode ist jetzt verfügbar. Du kannst den Server über die Buttons oben steuern.
          </p>
        </div>
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
import { ref, computed, onMounted } from 'vue'
import api from '../services/api'
import axios from 'axios'

// State
const activeProvider = ref('')
const selectedProvider = ref('llama-server') // Default to llama-server for FleetCode
const providerStatus = ref({
  'llama-server': false,
  'java-llama-cpp': false,
  llamacpp: false,
  ollama: false
})

// llama-server specific state
const llamaServerPort = ref(2026)
const llamaServerOnline = ref(false)
const checkingLlamaServer = ref(false)
const restartingServer = ref(false)
const startingServer = ref(false)
const stoppingServer = ref(false)
const llamaServerPid = ref(null)
const currentModel = ref(null)
const availableModels = ref([])
const selectedModel = ref('')
const contextSize = ref(8192)
const gpuLayers = ref(99)

const config = ref({
  llamaServer: {
    port: 2026,
    host: 'http://localhost'
  },
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

// Computed
const providerStatusColor = computed(() => {
  if (selectedProvider.value === 'llama-server') {
    return llamaServerOnline.value ? 'bg-green-500' : 'bg-red-500'
  }
  return 'bg-green-500'
})

// Methods
async function loadProviders() {
  loading.value = true
  try {
    const response = await api.getProviderStatus()
    activeProvider.value = response.activeProvider
    // Default to llama-server if not set
    selectedProvider.value = response.activeProvider || 'llama-server'
    providerStatus.value = response.providerStatus

    // Load llama-server port from settings if available
    if (response.llamaServerPort) {
      llamaServerPort.value = response.llamaServerPort
    }
  } catch (error) {
    console.error('Failed to load providers:', error)
    // Default to llama-server on error
    selectedProvider.value = 'llama-server'
  } finally {
    loading.value = false
  }

  // Check llama-server status on load
  await checkLlamaServerStatus()
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
    if (response.llamaServer) {
      config.value.llamaServer = response.llamaServer
      llamaServerPort.value = response.llamaServer.port || 2026
    }
  } catch (error) {
    console.error('Failed to load config:', error)
  }
}

async function checkLlamaServerStatus() {
  checkingLlamaServer.value = true
  try {
    const response = await axios.get(`/api/llm/providers/llama-server/health?port=${llamaServerPort.value}`, {
      timeout: 3000
    })
    llamaServerOnline.value = response.data?.online === true
  } catch (error) {
    console.log('llama-server check failed:', error.message)
    llamaServerOnline.value = false
  } finally {
    checkingLlamaServer.value = false
  }
}

async function restartLlamaServer() {
  restartingServer.value = true
  try {
    await axios.post(`/api/llm/providers/llama-server/restart`, {
      port: llamaServerPort.value,
      model: selectedModel.value || null,
      contextSize: contextSize.value,
      gpuLayers: gpuLayers.value
    })
    // Wait and check status
    setTimeout(async () => {
      await checkLlamaServerStatus()
      await loadLlamaServerStatus()
      restartingServer.value = false
    }, 3000)
  } catch (error) {
    console.error('Failed to restart llama-server:', error)
    restartingServer.value = false
  }
}

async function startLlamaServer() {
  if (!selectedModel.value) {
    alert('Bitte wähle ein Modell aus!')
    return
  }

  startingServer.value = true
  try {
    const response = await axios.post(`/api/llm/providers/llama-server/start`, {
      model: selectedModel.value,
      port: llamaServerPort.value,
      contextSize: contextSize.value,
      gpuLayers: gpuLayers.value
    })

    if (response.data.success) {
      llamaServerPid.value = response.data.pid
      currentModel.value = response.data.model

      // Warte kurz und prüfe Status
      setTimeout(async () => {
        await checkLlamaServerStatus()
        await loadLlamaServerStatus()
        startingServer.value = false
      }, 2000)
    } else {
      alert(response.data.message)
      startingServer.value = false
    }
  } catch (error) {
    console.error('Failed to start llama-server:', error)
    alert('Fehler beim Starten: ' + (error.response?.data?.message || error.message))
    startingServer.value = false
  }
}

async function stopLlamaServer() {
  stoppingServer.value = true
  try {
    const response = await axios.post(`/api/llm/providers/llama-server/stop`)

    if (response.data.success) {
      llamaServerOnline.value = false
      llamaServerPid.value = null
      currentModel.value = null
    }

    // Kurz warten und Status prüfen
    setTimeout(async () => {
      await checkLlamaServerStatus()
      stoppingServer.value = false
    }, 1000)
  } catch (error) {
    console.error('Failed to stop llama-server:', error)
    stoppingServer.value = false
  }
}

async function loadLlamaServerStatus() {
  try {
    const response = await axios.get(`/api/llm/providers/llama-server/status`)
    llamaServerOnline.value = response.data.online
    llamaServerPid.value = response.data.pid || null
    currentModel.value = response.data.model || null
    if (response.data.port) {
      llamaServerPort.value = response.data.port
    }
  } catch (error) {
    console.log('llama-server status check failed:', error.message)
  }
}

async function loadAvailableModels() {
  try {
    const response = await axios.get(`/api/llm/providers/llama-server/models`)
    availableModels.value = response.data.models || []

    // Wenn ein Modell läuft, dieses vorauswählen
    if (currentModel.value) {
      const modelName = currentModel.value.split('/').pop()
      if (availableModels.value.includes(modelName)) {
        selectedModel.value = modelName
      }
    } else if (availableModels.value.length > 0 && !selectedModel.value) {
      // Erstes Modell vorauswählen
      selectedModel.value = availableModels.value[0]
    }
  } catch (error) {
    console.log('Failed to load available models:', error.message)
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

    // Check llama-server status if switching to it
    if (provider === 'llama-server') {
      await checkLlamaServerStatus()
      await loadLlamaServerStatus()
      await loadAvailableModels()
    }

    // KEIN automatischer Reload mehr!
    // Der User kann jetzt in Ruhe die Einstellungen sehen und den Server starten

  } catch (error) {
    console.error('Failed to switch provider:', error)
    const errorMsg = error.response?.data?.message || error.message || 'Unbekannter Fehler'
    alert('Provider-Wechsel fehlgeschlagen:\n' + errorMsg)
  }
}

function getProviderDisplayName(provider) {
  if (provider === 'llama-server') return 'llama-server'
  if (provider === 'java-llama-cpp') return 'llama.cpp'
  if (provider === 'llamacpp') return 'llama.cpp'
  if (provider === 'ollama') return 'Ollama'
  return provider
}

function getProviderDescription(provider) {
  if (provider === 'llama-server') {
    return 'Embedded llama-server (Port ' + llamaServerPort.value + ') - Für FleetCode erforderlich'
  }
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
  await loadLlamaServerStatus()
  await loadAvailableModels()
})
</script>
