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
      <div class="prose dark:prose-invert max-w-none" v-html="formattedContent"></div>

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

const formattedContent = computed(() => {
  let content = props.message.content

  // Only apply markdown if enabled in settings
  if (!settingsStore.settings.markdownEnabled) {
    return escapeHtml(content)
  }

  // Enhanced markdown formatting
  // Code blocks with language syntax
  content = content.replace(/```(\w+)?\n([\s\S]*?)```/g, (match, lang, code) => {
    const languageLabel = lang ? `<span class="absolute top-2 right-2 text-xs text-gray-400 bg-gray-700/50 px-2 py-1 rounded">${lang}</span>` : ''
    return `<pre class="relative bg-gray-900 dark:bg-black text-gray-100 p-4 rounded-xl mt-3 mb-3 overflow-x-auto border border-gray-700/50 shadow-lg">${languageLabel}<code class="text-sm font-mono">${escapeHtml(code.trim())}</code></pre>`
  })

  // Inline code
  content = content.replace(/`([^`]+)`/g, (match, code) => {
    return `<code class="bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-200 px-2 py-0.5 rounded text-sm font-mono border border-gray-300 dark:border-gray-600">${escapeHtml(code)}</code>`
  })

  // Headers
  content = content.replace(/^### (.*$)/gim, '<h3 class="text-lg font-bold mt-4 mb-2 text-gray-900 dark:text-gray-100">$1</h3>')
  content = content.replace(/^## (.*$)/gim, '<h2 class="text-xl font-bold mt-5 mb-3 text-gray-900 dark:text-gray-100">$1</h2>')
  content = content.replace(/^# (.*$)/gim, '<h1 class="text-2xl font-bold mt-6 mb-4 text-gray-900 dark:text-gray-100">$1</h1>')

  // Bold (before italic to avoid conflicts)
  content = content.replace(/\*\*([^\*]+)\*\*/g, '<strong class="font-bold text-gray-900 dark:text-gray-100">$1</strong>')

  // Italic
  content = content.replace(/\*([^\*]+)\*/g, '<em class="italic">$1</em>')

  // Unordered lists
  content = content.replace(/^\- (.*$)/gim, '<li class="ml-6 mb-1">$1</li>')
  content = content.replace(/(<li class="ml-6 mb-1">.*<\/li>)/s, '<ul class="my-2 list-disc">$1</ul>')

  // Ordered lists
  content = content.replace(/^\d+\. (.*$)/gim, '<li class="ml-6 mb-1">$1</li>')

  // Links
  content = content.replace(/\[([^\]]+)\]\(([^\)]+)\)/g, '<a href="$2" class="text-blue-600 dark:text-blue-400 hover:underline font-medium" target="_blank" rel="noopener noreferrer">$1</a>')

  // Blockquotes
  content = content.replace(/^> (.*$)/gim, '<blockquote class="border-l-4 border-blue-500 dark:border-blue-600 bg-blue-50 dark:bg-blue-900/20 pl-4 py-2 my-3 italic text-gray-700 dark:text-gray-300 rounded-r">$1</blockquote>')

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
</style>
