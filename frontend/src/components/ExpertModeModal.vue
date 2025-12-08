<template>
  <Transition name="modal">
    <div v-if="show" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <!-- Header -->
        <div class="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
          <div>
            <h3 class="text-xl font-bold text-gray-900 dark:text-white">
              {{ isEditing ? 'Modus bearbeiten' : 'Neuen Modus hinzufügen' }}
            </h3>
            <p class="text-sm text-gray-500 dark:text-gray-400">
              Experte: {{ expert?.name }}
            </p>
          </div>
          <button
            @click="$emit('close')"
            class="p-2 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            <XMarkIcon class="w-5 h-5" />
          </button>
        </div>

        <!-- Form -->
        <div class="p-6 space-y-5">
          <!-- Name -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Modus-Name (Blickwinkel) *
            </label>
            <input
              v-model="form.name"
              type="text"
              placeholder="z.B. Verwaltungsrecht, Sozialrecht, Strafrecht"
              class="w-full px-4 py-2.5 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            />
          </div>

          <!-- Description -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Beschreibung
            </label>
            <input
              v-model="form.description"
              type="text"
              placeholder="Was macht dieser Modus?"
              class="w-full px-4 py-2.5 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            />
          </div>

          <!-- Prompt Addition -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Zusätzlicher Prompt für diesen Modus
            </label>
            <textarea
              v-model="form.promptAddition"
              rows="5"
              placeholder="AKTUELLER MODUS: [Name]

DEINE AUFGABE:
- ...

WICHTIG:
- ..."
              class="w-full px-4 py-2.5 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent resize-none font-mono text-sm"
            ></textarea>
            <p class="text-xs text-gray-500 mt-1">
              Wird an den Basis-Prompt des Experten angehängt.
            </p>
          </div>

          <!-- Keywords -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Keywords für Auto-Erkennung
            </label>
            <input
              v-model="form.keywords"
              type="text"
              placeholder="behörde,antrag,bescheid,verwaltung (komma-separiert)"
              class="w-full px-4 py-2.5 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            />
            <p class="text-xs text-gray-500 mt-1">
              Wenn ein Keyword in der Anfrage vorkommt, wird dieser Modus automatisch gewählt.
            </p>
          </div>

          <!-- Parameters -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Temperature: {{ form.temperature ?? 'Default' }}
              </label>
              <input
                v-model.number="form.temperature"
                type="range"
                min="0"
                max="2"
                step="0.1"
                class="w-full"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Priorität: {{ form.priority ?? 0 }}
              </label>
              <input
                v-model.number="form.priority"
                type="range"
                min="0"
                max="10"
                step="1"
                class="w-full"
              />
              <p class="text-xs text-gray-500 mt-1">Höhere Priorität = wird zuerst geprüft</p>
            </div>
          </div>

          <!-- Delete Button (only for editing) -->
          <div v-if="isEditing" class="border-t border-gray-200 dark:border-gray-700 pt-5">
            <button
              @click="confirmDelete"
              class="text-sm text-red-600 hover:text-red-700 flex items-center gap-2"
            >
              <TrashIcon class="w-4 h-4" />
              Modus löschen
            </button>
          </div>
        </div>

        <!-- Footer -->
        <div class="flex justify-end gap-3 p-6 border-t border-gray-200 dark:border-gray-700">
          <button
            @click="$emit('close')"
            class="px-4 py-2 rounded-lg text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
          >
            Abbrechen
          </button>
          <button
            @click="save"
            :disabled="!canSave || isSaving"
            class="px-4 py-2 rounded-lg bg-gradient-to-r from-purple-500 to-indigo-500 hover:from-purple-600 hover:to-indigo-600 text-white font-medium transition-all disabled:opacity-50"
          >
            <span v-if="isSaving">Speichern...</span>
            <span v-else>{{ isEditing ? 'Speichern' : 'Hinzufügen' }}</span>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { XMarkIcon, TrashIcon } from '@heroicons/vue/24/outline'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import { useConfirmDialog } from '../composables/useConfirmDialog'

const { confirmDelete: showDeleteConfirm } = useConfirmDialog()

const props = defineProps({
  show: Boolean,
  expert: Object,
  mode: Object
})

const emit = defineEmits(['close', 'saved'])
const { success, error } = useToast()

const form = ref({
  name: '',
  description: '',
  promptAddition: '',
  keywords: '',
  temperature: null,
  priority: 0
})

const isSaving = ref(false)

const isEditing = computed(() => !!props.mode)

const canSave = computed(() => {
  return form.value.name?.trim()
})

// Watch for mode changes (edit mode)
watch(() => props.mode, (newMode) => {
  if (newMode) {
    form.value = {
      name: newMode.name || '',
      description: newMode.description || '',
      promptAddition: newMode.promptAddition || '',
      keywords: newMode.keywords || '',
      temperature: newMode.temperature,
      priority: newMode.priority ?? 0
    }
  } else {
    resetForm()
  }
}, { immediate: true })

// Reset on close
watch(() => props.show, (show) => {
  if (!show && !props.mode) {
    resetForm()
  }
})

function resetForm() {
  form.value = {
    name: '',
    description: '',
    promptAddition: '',
    keywords: '',
    temperature: null,
    priority: 0
  }
}

async function save() {
  if (!canSave.value || !props.expert) return

  isSaving.value = true

  try {
    if (isEditing.value) {
      await api.updateExpertMode(props.mode.id, form.value)
      success(`Modus "${form.value.name}" aktualisiert`)
    } else {
      await api.addExpertMode(props.expert.id, form.value)
      success(`Modus "${form.value.name}" hinzugefügt`)
    }
    emit('saved')
  } catch (err) {
    console.error('Failed to save mode:', err)
    error(err.response?.data?.error || 'Fehler beim Speichern')
  } finally {
    isSaving.value = false
  }
}

async function confirmDelete() {
  if (!props.mode) return
  const confirmed = await showDeleteConfirm(props.mode.name)
  if (!confirmed) return

  try {
    await api.deleteExpertMode(props.mode.id)
    success(`Modus "${props.mode.name}" gelöscht`)
    emit('saved')
  } catch (err) {
    console.error('Failed to delete mode:', err)
    error('Fehler beim Löschen')
  }
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
