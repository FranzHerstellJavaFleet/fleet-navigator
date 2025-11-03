<template>
  <Transition name="modal">
    <div v-if="show" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
      <div class="
        bg-white/90 dark:bg-gray-800/90
        backdrop-blur-xl backdrop-saturate-150
        rounded-2xl shadow-2xl
        w-full max-w-3xl max-h-[90vh]
        border border-gray-200/50 dark:border-gray-700/50
        flex flex-col
        transform transition-all duration-300
      ">
        <!-- Header -->
        <div class="
          flex items-center justify-between p-6
          bg-gradient-to-r from-indigo-500/10 to-purple-500/10
          dark:from-indigo-500/20 dark:to-purple-500/20
          border-b border-gray-200/50 dark:border-gray-700/50
        ">
          <div class="flex items-center gap-4">
            <div class="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-500 shadow-lg">
              <SparklesIcon class="w-7 h-7 text-white" />
            </div>
            <h2 class="text-2xl font-bold bg-gradient-to-r from-gray-900 to-gray-700 dark:from-white dark:to-gray-300 bg-clip-text text-transparent">
              Eigenes Modell erstellen
            </h2>
          </div>
          <button
            @click="$emit('close')"
            :disabled="isCreating"
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

        <!-- Content -->
        <div class="flex-1 overflow-y-auto p-6">
          <div v-if="!isCreating" class="space-y-6">
            <!-- Model Name -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Modellname *
              </label>
              <input
                v-model="modelName"
                type="text"
                placeholder="z.B. nova:latest oder coder:v1"
                class="
                  w-full px-4 py-3
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-gray-100
                  rounded-xl
                  focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent
                  transition-all duration-200
                "
              />
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Format: name:tag (z.B. "nova:latest", "assistant:v1")
              </p>
            </div>

            <!-- Base Model Selection -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Basis-Modell *
              </label>
              <select
                v-model="baseModel"
                class="
                  w-full px-4 py-3
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-gray-100
                  rounded-xl
                  focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent
                  transition-all duration-200
                "
              >
                <option value="">-- Bitte wählen --</option>
                <option v-for="model in availableModels" :key="model.name" :value="model.name">
                  {{ model.name }} {{ model.size ? `(${model.size})` : '' }}
                </option>
              </select>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Das Modell, auf dem dein Custom Model basiert
              </p>
            </div>

            <!-- Description -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Beschreibung
              </label>
              <input
                v-model="description"
                type="text"
                placeholder="z.B. 'Hilfreicher deutscher Assistent'"
                class="
                  w-full px-4 py-3
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-gray-100
                  rounded-xl
                  focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent
                  transition-all duration-200
                "
              />
            </div>

            <!-- System Prompt -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                System Prompt (Charakter/Persönlichkeit)
              </label>
              <textarea
                v-model="systemPrompt"
                rows="5"
                placeholder="z.B. 'Du bist Nova, ein hilfreicher deutscher KI-Assistent. Du antwortest immer höflich und präzise...'"
                class="
                  w-full px-4 py-3
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-gray-100
                  rounded-xl
                  focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent
                  transition-all duration-200
                  resize-none
                "
              ></textarea>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Definiert die Persönlichkeit und das Verhalten des Modells
              </p>
            </div>

            <!-- Advanced Parameters -->
            <div class="border-t border-gray-200 dark:border-gray-700 pt-6">
              <button
                @click="showAdvanced = !showAdvanced"
                class="flex items-center gap-2 text-sm font-medium text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 transition-colors"
              >
                <ChevronDownIcon class="w-5 h-5 transition-transform duration-200" :class="{ 'rotate-180': showAdvanced }" />
                Erweiterte Parameter
              </button>

              <Transition name="fade">
                <div v-if="showAdvanced" class="mt-4 space-y-4">
                  <!-- Temperature -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Temperature: {{ temperature }}
                    </label>
                    <input
                      v-model.number="temperature"
                      type="range"
                      min="0"
                      max="2"
                      step="0.1"
                      class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                    />
                    <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Höher = kreativer, niedriger = fokussierter (Standard: 0.8)
                    </p>
                  </div>

                  <!-- Top P -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Top P: {{ topP }}
                    </label>
                    <input
                      v-model.number="topP"
                      type="range"
                      min="0"
                      max="1"
                      step="0.05"
                      class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                    />
                    <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Nucleus sampling (Standard: 0.9)
                    </p>
                  </div>

                  <!-- Top K -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Top K: {{ topK }}
                    </label>
                    <input
                      v-model.number="topK"
                      type="range"
                      min="0"
                      max="100"
                      step="5"
                      class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                    />
                    <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Top K sampling (Standard: 40)
                    </p>
                  </div>

                  <!-- Repeat Penalty -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Repeat Penalty: {{ repeatPenalty }}
                    </label>
                    <input
                      v-model.number="repeatPenalty"
                      type="range"
                      min="1"
                      max="2"
                      step="0.1"
                      class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                    />
                    <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Verhindert Wiederholungen (Standard: 1.1)
                    </p>
                  </div>
                </div>
              </Transition>
            </div>
          </div>

          <!-- Creating Progress -->
          <div v-else class="flex flex-col items-center justify-center py-12">
            <div class="animate-spin rounded-full h-16 w-16 border-b-2 border-indigo-500 mb-6"></div>
            <p class="text-lg font-medium text-gray-900 dark:text-white mb-2">
              {{ creationProgress }}
            </p>
            <div class="w-full max-w-md bg-gray-200 dark:bg-gray-700 rounded-full h-2 mt-4">
              <div class="bg-indigo-500 h-2 rounded-full transition-all duration-300" :style="{ width: creationProgressPercent + '%' }"></div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="
          p-6 border-t border-gray-200/50 dark:border-gray-700/50
          bg-gray-50/50 dark:bg-gray-900/50
          flex justify-end gap-3
        ">
          <button
            @click="$emit('close')"
            :disabled="isCreating"
            class="
              px-6 py-2.5 rounded-xl
              text-gray-700 dark:text-gray-300
              bg-white dark:bg-gray-800
              border border-gray-300 dark:border-gray-600
              hover:bg-gray-50 dark:hover:bg-gray-700
              font-medium
              shadow-sm hover:shadow-md
              transition-all duration-200
              transform hover:scale-105 active:scale-95
              disabled:opacity-50 disabled:cursor-not-allowed
            "
          >
            Abbrechen
          </button>
          <button
            @click="createModel"
            :disabled="!canCreate || isCreating"
            class="
              px-6 py-2.5 rounded-xl
              bg-gradient-to-r from-indigo-500 to-purple-500
              hover:from-indigo-600 hover:to-purple-600
              text-white font-medium
              shadow-sm hover:shadow-md
              transition-all duration-200
              transform hover:scale-105 active:scale-95
              disabled:opacity-50 disabled:cursor-not-allowed
              flex items-center gap-2
            "
          >
            <SparklesIcon class="w-5 h-5" />
            <span>Modell erstellen</span>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { XMarkIcon, SparklesIcon, ChevronDownIcon } from '@heroicons/vue/24/outline'
import api from '../services/api'
import { useToast } from 'vue-toastification'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'created'])

const toast = useToast()

// Form fields
const modelName = ref('')
const baseModel = ref('')
const description = ref('')
const systemPrompt = ref('')
const temperature = ref(0.8)
const topP = ref(0.9)
const topK = ref(40)
const repeatPenalty = ref(1.1)

// UI state
const showAdvanced = ref(false)
const isCreating = ref(false)
const creationProgress = ref('')
const creationProgressPercent = ref(0)
const availableModels = ref([])

const canCreate = computed(() => {
  return modelName.value.trim() !== '' && baseModel.value.trim() !== ''
})

onMounted(async () => {
  await loadAvailableModels()
})

async function loadAvailableModels() {
  try {
    availableModels.value = await api.getAvailableModels()
  } catch (error) {
    console.error('Failed to load available models:', error)
    toast.error('Fehler beim Laden der Modelle')
  }
}

async function createModel() {
  if (!canCreate.value) return

  isCreating.value = true
  creationProgress.value = 'Erstelle Modell...'
  creationProgressPercent.value = 0

  try {
    const request = {
      name: modelName.value.trim(),
      baseModel: baseModel.value,
      description: description.value.trim() || null,
      systemPrompt: systemPrompt.value.trim() || null,
      temperature: temperature.value,
      topP: topP.value,
      topK: topK.value,
      repeatPenalty: repeatPenalty.value
    }

    await api.createCustomModel(request, (progress) => {
      creationProgress.value = progress

      // Simple progress estimation
      if (progress.includes('Datenbank')) {
        creationProgressPercent.value = 20
      } else if (progress.includes('Ollama')) {
        creationProgressPercent.value = 50
      } else if (progress.includes('erfolgreich')) {
        creationProgressPercent.value = 100
      }
    })

    toast.success('Custom Model erfolgreich erstellt!')
    emit('created')
    emit('close')

    // Reset form
    resetForm()
  } catch (error) {
    console.error('Failed to create custom model:', error)
    toast.error('Fehler beim Erstellen des Modells: ' + error.message)
  } finally {
    isCreating.value = false
    creationProgress.value = ''
    creationProgressPercent.value = 0
  }
}

function resetForm() {
  modelName.value = ''
  baseModel.value = ''
  description.value = ''
  systemPrompt.value = ''
  temperature.value = 0.8
  topP.value = 0.9
  topK.value = 40
  repeatPenalty.value = 1.1
  showAdvanced.value = false
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
