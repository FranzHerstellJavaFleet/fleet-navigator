<template>
  <Transition name="modal">
    <div v-if="show" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div
        class="
          bg-white/90 dark:bg-gray-800/90
          backdrop-blur-xl backdrop-saturate-150
          rounded-2xl shadow-2xl
          w-full max-w-3xl max-h-[90vh] overflow-hidden
          border border-gray-200/50 dark:border-gray-700/50
          transform transition-all duration-300
        "
      >
        <!-- Header with Gradient -->
        <div class="
          flex items-center justify-between p-6
          bg-gradient-to-r from-purple-500/10 to-indigo-500/10
          dark:from-purple-500/20 dark:to-indigo-500/20
          border-b border-gray-200/50 dark:border-gray-700/50
        ">
          <div class="flex items-center space-x-4">
            <div class="
              p-3 rounded-xl
              bg-gradient-to-br from-purple-500 to-indigo-500
              shadow-lg
            ">
              <CommandLineIcon class="w-7 h-7 text-white" />
            </div>
            <div>
              <h2 class="text-2xl font-bold text-gray-900 dark:text-white">OS Agent</h2>
              <p class="text-sm text-gray-600 dark:text-gray-400">KI-gestützte OS-Automatisierung</p>
            </div>
          </div>
          <button
            @click="$emit('close')"
            class="
              p-2 rounded-lg
              text-gray-400 hover:text-gray-600 dark:hover:text-gray-300
              hover:bg-gray-100 dark:hover:bg-gray-700
              transition-all duration-200
              transform hover:scale-110 active:scale-95
            "
          >
            <XMarkIcon class="w-6 h-6" />
          </button>
        </div>

        <!-- Scrollable Content -->
        <div class="overflow-y-auto max-h-[calc(90vh-180px)]">

        <!-- Coming Soon Banner -->
        <div class="
          bg-gradient-to-r from-yellow-50 to-amber-50
          dark:from-yellow-900/20 dark:to-amber-900/20
          border-l-4 border-yellow-400
          p-5 m-6 rounded-r-xl
          shadow-sm
        ">
          <div class="flex items-start">
            <div class="flex-shrink-0">
              <ExclamationTriangleIcon class="w-6 h-6 text-yellow-600 dark:text-yellow-400" />
            </div>
            <div class="ml-3">
              <h3 class="text-sm font-semibold text-yellow-900 dark:text-yellow-200">
                Coming Soon - In Entwicklung
              </h3>
              <div class="mt-2 text-sm text-yellow-800 dark:text-yellow-300">
                <p>Der OS Agent ist aktuell in Entwicklung. Du kannst bereits die Einstellungen konfigurieren,
                  die Funktionalität wird schrittweise implementiert.</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Security Warning -->
        <div class="
          bg-gradient-to-r from-red-50 to-rose-50
          dark:from-red-900/20 dark:to-rose-900/20
          border-l-4 border-red-400
          p-5 m-6 rounded-r-xl
          shadow-sm
        ">
          <div class="flex items-start">
            <div class="flex-shrink-0">
              <ShieldExclamationIcon class="w-6 h-6 text-red-600 dark:text-red-400" />
            </div>
            <div class="ml-3">
              <h3 class="text-sm font-semibold text-red-900 dark:text-red-200">
                Sicherheitshinweis
              </h3>
              <div class="mt-2 text-sm text-red-800 dark:text-red-300">
                <p>Der OS Agent führt Befehle auf deinem System aus. Stelle sicher, dass Sandbox-Modus und
                  Command-Whitelist aktiviert sind. Deaktiviere niemals diese Sicherheitsfeatures!</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Content -->
        <div class="p-6 space-y-6">
          <!-- Model Selection -->
          <div class="
            bg-gradient-to-br from-gray-50 to-gray-100
            dark:from-gray-900 dark:to-gray-800
            p-5 rounded-xl
            border border-gray-200/50 dark:border-gray-700/50
            shadow-sm
            transition-all duration-200 hover:shadow-md
          ">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center">
              <CpuChipIcon class="w-6 h-6 mr-2 text-purple-500" />
              Modell-Auswahl
            </h3>

          <div class="space-y-4">
            <!-- Standard Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Standard-Modell
              </label>
              <select
                v-model="settings.model"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              >
                <option value="llama3.2:3b">llama3.2:3b (schnell, empfohlen)</option>
                <option value="qwen2.5:7b">qwen2.5:7b</option>
                <option value="mistral:7b">mistral:7b</option>
              </select>
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Schnelles Modell empfohlen für OS-Befehle
              </p>
            </div>

            <!-- Vision Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Vision-Modell
              </label>
              <select
                v-model="settings.visionModel"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              >
                <option value="llava:13b">llava:13b</option>
                <option value="llava:7b">llava:7b</option>
                <option value="moondream">moondream</option>
              </select>
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Für Screenshot-Analyse
              </p>
            </div>
          </div>
        </div>

          <!-- OS-Specific Settings -->
          <div class="
            bg-gradient-to-br from-gray-50 to-gray-100
            dark:from-gray-900 dark:to-gray-800
            p-5 rounded-xl
            border border-gray-200/50 dark:border-gray-700/50
            shadow-sm
            transition-all duration-200 hover:shadow-md
          ">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center">
              <CommandLineIcon class="w-6 h-6 mr-2 text-purple-500" />
              OS-Einstellungen
            </h3>

          <div class="space-y-4">
            <!-- Allowed Commands -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Erlaubte Befehle (Whitelist)
              </label>
              <input
                :value="settings.allowedCommands.join(', ')"
                @input="updateCommands($event.target.value)"
                type="text"
                placeholder="ls, cd, pwd, cat, grep, find, echo"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              />
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Kommagetrennte Liste - nur diese Befehle sind erlaubt
              </p>
            </div>

            <!-- Working Directory -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Arbeitsverzeichnis
              </label>
              <input
                v-model="settings.workingDirectory"
                type="text"
                placeholder="~/fleet-workspace"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              />
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Sandbox-Verzeichnis für Befehle
              </p>
            </div>

            <!-- Execution Limits -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Max. Ausführungszeit (Sekunden)
                </label>
                <input
                  v-model.number="settings.maxExecutionTimeSeconds"
                  type="number"
                  min="1"
                  max="300"
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Max. Output-Zeilen
                </label>
                <input
                  v-model.number="settings.maxOutputLines"
                  type="number"
                  min="100"
                  max="10000"
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                />
              </div>
            </div>

            <!-- Security Toggle Options -->
            <div class="space-y-3 border-t border-gray-200 dark:border-gray-700 pt-4">
              <label class="flex items-center space-x-3 cursor-pointer">
                <input
                  v-model="settings.sandboxEnabled"
                  type="checkbox"
                  class="w-5 h-5 text-green-500 border-gray-300 rounded focus:ring-green-500"
                />
                <div class="flex items-center gap-2">
                  <ShieldCheckIcon class="w-5 h-5 text-green-500" />
                  <span class="text-sm font-semibold text-gray-700 dark:text-gray-300">
                    Sandbox-Modus aktivieren (EMPFOHLEN!)
                  </span>
                </div>
              </label>

              <label class="flex items-center space-x-3 cursor-pointer">
                <input
                  v-model="settings.fileSystemAccessEnabled"
                  type="checkbox"
                  class="w-5 h-5 text-fleet-orange-500 border-gray-300 rounded focus:ring-fleet-orange-500"
                />
                <div class="flex items-center gap-2">
                  <FolderIcon class="w-5 h-5 text-fleet-orange-500" />
                  <span class="text-sm text-gray-700 dark:text-gray-300">
                    Dateisystem-Zugriff erlauben
                  </span>
                </div>
              </label>

              <label class="flex items-center space-x-3 cursor-pointer">
                <input
                  v-model="settings.networkAccessEnabled"
                  type="checkbox"
                  class="w-5 h-5 text-red-500 border-gray-300 rounded focus:ring-red-500"
                />
                <div class="flex items-center gap-2">
                  <ExclamationTriangleIcon class="w-5 h-5 text-red-500" />
                  <span class="text-sm text-gray-700 dark:text-gray-300">
                    Netzwerk-Zugriff erlauben (Sicherheitsrisiko!)
                  </span>
                </div>
              </label>
            </div>
          </div>
        </div>

          <!-- Feature Preview -->
          <div class="
            bg-gradient-to-br from-purple-50 to-indigo-50
            dark:from-purple-900/20 dark:to-indigo-900/20
            p-5 rounded-xl
            border border-purple-200/50 dark:border-purple-700/50
            shadow-sm
          ">
            <h3 class="text-sm font-semibold text-purple-900 dark:text-purple-200 mb-3 flex items-center">
              <SparklesIcon class="w-5 h-5 mr-2" />
              Geplante Features
            </h3>
            <ul class="text-sm text-purple-800 dark:text-purple-300 space-y-2">
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>Sichere Befehls-Ausführung in Sandbox</span>
              </li>
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>Dateioperationen (ls, cat, grep, find)</span>
              </li>
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>Screenshot-Analyse mit Vision Model</span>
              </li>
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>Automatische Task-Ausführung</span>
              </li>
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>Git-Integration (status, log, diff)</span>
              </li>
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>System-Monitoring (CPU, RAM, Disk)</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- Footer -->
        <div class="
          flex items-center justify-end gap-3 p-6
          bg-gray-50/50 dark:bg-gray-900/50
          border-t border-gray-200/50 dark:border-gray-700/50
        ">
          <button
            @click="$emit('close')"
            class="
              px-6 py-2.5 rounded-xl
              border border-gray-300 dark:border-gray-600
              text-gray-700 dark:text-gray-300
              font-medium
              hover:bg-gray-100 dark:hover:bg-gray-700
              transition-all duration-200
              transform hover:scale-105 active:scale-95
            "
          >
            Abbrechen
          </button>
          <button
            @click="saveSettings"
            :disabled="saving"
            class="
              px-6 py-2.5 rounded-xl
              bg-gradient-to-r from-purple-500 to-indigo-500
              text-white font-medium
              shadow-lg shadow-purple-500/30
              hover:shadow-xl hover:shadow-purple-500/40
              disabled:opacity-50 disabled:cursor-not-allowed
              transition-all duration-200
              transform hover:scale-105 active:scale-95
              flex items-center gap-2
            "
          >
            <ArrowDownTrayIcon v-if="!saving" class="w-5 h-5" />
            <ArrowPathIcon v-else class="w-5 h-5 animate-spin" />
            {{ saving ? 'Speichere...' : 'Einstellungen speichern' }}
          </button>
        </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'
import {
  CommandLineIcon,
  XMarkIcon,
  ExclamationTriangleIcon,
  ShieldExclamationIcon,
  ShieldCheckIcon,
  CpuChipIcon,
  CheckCircleIcon,
  SparklesIcon,
  ArrowDownTrayIcon,
  ArrowPathIcon,
  FolderIcon
} from '@heroicons/vue/24/outline'
import api from '../services/api'
import { useToast } from '../composables/useToast'

const { success, error: errorToast } = useToast()

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'saved'])

const settings = ref({
  model: 'llama3.2:3b',
  visionModel: 'llava:13b',
  allowedCommands: ['ls', 'cd', 'pwd', 'cat', 'grep', 'find', 'echo'],
  sandboxEnabled: true,
  maxExecutionTimeSeconds: 30,
  workingDirectory: '~/fleet-workspace',
  fileSystemAccessEnabled: true,
  networkAccessEnabled: false,
  maxOutputLines: 1000,
  status: 'coming_soon'
})

const saving = ref(false)

// Load settings when modal opens
watch(() => props.show, async (newVal) => {
  if (newVal) {
    try {
      const response = await api.getOSAgentSettings()
      settings.value = response.data
    } catch (error) {
      console.error('Failed to load OS Agent settings:', error)
    }
  }
})

const updateCommands = (value) => {
  settings.value.allowedCommands = value.split(',').map(c => c.trim()).filter(c => c)
}

const saveSettings = async () => {
  saving.value = true
  try {
    await api.updateOSAgentSettings(settings.value)
    success('OS Agent Einstellungen gespeichert')
    emit('saved')
    emit('close')
  } catch (err) {
    console.error('Failed to save OS Agent settings:', err)
    errorToast('Fehler beim Speichern der Einstellungen')
  } finally {
    saving.value = false
  }
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
