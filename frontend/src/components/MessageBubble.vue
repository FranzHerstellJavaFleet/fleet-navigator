<template>
  <div
    class="flex animate-fade-in"
    :class="isUser ? 'justify-end' : 'justify-start'"
  >
    <div
      class="
        max-w-3xl rounded-2xl relative group
        transition-all duration-300
        hover:shadow-lg
      "
      :class="messageClasses"
    >
      <!-- Message Header -->
      <div class="flex items-center justify-between mb-2">
        <div class="flex items-center gap-2">
          <!-- Avatar Icon -->
          <div
            class="flex-shrink-0 p-1.5 rounded-lg"
            :class="isUser ? 'bg-fleet-orange-500/20' : 'bg-blue-500/20'"
          >
            <UserCircleIcon v-if="isUser" class="w-5 h-5 text-fleet-orange-600 dark:text-fleet-orange-400" />
            <CpuChipIcon v-else class="w-5 h-5 text-blue-600 dark:text-blue-400" />
          </div>

          <span class="text-sm font-semibold" :class="isUser ? 'text-fleet-orange-700 dark:text-fleet-orange-400' : 'text-blue-700 dark:text-blue-400'">
            {{ isUser ? 'Du' : 'AI Assistant' }}
          </span>

          <!-- Metadata -->
          <div class="flex items-center gap-2 text-xs opacity-60">
            <div class="flex items-center gap-1">
              <ClockIcon class="w-3 h-3" />
              <span>{{ formatTime(message.createdAt) }}</span>
            </div>
            <span v-if="message.tokens" class="flex items-center gap-1">
              •
              <CpuChipIcon class="w-3 h-3" />
              {{ message.tokens }} tokens
            </span>
          </div>
        </div>

        <!-- Action Buttons (only for AI messages) -->
        <div v-if="!isUser" class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <button
            @click="copyToClipboard"
            class="
              p-1.5 rounded-lg
              hover:bg-gray-100 dark:hover:bg-gray-700
              transition-all duration-200
              transform hover:scale-110
            "
            :title="copied ? 'Kopiert!' : 'Text kopieren'"
          >
            <CheckIcon v-if="copied" class="w-4 h-4 text-green-500" />
            <ClipboardDocumentIcon v-else class="w-4 h-4 text-gray-600 dark:text-gray-400" />
          </button>
        </div>
      </div>

      <!-- Message Content -->
      <div class="prose dark:prose-invert max-w-none message-content" v-html="renderedContent"></div>

      <!-- Streaming Indicator -->
      <div v-if="message.isStreaming" class="mt-3 flex items-center gap-2">
        <div class="flex gap-1">
          <div class="w-2 h-2 bg-fleet-orange-500 rounded-full animate-bounce"></div>
          <div class="w-2 h-2 bg-fleet-orange-500 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
          <div class="w-2 h-2 bg-fleet-orange-500 rounded-full animate-bounce" style="animation-delay: 0.4s"></div>
        </div>
        <span class="text-xs text-fleet-orange-500 dark:text-fleet-orange-400 font-medium">Streaming...</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import {
  UserCircleIcon,
  CpuChipIcon,
  ClockIcon,
  ClipboardDocumentIcon,
  CheckIcon
} from '@heroicons/vue/24/outline'
import { useSettingsStore } from '../stores/settingsStore'
import { useToast } from '../composables/useToast'

const { success } = useToast()

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

const settingsStore = useSettingsStore()
const isUser = computed(() => props.message.role === 'USER')
const copied = ref(false)

const messageClasses = computed(() => {
  if (isUser.value) {
    return `
      px-5 py-4
      bg-gradient-to-br from-fleet-orange-50 to-orange-50
      dark:from-fleet-orange-900/30 dark:to-orange-900/30
      border-2 border-fleet-orange-400/50 dark:border-fleet-orange-500/50
      text-gray-800 dark:text-gray-100
    `
  } else {
    return `
      px-5 py-4
      bg-white/90 dark:bg-gray-800/90
      backdrop-blur-sm
      border border-gray-200/50 dark:border-gray-700/50
      text-gray-800 dark:text-gray-100
      shadow-sm
    `
  }
})

// Render content - backend now sends HTML-formatted content
const renderedContent = computed(() => {
  const content = props.message.content

  // For user messages, escape HTML
  if (isUser.value) {
    return escapeHtml(content)
  }

  // For AI messages, the backend sends HTML-formatted content
  // Just return it directly (v-html will render it)
  return content
})

function escapeHtml(text) {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

function formatTime(dateString) {
  const date = new Date(dateString)
  return date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })
}

async function copyToClipboard() {
  try {
    await navigator.clipboard.writeText(props.message.content)
    copied.value = true
    success('Text kopiert')
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (err) {
    console.error('Failed to copy text:', err)
  }
}
</script>

<style scoped>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fade-in 0.3s ease-out;
}

/* Prose Styling for Dark Mode */
:deep(.prose) {
  @apply text-gray-800 dark:text-gray-200;
}

:deep(.prose h1),
:deep(.prose h2),
:deep(.prose h3) {
  @apply text-gray-900 dark:text-gray-100;
}

:deep(.prose code) {
  @apply bg-gray-200 dark:bg-gray-700;
}

:deep(.prose pre) {
  @apply bg-gray-900 dark:bg-black;
}

:deep(.prose a) {
  @apply text-blue-600 dark:text-blue-400;
}

:deep(.prose strong) {
  @apply text-gray-900 dark:text-gray-100;
}

/* HTML Content Styling */
:deep(.message-content p) {
  @apply mb-3 last:mb-0;
}

:deep(.message-content br) {
  @apply block my-1;
}
</style>
