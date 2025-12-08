<template>
  <Transition name="modal">
    <div v-if="show" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-2">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-xl max-h-[95vh] overflow-hidden flex flex-col">
        <!-- Header mit Buttons -->
        <div class="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-purple-50 to-indigo-50 dark:from-purple-900/20 dark:to-indigo-900/20">
          <h3 class="text-lg font-bold text-gray-900 dark:text-white">
            {{ isEditing ? 'Experte bearbeiten' : 'Neuer Experte' }}
          </h3>
          <div class="flex items-center gap-2">
            <button
              @click="$emit('close')"
              class="px-3 py-1.5 text-sm rounded-lg text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
            >
              Abbrechen
            </button>
            <button
              @click="save"
              :disabled="!canSave || isSaving"
              class="px-3 py-1.5 text-sm rounded-lg bg-purple-500 hover:bg-purple-600 text-white font-medium transition-colors disabled:opacity-50"
            >
              {{ isSaving ? 'Speichern...' : 'Speichern' }}
            </button>
          </div>
        </div>

        <!-- Scrollbarer Form-Bereich -->
        <div class="flex-1 overflow-y-auto p-4 space-y-3">
          <!-- Row 1: Name + Rolle -->
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Name *</label>
              <input
                v-model="form.name"
                type="text"
                placeholder="z.B. Roland"
                class="w-full px-3 py-2 text-sm bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Rolle *</label>
              <input
                v-model="form.role"
                type="text"
                placeholder="z.B. Rechtsanwalt"
                class="w-full px-3 py-2 text-sm bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              />
            </div>
          </div>

          <!-- Row 2: Avatar + Beschreibung + Benachrichtigung -->
          <div class="flex gap-3">
            <!-- Avatar kompakt -->
            <div class="flex-shrink-0">
              <div
                v-if="form.avatarUrl"
                class="w-14 h-14 rounded-lg overflow-hidden border-2 border-purple-300 dark:border-purple-600 cursor-pointer relative group"
                @click="triggerAvatarFileInput"
              >
                <img :src="form.avatarUrl" alt="Avatar" class="w-full h-full object-cover" @error="handleAvatarError" />
                <div class="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                  <PhotoIcon class="w-5 h-5 text-white" />
                </div>
              </div>
              <div
                v-else
                class="w-14 h-14 rounded-lg bg-gray-200 dark:bg-gray-700 flex items-center justify-center border-2 border-dashed border-gray-300 dark:border-gray-600 cursor-pointer hover:border-purple-400 transition-colors"
                @click="triggerAvatarFileInput"
              >
                <PhotoIcon class="w-6 h-6 text-gray-400" />
              </div>
              <input ref="avatarFileInput" type="file" accept="image/*" @change="handleAvatarUpload" class="hidden" />
            </div>
            <div class="flex-1 space-y-2">
              <input
                v-model="form.description"
                type="text"
                placeholder="Kurze Beschreibung (optional)"
                class="w-full px-3 py-1.5 text-sm bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              />
              <label class="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400 cursor-pointer">
                <input type="checkbox" v-model="form.showSwitchNotification" class="w-3.5 h-3.5 text-purple-600 rounded" />
                Benachrichtigung beim Wechsel
              </label>
            </div>
          </div>

          <!-- Row 3: Provider + Modell -->
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Provider</label>
              <select
                v-model="form.providerType"
                class="w-full px-3 py-2 text-sm bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              >
                <option value="">System-Standard</option>
                <option value="llama-server">llama-server</option>
                <option value="java-llama-cpp">java-llama-cpp</option>
                <option value="ollama">Ollama</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                Modell * <span class="text-gray-400">({{ availableModels?.length || 0 }})</span>
              </label>
              <select
                v-model="form.baseModel"
                class="w-full px-3 py-2 text-sm bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              >
                <option value="" disabled>-- Bitte wählen --</option>
                <option v-for="model in availableModels" :key="model.name" :value="model.name">
                  {{ model.displayName || model.name }}
                </option>
              </select>
            </div>
          </div>

          <!-- GGUF Model (conditional) -->
          <div v-if="form.providerType === 'llama-server' || form.providerType === 'java-llama-cpp'">
            <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">GGUF-Modell (optional)</label>
            <input
              v-model="form.ggufModel"
              type="text"
              placeholder="z.B. qwen2.5-coder-7b-instruct-q4_k_m.gguf"
              class="w-full px-3 py-2 text-sm bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            />
          </div>

          <!-- Base Prompt -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="text-xs font-medium text-gray-700 dark:text-gray-300">Basis-Prompt *</label>
              <button
                @click="triggerBasePromptFileInput"
                type="button"
                class="flex items-center gap-1 px-2 py-1 text-xs rounded bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 hover:bg-purple-200 dark:hover:bg-purple-900/50"
              >
                <DocumentArrowUpIcon class="w-3 h-3" />
                Datei
              </button>
              <input ref="basePromptFileInput" type="file" accept=".txt,.md" @change="loadBasePromptFromFile" class="hidden" />
            </div>
            <textarea
              v-model="form.basePrompt"
              rows="4"
              placeholder="Du bist [Name], ein/e erfahrene/r [Rolle]..."
              class="w-full px-3 py-2 text-sm bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent resize-none font-mono"
            ></textarea>
          </div>

          <!-- Personality Prompt -->
          <div>
            <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Kommunikationsstil (optional)</label>
            <textarea
              v-model="form.personalityPrompt"
              rows="2"
              placeholder="z.B. Duze den Benutzer, sei freundlich..."
              class="w-full px-3 py-2 text-sm bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent resize-none font-mono"
            ></textarea>
          </div>

          <!-- Collapsible: Erweiterte Einstellungen -->
          <div class="border-t border-gray-200 dark:border-gray-700 pt-3">
            <button
              @click="showAdvanced = !showAdvanced"
              class="flex items-center gap-1 text-xs font-medium text-purple-600 dark:text-purple-400"
            >
              <ChevronDownIcon class="w-3 h-3 transition-transform" :class="{ 'rotate-180': showAdvanced }" />
              Erweiterte Einstellungen
            </button>

            <div v-if="showAdvanced" class="mt-3 grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">Temperature: {{ form.defaultTemperature }}</label>
                <input v-model.number="form.defaultTemperature" type="range" min="0" max="2" step="0.1" class="w-full h-1.5" />
              </div>
              <div>
                <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">Top P: {{ form.defaultTopP }}</label>
                <input v-model.number="form.defaultTopP" type="range" min="0" max="1" step="0.05" class="w-full h-1.5" />
              </div>
              <div>
                <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">Context: {{ form.defaultNumCtx?.toLocaleString() }}</label>
                <input v-model.number="form.defaultNumCtx" type="range" min="2048" max="131072" step="2048" class="w-full h-1.5" />
              </div>
              <div>
                <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">Max Tokens: {{ form.defaultMaxTokens?.toLocaleString() }}</label>
                <input v-model.number="form.defaultMaxTokens" type="range" min="256" max="16384" step="256" class="w-full h-1.5" />
              </div>
            </div>
          </div>

          <!-- Collapsible: RAG-Einstellungen -->
          <div class="border-t border-gray-200 dark:border-gray-700 pt-3">
            <button
              @click="showRAG = !showRAG"
              class="flex items-center gap-1 text-xs font-medium text-purple-600 dark:text-purple-400"
            >
              <ChevronDownIcon class="w-3 h-3 transition-transform" :class="{ 'rotate-180': showRAG }" />
              <GlobeAltIcon class="w-3 h-3" />
              RAG & Websuche
            </button>

            <div v-if="showRAG" class="mt-3 space-y-3">
              <!-- Websuche -->
              <label class="flex items-center gap-2 text-xs cursor-pointer">
                <input type="checkbox" v-model="form.autoWebSearch" class="w-3.5 h-3.5 text-blue-600 rounded" />
                <span class="text-gray-700 dark:text-gray-300">Immer Web durchsuchen</span>
              </label>

              <div v-if="form.autoWebSearch" class="ml-5 space-y-2">
                <div>
                  <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">Such-Domains (komma-getrennt)</label>
                  <input
                    v-model="form.searchDomains"
                    type="text"
                    placeholder="z.B. gesetze-im-internet.de, dejure.org"
                    class="w-full px-3 py-1.5 text-xs bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 rounded-lg"
                  />
                </div>
                <div>
                  <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">Max. Ergebnisse: {{ form.maxSearchResults }}</label>
                  <input v-model.number="form.maxSearchResults" type="range" min="1" max="10" class="w-full h-1.5" />
                </div>
              </div>

              <!-- Dateisuche -->
              <label class="flex items-center gap-2 text-xs cursor-pointer">
                <input type="checkbox" v-model="form.autoFileSearch" class="w-3.5 h-3.5 text-green-600 rounded" />
                <span class="text-gray-700 dark:text-gray-300">Dateisuche aktivieren</span>
              </label>

              <!-- Dokumenten-Verzeichnis -->
              <div>
                <label class="block text-xs text-gray-600 dark:text-gray-400 mb-1">Dokumenten-Verzeichnis</label>
                <div class="flex items-center gap-1">
                  <span class="text-xs text-gray-400">~/Dokumente/Fleet-Navigator/</span>
                  <input
                    v-model="form.documentDirectory"
                    type="text"
                    placeholder="Name"
                    class="flex-1 px-2 py-1 text-xs bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 rounded"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- Collapsible: Blickwinkel (nur beim Bearbeiten) -->
          <div v-if="isEditing" class="border-t border-gray-200 dark:border-gray-700 pt-3">
            <button
              @click="showModes = !showModes"
              class="flex items-center gap-1 text-xs font-medium text-purple-600 dark:text-purple-400"
            >
              <ChevronDownIcon class="w-3 h-3 transition-transform" :class="{ 'rotate-180': showModes }" />
              <EyeIcon class="w-3 h-3" />
              Blickwinkel ({{ modes.length }})
            </button>

            <div v-if="showModes" class="mt-3 space-y-2">
              <!-- Existing Modes -->
              <div
                v-for="mode in modes"
                :key="mode.id"
                class="flex items-center justify-between p-2 bg-purple-50 dark:bg-purple-900/20 rounded-lg text-xs"
              >
                <span class="font-medium text-gray-900 dark:text-white">{{ mode.name }}</span>
                <div class="flex gap-1">
                  <button @click="openEditMode(mode)" class="p-1 text-amber-500 hover:bg-amber-100 dark:hover:bg-amber-900/30 rounded">
                    <PencilIcon class="w-3 h-3" />
                  </button>
                  <button @click="deleteMode(mode)" class="p-1 text-red-500 hover:bg-red-100 dark:hover:bg-red-900/30 rounded">
                    <TrashIcon class="w-3 h-3" />
                  </button>
                </div>
              </div>

              <!-- Add Mode -->
              <div v-if="!showAddMode">
                <button @click="showAddMode = true" class="text-xs text-purple-600 dark:text-purple-400 hover:underline">
                  + Blickwinkel hinzufügen
                </button>
              </div>
              <div v-else class="p-2 bg-gray-50 dark:bg-gray-900/50 rounded-lg space-y-2">
                <input
                  v-model="newMode.name"
                  type="text"
                  placeholder="Name (z.B. Kritisch)"
                  class="w-full px-2 py-1 text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded"
                />
                <textarea
                  v-model="newMode.promptAddition"
                  rows="2"
                  placeholder="Prompt-Zusatz..."
                  class="w-full px-2 py-1 text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded resize-none"
                ></textarea>
                <div class="flex justify-end gap-2">
                  <button @click="showAddMode = false; resetNewMode()" class="px-2 py-1 text-xs text-gray-500">Abbrechen</button>
                  <button @click="addMode" :disabled="!newMode.name?.trim()" class="px-2 py-1 text-xs bg-purple-500 text-white rounded disabled:opacity-50">
                    Hinzufügen
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer mit Buttons (auch unten) -->
        <div class="flex justify-end gap-2 px-4 py-3 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900/50">
          <button
            @click="$emit('close')"
            class="px-3 py-1.5 text-sm rounded-lg text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
          >
            Abbrechen
          </button>
          <button
            @click="save"
            :disabled="!canSave || isSaving"
            class="px-4 py-1.5 text-sm rounded-lg bg-purple-500 hover:bg-purple-600 text-white font-medium transition-colors disabled:opacity-50"
          >
            {{ isSaving ? 'Speichern...' : (isEditing ? 'Speichern' : 'Erstellen') }}
          </button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Mode Edit Modal -->
  <ExpertModeModal
    :show="showModeModal"
    :expert="expert"
    :mode="editingMode"
    @close="closeModeModal"
    @saved="onModeSaved"
  />
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { XMarkIcon, ChevronDownIcon, PlusIcon, TrashIcon, EyeIcon, DocumentArrowUpIcon, PencilIcon, GlobeAltIcon, DocumentMagnifyingGlassIcon, PhotoIcon, ArrowUpTrayIcon } from '@heroicons/vue/24/outline'
import axios from 'axios'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import { useConfirmDialog } from '../composables/useConfirmDialog'
import ExpertModeModal from './ExpertModeModal.vue'

const { confirmDelete: showDeleteConfirm } = useConfirmDialog()

const props = defineProps({
  show: Boolean,
  expert: Object,
  availableModels: Array
})

const emit = defineEmits(['close', 'saved'])
const { success, error } = useToast()

const form = ref({
  name: '',
  role: '',
  description: '',
  avatarUrl: '',
  basePrompt: '',
  personalityPrompt: '',
  baseModel: '',
  providerType: '',
  ggufModel: '',
  defaultTemperature: 0.7,
  defaultTopP: 0.9,
  defaultNumCtx: 8192,
  defaultMaxTokens: 4096,
  autoWebSearch: false,
  searchDomains: '',
  maxSearchResults: 5,
  autoFileSearch: false,
  documentDirectory: '',
  showSwitchNotification: true
})

const showAdvanced = ref(false)
const showRAG = ref(false)
const showModes = ref(false)
const isSaving = ref(false)

// Auto-Context: Beim Modell-Wechsel automatisch max. Context-Größe laden
const isLoadingContext = ref(false)
watch(() => form.value.baseModel, async (newModel) => {
  if (!newModel) return
  try {
    isLoadingContext.value = true
    const data = await api.getModelDetails(newModel)
    if (data && data.context_length) {
      form.value.defaultNumCtx = data.context_length
    }
  } catch (err) {
    console.warn('Konnte Context-Größe nicht laden:', err.message)
  } finally {
    isLoadingContext.value = false
  }
})

// File input refs
const basePromptFileInput = ref(null)
const avatarFileInput = ref(null)
const isUploadingAvatar = ref(false)
const avatarUploadError = ref('')

// Modes management
const modes = ref([])
const showAddMode = ref(false)
const newMode = ref({ name: '', promptAddition: '', temperature: null })

// Mode editing modal
const showModeModal = ref(false)
const editingMode = ref(null)

const isEditing = computed(() => !!props.expert)

const canSave = computed(() => {
  return form.value.name?.trim() &&
         form.value.role?.trim() &&
         form.value.basePrompt?.trim() &&
         form.value.baseModel
})

// Watch for expert changes (edit mode)
watch(() => props.expert, (newExpert) => {
  if (newExpert) {
    form.value = {
      name: newExpert.name || '',
      role: newExpert.role || '',
      description: newExpert.description || '',
      avatarUrl: newExpert.avatarUrl || '',
      basePrompt: newExpert.basePrompt || '',
      personalityPrompt: newExpert.personalityPrompt || '',
      baseModel: newExpert.baseModel || '',
      providerType: newExpert.providerType || '',
      ggufModel: newExpert.ggufModel || '',
      defaultTemperature: newExpert.defaultTemperature ?? 0.7,
      defaultTopP: newExpert.defaultTopP ?? 0.9,
      defaultNumCtx: newExpert.defaultNumCtx ?? 8192,
      defaultMaxTokens: newExpert.defaultMaxTokens ?? 4096,
      autoWebSearch: newExpert.autoWebSearch ?? false,
      searchDomains: newExpert.searchDomains || '',
      maxSearchResults: newExpert.maxSearchResults ?? 5,
      autoFileSearch: newExpert.autoFileSearch ?? false,
      documentDirectory: newExpert.documentDirectory || '',
      showSwitchNotification: newExpert.showSwitchNotification ?? true
    }
    modes.value = newExpert.modes ? [...newExpert.modes] : []
  } else {
    resetForm()
  }
}, { immediate: true })

watch(() => props.show, (show) => {
  if (!show && !props.expert) {
    resetForm()
  }
})

function resetForm() {
  form.value = {
    name: '',
    role: '',
    description: '',
    avatarUrl: '',
    basePrompt: '',
    personalityPrompt: '',
    baseModel: '',
    providerType: '',
    ggufModel: '',
    defaultTemperature: 0.7,
    defaultTopP: 0.9,
    defaultNumCtx: 8192,
    defaultMaxTokens: 4096,
    autoWebSearch: false,
    searchDomains: '',
    maxSearchResults: 5,
    autoFileSearch: false,
    documentDirectory: '',
    showSwitchNotification: true
  }
  modes.value = []
  showAdvanced.value = false
  showRAG.value = false
  showModes.value = false
  showAddMode.value = false
  resetNewMode()
}

function handleAvatarError(event) {
  console.warn('Avatar konnte nicht geladen werden:', form.value.avatarUrl)
}

function triggerAvatarFileInput() {
  avatarFileInput.value?.click()
}

async function handleAvatarUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return

  avatarUploadError.value = ''

  if (file.size > 5 * 1024 * 1024) {
    avatarUploadError.value = 'Datei zu groß (max. 5 MB)'
    event.target.value = ''
    return
  }

  try {
    isUploadingAvatar.value = true
    const formData = new FormData()
    formData.append('file', file)

    const response = await axios.post('/api/experts/avatar/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    if (response.data.success) {
      form.value.avatarUrl = response.data.avatarUrl
      success('Avatar hochgeladen')
    } else {
      avatarUploadError.value = response.data.error || 'Upload fehlgeschlagen'
    }
  } catch (err) {
    console.error('Avatar upload failed:', err)
    avatarUploadError.value = err.response?.data?.error || 'Upload fehlgeschlagen'
  } finally {
    isUploadingAvatar.value = false
    event.target.value = ''
  }
}

function resetNewMode() {
  newMode.value = { name: '', promptAddition: '', temperature: null }
}

async function addMode() {
  if (!newMode.value.name?.trim()) return
  if (!props.expert?.id) {
    error('Bitte speichere den Experten zuerst')
    return
  }

  try {
    const created = await api.createExpertMode(props.expert.id, newMode.value)
    modes.value.push(created)
    resetNewMode()
    showAddMode.value = false
    success(`Blickwinkel "${created.name}" hinzugefügt`)
  } catch (err) {
    console.error('Failed to create mode:', err)
    error(err.response?.data?.error || 'Fehler beim Erstellen')
  }
}

async function deleteMode(mode) {
  const confirmed = await showDeleteConfirm(mode.name)
  if (!confirmed) return

  try {
    await api.deleteExpertMode(props.expert.id, mode.id)
    modes.value = modes.value.filter(m => m.id !== mode.id)
    success(`Blickwinkel "${mode.name}" gelöscht`)
  } catch (err) {
    console.error('Failed to delete mode:', err)
    error('Fehler beim Löschen')
  }
}

function openEditMode(mode) {
  editingMode.value = mode
  showModeModal.value = true
}

function closeModeModal() {
  showModeModal.value = false
  editingMode.value = null
}

async function onModeSaved() {
  closeModeModal()
  if (props.expert?.id) {
    try {
      const updatedModes = await api.getExpertModes(props.expert.id)
      modes.value = updatedModes
    } catch (err) {
      console.error('Failed to reload modes:', err)
    }
  }
}

function triggerBasePromptFileInput() {
  basePromptFileInput.value?.click()
}

async function loadBasePromptFromFile(event) {
  const file = event.target.files?.[0]
  if (!file) return

  try {
    const text = await file.text()
    form.value.basePrompt = text.trim()
    success(`Prompt aus "${file.name}" geladen`)
  } catch (err) {
    console.error('Failed to read file:', err)
    error('Fehler beim Lesen der Datei')
  }
  event.target.value = ''
}

async function save() {
  if (!canSave.value) return

  isSaving.value = true

  try {
    if (isEditing.value) {
      await api.updateExpert(props.expert.id, form.value)
      success(`Experte "${form.value.name}" aktualisiert`)
    } else {
      await api.createExpert(form.value)
      success(`Experte "${form.value.name}" erstellt`)
    }
    emit('saved')
  } catch (err) {
    console.error('Failed to save expert:', err)
    error(err.response?.data?.error || 'Fehler beim Speichern')
  } finally {
    isSaving.value = false
  }
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
