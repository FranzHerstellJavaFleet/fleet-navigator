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
          bg-gradient-to-r from-blue-500/10 to-cyan-500/10
          dark:from-blue-500/20 dark:to-cyan-500/20
          border-b border-gray-200/50 dark:border-gray-700/50
        ">
          <div class="flex items-center space-x-4">
            <div class="
              p-3 rounded-xl
              bg-gradient-to-br from-blue-500 to-cyan-500
              shadow-lg
            ">
              <EnvelopeIcon class="w-7 h-7 text-white" />
            </div>
            <div>
              <h2 class="text-2xl font-bold text-gray-900 dark:text-white">Email Agent</h2>
              <p class="text-sm text-gray-600 dark:text-gray-400">KI-gestützte E-Mail-Verwaltung</p>
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
                <p>Der Email Agent ist aktuell in Entwicklung. Du kannst bereits die Einstellungen konfigurieren,
                  die Funktionalität wird schrittweise implementiert.</p>
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
              <CpuChipIcon class="w-6 h-6 mr-2 text-blue-500" />
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
                <option value="qwen2.5:7b">qwen2.5:7b</option>
                <option value="llama3.2:3b">llama3.2:3b</option>
                <option value="mistral:7b">mistral:7b</option>
              </select>
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Wird für E-Mail-Analyse und -Generierung verwendet
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
                Für E-Mail-Anhänge mit Bildern
              </p>
            </div>
          </div>
        </div>

          <!-- Email-Specific Settings -->
          <div class="
            bg-gradient-to-br from-gray-50 to-gray-100
            dark:from-gray-900 dark:to-gray-800
            p-5 rounded-xl
            border border-gray-200/50 dark:border-gray-700/50
            shadow-sm
            transition-all duration-200 hover:shadow-md
          ">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center">
              <EnvelopeIcon class="w-6 h-6 mr-2 text-blue-500" />
              E-Mail-Einstellungen
            </h3>

          <div class="space-y-4">
            <!-- IMAP Server -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  IMAP Server
                </label>
                <input
                  v-model="settings.imapServer"
                  type="text"
                  placeholder="imap.gmail.com"
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  IMAP Port
                </label>
                <input
                  v-model.number="settings.imapPort"
                  type="number"
                  placeholder="993"
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                />
              </div>
            </div>

            <!-- SMTP Server -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  SMTP Server
                </label>
                <input
                  v-model="settings.smtpServer"
                  type="text"
                  placeholder="smtp.gmail.com"
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  SMTP Port
                </label>
                <input
                  v-model.number="settings.smtpPort"
                  type="number"
                  placeholder="587"
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                />
              </div>
            </div>

            <!-- Toggle Options -->
            <div class="space-y-3">
              <label class="flex items-center space-x-3 cursor-pointer">
                <input
                  v-model="settings.autoCategorizationEnabled"
                  type="checkbox"
                  class="w-5 h-5 text-fleet-orange-500 border-gray-300 rounded focus:ring-fleet-orange-500"
                />
                <span class="text-sm text-gray-700 dark:text-gray-300">
                  Automatische E-Mail-Kategorisierung aktivieren
                </span>
              </label>

              <label class="flex items-center space-x-3 cursor-pointer">
                <input
                  v-model="settings.useSsl"
                  type="checkbox"
                  class="w-5 h-5 text-fleet-orange-500 border-gray-300 rounded focus:ring-fleet-orange-500"
                />
                <span class="text-sm text-gray-700 dark:text-gray-300">
                  SSL/TLS verwenden (empfohlen)
                </span>
              </label>
            </div>
          </div>
        </div>

          <!-- Feature Preview -->
          <div class="
            bg-gradient-to-br from-blue-50 to-cyan-50
            dark:from-blue-900/20 dark:to-cyan-900/20
            p-5 rounded-xl
            border border-blue-200/50 dark:border-blue-700/50
            shadow-sm
          ">
            <h3 class="text-sm font-semibold text-blue-900 dark:text-blue-200 mb-3 flex items-center">
              <SparklesIcon class="w-5 h-5 mr-2" />
              Geplante Features
            </h3>
            <ul class="text-sm text-blue-800 dark:text-blue-300 space-y-2">
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>Intelligente E-Mail-Kategorisierung (Wichtig, Spam, Newsletter)</span>
              </li>
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>Automatische Antwortvorschläge generieren</span>
              </li>
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>E-Mail-Zusammenfassungen erstellen</span>
              </li>
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>Anhänge analysieren (PDFs, Bilder, Dokumente)</span>
              </li>
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>Multi-Account-Unterstützung</span>
              </li>
              <li class="flex items-start">
                <CheckCircleIcon class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
                <span>Kalendererkennung und Terminvorschläge</span>
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
              bg-gradient-to-r from-blue-500 to-cyan-500
              text-white font-medium
              shadow-lg shadow-blue-500/30
              hover:shadow-xl hover:shadow-blue-500/40
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
  EnvelopeIcon,
  XMarkIcon,
  ExclamationTriangleIcon,
  CpuChipIcon,
  CheckCircleIcon,
  SparklesIcon,
  ArrowDownTrayIcon,
  ArrowPathIcon
} from '@heroicons/vue/24/outline'
import api from '../services/api'
import { useToast } from '../composables/useToast'

const { success, error: errorToast } = useToast()

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'saved'])

const settings = ref({
  model: 'qwen2.5:7b',
  visionModel: 'llava:13b',
  imapServer: 'imap.gmail.com',
  smtpServer: 'smtp.gmail.com',
  imapPort: 993,
  smtpPort: 587,
  autoCategorizationEnabled: true,
  useSsl: true,
  status: 'coming_soon'
});

const saving = ref(false);

// Load settings when modal opens
watch(() => props.show, async (newVal) => {
  if (newVal) {
    try {
      const response = await api.getEmailAgentSettings();
      settings.value = response.data;
    } catch (error) {
      console.error('Failed to load Email Agent settings:', error);
    }
  }
});

const saveSettings = async () => {
  saving.value = true
  try {
    await api.updateEmailAgentSettings(settings.value)
    success('Email Agent Einstellungen gespeichert')
    emit('saved')
    emit('close')
  } catch (err) {
    console.error('Failed to save Email Agent settings:', err)
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
