<template>
  <!-- Letter Agent Settings Section -->
  <section class="bg-gradient-to-br from-green-50 to-emerald-50 dark:from-green-900/20 dark:to-emerald-900/20 p-5 rounded-xl border border-green-200/50 dark:border-green-700/50 shadow-sm">
    <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
      <DocumentTextIcon class="w-5 h-5 text-green-500" />
      Briefe Agent
    </h3>

    <div class="space-y-4">
      <!-- Model Selection -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
          <CpuChipIcon class="w-4 h-4 text-green-500" />
          Modell für Briefe
        </label>
        <select
          v-model="localSettings.model"
          @change="handleModelChange"
          class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500 focus:border-transparent"
        >
          <option value="">-- Modell wählen --</option>
          <option v-for="model in documentModels" :key="model" :value="model">
            {{ model }}
          </option>
        </select>
        <p class="mt-2 text-xs text-gray-500 dark:text-gray-400">
          Nur Text-Modelle geeignet für Briefe und Dokumente (keine Code/Vision-Modelle)
        </p>
      </div>

      <!-- Upload Directory -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
          <FolderIcon class="w-4 h-4 text-green-500" />
          Speicherort
        </label>
        <input
          type="text"
          v-model="localSettings.uploadDirectory"
          @change="handleSettingsChange"
          placeholder="~/FleetNavigator/Documents"
          class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500 focus:border-transparent"
        />
      </div>

      <!-- Text Editor Selection -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
          <DocumentTextIcon class="w-4 h-4 text-green-500" />
          Textverarbeitungssoftware
        </label>
        <select
          v-model="localSettings.textEditor"
          @change="handleSettingsChange"
          class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500 focus:border-transparent"
        >
          <option value="libreoffice">LibreOffice Writer</option>
          <option value="onlyoffice">OnlyOffice</option>
          <option value="msword">Microsoft Word</option>
          <option value="abiword">AbiWord</option>
          <option value="gedit">gedit (Texteditor)</option>
        </select>
        <p class="mt-2 text-xs text-gray-500 dark:text-gray-400">
          Software wird nach Dokumentenerstellung automatisch geöffnet
        </p>
      </div>

      <!-- Info Box -->
      <div class="p-4 rounded-xl bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-700/50">
        <div class="flex items-start gap-2">
          <InformationCircleIcon class="w-5 h-5 text-green-600 dark:text-green-400 flex-shrink-0 mt-0.5" />
          <div class="flex-1">
            <p class="text-xs text-green-800 dark:text-green-200 font-medium">
              Agent arbeitet autonom
            </p>
            <p class="text-xs text-green-700 dark:text-green-300 mt-1">
              Der Briefe Agent nutzt das hier konfigurierte Modell automatisch.
              Du musst beim Erstellen von Briefen kein Modell mehr auswählen.
            </p>
          </div>
        </div>
      </div>

      <!-- Status Indicator -->
      <div class="flex items-center justify-between p-3 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
        <span class="text-sm font-medium text-gray-700 dark:text-gray-300">Status</span>
        <span class="px-3 py-1 rounded-full text-xs font-semibold bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 border border-green-200 dark:border-green-700">
          ✓ Aktiv
        </span>
      </div>
    </div>
  </section>

  <!-- Letter Templates Section -->
  <section class="bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 p-5 rounded-xl border border-blue-200/50 dark:border-blue-700/50 shadow-sm mt-6">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
        <DocumentTextIcon class="w-5 h-5 text-blue-500" />
        Briefvorlagen
      </h3>
      <button
        @click="showNewTemplateDialog = true"
        class="px-3 py-1.5 rounded-lg bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium transition-colors flex items-center gap-2"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        Neue Vorlage
      </button>
    </div>

    <!-- Templates List -->
    <div v-if="letterTemplates.length > 0" class="space-y-2">
      <div
        v-for="template in letterTemplates"
        :key="template.id"
        class="p-3 rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 hover:border-blue-300 dark:hover:border-blue-600 transition-all"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <h4 class="font-medium text-gray-900 dark:text-white">{{ template.name }}</h4>
            <p v-if="template.description" class="text-xs text-gray-500 dark:text-gray-400 mt-1">{{ template.description }}</p>
            <p class="text-xs text-gray-600 dark:text-gray-300 mt-2 font-mono">{{ template.prompt.substring(0, 100) }}...</p>
            <div class="flex items-center gap-2 mt-2">
              <span v-if="template.category" class="px-2 py-0.5 rounded text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300">
                {{ template.category }}
              </span>
            </div>
          </div>
          <div class="flex items-center gap-2 ml-4">
            <button
              @click="editTemplate(template)"
              class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-600 dark:text-gray-400 transition-colors"
              title="Bearbeiten"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
            <button
              @click="deleteTemplate(template.id)"
              class="p-2 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/30 text-red-600 dark:text-red-400 transition-colors"
              title="Löschen"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="text-center py-8 text-gray-500 dark:text-gray-400">
      <p>Noch keine Vorlagen gespeichert</p>
      <p class="text-xs mt-1">Erstelle eine Vorlage, um häufig genutzte Brieftypen schnell auszuwählen</p>
    </div>
  </section>

  <!-- New/Edit Template Dialog -->
  <div
    v-if="showNewTemplateDialog || editingTemplate"
    class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
    @click.self="closeTemplateDialog"
  >
    <div class="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
      <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-4">
        {{ editingTemplate ? 'Vorlage bearbeiten' : 'Neue Vorlage erstellen' }}
      </h3>

      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Name</label>
          <input
            v-model="templateForm.name"
            type="text"
            placeholder="z.B. Kündigung KFZ-Versicherung"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Kategorie (optional)</label>
          <input
            v-model="templateForm.category"
            type="text"
            placeholder="z.B. Kündigungen, Bewerbungen, Anfragen"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Beschreibung (optional)</label>
          <input
            v-model="templateForm.description"
            type="text"
            placeholder="Kurze Beschreibung der Vorlage"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Prompt</label>
          <textarea
            v-model="templateForm.prompt"
            rows="8"
            placeholder="Erstelle eine formale Kündigung für meine KFZ-Versicherung..."
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white font-mono text-sm"
          ></textarea>
        </div>
      </div>

      <div class="flex justify-end gap-3 mt-6">
        <button
          @click="closeTemplateDialog"
          class="px-4 py-2 rounded-xl border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
        >
          Abbrechen
        </button>
        <button
          @click="saveTemplate"
          :disabled="!templateForm.name || !templateForm.prompt"
          class="px-4 py-2 rounded-xl bg-blue-500 hover:bg-blue-600 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ editingTemplate ? 'Speichern' : 'Erstellen' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import {
  DocumentTextIcon,
  CpuChipIcon,
  FolderIcon,
  InformationCircleIcon
} from '@heroicons/vue/24/outline'
import api from '../services/api'
import { filterDocumentModels } from '../utils/modelFilters'
import { useChatStore } from '../stores/chatStore'
import { useToast } from '../composables/useToast'

const chatStore = useChatStore()
const { success, error } = useToast()

const localSettings = ref({
  model: '',
  uploadDirectory: '~/FleetNavigator/Documents',
  textEditor: 'libreoffice',
  status: 'active'
})

// Letter Templates State
const letterTemplates = ref([])
const showNewTemplateDialog = ref(false)
const editingTemplate = ref(null)
const templateForm = ref({
  name: '',
  category: '',
  description: '',
  prompt: ''
})

// Filter available models for document generation
const documentModels = computed(() => {
  const allModels = chatStore.models || []
  console.log('DocumentAgent - All models:', allModels) // Debug

  // Extract model names (same pattern as SettingsModal)
  const modelNames = allModels.map(m => m.name || m)
  console.log('DocumentAgent - Model names:', modelNames) // Debug

  const filtered = filterDocumentModels(modelNames)
  console.log('DocumentAgent - Filtered models:', filtered) // Debug
  return filtered
})

// Load settings on mount
onMounted(async () => {
  try {
    // Load available models first
    await chatStore.loadModels()

    // Then load settings
    const response = await api.getDocumentAgentSettings()
    if (response.data) {
      localSettings.value = {
        model: response.data.model || 'llama3.1:8b',
        uploadDirectory: response.data.uploadDirectory || '~/FleetNavigator/Documents',
        textEditor: response.data.textEditor || 'libreoffice',
        status: response.data.status || 'active'
      }
    }

    // Load letter templates
    await loadLetterTemplates()
  } catch (err) {
    console.error('Error loading document agent settings:', err)
    error('Fehler beim Laden der Einstellungen')
  }
})

// Letter Templates Functions
async function loadLetterTemplates() {
  try {
    letterTemplates.value = await api.getLetterTemplates()
  } catch (err) {
    console.error('Error loading letter templates:', err)
    error('Fehler beim Laden der Vorlagen')
  }
}

function editTemplate(template) {
  editingTemplate.value = template
  templateForm.value = {
    name: template.name,
    category: template.category || '',
    description: template.description || '',
    prompt: template.prompt
  }
}

async function saveTemplate() {
  try {
    if (editingTemplate.value) {
      // Update existing template
      await api.updateLetterTemplate(editingTemplate.value.id, templateForm.value)
      success('Vorlage aktualisiert')
    } else {
      // Create new template
      await api.createLetterTemplate(templateForm.value)
      success('Vorlage erstellt')
    }

    closeTemplateDialog()
    await loadLetterTemplates()
  } catch (err) {
    console.error('Error saving template:', err)
    error('Fehler beim Speichern der Vorlage')
  }
}

async function deleteTemplate(id) {
  if (confirm('Möchten Sie diese Vorlage wirklich löschen?')) {
    try {
      await api.deleteLetterTemplate(id)
      success('Vorlage gelöscht')
      await loadLetterTemplates()
    } catch (err) {
      console.error('Error deleting template:', err)
      error('Fehler beim Löschen der Vorlage')
    }
  }
}

function closeTemplateDialog() {
  showNewTemplateDialog.value = false
  editingTemplate.value = null
  templateForm.value = {
    name: '',
    category: '',
    description: '',
    prompt: ''
  }
}

// Handle model change
const handleModelChange = async () => {
  await saveSettings()
}

// Handle settings change
const handleSettingsChange = async () => {
  await saveSettings()
}

// Save settings to backend
const saveSettings = async () => {
  try {
    await api.updateDocumentAgentSettings(localSettings.value)
    success('Einstellungen gespeichert')
  } catch (err) {
    console.error('Error saving document agent settings:', err)
    error('Fehler beim Speichern')
  }
}
</script>
