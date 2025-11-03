<template>
  <!-- Fullscreen Loading Overlay -->
  <Transition name="fade">
    <div v-if="isGenerating" class="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center z-[100]">
      <div class="text-center">
        <!-- Animated Document Icon -->
        <div class="relative mb-8">
          <div class="absolute inset-0 bg-gradient-to-r from-green-400 to-emerald-400 rounded-full blur-3xl opacity-30 animate-pulse"></div>
          <div class="relative">
            <!-- Animated Document -->
            <div class="animate-bounce">
              <DocumentTextIcon class="w-32 h-32 text-green-400 mx-auto" />
            </div>
            <!-- Animated Sparkles -->
            <div class="absolute top-0 left-0 animate-ping">
              <SparklesIcon class="w-8 h-8 text-yellow-400" />
            </div>
            <div class="absolute top-0 right-0 animate-ping" style="animation-delay: 0.5s">
              <SparklesIcon class="w-6 h-6 text-blue-400" />
            </div>
            <div class="absolute bottom-0 left-1/2 transform -translate-x-1/2 animate-ping" style="animation-delay: 1s">
              <SparklesIcon class="w-7 h-7 text-purple-400" />
            </div>
          </div>
        </div>

        <!-- Status Text -->
        <h3 class="text-3xl font-bold text-white mb-4 animate-pulse">
          {{ statusMessage }}
        </h3>
        <p class="text-lg text-gray-300 mb-8">
          Die KI erstellt gerade dein Dokument...
        </p>

        <!-- Animated Progress Bar -->
        <div class="max-w-md mx-auto mb-8">
          <div class="h-2 bg-gray-700 rounded-full overflow-hidden">
            <div class="h-full bg-gradient-to-r from-green-500 via-emerald-500 to-green-500 rounded-full animate-progress"></div>
          </div>
        </div>

        <!-- Tips -->
        <div class="max-w-lg mx-auto bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-white/20">
          <p class="text-sm text-gray-300 mb-3">
            💡 <strong class="text-white">Tipp:</strong> Je detaillierter dein Prompt, desto besser das Ergebnis!
          </p>
          <p class="text-xs text-gray-400">
            Bei weniger Rechenleistung kann dies 30-60 Sekunden dauern. Bitte habe etwas Geduld...
          </p>
        </div>
      </div>
    </div>
  </Transition>

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
          bg-gradient-to-r from-green-500/10 to-emerald-500/10
          dark:from-green-500/20 dark:to-emerald-500/20
          border-b border-gray-200/50 dark:border-gray-700/50
        ">
          <div class="flex items-center space-x-4">
            <div class="
              p-3 rounded-xl
              bg-gradient-to-br from-green-500 to-emerald-500
              shadow-lg
            ">
              <DocumentTextIcon class="w-7 h-7 text-white" />
            </div>
            <div>
              <h2 class="text-2xl font-bold text-gray-900 dark:text-white">Briefe Agent</h2>
              <p class="text-sm text-gray-600 dark:text-gray-400">KI-gestützte Brieferstellung</p>
            </div>
          </div>
          <button
            @click="close"
            :disabled="isGenerating"
            class="
              p-2 rounded-lg
              text-gray-400 hover:text-gray-600 dark:hover:text-gray-300
              hover:bg-gray-100 dark:hover:bg-gray-700
              transition-all duration-200
              transform hover:scale-110 active:scale-95
              disabled:opacity-50 disabled:cursor-not-allowed
            "
          >
            <XMarkIcon class="w-6 h-6" />
          </button>
        </div>

        <!-- Scrollable Content -->
        <div class="overflow-y-auto max-h-[calc(90vh-180px)] p-6">

          <!-- Brief Templates Selection ListBox -->
          <div v-if="letterTemplates.length > 0" class="mb-6">
            <div class="flex items-center justify-between mb-3">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
                📝 Brief-Vorlage auswählen ({{ filteredTemplates.length }})
              </label>
            </div>

            <!-- Search and Filter -->
            <div class="mb-3 space-y-2">
              <input
                v-model="templateSearch"
                type="text"
                placeholder="Vorlagen durchsuchen..."
                class="
                  w-full px-3 py-2 text-sm rounded-lg
                  border border-gray-300 dark:border-gray-600
                  bg-white dark:bg-gray-700
                  text-gray-900 dark:text-white
                  placeholder-gray-400 dark:placeholder-gray-500
                  focus:outline-none focus:ring-2 focus:ring-green-500
                "
              />

              <!-- Category Filter -->
              <div v-if="templateCategories.length > 0" class="flex flex-wrap gap-2">
                <button
                  @click="selectedCategory = null"
                  :class="[
                    'px-3 py-1 text-xs rounded-lg transition-all',
                    selectedCategory === null
                      ? 'bg-green-500 text-white'
                      : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
                  ]"
                >
                  Alle
                </button>
                <button
                  v-for="category in templateCategories"
                  :key="category"
                  @click="selectedCategory = category"
                  :class="[
                    'px-3 py-1 text-xs rounded-lg transition-all',
                    selectedCategory === category
                      ? 'bg-green-500 text-white'
                      : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
                  ]"
                >
                  {{ category }}
                </button>
              </div>
            </div>

            <!-- Templates ListBox -->
            <div class="space-y-2 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
              <button
                v-for="template in filteredTemplates"
                :key="template.id"
                @click="loadTemplate(template)"
                :disabled="isGenerating"
                class="
                  w-full text-left px-4 py-3 rounded-xl
                  transition-all duration-200
                  flex items-start gap-3
                  border-2
                  disabled:opacity-50 disabled:cursor-not-allowed
                  group
                "
                :class="selectedTemplateId === template.id
                  ? 'bg-green-900/40 dark:bg-green-900/40 border-green-500/50 shadow-lg shadow-green-500/20'
                  : 'bg-gray-50 dark:bg-gray-700/30 border-gray-200 dark:border-gray-600/30 hover:bg-gray-100 dark:hover:bg-gray-700/50 hover:border-green-400 dark:hover:border-green-500/50'"
              >
                <div class="flex-shrink-0 p-2 rounded-lg bg-gradient-to-br from-green-500/20 to-emerald-500/20">
                  <DocumentTextIcon class="w-5 h-5 text-green-500 dark:text-green-400" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="font-medium text-gray-900 dark:text-white truncate">
                    {{ template.name }}
                  </div>
                  <div v-if="template.description" class="text-xs text-gray-600 dark:text-gray-400 mt-1 line-clamp-2">
                    {{ template.description }}
                  </div>
                  <div v-if="template.category" class="mt-2">
                    <span class="inline-flex items-center px-2 py-1 text-xs rounded-lg bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300">
                      {{ template.category }}
                    </span>
                  </div>
                </div>
                <div v-if="selectedTemplateId === template.id" class="flex-shrink-0 mt-1">
                  <div class="w-5 h-5 rounded-full bg-green-500 flex items-center justify-center">
                    <svg class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                  </div>
                </div>
              </button>

              <!-- No Results -->
              <div v-if="filteredTemplates.length === 0" class="text-center py-12 text-gray-500 dark:text-gray-400">
                <DocumentTextIcon class="w-16 h-16 mx-auto mb-3 opacity-30" />
                <p class="text-sm font-medium">Keine Vorlagen gefunden</p>
                <p class="text-xs mt-1">Versuche eine andere Suche oder Kategorie</p>
              </div>
            </div>
          </div>

          <!-- Prompt Input -->
          <div class="mb-6">
            <div class="flex items-center justify-between mb-2">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Was möchtest du erstellen?
              </label>
              <button
                v-if="prompt.trim() && !isGenerating"
                @click="showSaveTemplateDialog = true"
                class="
                  flex items-center gap-1 px-2 py-1 text-xs
                  text-green-600 dark:text-green-400
                  hover:text-green-700 dark:hover:text-green-300
                  hover:bg-green-50 dark:hover:bg-green-900/20
                  rounded-lg
                  transition-all duration-200
                "
                title="Prompt als Vorlage speichern"
              >
                <BookmarkIcon class="w-4 h-4" />
                Speichern
              </button>
            </div>
            <textarea
              v-model="prompt"
              :disabled="isGenerating"
              rows="6"
              placeholder="Beispiel: Erstelle mir eine Kündigung für meine Hausratversicherung aufgrund der Tatsache, dass sie den Jahresbeitrag erhöht hat."
              class="
                w-full px-4 py-3 rounded-xl
                border border-gray-300 dark:border-gray-600
                bg-white dark:bg-gray-700
                text-gray-900 dark:text-white
                placeholder-gray-400 dark:placeholder-gray-500
                focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent
                resize-none
                disabled:opacity-50 disabled:cursor-not-allowed
                transition-all duration-200
              "
            ></textarea>
            <p class="mt-2 text-xs text-gray-500 dark:text-gray-400">
              Beschreibe genau, welches Dokument du erstellen möchtest. Je detaillierter, desto besser das Ergebnis.
            </p>
          </div>

          <!-- Info: Model is configured in settings -->
          <div class="mb-6 p-4 rounded-xl bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-700/50">
            <div class="flex items-start gap-2">
              <CpuChipIcon class="w-5 h-5 text-green-600 dark:text-green-400 flex-shrink-0 mt-0.5" />
              <div class="flex-1">
                <p class="text-sm font-medium text-green-900 dark:text-green-100">
                  Agent arbeitet autonom
                </p>
                <p class="text-xs text-green-700 dark:text-green-300 mt-1">
                  Das Modell für die Dokumentenerstellung wird in den Einstellungen konfiguriert.
                  Der Agent wählt es automatisch aus.
                </p>
              </div>
            </div>
          </div>

          <!-- Progress/Status -->
          <Transition name="fade">
            <div v-if="isGenerating" class="
              bg-gradient-to-r from-green-50 to-emerald-50
              dark:from-green-900/20 dark:to-emerald-900/20
              border border-green-200 dark:border-green-700/50
              rounded-xl p-5 mb-6
              shadow-sm
            ">
              <div class="flex items-center gap-3">
                <div class="animate-spin">
                  <ArrowPathIcon class="w-6 h-6 text-green-600 dark:text-green-400" />
                </div>
                <div class="flex-1">
                  <p class="text-sm font-semibold text-green-900 dark:text-green-100">
                    {{ statusMessage }}
                  </p>
                  <p class="text-xs text-green-700 dark:text-green-300 mt-1">
                    Bitte warten, dies kann einen Moment dauern...
                  </p>
                </div>
              </div>
            </div>
          </Transition>

          <!-- Success Message -->
          <Transition name="fade">
            <div v-if="successMessage" class="
              bg-gradient-to-r from-green-50 to-emerald-50
              dark:from-green-900/20 dark:to-emerald-900/20
              border border-green-200 dark:border-green-700/50
              rounded-xl p-5 mb-6
              shadow-sm
            ">
              <div class="flex items-start gap-3">
                <CheckCircleIcon class="w-6 h-6 text-green-600 dark:text-green-400 flex-shrink-0" />
                <div class="flex-1">
                  <p class="text-sm font-semibold text-green-900 dark:text-green-100">
                    {{ successMessage }}
                  </p>
                  <p class="text-xs text-green-700 dark:text-green-300 mt-1">
                    LibreOffice wurde geöffnet. Du kannst das Dokument jetzt bearbeiten.
                  </p>
                </div>
              </div>
            </div>
          </Transition>

          <!-- Error Message -->
          <Transition name="fade">
            <div v-if="errorMessage" class="
              bg-gradient-to-r from-red-50 to-rose-50
              dark:from-red-900/20 dark:to-rose-900/20
              border border-red-200 dark:border-red-700/50
              rounded-xl p-5 mb-6
              shadow-sm
            ">
              <div class="flex items-start gap-3">
                <ExclamationTriangleIcon class="w-6 h-6 text-red-600 dark:text-red-400 flex-shrink-0" />
                <div class="flex-1">
                  <p class="text-sm font-semibold text-red-900 dark:text-red-100">
                    Fehler bei der Dokumentenerstellung
                  </p>
                  <p class="text-xs text-red-700 dark:text-red-300 mt-1">
                    {{ errorMessage }}
                  </p>
                </div>
              </div>
            </div>
          </Transition>

          <!-- Examples -->
          <div class="
            bg-gradient-to-br from-gray-50 to-gray-100
            dark:from-gray-900 dark:to-gray-800
            p-5 rounded-xl
            border border-gray-200/50 dark:border-gray-700/50
            shadow-sm
          ">
            <h3 class="text-sm font-semibold text-gray-900 dark:text-white mb-3 flex items-center">
              <SparklesIcon class="w-5 h-5 mr-2 text-green-500" />
              Beispiele
            </h3>
            <div class="space-y-2">
              <button
                v-for="example in examples"
                :key="example"
                @click="prompt = example"
                :disabled="isGenerating"
                class="
                  w-full text-left px-4 py-2 rounded-lg
                  bg-white dark:bg-gray-800
                  border border-gray-200 dark:border-gray-700
                  text-sm text-gray-700 dark:text-gray-300
                  hover:border-green-400 dark:hover:border-green-500
                  hover:bg-green-50 dark:hover:bg-green-900/20
                  transition-all duration-200
                  disabled:opacity-50 disabled:cursor-not-allowed
                "
              >
                {{ example }}
              </button>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="
          flex items-center justify-end gap-3 p-6
          bg-gray-50/50 dark:bg-gray-900/50
          border-t border-gray-200/50 dark:border-gray-700/50
        ">
          <button
            @click="close"
            :disabled="isGenerating"
            class="
              px-6 py-2.5 rounded-xl
              border border-gray-300 dark:border-gray-600
              text-gray-700 dark:text-gray-300
              font-medium
              hover:bg-gray-100 dark:hover:bg-gray-700
              disabled:opacity-50 disabled:cursor-not-allowed
              transition-all duration-200
              transform hover:scale-105 active:scale-95
            "
          >
            {{ isGenerating ? 'Läuft...' : 'Abbrechen' }}
          </button>
          <button
            @click="generateDocument"
            :disabled="!canGenerate"
            class="
              px-6 py-2.5 rounded-xl
              bg-gradient-to-r from-green-500 to-emerald-500
              text-white font-medium
              shadow-lg shadow-green-500/30
              hover:shadow-xl hover:shadow-green-500/40
              disabled:opacity-50 disabled:cursor-not-allowed
              transition-all duration-200
              transform hover:scale-105 active:scale-95
              flex items-center gap-2
            "
          >
            <DocumentTextIcon v-if="!isGenerating" class="w-5 h-5" />
            <ArrowPathIcon v-else class="w-5 h-5 animate-spin" />
            {{ isGenerating ? 'Erstelle Dokument...' : 'Dokument erstellen' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Save Template Dialog -->
  <Transition name="modal">
    <div v-if="showSaveTemplateDialog" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
      <div class="
        bg-white dark:bg-gray-800
        rounded-2xl shadow-2xl
        w-full max-w-md
        border border-gray-200 dark:border-gray-700
        transform transition-all duration-300
      ">
        <!-- Header -->
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <h3 class="text-xl font-bold text-gray-900 dark:text-white">Vorlage speichern</h3>
          <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
            Speichere diesen Prompt als wiederverwendbare Vorlage
          </p>
        </div>

        <!-- Content -->
        <div class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Vorlagenname *
            </label>
            <input
              v-model="newTemplate.name"
              type="text"
              placeholder="z.B. Kündigung Versicherung"
              class="
                w-full px-4 py-2 rounded-lg
                border border-gray-300 dark:border-gray-600
                bg-white dark:bg-gray-700
                text-gray-900 dark:text-white
                focus:outline-none focus:ring-2 focus:ring-green-500
              "
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Beschreibung (optional)
            </label>
            <input
              v-model="newTemplate.description"
              type="text"
              placeholder="z.B. Vorlage für Versicherungskündigungen"
              class="
                w-full px-4 py-2 rounded-lg
                border border-gray-300 dark:border-gray-600
                bg-white dark:bg-gray-700
                text-gray-900 dark:text-white
                focus:outline-none focus:ring-2 focus:ring-green-500
              "
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Kategorie (optional)
            </label>
            <input
              v-model="newTemplate.category"
              type="text"
              placeholder="z.B. Kündigungen"
              class="
                w-full px-4 py-2 rounded-lg
                border border-gray-300 dark:border-gray-600
                bg-white dark:bg-gray-700
                text-gray-900 dark:text-white
                focus:outline-none focus:ring-2 focus:ring-green-500
              "
            />
          </div>
        </div>

        <!-- Footer -->
        <div class="p-6 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
          <button
            @click="cancelSaveTemplate"
            class="
              px-4 py-2 rounded-lg
              text-gray-700 dark:text-gray-300
              hover:bg-gray-100 dark:hover:bg-gray-700
              transition-colors
            "
          >
            Abbrechen
          </button>
          <button
            @click="saveTemplate"
            :disabled="!newTemplate.name.trim()"
            class="
              px-4 py-2 rounded-lg
              bg-gradient-to-r from-green-500 to-emerald-500
              text-white font-medium
              disabled:opacity-50 disabled:cursor-not-allowed
              hover:shadow-lg
              transition-all duration-200
            "
          >
            Speichern
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import {
  DocumentTextIcon,
  XMarkIcon,
  ExclamationTriangleIcon,
  CpuChipIcon,
  CheckCircleIcon,
  SparklesIcon,
  ArrowPathIcon,
  BookmarkIcon
} from '@heroicons/vue/24/outline'
import api from '../services/api'
import { useChatStore } from '../stores/chatStore'
import { useToast } from '../composables/useToast'

const { success: successToast, error: errorToast } = useToast()
const chatStore = useChatStore()

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'saved'])

const prompt = ref('')
const isGenerating = ref(false)
const statusMessage = ref('')
const successMessage = ref('')
const errorMessage = ref('')
const letterTemplates = ref([])
const showSaveTemplateDialog = ref(false)
const newTemplate = ref({
  name: '',
  description: '',
  category: ''
})

// Template filtering
const templateSearch = ref('')
const selectedCategory = ref(null)
const showTemplatesExpanded = ref(false)
const selectedTemplateId = ref(null)

// Computed: Get unique categories from templates
const templateCategories = computed(() => {
  const categories = letterTemplates.value
    .map(t => t.category)
    .filter(c => c && c.trim() !== '')
  return [...new Set(categories)].sort()
})

// Computed: Filter templates based on search and category
const filteredTemplates = computed(() => {
  let filtered = letterTemplates.value

  // Filter by category
  if (selectedCategory.value) {
    filtered = filtered.filter(t => t.category === selectedCategory.value)
  }

  // Filter by search term
  if (templateSearch.value.trim()) {
    const search = templateSearch.value.toLowerCase()
    filtered = filtered.filter(t =>
      t.name.toLowerCase().includes(search) ||
      (t.description && t.description.toLowerCase().includes(search)) ||
      (t.category && t.category.toLowerCase().includes(search))
    )
  }

  return filtered
})

const examples = [
  'Erstelle mir eine Kündigung für meine Hausratversicherung aufgrund der Tatsache, dass sie den Jahresbeitrag erhöht hat.',
  'Schreibe einen formalen Beschwerdebrief an meinen Internetanbieter wegen häufiger Verbindungsabbrüche.',
  'Erstelle ein Bewerbungsschreiben für die Position als Software-Entwickler bei einem Tech-Unternehmen.',
  'Verfasse eine höfliche Absage für eine Einladung zu einer Veranstaltung.'
]

const canGenerate = computed(() => {
  return prompt.value.trim().length > 10 && !isGenerating.value
})

// Load templates on mount
onMounted(async () => {
  await loadTemplates()
})

// Reset messages when modal opens
watch(() => props.show, (newVal) => {
  if (newVal) {
    successMessage.value = ''
    errorMessage.value = ''
    loadTemplates()
  }
})

// Load letter templates
const loadTemplates = async () => {
  try {
    letterTemplates.value = await api.getLetterTemplates()
  } catch (error) {
    console.error('Error loading templates:', error)
  }
}

// Load template into prompt
const loadTemplate = (template) => {
  prompt.value = template.prompt
  selectedTemplateId.value = template.id
  successToast(`Vorlage "${template.name}" geladen`)
}

// Save template
const saveTemplate = async () => {
  try {
    await api.createLetterTemplate({
      name: newTemplate.value.name,
      prompt: prompt.value,
      description: newTemplate.value.description || null,
      category: newTemplate.value.category || null
    })

    successToast('Vorlage gespeichert')
    await loadTemplates()

    // Reset form
    newTemplate.value = {
      name: '',
      description: '',
      category: ''
    }
    showSaveTemplateDialog.value = false
  } catch (error) {
    console.error('Error saving template:', error)
    errorToast('Fehler beim Speichern der Vorlage')
  }
}

// Cancel save template
const cancelSaveTemplate = () => {
  newTemplate.value = {
    name: '',
    description: '',
    category: ''
  }
  showSaveTemplateDialog.value = false
}

const generateDocument = async () => {
  if (!canGenerate.value) return

  isGenerating.value = true
  statusMessage.value = 'Generiere Dokument mit AI...'
  successMessage.value = ''
  errorMessage.value = ''

  try {
    // Agent uses configured model from settings - no need to specify model
    const response = await api.generateDocument({
      prompt: prompt.value
      // model is optional - backend uses configured model if not provided
    })

    if (response.success) {
      successMessage.value = response.message
      successToast('Dokument erfolgreich erstellt!')

      // Reset form after 3 seconds
      setTimeout(() => {
        prompt.value = ''
        successMessage.value = ''
        emit('close')
      }, 3000)
    } else {
      errorMessage.value = response.error || 'Unbekannter Fehler'
      errorToast('Fehler beim Erstellen')
    }
  } catch (error) {
    console.error('Document generation error:', error)
    errorMessage.value = error.message || 'Verbindungsfehler zum Server'
    errorToast('Fehler beim Erstellen')
  } finally {
    isGenerating.value = false
    statusMessage.value = ''
  }
}

const close = () => {
  if (!isGenerating.value) {
    emit('close')
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

.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Animated Progress Bar */
@keyframes progress {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.animate-progress {
  animation: progress 2s ease-in-out infinite;
}
</style>
