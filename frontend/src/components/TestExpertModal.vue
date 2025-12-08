<template>
  <Transition name="modal">
    <div v-if="show" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-3xl h-[80vh] flex flex-col">
        <!-- Header -->
        <div class="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
          <div>
            <h3 class="text-xl font-bold text-gray-900 dark:text-white">
              {{ expert?.name }} testen
            </h3>
            <p class="text-sm text-gray-500 dark:text-gray-400">
              {{ expert?.role }}
            </p>
          </div>
          <button
            @click="$emit('close')"
            class="p-2 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            <XMarkIcon class="w-5 h-5" />
          </button>
        </div>

        <!-- Chat Area -->
        <div class="flex-1 overflow-y-auto p-6 space-y-4">
          <div v-if="messages.length === 0" class="text-center text-gray-500 dark:text-gray-400 py-8">
            Stelle eine Frage an {{ expert?.name }}
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="[
              'max-w-[80%] p-4 rounded-2xl',
              msg.role === 'user'
                ? 'ml-auto bg-purple-500 text-white'
                : 'mr-auto bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white'
            ]"
          >
            <div v-if="msg.role === 'assistant' && msg.mode" class="text-xs text-purple-600 dark:text-purple-400 mb-1">
              Modus: {{ msg.mode }}
            </div>
            <p class="whitespace-pre-wrap">{{ msg.content }}</p>
          </div>

          <div v-if="isLoading" class="mr-auto bg-gray-100 dark:bg-gray-700 p-4 rounded-2xl">
            <div class="flex items-center gap-2 text-gray-500">
              <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-purple-500"></div>
              <span>{{ expert?.name }} denkt...</span>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="p-6 border-t border-gray-200 dark:border-gray-700">
          <!-- Mode Selector -->
          <div class="flex items-center gap-2 mb-3">
            <span class="text-sm text-gray-500 dark:text-gray-400">Modus:</span>
            <select
              v-model="selectedMode"
              class="px-3 py-1 text-sm bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 rounded-lg"
            >
              <option value="">Auto-Detect</option>
              <option v-for="mode in expert?.modes" :key="mode.id" :value="mode.name">
                {{ mode.name }}
              </option>
            </select>
          </div>

          <div class="flex gap-3">
            <input
              v-model="input"
              @keyup.enter="sendMessage"
              type="text"
              placeholder="Deine Frage..."
              :disabled="isLoading"
              class="flex-1 px-4 py-3 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-xl focus:ring-2 focus:ring-purple-500 focus:border-transparent disabled:opacity-50"
            />
            <button
              @click="sendMessage"
              :disabled="!input.trim() || isLoading"
              class="px-6 py-3 rounded-xl bg-gradient-to-r from-purple-500 to-indigo-500 hover:from-purple-600 hover:to-indigo-600 text-white font-medium transition-all disabled:opacity-50"
            >
              Senden
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'
import { XMarkIcon } from '@heroicons/vue/24/outline'
import api from '../services/api'
import { useToast } from '../composables/useToast'

const props = defineProps({
  show: Boolean,
  expert: Object
})

defineEmits(['close'])
const { error } = useToast()

const input = ref('')
const messages = ref([])
const isLoading = ref(false)
const selectedMode = ref('')

// Reset on expert change
watch(() => props.expert, () => {
  messages.value = []
  input.value = ''
  selectedMode.value = ''
})

async function sendMessage() {
  if (!input.value.trim() || !props.expert) return

  const userMessage = input.value.trim()
  input.value = ''

  // Add user message
  messages.value.push({
    role: 'user',
    content: userMessage
  })

  isLoading.value = true

  try {
    const request = {
      input: userMessage,
      mode: selectedMode.value || null
    }

    const response = await api.askExpert(props.expert.id, request)

    // Add assistant message
    messages.value.push({
      role: 'assistant',
      content: response.answer,
      mode: response.usedMode
    })
  } catch (err) {
    console.error('Failed to ask expert:', err)
    error(err.response?.data?.error || 'Fehler bei der Anfrage')

    // Add error message
    messages.value.push({
      role: 'assistant',
      content: 'Entschuldigung, es ist ein Fehler aufgetreten.'
    })
  } finally {
    isLoading.value = false
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
