<template>
  <Transition name="modal">
    <div v-if="show && model" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
      <div class="
        bg-white/90 dark:bg-gray-800/90
        backdrop-blur-xl backdrop-saturate-150
        rounded-2xl shadow-2xl
        w-full max-w-4xl max-h-[90vh]
        border border-gray-200/50 dark:border-gray-700/50
        flex flex-col
        transform transition-all duration-300
      ">
        <!-- Header -->
        <div class="
          flex items-center justify-between p-6
          bg-gradient-to-r from-purple-500/10 to-indigo-500/10
          dark:from-purple-500/20 dark:to-indigo-500/20
          border-b border-gray-200/50 dark:border-gray-700/50
        ">
          <div class="flex items-center gap-4">
            <div class="p-3 rounded-xl bg-gradient-to-br from-purple-500 to-indigo-500 shadow-lg">
              <SparklesIcon class="w-7 h-7 text-white" />
            </div>
            <div>
              <h2 class="text-2xl font-bold bg-gradient-to-r from-gray-900 to-gray-700 dark:from-white dark:to-gray-300 bg-clip-text text-transparent">
                {{ model.name }}
              </h2>
              <p class="text-sm text-gray-600 dark:text-gray-400">Version {{ model.version }}</p>
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

        <!-- Content -->
        <div class="flex-1 overflow-y-auto p-6">
          <div class="space-y-6">
            <!-- Basic Info -->
            <div class="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-4 space-y-3">
              <div>
                <span class="text-sm font-medium text-gray-500 dark:text-gray-400">Basis-Modell:</span>
                <p class="text-gray-900 dark:text-white font-medium">{{ model.baseModel }}</p>
              </div>
              <div v-if="model.description">
                <span class="text-sm font-medium text-gray-500 dark:text-gray-400">Beschreibung:</span>
                <p class="text-gray-900 dark:text-white">{{ model.description }}</p>
              </div>
              <div>
                <span class="text-sm font-medium text-gray-500 dark:text-gray-400">Erstellt:</span>
                <p class="text-gray-900 dark:text-white">{{ formatDateLong(model.createdAt, true) }}</p>
              </div>
              <div v-if="model.size">
                <span class="text-sm font-medium text-gray-500 dark:text-gray-400">Größe:</span>
                <p class="text-gray-900 dark:text-white">{{ formatBytes(model.size) }}</p>
              </div>
            </div>

            <!-- Ancestry Chain -->
            <div v-if="ancestry.length > 1">
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-3 flex items-center gap-2">
                <ChartBarIcon class="w-5 h-5" />
                Abstammung
              </h3>
              <div class="flex items-center gap-2 overflow-x-auto pb-2">
                <div v-for="(ancestor, index) in ancestry" :key="ancestor.id" class="flex items-center gap-2">
                  <div class="
                    px-3 py-2 rounded-lg
                    bg-gradient-to-br from-purple-100 to-indigo-100
                    dark:from-purple-900/30 dark:to-indigo-900/30
                    border border-purple-200 dark:border-purple-700
                    whitespace-nowrap
                  ">
                    <p class="text-sm font-medium text-purple-900 dark:text-purple-100">{{ ancestor.name }}</p>
                    <p class="text-xs text-purple-600 dark:text-purple-400">v{{ ancestor.version }}</p>
                  </div>
                  <ArrowRightIcon v-if="index < ancestry.length - 1" class="w-4 h-4 text-gray-400 flex-shrink-0" />
                </div>
              </div>
            </div>

            <!-- System Prompt -->
            <div v-if="model.systemPrompt">
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-3 flex items-center gap-2">
                <ChatBubbleLeftRightIcon class="w-5 h-5" />
                System Prompt
              </h3>
              <div class="
                bg-gray-50 dark:bg-gray-900/50
                border border-gray-200 dark:border-gray-700
                rounded-xl p-4
              ">
                <pre class="text-sm text-gray-900 dark:text-white whitespace-pre-wrap font-mono">{{ model.systemPrompt }}</pre>
              </div>
            </div>

            <!-- Parameters -->
            <div>
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-3 flex items-center gap-2">
                <Cog6ToothIcon class="w-5 h-5" />
                Parameter
              </h3>
              <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div v-if="model.temperature !== null" class="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-4">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Temperature</p>
                  <p class="text-lg font-bold text-gray-900 dark:text-white">{{ model.temperature }}</p>
                </div>
                <div v-if="model.topP !== null" class="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-4">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Top P</p>
                  <p class="text-lg font-bold text-gray-900 dark:text-white">{{ model.topP }}</p>
                </div>
                <div v-if="model.topK !== null" class="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-4">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Top K</p>
                  <p class="text-lg font-bold text-gray-900 dark:text-white">{{ model.topK }}</p>
                </div>
                <div v-if="model.repeatPenalty !== null" class="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-4">
                  <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Repeat Penalty</p>
                  <p class="text-lg font-bold text-gray-900 dark:text-white">{{ model.repeatPenalty }}</p>
                </div>
              </div>
            </div>

            <!-- Modelfile -->
            <div>
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-3 flex items-center gap-2">
                <DocumentTextIcon class="w-5 h-5" />
                Modelfile
              </h3>
              <div class="
                bg-gray-900 dark:bg-black
                border border-gray-700
                rounded-xl p-4
                overflow-x-auto
              ">
                <pre class="text-sm text-green-400 font-mono">{{ model.modelfile }}</pre>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer Actions -->
        <div class="
          p-6 border-t border-gray-200/50 dark:border-gray-700/50
          bg-gray-50/50 dark:bg-gray-900/50
          flex justify-between items-center
        ">
          <button
            @click="confirmDelete"
            class="
              px-6 py-2.5 rounded-xl
              text-red-600 dark:text-red-400
              bg-red-50 dark:bg-red-900/20
              border border-red-200 dark:border-red-800
              hover:bg-red-100 dark:hover:bg-red-900/30
              font-medium
              shadow-sm hover:shadow-md
              transition-all duration-200
              transform hover:scale-105 active:scale-95
              flex items-center gap-2
            "
          >
            <TrashIcon class="w-5 h-5" />
            <span>Löschen</span>
          </button>

          <div class="flex gap-3">
            <button
              @click="$emit('close')"
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
              "
            >
              Schließen
            </button>
            <button
              @click="$emit('edit', model)"
              class="
                px-6 py-2.5 rounded-xl
                bg-gradient-to-r from-purple-500 to-indigo-500
                hover:from-purple-600 hover:to-indigo-600
                text-white font-medium
                shadow-sm hover:shadow-md
                transition-all duration-200
                transform hover:scale-105 active:scale-95
                flex items-center gap-2
              "
            >
              <PencilIcon class="w-5 h-5" />
              <span>Neue Version erstellen</span>
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
  XMarkIcon,
  SparklesIcon,
  ChartBarIcon,
  ChatBubbleLeftRightIcon,
  Cog6ToothIcon,
  DocumentTextIcon,
  TrashIcon,
  PencilIcon,
  ArrowRightIcon
} from '@heroicons/vue/24/outline'
import api from '../services/api'
import { formatDateLong } from '../composables/useFormatters'
import { useToast } from 'vue-toastification'
import { useConfirmDialog } from '../composables/useConfirmDialog'

const { confirmDelete: showDeleteConfirm } = useConfirmDialog()

const props = defineProps({
  show: Boolean,
  model: Object
})

const emit = defineEmits(['close', 'edit', 'deleted'])

const toast = useToast()
const ancestry = ref([])

watch(() => props.model, async (newModel) => {
  if (newModel) {
    await loadAncestry()
  }
}, { immediate: true })

async function loadAncestry() {
  if (!props.model?.id) return

  try {
    ancestry.value = await api.getCustomModelAncestry(props.model.id)
  } catch (error) {
    console.error('Failed to load ancestry:', error)
    ancestry.value = [props.model]
  }
}

// formatDate importiert aus useFormatters.js als formatDateLong(dateString, true)

function formatBytes(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

async function confirmDelete() {
  const confirmed = await showDeleteConfirm(props.model.name, 'Das Modell wird nur aus der Datenbank gelöscht.')
  if (!confirmed) return

  try {
    await api.deleteCustomModel(props.model.id)
    toast.success('Custom Model wurde gelöscht')
    emit('deleted', props.model)
    emit('close')
  } catch (error) {
    console.error('Failed to delete custom model:', error)
    toast.error('Fehler beim Löschen: ' + error.message)
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
